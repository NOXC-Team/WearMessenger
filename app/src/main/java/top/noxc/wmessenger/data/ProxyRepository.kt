package top.noxc.wmessenger.data

import android.content.Context
import android.content.SharedPreferences

class ProxyRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "proxy_prefs"
        private const val KEY_PROXY_COUNT = "proxy_count"
        private const val KEY_ACTIVE_PROXY_ID = "active_proxy_id"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveProxy(type: String, server: String, port: Int, username: String, password: String, secret: String): Int {
        val count = prefs.getInt(KEY_PROXY_COUNT, 0)
        val id = count + 1
        
        prefs.edit()
            .putString("proxy_${id}_type", type)
            .putString("proxy_${id}_server", server)
            .putInt("proxy_${id}_port", port)
            .putString("proxy_${id}_username", username)
            .putString("proxy_${id}_password", password)
            .putString("proxy_${id}_secret", secret)
            .putBoolean("proxy_${id}_enabled", false)
            .putInt(KEY_PROXY_COUNT, id)
            .apply()
        
        return id
    }

    fun getAllProxies(): List<ProxyConfig> {
        val count = prefs.getInt(KEY_PROXY_COUNT, 0)
        val proxies = mutableListOf<ProxyConfig>()
        
        for (i in 1..count) {
            val type = prefs.getString("proxy_${i}_type", null) ?: continue
            val server = prefs.getString("proxy_${i}_server", null) ?: continue
            val port = prefs.getInt("proxy_${i}_port", 0)
            if (port == 0) continue
            
            val username = prefs.getString("proxy_${i}_username", "") ?: ""
            val password = prefs.getString("proxy_${i}_password", "") ?: ""
            val secret = prefs.getString("proxy_${i}_secret", "") ?: ""
            val enabled = prefs.getBoolean("proxy_${i}_enabled", false)
            
            proxies.add(ProxyConfig(i, type, server, port, username, password, secret, enabled))
        }
        
        return proxies
    }

    fun setProxyEnabled(id: Int, enabled: Boolean) {
        prefs.edit()
            .putBoolean("proxy_${id}_enabled", enabled)
            .apply()
    }

    fun setActiveProxyId(id: Int) {
        prefs.edit().putInt(KEY_ACTIVE_PROXY_ID, id).apply()
    }

    fun getActiveProxyId(): Int {
        return prefs.getInt(KEY_ACTIVE_PROXY_ID, -1)
    }

    fun removeProxy(id: Int) {
        prefs.edit()
            .remove("proxy_${id}_type")
            .remove("proxy_${id}_server")
            .remove("proxy_${id}_port")
            .remove("proxy_${id}_username")
            .remove("proxy_${id}_password")
            .remove("proxy_${id}_secret")
            .remove("proxy_${id}_enabled")
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun getProxy(): ProxyConfig? {
        val activeId = getActiveProxyId()
        if (activeId == -1) return null
        
        val type = prefs.getString("proxy_${activeId}_type", null) ?: return null
        val server = prefs.getString("proxy_${activeId}_server", null) ?: return null
        val port = prefs.getInt("proxy_${activeId}_port", 0)
        if (port == 0) return null
        
        val username = prefs.getString("proxy_${activeId}_username", "") ?: ""
        val password = prefs.getString("proxy_${activeId}_password", "") ?: ""
        val secret = prefs.getString("proxy_${activeId}_secret", "") ?: ""
        val enabled = prefs.getBoolean("proxy_${activeId}_enabled", false)
        
        return ProxyConfig(activeId, type, server, port, username, password, secret, enabled)
    }

    fun setEnabled(enabled: Boolean) {
        val activeId = getActiveProxyId()
        if (activeId != -1) {
            setProxyEnabled(activeId, enabled)
        }
    }
}

data class ProxyConfig(
    val id: Int,
    val type: String,
    val server: String,
    val port: Int,
    val username: String,
    val password: String,
    val secret: String,
    val enabled: Boolean
)
