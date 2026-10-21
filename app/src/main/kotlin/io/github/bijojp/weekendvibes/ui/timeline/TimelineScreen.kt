package io.github.bijojp.weekendvibes.ui.timeline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bijojp.weekendvibes.R
import io.github.bijojp.weekendvibes.domain.Activity
import io.github.bijojp.weekendvibes.domain.BreakCard
import io.github.bijojp.weekendvibes.domain.ExtraDay
import io.github.bijojp.weekendvibes.domain.FreeDay
import io.github.bijojp.weekendvibes.domain.Timeline
import io.github.bijojp.weekendvibes.ui.EditorRoute
import io.github.bijojp.weekendvibes.ui.LocalSnackbarHostState
import io.github.bijojp.weekendvibes.ui.Navigator
import io.github.bijojp.weekendvibes.ui.WeekendVibesViewModel
import io.github.bijojp.weekendvibes.ui.components.breakRange
import io.github.bijojp.weekendvibes.ui.components.longDay
import io.github.bijojp.weekendvibes.ui.components.timeText
import io.github.bijojp.weekendvibes.ui.components.weekdayCaps
import io.github.bijojp.weekendvibes.ui.theme.LocalDayColors
import kotlinx.coroutines.launch
import java.time.LocalDate

/** How far ahead the Timeline reaches at first, and how much more it adds each time the end comes into view. */
private const val WEEKS_AHEAD = 26L

private sealed interface ExtraDayAction {
    data object Add : ExtraDayAction

    data class Edit(val extraDay: ExtraDay) : ExtraDayAction

    data class Remove(val day: FreeDay) : ExtraDayAction
}

/** The home screen: every Break in date order, opening on This weekend. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(viewModel: WeekendVibesViewModel, nav: Navigator) {
    val state = viewModel.state.collectAsStateWithLifecycle().value ?: return
    var weeksAhead by rememberSaveable { mutableLongStateOf(WEEKS_AHEAD) }
    val cards = remember(state.agenda, state.today, weeksAhead) {
        val until = maxOf(state.today.plusWeeks(weeksAhead), state.agenda.lastDate ?: state.today)
        Timeline.build(state.agenda, state.today, until)
    }
    val thisWeekend = cards.indexOfFirst { it.isThisWeekend }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = thisWeekend)
    val scope = rememberCoroutineScope()
    var extraDayAction by remember { mutableStateOf<ExtraDayAction?>(null) }

    val currentThisWeekend by rememberUpdatedState(thisWeekend)
    val awayFromThisWeekend by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.none { it.index == currentThisWeekend } }
    }
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            info.totalItemsCount > 0 && (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd) {
        if (nearEnd) weeksAhead += WEEKS_AHEAD
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    if (awayFromThisWeekend) {
                        TextButton(onClick = { scope.launch { listState.animateScrollToItem(currentThisWeekend) } }) {
                            Text(stringResource(R.string.action_jump_this_weekend))
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            AddMenu(
                onAddActivity = { nav.open(EditorRoute()) },
                onAddExtraDay = { extraDayAction = ExtraDayAction.Add },
            )
        },
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
    ) { padding ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            items(cards, key = { it.span.start.toEpochDay() }) { card ->
                BreakCardView(
                    card = card,
                    today = state.today,
                    onAdd = { date -> nav.open(EditorRoute(date = date.toString())) },
                    onOpen = { nav.open(EditorRoute(activityId = it.id.value)) },
                    onEditExtraDay = { extraDayAction = ExtraDayAction.Edit(it) },
                    onRemoveExtraDay = { extraDayAction = ExtraDayAction.Remove(it) },
                )
            }
        }
    }

    when (val action = extraDayAction) {
        ExtraDayAction.Add -> ExtraDayDialog(
            existing = null,
            agenda = state.agenda,
            onSave = viewModel::setExtraDay,
            onDismiss = { extraDayAction = null },
        )
        is ExtraDayAction.Edit -> ExtraDayDialog(
            existing = action.extraDay,
            agenda = state.agenda,
            onSave = viewModel::setExtraDay,
            onDismiss = { extraDayAction = null },
        )
        is ExtraDayAction.Remove -> RemoveExtraDayDialog(
            day = action.day,
            onRemove = { viewModel.removeExtraDay(action.day.date) },
            onDismiss = { extraDayAction = null },
        )
        null -> Unit
    }
}

/** The + button: a new Activity (next Saturday unless changed), or an Extra day. */
@Composable
private fun AddMenu(onAddActivity: () -> Unit, onAddExtraDay: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FloatingActionButton(onClick = { open = true }) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_add_activity)) },
                onClick = {
                    open = false
                    onAddActivity()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_add_extra_day)) },
                onClick = {
                    open = false
                    onAddExtraDay()
                },
            )
        }
    }
}

@Composable
private fun BreakCardView(
    card: BreakCard,
    today: LocalDate,
    onAdd: (LocalDate) -> Unit,
    onOpen: (Activity) -> Unit,
    onEditExtraDay: (ExtraDay) -> Unit,
    onRemoveExtraDay: (FreeDay) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    OutlinedCard(
        colors = if (card.isThisWeekend) {
            CardDefaults.outlinedCardColors(containerColor = colors.primaryContainer.copy(alpha = 0.35f))
        } else {
            CardDefaults.outlinedCardColors()
        },
        border = if (card.isThisWeekend) BorderStroke(2.dp, colors.primary) else CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        Column(Modifier.padding(top = 12.dp, bottom = 6.dp)) {
            val title = cardTitle(card)
            val range = breakRange(card.span, today)
            Text(
                buildAnnotatedString {
                    if (title != null) {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(title) }
                        withStyle(SpanStyle(color = colors.onSurfaceVariant)) { append(" · $range") }
                    } else {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(range) }
                    }
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (card.isPast) colors.onSurfaceVariant else colors.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            card.days.forEach { day ->
                DayRow(day, onAdd, onOpen, onEditExtraDay, onRemoveExtraDay)
            }
        }
    }
}

/** "This weekend", "Long weekend", an Extra day's label, or null to show just the dates. */
@Composable
private fun cardTitle(card: BreakCard): String? {
    val span = card.span
    val label = card.days.firstNotNullOfOrNull { day -> day.extraDay?.label?.takeIf { it.isNotEmpty() } }
    return when {
        card.isThisWeekend && span.isLongWeekend -> stringResource(R.string.card_this_long_weekend)
        card.isThisWeekend && span.isWeekend -> stringResource(R.string.card_this_weekend)
        span.isLongWeekend -> stringResource(R.string.card_long_weekend)
        span.isWeekend -> null
        label != null -> label
        else -> stringResource(R.string.card_day_off)
    }
}

@Composable
private fun DayRow(
    day: FreeDay,
    onAdd: (LocalDate) -> Unit,
    onOpen: (Activity) -> Unit,
    onEditExtraDay: (ExtraDay) -> Unit,
    onRemoveExtraDay: (FreeDay) -> Unit,
) {
    val dayColor = LocalDayColors.current.of(day.date)
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp)) {
        Column(Modifier.width(52.dp).padding(top = 10.dp)) {
            Text(weekdayCaps(day.date), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = dayColor)
            Text(
                day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.weight(1f)) {
            day.extraDay?.label?.takeIf { it.isNotEmpty() }?.let { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = dayColor,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            if (day.activities.isEmpty()) {
                Text(
                    stringResource(R.string.day_nothing_planned),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().clickable { onAdd(day.date) }.padding(vertical = 10.dp),
                )
            } else {
                day.activities.forEach { ActivityRow(it, onOpen) }
            }
        }
        IconButton(onClick = { onAdd(day.date) }) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add_on_day, longDay(day.date)))
        }
        day.extraDay?.let { extraDay ->
            ExtraDayMenu(onEdit = { onEditExtraDay(extraDay) }, onRemove = { onRemoveExtraDay(day) })
        }
    }
}

@Composable
private fun ActivityRow(activity: Activity, onOpen: (Activity) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { onOpen(activity) }
            .padding(vertical = 8.dp),
    ) {
        val time = activity.time?.let { timeText(it) }
        val accent = MaterialTheme.colorScheme.primary
        Text(
            buildAnnotatedString {
                if (time != null) {
                    withStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) { append(time) }
                    append("  ")
                }
                append(activity.title)
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        if (activity.location.isNotEmpty()) {
            Text(
                activity.location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExtraDayMenu(onEdit: () -> Unit, onRemove: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(contentAlignment = Alignment.TopEnd) {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_extra_day_options))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_edit_label)) },
                onClick = {
                    open = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_remove_extra_day)) },
                onClick = {
                    open = false
                    onRemove()
                },
            )
        }
    }
}
