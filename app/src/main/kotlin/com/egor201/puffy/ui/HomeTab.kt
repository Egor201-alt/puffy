package com.egor201.puffy.ui

// Home tab — speed/timer stats, power button, active server
import android.net.TrafficStats
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.egor201.puffy.model.ServerProfile
import com.egor201.puffy.ui.theme.Cyan
import com.egor201.puffy.ui.theme.TextMuted
import com.egor201.puffy.ui.theme.TextPrimary
import com.egor201.puffy.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun HomeTab(
    activeServer: ServerProfile?,
    isConnected: Boolean,
    isConnecting: Boolean,
    onToggleConnection: () -> Unit,
    onCheckPing: () -> Unit
) {
    var elapsedSeconds by remember { mutableStateOf(0) }
    var rxSpeed by remember { mutableStateOf(0L) }
    var txSpeed by remember { mutableStateOf(0L) }

    LaunchedEffect(isConnected) {
        elapsedSeconds = 0
        var lastRx = TrafficStats.getTotalRxBytes()
        var lastTx = TrafficStats.getTotalTxBytes()
        while (isConnected) {
            delay(1000)
            elapsedSeconds++
            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            if (lastRx >= 0 && rx >= lastRx) rxSpeed = rx - lastRx
            if (lastTx >= 0 && tx >= lastTx) txSpeed = tx - lastTx
            lastRx = rx
            lastTx = tx
        }
        rxSpeed = 0
        txSpeed = 0
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isConnected) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SpeedStat(Icons.Default.ArrowUpward, formatSpeed(txSpeed))
                Text(formatElapsed(elapsedSeconds), color = TextSecondary, fontWeight = FontWeight.Medium)
                SpeedStat(Icons.Default.ArrowDownward, formatSpeed(rxSpeed))
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Spacer(modifier = Modifier.height(40.dp))
        }

        PowerButton(isConnected = isConnected, isConnecting = isConnecting, onClick = onToggleConnection)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = activeServer?.name ?: "Сервер не выбран",
            color = if (activeServer != null) Cyan else TextMuted,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = when {
                isConnecting -> "Подключение…"
                isConnected -> "Подключено"
                else -> "Нажмите для подключения"
            },
            color = TextSecondary,
            modifier = Modifier.padding(top = 2.dp)
        )

        if (activeServer != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Проверить",
                color = Cyan,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onCheckPing)
            )
        }
    }
}

@Composable
private fun SpeedStat(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(value, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    val kb = bytesPerSec / 1024.0
    return if (kb >= 1024) "%.1f MB/s".format(kb / 1024) else "%.0f KB/s".format(kb)
}

private fun formatElapsed(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
