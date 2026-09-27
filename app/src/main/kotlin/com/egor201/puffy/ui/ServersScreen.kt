package com.egor201.puffy.ui

// Servers tab — subscription cards + standalone servers, quick-action bar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.egor201.puffy.model.ServerProfile
import com.egor201.puffy.model.Subscription
import com.egor201.puffy.ui.theme.CardBorder
import com.egor201.puffy.ui.theme.CardSurface
import com.egor201.puffy.ui.theme.Cyan
import com.egor201.puffy.ui.theme.TextMuted
import com.egor201.puffy.ui.theme.TextPrimary
import com.egor201.puffy.ui.theme.TextSecondary
import com.egor201.puffy.ui.theme.pingColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ServersScreen(
    subscriptions: List<Subscription>,
    servers: List<ServerProfile>,
    activeServerId: String?,
    onSelectServer: (ServerProfile) -> Unit,
    onRefreshSubscription: (Subscription) -> Unit,
    onPingServer: (ServerProfile) -> Unit,
    onAddClick: () -> Unit,
    onPasteClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Сервера",
            color = TextPrimary,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(subscriptions) { sub ->
                SubscriptionCard(
                    subscription = sub,
                    servers = servers.filter { it.subscriptionId == sub.id },
                    activeServerId = activeServerId,
                    onSelectServer = onSelectServer,
                    onRefresh = { onRefreshSubscription(sub) },
                    onPingServer = onPingServer
                )
            }

            val standalone = servers.filter { it.subscriptionId == null }
            if (standalone.isNotEmpty()) {
                item {
                    Text(
                        "Ручные серверы",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(standalone) { server ->
                    ServerRow(
                        server = server,
                        isActive = server.id == activeServerId,
                        onClick = { onSelectServer(server) },
                        onPing = { onPingServer(server) }
                    )
                }
            }

            if (subscriptions.isEmpty() && standalone.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Text("Пока нет серверов — добавь снизу", color = TextSecondary)
                    }
                }
            }
        }

        QuickActionBar(onAddClick = onAddClick, onPasteClick = onPasteClick)
    }
}

@Composable
private fun QuickActionBar(onAddClick: () -> Unit, onPasteClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CardSurface),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickAction(Icons.Default.Add, "Добавить", Modifier.weight(1f), onClick = onAddClick)
        QuickAction(Icons.Default.ContentPaste, "Вставить", Modifier.weight(1f), onClick = onPasteClick)
        QuickAction(Icons.Default.QrCodeScanner, "QR", Modifier.weight(1f), enabled = false) {}
    }
}

@Composable
private fun QuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val tint = if (enabled) Cyan else TextMuted
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = tint, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SubscriptionCard(
    subscription: Subscription,
    servers: List<ServerProfile>,
    activeServerId: String?,
    onSelectServer: (ServerProfile) -> Unit,
    onRefresh: () -> Unit,
    onPingServer: (ServerProfile) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardSurface)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle",
                    tint = TextSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(subscription.name, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (subscription.updateIntervalHours > 0) {
                    Text("${subscription.updateIntervalHours}ч", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (subscription.trafficTotalBytes != null || subscription.expiresAtSeconds != null) {
            Spacer(modifier = Modifier.height(8.dp))
            TrafficBar(subscription)
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            servers.forEach { server ->
                ServerRow(
                    server = server,
                    isActive = server.id == activeServerId,
                    onClick = { onSelectServer(server) },
                    onPing = { onPingServer(server) }
                )
            }
        }
    }
}

@Composable
private fun TrafficBar(sub: Subscription) {
    val used = sub.trafficUsedBytes ?: 0L
    val total = sub.trafficTotalBytes
    val progress = if (total != null && total > 0) (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

    Column {
        if (total != null) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = Cyan,
                trackColor = CardBorder
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = if (total != null) "${formatBytes(used)} / ${formatBytes(total)}" else "",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
            if (sub.expiresAtSeconds != null) {
                val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(sub.expiresAtSeconds * 1000))
                Text("до $date", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ServerRow(server: ServerProfile, isActive: Boolean, onClick: () -> Unit, onPing: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isActive) Cyan.copy(alpha = 0.10f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardBorder),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Public, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(server.name, color = TextPrimary, fontWeight = FontWeight.Medium)
                Text(
                    "VLESS · ${server.network.uppercase()} · ${server.security.uppercase()}",
                    color = Cyan.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onPing() }) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(pingColor(server.pingMs))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = server.pingMs?.let { "${it}ms" } ?: "n/a",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes / 1024.0 / 1024.0 / 1024.0
    return if (gb >= 1) "%.1f GB".format(gb) else "%.0f MB".format(bytes / 1024.0 / 1024.0)
}
