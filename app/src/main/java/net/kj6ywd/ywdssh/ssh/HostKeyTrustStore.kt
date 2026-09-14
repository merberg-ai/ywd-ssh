package net.kj6ywd.ywdssh.ssh

import android.content.Context
import android.util.Base64
import net.kj6ywd.ywdssh.model.HostProfile
import org.connectbot.sshlib.PublicKey
import java.io.File
import java.security.MessageDigest

class HostKeyTrustStore(context: Context) {
    enum class Status {
        Trusted,
        Unknown,
        Changed,
    }

    private val file = File(context.filesDir, "known_hosts")
    private val lock = Any()

    fun status(profile: HostProfile, key: PublicKey): Status = synchronized(lock) {
        val alias = hostAlias(profile)
        val matching = readEntries().filter { it.host == alias }
        if (matching.isEmpty()) return@synchronized Status.Unknown

        val encoded = Base64.encodeToString(key.encoded, Base64.NO_WRAP)
        if (matching.any { it.type == key.type && it.encoded == encoded }) {
            Status.Trusted
        } else {
            Status.Changed
        }
    }

    fun trust(profile: HostProfile, key: PublicKey) = synchronized(lock) {
        file.parentFile?.mkdirs()
        val alias = hostAlias(profile)
        val retained = if (file.exists()) {
            file.readLines().filterNot { line ->
                line.trim().split(Regex("\\s+")).firstOrNull() == alias
            }
        } else {
            emptyList()
        }
        val encoded = Base64.encodeToString(key.encoded, Base64.NO_WRAP)
        val replacement = "$alias ${key.type} $encoded"
        file.writeText((retained + replacement).joinToString("\n", postfix = "\n"))
    }

    fun fingerprint(key: PublicKey): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.encoded)
        val value = Base64.encodeToString(digest, Base64.NO_WRAP or Base64.NO_PADDING)
        return "SHA256:$value"
    }

    private fun hostAlias(profile: HostProfile): String =
        if (profile.port == 22) profile.hostname else "[${profile.hostname}]:${profile.port}"

    private fun readEntries(): List<Entry> {
        if (!file.exists()) return emptyList()
        return file.readLines().mapNotNull { line ->
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 3) null else Entry(parts[0], parts[1], parts[2])
        }
    }

    private data class Entry(
        val host: String,
        val type: String,
        val encoded: String,
    )
}
