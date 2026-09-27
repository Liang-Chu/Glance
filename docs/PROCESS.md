# Process

> How this project is built, run, deployed, and reviewed **today**. Current state only.
> Git history preserves superseded procedures; this file describes current work.

Commands are written for Git Bash on Windows, where the wrapper is `./gradlew.bat`. On a POSIX
shell it is `./gradlew` and everything else is unchanged.

## 1 · Build the app

**When**: any change under `app/`.
**Preconditions**: JDK 17 on `JAVA_HOME`, and an Android SDK with platform 37 and build-tools 37
installed. `local.properties` points at it and is not committed.

1. `./gradlew.bat checkFileLength`
2. `./gradlew.bat testDebugUnitTest lintDebug`
3. `./gradlew.bat assembleDebug` for development, or `assembleRelease` for anything shipped
4. `python -m unittest discover -s examples/test-backend -p 'test_*.py' -v` and `bash tools/check-docs.sh`

Release builds enable R8 and resource shrinking. Publish a minified release signed with the
publisher key, never a debug build or debug-signed APK.

A release built or signed by hand is written under `local/release/`, never at the repository root —
the signer leaves an `.idsig` beside its output, one `git add` away from a commit.

For a publishable signed release, create a private signing key and an ignored
`local/keystore.properties` with `storeFile`, `storePassword`, `keyAlias`, and `keyPassword`.
`storeFile` is absolute or relative to the repository root. Never commit either file. Gradle signs
release builds when this file exists; otherwise it produces `app-release-unsigned.apk`. Never
publish an APK signed with the Android debug key. Keep the same publisher key for future updates;
changing it prevents in-place updates of existing installations. The published 1.0 APK and earlier test APKs used a debug key. Version 2.0 begins a dedicated
publisher signature, so those installations need uninstall/reinstall and fresh configuration.
Back up the private key and signing properties securely; do not attach them to a GitHub release.

CI in `.github/workflows/check.yml` runs the same checks and builds a minified unsigned APK without
Firebase configuration or signing secrets. Actions are pinned to commits; the Gradle distribution
has a checksum. CI execution on GitHub is separate from local verification.
SDK setup pins command-line tools 19.0 and requests `platform-tools` explicitly; the action's default
also requests the retired `tools` package. The platform package is `platforms;android-37.0`, and the
build tools package is `build-tools;37.0.0`. These match the locally verified toolchain.

**Verify**: `app/build/outputs/apk/debug/app-debug.apk` exists and
`"$ANDROID_HOME/build-tools/37.0.0/aapt2.exe" dump badging app/build/outputs/apk/debug/app-debug.apk`
reports `dev.liamchu.glance`.
**On failure**: a `C3 breach` means split the file named — do not raise the number; that is a
decision, and [`CONSTRAINTS.md`](CONSTRAINTS.md) says how it is recorded. A failing contract test
means either the code or [`CONTRACT.md`](CONTRACT.md) is wrong; decide which before changing either.

## 2 · Install it on a phone

**When**: verifying anything. `C9` counts the running thing, and a build log is not one.
**Preconditions**: USB debugging on, the phone authorised, `adb devices` listing it.

1. `"$ANDROID_HOME/platform-tools/adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk`
2. Open Glance, fill in the settings, allow notifications when asked.
3. In the Even Realities app, under Notifications, allow **Glance**.
4. For PUSH, follow "Firebase setup for push". For POLL, select it and press **Save and check now**.
5. Leave the phone alone and confirm subsequent messages arrive unattended.

**Verify**: the notification appears on the phone and is readable on the glasses. Neither one alone is
the verification. **Save and check now** proves the call and the notification only; that the app
wakes *itself* is proven by leaving it alone and seeing a run arrive unattended.
**On failure**: use "Collect diagnostic logs" below; with USB debugging, also capture
`adb logcat -b crash -d` and `adb logcat -s Glance:* WM-WorkerWrapper:*` while a run is due.

## Collect diagnostic logs

After a failure, reopen Glance and choose **DIAGNOSTICS → EXPORT LOGS**. Save the text file using
Android's document picker and attach it to the bug report, with the approximate time and action that
failed. Logs are kept locally and never uploaded automatically. Clearing cache preserves them;
export before uninstalling or clearing app data. **CLEAR LOCAL LOGS** does not remove exported files
or Android's own exit history.

The export contains recent typed events, HTTP statuses, the last recorded JVM crash, and device/app
versions. Exception messages, URLs, credentials, Firebase IDs, watcher names and notification content
are excluded. Android 11+ adds up to five OS process-exit records; older app versions may appear there.
ANRs, native crashes, abrupt kills, very early startup failures or storage exhaustion may have no saved
JVM stack. If Glance cannot reopen, collect `adb logcat -b crash -d` while connected; inspect raw logcat
for private data before sharing it. Exporting does not recover stack traces from before logging was installed.

For every distributed minified release, archive `app/build/outputs/mapping/release/mapping.txt` beside
the exact APK, record its version and SHA-256, and use that mapping with R8 Retrace when reading the
exported stack. A rebuilt APK's mapping is not interchangeable. Keep artifacts outside the source tree
or under ignored `local/`. No intentional crash button is shipped to users.

**Verify**: `DiagnosticsTest` checks persistent files, rotation, concurrent writes, privacy, handler
delegation and worker exception semantics. On a device, export after a real failure and inspect the
file; JVM tests do not verify Samsung's document picker or OS exit history.

Official platform reference: [ApplicationExitInfo](https://developer.android.com/reference/android/app/ApplicationExitInfo).

For the save-time crash regression, run `./gradlew.bat testDebugUnitTest --tests '*PushAutoInitTest'`.
The test supplies an SDK boundary that rejects its UI dispatcher, reproducing the exported 1.3 failure
without real Firebase credentials. Both enable and disable must run off UI; unavailable configuration
must skip the SDK, and cancellation/errors must propagate. On the phone, confirm **SAVE AND REGISTER**
stays in the list and the next export contains **SETTINGS_SAVE_FINISHED** without a newer crash. The
retained 1.3 crash remains visible until explicitly cleared; its old timestamp is not a new failure.

## Firebase setup for push

1. Create a project in [Firebase Console](https://console.firebase.google.com/); skip Analytics.
2. **Project settings → General → Your apps → Add app → Android**: register `dev.liamchu.glance`.
3. Download `google-services.json` to the phone. In Glance: **FIREBASE SETUP → IMPORT CONFIG FILE**.
   Skip SDK installation. No rebuild is needed; client configuration is stored locally without backup.
   Confirm the project ID in the success dialog. Returning home shows it on a black setup button;
   before setup the button is white. Import failure/cancellation must keep the existing project.
4. **Project settings → Cloud Messaging**: enable **Firebase Cloud Messaging API (V1)** if disabled.
5. Authorize your sending backend using one of the options below.
6. Add a PUSH watcher, scan its connection QR or enter URL/credential, then **SAVE AND REGISTER**.
7. Wait for **REGISTERED FOR PUSH** and send a test. Check phone display, expiry and G2 forwarding.

### Backend authorization

- **Same Firebase project:** configure Application Default Credentials for a service account with
  FCM sending permission. If your backend requires a JSON key, open **Project settings → Service
  accounts → Generate new private key** and configure it on the backend only.
- **Backend's account belongs to another project:** in the target project's Google Cloud IAM,
  grant that account **Firebase Cloud Messaging API Admin** (`roles/firebasecloudmessaging.admin`).
  Enable the FCM API in the account's project too. Send to
  `/v1/projects/TARGET_PROJECT_ID/messages:send` using the backend's own credentials.

All PUSH watchers share the imported project. Only authorize trusted senders: the IAM grant covers
the project, not a single watcher. Never import service-account private keys into Glance. The backend
learns the target project from `firebase_project_id` in registration; client API keys are never sent to it.

Replacing the imported project rotates subscriptions and pauses push. Follow the app's **Force stop**
and reopen instructions; watchers are retained. Identical reimport preserves subscriptions. First
import works immediately. The public SDK does not provide an independent FirebaseApp selector for
each watcher.

Official references: [Android setup](https://firebase.google.com/docs/android/setup),
[FCM registration](https://firebase.google.com/docs/cloud-messaging/android/get-started),
[cross-project authorization](https://firebase.google.com/docs/cloud-messaging/send/v1-api#authorize_a_service_account_from_a_different_project).

## Verify connection scanning

Run `./gradlew.bat testDebugUnitTest --tests '*Pairing*'` for the parser and scan-to-registration model/HTTP
tests, then the full build checks. Tests use synthetic keys and loopback servers; they do not contact a
real Firebase project or register a subscription in the owner's backend.

On a configured phone, open **+ NEW WATCHER → SCAN CONNECTION QR** and scan the existing desktop
**Connect phone · QR**. Allow camera access. Recognition must stop immediately and show the filled
editor. On Android 17+, use **ALLOW LOCAL NETWORK** for a LAN backend. Tap **SAVE AND REGISTER** once;
Glance must remain open on the list and eventually show
**REGISTERED FOR PUSH**. Check the backend's subscription, then deliver a normal FCM message to phone/G2.

Repeat in an existing watcher after changing expiry/length/name; only connection fields and PUSH mode
should change. From a new watcher, scan that same endpoint and choose **UPDATE**; confirm no extra
watcher/subscription. Cancel a scan, rotate the phone, background/reopen the scanner, deny camera
permission, and try a non-connection QR. Original settings must remain intact and recovery must be visible.
Test an offline PC and an invalid synthetic credential: save must retain the watcher, show failure/retry,
and recover when connectivity/key is corrected. Never capture the real QR or credential in debug screenshots.
The LAN permission follows [Android's local network protection](https://developer.android.com/privacy-and-security/local-network-permission).
Scanning uses [ZXing Android Embedded](https://github.com/journeyapps/zxing-android-embedded) in the app's own screen.

## 3 · Land a change

1. Update code and its owning DESIGN acceptance criteria, STATUS evidence and CONTRACT when relevant.
2. Keep the user-facing README consistent; link detailed wire formats instead of duplicating them.
3. Remove completed BACKLOG items and record any newly identified open work there.
4. Run build, test and documentation checks. Inspect every line of `git status --short`.
5. Stage explicit project paths and commit with a message describing the change. Never stage `local/`.

## 4 · Record a decision

Record the decision and its reason in the owning DESIGN section, update its acceptance criteria
and decision index, and fix contradictory code/docs in the same change. Keep only the current
decision; Git history preserves the previous wording. Unresolved choices belong in BACKLOG.

## 5 · Audit the documentation

Run `bash tools/check-docs.sh` before release and after renaming or removing documents. Fix broken
links, stale code-map entries and contradictions. The script must print `Documentation checks passed.`
Follow the lifecycle in [README.md](README.md); do not create empty scaffolding or duplicate guides.

## 6 · Activate a reserved slot

Only activate `docs/PARKED.md` when C8 identifies a complete component with a real target but missing
wiring. Record both, link it from [README.md](README.md), and remove it when no parked items remain.
Other new documents need a distinct purpose and an entry in the documentation index.

## 7 · Put down a file that must not reach GitHub

Store keys, signed artifacts, local configuration, logs, captures and temporary notes under `local/`,
which is ignored whole. Tool-owned build/cache paths and common secret filenames are also ignored.
Check `git status --short` and `git ls-files -ci --exclude-standard` before committing. If a real
secret was committed, rotate it; deleting the file does not remove it from Git history.

## Publish a release

1. Build and verify the signed production APK, including version, signature and alignment.
2. Retain its exact R8 mapping, version, SHA-256 and source snapshot privately for crash diagnosis.
3. Test the final signed APK on a phone: import, QR/manual setup, save staying in-app, registration,
   background push and expiry. Record the result in STATUS; list unverified conditions in BACKLOG.
4. Commit the reviewed source and push it to GitHub. Wait for CI to pass, then tag that commit.
5. Create a GitHub Release with the version tag, user-facing changes and upgrade instructions.
6. Attach the signed APK and SHA-256 checksums. Never upload signing keys, Firebase JSON, logs or
   private build directories. GitHub supplies source archives from the tag.
