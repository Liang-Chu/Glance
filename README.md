# Glance

Receive short messages from **your own backend** as Android notifications that clear themselves.
Built for [Even Realities G2](https://www.evenrealities.com/): the Even app forwards Glance's ordinary
Android notifications to your glasses. Glance does not connect to the glasses directly.

- **Push by default:** receive backend updates through Firebase Cloud Messaging (FCM).
- **Your project, your backend:** import your Firebase configuration on the phone; no shared Glance server.
- **Multiple watchers:** separate backends, credentials, notification limits and expiry times.
- **Scan to connect:** scan the existing Even-PIlot desktop QR to fill a PUSH watcher.
- **Optional polling:** scheduled checks with a minimum interval of 15 minutes.
- **Local diagnostics:** export recent events and crash details when something goes wrong.

## Install

Get the APK from [Releases](https://github.com/Liang-Chu/Glance/releases). Requires Android 8.0+;
push also requires Google Play services and connectivity to FCM.

**Upgrading to 2.0:** this is the first release with a dedicated publisher signing key. The old
1.0 APK and development test APKs used a debug key, so Android cannot update them in place.
Export any needed diagnostics and keep your Firebase JSON and backend settings, uninstall the old
app, then install and configure 2.0. Uninstalling removes local settings and logs. Later official
releases will use the same publisher key and support normal updates.

## Set up push

1. In [Firebase Console](https://console.firebase.google.com/), create a project. Skip Analytics.
2. Open **Project settings → General → Your apps → Add app → Android**.
   Enter package name `dev.liamchu.glance` and register the app.
3. Download `google-services.json` to your phone. In Glance, open **FIREBASE SETUP → IMPORT CONFIG FILE**.
   Skip Firebase's SDK installation steps; no APK rebuild is needed.
4. In **Project settings → Cloud Messaging**, check that **Firebase Cloud Messaging API (V1)** is
   enabled. Give your backend sending permission using the [backend setup instructions](docs/PROCESS.md#firebase-setup-for-push).
5. In Glance, tap **+ NEW WATCHER**. Enter the name, registration URL and backend credential, or tap
   **SCAN CONNECTION QR** and scan Even-PIlot's **Connect phone · QR**.
6. Tap **SAVE AND REGISTER**. Wait for **REGISTERED FOR PUSH**, then send a test from your backend.
7. Allow Glance notifications. In the Even Realities app, enable **Glance** under **Notifications**.

All PUSH watchers share the imported Firebase project. Each backend can use its own service account
if you authorize it for that project. **Service-account private keys stay on the backend.**
Changing the imported project requires a force-stop and reopen; Glance shows the steps.

After import, a confirmation shows your project ID. The home screen's white **FIREBASE SETUP**
button turns black and displays that ID; tap it to review or replace your configuration.

For polling, skip Firebase, select **POLL** in the watcher and tap **SAVE AND CHECK NOW**.

## Connect a backend

Use the [backend contract](docs/CONTRACT.md) and [runnable Python examples](examples/test-backend/README.md).

| Mode | Backend behavior |
| --- | --- |
| PUSH | Accept registration with an installation ID, subscription ID and target Firebase project; return HTTP 204. Send data-only FCM messages containing `subscription_id`, `title` and `text`. |
| POLL | Accept a POST with the watcher name and limits. Return HTTP 200 with `title` and `text`, or 204 when there is nothing to show. |

Glance sends the backend credential as a Bearer token. Content must fit the requested limits;
Glance rejects oversized messages instead of truncating them. Plain HTTP is supported for trusted
private networks; use HTTPS across untrusted networks.

## Troubleshooting

Open **DIAGNOSTICS → EXPORT LOGS** and attach the file to your bug report, with the action and time.
Logs stay on the phone until exported. They exclude credentials, backend URLs, Firebase IDs and
notification content. Clearing cache preserves logs; uninstalling or clearing app data removes them.
See [diagnostic details](docs/PROCESS.md#collect-diagnostic-logs).

Push and polling are best-effort: connectivity and Android power management affect delivery.
Force-stopping Glance prevents push until it is reopened. Polling may run later than its interval.
Registration confirms backend acceptance; a test message verifies delivery. Device observations and
remaining verification are recorded in [STATUS.md](docs/STATUS.md) and [BACKLOG.md](docs/BACKLOG.md).

## Build and contribute

Use JDK 17 and Android SDK 37. No Firebase configuration is needed to build.

```bash
bash gradlew checkFileLength testDebugUnitTest lintDebug assembleRelease
python -m unittest discover -s examples/test-backend -p 'test_*.py' -v
bash tools/check-docs.sh
```

On Windows, use `gradlew.bat`. Release builds are minified and unsigned unless you provide your
own signing key; see [build and signing](docs/PROCESS.md#1--build-the-app). Use `assembleDebug` for
development. Never publish a debug-signed APK or commit keys, local configuration or diagnostic logs.

Project documentation: [docs/README.md](docs/README.md). License: [MIT](LICENSE).
