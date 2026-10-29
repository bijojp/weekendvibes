package io.github.bijojp.weekendvibes.reminders

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import io.github.bijojp.weekendvibes.MainActivity
import io.github.bijojp.weekendvibes.R
import io.github.bijojp.weekendvibes.domain.Activity
import io.github.bijojp.weekendvibes.domain.Release
import io.github.bijojp.weekendvibes.domain.Reminders
import io.github.bijojp.weekendvibes.ui.components.formatTime

/** The three kinds of notification: the Digest before a Break, a Reminder for one timed Activity, and an Update alert. */
object Notifications {
    private const val CHANNEL_DIGEST = "digest"
    private const val CHANNEL_REMINDERS = "reminders"
    private const val CHANNEL_UPDATES = "updates"

    fun createChannels(context: Context) {
        NotificationManagerCompat.from(context).createNotificationChannelsCompat(
            listOf(
                NotificationChannelCompat.Builder(CHANNEL_DIGEST, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName(context.getString(R.string.channel_digest))
                    .setDescription(context.getString(R.string.channel_digest_description))
                    .build(),
                NotificationChannelCompat.Builder(CHANNEL_REMINDERS, NotificationManagerCompat.IMPORTANCE_HIGH)
                    .setName(context.getString(R.string.channel_reminders))
                    .setDescription(context.getString(R.string.channel_reminders_description))
                    .build(),
                NotificationChannelCompat.Builder(CHANNEL_UPDATES, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName(context.getString(R.string.channel_updates))
                    .setDescription(context.getString(R.string.channel_updates_description))
                    .build(),
            ),
        )
    }

    fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() && hasPermission(context)

    fun digest(context: Context, digest: Reminders.Digest) {
        val lines = digest.activities.map { activity ->
            listOfNotNull(activity.time?.let { formatTime(context, it) }, activity.title).joinToString("  ")
        }
        val title = when {
            digest.span.isLongWeekend -> context.getString(R.string.digest_title_long)
            digest.span.isWeekend -> context.getString(R.string.digest_title_weekend)
            else -> context.getString(R.string.digest_title_day_off)
        }
        val text = if (lines.isEmpty()) {
            context.getString(R.string.digest_free)
        } else {
            context.resources.getQuantityString(R.plurals.digest_planned, lines.size, lines.size)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_DIGEST)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(if (lines.isEmpty()) text else lines.joinToString(" · "))
            .setStyle(
                if (lines.isEmpty()) {
                    NotificationCompat.BigTextStyle().bigText(text)
                } else {
                    NotificationCompat.InboxStyle().setSummaryText(text).also { style -> lines.forEach(style::addLine) }
                },
            )
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .build()
        post(context, "digest/${digest.span.start}".hashCode(), notification)
    }

    fun reminder(context: Context, activity: Activity) {
        val time = activity.time?.let { formatTime(context, it) }
        val text = listOfNotNull(time, activity.location.takeIf { it.isNotEmpty() }).joinToString(" · ")
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(activity.title)
            .setContentText(text)
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .build()
        post(context, "reminder/${activity.id.value}/${activity.date}".hashCode(), notification)
    }

    /** Tapping it opens the release's page in the browser, where the APK is downloaded as with Check for updates. */
    fun update(context: Context, release: Release) {
        val openPage = PendingIntent.getActivity(
            context,
            0,
            Intent(Intent.ACTION_VIEW, release.page.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.update_alert_title, release.version))
            .setContentText(context.getString(R.string.update_alert_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.update_alert_body)))
            .setContentIntent(openPage)
            .setAutoCancel(true)
            .build()
        // One id for all of them, so a newer release replaces an alert that was never opened.
        post(context, "update".hashCode(), notification)
    }

    private fun post(context: Context, id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE,
    )
}
