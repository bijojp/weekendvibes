package io.github.bijojp.weekendvibes.ui.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bijojp.weekendvibes.R
import io.github.bijojp.weekendvibes.domain.BreakCard
import io.github.bijojp.weekendvibes.domain.ExtraDay
import io.github.bijojp.weekendvibes.domain.FreeDay
import io.github.bijojp.weekendvibes.domain.Timeline
import io.github.bijojp.weekendvibes.ui.EditorRoute
import io.github.bijojp.weekendvibes.ui.LocalSnackbarHostState
import io.github.bijojp.weekendvibes.ui.Navigator
import io.github.bijojp.weekendvibes.ui.SettingsRoute
import io.github.bijojp.weekendvibes.ui.WeekendVibesViewModel
import io.github.bijojp.weekendvibes.ui.components.Backdrop
import io.github.bijojp.weekendvibes.ui.components.CircleIconButton
import io.github.bijojp.weekendvibes.ui.components.Overline
import io.github.bijojp.weekendvibes.ui.components.AccentButton
import io.github.bijojp.weekendvibes.ui.components.breakRange
import io.github.bijojp.weekendvibes.ui.components.monthName
import io.github.bijojp.weekendvibes.ui.theme.Display
import io.github.bijojp.weekendvibes.ui.theme.Vibes
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** How far ahead the Timeline reaches at first, and how much more it adds each time the end comes into view. */
private const val WEEKS_AHEAD = 26L

/** One row of the Timeline's list. */
private sealed interface Entry {
    val key: String

    data object Memories : Entry {
        override val key = "memories"
    }

    data class Month(val date: LocalDate, val past: Boolean) : Entry {
        override val key = "month-$date-$past"
    }

    data class Hero(val card: BreakCard) : Entry {
        override val key = "hero"
    }

    data class Card(val card: BreakCard) : Entry {
        override val key = "card-${card.span.start}"
    }
}

/** Memories (past Breaks with something in them), then the hero and This weekend, then every Break ahead, by month. */
private fun entries(cards: List<BreakCard>): List<Entry> {
    val list = mutableListOf<Entry>()
    val past = cards.filter { it.isPast }
    if (past.isNotEmpty()) list += Entry.Memories
    var month: LocalDate? = null
    past.forEach { card ->
        val first = card.span.start.withDayOfMonth(1)
        if (first != month) list += Entry.Month(first, past = true)
        month = first
        list += Entry.Card(card)
    }
    // The hero opens the present: it sits above the first card from today on, which may be a weekday plan.
    month = null
    val thisWeekend = cards.firstOrNull { it.isThisWeekend }
    cards.filterNot { it.isPast }.forEachIndexed { index, card ->
        val first = card.span.start.withDayOfMonth(1)
        if (index == 0 && thisWeekend != null) {
            list += Entry.Hero(thisWeekend)
        } else if (first != month) {
            list += Entry.Month(first, past = false)
        }
        month = first
        list += Entry.Card(card)
    }
    return list
}

private sealed interface ExtraDayAction {
    data object Add : ExtraDayAction

    data class Edit(val extraDay: ExtraDay) : ExtraDayAction

    data class Remove(val day: FreeDay) : ExtraDayAction
}

/** The home screen: every Break in date order, opening on This weekend. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineScreen(viewModel: WeekendVibesViewModel, nav: Navigator) {
    val state = viewModel.state.collectAsStateWithLifecycle().value ?: return
    val colors = Vibes.colors
    var weeksAhead by rememberSaveable { mutableLongStateOf(WEEKS_AHEAD) }
    val cards = remember(state.agenda, state.today, weeksAhead) {
        val until = maxOf(state.today.plusWeeks(weeksAhead), state.agenda.lastDate ?: state.today)
        Timeline.build(state.agenda, state.today, until)
    }
    val rows = remember(cards) { entries(cards) }
    val heroIndex = rows.indexOfFirst { it is Entry.Hero }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = heroIndex)
    val scope = rememberCoroutineScope()
    var adding by remember { mutableStateOf(false) }
    var extraDayAction by remember { mutableStateOf<ExtraDayAction?>(null) }

    val currentHero by rememberUpdatedState(heroIndex)
    val heroDirection by remember {
        derivedStateOf {
            val visible = listState.layoutInfo.visibleItemsInfo
            when {
                visible.isEmpty() || visible.any { it.index == currentHero || it.index == currentHero + 1 } -> 0
                visible.first().index > currentHero -> -1
                else -> 1
            }
        }
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

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Backdrop {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = topInset + 72.dp, bottom = 160.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(rows, key = { it.key }, contentType = { it::class }) { row ->
                when (row) {
                    Entry.Memories -> MemoriesHeader()
                    is Entry.Month -> MonthHeader(row.date, state.today, faded = row.past)
                    is Entry.Hero -> Hero(row.card, state.today)
                    is Entry.Card -> BreakCardView(
                        card = row.card,
                        today = state.today,
                        onAdd = { date -> nav.open(EditorRoute(date = date.toString())) },
                        onOpen = { nav.open(EditorRoute(activityId = it.id.value)) },
                        onEditExtraDay = { extraDayAction = ExtraDayAction.Edit(it) },
                        onRemoveExtraDay = { extraDayAction = ExtraDayAction.Remove(it) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }

        TopBar(
            heroDirection = heroDirection,
            onJump = { scope.launch { listState.animateScrollToItem(currentHero) } },
            onSettings = { nav.open(SettingsRoute) },
        )

        // A soft fade so the button floats over the list instead of cutting it off.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(150.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, colors.canvas.copy(alpha = 0.92f), colors.canvas))),
        )
        AccentButton(
            text = stringResource(R.string.action_plan),
            icon = Icons.Rounded.Add,
            onClick = { adding = true },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 20.dp),
        )
        SnackbarHost(
            LocalSnackbarHostState.current,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 88.dp),
        )
    }

    if (adding) {
        AddSheet(
            onImportLink = { link ->
                adding = false
                nav.open(EditorRoute(importLink = link))
            },
            onAddActivity = {
                adding = false
                nav.open(EditorRoute())
            },
            onAddExtraDay = {
                adding = false
                extraDayAction = ExtraDayAction.Add
            },
            onDismiss = { adding = false },
        )
    }

    when (val action = extraDayAction) {
        ExtraDayAction.Add -> ExtraDaySheet(
            existing = null,
            agenda = state.agenda,
            today = state.today,
            onSave = viewModel::setExtraDay,
            onDismiss = { extraDayAction = null },
        )
        is ExtraDayAction.Edit -> ExtraDaySheet(
            existing = action.extraDay,
            agenda = state.agenda,
            today = state.today,
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

/** Wordmark, a way back to This weekend once it has scrolled away, and Settings. */
@Composable
private fun TopBar(heroDirection: Int, onJump: () -> Unit, onSettings: () -> Unit) {
    val colors = Vibes.colors
    Box(
        Modifier
            .fillMaxWidth()
            // Solid behind the wordmark so nothing scrolling under it shows through, not even faint text; fades only at the edge.
            .background(Brush.verticalGradient(0f to colors.canvas, 0.72f to colors.canvas, 1f to Color.Transparent))
            .statusBarsPadding()
            .padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
    ) {
        Text(
            buildAnnotatedString {
                append("Weekend ")
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, brush = colors.accent)) { append("Vibes") }
            },
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = Display),
            color = colors.text,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            // Just an arrow, so it never crowds the wordmark: up from later Breaks, down from Memories.
            AnimatedVisibility(heroDirection != 0, enter = fadeIn() + scaleIn(initialScale = 0.8f), exit = fadeOut() + scaleOut(targetScale = 0.8f)) {
                CircleIconButton(
                    icon = if (heroDirection < 0) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.action_jump_this_weekend),
                    onClick = onJump,
                    tint = Vibes.colors.accentMid,
                )
            }
            CircleIconButton(Icons.Rounded.Tune, stringResource(R.string.action_settings), onSettings)
        }
    }
}

/** The big opening line: how far away the weekend is, and what's lined up. */
@Composable
private fun Hero(card: BreakCard, today: LocalDate) {
    val colors = Vibes.colors
    val span = card.span
    val days = ChronoUnit.DAYS.between(today, span.start).toInt()
    val plans = card.days.sumOf { it.activities.size }
    Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 20.dp)) {
        Overline(
            stringResource(if (today in span || days <= 6) R.string.hero_kicker_this else R.string.hero_kicker_next) + " · " + breakRange(span, today),
            brush = colors.accent,
        )
        Spacer(Modifier.height(12.dp))
        val italic = SpanStyle(fontStyle = FontStyle.Italic, brush = colors.accent)
        Text(
            buildAnnotatedString {
                when {
                    today in span && span.isWeekend -> append(stringResource(R.string.hero_now_weekend))
                    today in span -> append(stringResource(R.string.hero_now_day_off))
                    days == 1 -> withStyle(italic) { append(stringResource(R.string.hero_tomorrow)) }
                    else -> {
                        withStyle(italic) { append(pluralStringResource(R.plurals.hero_days_lead, days, days)) }
                        // Android trims leading spaces from string resources, so the gap is added here.
                        append(" ")
                        append(stringResource(R.string.hero_days_tail))
                    }
                }
            },
            style = MaterialTheme.typography.displayLarge,
            color = colors.text,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (plans == 0) stringResource(R.string.hero_nothing) else pluralStringResource(R.plurals.hero_planned, plans, plans),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textSoft,
        )
    }
}

@Composable
private fun MonthHeader(date: LocalDate, today: LocalDate, faded: Boolean) {
    val colors = Vibes.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 10.dp),
    ) {
        Overline(monthName(date, today), color = if (faded) colors.textFaint else colors.textSoft)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f).height(1.dp).background(colors.hairline))
    }
}

@Composable
private fun MemoriesHeader() {
    val colors = Vibes.colors
    Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 4.dp)) {
        Text(stringResource(R.string.memories), style = MaterialTheme.typography.displaySmall, color = colors.text)
        Text(stringResource(R.string.memories_body), style = MaterialTheme.typography.bodyMedium, color = colors.textFaint)
    }
}
