package net.kj6ywd.ywdssh.data

import android.content.Context
import net.kj6ywd.ywdssh.model.HostProfile
import org.json.JSONArray
import org.json.JSONObject

class HostStore(context: Context) {
    private val prefs = context.getSharedPreferences("ywd_ssh_hosts", Context.MODE_PRIVATE)

    fun load(): List<HostProfile> = runCatching {
        val array = JSONArray(prefs.getString(KEY_HOSTS, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    HostProfile(
                        id = item.getString("id"),
                        label = item.optString("label", item.getString("hostname")),
                        hostname = item.getString("hostname"),
                        port = item.optInt("port", 22),
                        username = item.getString("username"),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    fun save(hosts: List<HostProfile>) {
        val array = JSONArray()
        hosts.forEach { host ->
            array.put(
                JSONObject()
                    .put("id", host.id)
                    .put("label", host.label)
                    .put("hostname", host.hostname)
                    .put("port", host.port)
                    .put("username", host.username),
            )
        }
        prefs.edit().putString(KEY_HOSTS, array.toString()).apply()
    }

    companion object {
        private const val KEY_HOSTS = "hosts"
    }
}
