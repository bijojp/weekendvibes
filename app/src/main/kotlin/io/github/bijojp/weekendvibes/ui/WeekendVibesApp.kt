package io.github.bijojp.weekendvibes.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import io.github.bijojp.weekendvibes.ui.components.LocalPictureFiles
import io.github.bijojp.weekendvibes.ui.editor.EditorScreen
import io.github.bijojp.weekendvibes.ui.settings.SettingsScreen
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
    LaunchedEffect(viewModel) {
        viewModel.sharedLinks.collect { link -> nav.open(EditorRoute(importLink = link)) }
    }

    CompositionLocalProvider(
        LocalSnackbarHostState provides snackbar,
        LocalPictureFiles provides viewModel::pictureFile,
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = nav::back,
            // Screens rise into place and settle back, rather than sliding sideways.
            transitionSpec = { (fadeIn() + slideInVertically { it / 12 }) togetherWith fadeOut() },
            popTransitionSpec = { fadeIn() togetherWith (fadeOut() + slideOutVertically { it / 12 }) },
            predictivePopTransitionSpec = { fadeIn() togetherWith (fadeOut() + slideOutVertically { it / 12 }) },
            entryProvider = entryProvider {
                entry<TimelineRoute> { TimelineScreen(viewModel, nav) }
                entry<SettingsRoute> { SettingsScreen(viewModel, nav) }
                entry<EditorRoute> { route -> EditorScreen(route, viewModel, nav) }
            },
        )
    }
}
