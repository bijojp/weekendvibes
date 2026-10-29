package io.github.bijojp.weekendvibes

import android.app.Application
import android.content.Context
import io.github.bijojp.weekendvibes.data.WeekendVibesData
import io.github.bijojp.weekendvibes.imports.LinkImporter
import io.github.bijojp.weekendvibes.reminders.Notifications
import io.github.bijojp.weekendvibes.reminders.ReminderScheduler
import io.github.bijojp.weekendvibes.updates.UpdateAlerts
import io.github.bijojp.weekendvibes.updates.UpdateChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class WeekendVibesApplication : Application() {
    val container by lazy { AppContainer(this) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        // Any change to Activities, Extra days or Settings plans the next alarm again.
        scope.launch {
            combine(container.agenda.agenda, container.settings.settings, ::Pair).collectLatest { (agenda, settings) ->
                delay(SETTLE_MILLIS)
                ReminderScheduler.schedule(this@WeekendVibesApplication, agenda, settings)
            }
        }
        // The daily check for a new version follows its switch in Settings.
        scope.launch {
            container.settings.settings.map { it.updateAlerts }.distinctUntilChanged().collect { on ->
                UpdateAlerts.schedule(this@WeekendVibesApplication, on)
            }
        }
    }

    private companion object {
        /** Lets a burst of changes settle before planning alarms. */
        const val SETTLE_MILLIS = 500L
    }
}

/** Hand-wired dependencies; small enough that a DI framework would only add ceremony. */
class AppContainer(context: Context) {
    private val data by lazy { WeekendVibesData.open(context, BuildConfig.VERSION_NAME) }

    val agenda get() = data.agenda
    val settings get() = data.settings
    val pictures get() = data.pictures
    val backups get() = data.backups
    val links by lazy { LinkImporter(context, data.pictures) }
    val updates by lazy { UpdateChecker(BuildConfig.VERSION_NAME) }
}
