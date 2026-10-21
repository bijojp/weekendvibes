package io.github.bijojp.weekendvibes.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bijojp.weekendvibes.R
import io.github.bijojp.weekendvibes.domain.ActivityId
import io.github.bijojp.weekendvibes.domain.Limits
import io.github.bijojp.weekendvibes.domain.ReminderLead
import io.github.bijojp.weekendvibes.domain.eventpage.EventOption
import io.github.bijojp.weekendvibes.domain.eventpage.rankedFor
import io.github.bijojp.weekendvibes.domain.isWeekendDay
import io.github.bijojp.weekendvibes.ui.EditorRoute
import io.github.bijojp.weekendvibes.ui.LocalSnackbarHostState
import io.github.bijojp.weekendvibes.ui.Navigator
import io.github.bijojp.weekendvibes.ui.WeekendVibesViewModel
import io.github.bijojp.weekendvibes.ui.components.Backdrop
import io.github.bijojp.weekendvibes.ui.components.CircleIconButton
import io.github.bijojp.weekendvibes.ui.components.ConfirmDialog
import io.github.bijojp.weekendvibes.ui.components.DatePickerDialog
import io.github.bijojp.weekendvibes.ui.components.FieldCard
import io.github.bijojp.weekendvibes.ui.components.FieldDivider
import io.github.bijojp.weekendvibes.ui.components.Overline
import io.github.bijojp.weekendvibes.ui.components.Pill
import io.github.bijojp.weekendvibes.ui.components.SunsetButton
import io.github.bijojp.weekendvibes.ui.components.TimePickerDialog
import io.github.bijojp.weekendvibes.ui.components.VibeTextField
import io.github.bijojp.weekendvibes.ui.components.mediumDay
import io.github.bijojp.weekendvibes.ui.components.pressable
import io.github.bijojp.weekendvibes.ui.components.timeText
import io.github.bijojp.weekendvibes.ui.theme.Vibes
import java.net.URI
import java.time.LocalTime

private enum class Dialog { Date, Time, Delete, Options }

/** Adds an Activity (by hand or from a shared link), or edits or deletes one. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(route: EditorRoute, viewModel: WeekendVibesViewModel, nav: Navigator) {
    val state = viewModel.state.collectAsStateWithLifecycle().value ?: return
    val colors = Vibes.colors
    val existing = route.activityId?.let { state.agenda.activity(ActivityId(it)) }
    var draft by remember { mutableStateOf(Draft.from(existing, route, state.today)) }
    var dialog by remember { mutableStateOf<Dialog?>(null) }
    val titleFocus = remember { FocusRequester() }
    val uriHandler = LocalUriHandler.current
    val scroll = rememberScrollState()
    val focusManager = LocalFocusManager.current

    // Link import: read the page, then fill in whatever the user hasn't typed yet.
    var importState by remember { mutableStateOf<ImportState?>(if (route.importLink != null) ImportState.Reading else null) }
    var attempt by remember { mutableIntStateOf(0) }
    var options by remember { mutableStateOf<List<EventOption>>(emptyList()) }
    var chosen by remember { mutableStateOf<EventOption?>(null) }
    LaunchedEffect(route.importLink, attempt) {
        val link = route.importLink ?: return@LaunchedEffect
        importState = ImportState.Reading
        val result = viewModel.readLink(link)
        if (result == null) {
            importState = ImportState.Failed
            return@LaunchedEffect
        }
        options = result.page.options.rankedFor(state.settings.homeCity, state.today)
        chosen = options.firstOrNull()
        draft = draft.copy(
            title = draft.title.ifBlank { result.page.title.orEmpty().take(Limits.TITLE) },
            picture = draft.picture ?: result.picture,
        ).let { filled -> chosen?.let(filled::with) ?: filled }
        importState = ImportState.Read(runCatching { URI(link).host.removePrefix("www.") }.getOrNull() ?: link)
    }

    // While a link is read and once it's filled in, show the top: the status, the cover and the title.
    LaunchedEffect(importState) {
        if (importState == null) return@LaunchedEffect
        focusManager.clearFocus()
        scroll.animateScrollTo(0)
    }

    LaunchedEffect(Unit) {
        if (existing == null && route.importLink == null) titleFocus.requestFocus()
    }

    fun save() {
        if (existing != null) {
            viewModel.editActivity(existing.id, draft.date, draft.fields())
        } else {
            viewModel.addActivity(draft.date, draft.fields())
        }
        nav.back()
    }

    Backdrop {
        Column(Modifier.fillMaxSize().imePadding()) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(Icons.Rounded.Close, stringResource(R.string.action_close), nav::back)
                Spacer(Modifier.weight(1f))
                if (existing != null) {
                    CircleIconButton(Icons.Rounded.DeleteOutline, stringResource(R.string.action_delete), { dialog = Dialog.Delete }, tint = colors.danger)
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(scroll)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
            ) {
                importState?.let { importing ->
                    ImportPanel(
                        state = importing,
                        optionCount = options.size,
                        onRetry = { attempt++ },
                        onChooseOption = { dialog = Dialog.Options },
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    )
                }

                CoverField(draft.picture, viewModel) { draft = draft.copy(picture = it) }
                Spacer(Modifier.height(20.dp))

                VibeTextField(
                    value = draft.title,
                    onValueChange = { draft = draft.copy(title = it.take(Limits.TITLE)) },
                    placeholder = stringResource(R.string.field_title_hint),
                    singleLine = false,
                    textStyle = MaterialTheme.typography.displaySmall,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.focusRequester(titleFocus).padding(start = 0.dp),
                )

                Overline(stringResource(R.string.field_when), modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pill(
                        text = mediumDay(draft.date, state.today),
                        icon = Icons.Rounded.CalendarMonth,
                        selected = true,
                        accent = colors.dayColor(draft.date),
                        onClick = { dialog = Dialog.Date },
                    )
                    Pill(
                        text = draft.time?.let { timeText(it) } ?: stringResource(R.string.field_time_add),
                        icon = Icons.Rounded.Schedule,
                        selected = draft.time != null,
                        accent = colors.dayColor(draft.date),
                        onClick = { dialog = Dialog.Time },
                    )
                    if (draft.time != null) {
                        Pill(
                            text = stringResource(R.string.field_time_clear),
                            onClick = { draft = draft.copy(time = null, reminder = null) },
                        )
                    }
                }
                if (!state.agenda.isFree(draft.date) && !draft.date.isWeekendDay) {
                    Text(
                        stringResource(R.string.field_date_becomes_extra, mediumDay(draft.date, state.today)),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.extra,
                        modifier = Modifier.padding(start = 4.dp, top = 10.dp),
                    )
                }

                AnimatedVisibility(draft.time != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Column {
                        Overline(stringResource(R.string.field_remind), modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReminderChoices.forEach { (lead, label) ->
                                Pill(
                                    text = stringResource(label),
                                    selected = draft.reminder == lead,
                                    accent = colors.sunsetMid,
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                                    onClick = { draft = draft.copy(reminder = lead) },
                                )
                            }
                        }
                    }
                }

                Overline(stringResource(R.string.field_details), modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 10.dp))
                FieldCard {
                    VibeTextField(
                        value = draft.location,
                        onValueChange = { draft = draft.copy(location = it.take(Limits.LOCATION)) },
                        placeholder = stringResource(R.string.field_location),
                        icon = Icons.Rounded.Place,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    )
                    FieldDivider()
                    VibeTextField(
                        value = draft.link,
                        onValueChange = { draft = draft.copy(link = it.take(Limits.LINK)) },
                        placeholder = stringResource(R.string.field_link),
                        icon = Icons.Rounded.Link,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        trailing = draft.link.trim().takeIf { it.startsWith("https://") || it.startsWith("http://") }?.let { link ->
                            {
                                Box(
                                    Modifier.size(32.dp).clip(CircleShape).background(colors.surface).pressable { runCatching { uriHandler.openUri(link) } },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.OpenInNew,
                                        contentDescription = stringResource(R.string.action_open_link),
                                        tint = colors.textSoft,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        },
                    )
                    FieldDivider()
                    VibeTextField(
                        value = draft.notes,
                        onValueChange = { draft = draft.copy(notes = it.take(Limits.NOTES)) },
                        placeholder = stringResource(R.string.field_notes),
                        icon = Icons.AutoMirrored.Rounded.Notes,
                        singleLine = false,
                        minLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    )
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, colors.canvas)))
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                SunsetButton(
                    text = stringResource(if (existing == null) R.string.editor_save_new else R.string.editor_save_edit),
                    enabled = draft.canSave,
                    onClick = ::save,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        SnackbarHost(LocalSnackbarHostState.current, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 88.dp))
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
        Dialog.Delete -> ConfirmDialog(
            title = stringResource(R.string.delete_activity_title),
            body = stringResource(R.string.delete_activity_body, existing?.title.orEmpty()),
            confirm = stringResource(R.string.action_delete),
            onConfirm = {
                existing?.let { viewModel.deleteActivity(it.id) }
                nav.back()
            },
            onDismiss = { dialog = null },
        )
        Dialog.Options -> OptionsSheet(
            options = options,
            homeCity = state.settings.homeCity,
            selected = chosen,
            today = state.today,
            onPick = { option ->
                chosen = option
                draft = draft.with(option)
            },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

private val ReminderChoices = listOf(
    null to R.string.remind_off,
    ReminderLead.HalfHour to R.string.remind_30m,
    ReminderLead.Hour to R.string.remind_1h,
    ReminderLead.ThreeHours to R.string.remind_3h,
    ReminderLead.Day to R.string.remind_1d,
)

private val DEFAULT_TIME: LocalTime = LocalTime.of(10, 0)
