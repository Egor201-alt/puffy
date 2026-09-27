package com.egor201.puffy

// App entry point — bottom-tab navigation, VPN permission, orchestration
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.egor201.puffy.core.PingTester
import com.egor201.puffy.core.SubscriptionParser
import com.egor201.puffy.core.VlessUriParser
import com.egor201.puffy.core.VpnCoreService
import com.egor201.puffy.core.buildConnectIntent
import com.egor201.puffy.core.buildDisconnectIntent
import com.egor201.puffy.data.AppRepository
import com.egor201.puffy.model.ServerProfile
import com.egor201.puffy.model.Subscription
import com.egor201.puffy.net.SubscriptionFetcher
import com.egor201.puffy.ui.AddScreen
import com.egor201.puffy.ui.AppTab
import com.egor201.puffy.ui.BottomNavBar
import com.egor201.puffy.ui.HomeTab
import com.egor201.puffy.ui.ServersScreen
import com.egor201.puffy.ui.SettingsScreen
import com.egor201.puffy.ui.theme.OceanBackdrop
import com.egor201.puffy.ui.theme.PuffyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: AppRepository
    private var pendingServer: ServerProfile? = null

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val server = pendingServer
        pendingServer = null
        if (result.resultCode == RESULT_OK && server != null) {
            startVpn(server)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = AppRepository(applicationContext)

        setContent {
            PuffyTheme {
                var tab by remember { mutableStateOf(AppTab.HOME) }
                var showAddScreen by remember { mutableStateOf(false) }
                var prefillUrl by remember { mutableStateOf("") }
                var addError by remember { mutableStateOf<String?>(null) }
                var isAddingSubscription by remember { mutableStateOf(false) }
                var autoUpdateEnabled by remember { mutableStateOf(true) }
                var isConnecting by remember { mutableStateOf(false) }
                var connected by remember { mutableStateOf(VpnCoreService.isConnected) }

                val subscriptions by repository.subscriptions.collectAsStateWithLifecycle(initialValue = emptyList())
                val servers by repository.servers.collectAsStateWithLifecycle(initialValue = emptyList())
                val activeServerId by repository.activeServerId.collectAsStateWithLifecycle(initialValue = null)
                val activeServer = servers.firstOrNull { it.id == activeServerId }

                LaunchedEffect(Unit) {
                    connected = VpnCoreService.isConnected
                    if (autoUpdateEnabled) refreshDueSubscriptions(subscriptions)
                }

                LaunchedEffect(isConnecting) {
                    while (isConnecting) {
                        delay(400)
                        if (VpnCoreService.isConnected) {
                            connected = true
                            isConnecting = false
                        }
                    }
                }

                fun toggleConnection() {
                    if (connected) {
                        startService(buildDisconnectIntent(this@MainActivity))
                        connected = false
                        isConnecting = false
                    } else {
                        val server = activeServer ?: servers.firstOrNull()
                        if (server == null) {
                            tab = AppTab.SERVERS
                            return
                        }
                        lifecycleScope.launch { repository.setActiveServer(server.id) }
                        isConnecting = true
                        requestPermissionAndConnect(server)
                    }
                }

                fun handlePaste() {
                    val clip = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val text = clip.primaryClip?.getItemAt(0)?.coerceToText(this@MainActivity)?.toString()?.trim().orEmpty()
                    when {
                        text.startsWith("vless://") -> {
                            val parsed = VlessUriParser.parse(text)
                            if (parsed != null) {
                                lifecycleScope.launch { repository.addStandaloneServer(parsed) }
                                Toast.makeText(this@MainActivity, "Сервер добавлен", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@MainActivity, "Не удалось разобрать ссылку", Toast.LENGTH_SHORT).show()
                            }
                        }
                        text.startsWith("http://") || text.startsWith("https://") -> {
                            prefillUrl = text
                            addError = null
                            showAddScreen = true
                        }
                        else -> Toast.makeText(this@MainActivity, "В буфере нет ссылки", Toast.LENGTH_SHORT).show()
                    }
                }

                when {
                    showAddScreen -> AddScreen(
                        isLoading = isAddingSubscription,
                        error = addError,
                        initialUrl = prefillUrl,
                        onBack = { showAddScreen = false; prefillUrl = "" },
                        onAddSubscription = { name, url ->
                            lifecycleScope.launch {
                                isAddingSubscription = true
                                addError = null
                                val result = SubscriptionFetcher.fetch(url)
                                result.onSuccess { fetched ->
                                    val meta = SubscriptionParser.parseMeta(fetched.headers)
                                    val subscriptionId = repository.newId()
                                    val parsedServers = SubscriptionParser.parseServers(fetched.body, subscriptionId)
                                    if (parsedServers.isEmpty()) {
                                        addError = "В подписке не найдено vless-серверов"
                                    } else {
                                        repository.addSubscription(
                                            Subscription(
                                                id = subscriptionId,
                                                name = meta.title ?: name,
                                                url = url,
                                                lastUpdatedAt = System.currentTimeMillis(),
                                                updateIntervalHours = meta.updateIntervalHours ?: 0,
                                                trafficUsedBytes = meta.trafficUsedBytes,
                                                trafficTotalBytes = meta.trafficTotalBytes,
                                                expiresAtSeconds = meta.expiresAtSeconds
                                            ),
                                            parsedServers
                                        )
                                        showAddScreen = false
                                        prefillUrl = ""
                                        tab = AppTab.SERVERS
                                    }
                                }.onFailure {
                                    addError = "Не удалось загрузить подписку: ${it.message}"
                                }
                                isAddingSubscription = false
                            }
                        },
                        onAddServer = { server ->
                            lifecycleScope.launch {
                                repository.addStandaloneServer(server)
                                showAddScreen = false
                                tab = AppTab.SERVERS
                            }
                        }
                    )

                    else -> Box(modifier = Modifier.fillMaxSize()) {
                        OceanBackdrop {
                            Box(modifier = Modifier.fillMaxSize().padding(bottom = 88.dp)) {
                                when (tab) {
                                    AppTab.HOME -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        HomeTab(
                                            activeServer = activeServer,
                                            isConnected = connected,
                                            isConnecting = isConnecting,
                                            onToggleConnection = { toggleConnection() },
                                            onCheckPing = {
                                                activeServer?.let { s ->
                                                    lifecycleScope.launch {
                                                        val ms = PingTester.pingMs(s.address, s.port)
                                                        repository.updateServerPing(s.id, ms)
                                                    }
                                                }
                                            }
                                        )
                                    }

                                    AppTab.SERVERS -> ServersScreen(
                                        subscriptions = subscriptions,
                                        servers = servers,
                                        activeServerId = activeServerId,
                                        onSelectServer = { server ->
                                            lifecycleScope.launch { repository.setActiveServer(server.id) }
                                            if (connected) {
                                                startService(buildDisconnectIntent(this@MainActivity))
                                                connected = false
                                                isConnecting = true
                                                requestPermissionAndConnect(server)
                                            }
                                        },
                                        onRefreshSubscription = { sub -> lifecycleScope.launch { refreshSubscription(sub) } },
                                        onPingServer = { server ->
                                            lifecycleScope.launch {
                                                val ms = PingTester.pingMs(server.address, server.port)
                                                repository.updateServerPing(server.id, ms)
                                            }
                                        },
                                        onAddClick = { prefillUrl = ""; addError = null; showAddScreen = true },
                                        onPasteClick = { handlePaste() }
                                    )

                                    AppTab.SETTINGS -> SettingsScreen(
                                        subscriptions = subscriptions,
                                        autoUpdateEnabled = autoUpdateEnabled,
                                        onAutoUpdateChanged = { autoUpdateEnabled = it },
                                        onRemoveSubscription = { sub -> lifecycleScope.launch { repository.removeSubscription(sub.id) } },
                                        onClearAll = {
                                            lifecycleScope.launch {
                                                subscriptions.forEach { repository.removeSubscription(it.id) }
                                                servers.filter { it.subscriptionId == null }.forEach { repository.removeServer(it.id) }
                                            }
                                        },
                                        onBack = { tab = AppTab.HOME }
                                    )
                                }
                            }

                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                                BottomNavBar(selected = tab, onSelect = { tab = it })
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun refreshSubscription(sub: Subscription) {
        val result = SubscriptionFetcher.fetch(sub.url)
        result.onSuccess { fetched ->
            val meta = SubscriptionParser.parseMeta(fetched.headers)
            val parsedServers = SubscriptionParser.parseServers(fetched.body, sub.id)
            repository.refreshSubscription(
                sub.id,
                sub.copy(
                    name = meta.title ?: sub.name,
                    lastUpdatedAt = System.currentTimeMillis(),
                    updateIntervalHours = meta.updateIntervalHours ?: sub.updateIntervalHours,
                    trafficUsedBytes = meta.trafficUsedBytes ?: sub.trafficUsedBytes,
                    trafficTotalBytes = meta.trafficTotalBytes ?: sub.trafficTotalBytes,
                    expiresAtSeconds = meta.expiresAtSeconds ?: sub.expiresAtSeconds
                ),
                parsedServers
            )
        }
    }

    private suspend fun refreshDueSubscriptions(subscriptions: List<Subscription>) {
        val now = System.currentTimeMillis()
        subscriptions.forEach { sub ->
            if (sub.updateIntervalHours <= 0) return@forEach
            val dueAt = sub.lastUpdatedAt + sub.updateIntervalHours * 3_600_000L
            if (now >= dueAt) refreshSubscription(sub)
        }
    }

    private fun requestPermissionAndConnect(server: ServerProfile) {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            pendingServer = server
            vpnPermissionLauncher.launch(intent)
        } else {
            startVpn(server)
        }
    }

    private fun startVpn(server: ServerProfile) {
        startService(buildConnectIntent(this, server))
    }
}
