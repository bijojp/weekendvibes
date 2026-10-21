package io.github.bijojp.weekendvibes

import android.app.Application
import android.content.Context
import io.github.bijojp.weekendvibes.data.WeekendVibesData

class WeekendVibesApplication : Application() {
    val container by lazy { AppContainer(this) }
}

/** Hand-wired dependencies; small enough that a DI framework would only add ceremony. */
class AppContainer(context: Context) {
    private val data by lazy { WeekendVibesData.open(context) }

    val agenda get() = data.agenda
    val settings get() = data.settings
}
