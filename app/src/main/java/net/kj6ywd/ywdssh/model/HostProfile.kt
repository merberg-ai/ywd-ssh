package net.kj6ywd.ywdssh.model

import java.util.UUID

data class HostProfile(
    val id: String,
    val label: String,
    val hostname: String,
    val port: Int,
    val username: String,
) {
    companion object {
        fun create(
            label: String,
            hostname: String,
            port: Int = 22,
            username: String,
        ) = HostProfile(
            id = UUID.randomUUID().toString(),
            label = label.ifBlank { hostname },
            hostname = hostname.trim(),
            port = port,
            username = username.trim(),
        )
    }
}
