package io.github.bijojp.weekendvibes.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import io.github.bijojp.weekendvibes.domain.Agenda
import io.github.bijojp.weekendvibes.domain.Reminders
import io.github.bijojp.weekendvibes.domain.Settings
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Keeps exactly one alarm set: the next Digest or Reminder, as in Weekly.
 * Alarms are windowed rather than exact, so no exact-alarm permission is needed and one may
 * arrive up to [WINDOW_MILLIS] late.
 */
object ReminderScheduler {
    private const val WINDOW_MILLIS = 10 * 60 * 1000L

    /** Anything that fell due longer ago than this (phone off, alarm lost) is no longer worth a notification. */
    private const val STALE_HOURS = 2L

    private const val PREFS = "reminders"
    private const val LAST_CHECK = "last_check"

    fun schedule(context: Context, agenda: Agenda, settings: Settings) {
        val now = LocalDateTime.now()
        // Looks from the last delivery, so a Digest due a moment ago still goes out, but never further
        // back than deliver() would post. If the clock went back, the last delivery is in the future.
        val after = (lastCheck(context) ?: now.also { setLastCheck(context, it) }).coerceIn(now.minusHours(STALE_HOURS), now)
        val next = Reminders.next(agenda, settings, after)
        val alarms = context.getSystemService(AlarmManager::class.java)
        val intent = alarmIntent(context)
        if (next == null) {
            alarms.cancel(intent)
        } else {
            alarms.setWindow(AlarmManager.RTC_WAKEUP, next.toEpochMillis(), WINDOW_MILLIS, intent)
        }
    }

    /** Posts whatever fell due since the last delivery, then sets the next alarm. */
    fun deliver(context: Context, agenda: Agenda, settings: Settings) {
        val now = LocalDateTime.now()
        val from = maxOf(lastCheck(context) ?: now, now.minusHours(STALE_HOURS))
        Reminders.digestsDue(agenda, settings, from, now).forEach { Notifications.digest(context, it) }
        Reminders.remindersDue(agenda, from, now).forEach { Notifications.reminder(context, it) }
        setLastCheck(context, now)
        schedule(context, agenda, settings)
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_ALARM),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun lastCheck(context: Context): LocalDateTime? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(LAST_CHECK, null)?.let(LocalDateTime::parse)

    private fun setLastCheck(context: Context, time: LocalDateTime) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(LAST_CHECK, time.toString()) }
    }

    private fun LocalDateTime.toEpochMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
