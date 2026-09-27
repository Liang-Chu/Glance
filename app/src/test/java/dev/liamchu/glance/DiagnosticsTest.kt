package dev.liamchu.glance

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DiagnosticsTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun `saved crash survives reopening and event rotation`() {
        val folder = temporary.newFolder()
        val first = DiagnosticFiles(folder)
        first.saveCrash("saved crash")
        repeat(40) { first.append("event-$it " + "x".repeat(16000)) }
        val reopened = DiagnosticFiles(folder)
        val report = reopened.snapshot()
        assertTrue(reopened.hasCrash())
        assertTrue(report.contains("saved crash"))
        assertTrue(report.contains("event-39 "))
        assertFalse(report.contains("event-0 "))
        assertTrue(File(folder, "events.txt").length() <= DiagnosticFiles.EVENT_BYTES)
        assertTrue(File(folder, "events-previous.txt").length() <= DiagnosticFiles.EVENT_BYTES)
    }

    @Test fun `records are bounded and a newer crash atomically replaces the previous one`() {
        val folder = temporary.newFolder()
        val files = DiagnosticFiles(folder)
        files.append("x".repeat(DiagnosticFiles.RECORD_BYTES * 2))
        files.saveCrash("old crash")
        files.saveCrash("new crash " + "x".repeat(DiagnosticFiles.CRASH_BYTES * 2))
        assertEquals(DiagnosticFiles.RECORD_BYTES.toLong(), File(folder, "events.txt").length())
        assertEquals(DiagnosticFiles.CRASH_BYTES.toLong(), File(folder, "last-crash.txt").length())
        assertFalse(File(folder, "last-crash.tmp").exists())
        assertTrue(files.snapshot().contains("[truncated]"))
        assertFalse(files.snapshot().contains("old crash"))
        assertTrue(files.snapshot().contains("new crash"))
    }

    @Test fun `concurrent append preserves complete records`() {
        val folder = temporary.newFolder()
        val files = DiagnosticFiles(folder)
        val pool = Executors.newFixedThreadPool(4)
        val jobs = (0 until 100).map { n -> pool.submit { files.append("record-$n") } }
        pool.shutdown()
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS))
        jobs.forEach { it.get() }
        assertEquals((0 until 100).map { "record-$it" }.toSet(),
            File(folder, "events.txt").readLines().toSet())
    }

    @Test fun `clear removes owned records only`() {
        val folder = temporary.newFolder()
        val files = DiagnosticFiles(folder)
        File(folder, "unrelated.txt").writeText("keep")
        files.append("event")
        files.saveCrash("crash")
        files.clear()
        assertFalse(files.hasCrash())
        assertFalse(files.snapshot().contains("event\n"))
        assertTrue(File(folder, "unrelated.txt").exists())
    }

    @Test fun `exception messages never reach disk including causes and suppressed failures`() {
        val privateMessage = "Bearer SECRET https://private.example/path?key=PRIVATE notification-body"
        val original = IllegalStateException(privateMessage, IOException(privateMessage))
        original.addSuppressed(SecurityException(privateMessage))
        original.stackTrace = arrayOf(StackTraceElement("dev.liamchu.glance.Example", "run", "Example.kt", 42))
        val files = DiagnosticFiles(temporary.newFolder())
        files.saveCrash(diagnosticTrace(original))
        val report = files.snapshot()
        assertTrue(report.contains("IllegalStateException"))
        assertTrue(report.contains("IOException"))
        assertTrue(report.contains("SecurityException"))
        assertTrue(report.contains("Example.run(Example.kt:42)"))
        listOf("SECRET", "private.example", "PRIVATE", "notification-body", "Bearer").forEach {
            assertFalse("Must not export $it", report.contains(it))
        }
    }

    @Test fun `cyclic exceptions and very deep stacks are bounded`() {
        val one = RuntimeException()
        val two = RuntimeException(one)
        one.initCause(two)
        one.stackTrace = Array(10000) { StackTraceElement("Class", "method", "Source.kt", it) }
        val trace = diagnosticTrace(one)
        assertTrue(trace.contains("frames truncated"))
        assertTrue(trace.length < 8000)
    }

    @Test fun `uncaught handler saves before delegating the original failure`() {
        val files = DiagnosticFiles(temporary.newFolder())
        val error = IllegalStateException("private")
        var delegated = 0
        val handler = RecordingExceptionHandler(Thread.UncaughtExceptionHandler { thread, actual ->
            assertSame(Thread.currentThread(), thread)
            assertSame(error, actual)
            assertTrue(files.hasCrash())
            delegated++
        }, { _, actual -> files.saveCrash(diagnosticTrace(actual)) }, { fail("Save should succeed") })
        handler.uncaughtException(Thread.currentThread(), error)
        assertEquals(1, delegated)
    }

    @Test fun `failed crash recording cannot swallow or replace the original crash`() {
        val original = IllegalArgumentException("original")
        var writeFailed = false
        var delegated = 0
        val handler = RecordingExceptionHandler(Thread.UncaughtExceptionHandler { _, error ->
            assertSame(original, error)
            delegated++
        }, { _, _ -> throw IOException("disk full") }, { writeFailed = true })
        handler.uncaughtException(Thread.currentThread(), original)
        assertTrue(writeFailed)
        assertEquals(1, delegated)
    }

    @Test fun `worker diagnostics preserve cancellation and failure semantics`() = runBlocking {
        val cancelled = CancellationException("cancelled")
        try {
            Diagnostics.worker(DiagnosticEvent.POLL_WORK, 1, 0) { throw cancelled }
            fail("Cancellation must propagate")
        } catch (caught: CancellationException) { assertSame(cancelled, caught) }
        val failure = IOException("backend-private")
        try {
            Diagnostics.worker(DiagnosticEvent.REGISTER_WORK, 1, 0) { throw failure }
            fail("Failure must propagate")
        } catch (caught: IOException) { assertSame(failure, caught) }
        val linkage = NoSuchMethodError("private detail")
        try {
            Diagnostics.worker(DiagnosticEvent.REGISTER_WORK, 1, 0) { throw linkage }
            fail("Errors captured by WorkManager must propagate unchanged too")
        } catch (caught: NoSuchMethodError) { assertSame(linkage, caught) }
    }
}
