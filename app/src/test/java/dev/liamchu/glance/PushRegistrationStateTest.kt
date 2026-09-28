package dev.liamchu.glance

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PushRegistrationStateTest {
    private fun registered(id: Int = 1) = Watcher.blank(id).copy(
        name = "Updates", url = "https://backend.example/api/glance", credential = "test-credential",
        pushKey = "test-subscription-$id", pushAddress = "test-address", pushRegistered = true,
    )

    @Test fun `opening a registered watcher after a previous failure does not enqueue HTTP work`() {
        // Reproduce the reported case: FCM remains usable while the backend URL is unreachable.
        val persisted = registered().copy(lastRunFailed = true)
        val restored = Watcher.fromJson(JSONObject(persisted.toJson().toString()))
        repeat(3) {
            PushRegistration.enqueueMissing(listOf(restored)) { fail("Opening the app must not refresh registration") }
        }
        // An automatic job already queued by an earlier app version must also be harmless.
        assertFalse(restored.shouldRegisterPush(force = false))
    }

    @Test fun `opening queues only missing registrations including an observed address change`() {
        val successful = registered()
        val pending = registered(2).copy(pushRegistered = false)
        val changedAddress = registered(3).copy(pushRegistered = false, pushAddress = "replacement-address")
        val noAddress = registered(4).copy(pushAddress = "")
        val poll = registered(5).copy(delivery = Watcher.POLL, pushRegistered = false)
        val enqueued = mutableListOf<Int>()
        PushRegistration.enqueueMissing(listOf(successful, pending, changedAddress, noAddress, poll), enqueued::add)
        assertEquals(listOf(2, 3, 4), enqueued)
    }

    @Test fun `manual retry still registers an unchanged watcher but never a polling watcher`() {
        assertTrue(registered().shouldRegisterPush(force = true))
        assertTrue(registered().copy(pushRegistered = false).shouldRegisterPush(force = false))
        assertFalse(registered().copy(delivery = Watcher.POLL).shouldRegisterPush(force = true))
    }

    @Test fun `unchanged save keeps the previously confirmed registration across persistence`() {
        val original = registered()
        val saved = WatcherDraft(original).copy(name = " Updates ", url = " ${original.url} ").forSave()
        val restored = Watcher.fromJson(JSONObject(saved.toJson().toString()))
        assertTrue(restored.pushRegistered)
        assertEquals(original.pushAddress, restored.pushAddress)
        assertEquals(original.pushKey, restored.pushKey)
    }

    @Test fun `backend relevant edits require registration even when a subscription is retained`() {
        val draft = WatcherDraft(registered())
        listOf(draft.copy(name = "Other"), draft.copy(maxLength = "120"), draft.copy(expirySeconds = "10"),
            draft.copy(url = "https://another.example/api/glance"), draft.copy(credential = "replacement-key"))
            .forEach {
                val saved = it.forSave()
                assertFalse(saved.pushRegistered)
                assertTrue(saved.needsPushRegistration)
            }
    }

    @Test fun `save cannot overwrite registration that completed while the editor was open`() {
        val draft = WatcherDraft(registered().copy(pushRegistered = false, pushAddress = ""))
        val saved = draft.forSave().preserveRegistrationFrom(registered())
        assertTrue(saved.pushRegistered)
        assertEquals("test-address", saved.pushAddress)
    }

    @Test fun `save cannot revive the old address after FCM changes it while the editor is open`() {
        val draft = WatcherDraft(registered())
        val current = registered().copy(pushRegistered = false, pushAddress = "replacement-address")
        val saved = draft.forSave().preserveRegistrationFrom(current)
        assertFalse(saved.pushRegistered)
        assertEquals("replacement-address", saved.pushAddress)
        assertTrue(saved.needsPushRegistration)
    }

    @Test fun `registration status cannot transfer to an edited connection`() {
        val saved = WatcherDraft(registered()).copy(url = "https://another.example/api/glance")
            .forSave().preserveRegistrationFrom(registered())
        assertFalse(saved.pushRegistered)
        assertNotEquals(registered().pushKey, saved.pushKey)
    }

    @Test fun `refresh failure describes registration separately from receiving push`() {
        val watcher = registered()
        val text = failureExplanation(watcher, "backend unreachable", FailureSource.REGISTRATION)
        assertTrue(text.contains("refresh push registration"))
        assertTrue(text.contains("Previously registered pushes may still arrive"))
        assertFalse(text.contains(watcher.url))
        assertFalse(text.contains(watcher.credential))
        assertFalse(text.contains(watcher.pushAddress))
        assertEquals("backend unreachable", failureExplanation(watcher.copy(pushRegistered = false),
            "backend unreachable", FailureSource.REGISTRATION))
        assertEquals("push had no title or no text", failureExplanation(watcher,
            "push had no title or no text", FailureSource.DELIVERY))
    }

    @Test fun `push receipt does not depend on current HTTP registration status`() {
        val pending = registered().copy(pushRegistered = false, lastRunFailed = true)
        val data = mapOf("subscription_id" to pending.pushKey, "title" to "Hello", "text" to "An update")
        assertSame(pending, PushMessage.target(data, listOf(pending)))
        assertEquals(Outcome.Content("Hello", "An update"), PushMessage.content(data, pending))
    }
}
