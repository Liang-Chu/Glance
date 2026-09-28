package dev.liamchu.glance

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import kotlinx.coroutines.CancellationException
import java.io.File
import java.time.Instant
import kotlin.system.exitProcess

/** Only fixed event names and numeric metadata can enter normal logs. */
enum class DiagnosticEvent {
    APP_START, APP_READY, FIREBASE_READY, FIREBASE_NOT_CONFIGURED, FIREBASE_RESTART_REQUIRED,
    CONFIG_IMPORTED, CONFIG_IMPORT_FAILED, POLL_WORK, REGISTER_WORK, REMOVE_WORK,
    WORK_FINISHED, WORK_CANCELLED, WORK_EXCEPTION, HTTP_POLL, HTTP_REGISTER, HTTP_REMOVE,
    HTTP_STATUS, HTTP_EXCEPTION, INVALID_URL, INVALID_CREDENTIAL, RESPONSE_TOO_LARGE,
    INVALID_JSON, INVALID_CONTENT_TYPE, MISSING_CONTENT, EMPTY_CONTENT, TITLE_TOO_LONG, TEXT_TOO_LONG,
    FCM_REGISTRATION_FAILED, REGISTRATION_SKIPPED, REGISTRATION_DONE, REGISTRATION_RETRY, REGISTRATION_FAILED,
    REGISTRATION_GAVE_UP, REMOVAL_DONE, REMOVAL_RETRY, REMOVAL_GAVE_UP,
    PUSH_RECEIVED, PUSH_IGNORED_PROJECT, PUSH_IGNORED_SUBSCRIPTION, PUSH_ADDRESS_CHANGED,
    PUSH_MESSAGES_LOST, NOTIFICATIONS_DISABLED, CONTENT_POSTED, DELIVERY_FAILED,
    EXPORT_FAILED, SETTINGS_WRITE_FAILED, SETTINGS_SAVE_STARTED, SETTINGS_SAVED, SETTINGS_SAVE_FINISHED,
}

object Diagnostics {
    @Volatile private var files: DiagnosticFiles? = null
    @Volatile var writeFailed: Boolean = false
        private set

    @Suppress("DEPRECATION") // Thread.id remains available on API 26; threadId() requires a newer runtime.
    fun install(context: Context) {
        if (files != null) return
        val store = DiagnosticFiles(File(context.noBackupFilesDir, "diagnostics"))
        files = store
        val original = Thread.getDefaultUncaughtExceptionHandler() ?: Thread.UncaughtExceptionHandler { _, _ ->
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
        Thread.setDefaultUncaughtExceptionHandler(RecordingExceptionHandler(original, { thread, error ->
            store.saveCrash("${Instant.now()} UNCAUGHT_EXCEPTION\n${deviceHeader()}" +
                "thread_id=${thread.id}\n" + diagnosticTrace(error))
        }, { markWriteFailure() }))
        write { it.append(recordHeader(DiagnosticEvent.APP_START, null, null) + "\n" + deviceHeader()) }
    }

    fun event(event: DiagnosticEvent, watcherId: Int? = null, code: Int? = null) {
        write { it.append(recordHeader(event, watcherId, code)) }
    }

    fun failure(event: DiagnosticEvent, error: Throwable, watcherId: Int? = null) {
        write { it.append(recordHeader(event, watcherId, null) + "\n" + diagnosticTrace(error)) }
    }

    suspend fun <T> worker(started: DiagnosticEvent, id: Int?, attempt: Int, block: suspend () -> T): T {
        event(started, id, attempt)
        try {
            return block().also { event(DiagnosticEvent.WORK_FINISHED, id) }
        } catch (e: CancellationException) {
            event(DiagnosticEvent.WORK_CANCELLED, id)
            throw e
        } catch (e: Throwable) {
            // WorkManager can capture Errors too. Never let recording replace the original failure.
            try { failure(DiagnosticEvent.WORK_EXCEPTION, e, id) }
            finally { throw e }
        }
    }

    fun hasCrash(): Boolean = files?.hasCrash() == true

    fun clear() {
        checkNotNull(files).clear()
        writeFailed = false
    }

    /** Caller runs on IO; throws if local files cannot be read, never claims an empty export worked. */
    fun report(context: Context): String = buildString {
        append("GLANCE DIAGNOSTICS\nExported: ${Instant.now()}\n").append(deviceHeader())
        append("Firebase ready=${FirebaseRuntime.ready}; restart required=${FirebaseRuntime.restartRequired}\n")
        append("Notifications allowed=${Notifier.canPost(context)}; logging write failed=$writeFailed\n")
        append("No credentials, URLs, Firebase IDs, watcher names or notification content are recorded.\n")
        append("Exception messages and raw OS traces are excluded; stack frames are retained.\n\n")
        append(checkNotNull(files).snapshot())
        append("\nRECENT ANDROID PROCESS EXITS (may include earlier app versions)\n")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            append("Unavailable before Android 11.\n")
        } else try {
            val exits = context.getSystemService(ActivityManager::class.java)
                .getHistoricalProcessExitReasons(context.packageName, 0, 5)
            if (exits.isEmpty()) append("No exit records provided by Android.\n")
            exits.forEach {
                append("${Instant.ofEpochMilli(it.timestamp)} reason=${exitReason(it.reason)}(${it.reason})")
                append(" status=${it.status} importance=${it.importance} pss_kb=${it.pss} rss_kb=${it.rss}\n")
            }
        } catch (_: RuntimeException) {
            append("Android exit history unavailable on this device.\n")
        }
        append("\nNative crashes, ANRs, abrupt kills and storage failures may have no saved JVM stack.\n")
    }

    private fun recordHeader(event: DiagnosticEvent, id: Int?, code: Int?): String = buildString {
        append(Instant.now()).append(' ').append(event.name)
        if (id != null) append(" watcher_id=").append(id)
        if (code != null) append(" code=").append(code)
    }

    private fun deviceHeader(): String = "app=${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE}) " +
        "build_type=${BuildConfig.BUILD_TYPE} sdk=${Build.VERSION.SDK_INT}\n" +
        "device=${diagnosticSymbol(Build.MANUFACTURER)} ${diagnosticSymbol(Build.MODEL)}\n"

    private fun write(action: (DiagnosticFiles) -> Unit) {
        val store = files ?: return // Not installed in pure JVM tests or before attachBaseContext.
        try { action(store) }
        catch (_: Exception) { markWriteFailure() }
    }

    private fun markWriteFailure() {
        writeFailed = true
        Log.e("Glance", "Could not persist local diagnostics; original app behavior is unchanged.")
    }

    private fun exitReason(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_CRASH -> "JVM_CRASH"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "NATIVE_CRASH"
        ApplicationExitInfo.REASON_ANR -> "ANR"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INITIALIZATION_FAILURE"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_REQUESTED"
        ApplicationExitInfo.REASON_USER_STOPPED -> "USER_STOPPED"
        ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
        ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF"
        ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "PERMISSION_CHANGE"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE_USAGE"
        else -> "OTHER_OR_UNKNOWN"
    }
}
