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

Inside `app/src/main/java/dev/liamchu/blip/`, one file per job:

| File | Responsible for |
| --- | --- |
| `Settings.kt` | the five settings and the failure flag, their starting values, and whether a URL is usable |
| `Backend.kt` | the one call to the user's backend, and turning its answer into an outcome |
| `Notifier.kt` | posting, and the lifetime that makes a notification clear itself |
| `BlipWorker.kt` | one run: read settings, call once, post or do not post |
| `Scheduler.kt` | registering the repeating work with WorkManager |
| `MainActivity.kt` | the settings screen |

## Dependencies

`MainActivity` and `BlipWorker` are the two entry points and both depend inward on the rest.
`Backend`, `Notifier` and `Scheduler` know nothing about each other; `Backend` takes a `Settings`
and returns an `Outcome`, and that is the only shape crossing between them.

**Nothing enforces this — unenforced.** It is one Gradle module, so the direction holds by
convention rather than by a compiler error or a module boundary.

## Entry points

| | |
|---|---|
| build | `./gradlew.bat assembleDebug` (`./gradlew` on a POSIX shell) |
| runs first, unattended | `BlipWorker.doWork()`, woken by WorkManager |
| runs first, when opened | `MainActivity` |
| the seam for tests | `Backend.fetch(Settings)` — the only call that leaves the process. **No fake and no tests exist yet**; adding them is tracked in [`BACKLOG.md`](BACKLOG.md) |
| the gate | `./gradlew.bat checkFileLength` — the `C3` line-count check |
