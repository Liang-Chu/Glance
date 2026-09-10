# Glance

An Android app that asks a backend **you** run for something to say, on a schedule you set, and shows
it as a notification that clears itself.

Built to feed [Even Realities G2](https://www.evenrealities.com/) glasses through their notification
feature — but it posts an ordinary Android notification, so anything that reads those will see it.

**Glance has no backend of its own.** No API keys, no accounts, no server. A fresh install talks to
nothing until you point it at something you control.

You can have several **watchers**, each named, each with its own backend, schedule and limits. They
share nothing: separate notifications, separate failures, separate schedules.

## Install

Download an APK from releases, or build one:

```bash
./gradlew assembleDebug
```

Needs JDK 17 and Android SDK 37 — both produce confusing errors if wrong. Everything else is a
standard Android build.

## Set it up

1. **+ NEW WATCHER** — give it a name, a backend URL, and a credential if your backend wants one.
2. **Save and check now** — runs one check immediately instead of waiting out the interval.
3. Allow notifications when asked.
4. In the Even Realities app, under **Notifications**, allow **Glance**.

A new watcher starts at once a day, gone after three seconds, 80 characters.

## Write a backend

Full specification: [`docs/CONTRACT.md`](docs/CONTRACT.md). Working reference implementation:
[`examples/test-backend/`](examples/test-backend/), about a hundred lines of dependency-free Python.

Glance sends one `POST`, with `Authorization: Bearer <credential>` when you set one:

```json
{
  "watcher": "Kotlin tips",
  "max_length": 80,
  "title_max_length": 32,
  "expires_after_seconds": 3,
  "interval_minutes": 1440,
  "client": "Glance/0.1"
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

Both members are **required and non-empty** — a missing one is a failed run, not a blank
notification. Glance **does not truncate**: exceed `max_length` or `title_max_length` and the run
fails. Your URL is called exactly as typed, redirects are not followed, and there is one attempt per
run with no retry.

Plain `http` works, so a box on your own network is fine — but the credential crosses the network in
the clear, so use `https` off a network you trust.

## Known limits

- **Checks happen at most every 15 minutes** — WorkManager's floor for repeating work. Ask for less
  and Android silently rounds up, so Glance refuses instead.
- **Notifications live at least 3 seconds**, or they risk vanishing before the glasses are handed
  them.
- **How much text the glasses display is unmeasured.** 86 characters have been read; the ceiling is
  unknown — [`docs/reference/g2-notifications.md`](docs/reference/g2-notifications.md).
- **Intervals are best-effort.** Android batches background work when the device is idle.

## Documentation

Everything starts at [`docs/README.md`](docs/README.md). [`DESIGN.md`](docs/DESIGN.md) is why anything
is the way it is; [`STATUS.md`](docs/STATUS.md) is what is actually true right now, including what has
never been run.

Contributing: read [`docs/PROCESS.md`](docs/PROCESS.md). The rule that matters most is that
**documentation moves in the same commit as the code**.

## Licence

MIT — see [`LICENSE`](LICENSE).
