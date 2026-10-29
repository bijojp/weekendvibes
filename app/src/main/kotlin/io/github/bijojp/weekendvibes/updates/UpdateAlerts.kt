package io.github.bijojp.weekendvibes.updates

import android.content.Context
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.bijojp.weekendvibes.WeekendVibesApplication
import io.github.bijojp.weekendvibes.domain.Updates
import io.github.bijojp.weekendvibes.reminders.Notifications
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * Update alerts (ADR 0002): about once a day, while the phone is online, the same request Check for updates makes.
 * A newer release gets one notification; WorkManager keeps the schedule across reboots and app updates.
 */
object UpdateAlerts {
    private const val WORK = "update-alerts"
    private const val PREFS = "updates"
    private const val LAST_ALERTED = "last_alerted"

    /** Starts the daily check when [enabled], or stops it. Calling it again keeps the schedule already running. */
    fun schedule(context: Context, enabled: Boolean) {
        val work = WorkManager.getInstance(context)
        if (!enabled) {
            work.cancelUniqueWork(WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<CheckWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        work.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    class CheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val container = (applicationContext as WeekendVibesApplication).container
            if (!container.settings.settings.first().updateAlerts) return Result.success()
            // A failed check waits for tomorrow's instead of retrying, so it stays at one request a day.
            val release = try {
                container.updates.newerRelease()
            } catch (e: IOException) {
                null
            } ?: return Result.success()
            val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (Updates.needsAlert(release.version, prefs.getString(LAST_ALERTED, null))) {
                Notifications.update(applicationContext, release)
                prefs.edit { putString(LAST_ALERTED, release.version) }
            }
            return Result.success()
        }
    }
}
