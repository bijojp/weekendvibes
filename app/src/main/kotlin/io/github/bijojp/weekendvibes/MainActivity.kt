package io.github.bijojp.weekendvibes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
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
                WeekendVibesViewModel(container.agenda, container.settings)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val dark = when (state?.settings?.theme) {
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
                else -> isSystemInDarkTheme()
            }
            WeekendVibesTheme(darkTheme = dark) {
                WeekendVibesApp(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshToday()
    }
}
