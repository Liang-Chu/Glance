package dev.liamchu.glance

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Collections
import java.util.IdentityHashMap

/** A bounded event history plus a separate last crash that normal events cannot rotate away. */
internal class DiagnosticFiles(private val directory: File) {
    companion object {
        const val EVENT_BYTES = 256 * 1024
        const val RECORD_BYTES = 32 * 1024
        const val CRASH_BYTES = 64 * 1024
    }

    private val current = File(directory, "events.txt")
    private val previous = File(directory, "events-previous.txt")
    private val crash = File(directory, "last-crash.txt")
    private val temporaryCrash = File(directory, "last-crash.tmp")

    @Synchronized fun append(record: String) {
        ensureDirectory()
        val bytes = bounded(record, RECORD_BYTES)
        if (current.length() + bytes.size > EVENT_BYTES) {
            Files.move(current.toPath(), previous.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        FileOutputStream(current, true).use { it.write(bytes) }
    }

    @Synchronized fun saveCrash(record: String) {
        ensureDirectory()
        FileOutputStream(temporaryCrash).use {
            it.write(bounded(record, CRASH_BYTES))
            it.fd.sync()
        }
        Files.move(temporaryCrash.toPath(), crash.toPath(),
            StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    @Synchronized fun snapshot(): String = buildString {
        append("LAST RECORDED JVM CRASH\n")
        append(read(crash, CRASH_BYTES))
        append("\nRECENT EVENTS (oldest first)\n")
        append(read(previous, EVENT_BYTES))
        append(read(current, EVENT_BYTES))
    }

    @Synchronized fun hasCrash(): Boolean = crash.isFile

    @Synchronized fun clear() {
        listOf(current, previous, crash, temporaryCrash).forEach { Files.deleteIfExists(it.toPath()) }
    }

    private fun ensureDirectory() {
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Diagnostic directory unavailable")
    }

    private fun read(file: File, limit: Int): String {
        if (!file.exists()) return "(none)\n"
        return file.inputStream().use { stream ->
            val buffer = ByteArray(limit)
            var size = 0
            while (size < limit) {
                val count = stream.read(buffer, size, limit - size)
                if (count < 0) break
                size += count
            }
            String(buffer, 0, size, Charsets.UTF_8)
        }
    }

    private fun bounded(text: String, limit: Int): ByteArray {
        val bytes = (text.trimEnd() + "\n").toByteArray(Charsets.UTF_8)
        if (bytes.size <= limit) return bytes
        val marker = "\n[truncated]\n".toByteArray()
        return bytes.copyOf(limit - marker.size) + marker
    }
}

/** Exception messages/toString may contain credentials, URLs or notification text. Never read them. */
internal fun diagnosticTrace(error: Throwable): String = buildString {
    val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
    val pending = ArrayDeque<Pair<String, Throwable>>()
    pending.add("Exception" to error)
    var count = 0
    while (pending.isNotEmpty() && count++ < 8) {
        val (label, cause) = pending.removeFirst()
        if (!seen.add(cause)) continue
        append(label).append(": ").append(diagnosticSymbol(cause.javaClass.name)).append('\n')
        cause.stackTrace.take(48).forEach { frame ->
            append("    at ").append(diagnosticSymbol(frame.className)).append('.')
                .append(diagnosticSymbol(frame.methodName)).append('(')
                .append(diagnosticSymbol(frame.fileName ?: "UnknownSource"))
                .append(':').append(frame.lineNumber).append(")\n")
        }
        if (cause.stackTrace.size > 48) append("    [frames truncated]\n")
        cause.cause?.let { pending.add("Caused by" to it) }
        cause.suppressed.take(3).forEach { pending.add("Suppressed" to it) }
    }
    if (pending.isNotEmpty()) append("[causes truncated]\n")
    append("Exception messages omitted for privacy.\n")
}

internal fun diagnosticSymbol(value: String): String = value.take(160).map {
    if (it.isLetterOrDigit() || it in "._$<>- ") it else '?'
}.joinToString("")

/** Persist synchronously, then let Android's original handler terminate/report the same failure. */
internal class RecordingExceptionHandler(
    private val next: Thread.UncaughtExceptionHandler,
    private val record: (Thread, Throwable) -> Unit,
    private val onWriteFailure: () -> Unit,
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, error: Throwable) {
        try {
            record(thread, error)
        } catch (_: Throwable) {
            // Even OOM/storage failure must not replace the original crash or prevent termination.
            onWriteFailure()
        } finally {
            next.uncaughtException(thread, error)
        }
    }
}
