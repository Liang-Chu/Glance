package dev.liamchu.glance

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

    private const val CHANNEL_ID = "glance"

    /**
     * A slot per watcher, so two watchers never overwrite each other's
     * notification — and a watcher's own next notification does replace its last,
     * which is what keeps the shade from filling up.
     */
    private const val CONTENT_TAG = "content"
    private const val FAILURE_TAG = "failure"

    fun canPost(context: Context): Boolean {
        val manager = NotificationManagerCompat.from(context)
        return manager.areNotificationsEnabled() &&
            manager.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    /**
     * The header line on the glasses is the app label — one per install, and not
     * ours to vary per notification. What is ours is the title beneath it, which
     * carries the backend's words, and the sub-text, which names the watcher.
     * Whether a listener forwards sub-text is up to the listener; it costs a line
     * to try and nothing if it is ignored.
     */
    fun postContent(context: Context, watcher: Watcher, title: String, text: String) {
        val notification = builder(context)
            .setSubText(watcher.name)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            // Never zero: Android reads a timeout of 0 as "no timeout at all".
            // expiryProblem refuses anything under the floor, so this is always positive.
            .setTimeoutAfter(watcher.expiryMillis)
            .setAutoCancel(true)
            .build()
        post(context, CONTENT_TAG, watcher.id, notification)
    }

    /**
     * Criterion failure-2: every reason handed in here is written by this app —
     * a status code, a length, an unreachable host. None of it is response body.
     * The failure notice does not expire: it is the only sign the app is broken.
     */
    fun postFailure(context: Context, watcher: Watcher, reason: String, source: FailureSource) {
        val notification = builder(context)
            .setContentTitle(context.getString(if (source == FailureSource.REGISTRATION)
                R.string.registration_failure_title else R.string.failure_title, watcher.name))
            .setContentText(reason)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reason))
            .setAutoCancel(true)
            .build()
        post(context, FAILURE_TAG, watcher.id, notification)
    }

    /** A deleted watcher must not leave its notifications behind. */
    fun clear(context: Context, watcherId: Int) {
        val manager = NotificationManagerCompat.from(context)
        manager.cancel(CONTENT_TAG, watcherId)
        clearFailure(context, watcherId)
    }

    fun clearFailure(context: Context, watcherId: Int) {
        NotificationManagerCompat.from(context).cancel(FAILURE_TAG, watcherId)
    }

    private fun builder(context: Context): NotificationCompat.Builder {
        ensureChannel(context)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_glance)
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
    private fun post(context: Context, tag: String, id: Int, notification: Notification) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        manager.notify(tag, id, notification)
    }
}
