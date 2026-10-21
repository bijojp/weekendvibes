package io.github.bijojp.weekendvibes.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.bijojp.weekendvibes.WeekendVibesApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Wakes for the next alarm, and for anything that clears alarms (reboot, app update, clock changes). */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as WeekendVibesApplication).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val agenda = container.agenda.agenda.first()
                val settings = container.settings.settings.first()
                when (intent.action) {
                    ACTION_ALARM -> ReminderScheduler.deliver(context, agenda, settings)
                    else -> ReminderScheduler.schedule(context, agenda, settings)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_ALARM = "io.github.bijojp.weekendvibes.ALARM"
    }
}
