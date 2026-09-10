# Orientation — the shape of the codebase

*What lives where, and which way dependencies point. Read second, after the project README.
Why anything is this way is [`DESIGN.md`](DESIGN.md); what is built is [`STATUS.md`](STATUS.md).*

## Layout

```
<repo root>/
  app/        the Android application — the only module
  docs/       the documentation set; docs/README.md is its only entrance
  gradle/     the Gradle wrapper
  .claude/    agent configuration: the skills this repository loads
```

Inside `app/src/main/java/dev/liamchu/glance/`, one file per job:

| File | Responsible for |
| --- | --- |
| `Watcher.kt` | one watcher: its settings, its derived durations, and its JSON form |
| `Settings.kt` | the store of all watchers, and every rule about what may be entered |
| `Backend.kt` | the one call to the user's backend, and turning its answer into an outcome |
| `Notifier.kt` | posting, and the lifetime that makes a notification clear itself |
| `GlanceWorker.kt` | one run: read settings, call once, post or do not post |
| `Scheduler.kt` | registering the repeating work with WorkManager |
| `MainActivity.kt` | the list of watchers, and which one is open |
| `WatcherEdit.kt` | one watcher's settings screen |
| `Components.kt` | the shared field, duration and rule composables, and how a watcher is labelled |
| `Theme.kt` | the monochrome palette, the monospaced type, and the square corners |

## Dependencies

`MainActivity` and `GlanceWorker` are the two entry points and both depend inward on the rest.
`Backend`, `Notifier` and `Scheduler` know nothing about each other; `Backend` takes a `Settings`
and returns an `Outcome`, and that is the only shape crossing between them.

**Nothing enforces this — unenforced.** It is one Gradle module, so the direction holds by
convention rather than by a compiler error or a module boundary.

## Entry points

| | |
|---|---|
| build | `./gradlew.bat assembleDebug` (`./gradlew` on a POSIX shell) |
| runs first, unattended | `GlanceWorker.doWork()`, woken by WorkManager |
| runs first, when opened | `MainActivity` |
| the seam for tests | `Backend.fetch(Settings)` — the only call that leaves the process. `app/src/test/` drives it against a real loopback HTTP server rather than a fake, so the tests exercise the wire and not a mock of it |
| run the tests | `./gradlew.bat testDebugUnitTest` |
| the gate | `./gradlew.bat checkFileLength` — the `C3` line-count check |
