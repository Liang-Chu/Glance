# Design

> Latest design only. Decisions only — no process, no plans, no change log.

## 0 · Decision index — grep before designing anything

| Question | Decided in |
| --- | --- |
| What platform does this software run on? | **[`DESIGN.md`](DESIGN.md) "Purpose and delivery path"** (owner, 2026-09-10) |
| How does a notification get from the phone to the glasses? | **[`DESIGN.md`](DESIGN.md) "Purpose and delivery path"** (owner, 2026-09-10: "using their notification feature") |
| Do we pair with, scan for, or speak a link protocol to the glasses? | **[`DESIGN.md`](DESIGN.md) "Purpose and delivery path"** (owner, 2026-09-10) |
| Does a notification stay on screen until someone dismisses it? | **[`DESIGN.md`](DESIGN.md) "Purpose and delivery path"** (owner, 2026-09-10: "expire automatically") |
| Where does the text of a tip come from? | **[`DESIGN.md`](DESIGN.md) "Where a tip comes from"** (owner, 2026-09-10) |
| Does this project run a server, or hold any API key or account of its own? | **[`DESIGN.md`](DESIGN.md) "Where a tip comes from"** (owner, 2026-09-10: "i dont want to host a server for this one") |
| How does the app reach an LLM the user runs themselves, versus a hosted LLM API? | **[`DESIGN.md`](DESIGN.md) "Where a tip comes from"** (owner, 2026-09-10) |
| Is the search engine or the LLM vendor fixed by this project? | **[`DESIGN.md`](DESIGN.md) "Where a tip comes from"** (owner, 2026-09-10: "something like brave") |
| What happens when a provider is missing or unreachable? | **[`DESIGN.md`](DESIGN.md) "Where a tip comes from"**, applying [`CONSTRAINTS.md`](CONSTRAINTS.md) "C1 — Fail fast; no fallbacks" |
| How often does a tip appear? | **[`DESIGN.md`](DESIGN.md) "Where a tip comes from"** (owner, 2026-09-10) |
| Why does a notification expire, and what cancels it? | **[`DESIGN.md`](DESIGN.md) "Purpose and delivery path"** (owner, 2026-09-10: "i dont want the user have to deal with the notification on the phone again") |
| How is the same tip kept from appearing again? | **[`DESIGN.md`](DESIGN.md) "Where a tip comes from"** (owner, 2026-09-10) |
| Which keyword does a run use? | **[`DESIGN.md`](DESIGN.md) "Where a tip comes from"** (owner, 2026-09-10) |
| What does the user see when a run fails? | **[`DESIGN.md`](DESIGN.md) "What a failed run shows"** (owner, 2026-09-10) |

Grep the whole file before concluding a question is undecided.

## 1 · Purpose and delivery path

> **Prerequisites**: none.
> **Decides**: what this software is, and the route its notifications take to the glasses.

Recorded from the owner, 2026-09-10, verbatim: *"Eventually this is a android app that send some
concise notification which expire automatically."* and *"My goal is to use that to push notification
to Even G2 automatically using their notification feature."*

This is an **Android application**. What it produces is Android notifications, and each one **stops
being displayed after a bounded lifetime without anybody dismissing it**.

Those notifications reach the **Even G2** glasses through **the G2's own notification feature** — the
path that already carries phone notifications to the glasses. This project therefore builds **no
transport of its own to the glasses**: it does not pair with them, scan for them, or speak a link
protocol to them. Seen from this repository, the glasses are one more consumer of ordinary Android
notifications, and the app's job ends when Android accepts the notification.

**Why it expires**, recorded from the owner, 2026-09-10, verbatim: *"the reason i want to cancel it
is cause i dont want the user have to deal with the notification on the phone again"*. The expiry
exists to keep the phone's notification shade clear, not to control the glasses. What the glasses do
with a withdrawal is therefore interesting but not load-bearing: this requirement is satisfied on the
phone whatever they do.

The lifetime is carried **on the notification itself**, so Android clears it whether or not this app
is running. Nothing schedules a cancellation, and no component has to survive to do the clearing.

How long the lifetime is remains the user's setting, and what the glasses show is tracked in
[`BACKLOG.md`](BACKLOG.md).

### Acceptance

- `purpose-1` The delivered artifact is an Android application package.
- `purpose-2` Every notification the app posts leaves the notification shade with no user action.
- `purpose-3` No source file in this repository connects to the glasses: nothing in it pairs, scans,
  or speaks a link protocol to them.
- `purpose-4` A posted notification clears itself with the app force-stopped and no work scheduled;
  the lifetime rides on the notification, not on something that must still be alive to fire.

## 2 · Where a tip comes from

> **Prerequisites**: none.
> **Decides**: what produces the text of a notification, which outside services are involved, and
> who supplies the credentials for them.

Recorded from the owner, 2026-09-10, verbatim: *"the first thing i want it to do is to setup some
keywords of the field and lookup some tip from that field using something like brave and make it
concise enough asa notification(customize length) and show a notification. the notification should be
expire in a time the user set."*, *"i want to make it configable"*, *"the user can either choose to
stream info from their server, or setup the api key which only stay on their own device to do the
summary"*, and *"i dont want to host a server for this one"*. The owner chose a fixed interval —
a tip every N hours — over firing at set times of day or on a manual tap.

**The pipeline.** A timer fires on an interval the user sets. The app takes a keyword from a set the
user configured, asks a **web search provider** for material in that field, has an **LLM** condense
what comes back to a length the user set, and posts the result as an Android notification that stops
displaying after a time the user set.

**No service in that pipeline belongs to this project.** This project runs no server, holds no API
key, and pays for no account. Every outside service is reached with configuration the user supplies
and which stays on the user's own device. That applies to the LLM two ways — an endpoint the user
runs themselves, or a hosted LLM API the user has a key for — and both are the same shape, **a base
URL plus a credential**, so the app carries **one provider mechanism rather than a branch per
vendor**, per [`CONSTRAINTS.md`](CONSTRAINTS.md) "C4 — Solve it structurally, not by stacking cases".
The search provider is configured the same way: the owner said *"something like brave"*, so Brave is
an example rather than a fixed dependency, and no vendor is named in the code.

**When a provider is missing or unreachable, no tip is posted and no substitute text is invented.**
Truncating a search snippet because the LLM did not answer would be exactly the defaulting that
[`CONSTRAINTS.md`](CONSTRAINTS.md) "C1 — Fail fast; no fallbacks" forbids: the notification would
claim to be a tip while carrying something else.

**Which keyword, and no repeats.** Keywords are used **in rotation**, so one keyword cannot take
consecutive runs while others go unused. The app **remembers the results it has already shown** and
skips them, which is what stops a fixed keyword set from returning the same top results every few
hours. That memory is a local store on the device: it is what the app knows about itself, it is sent
nowhere, and it is the one piece of state a run carries between invocations.

What is still open about this — when that memory forgets a result, and what the length setting
counts — is tracked in [`BACKLOG.md`](BACKLOG.md).

### Acceptance

- `tip-source-1` The repository contains no API key, no endpoint this project operates, and no
  account this project pays for. A fresh install talks to nothing until the user configures it.
- `tip-source-2` Every outside service the app calls is reached at a base URL that came from user
  configuration; changing that configuration changes which endpoint is called, with no code change.
- `tip-source-3` An endpoint the user runs and a hosted vendor API are served by one code path. No
  vendor name appears in a conditional.
- `tip-source-4` When a configured provider is absent or unreachable, the app posts no notification
  and writes no substitute text.
- `tip-source-5` The interval between tips is a user setting counted in hours, and the app schedules
  no repeating work more often than that setting.
- `tip-source-6` The length of a notification and the time it stops displaying are both user
  settings, and neither is a constant in the source.
- `tip-source-7` A result already shown is not shown again while it is remembered, and the store of
  what was shown never leaves the device.
- `tip-source-8` Keywords advance in rotation: with more than one configured, no keyword is used
  twice before every other has been used once.

## 3 · What a failed run shows

> **Prerequisites**: [`DESIGN.md`](DESIGN.md) "Where a tip comes from".
> **Decides**: what the user sees when a provider cannot be reached.

A provider this project does not run can be unreachable for ordinary reasons — an LLM on the user's
home network is simply absent when the phone is on mobile data. That is the expected case, not an
exceptional one.

The app **tells the user once**. On the first failed run it posts a notification saying the run
failed; it stays quiet through every further failure, and only becomes able to speak again after a
run has succeeded. Silence would let the app stay broken for days unnoticed; a notification per
attempt would nag all day on mobile data.

That notification is **not a tip and does not look like one**. It carries no text from a search
result, because [`CONSTRAINTS.md`](CONSTRAINTS.md) "C1 — Fail fast; no fallbacks" is what makes the
run fail in the first place — dressing the failure in retrieved text would smuggle back exactly the
substitute the constraint exists to prevent.

### Acceptance

- `failure-1` Consecutive failed runs produce one notification between them, not one each; the app
  regains its voice only after a run succeeds.
- `failure-2` A failure notification carries no text originating from a search result or a provider
  response.
- `failure-3` A failed run posts no tip, and the store of shown results is unchanged by it.
