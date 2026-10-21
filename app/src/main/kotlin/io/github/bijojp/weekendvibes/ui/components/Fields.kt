package io.github.bijojp.weekendvibes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.github.bijojp.weekendvibes.ui.theme.Vibes

/** A group of fields on one raised panel, separated by hairlines. */
@Composable
fun FieldCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = Vibes.colors
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.raised)
            .border(1.dp, colors.hairline, shape),
        content = content,
    )
}

@Composable
fun FieldDivider() {
    Box(Modifier.padding(start = 52.dp).fillMaxWidth().height(1.dp).background(Vibes.colors.hairline))
}

/** A borderless text field: optional icon and small caps label, the placeholder in faint text. */
@Composable
fun VibeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: ImageVector? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = Vibes.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = textStyle.copy(color = colors.text),
        cursorBrush = SolidColor(colors.accentMid),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = colors.textSoft, modifier = Modifier.padding(end = 16.dp).size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    if (label != null) {
                        Overline(label, color = colors.textFaint, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Box {
                        if (value.isEmpty()) Text(placeholder, style = textStyle, color = colors.textFaint)
                        inner()
                    }
                }
                if (trailing != null) {
                    Box(Modifier.padding(start = 8.dp)) { trailing() }
                }
            }
        },
    )
}
