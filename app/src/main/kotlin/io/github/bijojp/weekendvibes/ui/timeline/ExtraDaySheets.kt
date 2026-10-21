package io.github.bijojp.weekendvibes.ui.timeline

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import io.github.bijojp.weekendvibes.ui.components.ConfirmDialog
import io.github.bijojp.weekendvibes.ui.components.DatePickerDialog
import io.github.bijojp.weekendvibes.ui.components.FieldCard
import io.github.bijojp.weekendvibes.ui.components.Pill
import io.github.bijojp.weekendvibes.ui.components.AccentButton
import io.github.bijojp.weekendvibes.ui.components.VibeSheet
import io.github.bijojp.weekendvibes.ui.components.VibeTextField
import io.github.bijojp.weekendvibes.ui.components.longDay
import io.github.bijojp.weekendvibes.ui.theme.Vibes
import java.time.LocalDate

/** Marks a weekday as an Extra day, or renames [existing]. */
@Composable
fun ExtraDaySheet(
    existing: ExtraDay?,
    agenda: Agenda,
    today: LocalDate,
    onSave: (LocalDate, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = Vibes.colors
    var date by remember { mutableStateOf(existing?.date) }
    var label by remember { mutableStateOf(existing?.label.orEmpty()) }
    var picking by remember { mutableStateOf(false) }

    VibeSheet(
        title = stringResource(if (existing == null) R.string.extra_day_add_title else R.string.extra_day_edit_title),
        onDismiss = onDismiss,
    ) {
        if (existing == null) {
            Pill(
                text = date?.let(::longDay) ?: stringResource(R.string.extra_day_pick_date),
                icon = Icons.Rounded.CalendarMonth,
                selected = date != null,
                accent = colors.extra,
                onClick = { picking = true },
            )
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.extra_day_help), style = MaterialTheme.typography.bodySmall, color = colors.textFaint)
        } else {
            Text(longDay(existing.date), style = MaterialTheme.typography.titleMedium, color = colors.extra)
        }
        Spacer(Modifier.height(16.dp))
        FieldCard {
            VibeTextField(
                value = label,
                onValueChange = { label = it.take(Limits.LABEL) },
                placeholder = stringResource(R.string.field_label_hint),
                label = stringResource(R.string.field_label),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
        }
        Spacer(Modifier.height(24.dp))
        AccentButton(
            text = stringResource(R.string.action_save),
            enabled = date != null,
            onClick = {
                date?.let { onSave(it, label) }
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (picking) {
        DatePickerDialog(
            initial = date ?: generateSequence(today) { it.plusDays(1) }.first { !it.isWeekendDay && agenda.extraDay(it) == null },
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
    ConfirmDialog(
        title = stringResource(R.string.extra_day_remove_title),
        body = if (count == 0) {
            stringResource(R.string.extra_day_remove_body, name)
        } else {
            pluralStringResource(R.plurals.extra_day_remove_with_activities, count, name, count)
        },
        confirm = stringResource(R.string.action_remove),
        onConfirm = onRemove,
        onDismiss = onDismiss,
    )
}
