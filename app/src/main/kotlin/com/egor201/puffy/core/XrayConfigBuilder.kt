package com.egor201.puffy.core

// Xray JSON config builder — tun inbound, no separate tun2socks needed
import com.egor201.puffy.model.ServerProfile
import org.json.JSONArray
import org.json.JSONObject

object XrayConfigBuilder {

    fun build(profile: ServerProfile): String {
        val root = JSONObject()
        root.put("log", JSONObject().put("loglevel", "warning"))

        val sniffing = JSONObject()
            .put("enabled", true)
            .put("destOverride", JSONArray().put("http").put("tls").put("quic"))

        val tunInbound = JSONObject()
            .put("tag", "tun")
            .put("protocol", "tun")
            .put("settings", JSONObject().put("name", "puffy0").put("MTU", 1500).put("userLevel", 8))
            .put("sniffing", sniffing)

        root.put("inbounds", JSONArray().put(tunInbound))

        val user = JSONObject()
            .put("id", profile.uuid)
            .put("encryption", profile.encryption.ifBlank { "none" })
        if (profile.flow.isNotBlank()) user.put("flow", profile.flow)

        val vnext = JSONObject()
            .put("address", profile.address)
            .put("port", profile.port)
            .put("users", JSONArray().put(user))

        val streamSettings = JSONObject().put("network", profile.network.ifBlank { "tcp" })

        when (profile.network) {
            "ws" -> {
                val wsSettings = JSONObject()
                if (profile.path.isNotBlank()) wsSettings.put("path", profile.path)
                if (profile.host.isNotBlank()) wsSettings.put("headers", JSONObject().put("Host", profile.host))
                streamSettings.put("wsSettings", wsSettings)
            }
            "grpc" -> streamSettings.put("grpcSettings", JSONObject().put("serviceName", profile.path))
            "tcp" -> if (profile.headerType == "http") {
                streamSettings.put(
                    "tcpSettings",
                    JSONObject().put(
                        "header",
                        JSONObject().put("type", "http").put(
                            "request",
                            JSONObject().put(
                                "headers",
                                JSONObject().put("Host", JSONArray().put(profile.host.ifBlank { profile.address }))
                            )
                        )
                    )
                )
            }
        }

        when (profile.security) {
            "tls" -> {
                val tlsSettings = JSONObject().put("allowInsecure", profile.allowInsecure)
                if (profile.sni.isNotBlank()) tlsSettings.put("serverName", profile.sni)
                if (profile.fingerprint.isNotBlank()) tlsSettings.put("fingerprint", profile.fingerprint)
                if (profile.alpn.isNotBlank()) tlsSettings.put("alpn", JSONArray(profile.alpn.split(",")))
                streamSettings.put("security", "tls")
                streamSettings.put("tlsSettings", tlsSettings)
            }
            "reality" -> {
                val realitySettings = JSONObject()
                    .put("show", false)
                    .put("fingerprint", profile.fingerprint.ifBlank { "chrome" })
                    .put("publicKey", profile.publicKey)
                if (profile.sni.isNotBlank()) realitySettings.put("serverName", profile.sni)
                if (profile.shortId.isNotBlank()) realitySettings.put("shortId", profile.shortId)
                if (profile.spiderX.isNotBlank()) realitySettings.put("spiderX", profile.spiderX)
                streamSettings.put("security", "reality")
                streamSettings.put("realitySettings", realitySettings)
            }
        }

        val outbound = JSONObject()
            .put("tag", "proxy")
            .put("protocol", "vless")
            .put("settings", JSONObject().put("vnext", JSONArray().put(vnext)))
            .put("streamSettings", streamSettings)

        val direct = JSONObject()
            .put("tag", "direct")
            .put("protocol", "freedom")
            .put("streamSettings", JSONObject().put("sockopt", JSONObject().put("domainStrategy", "UseIP")))

        val block = JSONObject()
            .put("tag", "block")
            .put("protocol", "blackhole")
            .put("settings", JSONObject().put("response", JSONObject().put("type", "http")))

        root.put("outbounds", JSONArray().put(outbound).put(direct).put(block))
        root.put("routing", JSONObject().put("domainStrategy", "AsIs").put("rules", JSONArray()))
        root.put("dns", JSONObject().put("hosts", JSONObject()).put("servers", JSONArray()))

        return root.toString(2)
    }
}
