package dev.liamchu.blip

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * DESIGN.md "The stack": WorkManager owns the interval, because a repeating job
 * survives Doze only when the operating system is the thing scheduling it. It
 * also re-registers itself after a reboot, which is criterion purpose-5.
 *
 * The interval arrives already in minutes and already checked against
 * WorkManager's floor — see intervalProblem in Settings.kt. Nothing here clamps
 * it, because a clamp is a silent lie about how often the user will be told.
 */
object Scheduler {

    private const val WORK_NAME = "blip-poll"
    private const val NOW_WORK_NAME = "blip-now"

    fun schedule(context: Context, intervalMinutes: Long) {
        val request = PeriodicWorkRequestBuilder<BlipWorker>(
            intervalMinutes,
            TimeUnit.MINUTES,
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()

        // UPDATE so criterion settings-4 holds: a new frequency reaches the next
        // run without a reinstall, and without restarting the period from zero.
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /**
     * One run, immediately. The floor on repeating work is fifteen minutes, which
     * makes "does this work at all" a fifteen-minute question; this makes it a
     * ten-second one. One-shot work has no floor.
     *
     * It does not touch the repeating schedule — that is still the thing that has
     * to be proven, and a check that ran because a button was pressed proves the
     * call and the notification, not the waking up.
     */
    fun checkNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<BlipWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            ).build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(NOW_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}
