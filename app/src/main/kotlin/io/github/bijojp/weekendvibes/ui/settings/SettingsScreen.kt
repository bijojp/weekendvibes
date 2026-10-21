package io.github.bijojp.weekendvibes.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
import android.provider.Settings.EXTRA_APP_PACKAGE
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bijojp.weekendvibes.BuildConfig
import io.github.bijojp.weekendvibes.R
import io.github.bijojp.weekendvibes.domain.ThemeMode
import io.github.bijojp.weekendvibes.reminders.Notifications
import io.github.bijojp.weekendvibes.ui.LocalSnackbarHostState
import io.github.bijojp.weekendvibes.ui.Navigator
import io.github.bijojp.weekendvibes.ui.WeekendVibesViewModel
import io.github.bijojp.weekendvibes.ui.components.Backdrop
import io.github.bijojp.weekendvibes.ui.components.CircleIconButton
import io.github.bijojp.weekendvibes.ui.components.ConfirmDialog
import io.github.bijojp.weekendvibes.ui.components.FieldCard
import io.github.bijojp.weekendvibes.ui.components.FieldDivider
import io.github.bijojp.weekendvibes.ui.components.Overline
import io.github.bijojp.weekendvibes.ui.components.VibeTextField
import io.github.bijojp.weekendvibes.ui.components.pressable
import io.github.bijojp.weekendvibes.ui.theme.Vibes

@Composable
fun SettingsScreen(viewModel: WeekendVibesViewModel, nav: Navigator) {
    val state = viewModel.state.collectAsStateWithLifecycle().value ?: return
    val settings = state.settings
    val colors = Vibes.colors
    val context = LocalContext.current
    var canPost by remember { mutableStateOf(Notifications.canPost(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { canPost = Notifications.canPost(context) }
    var homeCity by remember { mutableStateOf(settings.homeCity) }
    var restoreFrom by remember { mutableStateOf<Uri?>(null) }

    val backUp = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) viewModel.backUp(uri)
    }
    val pickBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> restoreFrom = uri }

    Backdrop {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Box(Modifier.padding(top = 8.dp)) {
                CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, null, nav::back)
            }
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.displayMedium,
                color = colors.text,
                modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
            )

            Section(stringResource(R.string.settings_notifications)) {
                ToggleRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = stringResource(R.string.settings_digest),
                    body = stringResource(R.string.settings_digest_body),
                    checked = settings.digestEnabled,
                    onChange = { on -> viewModel.updateSettings { it.copy(digestEnabled = on) } },
                )
                if (!canPost) {
                    FieldDivider()
                    ActionRow(
                        icon = Icons.Rounded.NotificationsOff,
                        title = stringResource(R.string.settings_notifications_off),
                        body = stringResource(R.string.settings_notifications_off_body),
                        onClick = {
                            context.startActivity(Intent(ACTION_APP_NOTIFICATION_SETTINGS).putExtra(EXTRA_APP_PACKAGE, context.packageName))
                        },
                    )
                }
            }

            Section(stringResource(R.string.settings_import)) {
                VibeTextField(
                    value = homeCity,
                    onValueChange = { value ->
                        homeCity = value.take(40)
                        viewModel.updateSettings { it.copy(homeCity = homeCity.trim().ifEmpty { it.homeCity }) }
                    },
                    placeholder = "Bengaluru",
                    label = stringResource(R.string.settings_home_city),
                    icon = Icons.Rounded.LocationCity,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                Text(
                    stringResource(R.string.settings_home_city_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textFaint,
                    modifier = Modifier.padding(start = 52.dp, end = 16.dp, bottom = 14.dp),
                )
            }

            Overline(stringResource(R.string.settings_look), modifier = Modifier.padding(start = 4.dp, top = 28.dp, bottom = 10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    ThemeMode.System to R.string.theme_system,
                    ThemeMode.Dark to R.string.theme_dark,
                    ThemeMode.Light to R.string.theme_light,
                ).forEach { (mode, label) ->
                    ThemeTile(
                        mode = mode,
                        label = stringResource(label),
                        selected = settings.theme == mode,
                        onClick = { viewModel.updateSettings { it.copy(theme = mode) } },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Section(stringResource(R.string.settings_data)) {
                ActionRow(
                    icon = Icons.Rounded.CloudUpload,
                    title = stringResource(R.string.settings_backup),
                    body = stringResource(R.string.settings_backup_body),
                    onClick = { backUp.launch(viewModel.backupFileName()) },
                )
                FieldDivider()
                ActionRow(
                    icon = Icons.Rounded.Restore,
                    title = stringResource(R.string.settings_restore),
                    body = stringResource(R.string.settings_restore_body),
                    onClick = { pickBackup.launch(arrayOf("application/zip", "application/octet-stream")) },
                )
            }

            Overline(stringResource(R.string.settings_about), modifier = Modifier.padding(start = 4.dp, top = 28.dp, bottom = 10.dp))
            Text(
                stringResource(R.string.settings_about_body, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSoft,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.settings_fonts),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textFaint,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        SnackbarHost(LocalSnackbarHostState.current, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp))
    }

    restoreFrom?.let { uri ->
        ConfirmDialog(
            title = stringResource(R.string.restore_title),
            body = stringResource(R.string.restore_body),
            confirm = stringResource(R.string.action_restore),
            onConfirm = { viewModel.restore(uri) },
            onDismiss = { restoreFrom = null },
        )
    }
}

/** A theme choice shown as a tiny screen in that theme; System is half dark, half light. */
@Composable
private fun ThemeTile(mode: ThemeMode, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Vibes.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .background(colors.raised)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.accentMid else colors.hairline, shape)
            .semantics {
                this.selected = selected
                role = Role.RadioButton
            }
            .pressable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            ThemePreview(mode, Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(14.dp)))
            if (selected) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp).clip(CircleShape).background(colors.accentMid),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(14.dp))
                }
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.text else colors.textSoft,
            maxLines = 1,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** A miniature of the app in a theme: a card and the accent button on that theme's canvas. */
@Composable
private fun ThemePreview(mode: ThemeMode, modifier: Modifier = Modifier) {
    val accent = Brush.linearGradient(listOf(Color(0xFFE3FBA8), Color(0xFF7EEBC0), Color(0xFF2CC9B5)))
    Canvas(modifier) {
        fun scene(canvas: Color, card: Color, line: Color) {
            drawRect(canvas)
            drawRoundRect(card, Offset(size.width * 0.12f, size.height * 0.18f), Size(size.width * 0.76f, size.height * 0.38f), CornerRadius(6.dp.toPx()))
            drawRoundRect(line, Offset(size.width * 0.2f, size.height * 0.28f), Size(size.width * 0.42f, size.height * 0.07f), CornerRadius(3.dp.toPx()))
            drawRoundRect(line, Offset(size.width * 0.2f, size.height * 0.42f), Size(size.width * 0.28f, size.height * 0.06f), CornerRadius(3.dp.toPx()))
            drawRoundRect(accent, Offset(size.width * 0.3f, size.height * 0.68f), Size(size.width * 0.4f, size.height * 0.15f), CornerRadius(size.height))
        }
        val dark = { scene(Color(0xFF09090B), Color(0xFF1C1C21), Color(0xFF6F6A64)) }
        val light = { scene(Color(0xFFF3F5F0), Color(0xFFFFFFFF), Color(0xFFC9D1CB)) }
        when (mode) {
            ThemeMode.Dark -> dark()
            ThemeMode.Light -> light()
            ThemeMode.System -> {
                clipRect(right = size.width / 2) { dark() }
                clipRect(left = size.width / 2) { light() }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Overline(title, modifier = Modifier.padding(start = 4.dp, top = 28.dp, bottom = 10.dp))
    FieldCard { content() }
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = Vibes.colors
    Row(
        Modifier.fillMaxWidth().pressable { onChange(!checked) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = colors.textSoft, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.text)
            Text(body, style = MaterialTheme.typography.bodySmall, color = colors.textSoft)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onAccent,
                checkedTrackColor = colors.accentMid,
                uncheckedThumbColor = colors.textFaint,
                uncheckedTrackColor = colors.surface,
                uncheckedBorderColor = colors.hairline,
            ),
        )
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, body: String, onClick: () -> Unit) {
    val colors = Vibes.colors
    Row(
        Modifier.fillMaxWidth().pressable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = colors.textSoft, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.text)
            Text(body, style = MaterialTheme.typography.bodySmall, color = colors.textSoft)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = colors.textFaint)
    }
}
