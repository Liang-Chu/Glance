package dev.liamchu.glance

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.Executors

class PushAutoInitTest {
    // The exported 1.3 crash came from Tasks.await -> checkNotMainThread inside setAutoInitEnabled.
    // Model that SDK boundary on a dedicated UI dispatcher; no live project or network is needed.
    @Test fun `save can enable auto init from UI and then return to the same UI thread`() = onUi {
        val uiThread = Thread.currentThread()
        val order = mutableListOf<String>()
        val stored = listOf(Watcher.blank(7).copy(pushKey = "test-subscription"))
        order += "settings saved"
        PushRegistration.updateAutoInit(
            ready = { true },
            readWatchers = {
                yield() // DataStore suspension must not put the SDK call back on the UI dispatcher.
                stored
            },
            setEnabled = {
                check(Thread.currentThread() !== uiThread) { "Must not be called on the main application thread" }
                assertTrue(it)
                order += "SDK enabled"
            },
        )
        assertSame(uiThread, Thread.currentThread())
        order += "return to watcher list"
        assertEquals(listOf("settings saved", "SDK enabled", "return to watcher list"), order)
    }

    @Test fun `deleting last push watcher disables auto init off UI including mixed watcher lists`() = onUi {
        val uiThread = Thread.currentThread()
        val poll = Watcher.blank(1).copy(delivery = Watcher.POLL)
        listOf(emptyList(), listOf(poll), listOf(poll, Watcher.blank(2))).forEach { watchers ->
            var enabled: Boolean? = null
            PushRegistration.updateAutoInit({ true }, { watchers }) {
                assertNotSame(uiThread, Thread.currentThread())
                enabled = it
            }
            assertEquals(watchers.any { it.isPush }, enabled)
            assertSame(uiThread, Thread.currentThread())
        }
    }

    @Test fun `unconfigured or restarting Firebase never reads watchers or calls the SDK`() = onUi {
        PushRegistration.updateAutoInit({ false }, {
            error("Must not read settings for an unavailable project")
        }, { error("Must not initialize an unavailable project") })
    }

    @Test fun `store cancellation is preserved and never enables messaging`() = onUi {
        val cancellation = CancellationException("cancelled")
        try {
            PushRegistration.updateAutoInit({ true }, { throw cancellation }, {
                fail("SDK must not run after a cancelled read")
            })
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            // Coroutine debug stack recovery may copy exceptions when crossing dispatchers.
            assertTrue(actual === cancellation || actual.cause === cancellation)
        }
    }

    @Test fun `other SDK failures propagate rather than being reported as successful saves`() = onUi {
        val failure = IllegalStateException("SDK unavailable")
        try {
            PushRegistration.updateAutoInit({ true }, { listOf(Watcher.blank(1)) }, { throw failure })
            fail("SDK failure must propagate")
        } catch (actual: IllegalStateException) {
            assertTrue(actual === failure || actual.cause === failure)
        }
    }

    private fun onUi(block: suspend () -> Unit) {
        Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "regression-ui") }
            .asCoroutineDispatcher().use { dispatcher -> runBlocking(dispatcher) { block() } }
    }
}
