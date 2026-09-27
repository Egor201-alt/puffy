package com.egor201.puffy.core

// VLESS URI parser
import com.egor201.puffy.model.ServerProfile
import java.net.URLDecoder
import java.util.UUID

object VlessUriParser {

    fun parse(link: String, subscriptionId: String? = null): ServerProfile? {
        val trimmed = link.trim()
        if (!trimmed.startsWith("vless://")) return null

        return try {
            val withoutScheme = trimmed.removePrefix("vless://")
            val hashIndex = withoutScheme.indexOf('#')
            val nameRaw = if (hashIndex >= 0) withoutScheme.substring(hashIndex + 1) else ""
            val beforeHash = if (hashIndex >= 0) withoutScheme.substring(0, hashIndex) else withoutScheme

            val atIndex = beforeHash.indexOf('@')
            if (atIndex < 0) return null
            val uuid = beforeHash.substring(0, atIndex)

            val afterAt = beforeHash.substring(atIndex + 1)
            val queryIndex = afterAt.indexOf('?')
            val hostPort = if (queryIndex >= 0) afterAt.substring(0, queryIndex) else afterAt
            val query = if (queryIndex >= 0) afterAt.substring(queryIndex + 1) else ""

            val hostPortParts = hostPort.split(":")
            val host = hostPortParts.getOrNull(0)?.trim('[', ']') ?: return null
            val port = hostPortParts.getOrNull(1)?.toIntOrNull() ?: 443

            val params = parseQuery(query)
            val name = if (nameRaw.isNotBlank()) decode(nameRaw) else host

            ServerProfile(
                id = UUID.randomUUID().toString(),
                subscriptionId = subscriptionId,
                name = name,
                address = host,
                port = port,
                uuid = uuid,
                encryption = params["encryption"] ?: "none",
                flow = params["flow"] ?: "",
                network = params["type"] ?: "tcp",
                security = params["security"] ?: "none",
                sni = params["sni"] ?: "",
                fingerprint = params["fp"] ?: "",
                publicKey = params["pbk"] ?: "",
                shortId = params["sid"] ?: "",
                spiderX = params["spx"] ?: "",
                path = params["path"] ?: "",
                host = params["host"] ?: "",
                headerType = params["headerType"] ?: "none",
                alpn = params["alpn"] ?: "",
                allowInsecure = params["allowInsecure"] == "1" || params["allowInsecure"] == "true",
                rawLink = trimmed
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split("&").mapNotNull { part ->
            val idx = part.indexOf('=')
            if (idx < 0) return@mapNotNull null
            part.substring(0, idx) to decode(part.substring(idx + 1))
        }.toMap()
    }

    private fun decode(value: String): String = try {
        URLDecoder.decode(value, "UTF-8")
    } catch (e: Exception) {
        value
    }
}
