package dev.liamchu.glance

import androidx.work.WorkInfo
import org.junit.Assert.*
import org.junit.Test

class ConnectionFeedbackTest {
    private fun watcher() = Watcher.blank(1).copy(
        name = "My alerts", url = "https://backend.example/api/glance", credential = "test-key",
        pushKey = "test-subscription", pushAddress = "test-address", pushRegistered = true,
    )

    private fun confirmed(expected: Watcher, current: Watcher?, reported: Boolean,
        hash: String? = registrationAddressHash("test-address")): Boolean =
        registrationConfirmed(expected, current, reported, hash)

    @Test fun `request not observed yet is connecting without claiming success`() {
        assertEquals(ConnectionPhase.CONNECTING, connectionPhase(null, true, null, 0))
    }

    @Test fun `queued or blocked first attempt waits for network or scheduling`() {
        listOf(WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED).forEach {
            assertEquals(ConnectionPhase.WAITING, connectionPhase(it, false, null, 0))
            // A previously confirmed subscription cannot turn waiting work into new success.
            assertEquals(ConnectionPhase.WAITING, connectionPhase(it, true, null, 0))
        }
    }

    @Test fun `running attempt without failure reason shows connecting`() {
        assertEquals(ConnectionPhase.CONNECTING, connectionPhase(WorkInfo.State.RUNNING, true, null, 0))
        assertEquals(ConnectionPhase.CONNECTING, connectionPhase(WorkInfo.State.RUNNING, false, "", 1))
    }

    @Test fun `queued retry keeps failed attempt visible while awaiting network`() {
        listOf(WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED).forEach {
            assertEquals(ConnectionPhase.RETRYING, connectionPhase(it, true, "backend unreachable", 1))
            assertEquals(ConnectionPhase.RETRYING, connectionPhase(it, false, null, 4))
        }
    }

    @Test fun `running work with a recorded failure reason remains retrying`() {
        assertEquals(ConnectionPhase.RETRYING,
            connectionPhase(WorkInfo.State.RUNNING, true, "key rejected", 0))
    }

    @Test fun `successful request becomes connected only with current confirmation`() {
        val saved = watcher()
        assertEquals(ConnectionPhase.CONNECTED, connectionPhase(WorkInfo.State.SUCCEEDED,
            confirmed(saved, saved, true), null, 0))
    }

    @Test fun `successful but skipped work cannot claim a connection`() {
        val saved = watcher()
        assertEquals(ConnectionPhase.FAILED, connectionPhase(WorkInfo.State.SUCCEEDED,
            confirmed(saved, saved, false), null, 0))
    }

    @Test fun `failed request remains failed despite a previous registration`() {
        assertEquals(ConnectionPhase.FAILED,
            connectionPhase(WorkInfo.State.FAILED, true, "backend unreachable", 4))
    }

    @Test fun `cancelled request remains cancelled despite a previous registration`() {
        assertEquals(ConnectionPhase.CANCELLED,
            connectionPhase(WorkInfo.State.CANCELLED, true, null, 0))
    }

    @Test fun `previous registration alone does not confirm this save`() {
        val saved = watcher()
        assertFalse(confirmed(saved, saved, reported = false))
    }

    @Test fun `deleted watcher cannot inherit a completed request`() {
        assertFalse(confirmed(watcher(), null, reported = true))
    }

    @Test fun `edited or replacement watcher rejects old request confirmation`() {
        val expected = watcher()
        listOf(expected.copy(id = 2), expected.copy(name = "Other alerts"),
            expected.copy(url = "https://other.example/api/glance"), expected.copy(credential = "new-key"),
            expected.copy(pushKey = "replacement-subscription"), expected.copy(delivery = Watcher.POLL),
            expected.copy(maxLength = 120), expected.copy(expirySeconds = 10)).forEach {
            assertFalse(confirmed(expected, it, reported = true))
        }
    }

    @Test fun `address change awaiting registration rejects old confirmation`() {
        val expected = watcher()
        val changed = expected.copy(pushAddress = "replacement-address", pushRegistered = false)
        assertFalse(confirmed(expected, changed, reported = true))
    }

    @Test fun `failed persistence cannot be reported as connected`() {
        val expected = watcher().copy(pushAddress = "", pushRegistered = false)
        assertFalse(confirmed(expected, expected, reported = true))
    }

    @Test fun `first confirmed registration accepts address populated by worker`() {
        val expected = watcher().copy(pushAddress = "", pushRegistered = false)
        assertTrue(confirmed(expected, watcher(), reported = true))
    }

    @Test fun `unchanged confirmed watcher accepts result despite unrelated failure flag`() {
        val expected = watcher()
        assertTrue(confirmed(expected, expected.copy(lastRunFailed = true), reported = true))
    }

    @Test fun `missing address fingerprint rejects otherwise successful result`() {
        val saved = watcher()
        assertFalse(confirmed(saved, saved, reported = true, hash = null))
    }

    @Test fun `empty address fingerprint rejects otherwise successful result`() {
        val saved = watcher()
        assertFalse(confirmed(saved, saved, reported = true, hash = ""))
    }

    @Test fun `mismatched address fingerprint rejects otherwise successful result`() {
        val saved = watcher()
        assertFalse(confirmed(saved, saved, reported = true, hash = registrationAddressHash("other-address")))
    }

    @Test fun `replacement address registered by later work rejects old completed result`() {
        val expected = watcher()
        val newer = expected.copy(pushAddress = "replacement-address", pushRegistered = true)
        assertFalse(confirmed(expected, newer, reported = true))
        assertTrue(confirmed(expected, newer, reported = true, hash = registrationAddressHash(newer.pushAddress)))
    }
}
