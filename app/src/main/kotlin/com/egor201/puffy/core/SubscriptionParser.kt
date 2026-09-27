package com.egor201.puffy.core

// Subscription body + userinfo header parsing
import android.util.Base64
import com.egor201.puffy.model.ServerProfile

data class SubscriptionMeta(
    val trafficUsedBytes: Long? = null,
    val trafficTotalBytes: Long? = null,
    val expiresAtSeconds: Long? = null,
    val updateIntervalHours: Int? = null,
    val title: String? = null
)

object SubscriptionParser {

    fun parseServers(body: String, subscriptionId: String): List<ServerProfile> {
        val decoded = decodeIfBase64(body.trim())
        return decoded.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("vless://") }
            .mapNotNull { VlessUriParser.parse(it, subscriptionId) }
            .toList()
    }

    fun parseMeta(headers: Map<String, List<String>>): SubscriptionMeta {
        val userInfo = headerValue(headers, "subscription-userinfo")
        var used: Long? = null
        var total: Long? = null
        var expire: Long? = null

        if (userInfo != null) {
            val fields = userInfo.split(";").map { it.trim() }
            var upload = 0L
            var download = 0L
            for (field in fields) {
                val (key, value) = field.split("=").let { it.getOrElse(0) { "" } to it.getOrElse(1) { "" } }
                val number = value.toLongOrNull() ?: continue
                when (key) {
                    "upload" -> upload = number
                    "download" -> download = number
                    "total" -> total = number
                    "expire" -> expire = number
                }
            }
            used = upload + download
        }

        val intervalHeader = headerValue(headers, "profile-update-interval")?.toIntOrNull()
        val titleHeader = headerValue(headers, "profile-title")?.let { decodeIfBase64(it) }

        return SubscriptionMeta(
            trafficUsedBytes = used,
            trafficTotalBytes = total,
            expiresAtSeconds = expire,
            updateIntervalHours = intervalHeader,
            title = titleHeader
        )
    }

    private fun headerValue(headers: Map<String, List<String>>, name: String): String? {
        val entry = headers.entries.firstOrNull { it.key?.equals(name, ignoreCase = true) == true }
        return entry?.value?.firstOrNull()
    }

    private fun decodeIfBase64(input: String): String {
        val looksLikeLinks = input.contains("://")
        if (looksLikeLinks) return input
        return try {
            val normalized = input.replace("-", "+").replace("_", "/")
            String(Base64.decode(normalized, Base64.DEFAULT))
        } catch (e: Exception) {
            input
        }
    }
}
