package com.egor201.puffy.ui

// Floating bottom navigation — Home / Servers / Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egor201.puffy.ui.theme.BgElevated
import com.egor201.puffy.ui.theme.Cyan
import com.egor201.puffy.ui.theme.TextMuted

enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("Главная", Icons.Default.Home),
    SERVERS("Сервера", Icons.Default.Dns),
    SETTINGS("Настройки", Icons.Default.Settings)
}

@Composable
fun BottomNavBar(selected: AppTab, onSelect: (AppTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(BgElevated)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        AppTab.values().forEach { tab ->
            val color by animateColorAsState(if (tab == selected) Cyan else TextMuted, label = "tabColor")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelect(tab) }
                    .padding(vertical = 6.dp)
            ) {
                Icon(tab.icon, contentDescription = tab.label, tint = color, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(tab.label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
