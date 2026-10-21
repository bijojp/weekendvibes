package io.github.bijojp.weekendvibes

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.bijojp.weekendvibes.domain.ThemeMode
import io.github.bijojp.weekendvibes.ui.WeekendVibesApp
import io.github.bijojp.weekendvibes.ui.WeekendVibesViewModel
import io.github.bijojp.weekendvibes.ui.theme.WeekendVibesTheme

class MainActivity : ComponentActivity() {
    private val viewModel: WeekendVibesViewModel by viewModels {
        viewModelFactory {
            initializer {
                val container = (application as WeekendVibesApplication).container
                WeekendVibesViewModel(container.agenda, container.settings, container.pictures, container.backups, container.links, container.updates)
            }
        }
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        askForNotificationsOnce()
        if (savedInstanceState == null) receiveShare(intent)
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val dark = when (state?.settings?.theme) {
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
                else -> isSystemInDarkTheme()
            }
            // Status and navigation bar icons follow the app's theme, not just the system's.
            DisposableEffect(dark) {
                val transparent = Color.Transparent.toArgb()
                val style = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            WeekendVibesTheme(darkTheme = dark) {
                WeekendVibesApp(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        receiveShare(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshToday()
    }

    /** Link import: another app shared text with an event link in it. */
    private fun receiveShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val text = listOfNotNull(intent.getStringExtra(Intent.EXTRA_TEXT), intent.getStringExtra(Intent.EXTRA_SUBJECT)).joinToString(" ")
        viewModel.receiveShared(text)
    }

    /** The Digest is on by default, so the permission is asked for on first launch; Settings links to it later. */
    private fun askForNotificationsOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(ASKED_NOTIFICATIONS, false)) return
        prefs.edit { putBoolean(ASKED_NOTIFICATIONS, true) }
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private companion object {
        const val PREFS = "ui"
        const val ASKED_NOTIFICATIONS = "asked_notifications"
    }
}
