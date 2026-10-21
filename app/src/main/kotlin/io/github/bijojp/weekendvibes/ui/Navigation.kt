package io.github.bijojp.weekendvibes.ui

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object TimelineRoute : NavKey

/** Add an Activity, or edit [activityId]. [date] pre-fills a new Activity's day (ISO). */
@Serializable
data class EditorRoute(val activityId: String? = null, val date: String? = null) : NavKey

/** The Timeline is always at the bottom of the back stack. */
class Navigator(private val backStack: NavBackStack<NavKey>) {
    fun open(route: NavKey) {
        backStack.add(route)
    }

    fun back() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
}
