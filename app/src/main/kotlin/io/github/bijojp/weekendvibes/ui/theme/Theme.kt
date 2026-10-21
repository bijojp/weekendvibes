package io.github.bijojp.weekendvibes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import java.time.DayOfWeek
import java.time.LocalDate

// Built around the web version's Saturday teal and Sunday rose.
private val LightColors = lightColorScheme(
    primary = Color(0xFF0F7F81),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBDEFED),
    onPrimaryContainer = Color(0xFF002020),
    secondary = Color(0xFFB23A62),
    onSecondary = Color.White,
    background = Color(0xFFF8FBFB),
    surface = Color(0xFFF8FBFB),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80D5D2),
    onPrimary = Color(0xFF003736),
    primaryContainer = Color(0xFF004F4F),
    onPrimaryContainer = Color(0xFFBDEFED),
    secondary = Color(0xFFFFB1C6),
    onSecondary = Color(0xFF5E1131),
    background = Color(0xFF111415),
    surface = Color(0xFF111415),
)

/** The colour each kind of Free day is labelled in. */
data class DayColors(val saturday: Color, val sunday: Color, val extra: Color) {
    fun of(date: LocalDate): Color = when (date.dayOfWeek) {
        DayOfWeek.SATURDAY -> saturday
        DayOfWeek.SUNDAY -> sunday
        else -> extra
    }
}

private val LightDays = DayColors(saturday = Color(0xFF0F7F81), sunday = Color(0xFFB23A62), extra = Color(0xFF9A5B00))
private val DarkDays = DayColors(saturday = Color(0xFF80D5D2), sunday = Color(0xFFFFB1C6), extra = Color(0xFFFFB960))

val LocalDayColors = staticCompositionLocalOf { LightDays }

@Composable
fun WeekendVibesTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDayColors provides if (darkTheme) DarkDays else LightDays) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            content = content,
        )
    }
}
