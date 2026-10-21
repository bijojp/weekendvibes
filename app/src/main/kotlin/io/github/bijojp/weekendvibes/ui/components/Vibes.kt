package io.github.bijojp.weekendvibes.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.bijojp.weekendvibes.ui.theme.Vibes
import java.io.File

/** Resolves a stored Picture's file name to its file. Provided once by the app. */
val LocalPictureFiles = staticCompositionLocalOf<(String) -> File> { error("LocalPictureFiles not provided") }

/** The canvas every screen sits on: near-black (or warm paper) with two slow, soft glows. */
@Composable
fun Backdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val colors = Vibes.colors
    val drift by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )
    val strength = if (colors.isDark) 0.24f else 0.30f
    Box(
        modifier
            .fillMaxSize()
            .background(colors.canvas)
            .drawBehind {
                val warm = Offset(size.width * (0.85f - 0.15f * drift), size.height * (0.02f + 0.04f * drift))
                drawCircle(
                    Brush.radialGradient(listOf(colors.glowWarm.copy(alpha = strength), Color.Transparent), warm, size.width * 0.9f),
                    radius = size.width * 0.9f,
                    center = warm,
                )
                val cool = Offset(size.width * (0.05f + 0.12f * drift), size.height * (0.12f - 0.03f * drift))
                drawCircle(
                    Brush.radialGradient(listOf(colors.glowCool.copy(alpha = strength * 0.55f), Color.Transparent), cool, size.width * 0.75f),
                    radius = size.width * 0.75f,
                    center = cool,
                )
            },
        content = content,
    )
}

/** Clickable that sinks a little under the finger and ticks the haptics, instead of a ripple. */
fun Modifier.pressable(enabled: Boolean = true, onLongClick: (() -> Unit)? = null, onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(dampingRatio = 0.55f, stiffness = 600f), label = "press")
    val haptics = LocalHapticFeedback.current
    this
        .scale(scale)
        .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}

/** A rounded panel. [highlighted] gives it the accent edge and glow (This weekend). */
@Composable
fun VibeCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Vibes.colors
    val edge = if (highlighted) {
        colors.accent
    } else {
        Brush.verticalGradient(listOf(colors.hairline.copy(alpha = colors.hairline.alpha * 1.6f), colors.hairline.copy(alpha = 0.02f)))
    }
    Column(
        modifier
            .then(
                if (highlighted) {
                    Modifier.shadow(28.dp, shape, ambientColor = colors.accentMid, spotColor = colors.accentEnd)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(colors.surface)
            .border(if (highlighted) 1.5.dp else 1.dp, edge, shape),
        content = content,
    )
}

/** Tiny tracked-out caps, the app's overline voice ("THIS WEEKEND", "NOVEMBER"). */
@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = Vibes.colors.textSoft, brush: Brush? = null) {
    Text(
        text.uppercase(),
        style = if (brush != null) MaterialTheme.typography.labelSmall.merge(TextStyle(brush = brush)) else MaterialTheme.typography.labelSmall,
        color = if (brush != null) Color.Unspecified else color,
        modifier = modifier,
    )
}

/** The main call to action: an accent pill with dark text. */
@Composable
fun AccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = 56.dp,
) {
    val colors = Vibes.colors
    Row(
        modifier
            .height(height)
            .then(if (enabled) Modifier.shadow(18.dp, CircleShape, ambientColor = colors.accentMid, spotColor = colors.accentEnd) else Modifier)
            .clip(CircleShape)
            .background(if (enabled) colors.accent else Brush.linearGradient(listOf(colors.raised, colors.raised)))
            .pressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val content = if (enabled) colors.onAccent else colors.textFaint
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
            Box(Modifier.size(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = content, textAlign = TextAlign.Center)
    }
}

/** A small rounded chip: a choice, a filter, or a quiet action. */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    icon: ImageVector? = null,
    accent: Color = Vibes.colors.text,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    onClick: (() -> Unit)? = null,
) {
    val colors = Vibes.colors
    val shape = CircleShape
    Row(
        modifier
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.16f) else colors.raised)
            .border(1.dp, if (selected) accent.copy(alpha = 0.7f) else colors.hairline, shape)
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (selected) accent else colors.textSoft, modifier = Modifier.size(18.dp))
            Box(Modifier.size(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) accent else colors.text, maxLines = 1)
    }
}

/** A round icon button on a raised disc. */
@Composable
fun CircleIconButton(icon: ImageVector, contentDescription: String?, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Vibes.colors.text) {
    val colors = Vibes.colors
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(colors.raised.copy(alpha = 0.9f))
            .border(1.dp, colors.hairline, CircleShape)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/** A stored Picture, cropped to fill. */
@Composable
fun PictureImage(name: String, modifier: Modifier = Modifier, contentDescription: String? = null) {
    AsyncImage(
        model = LocalPictureFiles.current(name),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

/** A sweeping highlight for things that are loading. */
@Composable
fun shimmerBrush(): Brush {
    val colors = Vibes.colors
    val x by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -600f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "x",
    )
    return Brush.linearGradient(
        listOf(colors.raised, colors.accentMid.copy(alpha = 0.25f), colors.raised),
        start = Offset(x, 0f),
        end = Offset(x + 600f, 300f),
    )
}
