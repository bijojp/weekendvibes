package io.github.bijojp.weekendvibes.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.bijojp.weekendvibes.R
import io.github.bijojp.weekendvibes.ui.WeekendVibesViewModel
import io.github.bijojp.weekendvibes.ui.components.PictureImage
import io.github.bijojp.weekendvibes.ui.components.Pill
import io.github.bijojp.weekendvibes.ui.components.pressable
import io.github.bijojp.weekendvibes.ui.theme.Vibes

/**
 * The Activity's one Picture: a wide cover with Change and Remove, or an invitation to add one.
 * A picked picture is copied straight away; it only becomes permanent when the form is saved.
 */
@Composable
internal fun CoverField(picture: String?, viewModel: WeekendVibesViewModel, onChange: (String?) -> Unit) {
    val colors = Vibes.colors
    val choose = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.importPicture(uri, onImported = onChange)
    }
    val pick = { choose.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    val shape = RoundedCornerShape(28.dp)
    AnimatedContent(picture, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "cover") { current ->
        if (current != null) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(shape)) {
                PictureImage(current, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f))))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                ) {
                    Pill(stringResource(R.string.field_cover_change), onClick = pick)
                    Pill(stringResource(R.string.field_cover_remove), onClick = { onChange(null) })
                }
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .drawBehind {
                        drawRoundRect(
                            color = colors.textFaint.copy(alpha = 0.45f),
                            cornerRadius = CornerRadius(28.dp.toPx()),
                            style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))),
                        )
                    }
                    .clip(shape)
                    .pressable(onClick = pick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, tint = colors.textSoft, modifier = Modifier.size(22.dp))
                Text(stringResource(R.string.field_cover), style = MaterialTheme.typography.titleSmall, color = colors.textSoft, modifier = Modifier.padding(start = 10.dp))
            }
        }
    }
}
