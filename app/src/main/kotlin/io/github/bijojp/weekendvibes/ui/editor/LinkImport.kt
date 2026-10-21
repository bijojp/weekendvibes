package io.github.bijojp.weekendvibes.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.bijojp.weekendvibes.R
import io.github.bijojp.weekendvibes.domain.eventpage.EventOption
import io.github.bijojp.weekendvibes.ui.components.Pill
import io.github.bijojp.weekendvibes.ui.components.VibeSheet
import io.github.bijojp.weekendvibes.ui.components.mediumDay
import io.github.bijojp.weekendvibes.ui.components.pressable
import io.github.bijojp.weekendvibes.ui.components.shimmerBrush
import io.github.bijojp.weekendvibes.ui.components.timeText
import io.github.bijojp.weekendvibes.ui.theme.Vibes
import java.time.LocalDate

/** Where Link import has got to. */
internal sealed interface ImportState {
    data object Reading : ImportState

    data object Failed : ImportState

    data class Read(val host: String) : ImportState
}

/** The strip above the form while a shared link is read, and what came of it. */
@Composable
internal fun ImportPanel(
    state: ImportState,
    optionCount: Int,
    onRetry: () -> Unit,
    onChooseOption: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Vibes.colors
    val shape = RoundedCornerShape(22.dp)
    val base = modifier.fillMaxWidth().clip(shape)
    when (state) {
        ImportState.Reading -> Row(
            base.background(shimmerBrush()).border(1.dp, colors.hairline, shape).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.sunsetStart, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.import_reading), style = MaterialTheme.typography.titleSmall, color = colors.text, modifier = Modifier.padding(start = 12.dp))
        }
        ImportState.Failed -> Column(base.background(colors.raised).border(1.dp, colors.hairline, shape).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LinkOff, contentDescription = null, tint = colors.danger, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.import_failed), style = MaterialTheme.typography.titleSmall, color = colors.text, modifier = Modifier.padding(start = 12.dp))
            }
            Text(
                stringResource(R.string.import_failed_body),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSoft,
                modifier = Modifier.padding(start = 32.dp, top = 2.dp, bottom = 12.dp),
            )
            Pill(stringResource(R.string.import_retry), icon = Icons.Rounded.Refresh, onClick = onRetry, modifier = Modifier.padding(start = 32.dp))
        }
        is ImportState.Read -> Row(
            base.background(colors.saturday.copy(alpha = 0.10f)).border(1.dp, colors.saturday.copy(alpha = 0.35f), shape).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.saturday, modifier = Modifier.size(20.dp))
            Text(
                stringResource(R.string.import_read, state.host),
                style = MaterialTheme.typography.titleSmall,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp).weight(1f),
            )
            if (optionCount > 1) {
                Pill(
                    text = pluralStringResource(R.plurals.import_options, optionCount, optionCount),
                    icon = Icons.Rounded.EventRepeat,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    onClick = onChooseOption,
                )
            }
        }
    }
}

/** Every date and venue an event page listed, Home city first; others behind "Show all". */
@Composable
internal fun OptionsSheet(
    options: List<EventOption>,
    homeCity: String,
    selected: EventOption?,
    today: LocalDate,
    onPick: (EventOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = Vibes.colors
    val local = options.filter { it.isIn(homeCity) }
    var showAll by remember { mutableStateOf(local.isEmpty() || local.size == options.size) }
    val shown = if (showAll) options else local
    VibeSheet(title = stringResource(R.string.options_title), onDismiss = onDismiss) {
        if (!showAll) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Text(stringResource(R.string.options_home_city, homeCity), style = MaterialTheme.typography.bodySmall, color = colors.textSoft, modifier = Modifier.weight(1f))
                Pill(
                    pluralStringResource(R.plurals.options_show_all, options.size, options.size),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    onClick = { showAll = true },
                )
            }
        }
        LazyColumn(Modifier.heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shown) { option ->
                val isSelected = option == selected
                val shape = RoundedCornerShape(18.dp)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(if (isSelected) colors.sunsetMid.copy(alpha = 0.12f) else colors.raised)
                        .border(1.dp, if (isSelected) colors.sunsetMid.copy(alpha = 0.6f) else colors.hairline, shape)
                        .pressable {
                            onPick(option)
                            onDismiss()
                        }
                        .padding(16.dp),
                ) {
                    Text(
                        listOfNotNull(mediumDay(option.date, today), option.time?.let { timeText(it) }).joinToString(" · "),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.dayColor(option.date).takeIf { option.date.dayOfWeek.value >= 6 } ?: colors.text,
                    )
                    option.place?.let {
                        Spacer(Modifier.height(2.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = colors.textSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
