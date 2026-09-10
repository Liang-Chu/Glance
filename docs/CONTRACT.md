# Contract — the app and the user's backend

*The app calls one backend the user wrote; this file is the whole of what the two sides agree on.
Everything the notification says is decided on the backend's side of it.*

Owner: this repository defines the shape · **the client side is implemented and exercised against a
real HTTP server**, on 2026-09-10, by `app/src/test/java/dev/liamchu/blip/BackendContractTest.kt`.
`Backend.kt` is the whole of the client. No backend written by a person has answered it yet, and no
run has happened on a phone — the `Verified` column names the test per clause, and says plainly where
there is none.

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
| method | `POST`, always | test "posts the max length to the configured url" |
| URL | the configured URL verbatim; no path appended, no query added | same test — the server answers one path only, so an appended path would miss it |
| `Content-Type` | `application/json` | **implemented, not asserted by any test** |
| `Authorization` | `Bearer <credential>`; the header is **absent**, not empty, when no credential is set | tests "sends the credential as a bearer token" and "omits the authorization header entirely when no credential is set" |
| body | a JSON object with exactly one member, `max_length`, an integer count of characters | test "posts the max length to the configured url" |
| redirects | not followed. A redirect is a failed run | test "a redirect is not followed" |
| timeouts | 10 s to connect, 30 s to read | **implemented, not asserted by any test** |
| retries | none. One attempt per run; the next run is the retry | **implemented, not asserted by any test** |
| unreachable | a failed run, named as such | test "an unreachable backend is a failure" |

## Responses

Three outcomes, and nothing else is defined.

| Status | Body | What the app does | Verified |
| --- | --- | --- | --- |
| `200` | `{"title": "...", "text": "..."}` | posts the notification | test "two hundred with title and text is content" |
| `204` | none | posts nothing; **this is not a failure** | test "two hundred and four is not a failure" |
| anything else | ignored | failed run | test "a server error is a failure carrying the status" |

On `200`, **both members are required and both must be non-empty.** There are no optional fields and
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
