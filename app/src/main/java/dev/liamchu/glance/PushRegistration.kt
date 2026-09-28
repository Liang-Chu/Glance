package dev.liamchu.glance

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailabilityLight
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.installations.FirebaseInstallations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object PushRegistration {
    private const val KEY_FORCE = "force_registration"

    fun unavailableReason(context: Context): String? {
        if (FirebaseRuntime.restartRequired) return "FIREBASE PROJECT CHANGED. FINISH THE RESTART IN FIREBASE SETUP."
        if (!FirebaseRuntime.ready) return "IMPORT GOOGLE-SERVICES.JSON IN FIREBASE SETUP FIRST."
        if (GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS) {
            return "PUSH NEEDS AVAILABLE, UP-TO-DATE GOOGLE PLAY SERVICES."
        }
        return null
    }

    private fun name(id: Int) = "glance-register-" + id

    fun enqueue(context: Context, id: Int, policy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE,
        force: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<PushRegistrationWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(workDataOf(Scheduler.KEY_WATCHER_ID to id, KEY_FORCE to force))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(name(id), policy, request)
    }

    suspend fun enqueueMissing(context: Context) = enqueueMissing(WatcherStore.all(context)) {
        enqueue(context, it, ExistingWorkPolicy.KEEP)
    }

    internal fun enqueueMissing(watchers: List<Watcher>, enqueue: (Int) -> Unit) {
        watchers.filter { it.needsPushRegistration }.forEach { enqueue(it.id) }
    }

    internal fun isForced(data: androidx.work.Data): Boolean = data.getBoolean(KEY_FORCE, false)

    fun cancel(context: Context, id: Int) {
        WorkManager.getInstance(context).cancelUniqueWork(name(id))
    }

    suspend fun enqueueRemovals(context: Context) {
        WatcherStore.retired(context).forEach { enqueueRemoval(context, it.pushKey) }
    }

    private fun enqueueRemoval(context: Context, key: String) {
        val request = OneTimeWorkRequestBuilder<PushUnregisterWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            // Keep credentials out of WorkManager's input Data (also capped at 10 KiB).
            .setInputData(workDataOf("subscription_id" to key))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("glance-unregister-" + key,
            ExistingWorkPolicy.KEEP, request)
    }

    suspend fun updateAutoInit(context: Context) = updateAutoInit(
        ready = { FirebaseRuntime.ready },
        readWatchers = { WatcherStore.all(context) },
        setEnabled = { FirebaseMessaging.getInstance().isAutoInitEnabled = it },
    )

    /** Dependencies keep the SDK's thread requirement testable without contacting Firebase. */
    internal suspend fun updateAutoInit(
        ready: () -> Boolean,
        readWatchers: suspend () -> List<Watcher>,
        setEnabled: (Boolean) -> Unit,
    ) = withContext(Dispatchers.IO) {
        // With FID enabled, the SDK setter can call Tasks.await internally. A suspend caller
        // alone does not leave Main; save/delete/import must move this entire operation off UI.
        if (ready()) setEnabled(readWatchers().any { it.isPush })
    }
}

/** Retiring a subscription retains only the information needed to tell its old backend. */
class PushUnregisterWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = Diagnostics.worker(DiagnosticEvent.REMOVE_WORK,
        null, runAttemptCount) { run() }

    private suspend fun run(): Result {
        val key = inputData.getString("subscription_id") ?: return Result.failure()
        val watcher = WatcherStore.retired(applicationContext).firstOrNull { it.pushKey == key }
            ?: return Result.success()
        val removed = Backend.unregister(watcher) == Outcome.NothingToSay
        if (!removed && runAttemptCount < 4) {
            Diagnostics.event(DiagnosticEvent.REMOVAL_RETRY, watcher.id, runAttemptCount)
            return Result.retry()
        }
        // Bound retention of retired credentials as well as retries.
        WatcherStore.forgetRetired(applicationContext, key)
        Diagnostics.event(if (removed) DiagnosticEvent.REMOVAL_DONE else DiagnosticEvent.REMOVAL_GAVE_UP, watcher.id)
        return if (removed) Result.success() else Result.failure()
    }
}

class PushRegistrationWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = Diagnostics.worker(DiagnosticEvent.REGISTER_WORK,
        inputData.getInt(Scheduler.KEY_WATCHER_ID, -1), runAttemptCount) { run() }

    private suspend fun run(): Result {
        val context = applicationContext
        val id = inputData.getInt(Scheduler.KEY_WATCHER_ID, -1)
        val watcher = WatcherStore.byId(context, id) ?: return Result.success()
        // Also discard previously queued automatic refreshes after an app update or another success.
        if (!watcher.shouldRegisterPush(PushRegistration.isForced(inputData))) return Result.success()
        if (!FirebaseRuntime.ready) {
            Diagnostics.event(DiagnosticEvent.REGISTRATION_SKIPPED, id)
            return Result.success() // Import/restart enqueues registration again.
        }
        val projectId = FirebaseRuntime.projectId ?: return Result.success()
        val problem = PushRegistration.unavailableReason(context)
        if (problem != null) {
            Diagnostics.event(DiagnosticEvent.REGISTRATION_SKIPPED, id)
            Delivery.show(context, watcher, Outcome.Failed(problem), FailureSource.REGISTRATION)
            return Result.failure()
        }
        val (result, address) = withContext(Dispatchers.IO) {
            try {
                val messaging = FirebaseMessaging.getInstance()
                messaging.isAutoInitEnabled = true
                Tasks.await(messaging.register(), 30, TimeUnit.SECONDS)
                val address = Tasks.await(FirebaseInstallations.getInstance().id, 30, TimeUnit.SECONDS)
                Backend.register(watcher, address, projectId) to address
            } catch (e: ExecutionException) {
                Diagnostics.failure(DiagnosticEvent.FCM_REGISTRATION_FAILED, e, id)
                Outcome.Failed("Could not obtain the push address. Check connectivity and Firebase setup.") to null
            } catch (e: TimeoutException) {
                Diagnostics.failure(DiagnosticEvent.FCM_REGISTRATION_FAILED, e, id)
                Outcome.Failed("Timed out obtaining the push address.") to null
            } catch (e: InterruptedException) {
                Diagnostics.failure(DiagnosticEvent.FCM_REGISTRATION_FAILED, e, id)
                Thread.currentThread().interrupt()
                return@withContext Outcome.Failed("Push registration was interrupted.") to null
            }
        }
        val current = WatcherStore.byId(context, id) ?: return Result.success()
        if (!current.sameConfiguration(watcher)) return Result.success()
        if (result == Outcome.NothingToSay) {
            checkNotNull(address)
            WatcherStore.recordPushRegistration(context, watcher, address)
            Diagnostics.event(DiagnosticEvent.REGISTRATION_DONE, id)
            return Result.success()
        }
        Delivery.show(context, current, result, FailureSource.REGISTRATION)
        // Bound retries; the user can retry registration by saving the watcher.
        Diagnostics.event(if (runAttemptCount < 4) DiagnosticEvent.REGISTRATION_RETRY
            else DiagnosticEvent.REGISTRATION_GAVE_UP, id, runAttemptCount)
        return if (runAttemptCount < 4) Result.retry() else Result.failure()
    }
}
