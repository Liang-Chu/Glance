# Blip

An Android app that asks a backend **you** run for something to say, on a schedule you set, and shows
it as a notification that clears itself.

Built to feed [Even G2](https://www.evenrealities.com/) glasses through their notification feature,
but it is an ordinary Android notification and anything that reads those will see it.

## What it does

*Aspirational: this section describes the intended product. What is actually built is
[`docs/STATUS.md`](docs/STATUS.md).*

- Calls one backend you wrote, at your URL with your credential, on an interval you choose.
- Turns the reply into a notification that takes itself away again, so you never deal with it on the
  phone.
- Decides nothing about what the notification says — that is your backend's job, and the shape of the
  call between you is [`docs/CONTRACT.md`](docs/CONTRACT.md).
- Ships with no keys, no accounts, and no server of its own. A fresh install talks to nothing until
  you point it somewhere.

## Connect a backend

Blip has no backend of its own — you run one, and Blip calls it. In the app, fill in the backend
URL, an optional credential, how often to check, how long a notification lasts, and the maximum
length. Then **Save and schedule**.

On each run Blip sends:

```http
POST <your URL>
Content-Type: application/json
Authorization: Bearer <your credential>     # the header is absent when the credential is blank
User-Agent: Blip/<version>

{"max_length": 120}
```

and expects exactly one of three answers:

```jsonc
// 200 — show this
{"title": "Kotlin", "text": "buildList { } beats mutableListOf when you build the list once."}

// 204 — nothing to say right now. Blip stays silent, and this is not an error.

// anything else — a failed run. Blip says so once, then stays quiet until a run succeeds.
```

Worth knowing before you write it:

- **`title` and `text` are both required and both non-empty.** A missing one is a failed run, not a
  blank notification — Blip never invents what you did not send.
- **Blip does not truncate.** `text` must be within the `max_length` it sent and `title` within 32
  characters; over-length is a failed run.
- **Your URL is called exactly as typed** — no path appended, no query added, redirects not followed.
- **One attempt per run, no retry.** 10 s to connect, 30 s to read. The next run is the retry.
- **Plain `http` works**, so a box on your own network is fine. Over `http` your credential crosses
  the network in the clear — use `https` for anything off a network you trust.

Try it before installing anything:

```bash
curl -sS -X POST "$URL" -H 'Content-Type: application/json'   -H "Authorization: Bearer $CREDENTIAL" -d '{"max_length": 120}' -i
```

**[`docs/CONTRACT.md`](docs/CONTRACT.md) is the specification and wins over this section**, which is
only the quickstart. If the two ever disagree, that file is right and this one is a bug.

## Start here

- Writing the backend — [`docs/CONTRACT.md`](docs/CONTRACT.md)
- What exists and what runs today — [`docs/STATUS.md`](docs/STATUS.md)
- Why anything is built the way it is — [`docs/DESIGN.md`](docs/DESIGN.md)
- How to build, run, and review — [`docs/PROCESS.md`](docs/PROCESS.md)

Everything else starts at [`docs/README.md`](docs/README.md) — the only entrance to the docs.
