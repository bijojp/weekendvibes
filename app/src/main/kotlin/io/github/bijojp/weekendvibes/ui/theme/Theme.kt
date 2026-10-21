package io.github.bijojp.weekendvibes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.bijojp.weekendvibes.R
import java.time.DayOfWeek
import java.time.LocalDate

// The look: a warm near-black canvas, an editorial serif for anything worth reading twice, a crisp
// sans for everything else, and one signature "golden hour" gradient used sparingly.

/** Colours beyond Material's roles. Read them with [Vibes.colors]. */
@Immutable
data class VibesColors(
    val canvas: Color,
    val surface: Color,
    val raised: Color,
    val hairline: Color,
    val text: Color,
    val textSoft: Color,
    val textFaint: Color,
    val saturday: Color,
    val sunday: Color,
    val extra: Color,
    val sunsetStart: Color,
    val sunsetMid: Color,
    val sunsetEnd: Color,
    val onSunset: Color,
    val glowWarm: Color,
    val glowCool: Color,
    val danger: Color,
    val isDark: Boolean,
) {
    /** The signature gradient: buttons, This weekend, the hero. */
    val sunset: Brush get() = Brush.linearGradient(listOf(sunsetStart, sunsetMid, sunsetEnd))

    /** The colour a kind of Free day is labelled in. */
    fun dayColor(date: LocalDate): Color = when (date.dayOfWeek) {
        DayOfWeek.SATURDAY -> saturday
        DayOfWeek.SUNDAY -> sunday
        else -> extra
    }
}

private val Night = VibesColors(
    canvas = Color(0xFF09090B),
    surface = Color(0xFF131316),
    raised = Color(0xFF1C1C21),
    hairline = Color(0x1AFFFFFF),
    text = Color(0xFFF6F1E9),
    textSoft = Color(0xFFB4ADA3),
    textFaint = Color(0xFF6F6A64),
    saturday = Color(0xFF6FE3CF),
    sunday = Color(0xFFFF86A8),
    extra = Color(0xFFFFC46B),
    sunsetStart = Color(0xFFFFC36B),
    sunsetMid = Color(0xFFFF7A6B),
    sunsetEnd = Color(0xFFFF4F8B),
    onSunset = Color(0xFF1A0E0A),
    glowWarm = Color(0xFFFF7A4D),
    glowCool = Color(0xFF7B5CFF),
    danger = Color(0xFFFF6B6B),
    isDark = true,
)

private val Day = VibesColors(
    canvas = Color(0xFFF7F2EA),
    surface = Color(0xFFFFFCF7),
    raised = Color(0xFFF1EBE1),
    hairline = Color(0x1A1A140C),
    text = Color(0xFF1B1712),
    textSoft = Color(0xFF6B6359),
    textFaint = Color(0xFFA39A8E),
    saturday = Color(0xFF0E8C7A),
    sunday = Color(0xFFD23D6E),
    extra = Color(0xFFB86E00),
    sunsetStart = Color(0xFFFFB45A),
    sunsetMid = Color(0xFFFF6A55),
    sunsetEnd = Color(0xFFF23E7D),
    onSunset = Color(0xFF1A0E0A),
    glowWarm = Color(0xFFFFB27A),
    glowCool = Color(0xFFB7A6FF),
    danger = Color(0xFFD03A3A),
    isDark = false,
)

private val LocalVibesColors = staticCompositionLocalOf { Night }

object Vibes {
    val colors: VibesColors
        @Composable get() = LocalVibesColors.current
}

/** Fraunces at a large optical size and a soft finish reads warm and editorial in headlines. */
private fun fraunces(weight: Int, opticalSize: Float, italic: Boolean = false) = Font(
    resId = if (italic) R.font.fraunces_italic else R.font.fraunces,
    weight = FontWeight(weight),
    style = if (italic) FontStyle.Italic else FontStyle.Normal,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("opsz", opticalSize),
        FontVariation.Setting("SOFT", 100f),
    ),
)

private fun manrope(weight: Int) = Font(
    resId = R.font.manrope,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Display = FontFamily(
    fraunces(300, 144f),
    fraunces(400, 144f),
    fraunces(500, 144f),
    fraunces(600, 72f),
    fraunces(400, 144f, italic = true),
    fraunces(500, 144f, italic = true),
)

val Sans = FontFamily(manrope(400), manrope(500), manrope(600), manrope(700), manrope(800))

private val VibesTypography = Typography(
    displayLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Normal, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-0.02).em),
    displayMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-0.02).em),
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.01).em),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.01).em),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.em),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.02.em),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.18.em),
)

@Composable
fun WeekendVibesTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) Night else Day
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.sunsetMid,
            onPrimary = colors.onSunset,
            secondary = colors.saturday,
            background = colors.canvas,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.raised,
            onSurfaceVariant = colors.textSoft,
            surfaceContainerHigh = colors.raised,
            surfaceContainer = colors.surface,
            surfaceContainerLow = colors.surface,
            outline = colors.hairline,
            outlineVariant = colors.hairline,
            error = colors.danger,
        )
    } else {
        lightColorScheme(
            primary = colors.sunsetMid,
            onPrimary = colors.onSunset,
            secondary = colors.saturday,
            background = colors.canvas,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.raised,
            onSurfaceVariant = colors.textSoft,
            surfaceContainerHigh = colors.raised,
            surfaceContainer = colors.surface,
            surfaceContainerLow = colors.surface,
            outline = colors.hairline,
            outlineVariant = colors.hairline,
            error = colors.danger,
        )
    }
    CompositionLocalProvider(LocalVibesColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = VibesTypography, content = content)
    }
}
