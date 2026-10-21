package io.github.bijojp.weekendvibes.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.bijojp.weekendvibes.R
import io.github.bijojp.weekendvibes.ui.components.VibeSheet
import io.github.bijojp.weekendvibes.ui.components.pressable
import io.github.bijojp.weekendvibes.ui.theme.Vibes

/** What the "Plan something" button offers: an Activity, or an Extra day. Plus a nudge about Link import. */
@Composable
fun AddSheet(onAddActivity: () -> Unit, onAddExtraDay: () -> Unit, onDismiss: () -> Unit) {
    val colors = Vibes.colors
    VibeSheet(title = stringResource(R.string.add_title), onDismiss = onDismiss) {
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
