# Design

> Latest design only. Decisions only — no process, no plans, no change log.

## 0 · Decision index — grep before designing anything

| Question | Decided in |
| --- | --- |
| What platform does this software run on? | **[`DESIGN.md`](DESIGN.md) "Purpose and delivery path"** (owner, 2026-09-10) |
| How does a notification get from the phone to the glasses? | **[`DESIGN.md`](DESIGN.md) "Purpose and delivery path"** (owner, 2026-09-10: "using their notification feature") |
| Do we pair with, scan for, or speak a link protocol to the glasses? | **[`DESIGN.md`](DESIGN.md) "Purpose and delivery path"** (owner, 2026-09-10) |
| Does a notification stay on screen until someone dismisses it? | **[`DESIGN.md`](DESIGN.md) "Purpose and delivery path"** (owner, 2026-09-10: "expire automatically") |
| Why does a notification expire, and what cancels it? | **[`DESIGN.md`](DESIGN.md) "Purpose and delivery path"** (owner, 2026-09-10: "i dont want the user have to deal with the notification on the phone again") |
| Where does the text of a notification come from? | **[`DESIGN.md`](DESIGN.md) "Where a notification's content comes from"** (owner, 2026-09-10: "accept some backend input and make them a notification") |
| Does the app choose topics, write prompts, or call an LLM? | **[`DESIGN.md`](DESIGN.md) "Where a notification's content comes from"** (owner, 2026-09-10: ruled out) |
| Does this project run a server, or hold any API key or account of its own? | **[`DESIGN.md`](DESIGN.md) "Where a notification's content comes from"** (owner, 2026-09-10: "i dont want to host a server for this one") |
| What is the exact shape of the call to the backend? | **[`CONTRACT.md`](CONTRACT.md)** |
| What happens to a response longer than the length setting? | **[`CONTRACT.md`](CONTRACT.md) "Responses"** (owner, 2026-09-10: a breach, not something to trim) |
| What can the user change? | **[`DESIGN.md`](DESIGN.md) "What the user can change"** (owner, 2026-09-10) |
| Can an Even-PIlot connection QR fill a watcher? | **[`DESIGN.md`](DESIGN.md) "Scan a backend connection"** (owner, 2026-09-26) |
| Which delivery mode does a new watcher start with? | **[`DESIGN.md`](DESIGN.md) "What the user can change"** (owner, 2026-09-25) |
| Does a plain `http` backend work, and what does it cost? | **[`DESIGN.md`](DESIGN.md) "What the user can change"** |
| What does the user see when a run fails? | **[`DESIGN.md`](DESIGN.md) "What a failed run shows"** (owner, 2026-09-10) |
| How can the user find out why Glance crashed or stopped delivering? | **[`DESIGN.md`](DESIGN.md) "Local diagnostics"** (owner, 2026-09-25) |
| What is the app written in, and what runs the interval? | **[`DESIGN.md`](DESIGN.md) "The stack"** |
| Who supplies Firebase configuration, and can a backend use its own service account? | **[`DESIGN.md`](DESIGN.md) "User-owned Firebase setup"** (owner, 2026-09-25) |
| How does the user know Firebase configuration was imported? | **[`DESIGN.md`](DESIGN.md) "User-owned Firebase setup"** (owner, 2026-09-26) |
| Can a backend push content immediately, and does this require Firebase? | **[`DESIGN.md`](DESIGN.md) "Push delivery"** (owner, 2026-09-25) |
| Can there be more than one watcher, and what do they share? | **[`DESIGN.md`](DESIGN.md) "Many watchers, each named"** (owner, 2026-09-10: "allow the user setup multiple notification watcher") |
| What identifies a watcher — its name, or something else? | **[`DESIGN.md`](DESIGN.md) "Many watchers, each named"** |
| Does the backend learn which watcher is asking? | **[`DESIGN.md`](DESIGN.md) "Many watchers, each named"** |
| What may the app look like, and may it use colour? | **[`DESIGN.md`](DESIGN.md) "How it looks"** (owner, 2026-09-10: "black - white - gray pixil style") |

Grep the whole file before concluding a question is undecided.

## 1 · Purpose and delivery path

Glance is an Android client that posts backend-authored notifications. The Even Realities app
forwards them to G2 glasses through its existing notification feature. Glance does not pair with,
scan for, or speak a transport protocol to the glasses.

Content expiry is carried on the Android notification itself, keeping the phone shade clear even
when Glance is no longer running. It does not control how the glasses handle withdrawal.
Configured watchers deliver in the background without keeping a foreground service alive.

### Acceptance

- `purpose-1` The delivered artifact is an Android application package.
- `purpose-2` Every notification the app posts leaves the notification shade with no user action.
- `purpose-3` No source file in this repository connects to the glasses: nothing in it pairs, scans,
  or speaks a link protocol to them.
- `purpose-4` A posted notification clears itself with the app force-stopped and no work scheduled;
  the lifetime rides on the notification, not on something that must still be alive to fire.
- `purpose-5` Notifications keep appearing across a reboot with the app never opened again.

## 2 · Where a notification's content comes from

Each watcher either polls a user-owned backend or registers with it for FCM push. Wire formats
belong to [CONTRACT.md](CONTRACT.md). Content generation, topics, prompts, model choice and repeat
suppression belong to the backend. Glance keeps no content history or response cache.

There is no Glance-operated server, shared account or embedded Firebase project. Users import their
own Android client configuration on the phone. Sending credentials stay on their backends.

### Acceptance

- `content-1` No Firebase service-account key or backend credential is committed or embedded in the
  APK. No Firebase project or Android API key is embedded either. A fresh install does not register
  with FCM until the user imports client configuration and saves a push watcher.
- `content-2` Content polling contacts only the configured backend. Push registration additionally
  uses Firebase infrastructure; this dependency is documented before setup.
- `content-3` The app contains no prompts, model selection, or topic selection.
- `content-4` No content history or response cache exists. Watcher registration metadata lives in
  DataStore; WorkManager and Firebase also keep their own delivery state.
- `content-5` When the backend cannot be reached or breaks [`CONTRACT.md`](CONTRACT.md), the app
  posts no content notification and writes no substitute content.


## 3 · What the user can change

Settings belong to individual watchers; see "Many watchers, each named".

| Setting | Unit | Starting value |
| --- | --- | --- |
| Name | required, unique ignoring case and surrounding whitespace | empty |
| Delivery | PUSH or POLL | PUSH; saved records retain their mode |
| Backend URL | absolute HTTP(S), no user-info, fragments or invalid ports | empty |
| Credential | optional printable ASCII, masked | empty |
| Check every | POLL only; days + hours + minutes, at least 15 minutes | 1 day |
| Expires after | minutes + seconds, at least 3 seconds | 3 seconds |
| Maximum length | characters, positive integer | 80 |

Empty duration boxes count as zero. Reject intervals under WorkManager's 15-minute floor instead
of silently rounding them. Reject expiry under three seconds, including zero (Android interprets
zero as no timeout). The 80-character default is below the observed G2 display length; it does not
claim a measured maximum.

Plain HTTP is supported for private networks; credentials travel in cleartext over HTTP, so users
should choose HTTPS across untrusted networks. Credentials and imported client configuration stay
in app-private storage, excluded from backup and logs.

New watchers default to PUSH (owner, 2026-09-25). Pre-push saved records still load as POLL so an
upgrade cannot silently change their behavior. Scan-specific defaults are in "Scan a backend connection".

### Acceptance

- `settings-1` The editor offers the settings in the table; push hides the polling interval and
  offers Save and register instead of Save and check now. Firebase setup is a separate guided page.
- `settings-2` Each starting value appears in exactly one place in the source. A new watcher opens
  in push mode; reopening a saved watcher preserves its mode.
- `settings-3` A URL that is not an absolute `http` or `https` URL is refused when it is entered, not
  at the moment a run tries to use it. URLs with user-info, fragments or invalid ports are refused;
  credentials must be printable ASCII and are masked in the editor.
- `settings-4` Changing the check frequency changes the interval of the next scheduled run without
  the app being reinstalled.
- `settings-5` The credential is held in the app's private storage, and the app opts out of Android
  backup so it cannot leave the device that way.
- `settings-6` No credential, and no part of one, is ever written to a log or into the text of a
  notification.
- `settings-7` A backend reached over plain `http` works on a current Android release. Nothing the
  settings screen accepts fails later for being cleartext.
- `settings-8` A polling interval below fifteen minutes is refused when entered, in every unit, and
  no code path rounds one up.
- `settings-9` An expiry totalling under three seconds is refused when it is entered. No expiry ever
  reaches the platform as zero, which it would read as "no expiry at all".
- `settings-10` Two spellings of one duration schedule identically: 1 hour and 60 minutes agree, and
  1 minute and 1 day do not.
- `settings-11` A manual check runs one check immediately and leaves the repeating schedule alone:
  pressing it does not move when the next automatic check falls due.

## 4 · What a failed run shows

Each watcher reports the first failure with a separate notification, then stays quiet until
successful content delivery or registration clears its failure state and notice. Failure notices
remain until cleared or dismissed; they contain fixed reasons, never backend response bodies.
Registration failures are labelled as registration failures in both notifications and diagnostics,
not as failed content delivery or a general claim that the watcher cannot be reached. A failed
manual refresh of an unchanged, previously registered watcher preserves its confirmed registration
and explains that previously registered pushes may still arrive.

A backend returning HTTP 204 for polling has nothing to say: it neither posts content nor changes
failure state. Delivery and state changes are serialized with watcher edits/deletion so a stale
result cannot revive a deleted watcher or affect a replacement configuration.

### Acceptance

- `failure-1` Consecutive failed runs produce one notification between them, not one each; the app
  regains its voice only after a run succeeds. Successful content/registration clears its old failure
  notice; stale configurations cannot update state or post notifications.
- `failure-2` A failure notification carries no text originating from the backend's response body.
- `failure-3` A run in which the backend reports it has nothing to say posts nothing, and neither
  raises nor clears the failure state.
- `failure-4` Push registration failures use a registration-specific title and REGISTRATION_FAILED
  event. Content failures use DELIVERY_FAILED; a malformed message is not called an unreachable backend.

## 5 · The stack

Kotlin and Jetpack Compose provide the UI; DataStore holds settings; WorkManager handles polling
and bounded push registration/removal retries. Minimum SDK 26 provides notification expiry; compile
and target SDK are 37. AGP supplies Kotlin support; do not add the legacy Android Kotlin plugin.

WorkManager persists polling across reboot and may defer it in Doze. It does not guarantee exact
timing. FCM delivers content without a network fetch in its callback. There is no alarm loop,
foreground service or app content database; Firebase and WorkManager maintain their own state.

Startup cancels the old single-watcher jobs named `glance-poll` and `glance-now`. They carry no
watcher ID and can only wake up without doing useful work. Current per-watcher schedules are kept.

### Acceptance

- `stack-1` Polling uses WorkManager; push uses FirebaseMessagingService with no network fetch in its
  message callback. No alarm loop or long-lived service exists.
- `stack-2` The app declares no content database; registration metadata lives with watcher settings.
- `stack-3` A release build installs and runs on a device at minimum SDK 26.
- `stack-4` Startup cancels obsolete id-less work without cancelling current per-watcher jobs.

## 6 · How it looks

The owner's requested style is black, white and grey, monospaced type, square corners and a
block-built icon. Errors use contrast rather than another hue. The window is black before Compose
draws to avoid a white flash on launch.

### Acceptance

- `look-1` Every colour the source declares is black, white, or a grey with equal red, green and blue
  channels. A hue anywhere is a defect.
- `look-2` Every text style the app defines is monospaced.
- `look-3` No corner in the app is rounded.
- `look-4` A warning is distinguished by inversion, never by colour.
- `look-5` The window is black before Compose draws, so launching never flashes white.

## 7 · Many watchers, each named

Watchers have independent settings, schedules, notification slots, failure state and subscription
IDs. PUSH watchers share one Firebase installation. New saves require unique names; existing
records are not renamed automatically.

Identity is an integer ID, never the editable name. Allocate IDs once and never reuse deleted IDs.
Send the name to the backend so a single endpoint can serve different content for different watchers.
Deleting one watcher cancels only its work and notifications.

DataStore also holds imported client configuration and retired push subscriptions awaiting removal.
Retired credentials are kept only until removal succeeds or five attempts finish. WorkManager input
carries the subscription ID, never the serialized credential-bearing watcher.

### Acceptance

- `watchers-1` Two watchers can hold notifications at the same time; neither replaces the other's.
- `watchers-2` A watcher's next notification replaces its own previous one, so the shade does not
  fill up.
- `watchers-3` A failing watcher posts one failure notice naming itself, and the other watchers go on
  posting.
- `watchers-4` Deleting a watcher cancels its scheduled work and removes its notifications; every
  other watcher is untouched.
- `watchers-5` A deleted watcher's id is never given to a later one.
- `watchers-6` The watcher's name reaches the backend in the request.
- `watchers-7` Renaming a watcher changes no schedule and loses no state.
- `watchers-8` The app declares no database; the watchers live in the settings store.

## 8 · Push delivery

PUSH is the default for new watchers. The backend receives an authenticated registration with
the installation's FCM address, subscription ID, target Firebase project and notification limits.
It sends data-only messages through that target project; Glance validates and posts their content.
The Firebase and backend protocols remain those in [CONTRACT.md](CONTRACT.md).

Use the SDK's `register()` and `onRegistered()` installation-ID APIs. New or changed watchers and
SDK address changes require registration. Opening the app retries only missing registrations;
it does not contact the backend for an already registered watcher with a saved address. Workers
also skip redundant automatic jobs, including ones queued before an upgrade. SAVE AND REGISTER
explicitly retries even unchanged settings, so the user can restore a lost backend subscription.
An unchanged save preserves the latest registration state, including SDK changes while the editor
was open; a refresh attempt itself does not revoke a previous success. Registration and removal
each allow five attempts with WorkManager backoff. Registration success proves backend acceptance,
not end-to-end delivery. Receiving FCM content never depends on the current HTTP registration flag.

Save, delete and import run FCM auto-init updates on Dispatchers.IO: the SDK setter may internally
wait for the installation ID. Resume the caller's dispatcher afterward and propagate cancellation
or SDK failures. This prevents the main-thread wait found in the Samsung 1.3 crash export.

Changing URL, credential, delivery mode or Firebase configuration rotates the subscription ID.
Deletion or leaving PUSH immediately revokes the old ID locally and queues best-effort removal.
Backends must tombstone retired IDs against late registration; unreachable ones may retain unused IDs.

FCM is best-effort, not exactly-once. Connectivity, Play services, permissions and power management
can affect delivery. High priority is for time-sensitive, visible messages. The backend chooses FCM
queue TTL; watcher expiry starts when Glance posts the notification. There is no delivery history.

### Acceptance

- `push-1` New watchers start with push; saved delivery modes are preserved, including polling for
  records from before push support. No push watcher schedules periodic content fetches. An unsaved
  draft has no subscription and must not enqueue backend removal.
- `push-2` Registration carries the current installation ID, target Firebase project ID, subscription ID,
  name and limits, and
  accepts only HTTP 204 as success. Backend credentials are never sent to Firebase.
- `push-3` Missing, empty or oversized content is rejected using the same limits as polling.
- `push-4` Messages for deleted, replaced or polling subscriptions post nothing.
- `push-5` Data-only messages post through Notifier with watcher-specific expiry, without a network
  request or detached background coroutine in the FCM callback.
- `push-6` SDK address refresh triggers registration; its callback does not cause an endless loop.
- `push-7` Builds without Firebase configuration support polling and refuse to save a push watcher
  with an explicit import instruction. No client configuration or sending credentials are required to build.
- `push-8` A real high-priority push reaches the phone and glasses while Glance is backgrounded;
  it expires on the phone. This requires configured hardware verification, recorded in STATUS.md.
- `push-9` Enabling/disabling auto-init after settings changes runs off the UI thread even when the
  caller is a UI coroutine; unavailable Firebase is skipped and completion resumes the caller normally.
- `push-10` Opening a registered watcher or executing its old automatic job causes no registration
  request. Missing registrations and SDK address changes still register; a manual save can force a retry.
- `push-11` An unchanged save or failed refresh does not erase confirmed registration. Configuration
  changes invalidate confirmation, and an editor cannot overwrite a newer address or registration result.

## 9 · User-owned Firebase setup

Users import `google-services.json` locally, including the project owner. No Firebase project
or key is bundled in the APK. Save only the required Android client fields and reject service-account
private keys. An installation uses one project; its watchers may connect to independent backends.

A backend's own service account needs FCM sending permission in the user's target project. Cross-
project authorization is an explicit, project-wide trust grant; it is not watcher-scoped IAM.

The setup screen shows five numbered actions: create project, register Android app, import JSON,
authorize backend, save watcher. Put same-project and cross-project backend details behind a help
button, keeping server private keys off the phone. This follows the owner's 2026-09-26 request for
short, step-by-step guidance.

Confirm a completed import with a high-contrast dialog naming the saved project ID. Keep the project
visible in a status panel on the setup screen. On the home screen, FIREBASE SETUP is white until a
project is imported, then black with the saved project ID below the label. Allow the ID to wrap;
never shorten it to a guessed display name. The file supplies the ID, so displaying it needs no
extra network request. Loading is neutral; import errors stay beside the file button. Keep restart
requirements explicit: imported configuration does not claim that a backend is registered or push
delivery has been verified.

First import works immediately. Reimporting the same file preserves subscriptions. Replacing the
configuration requires confirmation and a process restart because SDK components retain their
initialization options. Pause push until Force stop and reopen; polling continues independently.

### Acceptance

- `firebase-1` A clean checkout builds without client configuration, server credentials or signing keys.
- `firebase-2` Import selects the exact Android package, validates all required fields and their
  consistency, and rejects malformed files, server private keys and input over 256 KiB.
- `firebase-3` App startup restores the imported project before FCM service callbacks run. No default
  project is silently substituted when configuration is absent.
- `firebase-4` Replacing configuration invalidates old subscription IDs and pauses push until restart;
  all saved push watchers then register in the selected project. Polling settings remain intact.
- `firebase-5` The app shows five numbered setup actions with expandable backend help explaining that a backend's own key needs
  permission for the target project; it never requests that private key on the phone.
- `firebase-6` Poll responses are bounded at 64 KiB, title/text must be JSON strings, credentials are
  never logged, and large credentials cannot overflow WorkManager's input-data limit.
- `firebase-7` A completed import opens a project-ID confirmation. Returning home or reopening the
  app shows a black setup button with that saved ID; an unconfigured installation shows a white
  setup button. Failed/cancelled imports preserve the previous project and do not report success.
- `firebase-8` A replaced project is shown as saved but awaiting restart, without implying active push.

## 10 · Local diagnostics

Owner, 2026-09-25: *"有没有办法保存log起码下次出现我能指导为什么呢"*.

Diagnostics are local, bounded and available from the home screen. Fixed event names, timestamps,
local numeric watcher IDs, HTTP status codes and exception stack frames record startup, configuration,
registration, polling and push delivery. Backend URLs, credentials, Firebase identifiers, watcher
names, notification text and exception messages are never logged. The exception type, cause chain
and source positions remain; messages and raw Android trace buffers are excluded for privacy.

Install the uncaught-exception recorder before application initialization. Save the last JVM crash
synchronously, then delegate to Android's original handler with the original exception. Worker
exceptions are recorded and rethrown, preserving WorkManager's cancellation and failure behavior.
Logging failure sets a visible diagnostic warning and must not replace a crash or break delivery.

Keep two event files of at most 256 KiB each and one 64 KiB last-crash file in no-backup private
storage. An atomic crash replacement may use another 64 KiB temporary file. Restart and clearing
cache preserve records; uninstalling or clearing app data removes them. Normal event rotation never
removes the last crash. Export uses Android's document picker and uploads nothing automatically.
Clearing local logs leaves exported copies and Android's own exit history untouched.

Exports include app version, Android version, device model and, on Android 11+, the last five process
exit reasons provided by the OS. These can distinguish ANR/native crash/system termination without
claiming a JVM stack always exists. Early failures, abrupt termination and exhausted storage or memory
can prevent recording. Retain the exact release mapping alongside every distributed minified APK.

### Acceptance

- `diagnostics-1` Logs survive reopening and rotate within their size limits; the last crash survives rotation.
- `diagnostics-2` Exceptions, their causes and suppressed exceptions export frames but no messages.
- `diagnostics-3` The crash recorder delegates the original failure even when saving fails; workers rethrow failures/cancellation.
- `diagnostics-4` DIAGNOSTICS offers export and clear without storage permission or automatic upload.
- `diagnostics-5` Reports identify the app build and include available OS exit reasons without raw trace data.

## 11 · Scan a backend connection

Owner, 2026-09-26: *"使用 Even-PIlot 桌面现有的同一张二维码和同一个 key。不要写死 Tailscale
地址，也不要修改 Firebase 或后端推送协议。"*

New and existing watcher editors offer **SCAN CONNECTION QR**. A bundled ZXing decoder reads live
camera frames on-device, without taking a photo or installing a scanner. Camera permission is requested
only on entry; denial and camera failure provide retry, settings and cancel. The camera pauses when
backgrounded and is released on exit. The scanner excludes its credential-bearing preview from screenshots.
On Android 17+, the editor also offers local-network permission for direct LAN backends. Public internet
endpoints remain usable without granting LAN access; denial keeps the permission/retry/settings guidance visible.

The desktop's v1 URL is parsed by the rules in [`CONTRACT.md`](CONTRACT.md) "Connection QR v1".
Recognition stops after one result. Valid input fills an unsaved PUSH draft and returns to the editor;
invalid input shows a fixed error and a rescan option. Cancel, invalid codes and late frames never save
or register anything. Drafts survive activity recreation. Recognition shows a high-contrast **QR SCANNED**
panel explaining that the connection is filled but not yet registered. A single **SAVE AND REGISTER**
writes private settings and queues the existing registration work, showing an English progress/result
dialog for this request. **CONNECTED** names the watcher only after HTTP registration is accepted and
its current configuration/address is confirmed. Registration does not prove message delivery; the
dialog asks for a backend test. **DONE** returns to the in-app list without finishing the activity.
While waiting, users may continue in the background. A failed attempt offers **RETRY NOW** and
**REVIEW SETTINGS**; automatic retries remain bounded. Observe the specific request across activity
recreation with lifecycle-aware collection. Earlier successes, skipped work, changed/deleted watchers,
and cancelled requests cannot produce a new successful confirmation. Private result metadata contains
a confirmation flag and address fingerprint, never the raw address or credential.

A new scanned watcher starts with an available `Even-PIlot` name (numeric suffix when needed), the
ordinary length limit, and 30-second expiry. Subsequent scans retain draft preferences. Scanning an
existing watcher preserves name, durations and length while replacing URL/credential and selecting PUSH.
When a new draft matches an existing registration endpoint, even with a rotated key, the user chooses
an existing watcher to update or cancels; no duplicate is silently created. Save checks again. Scanning
the same connection and retrying registration retain the existing subscription ID.

Registration uses the same Firebase installation and backend request. HTTP 401/403 explicitly identify a
rejected key and suggest rescanning. Connection failures retain the watcher and its bounded retry path;
the editor and list explain how to retry. No QR key enters logs, analytics, queries or error text.

### Acceptance

- `scan-1` Live camera recognition fills the editor once; cancellation and invalid input do not mutate saved watchers.
- `scan-2` IPv4, HTTPS, bracketed IPv6 and ports come from the QR; form decoding happens once and duplicate parameters are refused.
- `scan-3` New defaults and unique names are applied; existing preferences survive scans and duplicate connections require an explicit selection.
- `scan-4` Save uses the current registration contract; retries/rescans retain identity and failures never delete the watcher.
- `scan-5` Permission denial, unavailable camera, bad codes, unreachable backend and rejected credentials have visible recovery actions.
- `scan-6` Camera preview stops on background/exit, produces no image files, and never exposes the QR/key in logs or screenshots.
- `scan-7` A recognized connection shows QR SCANNED with an explicit save instruction. Saving a push
  watcher shows progress and a watcher-specific result; a success from an earlier request cannot confirm this save.
- `scan-8` Only the current confirmed registration can display CONNECTED. Waiting/retry/failure/cancellation
  stay distinct, errors offer retry and review, and confirmation does not claim that a message reached G2.
