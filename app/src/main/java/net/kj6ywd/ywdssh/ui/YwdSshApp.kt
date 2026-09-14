package net.kj6ywd.ywdssh.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.kj6ywd.ywdssh.data.HostStore
import net.kj6ywd.ywdssh.model.HostProfile
import net.kj6ywd.ywdssh.ssh.SshSessionController
import org.connectbot.terminal.Terminal

private data class ActiveSession(
    val profile: HostProfile,
    val password: String,
)

@Composable
fun YwdSshApp() {
    val context = LocalContext.current.applicationContext
    val store = remember { HostStore(context) }
    var hosts by remember { mutableStateOf(store.load()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingConnect by remember { mutableStateOf<HostProfile?>(null) }
    var activeSession by remember { mutableStateOf<ActiveSession?>(null) }

    val active = activeSession
    if (active != null) {
        TerminalSessionScreen(
            profile = active.profile,
            password = active.password,
            onBack = { activeSession = null },
        )
        return
    }

    HostsScreen(
        hosts = hosts,
        onAdd = { showAddDialog = true },
        onConnect = { pendingConnect = it },
        onDelete = { host ->
            hosts = hosts.filterNot { it.id == host.id }
            store.save(hosts)
        },
    )

    if (showAddDialog) {
        AddHostDialog(
            onDismiss = { showAddDialog = false },
            onSave = { host ->
                hosts = (hosts + host).sortedBy { it.label.lowercase() }
                store.save(hosts)
                showAddDialog = false
            },
        )
    }

    pendingConnect?.let { host ->
        PasswordDialog(
            profile = host,
            onDismiss = { pendingConnect = null },
            onConnect = { password ->
                pendingConnect = null
                activeSession = ActiveSession(host, password)
            },
        )
    }
}

@Composable
private fun HostsScreen(
    hosts: List<HostProfile>,
    onAdd: () -> Unit,
    onConnect: (HostProfile) -> Unit,
    onDelete: (HostProfile) -> Unit,
) {
    Scaffold(
        containerColor = YwdBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = YwdCyan,
                contentColor = Color.Black,
            ) {
                Text("+", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
        },
        topBar = { YwdHeader() },
    ) { padding ->
        if (hosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NO HOSTS CONFIGURED", color = YwdMuted)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap + to add an SSH target.", color = YwdText)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(hosts, key = { it.id }) { host ->
                    HostCard(host, onConnect, onDelete)
                }
            }
        }
    }
}

@Composable
private fun YwdHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(YwdSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "YWD//SSH",
                color = YwdCyan,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Text("NO CLOUD // NO SUBS", color = YwdMagenta, fontSize = 10.sp)
        }
        Text("simple secure shell", color = YwdMuted, fontSize = 11.sp)
    }
}

@Composable
private fun HostCard(
    host: HostProfile,
    onConnect: (HostProfile) -> Unit,
    onDelete: (HostProfile) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConnect(host) },
        colors = CardDefaults.cardColors(containerColor = YwdSurface),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("●", color = YwdGreen, fontSize = 11.sp)
                Spacer(Modifier.size(8.dp))
                Text(host.label, color = YwdText, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onDelete(host) }) {
                    Text("DELETE", color = YwdMuted, fontSize = 10.sp)
                }
            }
            Text(
                "${host.username}@${host.hostname}:${host.port}",
                color = YwdCyan,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun AddHostDialog(
    onDismiss: () -> Unit,
    onSave: (HostProfile) -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var hostname by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22") }
    var username by remember { mutableStateOf("") }
    val parsedPort = port.toIntOrNull()
    val valid = hostname.isNotBlank() &&
        username.isNotBlank() &&
        parsedPort != null &&
        parsedPort in 1..65535

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = YwdSurface,
        title = { Text("ADD HOST", color = YwdCyan) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(label, { label = it }, label = { Text("Nickname") }, singleLine = true)
                OutlinedTextField(hostname, { hostname = it }, label = { Text("Hostname / IP") }, singleLine = true)
                OutlinedTextField(
                    port,
                    { port = it.filter(Char::isDigit).take(5) },
                    label = { Text("Port") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(username, { username = it }, label = { Text("Username") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        HostProfile.create(
                            label = label,
                            hostname = hostname,
                            port = parsedPort ?: 22,
                            username = username,
                        ),
                    )
                },
            ) { Text("SAVE") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
    )
}

@Composable
private fun PasswordDialog(
    profile: HostProfile,
    onDismiss: () -> Unit,
    onConnect: (String) -> Unit,
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = YwdSurface,
        title = { Text("CONNECT", color = YwdCyan) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${profile.username}@${profile.hostname}:${profile.port}", color = YwdMuted)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                )
                Text("Passwords are never saved.", color = YwdMuted, fontSize = 10.sp)
            }
        },
        confirmButton = {
            Button(onClick = { onConnect(password) }) { Text("CONNECT") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
    )
}

@Composable
private fun TerminalSessionScreen(
    profile: HostProfile,
    password: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val controller = remember(profile.id) {
        SshSessionController(context.applicationContext, profile)
    }
    val state by controller.state.collectAsState()
    val hostKeyPrompt by controller.hostKeyPrompt.collectAsState()

    LaunchedEffect(controller, password) {
        controller.connect(password)
    }
    DisposableEffect(controller) {
        onDispose { controller.closeController() }
    }
    BackHandler {
        controller.disconnect()
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(YwdBackground),
    ) {
        TerminalHeader(profile, state, onBack)
        HorizontalDivider(color = YwdSurfaceAlt)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Terminal(
                terminalEmulator = controller.terminal,
                modifier = Modifier.fillMaxSize(),
                initialFontSize = 13.sp,
                backgroundColor = YwdBackground,
                foregroundColor = YwdText,
                keyboardEnabled = true,
                showSoftKeyboard = true,
                onPasteRequest = {
                    clipboard.getText()?.text?.let(controller.terminal::pasteText)
                },
                onHyperlinkClick = { url ->
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                },
            )

            when (val current = state) {
                is SshSessionController.SessionState.Connected -> Unit
                is SshSessionController.SessionState.Failed -> {
                    SessionOverlay(
                        title = "CONNECTION FAILED",
                        detail = current.message,
                        busy = false,
                        onRetry = { controller.connect(password) },
                    )
                }
                is SshSessionController.SessionState.Closed -> Unit
                else -> SessionOverlay(
                    title = stateLabel(current),
                    detail = "${profile.hostname}:${profile.port}",
                    busy = true,
                    onRetry = null,
                )
            }
        }

        ExtraKeyBar(controller)
    }

    hostKeyPrompt?.let { prompt ->
        HostKeyDialog(prompt, controller)
    }
}

@Composable
private fun TerminalHeader(
    profile: HostProfile,
    state: SshSessionController.SessionState,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(YwdSurface)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) { Text("<", color = YwdCyan, fontSize = 20.sp) }
        Column {
            Text(profile.label, color = YwdText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("${profile.username}@${profile.hostname}", color = YwdMuted, fontSize = 9.sp)
        }
        Spacer(Modifier.weight(1f))
        val connected = state is SshSessionController.SessionState.Connected
        Text(
            if (connected) "● ONLINE" else "○ ${stateLabel(state)}",
            color = if (connected) YwdGreen else YwdMagenta,
            fontSize = 9.sp,
        )
    }
}

@Composable
private fun BoxScope.SessionOverlay(
    title: String,
    detail: String,
    busy: Boolean,
    onRetry: (() -> Unit)?,
) {
    Surface(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(24.dp),
        color = YwdSurface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (busy) CircularProgressIndicator(color = YwdCyan, modifier = Modifier.size(28.dp))
            Text(title, color = if (busy) YwdCyan else YwdRed, fontWeight = FontWeight.Bold)
            Text(detail, color = YwdMuted, fontSize = 11.sp)
            if (onRetry != null) {
                Button(onClick = onRetry) { Text("RETRY") }
            }
        }
    }
}

@Composable
private fun ExtraKeyBar(controller: SshSessionController) {
    val keys = listOf(
        "ESC" to byteArrayOf(0x1B),
        "TAB" to byteArrayOf(0x09),
        "↑" to "\u001B[A".toByteArray(),
        "↓" to "\u001B[B".toByteArray(),
        "←" to "\u001B[D".toByteArray(),
        "→" to "\u001B[C".toByteArray(),
        "^C" to byteArrayOf(0x03),
        "^D" to byteArrayOf(0x04),
        "/" to "/".toByteArray(),
        "~" to "~".toByteArray(),
        "|" to "|".toByteArray(),
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(YwdSurface)
            .padding(vertical = 5.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        items(keys) { (label, bytes) ->
            Button(
                onClick = { controller.sendBytes(bytes) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = YwdSurfaceAlt,
                    contentColor = YwdCyan,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(label, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun HostKeyDialog(
    prompt: SshSessionController.HostKeyPrompt,
    controller: SshSessionController,
) {
    AlertDialog(
        onDismissRequest = { controller.answerHostKeyPrompt(false) },
        containerColor = YwdSurface,
        title = {
            Text(
                if (prompt.changed) "HOST KEY CHANGED" else "NEW HOST KEY",
                color = if (prompt.changed) YwdRed else YwdCyan,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (prompt.changed) {
                    Text(
                        "WARNING: this host presented a different key than the one previously trusted.",
                        color = YwdRed,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    Text("Verify this fingerprint before trusting the host.", color = YwdText)
                }
                Text(prompt.hostname, color = YwdText)
                Text(prompt.keyType, color = YwdMuted)
                Text(prompt.fingerprint, color = YwdGreen, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = { controller.answerHostKeyPrompt(true) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (prompt.changed) YwdRed else YwdCyan,
                    contentColor = Color.Black,
                ),
            ) {
                Text(if (prompt.changed) "TRUST NEW KEY" else "TRUST")
            }
        },
        dismissButton = {
            TextButton(onClick = { controller.answerHostKeyPrompt(false) }) {
                Text("REJECT")
            }
        },
    )
}

private fun stateLabel(state: SshSessionController.SessionState): String = when (state) {
    SshSessionController.SessionState.Idle -> "IDLE"
    SshSessionController.SessionState.Connecting -> "CONNECTING"
    SshSessionController.SessionState.Authenticating -> "AUTHENTICATING"
    SshSessionController.SessionState.OpeningTerminal -> "OPENING PTY"
    SshSessionController.SessionState.Connected -> "ONLINE"
    SshSessionController.SessionState.Closed -> "CLOSED"
    is SshSessionController.SessionState.Failed -> "FAILED"
}
