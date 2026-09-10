# Status — what is true right now

*The present view. [`BACKLOG.md`](BACKLOG.md) is what is not done; this is what is. Overwritten,
never appended — a dated snapshot of one day belongs in a report.*

Stage: **built; the backend client is tested, the app has never run on a device** · last full sweep
2026-09-10

| Component | Covers | State | Verified how, when |
| --- | --- | --- | --- |
| Android app package | `app/`, `app/build/outputs/apk/debug/app-debug.apk` | **built, unwired** | `./gradlew.bat assembleDebug` succeeded and `aapt2 dump badging` reports `dev.liamchu.blip`, targetSdk 37, 2026-09-10 |
| Settings screen | `app/src/main/java/dev/liamchu/blip/MainActivity.kt` | **built, never run** | compiles; never opened on a device. Its URL check is covered by `UrlValidationTest`, 2026-09-10 |
| Backend client | `app/src/main/java/dev/liamchu/blip/Backend.kt` | **live** | `./gradlew.bat testDebugUnitTest` — 15 cases in `BackendContractTest` drive it against a real loopback HTTP server: 200, 204, 500, redirect, unreachable, non-JSON, missing member, empty member, over-length text, over-length title, and the exact-limit boundary, 2026-09-10 |
| Scheduled run | `app/src/main/java/dev/liamchu/blip/BlipWorker.kt`, `Scheduler.kt` | **built, never run** | compiles; WorkManager pulled `RECEIVE_BOOT_COMPLETED` into the merged manifest, which is what would carry it across a reboot, 2026-09-10 |
| Notification and its expiry | `app/src/main/java/dev/liamchu/blip/Notifier.kt` | **built, never run** | compiles; no notification has been posted, 2026-09-10 |
| `C3` file-length gate | `build.gradle.kts` | **live** | `./gradlew.bat checkFileLength` fails on a 501-line file and passes on the tree as it stands, 2026-09-10 |
| Backend contract | [`CONTRACT.md`](CONTRACT.md) | **live on the client side** | every clause carries the test that exercises it, or says it has none. No backend written by a person has answered it, 2026-09-10 |
| Delivery to the G2 glasses | no path yet | **not started** | no notification from this app has been seen on the glasses, 2026-09-10 |
| Documentation set | `docs/`, `README.md`, `CLAUDE.md` | **live** | `ls docs/` lists a file for every active slot the map names, and every link in `docs/README.md` resolves, 2026-09-10 |
| Version control | `.git/` | **live** | `git rev-parse --show-toplevel` names this directory, 2026-09-10 |

**Tested is not the same as run.** `C9` counts the running thing, and for this project that is a
notification readable on the glasses. The backend client has met a real bar — it talks to an actual
socket, not a mock — but nothing here has posted a notification, been woken by WorkManager, or
reached the glasses.

A row is suspect when its path has commits newer than its verification date — then re-verify it or
delete it. No proportions: name which parts are live, which are built but unwired, which do not
exist.
