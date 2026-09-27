package com.egor201.puffy.data

// Local storage — subscriptions + servers, DataStore-backed JSON
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.egor201.puffy.model.ServerProfile
import com.egor201.puffy.model.Subscription
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "puffy_store")

class AppRepository(private val context: Context) {

    private val subscriptionsKey = stringPreferencesKey("subscriptions_json")
    private val serversKey = stringPreferencesKey("servers_json")
    private val activeServerKey = stringPreferencesKey("active_server_id")

    val subscriptions: Flow<List<Subscription>> =
        context.dataStore.data.map { decodeSubscriptions(it[subscriptionsKey] ?: "[]") }

    val servers: Flow<List<ServerProfile>> =
        context.dataStore.data.map { decodeServers(it[serversKey] ?: "[]") }

    val activeServerId: Flow<String?> =
        context.dataStore.data.map { it[activeServerKey] }

    suspend fun addStandaloneServer(profile: ServerProfile) {
        context.dataStore.edit { prefs ->
            val current = decodeServers(prefs[serversKey] ?: "[]").toMutableList()
            current.add(profile)
            prefs[serversKey] = encodeServers(current)
        }
    }

    suspend fun addSubscription(subscription: Subscription, fetchedServers: List<ServerProfile>) {
        context.dataStore.edit { prefs ->
            val subs = decodeSubscriptions(prefs[subscriptionsKey] ?: "[]").toMutableList()
            subs.add(subscription)
            prefs[subscriptionsKey] = encodeSubscriptions(subs)

            val servers = decodeServers(prefs[serversKey] ?: "[]").toMutableList()
            servers.addAll(fetchedServers)
            prefs[serversKey] = encodeServers(servers)
        }
    }

    suspend fun refreshSubscription(subscriptionId: String, updated: Subscription, fetchedServers: List<ServerProfile>) {
        context.dataStore.edit { prefs ->
            val subs = decodeSubscriptions(prefs[subscriptionsKey] ?: "[]")
                .map { if (it.id == subscriptionId) updated else it }
            prefs[subscriptionsKey] = encodeSubscriptions(subs)

            val servers = decodeServers(prefs[serversKey] ?: "[]")
                .filterNot { it.subscriptionId == subscriptionId }
                .toMutableList()
            servers.addAll(fetchedServers)
            prefs[serversKey] = encodeServers(servers)
        }
    }

    suspend fun removeSubscription(subscriptionId: String) {
        context.dataStore.edit { prefs ->
            val subs = decodeSubscriptions(prefs[subscriptionsKey] ?: "[]").filterNot { it.id == subscriptionId }
            prefs[subscriptionsKey] = encodeSubscriptions(subs)

            val servers = decodeServers(prefs[serversKey] ?: "[]").filterNot { it.subscriptionId == subscriptionId }
            prefs[serversKey] = encodeServers(servers)
        }
    }

    suspend fun removeServer(serverId: String) {
        context.dataStore.edit { prefs ->
            val servers = decodeServers(prefs[serversKey] ?: "[]").filterNot { it.id == serverId }
            prefs[serversKey] = encodeServers(servers)
        }
    }

    suspend fun setActiveServer(id: String) {
        context.dataStore.edit { it[activeServerKey] = id }
    }

    suspend fun updateServerPing(serverId: String, pingMs: Int?) {
        context.dataStore.edit { prefs ->
            val servers = decodeServers(prefs[serversKey] ?: "[]").map {
                if (it.id == serverId) it.copy(pingMs = pingMs) else it
            }
            prefs[serversKey] = encodeServers(servers)
        }
    }

    fun newId(): String = UUID.randomUUID().toString()

    private fun encodeServers(list: List<ServerProfile>): String {
        val array = JSONArray()
        list.forEach { p ->
            array.put(
                JSONObject()
                    .put("id", p.id)
                    .put("subscriptionId", p.subscriptionId ?: JSONObject.NULL)
                    .put("name", p.name)
                    .put("address", p.address)
                    .put("port", p.port)
                    .put("uuid", p.uuid)
                    .put("encryption", p.encryption)
                    .put("flow", p.flow)
                    .put("network", p.network)
                    .put("security", p.security)
                    .put("sni", p.sni)
                    .put("fingerprint", p.fingerprint)
                    .put("publicKey", p.publicKey)
                    .put("shortId", p.shortId)
                    .put("spiderX", p.spiderX)
                    .put("path", p.path)
                    .put("host", p.host)
                    .put("headerType", p.headerType)
                    .put("alpn", p.alpn)
                    .put("allowInsecure", p.allowInsecure)
                    .put("pingMs", p.pingMs ?: JSONObject.NULL)
                    .put("rawLink", p.rawLink)
            )
        }
        return array.toString()
    }

    private fun decodeServers(raw: String): List<ServerProfile> {
        val array = JSONArray(raw)
        val result = mutableListOf<ServerProfile>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            result.add(
                ServerProfile(
                    id = o.getString("id"),
                    subscriptionId = o.optString("subscriptionId", "").ifBlank { null },
                    name = o.getString("name"),
                    address = o.getString("address"),
                    port = o.getInt("port"),
                    uuid = o.getString("uuid"),
                    encryption = o.optString("encryption", "none"),
                    flow = o.optString("flow", ""),
                    network = o.optString("network", "tcp"),
                    security = o.optString("security", "none"),
                    sni = o.optString("sni", ""),
                    fingerprint = o.optString("fingerprint", ""),
                    publicKey = o.optString("publicKey", ""),
                    shortId = o.optString("shortId", ""),
                    spiderX = o.optString("spiderX", ""),
                    path = o.optString("path", ""),
                    host = o.optString("host", ""),
                    headerType = o.optString("headerType", "none"),
                    alpn = o.optString("alpn", ""),
                    allowInsecure = o.optBoolean("allowInsecure", false),
                    pingMs = if (o.isNull("pingMs")) null else o.optInt("pingMs"),
                    rawLink = o.optString("rawLink", "")
                )
            )
        }
        return result
    }

    private fun encodeSubscriptions(list: List<Subscription>): String {
        val array = JSONArray()
        list.forEach { s ->
            array.put(
                JSONObject()
                    .put("id", s.id)
                    .put("name", s.name)
                    .put("url", s.url)
                    .put("lastUpdatedAt", s.lastUpdatedAt)
                    .put("updateIntervalHours", s.updateIntervalHours)
                    .put("trafficUsedBytes", s.trafficUsedBytes ?: JSONObject.NULL)
                    .put("trafficTotalBytes", s.trafficTotalBytes ?: JSONObject.NULL)
                    .put("expiresAtSeconds", s.expiresAtSeconds ?: JSONObject.NULL)
            )
        }
        return array.toString()
    }

    private fun decodeSubscriptions(raw: String): List<Subscription> {
        val array = JSONArray(raw)
        val result = mutableListOf<Subscription>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            result.add(
                Subscription(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    url = o.getString("url"),
                    lastUpdatedAt = o.optLong("lastUpdatedAt", 0L),
                    updateIntervalHours = o.optInt("updateIntervalHours", 0),
                    trafficUsedBytes = if (o.isNull("trafficUsedBytes")) null else o.optLong("trafficUsedBytes"),
                    trafficTotalBytes = if (o.isNull("trafficTotalBytes")) null else o.optLong("trafficTotalBytes"),
                    expiresAtSeconds = if (o.isNull("expiresAtSeconds")) null else o.optLong("expiresAtSeconds")
                )
            )
        }
        return result
    }
}
