package cl.icripto.xmrpos.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get

suspend fun testServerUrl(url: String): Boolean {
    if (url.isBlank()) {
        return false
    }
    return try {
        val client = HttpClient(Android) {
            install(HttpTimeout) {
                requestTimeoutMillis = 2500
                connectTimeoutMillis = 2500
                socketTimeoutMillis = 2500
            }
        }
        val testUrl = url.removeSuffix("/")
        val response = client.get("$testUrl/get_info")
        response.status.value == 200
    } catch (e: Exception) {
//        Log.e("ServerChecker", "Error testing server URL: ${e.message}", e)
        false
    }
}
