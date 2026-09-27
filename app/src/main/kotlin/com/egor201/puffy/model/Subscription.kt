package com.egor201.puffy.model

// A subscription group — a URL that returns a list of server links,
// plus metadata reported by the panel (traffic, expiry, update interval).
data class Subscription(
    val id: String,
    val name: String,
    val url: String,
    val lastUpdatedAt: Long = 0L,
    val updateIntervalHours: Int = 0,
    val trafficUsedBytes: Long? = null,
    val trafficTotalBytes: Long? = null,
    val expiresAtSeconds: Long? = null
)
