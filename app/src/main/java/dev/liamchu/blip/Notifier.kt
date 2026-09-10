package dev.liamchu.blip

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Posting, and the expiry that DESIGN.md "Purpose and delivery path" turns on:
 * the lifetime rides on the notification itself, so Android clears it whether or
 * not this app is alive (criterion purpose-4).
 */
object Notifier {

    private const val CHANNEL_ID = "blip"
    private const val ID_CONTENT = 1
    private const val ID_FAILURE = 2
    /**
     * A zero expiry means "gone from the phone at once", and it cannot be passed
     * to the platform as zero: Android reads a timeout of 0 as "no timeout at
     * all", which is the exact opposite of what was asked for. The smallest
     * positive value asks it to cancel as soon as it is able.
     *
     * Whether a notification that lives this briefly is still forwarded to the
     * glasses is UNVERIFIED — the listener that forwards it is told when the
     * notification is posted, not when it is cancelled, so it should be, but only
     * the hardware can settle it. Tracked in BACKLOG.md.
     */
    private const val AT_ONCE_MS = 1L

    fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun postContent(context: Context, title: String, text: String, expiryMillis: Long) {
        val notification = builder(context)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setTimeoutAfter(if (expiryMillis <= 0L) AT_ONCE_MS else expiryMillis)
            .setAutoCancel(true)
            .build()
        post(context, ID_CONTENT, notification)
    }

    /**
     * Criterion failure-2: every reason handed in here is written by this app —
     * a status code, a length, an unreachable host. None of it is response body.
     * The failure notice does not expire: it is the only sign the app is broken.
     */
    fun postFailure(context: Context, reason: String) {
        val notification = builder(context)
            .setContentTitle(context.getString(R.string.failure_title))
            .setContentText(reason)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reason))
            .setAutoCancel(true)
            .build()
        post(context, ID_FAILURE, notification)
    }

    private fun builder(context: Context): NotificationCompat.Builder {
        ensureChannel(context)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_blip)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    }

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    // areNotificationsEnabled() is false whenever POST_NOTIFICATIONS is not granted,
    // so the guard below is the permission check; lint cannot see that it is.
    @SuppressLint("MissingPermission")
    private fun post(context: Context, id: Int, notification: Notification) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        manager.notify(id, notification)
    }
}
