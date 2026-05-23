package top.noxc.wmessenger.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object TranslationService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun translate(text: String, provider: String = "google", targetLang: String = "zh-CN"): String =
        withContext(Dispatchers.IO) {
            try {
                val primaryResult = when (provider.lowercase()) {
                    "bing" -> translateBing(text, targetLang)
                    else -> translateGoogle(text, targetLang)
                }
                if (primaryResult != "Failed" && primaryResult != "No result") {
                    return@withContext primaryResult
                }
                
                val fallbackProvider = if (provider.lowercase() == "bing") "google" else "bing"
                when (fallbackProvider) {
                    "bing" -> translateBing(text, targetLang)
                    else -> translateGoogle(text, targetLang)
                }
            } catch (e: Exception) {
                "Translation failed: ${e.message}"
            }
        }

    private fun translateGoogle(text: String, targetLang: String): String {
        val encoded = URLEncoder.encode(text, "UTF-8")
        val url = "https://translate.google.com/m?sl=auto&tl=$targetLang&q=$encoded"
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: return "Failed"
        val doc = Jsoup.parse(html)
        val result = doc.select("div.result-container, div.t0").first()?.text()
        return result ?: "No result"
    }

    private fun translateBing(text: String, targetLang: String): String {
        val encoded = URLEncoder.encode(text, "UTF-8")
        val url = "https://www.bing.com/translator?text=$encoded&from=&to=$targetLang"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build()
        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: return "Failed"
        val doc = Jsoup.parse(html)
        val result = doc.select("div#t_src, span#t_src").first()?.text()
        return result ?: "No result"
    }
}
