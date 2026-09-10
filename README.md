# Glance

An Android app that asks a backend **you** run for something to say, on a schedule you set, and shows
it as a notification that clears itself.

Built to feed [Even Realities G2](https://www.evenrealities.com/) glasses through their notification
feature — but it posts an ordinary Android notification, so anything that reads those will see it.

**Glance has no backend of its own.** No API keys, no accounts, no server. A fresh install talks to
nothing until you point it at something you control.

Several **watchers** can run at once, each named, each with its own backend and settings. They share
nothing: separate notifications, separate failures, separate schedules.

## Install

Download an APK from releases, or build one:

```bash
./gradlew assembleDebug
```

Needs JDK 17 and Android SDK 37 — both produce confusing errors if wrong. Everything else is a
standard Android build.

## Set it up

1. **+ NEW WATCHER** — name, backend URL, and a credential if your backend wants one.
2. **Save and check now** — runs one check immediately instead of waiting out the interval.
3. Allow notifications when asked.
4. In the Even Realities app, under **Notifications**, allow **Glance**.

## Write a backend

Specification: [`docs/CONTRACT.md`](docs/CONTRACT.md). Reference implementation:
[`examples/test-backend/`](examples/test-backend/), dependency-free Python.

Glance sends one `POST`, with `Authorization: Bearer <credential>` when you set one:

```json
{
  "watcher": "Kotlin tips",
  "max_length": 80,
  "title_max_length": 32,
  "expires_after_seconds": 3,
  "interval_minutes": 1440,
  "client": "Glance"
}
```

`watcher` is the name you gave it, so one backend can serve several. Every limit Glance enforces is
in the request, so you never hardcode its numbers.

It expects one of three answers:

| Status | Body | Glance does |
| --- | --- | --- |
| `200` | `{"title": "...", "text": "..."}` | shows it |
| `204` | none | nothing, silently — not an error |
| anything else | ignored | one failure notice, then quiet until a run succeeds |

`title` and `text` are both **required and non-empty**, and Glance **does not truncate** — exceed a
limit and the run fails rather than showing something you did not write. One attempt per run, no
retry, redirects not followed.

Plain `http` works, so a box on your own network is fine — but the credential crosses the network in
the clear, so use `https` off a network you trust.

## Known limits

- **Checks happen at most every 15 minutes** — WorkManager's floor for repeating work. Ask for less
  and Android silently rounds up, so Glance refuses instead.
- **Intervals are best-effort.** Android batches background work when the device is idle, so a check
  can land late.
- **How much text the glasses display is unmeasured** —
  [`docs/reference/g2-notifications.md`](docs/reference/g2-notifications.md).

## Documentation

Start at [`docs/README.md`](docs/README.md). [`STATUS.md`](docs/STATUS.md) is what is actually true
right now, including what has never been run. Contributing: [`docs/PROCESS.md`](docs/PROCESS.md).

## Licence

MIT — see [`LICENSE`](LICENSE).
