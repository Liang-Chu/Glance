# Orientation — the shape of the codebase

*What lives where, and which way dependencies point. Read second, after the project README.
Why anything is this way is [`DESIGN.md`](DESIGN.md); what is built is [`STATUS.md`](STATUS.md).*

## Layout

```
<repo root>/
  app/        the Android application — the only module
  docs/       the documentation set; docs/README.md is its only entrance
  examples/   a reference backend that answers CONTRACT.md, for people writing their own
  tools/      the documentation checks, run before a release
  gradle/     the Gradle wrapper
  .github/    CI checks with no Firebase configuration or signing secrets
  local/      never committed — signed builds, keys, logs, notes; ignored whole by .gitignore
```

Inside `app/src/main/java/dev/liamchu/glance/`, one file per job:

| File | Responsible for |
| --- | --- |
| `Watcher.kt` | one watcher: its settings, its derived durations, and its JSON form |
| `FirebaseConfig.kt` | strict validation of the user's Android Firebase configuration |
| `GlanceApplication.kt` | runtime initialization before activity and background service callbacks |
| `FirebaseSetup.kt` | local file import and the in-app setup guide |
| `DiagnosticFiles.kt` | bounded persistent event/crash files and message-free exception formatting |
| `Diagnostics.kt` | crash-handler installation, typed events, worker failure recording and report metadata |
| `DiagnosticsScreen.kt` | user-initiated log export and clear |
| `Settings.kt` | the store of all watchers, and every rule about what may be entered |
| `Backend.kt` | the one call to the user's backend, and turning its answer into an outcome |
| `Notifier.kt` | posting, and the lifetime that makes a notification clear itself |
| `GlanceWorker.kt` | one run: read settings, call once, post or do not post |
| `Scheduler.kt` | choosing polling work or push registration for each watcher |
| `GlanceMessagingService.kt` | receiving FCM content and installation-address updates |
| `PushMessage.kt` | routing subscription IDs and validating pushed content |
| `PushRegistration.kt` | registering/removing backend subscriptions with bounded retries |
| `Delivery.kt` | shared notification and failure handling for both transports |
| `MainActivity.kt` | the list of watchers, and which one is open |
| `WatcherEdit.kt` | one watcher's settings screen |
| `WatcherDraft.kt` | unsaved editor values, scanned defaults, validation and activity-state restoration |
| `PairingConnection.kt` | desktop v1 QR parsing, single-result guard and duplicate connection matching |
| `ConnectionScanner.kt` | live camera preview, on-demand permission and camera lifecycle |
| `ConnectionFeedback.kt` | English registration progress/result dialog and current-request confirmation |
| `LocalNetworkAccess.kt` | Android 17+ LAN permission and recovery controls in the editor |
| `Components.kt` | the shared field, duration and rule composables, and how a watcher is labelled |
| `Theme.kt` | the monochrome palette, the monospaced type, and the square corners |

## Dependencies

`MainActivity`, `GlanceWorker` and `GlanceMessagingService` are entry points and depend inward.
`Backend` takes a `Watcher` and returns an `Outcome`; `Delivery` passes validated content to
`Notifier`. Registration uses WorkManager; content delivery through FCM does not wait for it.

**Nothing enforces this — unenforced.** It is one Gradle module, so the direction holds by
convention rather than by a compiler error or a module boundary.

## Entry points

| | |
|---|---|
| build | `./gradlew.bat assembleDebug` (`./gradlew` on a POSIX shell) |
| runs first, unattended | `GlanceWorker.doWork()`, woken by WorkManager |
| push content | `GlanceMessagingService.onMessageReceived()`, invoked by FCM |
| push registration | `PushRegistrationWorker.doWork()` and `onRegistered()` for address changes |
| runs first, when opened | `MainActivity` |
| the seams for tests | `Backend` HTTP operations, `FirebaseConfig` import validation and `PushMessage` routing; loopback servers exercise the wire without real accounts |
| run the tests | `./gradlew.bat testDebugUnitTest` |
| the gate | `./gradlew.bat checkFileLength` — the `C3` line-count check |
| the doc checks | `bash tools/check-docs.sh` |
