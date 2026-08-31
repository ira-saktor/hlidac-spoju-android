package cz.hlidacspoju.android.ui.main

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import cz.hlidacspoju.android.ui.LocalStrings
import cz.hlidacspoju.android.ui.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AppViewModel, onOpenSettings: () -> Unit) {
    val settings by viewModel.settings.collectAsState()
    val statusByConnectionId by viewModel.statusByConnectionId.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingConnection by remember { mutableStateOf<WatchedConnection?>(null) }
    var detailConnectionId by remember { mutableStateOf<String?>(null) }
    val strings = LocalStrings.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings("app_title")) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = strings("settings"))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = strings("add_connection"))
            }
        }
    ) { padding ->
        if (settings.watchedConnections.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text(strings("no_connections"))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(settings.watchedConnections, key = { it.id }) { connection ->
                    ConnectionRow(
                        connection = connection,
                        status = statusByConnectionId[connection.id],
                        onToggle = { enabled -> viewModel.setConnectionEnabled(connection.id, enabled) },
                        onDelete = { viewModel.removeConnection(connection.id) },
                        onClick = { detailConnectionId = connection.id }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        ConnectionDialog(
            viewModel = viewModel,
            existing = null,
            onDismiss = { showAddDialog = false },
            onSave = { connection ->
                viewModel.addOrUpdateConnection(connection)
                showAddDialog = false
            }
        )
    }

    if (editingConnection != null) {
        ConnectionDialog(
            viewModel = viewModel,
            existing = editingConnection,
            onDismiss = { editingConnection = null },
            onSave = { connection ->
                viewModel.addOrUpdateConnection(connection)
                editingConnection = null
            }
        )
    }

    val detailConnection = settings.watchedConnections.find { it.id == detailConnectionId }
    if (detailConnection != null) {
        ConnectionDetailDialog(
            connection = detailConnection,
            status = statusByConnectionId[detailConnection.id],
            onDismiss = { detailConnectionId = null },
            onEdit = {
                editingConnection = detailConnection
                detailConnectionId = null
            }
        )
    }
}

@Composable
private fun ConnectionDetailDialog(
    connection: WatchedConnection,
    status: cz.hlidacspoju.android.service.ConnectionStatusUpdate?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(connection.name.ifBlank { "${connection.lineName} ${connection.stopName} → ${connection.direction}" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${strings("line")}: ${connection.lineName}")
                Text("${strings("stop")}: ${connection.stopName}")
                Text("${strings("direction")}: ${connection.direction}")
                Text("${strings("time_from")}: ${connection.timeFrom}")
                Text("${strings("time_to")}: ${connection.timeTo}")
                Text(
                    "${strings("days_of_week")}: " +
                        connection.days.sortedBy { it.value }.joinToString(", ") { dayLabel(strings, it) }
                )
                when {
                    status == null || !status.isScheduledToday -> Text(strings("not_scheduled_today"))
                    status.windowPassedToday -> Text(strings("window_passed_today"))
                    else -> {
                        Text(strings.get("delayed_on_time", status.delayedCount, status.onTimeCount))
                        Text("${strings("not_departed")}: ${status.notDepartedCount}")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onEdit) { Text(strings("edit")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings("close")) }
        }
    )
}

private fun dayLabel(strings: Strings, day: java.time.DayOfWeek): String = when (day) {
    java.time.DayOfWeek.MONDAY -> strings("day_mon")
    java.time.DayOfWeek.TUESDAY -> strings("day_tue")
    java.time.DayOfWeek.WEDNESDAY -> strings("day_wed")
    java.time.DayOfWeek.THURSDAY -> strings("day_thu")
    java.time.DayOfWeek.FRIDAY -> strings("day_fri")
    java.time.DayOfWeek.SATURDAY -> strings("day_sat")
    java.time.DayOfWeek.SUNDAY -> strings("day_sun")
}

@Composable
private fun ConnectionRow(
    connection: WatchedConnection,
    status: cz.hlidacspoju.android.service.ConnectionStatusUpdate?,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(connection.name.ifBlank { "${connection.lineName} ${connection.stopName} → ${connection.direction}" })
            val statusText = when {
                status == null || !status.isScheduledToday -> strings("not_scheduled_today")
                status.windowPassedToday -> strings("window_passed_today")
                status.delayedCount == 0 && status.onTimeCount == 0 && status.notDepartedCount == 0 -> strings("no_current_departures")
                else -> strings.get("delayed_on_time", status.delayedCount, status.onTimeCount)
            }
            Text(statusText, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
        Row {
            Switch(checked = connection.isEnabled, onCheckedChange = onToggle)
            IconButton(onClick = onDelete) {
                Icon(androidx.compose.material.icons.Icons.Filled.Delete, contentDescription = strings("delete"))
            }
        }
    }
}
