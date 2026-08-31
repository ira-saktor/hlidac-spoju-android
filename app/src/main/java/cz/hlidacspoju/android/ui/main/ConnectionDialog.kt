package cz.hlidacspoju.android.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.hlidacspoju.android.model.PidStopGroup
import cz.hlidacspoju.android.model.PidStopsDocument
import cz.hlidacspoju.android.model.WatchedConnection
import cz.hlidacspoju.android.ui.AppViewModel
import cz.hlidacspoju.android.ui.LocalStrings
import cz.hlidacspoju.android.ui.Strings
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Dialog for creating or editing a [WatchedConnection]. Stop names, lines and directions are
 * looked up against the public PID stop register via [AppViewModel] so the user picks from
 * suggestions instead of typing exact names.
 */
private fun dayLabel(strings: Strings, day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> strings("day_mon")
    DayOfWeek.TUESDAY -> strings("day_tue")
    DayOfWeek.WEDNESDAY -> strings("day_wed")
    DayOfWeek.THURSDAY -> strings("day_thu")
    DayOfWeek.FRIDAY -> strings("day_fri")
    DayOfWeek.SATURDAY -> strings("day_sat")
    DayOfWeek.SUNDAY -> strings("day_sun")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConnectionDialog(
    viewModel: AppViewModel,
    existing: WatchedConnection?,
    onDismiss: () -> Unit,
    onSave: (WatchedConnection) -> Unit
) {
    val strings = LocalStrings.current
    var stopsDocument by remember { mutableStateOf<PidStopsDocument?>(null) }
    LaunchedEffect(Unit) {
        stopsDocument = runCatching { viewModel.loadPidStops() }.getOrNull()
    }

    var stopQuery by remember { mutableStateOf(existing?.stopName ?: "") }
    var selectedStopGroup by remember { mutableStateOf<PidStopGroup?>(null) }
    var stopMenuExpanded by remember { mutableStateOf(false) }

    var lineName by remember { mutableStateOf(existing?.lineName ?: "") }
    var lineMenuExpanded by remember { mutableStateOf(false) }

    var direction by remember { mutableStateOf(existing?.direction ?: "") }
    var directionMenuExpanded by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var timeFrom by remember { mutableStateOf(existing?.timeFrom?.toString() ?: "08:00") }
    var timeTo by remember { mutableStateOf(existing?.timeTo?.toString() ?: "09:00") }
    val selectedDays = remember {
        mutableStateOf(existing?.days ?: setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY))
    }

    val stopSuggestions = stopsDocument?.let { doc ->
        if (selectedStopGroup?.name == stopQuery) emptyList()
        else viewModel.searchStopGroups(doc, stopQuery).take(20)
    } ?: emptyList()

    val lineNames = selectedStopGroup?.stops
        ?.flatMap { it.lines }
        ?.map { it.name }
        ?.distinct()
        ?.sorted()
        ?: emptyList()

    val directions = selectedStopGroup?.stops
        ?.flatMap { it.lines }
        ?.filter { it.name == lineName }
        ?.flatMap { listOfNotNull(it.direction, it.direction2) }
        ?.filter { it.isNotBlank() }
        ?.distinct()
        ?: emptyList()

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = false),
        title = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = strings("back"))
                }
                Text(if (existing == null) strings("add_connection") else strings("edit_connection"))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(expanded = stopMenuExpanded && stopSuggestions.isNotEmpty(), onExpandedChange = { stopMenuExpanded = it }) {
                    OutlinedTextField(
                        value = stopQuery,
                        onValueChange = {
                            stopQuery = it
                            selectedStopGroup = null
                            lineName = ""
                            direction = ""
                            stopMenuExpanded = true
                        },
                        label = { Text(strings("stop")) },
                        modifier = Modifier.menuAnchor()
                    )
                    DropdownMenu(
                        expanded = stopMenuExpanded && stopSuggestions.isNotEmpty(),
                        onDismissRequest = { stopMenuExpanded = false },
                        properties = androidx.compose.ui.window.PopupProperties(focusable = false),
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        stopSuggestions.forEach { group ->
                            DropdownMenuItem(text = { Text(group.name) }, onClick = {
                                selectedStopGroup = group
                                stopQuery = group.name
                                lineName = ""
                                direction = ""
                                stopMenuExpanded = false
                            })
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = lineMenuExpanded && lineNames.isNotEmpty(), onExpandedChange = { if (lineNames.isNotEmpty()) lineMenuExpanded = it }) {
                    OutlinedTextField(
                        value = lineName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings("line")) },
                        modifier = Modifier.menuAnchor()
                    )
                    DropdownMenu(
                        expanded = lineMenuExpanded && lineNames.isNotEmpty(),
                        onDismissRequest = { lineMenuExpanded = false },
                        properties = androidx.compose.ui.window.PopupProperties(focusable = false),
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        lineNames.forEach { candidate ->
                            DropdownMenuItem(text = { Text(candidate) }, onClick = {
                                lineName = candidate
                                direction = ""
                                lineMenuExpanded = false
                            })
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = directionMenuExpanded && directions.isNotEmpty(), onExpandedChange = { if (directions.isNotEmpty()) directionMenuExpanded = it }) {
                    OutlinedTextField(
                        value = direction,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings("direction")) },
                        modifier = Modifier.menuAnchor()
                    )
                    DropdownMenu(
                        expanded = directionMenuExpanded && directions.isNotEmpty(),
                        onDismissRequest = { directionMenuExpanded = false },
                        properties = androidx.compose.ui.window.PopupProperties(focusable = false),
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        directions.forEach { candidate ->
                            DropdownMenuItem(text = { Text(candidate) }, onClick = {
                                direction = candidate
                                directionMenuExpanded = false
                            })
                        }
                    }
                }

                OutlinedTextField(value = timeFrom, onValueChange = { timeFrom = it }, label = { Text(strings("time_from")) })
                OutlinedTextField(value = timeTo, onValueChange = { timeTo = it }, label = { Text(strings("time_to")) })
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings("custom_name")) })
                Text(strings("days_of_week"))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        val checked = selectedDays.value.contains(day)
                        FilterChip(
                            selected = checked,
                            onClick = {
                                selectedDays.value = if (checked) selectedDays.value - day else selectedDays.value + day
                            },
                            label = { Text(dayLabel(strings, day)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedStopGroup != null && lineName.isNotBlank() && direction.isNotBlank(),
                onClick = {
                    val stopGroup = selectedStopGroup ?: return@TextButton
                    val parsedFrom = runCatching { LocalTime.parse(timeFrom) }.getOrDefault(LocalTime.of(8, 0))
                    val parsedTo = runCatching { LocalTime.parse(timeTo) }.getOrDefault(LocalTime.of(9, 0))
                    val gtfsStopIds = stopGroup.stops
                        .filter { stop -> stop.lines.any { it.name == lineName } }
                        .flatMap { it.gtfsIds }
                        .distinct()
                    onSave(
                        (existing ?: WatchedConnection()).copy(
                            stopName = stopGroup.name,
                            lineName = lineName,
                            direction = direction,
                            gtfsStopIds = gtfsStopIds,
                            name = name.ifBlank { "$lineName ${stopGroup.name} → $direction" },
                            timeFrom = parsedFrom,
                            timeTo = parsedTo,
                            days = selectedDays.value
                        )
                    )
                }
            ) {
                Text(strings("save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings("cancel"))
            }
        }
    )
}
