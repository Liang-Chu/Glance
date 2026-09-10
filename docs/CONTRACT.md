# Contract — the app and the user's backend

*The app calls one backend the user wrote; this file is the whole of what the two sides agree on.
Everything the notification says is decided on the backend's side of it.*

Owner: this repository defines the shape · **the client side is implemented and has never been
run.** `app/src/main/java/dev/liamchu/blip/Backend.kt` is the whole of it, and no backend has ever
answered it — so every clause below is still a specification, and the `Verified` column says which
code implements it rather than claiming it works.

What a backend author still has to build is theirs; what this app still has to build is
[`BACKLOG.md`](BACKLOG.md), not this file.

## The call

One `POST`, to the URL the user configured, exactly as they typed it. **The app appends no path and
adds no query parameters** — if the backend wants a path, it belongs in the URL the user enters.

```
POST <the configured URL>
Content-Type: application/json
Authorization: Bearer <the configured credential>     ← omitted entirely when the credential is empty
User-Agent: Blip/<version>

{"max_length": 120}
```

| Clause | Shape | Verified |
| --- | --- | --- |
| method | `POST`, always | implemented in `Backend.kt`; never exercised |
| URL | the configured URL verbatim; no path appended, no query added | implemented in `Backend.kt`; never exercised |
| `Content-Type` | `application/json` | implemented in `Backend.kt`; never exercised |
| `Authorization` | `Bearer <credential>`; the header is **absent**, not empty, when no credential is set | implemented in `Backend.kt`; never exercised |
| body | a JSON object with exactly one member, `max_length`, an integer count of characters | implemented in `Backend.kt`; never exercised |
| redirects | not followed. A redirect is a failed run | implemented in `Backend.kt`; never exercised |
| timeouts | 10 s to connect, 30 s to read | implemented in `Backend.kt`; never exercised |
| retries | none. One attempt per run; the next run is the retry | implemented in `Backend.kt`; never exercised |

## Responses

Three outcomes, and nothing else is defined.

| Status | Body | What the app does | Verified |
| --- | --- | --- | --- |
| `200` | `{"title": "...", "text": "..."}` | posts the notification | implemented in `Backend.kt`; never exercised |
| `204` | none | posts nothing; **this is not a failure** | implemented in `Backend.kt`; never exercised |
| anything else | ignored | failed run | implemented in `Backend.kt`; never exercised |

On `200`, **both members are required and both must be non-empty.** There are no optional fields and
no defaults: a response missing `title`, missing `text`, or carrying an empty string for either is a
failed run, per [`CONSTRAINTS.md`](CONSTRAINTS.md) "C1 — Fail fast; no fallbacks". Unknown extra
members are ignored, so a backend may return more without breaking anything.

| Limit | Value | Verified |
| --- | --- | --- |
| `text` length | at most the `max_length` sent in the request | implemented in `Backend.kt`; never exercised |
| `title` length | at most 32 characters | implemented in `Backend.kt`; never exercised |

**An over-length response is a breach, not something to trim** (owner, 2026-09-10). The app does not
truncate: it sent the limit, the backend agreed to respect it, and quietly cutting the text would
make the app change what the backend said. A run that receives one posts nothing and is a failed run.

`204` exists so that a backend with nothing to say does not have to invent something. A run that ends
this way is silent and leaves the failure state alone — see [`DESIGN.md`](DESIGN.md) "What a failed
run shows".

## Worked example

A backend that always has something to say, in any language, is this much:

```
POST /blip  ->  200
{"title": "Kotlin", "text": "buildList { } beats mutableListOf when you only build the list once."}
```

And one that is rate-limiting itself to office hours returns `204` the rest of the time.

Testable before the app exists:

```bash
curl -sS -X POST "$URL" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $CREDENTIAL" \
  -d '{"max_length": 120}' -i
```

**Breaking a clause is a decision**, recorded and coordinated before it ships — never an edit to this
file on its own.
