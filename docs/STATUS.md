# Status — what is true right now

*The present view. [`BACKLOG.md`](BACKLOG.md) is what is not done; this is what is. Overwritten,
never appended — a dated snapshot of one day belongs in a report.*

Stage: **it works end to end on a phone** — the backend's log shows the app's call arriving, and what
it returned was displayed unchanged. Whether it reaches the glasses is still unrecorded · last full
sweep 2026-09-10

| Component | Covers | State | Verified how, when |
| --- | --- | --- | --- |
| Android app package | `app/`, `app/build/outputs/apk/debug/app-debug.apk` | **live** | installed on the owner's phone and opened; `aapt2 dump badging` reports `dev.liamchu.glance`, targetSdk 37, 2026-09-10 |
| Settings screen | `app/src/main/java/dev/liamchu/glance/MainActivity.kt`, `Theme.kt`, `Settings.kt` | **live** | used by the owner to configure a real backend and save, 2026-09-10. Its validation is covered by `UrlValidationTest` and `SettingsTest` |
| Backend client | `app/src/main/java/dev/liamchu/glance/Backend.kt` | **live** | the backend's own log shows the call arriving from the phone — `User-Agent: Glance/0.1`, carrying `max_length` 120, `title_max_length` 32, `expires_after_seconds` 63, `interval_minutes` 15 — and answered 200. Also driven across every outcome by `BackendContractTest`, 2026-09-10 |
| Manual check | `app/src/main/java/dev/liamchu/glance/Scheduler.kt` `checkNow` | **live** | pressed on the phone, producing a notification carrying the backend's content, 2026-09-10 |
| Notification | `app/src/main/java/dev/liamchu/glance/Notifier.kt` | **live** | the owner saw the title and text displayed, and confirmed it was character-for-character what the backend returned — nothing truncated, reworded, or substituted, 2026-09-10 |
| Notification expiry | `Notifier.kt` `setTimeoutAfter` | **built, not observed** | nobody has yet watched one clear itself, 2026-09-10 |
| Unattended scheduled run | `app/src/main/java/dev/liamchu/glance/GlanceWorker.kt`, `Scheduler.kt` | **built, never run** | every run so far was started by a button. WorkManager waking the app on its own is the load-bearing claim of [`DESIGN.md`](DESIGN.md) "The stack" and no test can reach it, 2026-09-10 |
| Backend contract | [`CONTRACT.md`](CONTRACT.md) | **live** | every clause names the test that exercises it or says it has none; a real backend has answered it, and the request it received carried every field this file promises, 2026-09-10 |
| `C3` file-length gate | `build.gradle.kts` | **live** | `./gradlew.bat checkFileLength` fails on a 501-line file and passes on the tree as it stands, 2026-09-10 |
| Delivery to the G2 glasses | the Even Realities app's notification list | **unknown** | a notification was seen, but which screen it was seen on is not recorded here, 2026-09-10 |
| Documentation set | `docs/`, `README.md`, `CLAUDE.md` | **live** | `ls docs/` lists a file for every active slot the map names, and every link in `docs/README.md` resolves, 2026-09-10 |
| Version control | `.git/` | **live** | `git rev-parse --show-toplevel` names this directory, 2026-09-10 |

**Two things remain unproven, and they are the two that matter.** `C9` counts the running thing, and
for this project that is a notification readable on the glasses — which is not yet recorded. And every
run so far happened because somebody pressed a button, so the app waking itself is still a claim
rather than an observation.

A row is suspect when its path has commits newer than its verification date — then re-verify it or
delete it. No proportions: name which parts are live, which are built but unwired, which do not
exist.
