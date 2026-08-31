package cz.hlidacspoju.android.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.hlidacspoju.android.model.WatchedConnection
import cz.hlidacspoju.android.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AppViewModel, onOpenSettings: () -> Unit) {
    val settings by viewModel.settings.collectAsState()
    val statusByConnectionId by viewModel.statusByConnectionId.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hlídač spojů") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Nastavení")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Přidat spoj")
            }
        }
    ) { padding ->
        if (settings.watchedConnections.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text("Zatím nemáte žádné sledované spoje.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(settings.watchedConnections, key = { it.id }) { connection ->
                    ConnectionRow(
                        connection = connection,
                        status = statusByConnectionId[connection.id],
                        onToggle = { enabled -> viewModel.setConnectionEnabled(connection.id, enabled) },
                        onDelete = { viewModel.removeConnection(connection.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        ConnectionDialog(
            existing = null,
            onDismiss = { showAddDialog = false },
            onSave = { connection ->
                viewModel.addOrUpdateConnection(connection)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ConnectionRow(
    connection: WatchedConnection,
    status: cz.hlidacspoju.android.service.ConnectionStatusUpdate?,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(connection.name.ifBlank { "${connection.lineName} ${connection.stopName} → ${connection.direction}" })
            val statusText = when {
                status == null || !status.isScheduledToday -> "Na dnešek není nastaveno hlídání spoje"
                status.delayedCount == 0 && status.onTimeCount == 0 && status.notDepartedCount == 0 -> "Žádné aktuální spoje v okně"
                else -> "Zpožděno: ${status.delayedCount}, na čas: ${status.onTimeCount}"
            }
            Text(statusText, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
        Row {
            Switch(checked = connection.isEnabled, onCheckedChange = onToggle)
            IconButton(onClick = onDelete) {
                Icon(androidx.compose.material.icons.Icons.Filled.Delete, contentDescription = "Smazat")
            }
        }
    }
}
