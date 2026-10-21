package io.github.bijojp.weekendvibes.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalResources
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import io.github.bijojp.weekendvibes.ui.editor.EditorScreen
import io.github.bijojp.weekendvibes.ui.timeline.TimelineScreen

val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }

@Composable
fun WeekendVibesApp(viewModel: WeekendVibesViewModel) {
    val backStack = rememberNavBackStack(TimelineRoute)
    val nav = remember(backStack) { Navigator(backStack) }
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(viewModel, resources) {
        viewModel.messages.collect { message ->
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(resources.getString(message))
        }
    }

    CompositionLocalProvider(LocalSnackbarHostState provides snackbar) {
        NavDisplay(
            backStack = backStack,
            onBack = nav::back,
            entryProvider = entryProvider {
                entry<TimelineRoute> { TimelineScreen(viewModel, nav) }
                entry<EditorRoute> { route -> EditorScreen(route, viewModel, nav) }
            },
        )
    }
}
