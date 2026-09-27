package dev.liamchu.glance

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/** Initialize before WorkManager or message services need the user's default Firebase app. */
class GlanceApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // Install before providers, Firebase and DataStore startup can fail.
        Diagnostics.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        // Required before any service callback; the small local read is dispatched off the UI thread.
        val config = runBlocking(Dispatchers.IO) { WatcherStore.firebaseConfig(this@GlanceApplication) }
        FirebaseRuntime.initialize(this, config)
        Scheduler.cancelObsoleteWork(this)
        Diagnostics.event(DiagnosticEvent.APP_READY)
    }
}

object FirebaseRuntime {
    @Volatile private var active: FirebaseConfig? = null
    @Volatile private var selected: FirebaseConfig? = null

    val ready: Boolean get() = active != null && active == selected
    val restartRequired: Boolean get() = active != null && active != selected
    val senderId: String? get() = if (ready) active?.senderId else null
    val projectId: String? get() = if (ready) active?.projectId else null

    @Synchronized
    fun initialize(context: Context, config: FirebaseConfig?) {
        selected = config
        if (config == null) {
            Diagnostics.event(DiagnosticEvent.FIREBASE_NOT_CONFIGURED)
            return
        }
        if (active != null) return
        FirebaseApp.initializeApp(context.applicationContext, FirebaseOptions.Builder()
            .setProjectId(config.projectId).setApplicationId(config.applicationId)
            .setGcmSenderId(config.senderId).setApiKey(config.apiKey).build())
        active = config
        Diagnostics.event(DiagnosticEvent.FIREBASE_READY)
    }

    /** SDK components retain their project for the process lifetime. Never swap them in place. */
    @Synchronized
    fun select(context: Context, config: FirebaseConfig) {
        selected = config
        if (active == null) initialize(context, config)
        else if (restartRequired) {
            FirebaseMessaging.getInstance().isAutoInitEnabled = false
            Diagnostics.event(DiagnosticEvent.FIREBASE_RESTART_REQUIRED)
        }
    }
}
