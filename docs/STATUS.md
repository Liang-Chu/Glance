# Status — what is true right now

Stage: **Glance 2.0.1 (code 9) signed APK built with registration-trigger and failure-reporting fixes**.
Updated 2026-09-28. Open work is tracked in [BACKLOG.md](BACKLOG.md).

| Component | Evidence |
| --- | --- |
| Save/register crash | The owner's Samsung/API 36 export from 1.3 identified Firebase's auto-init setter waiting on the main thread. Retrace used the exact archived mapping. Save/delete/import now dispatch that operation to IO; five regression tests cover UI callers, enable/disable, readiness, cancellation and errors. Final 2.0 phone verification is still pending. |
| FCM registration and delivery | The 2026-09-28 Samsung/API 36 export shows successful registration and repeated PUSH_RECEIVED / CONTENT_POSTED events in 2.0 build 7, with no recorded JVM crash. At 17:18–17:21 UTC, HTTP_REGISTER failed with connection errors/timeouts; no content polling occurred. The owner reported a subsequent successful push, beyond this export. This does not establish delivery under controlled Doze or all network conditions. |
| Registration and failure reporting | Opening the app now enqueues only missing registrations; already queued automatic work also skips confirmed watchers. Manual save still retries. Unchanged saves preserve current registration/address state, and attempts no longer reset confirmed registration before contacting the backend. Registration errors have their own notification title and diagnostic event. Ten new regression tests cover entry scheduling, manual retry, persistence, edits/address races, failure wording and reception while registration is unconfirmed. |
| QR connection | 24 parser/flow tests cover URL decoding, invalid input, duplicate endpoints/names, preference and ID preservation, one-result/cancel behavior, draft restoration, generated QR decoding and loopback registration. Real camera/permission UX still requires the phone. |
| Firebase setup | Import now opens a high-contrast success dialog with the actual project ID and retains a status panel. Home shows white before setup and black with the saved project ID afterward; it reloads the selection on return and app entry. Restart guidance remains explicit, errors appear beside import, and long IDs can wrap. Five existing parser tests and release lint pass; visual/lifecycle verification on a phone is pending. |
| Diagnostics | A real Samsung export was used to diagnose the save crash. Nine JVM tests cover bounded persistence, privacy, rotation and exception handling. API 26 export and clear-cache retention still need device checks. |
| Polling and notifications | Earlier phone/G2 observations confirm manual polling, unchanged backend content and notification expiry. Unattended content polling and independent simultaneous watchers remain unverified. Startup now cancels only the obsolete id-less jobs seen in the log; current watcher schedules are preserved. |
| Local Android validation | 97 JVM tests passed, following a targeted 21-test registration/QR run; debug and release lint each report 0 errors and 3 dependency-update advisories. File-length gate and minified release build passed. No physical device or emulator is attached. |
| Example backends | Seven Python loopback tests passed, covering auth privacy, registration/address refresh/removal/tombstones, invalid input and sender validation. The oversized polling request test now checks rejection before sending a body, avoiding a Windows socket-close race. No new live FCM send was performed by these tests. |
| Release package | Version 2.0.1, code 9, min SDK 26, target 37. APK is not debuggable; publisher certificate, APK v2 signature and 16 KiB alignment were verified. The signing certificate matches 2.0 builds 6–8 for in-place updates. The former public 1.0 APK matches the local debug-signed copy, so those installations still require reinstall. |
| Source and privacy | 70 source files, 230 reachable Git history blobs and APK contents scanned with no owner Firebase values, private-key blocks or credential-pattern findings. The test-only private IP was replaced with a documentation address. Firebase/server keys and publisher signing material are ignored; the release key/config also have user-only filesystem access. |
| Documentation and CI | README updated; repeated protocol examples link to CONTRACT. Empty reserved-document scaffolding, superseded prose and stale gate descriptions removed. Documentation checks pass. The first GitHub run exposed a retired SDK package in the setup action's defaults; the workflow now explicitly requests platform-tools. Current remote results: [GitHub Actions](https://github.com/Liang-Chu/Glance/actions/workflows/check.yml). |

The dedicated publisher key is stored locally and must be retained for updates. Its private files
are not publication assets. The exact APK mapping and source snapshot are archived for retracing;
a rebuilt APK's mapping is not interchangeable.

C9 device verification is established only for the older observed flows. Local tests and a valid
production signature do not establish final 2.0 camera, background delivery or glasses behavior.
