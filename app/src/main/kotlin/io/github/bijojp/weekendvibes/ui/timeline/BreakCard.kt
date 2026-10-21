package io.github.bijojp.weekendvibes.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.bijojp.weekendvibes.R
import io.github.bijojp.weekendvibes.domain.Activity
import io.github.bijojp.weekendvibes.domain.BreakCard
import io.github.bijojp.weekendvibes.domain.ExtraDay
import io.github.bijojp.weekendvibes.domain.FreeDay
import io.github.bijojp.weekendvibes.domain.isWeekendDay
import io.github.bijojp.weekendvibes.ui.components.Overline
import io.github.bijojp.weekendvibes.ui.components.PictureImage
import io.github.bijojp.weekendvibes.ui.components.VibeCard
import io.github.bijojp.weekendvibes.ui.components.breakRange
import io.github.bijojp.weekendvibes.ui.components.longDay
import io.github.bijojp.weekendvibes.ui.components.pressable
import io.github.bijojp.weekendvibes.ui.components.relativeText
import io.github.bijojp.weekendvibes.ui.components.timeText
import io.github.bijojp.weekendvibes.ui.components.weekdayCaps
import io.github.bijojp.weekendvibes.ui.theme.Vibes
import java.time.LocalDate

/** One Break: a kicker and its dates, then each day with its plans. This weekend glows. */
@Composable
fun BreakCardView(
    card: BreakCard,
    today: LocalDate,
    onAdd: (LocalDate) -> Unit,
    onOpen: (Activity) -> Unit,
    onEditExtraDay: (ExtraDay) -> Unit,
    onRemoveExtraDay: (FreeDay) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Vibes.colors
    VibeCard(
        highlighted = card.isThisWeekend,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    if (card.isThisWeekend) {
                        Overline(kicker(card), brush = colors.accent)
                    } else {
                        Overline(kicker(card), color = if (card.days.any { it.extraDay != null } && !card.span.isWeekend) colors.extra else colors.textSoft)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        breakRange(card.span, today),
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (card.isPast) colors.textSoft else colors.text,
                    )
                }
                Text(
                    relativeText(card.span, today),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textFaint,
                    modifier = Modifier.padding(bottom = 4.dp, end = 4.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
            card.days.forEachIndexed { index, day ->
                if (index > 0) Spacer(Modifier.height(14.dp))
                DayBlock(day, onAdd, onOpen, onEditExtraDay, onRemoveExtraDay)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** "THIS WEEKEND", "LONG WEEKEND · 3 DAYS", an Extra day's label, "DAY OFF" or "WEEKEND". */
@Composable
private fun kicker(card: BreakCard): String {
    val span = card.span
    val label = card.days.firstNotNullOfOrNull { day -> day.extraDay?.label?.takeIf { it.isNotEmpty() } }
    return when {
        card.isThisWeekend && span.isLongWeekend -> stringResource(R.string.card_this_long_weekend)
        card.isThisWeekend && span.isWeekend -> stringResource(R.string.card_this_weekend)
        span.isLongWeekend -> pluralStringResource(R.plurals.card_long_weekend, span.days.size, span.days.size)
        span.isWeekend -> stringResource(R.string.card_weekend)
        label != null -> label
        card.days.any { it.extraDay != null } -> stringResource(R.string.card_day_off)
        else -> stringResource(R.string.card_weekday)
    }
}

@Composable
private fun DayBlock(
    day: FreeDay,
    onAdd: (LocalDate) -> Unit,
    onOpen: (Activity) -> Unit,
    onEditExtraDay: (ExtraDay) -> Unit,
    onRemoveExtraDay: (FreeDay) -> Unit,
) {
    val colors = Vibes.colors
    // Weekend days and Extra days wear their colour; a weekday that only has plans stays neutral.
    val dayColor = if (day.date.isWeekendDay || day.extraDay != null) colors.dayColor(day.date) else colors.textSoft
    Row {
        // The day rail: SAT over a big serif 24, and an Extra day's label and options under it.
        Column(Modifier.width(60.dp).padding(top = 2.dp)) {
            Overline(weekdayCaps(day.date), color = dayColor)
            Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.displaySmall, color = colors.text)
            day.extraDay?.let { extraDay ->
                if (extraDay.label.isNotEmpty()) {
                    Text(
                        extraDay.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = dayColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
                ExtraDayMenu(onEdit = { onEditExtraDay(extraDay) }, onRemove = { onRemoveExtraDay(day) })
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (day.activities.isEmpty()) {
                FreeDayTile(onClick = { onAdd(day.date) }, description = stringResource(R.string.action_add_on_day, longDay(day.date)))
            } else {
                day.activities.forEach { activity ->
                    if (activity.picture != null) PictureTile(activity, onOpen) else PlainTile(activity, dayColor, onOpen)
                }
                AddAnother(onClick = { onAdd(day.date) })
            }
        }
    }
}

/** A plan with a Picture: the image fills the tile, the words sit on a dark scrim at the bottom. */
@Composable
private fun PictureTile(activity: Activity, onOpen: (Activity) -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(shape)
            .pressable { onOpen(activity) },
    ) {
        PictureImage(activity.picture!!, Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.86f))),
        )
        activity.time?.let { time ->
            Text(
                timeText(time),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(
                activity.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (activity.location.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Icon(Icons.Rounded.Place, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                    Text(
                        activity.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}

/** A plan without a Picture: a raised tile with a thin bar in the day's colour. */
@Composable
private fun PlainTile(activity: Activity, accent: Color, onOpen: (Activity) -> Unit) {
    val colors = Vibes.colors
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.raised)
            .border(1.dp, colors.hairline, shape)
            .pressable { onOpen(activity) }
            .heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.padding(start = 12.dp).width(3.dp).height(36.dp).clip(CircleShape).background(accent))
        Column(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                activity.time?.let { timeText(it) } ?: stringResource(R.string.anytime),
                style = MaterialTheme.typography.labelMedium,
                color = if (activity.time != null) accent else colors.textFaint,
            )
            Text(
                activity.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (activity.location.isNotEmpty()) {
                Text(
                    activity.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(Modifier.padding(end = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (activity.reminder != null && activity.time != null) {
                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = colors.textFaint, modifier = Modifier.size(16.dp))
            }
            if (activity.link.isNotEmpty()) {
                Icon(Icons.Rounded.Link, contentDescription = null, tint = colors.textFaint, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** An empty day: a dashed outline inviting a plan. */
@Composable
private fun FreeDayTile(onClick: () -> Unit, description: String) {
    val colors = Vibes.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .drawBehind {
                drawRoundRect(
                    color = colors.textFaint.copy(alpha = 0.45f),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))),
                )
            }
            .clip(RoundedCornerShape(20.dp))
            .pressable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.day_free), style = MaterialTheme.typography.bodyMedium, color = colors.textFaint, modifier = Modifier.weight(1f))
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(colors.raised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = description, tint = colors.textSoft, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AddAnother(onClick: () -> Unit) {
    val colors = Vibes.colors
    Row(
        Modifier
            .clip(CircleShape)
            .pressable(onClick = onClick)
            .padding(PaddingValues(horizontal = 6.dp, vertical = 4.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.textFaint, modifier = Modifier.size(16.dp))
        Text(
            stringResource(R.string.day_add_another),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textFaint,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun ExtraDayMenu(onEdit: () -> Unit, onRemove: () -> Unit) {
    val colors = Vibes.colors
    var open by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier
                .padding(top = 4.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(colors.raised)
                .pressable { open = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.MoreHoriz, contentDescription = stringResource(R.string.action_extra_day_options), tint = colors.textSoft, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = colors.raised,
            shape = RoundedCornerShape(18.dp),
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_edit_label)) },
                onClick = {
                    open = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_remove_extra_day), color = colors.danger) },
                onClick = {
                    open = false
                    onRemove()
                },
            )
        }
    }
}
