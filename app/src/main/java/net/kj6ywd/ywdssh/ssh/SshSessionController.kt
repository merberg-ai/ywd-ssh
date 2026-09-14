package net.kj6ywd.ywdssh.ssh

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.kj6ywd.ywdssh.model.HostProfile
import org.connectbot.sshlib.AuthResult
import org.connectbot.sshlib.ConnectResult
import org.connectbot.sshlib.HostKeyVerifier
import org.connectbot.sshlib.PublicKey
import org.connectbot.sshlib.SshClient
import org.connectbot.sshlib.SshSession
import org.connectbot.terminal.TerminalEmulator
import org.connectbot.terminal.TerminalEmulatorFactory
import java.nio.charset.StandardCharsets

class SshSessionController(
    context: Context,
    val profile: HostProfile,
) {
    sealed interface SessionState {
        data object Idle : SessionState
        data object Connecting : SessionState
        data object Authenticating : SessionState
        data object OpeningTerminal : SessionState
        data object Connected : SessionState
        data object Closed : SessionState
        data class Failed(val message: String) : SessionState
    }

    data class HostKeyPrompt(
        val hostname: String,
        val keyType: String,
        val fingerprint: String,
        val changed: Boolean,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val trustStore = HostKeyTrustStore(context.applicationContext)

    private val _state = MutableStateFlow<SessionState>(SessionState.Idle)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _hostKeyPrompt = MutableStateFlow<HostKeyPrompt?>(null)
    val hostKeyPrompt: StateFlow<HostKeyPrompt?> = _hostKeyPrompt.asStateFlow()

    private var client: SshClient? = null
    private var session: SshSession? = null
    private var connectJob: Job? = null
    private var stdoutJob: Job? = null
    private var stderrJob: Job? = null
    private var disconnectJob: Job? = null
    private var pendingHostKeyDecision: CompletableDeferred<Boolean>? = null

    val terminal: TerminalEmulator = TerminalEmulatorFactory.create(
        initialRows = 24,
        initialCols = 80,
        defaultForeground = Color(0xFFD7F7FF),
        defaultBackground = Color(0xFF05070A),
        onKeyboardInput = { data ->
            scope.launch {
                runCatching { session?.write(data) }
            }
        },
        onResize = { dimensions ->
            scope.launch {
                runCatching {
                    session?.resizeTerminal(
                        widthChars = dimensions.columns,
                        heightRows = dimensions.rows,
                        widthPixels = dimensions.widthPixels,
                        heightPixels = dimensions.heightPixels,
                    )
                }
            }
        },
        autoDetectUrls = true,
    )

    fun connect(password: String) {
        if (connectJob?.isActive == true || _state.value == SessionState.Connected) return
        connectJob = scope.launch {
            connectInternal(password)
        }
    }

    fun disconnect() {
        if (disconnectJob?.isActive == true) return
        disconnectJob = scope.launch {
            cleanup()
            _state.value = SessionState.Closed
        }
    }

    fun answerHostKeyPrompt(accept: Boolean) {
        pendingHostKeyDecision?.complete(accept)
    }

    fun sendText(value: String) {
        sendBytes(value.toByteArray(StandardCharsets.UTF_8))
    }

    fun sendBytes(value: ByteArray) {
        scope.launch {
            runCatching { session?.write(value) }
        }
    }

    fun closeController() {
        disconnect()
        scope.launch {
            disconnectJob?.join()
            scope.cancel()
        }
    }

    private suspend fun connectInternal(password: String) {
        try {
            cleanup(resetState = false)
            _state.value = SessionState.Connecting

            val verifier = object : HostKeyVerifier {
                override suspend fun verify(key: PublicKey): Boolean = verifyHostKey(key)
            }

            val newClient = SshClient(
                host = profile.hostname,
                hostKeyVerifier = verifier,
                port = profile.port,
                clientVersion = "SSH-2.0-YWDSSH_0.0.1",
            )
            client = newClient

            when (val result = newClient.connect()) {
                is ConnectResult.Success -> Unit
                else -> fail("SSH connection failed: $result")
            }

            _state.value = SessionState.Authenticating
            when (val result = newClient.authenticatePassword(profile.username, password)) {
                is AuthResult.Success -> Unit
                else -> fail("Authentication failed: $result")
            }

            _state.value = SessionState.OpeningTerminal
            val newSession = newClient.openSession() ?: fail("Server did not open an SSH session")
            session = newSession

            val size = terminal.dimensions
            val ptyOk = newSession.requestPty(
                terminalType = "xterm-256color",
                widthChars = size.columns,
                heightRows = size.rows,
                widthPixels = size.widthPixels,
                heightPixels = size.heightPixels,
            )
            if (!ptyOk) fail("Server rejected the PTY request")
            if (!newSession.requestShell()) fail("Server rejected the interactive shell")

            stdoutJob = scope.launch {
                for (data in newSession.stdout) {
                    terminal.writeInput(data)
                }
            }
            stderrJob = scope.launch {
                for (data in newSession.stderr) {
                    terminal.writeInput(data)
                }
            }
            scope.launch {
                newClient.disconnectedFlow.collect { cause ->
                    if (_state.value == SessionState.Connected) {
                        _state.value = SessionState.Failed(
                            cause?.message ?: "SSH connection closed by remote host",
                        )
                    }
                }
            }

            _state.value = SessionState.Connected
        } catch (failure: SessionFailure) {
            cleanup(resetState = false)
            _state.value = SessionState.Failed(failure.message ?: "SSH session failed")
        } catch (failure: Throwable) {
            cleanup(resetState = false)
            _state.value = SessionState.Failed(failure.message ?: failure.javaClass.simpleName)
        }
    }

    private suspend fun verifyHostKey(key: PublicKey): Boolean {
        val status = trustStore.status(profile, key)
        when (status) {
            HostKeyTrustStore.Status.Trusted -> return true
            HostKeyTrustStore.Status.Unknown,
            HostKeyTrustStore.Status.Changed -> Unit
        }

        val decision = CompletableDeferred<Boolean>()
        pendingHostKeyDecision = decision
        _hostKeyPrompt.value = HostKeyPrompt(
            hostname = if (profile.port == 22) profile.hostname else "${profile.hostname}:${profile.port}",
            keyType = key.type,
            fingerprint = trustStore.fingerprint(key),
            changed = status == HostKeyTrustStore.Status.Changed,
        )

        val accepted = decision.await()
        pendingHostKeyDecision = null
        _hostKeyPrompt.value = null
        if (accepted) trustStore.trust(profile, key)
        return accepted
    }

    private suspend fun cleanup(resetState: Boolean = true) {
        pendingHostKeyDecision?.complete(false)
        pendingHostKeyDecision = null
        _hostKeyPrompt.value = null
        stdoutJob?.cancel()
        stderrJob?.cancel()
        stdoutJob = null
        stderrJob = null
        runCatching { session?.close() }
        session = null
        runCatching { client?.disconnect() }
        client = null
        if (resetState) _state.value = SessionState.Idle
    }

    private fun fail(message: String): Nothing = throw SessionFailure(message)

    private class SessionFailure(message: String) : RuntimeException(message)
}
