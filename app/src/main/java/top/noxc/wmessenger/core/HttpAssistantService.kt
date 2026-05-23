package top.noxc.wmessenger.core

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Method
import fi.iki.elonen.NanoHTTPD.Response
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL

object HttpAssistantService {
    private const val TAG = "HttpAssistant"
    private const val DEFAULT_PORT = 6767
    private const val RAW_BASE = "https://raw.githubusercontent.com/NOXC-Team/WEaR/main"

    private var server: NanoHTTPD? = null
    private var isRunning = false
    var lastError: String? = null

    var currentScreen: String = "unknown"
    var appContext: Context? = null

    var cloudPasswordCallback: ((String) -> Unit)? = null
    var proxyConfigCallback: ((String, String, Int, String, String, String) -> Unit)? = null

    private val htmlCacheDir: File?
        get() = appContext?.let { File(it.cacheDir, "http_assistant_html") }

    private val appLanguage: String
        get() {
            return appContext?.resources?.configuration?.locales?.get(0)?.language?.lowercase() ?: "en"
        }

    private fun getHtmlPath(filename: String): String {
        val lang = appLanguage
        return if (lang == "zh") "html/zh/$filename" else "html/$filename"
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get IP error", e)
        }
        return "127.0.0.1"
    }

    private fun downloadHtml(filename: String): String? {
        return try {
            val url = URL("$RAW_BASE/$filename")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Download $filename failed: ${e.message}")
            null
        }
    }

    private fun getCachedHtml(filename: String): String? {
        return try {
            htmlCacheDir?.let { dir ->
                dir.mkdirs()
                val file = File(dir, filename)
                if (file.exists()) file.readText() else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun cacheHtml(filename: String, content: String) {
        try {
            htmlCacheDir?.let { dir ->
                dir.mkdirs()
                File(dir, filename).writeText(content)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cache $filename failed: ${e.message}")
        }
    }

    private fun getHtml(filename: String, fallback: () -> String): String {
        val cached = getCachedHtml(filename)
        if (cached != null) return cached

        val downloaded = downloadHtml(filename)
        if (downloaded != null) {
            cacheHtml(filename, downloaded)
            return downloaded
        }

        val fresh = fallback()
        cacheHtml(filename, fresh)
        return fresh
    }

    fun start(port: Int = DEFAULT_PORT) {
        if (isRunning) {
            Log.w(TAG, "Server already running")
            return
        }

        Log.d(TAG, "Starting HTTP Assistant on port $port...")
        try {
            val httpServer = object : NanoHTTPD(port) {
                private fun respond(status: Response.Status, mime: String, content: String): Response {
                    return newFixedLengthResponse(status, mime, ByteArrayInputStream(content.toByteArray()), content.length.toLong())
                }

                override fun serve(session: IHTTPSession): Response {
                    val uri = session.uri
                    val method = session.method

                    return when {
                        uri == "/" && method == Method.GET -> servePage()
                        uri == "/api/cloud-password" && method == Method.POST -> handleCloudPassword(session)
                        uri == "/api/proxy" && method == Method.POST -> handleProxy(session)
                        uri == "/api/status" && method == Method.GET -> {
                            respond(Response.Status.OK, "application/json", """{"success":true,"message":"running","screen":"$currentScreen"}""")
                        }
                        else -> respond(Response.Status.NOT_FOUND, "application/json", """{"success":false,"message":"Not found"}""")
                    }
                }

                private fun servePage(): Response {
                    val html = when (currentScreen) {
                        "LOGIN" -> getHtml(getHtmlPath("login.html")) { loginPageFallback() }
                        "PROXY_ADD" -> getHtml(getHtmlPath("proxy.html")) { proxyPageFallback() }
                        else -> getHtml(getHtmlPath("default.html")) { defaultPageFallback() }
                    }
                    return respond(Response.Status.OK, "text/html", html)
                }

                private fun loginPageFallback(): String = """
                    <!DOCTYPE html>
                    <html>
                    <head><meta name="viewport" content="width=device-width, initial-scale=1"><title>WM - Login</title>
                    <style>
                        body{font-family:sans-serif;background:#1a1a1a;color:#fff;padding:16px}
                        h2{color:#2AABEE}
                        input{background:#2a2a2a;border:1px solid #555;color:#fff;padding:12px;margin:8px 0;width:100%;box-sizing:border-box;font-size:16px}
                        button{background:#2AABEE;color:#000;border:none;padding:14px 20px;margin-top:12px;cursor:pointer;width:100%;font-size:16px;font-weight:bold}
                        .status{color:#888;margin-top:12px;font-size:14px}
                    </style></head>
                    <body>
                    <h2>Cloud Password</h2>
                    <form onsubmit="submitPassword(event)">
                        <input type="password" id="pwd" placeholder="Enter Cloud Password" autofocus>
                        <button type="submit">Submit Password</button>
                    </form>
                    <div class="status" id="status"></div>
                    <script>
                    async function submitPassword(e) {
                        e.preventDefault();
                        const pwd = document.getElementById('pwd').value;
                        if (!pwd) return;
                        document.getElementById('status').textContent = 'Submitting...';
                        try {
                            const r = await fetch('/api/cloud-password', {
                                method: 'POST',
                                headers: {'Content-Type': 'application/json'},
                                body: JSON.stringify({password: pwd})
                            });
                            const data = await r.json();
                            document.getElementById('status').textContent = data.message;
                            if (data.success) document.getElementById('pwd').value = '';
                        } catch(err) {
                            document.getElementById('status').textContent = 'Error: ' + err.message;
                        }
                    }
                    </script>
                    </body>
                    </html>
                """.trimIndent()

                private fun proxyPageFallback(): String = """
                    <!DOCTYPE html>
                    <html>
                    <head><meta name="viewport" content="width=device-width, initial-scale=1"><title>WM - Proxy</title>
                    <style>
                        body{font-family:sans-serif;background:#1a1a1a;color:#fff;padding:16px}
                        h2{color:#2AABEE}
                        input,select{background:#2a2a2a;border:1px solid #555;color:#fff;padding:12px;margin:8px 0;width:100%;box-sizing:border-box;font-size:16px}
                        button{background:#2AABEE;color:#000;border:none;padding:14px 20px;margin-top:12px;cursor:pointer;width:100%;font-size:16px;font-weight:bold}
                        .tabs{display:flex;gap:4px;margin:8px 0}
                        .tab{flex:1;padding:10px;text-align:center;background:#2a2a2a;border:1px solid #555;color:#888;cursor:pointer;font-size:14px}
                        .tab.active{background:#2AABEE;color:#000;border-color:#2AABEE}
                        .status{color:#888;margin-top:12px;font-size:14px}
                        .hidden{display:none}
                    </style></head>
                    <body>
                    <h2>Add Proxy</h2>
                    <div class="tabs">
                        <div class="tab active" onclick="switchType('SOCKS5')">SOCKS5</div>
                        <div class="tab" onclick="switchType('HTTP')">HTTP</div>
                        <div class="tab" onclick="switchType('MTProto')">MTProto</div>
                    </div>
                    <form onsubmit="submitProxy(event)">
                        <input id="server" placeholder="Server" required>
                        <input id="port" type="number" placeholder="Port" required>
                        <div id="authFields">
                            <input id="username" placeholder="Username">
                            <input id="password" type="password" placeholder="Password">
                        </div>
                        <div id="secretField" class="hidden">
                            <input id="secret" placeholder="Secret (hex)">
                        </div>
                        <button type="submit">Add & Enable</button>
                    </form>
                    <div class="status" id="status"></div>
                    <script>
                    var currentType = 'SOCKS5';
                    function switchType(type) {
                        currentType = type;
                        document.querySelectorAll('.tab').forEach(function(t){t.classList.remove('active')});
                        event.target.classList.add('active');
                        if (type === 'MTProto') {
                            document.getElementById('authFields').classList.add('hidden');
                            document.getElementById('secretField').classList.remove('hidden');
                        } else {
                            document.getElementById('authFields').classList.remove('hidden');
                            document.getElementById('secretField').classList.add('hidden');
                        }
                    }
                    async function submitProxy(e) {
                        e.preventDefault();
                        document.getElementById('status').textContent = 'Submitting...';
                        try {
                            const r = await fetch('/api/proxy', {
                                method: 'POST',
                                headers: {'Content-Type': 'application/json'},
                                body: JSON.stringify({
                                    type: currentType,
                                    server: document.getElementById('server').value,
                                    port: parseInt(document.getElementById('port').value),
                                    username: document.getElementById('username').value,
                                    password: document.getElementById('password').value,
                                    secret: document.getElementById('secret').value
                                })
                            });
                            const data = await r.json();
                            document.getElementById('status').textContent = data.message;
                            if (data.success) {
                                document.getElementById('server').value = '';
                                document.getElementById('port').value = '';
                                document.getElementById('username').value = '';
                                document.getElementById('password').value = '';
                                document.getElementById('secret').value = '';
                            }
                        } catch(err) {
                            document.getElementById('status').textContent = 'Error: ' + err.message;
                        }
                    }
                    </script>
                    </body>
                    </html>
                """.trimIndent()

                private fun defaultPageFallback(): String = """
                    <!DOCTYPE html>
                    <html>
                    <head><meta name="viewport" content="width=device-width, initial-scale=1"><title>WM Assistant</title>
                    <style>
                        body{font-family:sans-serif;background:#1a1a1a;color:#fff;padding:16px;text-align:center}
                        h2{color:#2AABEE}
                        p{color:#888}
                    </style></head>
                    <body>
                    <h2>WearMessenger HTTP Assistant</h2>
                    <p>Current screen: $currentScreen</p>
                    <p>Navigate to Login or Proxy page to use remote input.</p>
                    </body>
                    </html>
                """.trimIndent()

                private fun handleCloudPassword(session: IHTTPSession): Response {
                    return try {
                        val body = readBody(session)
                        val json = JSONObject(body)
                        val password = json.getString("password")
                        cloudPasswordCallback?.invoke(password)
                        respond(Response.Status.OK, "application/json", """{"success":true,"message":"Password submitted"}""")
                    } catch (e: Exception) {
                        respond(Response.Status.BAD_REQUEST, "application/json", """{"success":false,"message":"${e.message}"}""")
                    }
                }

                private fun handleProxy(session: IHTTPSession): Response {
                    return try {
                        val body = readBody(session)
                        val json = JSONObject(body)
                        val type = json.getString("type")
                        val server = json.getString("server")
                        val port = json.getInt("port")
                        val username = json.optString("username", "")
                        val password = json.optString("password", "")
                        val secret = json.optString("secret", "")
                        proxyConfigCallback?.invoke(type, server, port, username, password, secret)
                        respond(Response.Status.OK, "application/json", """{"success":true,"message":"Proxy config submitted"}""")
                    } catch (e: Exception) {
                        respond(Response.Status.BAD_REQUEST, "application/json", """{"success":false,"message":"${e.message}"}""")
                    }
                }

                private fun readBody(session: IHTTPSession): String {
                    val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
                    if (contentLength == 0) return ""
                    val buffer = ByteArray(contentLength)
                    session.inputStream.read(buffer, 0, contentLength)
                    return String(buffer)
                }
            }
            httpServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            server = httpServer
            isRunning = true
            lastError = null
            Log.d(TAG, "HTTP Assistant started on port $port")
        } catch (e: Exception) {
            lastError = e.message ?: "Unknown error"
            Log.e(TAG, "Failed to start HTTP Assistant", e)
        }
    }

    fun stop() {
        try {
            server?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        }
        server = null
        isRunning = false
        Log.d(TAG, "HTTP Assistant stopped")
    }

    fun isRunning(): Boolean = isRunning
}
