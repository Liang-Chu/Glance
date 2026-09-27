package dev.liamchu.glance

/** Validation at the FCM input boundary; no defaults for missing content. */
object PushMessage {
    fun target(data: Map<String, String>, watchers: List<Watcher>): Watcher? {
        val key = data["subscription_id"] ?: return null
        if (key.isEmpty()) return null
        return watchers.firstOrNull { it.isPush && it.pushKey == key }
    }

    fun content(data: Map<String, String>, watcher: Watcher): Outcome {
        val title = data["title"]
        val text = data["text"]
        if (title == null || text == null) {
            Diagnostics.event(DiagnosticEvent.MISSING_CONTENT, watcher.id)
            return Outcome.Failed("push had no title or no text")
        }
        return Backend.validateContent(title, text, watcher.maxLength)
    }
}
