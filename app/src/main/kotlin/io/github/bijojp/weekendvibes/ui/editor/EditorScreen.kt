package io.github.bijojp.weekendvibes.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bijojp.weekendvibes.R
import io.github.bijojp.weekendvibes.domain.ActivityId
import io.github.bijojp.weekendvibes.domain.Limits
import io.github.bijojp.weekendvibes.domain.isWeekendDay
import io.github.bijojp.weekendvibes.ui.EditorRoute
import io.github.bijojp.weekendvibes.ui.Navigator
import io.github.bijojp.weekendvibes.ui.WeekendVibesViewModel
import io.github.bijojp.weekendvibes.ui.components.BackButton
import io.github.bijojp.weekendvibes.ui.components.DatePickerDialog
import io.github.bijojp.weekendvibes.ui.components.HelpText
import io.github.bijojp.weekendvibes.ui.components.SectionHeader
import io.github.bijojp.weekendvibes.ui.components.TimePickerDialog
import io.github.bijojp.weekendvibes.ui.components.longDay
import io.github.bijojp.weekendvibes.ui.components.mediumDay
import io.github.bijojp.weekendvibes.ui.components.timeText
import java.time.LocalTime

private enum class Dialog { Date, Time, Delete }

/** Adds an Activity, or edits or deletes one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(route: EditorRoute, viewModel: WeekendVibesViewModel, nav: Navigator) {
    val state = viewModel.state.collectAsStateWithLifecycle().value ?: return
    val existing = route.activityId?.let { state.agenda.activity(ActivityId(it)) }
    var draft by remember { mutableStateOf(Draft.from(existing, route, state.today)) }
    var dialog by remember { mutableStateOf<Dialog?>(null) }
    val titleFocus = remember { FocusRequester() }

    // A new Activity starts with its title, so the keyboard opens straight away.
    LaunchedEffect(Unit) {
        if (existing == null) titleFocus.requestFocus()
    }

    fun save() {
        if (existing != null) {
            viewModel.editActivity(existing.id, draft.date, draft.fields())
        } else {
            viewModel.addActivity(draft.date, draft.fields())
        }
        nav.back()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (existing == null) R.string.editor_new else R.string.editor_edit)) },
                navigationIcon = { BackButton(nav) },
                actions = {
                    if (existing != null) {
                        IconButton(onClick = { dialog = Dialog.Delete }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                    TextButton(onClick = ::save, enabled = draft.canSave) { Text(stringResource(R.string.action_save)) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            OutlinedTextField(
                value = draft.title,
                onValueChange = { draft = draft.copy(title = it.take(Limits.TITLE)) },
                label = { Text(stringResource(R.string.field_title)) },
                placeholder = { Text(stringResource(R.string.field_title_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).focusRequester(titleFocus),
            )

            SectionHeader(stringResource(R.string.field_date))
            OutlinedButton(onClick = { dialog = Dialog.Date }, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(longDay(draft.date))
            }
            // Only reachable from Link import for now; the date picker offers Free days only.
            if (!state.agenda.isFree(draft.date) && !draft.date.isWeekendDay) {
                HelpText(stringResource(R.string.field_date_becomes_extra, mediumDay(draft.date, state.today)))
            }

            SectionHeader(stringResource(R.string.field_time))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    stringResource(R.string.field_time_on),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = draft.time != null,
                    onCheckedChange = { on -> draft = draft.copy(time = if (on) DEFAULT_TIME else null) },
                )
            }
            draft.time?.let { time ->
                OutlinedButton(onClick = { dialog = Dialog.Time }, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(timeText(time))
                }
            }

            SectionHeader(stringResource(R.string.field_details))
            OutlinedTextField(
                value = draft.location,
                onValueChange = { draft = draft.copy(location = it.take(Limits.LOCATION)) },
                label = { Text(stringResource(R.string.field_location)) },
                leadingIcon = { Icon(Icons.Filled.Place, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
            OutlinedTextField(
                value = draft.link,
                onValueChange = { draft = draft.copy(link = it.take(Limits.LINK)) },
                label = { Text(stringResource(R.string.field_link)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
            OutlinedTextField(
                value = draft.notes,
                onValueChange = { draft = draft.copy(notes = it.take(Limits.NOTES)) },
                label = { Text(stringResource(R.string.field_notes)) },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }

    when (dialog) {
        Dialog.Date -> DatePickerDialog(
            initial = draft.date,
            selectable = { state.agenda.isFree(it) || it == draft.date },
            onPicked = { draft = draft.copy(date = it) },
            onDismiss = { dialog = null },
        )
        Dialog.Time -> TimePickerDialog(
            initial = draft.time ?: DEFAULT_TIME,
            onPicked = { draft = draft.copy(time = it) },
            onDismiss = { dialog = null },
        )
        Dialog.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.delete_activity_title)) },
            text = { Text(stringResource(R.string.delete_activity_body, draft.title.ifBlank { existing?.title.orEmpty() })) },
            confirmButton = {
                TextButton(
                    onClick = {
                        existing?.let { viewModel.deleteActivity(it.id) }
                        dialog = null
                        nav.back()
                    },
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
        null -> Unit
    }
}

private val DEFAULT_TIME: LocalTime = LocalTime.of(10, 0)
