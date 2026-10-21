package io.github.bijojp.weekendvibes.ui.timeline

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.bijojp.weekendvibes.R
import io.github.bijojp.weekendvibes.domain.Agenda
import io.github.bijojp.weekendvibes.domain.ExtraDay
import io.github.bijojp.weekendvibes.domain.FreeDay
import io.github.bijojp.weekendvibes.domain.Limits
import io.github.bijojp.weekendvibes.domain.isWeekendDay
import io.github.bijojp.weekendvibes.ui.components.DatePickerDialog
import io.github.bijojp.weekendvibes.ui.components.longDay
import java.time.LocalDate

/** Marks a weekday as an Extra day, or changes the label of [existing]. */
@Composable
fun ExtraDayDialog(
    existing: ExtraDay?,
    agenda: Agenda,
    onSave: (LocalDate, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var date by remember { mutableStateOf(existing?.date) }
    var label by remember { mutableStateOf(existing?.label.orEmpty()) }
    var picking by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.extra_day_add_title else R.string.extra_day_edit_title)) },
        text = {
            Column {
                if (existing == null) {
                    OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(date?.let(::longDay) ?: stringResource(R.string.extra_day_pick_date))
                    }
                } else {
                    Text(longDay(existing.date), style = MaterialTheme.typography.bodyLarge)
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(Limits.LABEL) },
                    label = { Text(stringResource(R.string.field_label)) },
                    placeholder = { Text(stringResource(R.string.field_label_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                if (existing == null) {
                    Text(
                        stringResource(R.string.extra_day_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    date?.let { onSave(it, label) }
                    onDismiss()
                },
                enabled = date != null,
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )

    if (picking) {
        DatePickerDialog(
            initial = date ?: LocalDate.now(),
            selectable = { !it.isWeekendDay && agenda.extraDay(it) == null },
            onPicked = { date = it },
            onDismiss = { picking = false },
        )
    }
}

/** Asks before unmarking an Extra day, since its Activities go with it. */
@Composable
fun RemoveExtraDayDialog(day: FreeDay, onRemove: () -> Unit, onDismiss: () -> Unit) {
    val name = day.extraDay?.label?.takeIf { it.isNotEmpty() }?.let { "$it (${longDay(day.date)})" } ?: longDay(day.date)
    val count = day.activities.size
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.extra_day_remove_title)) },
        text = {
            Text(
                if (count == 0) {
                    stringResource(R.string.extra_day_remove_body, name)
                } else {
                    pluralStringResource(R.plurals.extra_day_remove_with_activities, count, name, count)
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onRemove()
                    onDismiss()
                },
            ) { Text(stringResource(R.string.action_remove)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
