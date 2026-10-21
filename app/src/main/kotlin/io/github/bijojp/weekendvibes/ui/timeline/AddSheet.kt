package io.github.bijojp.weekendvibes.ui.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.bijojp.weekendvibes.R
import io.github.bijojp.weekendvibes.domain.Limits
import io.github.bijojp.weekendvibes.domain.eventpage.SharedText
import io.github.bijojp.weekendvibes.ui.components.FieldCard
import io.github.bijojp.weekendvibes.ui.components.Pill
import io.github.bijojp.weekendvibes.ui.components.SunsetButton
import io.github.bijojp.weekendvibes.ui.components.VibeSheet
import io.github.bijojp.weekendvibes.ui.components.VibeTextField
import io.github.bijojp.weekendvibes.ui.components.pressable
import io.github.bijojp.weekendvibes.ui.theme.Vibes
import kotlinx.coroutines.launch

/** What the "Plan something" button offers: an event link to fill in from, an Activity, or an Extra day. */
@Composable
fun AddSheet(onImportLink: (String) -> Unit, onAddActivity: () -> Unit, onAddExtraDay: () -> Unit, onDismiss: () -> Unit) {
    val colors = Vibes.colors
    VibeSheet(title = stringResource(R.string.add_title), onDismiss = onDismiss) {
        LinkBox(onImportLink)
        Spacer(Modifier.height(16.dp))
        Choice(
            icon = Icons.Rounded.AutoAwesome,
            tint = Brush.linearGradient(listOf(colors.sunsetStart, colors.sunsetEnd)),
            title = stringResource(R.string.add_activity),
            body = stringResource(R.string.add_activity_body),
            onClick = onAddActivity,
        )
        Spacer(Modifier.height(12.dp))
        Choice(
            icon = Icons.Rounded.BeachAccess,
            tint = Brush.linearGradient(listOf(colors.extra, colors.extra.copy(alpha = 0.6f))),
            title = stringResource(R.string.add_extra_day),
            body = stringResource(R.string.add_extra_day_body),
            onClick = onAddExtraDay,
        )
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.IosShare, contentDescription = null, tint = colors.textFaint, modifier = Modifier.size(18.dp))
            Text(
                stringResource(R.string.add_share_tip),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textFaint,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}

/** Paste (or type) an event link; once it holds one, "Fill it in" reads the page like a shared link. */
@Composable
private fun LinkBox(onImportLink: (String) -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    val link = SharedText.firstUrl(text)
    val go = { link?.let(onImportLink) }

    FieldCard {
        VibeTextField(
            value = text,
            onValueChange = { text = it.take(Limits.LINK) },
            placeholder = stringResource(R.string.add_link_hint),
            icon = Icons.Rounded.Link,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { go() }),
            trailing = if (text.isEmpty()) {
                {
                    Pill(
                        text = stringResource(R.string.add_link_paste),
                        icon = Icons.Rounded.ContentPaste,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        onClick = {
                            scope.launch {
                                // Read only on this tap, so Android's "pasted from your clipboard" notice means something.
                                val clip = clipboard.getClipEntry()?.clipData
                                val pasted = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(context).toString() else ""
                                text = pasted.take(Limits.LINK)
                            }
                        },
                    )
                }
            } else {
                null
            },
        )
    }
    AnimatedVisibility(link != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        SunsetButton(
            text = stringResource(R.string.add_link_fill),
            icon = Icons.Rounded.AutoAwesome,
            onClick = { go() },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
    }
}

@Composable
private fun Choice(icon: ImageVector, tint: Brush, title: String, body: String, onClick: () -> Unit) {
    val colors = Vibes.colors
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.raised)
            .border(1.dp, colors.hairline, shape)
            .pressable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(tint), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color(0xFF1A0E0A), modifier = Modifier.size(24.dp))
        }
        Column(Modifier.padding(start = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.text)
            Text(body, style = MaterialTheme.typography.bodySmall, color = colors.textSoft)
        }
    }
}
