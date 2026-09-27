package com.egor201.puffy.model

// A single outbound server, parsed from a vless:// link.
data class ServerProfile(
    val id: String,
    val subscriptionId: String? = null,
    val name: String,
    val address: String,
    val port: Int,
    val uuid: String,
    val encryption: String = "none",
    val flow: String = "",
    val network: String = "tcp",
    val security: String = "none",
    val sni: String = "",
    val fingerprint: String = "",
    val publicKey: String = "",
    val shortId: String = "",
    val spiderX: String = "",
    val path: String = "",
    val host: String = "",
    val headerType: String = "none",
    val alpn: String = "",
    val allowInsecure: Boolean = false,
    val pingMs: Int? = null,
    val rawLink: String = ""
)
