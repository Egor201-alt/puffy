package com.egor201.puffy.ui

// Settings — subscriptions management, auto-update, about
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.egor201.puffy.model.Subscription
import com.egor201.puffy.ui.theme.OceanBackdrop
import com.egor201.puffy.ui.theme.TextPrimary
import com.egor201.puffy.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    subscriptions: List<Subscription>,
    autoUpdateEnabled: Boolean,
    onAutoUpdateChanged: (Boolean) -> Unit,
    onRemoveSubscription: (Subscription) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit
) {
    OceanBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                item {
                    ListItem(
                        headlineContent = { Text("Auto-update subscriptions", color = TextPrimary) },
                        supportingContent = { Text("Refresh on app launch if interval elapsed", color = TextSecondary) },
                        trailingContent = {
                            Switch(checked = autoUpdateEnabled, onCheckedChange = onAutoUpdateChanged)
                        }
                    )
                    HorizontalDivider()
                }

                if (subscriptions.isNotEmpty()) {
                    item {
                        Text(
                            "Subscriptions",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp)
                        )
                    }
                    items(subscriptions) { sub ->
                        ListItem(
                            headlineContent = { Text(sub.name, color = TextPrimary) },
                            supportingContent = { Text(sub.url, color = TextSecondary) },
                            trailingContent = {
                                IconButton(onClick = { onRemoveSubscription(sub) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = TextSecondary)
                                }
                            }
                        )
                    }
                    item { HorizontalDivider() }
                }

                item {
                    ListItem(
                        headlineContent = { Text("Clear all data", color = MaterialTheme.colorScheme.error) },
                        supportingContent = { Text("Removes every subscription and server", color = TextSecondary) },
                        modifier = Modifier.clickable(onClick = onClearAll)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Puffy 0.1.0 — VLESS/Xray client",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
