package dev.liamchu.blip

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * DESIGN.md "The stack": WorkManager owns the interval, because a repeating job
 * survives Doze only when the operating system is the thing scheduling it. It
 * also re-registers itself after a reboot, which is criterion purpose-5.
 */
object Scheduler {

    private const val WORK_NAME = "blip-poll"

    fun schedule(context: Context, frequencyHours: Int) {
        val request = PeriodicWorkRequestBuilder<BlipWorker>(
            frequencyHours.toLong(),
            TimeUnit.HOURS,
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
}
