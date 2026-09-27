package com.egor201.puffy.ui

// Add — subscription URL or a single vless:// link
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.egor201.puffy.core.VlessUriParser
import com.egor201.puffy.model.ServerProfile
import com.egor201.puffy.ui.theme.OceanBackdrop
import com.egor201.puffy.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    isLoading: Boolean,
    error: String?,
    initialUrl: String = "",
    onBack: () -> Unit,
    onAddSubscription: (name: String, url: String) -> Unit,
    onAddServer: (ServerProfile) -> Unit
) {
    var tabIndex by remember { mutableStateOf(if (initialUrl.isNotBlank()) 0 else 0) }
    var subName by remember { mutableStateOf("") }
    var subUrl by remember { mutableStateOf(initialUrl) }
    var link by remember { mutableStateOf("") }
    var linkError by remember { mutableStateOf<String?>(null) }

    OceanBackdrop {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Add server") },
                    navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                )
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                TabRow(selectedTabIndex = tabIndex) {
                    Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Subscription") })
                    Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Single link") })
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (tabIndex == 0) {
                    OutlinedTextField(
                        value = subName,
                        onValueChange = { subName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = subUrl,
                        onValueChange = { subUrl = it },
                        label = { Text("Subscription URL") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    if (error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onAddSubscription(subName.ifBlank { "Subscription" }, subUrl.trim()) },
                        enabled = subUrl.isNotBlank() && !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Add subscription")
                        }
                    }
                } else {
                    Text("Paste a vless:// link", color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it; linkError = null },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        label = { Text("vless:// link") }
                    )
                    if (linkError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(linkError!!, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val parsed = VlessUriParser.parse(link)
                            if (parsed == null) linkError = "Could not parse this link" else onAddServer(parsed)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add server")
                    }
                }
            }
        }
    }
}
