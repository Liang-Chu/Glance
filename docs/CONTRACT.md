# Contract — the app and the user's backend

*The app calls one backend the user wrote; this file is the whole of what the two sides agree on.
Everything the notification says is decided on the backend's side of it.*

Owner: this repository defines the shape. Polling has been observed on a phone (2026-09-10).
`BackendContractTest` exercises polling against real loopback HTTP. `PushContractTest` exercises
push registration/removal over HTTP and message validation locally (2026-09-25). The owner reported FCM working on Samsung; the new configuration-import flow has not been device-tested; [`STATUS.md`](STATUS.md) records that boundary.

What a backend author still has to build is theirs; what this app still has to build is
[`BACKLOG.md`](BACKLOG.md), not this file.

## The call

This section describes **polling**. Push watchers use "Push delivery" below.

One `POST`, to the URL the user configured, exactly as they typed it. **The app appends no path and
adds no query parameters** — if the backend wants a path, it belongs in the URL the user enters.

```
POST <the configured URL>
Content-Type: application/json
Authorization: Bearer <the configured credential>     ← omitted entirely when the credential is empty
User-Agent: Glance/<version>

{
  "watcher": "Kotlin tips",
  "max_length": 80,
  "title_max_length": 32,
  "expires_after_seconds": 3,
  "interval_minutes": 1440,
  "client": "Glance"
}
```

Every limit the app will enforce is stated in the request, so a backend never has to hardcode one of
this project's numbers or guess at it. The two that matter are `max_length` and `title_max_length`:
exceed either and the run fails.

| Field | Means |
| --- | --- |
| `watcher` | the name the user gave this watcher, so one backend can serve several |
| `max_length` | the most characters `text` may have |
| `title_max_length` | the most characters `title` may have |
| `expires_after_seconds` | how long the notification will be visible once posted |
| `interval_minutes` | how long until the next call, so a backend can pace itself |
| `client` | the app asking. Unversioned on purpose, so a backend cannot branch on a version it did not choose; the `User-Agent` header carries the version for debugging |

| Clause | Shape | Verified |
| --- | --- | --- |
| method | `POST`, always | test "posts to the configured url" |
| URL | the configured URL verbatim; no path appended, no query added | same test — the server answers one path only, so an appended path would miss it |
| limits it states | are the limits it then enforces | test "the limits it sends are the ones it then enforces" |
| `Content-Type` | `application/json` | test "posts to the configured url" |
| `Authorization` | `Bearer <credential>`; the header is **absent**, not empty, when no credential is set | tests "sends the credential as a bearer token" and "omits the authorization header entirely when no credential is set" |
| body | a JSON object carrying the fields above | test "tells the backend every limit it has to work within" |
| redirects | not followed. A redirect is a failed run | test "a redirect is not followed" |
| timeouts | 10 s to connect, 30 s to read | **implemented, not asserted by any test** |
| transport | `http` or `https`; cleartext is permitted so a backend on the user's own network works | manifest sets `usesCleartextTraffic`; needs a device to confirm |
| retries | none. One attempt per run; the next run is the retry | **implemented, not asserted by any test** |
| unreachable | a failed run, named as such | test "an unreachable backend is a failure" |

## Responses

Three outcomes, and nothing else is defined.

| Status | Body | What the app does | Verified |
| --- | --- | --- | --- |
| `200` | `{"title": "...", "text": "..."}` | posts the notification | test "two hundred with title and text is content" |
| `204` | none | posts nothing; **this is not a failure** | test "two hundred and four is not a failure" |
| anything else | ignored | failed run | test "a server error is a failure carrying the status" |

On `200`, **both members must be non-empty JSON strings.** Numbers, booleans, objects, arrays and
null are rejected instead of being converted to text. The entire response body, including extra
fields, is limited to **64 KiB** and read with a bounded buffer. There are no optional fields and
no defaults: a response missing `title`, missing `text`, or carrying an empty string for either is a
failed run, per [`CONSTRAINTS.md`](CONSTRAINTS.md) "C1 — Fail fast; no fallbacks". Unknown extra
members are ignored, so a backend may return more without breaking anything.

| Limit | Value | Verified |
| --- | --- | --- |
| `text` length | at most the `max_length` sent in the request | tests "text longer than max length is refused rather than trimmed" and "text exactly at max length is accepted" |
| `title` length | at most 32 characters | test "title longer than its limit is refused" |
| missing or empty member | a failed run, never a blank notification | tests "a missing text is a failure and not an empty notification" and "an empty text is a failure" |
| body that is not JSON | a failed run | test "a body that is not json is a failure" |
| unknown extra members | ignored | test "unknown members are ignored rather than rejected" |

**An over-length response is a breach, not something to trim** (owner, 2026-09-10). The app does not
truncate: it sent the limit, the backend agreed to respect it, and quietly cutting the text would
make the app change what the backend said. A run that receives one posts nothing and is a failed run.

`204` exists so that a backend with nothing to say does not have to invent something. A run that ends
this way is silent and leaves the failure state alone — see [`DESIGN.md`](DESIGN.md) "What a failed
run shows".

## Worked example

A backend that always has something to say, in any language, is this much:

```
POST /glance  ->  200
{"title": "Kotlin", "text": "buildList { } beats mutableListOf when you only build the list once."}
```

And one that is rate-limiting itself to office hours returns `204` the rest of the time.

Testable before the app exists:

```bash
curl -sS -X POST "$URL" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $CREDENTIAL" \
  -d '{"max_length": 80, "title_max_length": 32}' -i
```

**Breaking a clause is a decision**, recorded and coordinated before it ships — never an edit to this
file on its own.

## Push delivery

The same configured URL, authentication header, redirect policy and connection/read timeouts apply.
The URL accepts both operations below and returns **204 with no body** on success. A 200 polling
response is not a successful registration. Registration/removal each retry up to five attempts
using WorkManager exponential backoff; polling still makes one attempt per scheduled run.

### Register or update a subscription

```json
{
  "operation": "register_push",
  "subscription_id": "a-unique-watcher-id",
  "installation_id": "the-phone-firebase-installation-id",
  "firebase_project_id": "the-users-target-project",
  "watcher": "Kotlin tips",
  "max_length": 80,
  "title_max_length": 32,
  "expires_after_seconds": 3,
  "client": "Glance"
}
```

Upsert by `subscription_id`. Update the installation ID and all settings when registration repeats;
several watchers may share one installation. `firebase_project_id` identifies the user-imported
target project and was added in 1.1; backends should tolerate additional request fields. The backend decides when to send; no polling interval
is sent. Glance registers on save and SDK address changes; opening the app retries only registrations
that are not confirmed. Registration is not a heartbeat or a backend health check. Backends must retain
accepted subscriptions until removal or invalidation; SAVE AND REGISTER can restore a lost subscription.
A successful response means
registration was accepted, not that notification delivery is confirmed.

### Send content

Authenticate your backend to the app's Firebase project using a service account or Application
Default Credentials with FCM sending permission. These credentials never belong in the APK, the
watcher's Credential field, source control or a registration response. The watcher's credential is
only for authenticating Glance to your own backend. All configured push backends are trusted senders.

Send through the `firebase_project_id` received during registration. A service account from another
project works after the user grants it FCM sending permission on this target project. Do not assume
the target project is the one that issued the backend key. On a backend serving multiple users,
validate allowed target projects against your authenticated account configuration. The app sends no
Android API key or service-account key. See [authorization](https://firebase.google.com/docs/cloud-messaging/send/v1-api#authorize_a_service_account_from_a_different_project).

Send to `POST https://fcm.googleapis.com/v1/projects/PROJECT_ID/messages:send`:

```json
{
  "message": {
    "fid": "the-installation_id-from-registration",
    "data": {
      "subscription_id": "the-subscription_id-from-registration",
      "title": "Kotlin",
      "text": "Your build finished."
    },
    "android": { "priority": "HIGH", "ttl": "30s" }
  }
}
```

Use **data only**: do not include a `notification` object or use Firebase's notification composer.
Those messages can be displayed by the SDK in the background, bypassing Glance's validation and
expiry. The installation address uses the current FCM `fid` field. Payload data values are strings.
`title` and `text` obey the same validation as polling, including UTF-16 code-unit length as counted
by Kotlin. FCM also limits message size; the example sender checks it before transmission.

The 30-second queue TTL above is an example, chosen by the backend, not a polling delay or the
notification's display duration. Use HIGH for time-sensitive visible content and NORMAL for other
updates; high-priority delivery is still best-effort. Unknown or retired subscription IDs are ignored.
There is no delivery history, replay recovery or exactly-once guarantee. A missing-message callback
produces a failure notice rather than claiming the messages arrived.

### Remove a subscription

```json
{"operation":"unregister_push","subscription_id":"the-old-subscription-id","client":"Glance"}
```

Removal is idempotent: return 204 even if already removed. Keep a tombstone and reject later
registration for the retired ID, since an old network request could arrive after removal.
Glance retires the old ID when the watcher is deleted or its URL, credential, delivery mode or imported Firebase configuration changes.
It stops routing the old ID immediately, then attempts backend removal. If removal exhausts its
retries, the app discards the retained removal credential; clean up that backend record separately. Removing a watcher does not revoke other watchers.

Reference implementation and local tests: [`../examples/test-backend/README.md`](../examples/test-backend/README.md).
Protocol references: [FCM message format](https://firebase.google.com/docs/reference/fcm/rest/v1/projects.messages),
[Android receipt](https://firebase.google.com/docs/cloud-messaging/android/receive-messages),
and [priority](https://firebase.google.com/docs/cloud-messaging/android-message-priority).
## Connection QR v1

Even-PIlot desktop's **Connect phone · QR** supplies a URL of this form:

```text
http://<host>:<port>/#pilot-pair=1&pilot-token=<form-encoded-connection-key>
```

Glance and Even Hub use the same QR and backend key. This is a local configuration input only;
it changes none of the polling, `register_push`, FID, Firebase authorization or FCM message contracts.

- Accept HTTP/HTTPS with a host, valid port, no user-info, no query and only an empty or `/` root path.
  Accept bracketed IPv6 and HTTPS domains. Read the entire origin from the QR; no deployment address is embedded.
- Parse the **raw fragment**, splitting parameters before decoding. Decode names/values exactly once
  as UTF-8 form data: `+` becomes space and `%2B` becomes `+`. Require one `pilot-pair=1` and one
  `pilot-token`; reject duplicate parameter names, missing fields and other versions. Unknown single
  parameters are ignored. Malformed URL/percent syntax and raw input over 8,192 UTF-16 units are refused.
- Require a key of 24–512 UTF-16 units without CR/LF; preserve whitespace and percent characters.
  The existing HTTP credential validation still requires printable ASCII before Save, without changing
  the key or silently encoding it a second time.
- Build the registration URL from the normalized origin plus `/api/glance`. Put the decoded key in
  Credential; the existing HTTP layer adds `Authorization: Bearer `. The key never enters a request URL.
- New scans use PUSH, an available `Even-PIlot` name, max length 80, title limit 32 and expiry 30 seconds.
  Editing an existing watcher retains its name and notification preferences. Duplicate endpoints offer
  explicit updates rather than another registration. Successful save still requires backend HTTP 204.

`PairingConnectionTest` covers parsing, encoding, validation and single-result/cancellation guards.
`PairingFlowTest` covers preferences, duplicate matching, identity reuse, draft restoration, generated QR
pixels through decoding into a saved-format watcher and loopback registration, plus 401/403 and offline failures.
Real camera/permission dialogs and Firebase/G2 delivery require device verification; see [`STATUS.md`](STATUS.md).
