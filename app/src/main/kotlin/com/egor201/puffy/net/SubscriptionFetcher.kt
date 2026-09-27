package com.egor201.puffy.net

// Lightweight subscription fetcher — no OkHttp/Retrofit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class FetchResult(val body: String, val headers: Map<String, List<String>>)

object SubscriptionFetcher {

    suspend fun fetch(url: String): Result<FetchResult> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("User-Agent", "Puffy/1.0 (Android)")

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val headers = connection.headerFields
            connection.disconnect()

            Result.success(FetchResult(body, headers))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
