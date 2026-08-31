package cz.hlidacspoju.android.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import cz.hlidacspoju.android.model.WatchedConnection
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Simple dialog for creating or editing a [WatchedConnection]. Stop/line lookup against the PID
 * register is intentionally left as a follow-up; this captures the core fields by hand for now.
 */
@Composable
fun ConnectionDialog(
    existing: WatchedConnection?,
    onDismiss: () -> Unit,
    onSave: (WatchedConnection) -> Unit
) {
    var stopName by remember { mutableStateOf(existing?.stopName ?: "") }
    var lineName by remember { mutableStateOf(existing?.lineName ?: "") }
    var direction by remember { mutableStateOf(existing?.direction ?: "") }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var timeFrom by remember { mutableStateOf(existing?.timeFrom?.toString() ?: "08:00") }
    var timeTo by remember { mutableStateOf(existing?.timeTo?.toString() ?: "09:00") }
    val selectedDays = remember {
        mutableStateOf(existing?.days ?: setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Přidat spoj" else "Upravit spoj") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = stopName, onValueChange = { stopName = it }, label = { Text("Zastávka") })
                OutlinedTextField(value = lineName, onValueChange = { lineName = it }, label = { Text("Linka") })
                OutlinedTextField(value = direction, onValueChange = { direction = it }, label = { Text("Směr") })
                OutlinedTextField(value = timeFrom, onValueChange = { timeFrom = it }, label = { Text("Od (HH:mm)") })
                OutlinedTextField(value = timeTo, onValueChange = { timeTo = it }, label = { Text("Do (HH:mm)") })
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Vlastní název (nepovinné)") })
                Text("Dny v týdnu")
                Row {
                    DayOfWeek.entries.forEach { day ->
                        Row {
                            Checkbox(
                                checked = selectedDays.value.contains(day),
                                onCheckedChange = { checked ->
                                    selectedDays.value = if (checked) selectedDays.value + day else selectedDays.value - day
                                }
                            )
                            Text(day.name.take(2))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedFrom = runCatching { LocalTime.parse(timeFrom) }.getOrDefault(LocalTime.of(8, 0))
                val parsedTo = runCatching { LocalTime.parse(timeTo) }.getOrDefault(LocalTime.of(9, 0))
                onSave(
                    (existing ?: WatchedConnection()).copy(
                        stopName = stopName,
                        lineName = lineName,
                        direction = direction,
                        name = name.ifBlank { "$lineName $stopName → $direction" },
                        timeFrom = parsedFrom,
                        timeTo = parsedTo,
                        days = selectedDays.value
                    )
                )
            }) {
                Text("Uložit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit")
            }
        }
    )
}
