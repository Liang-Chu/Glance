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
| What does the user see when a run fails? | **[`DESIGN.md`](DESIGN.md) "What a failed run shows"** (owner, 2026-09-10) |
| What is the app written in, and what runs the interval? | **[`DESIGN.md`](DESIGN.md) "The stack"** |

Grep the whole file before concluding a question is undecided.

## 1 · Purpose and delivery path

> **Prerequisites**: none.
> **Decides**: what this software is, and the route its notifications take to the glasses.

Recorded from the owner, 2026-09-10, verbatim: *"Eventually this is a android app that send some
concise notification which expire automatically."* and *"My goal is to use that to push notification
to Even G2 automatically using their notification feature."*

This is an **Android application**. What it produces is Android notifications, and each one **stops
being displayed after a bounded lifetime without anybody dismissing it**. Where their text comes from
is [`DESIGN.md`](DESIGN.md) "Where a notification's content comes from".

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

**The app runs without being opened.** The owner, 2026-09-10: *"It should able to run in the
background silencly."* Once the settings are filled in, the app has no reason to be launched again,
and nothing it does requires a screen.

### Acceptance

- `purpose-1` The delivered artifact is an Android application package.
- `purpose-2` Every notification the app posts leaves the notification shade with no user action.
- `purpose-3` No source file in this repository connects to the glasses: nothing in it pairs, scans,
  or speaks a link protocol to them.
- `purpose-4` A posted notification clears itself with the app force-stopped and no work scheduled;
  the lifetime rides on the notification, not on something that must still be alive to fire.
- `purpose-5` Notifications keep appearing across a reboot with the app never opened again.

## 2 · Where a notification's content comes from

> **Prerequisites**: none.
> **Decides**: what produces the text of a notification, and what this app is responsible for.

Recorded from the owner, 2026-09-10, verbatim: *"it eventually just something accept some backend
input and make them a notification that will expire"*, *"i expect the user build the backend
themselves"*, and *"i dont want to host a server for this one"*.

**The app is a client and nothing else.** On an interval the user sets, it calls **one backend the
user runs**, at a URL and with a credential the user supplied, and turns the response into a
notification that expires. It does not decide what the notification says. The exact shape of that
call is [`CONTRACT.md`](CONTRACT.md).

~~The app chose a keyword, asked an LLM for something in that field, and remembered what it had
already posted so as not to repeat itself.~~ → **all content generation is ruled out of the app**
(owner, 2026-09-10: *"You can define how the api gonna looks like since i expect the user build the
backend themselves"*). Topic selection, prompts, model choice, and repeat suppression are the
backend's concern and are absent here. **The app keeps no state beyond its settings** — a run reads
settings, makes one call, and posts or does not post.

**No service this app calls belongs to this project.** It runs no server, holds no API key, and pays
for no account. A fresh install talks to nothing until the user gives it a URL.

### Acceptance

- `content-1` The repository contains no API key, no endpoint this project operates, and no account
  this project pays for. A fresh install with no URL configured makes no network call.
- `content-2` The only host the app contacts is the one in the user's configured URL.
- `content-3` The source contains no prompt, no model name, no topic list, and no vendor name.
- `content-4` The app stores nothing between runs except its settings: no history of what it posted,
  no cache of responses.
- `content-5` When the backend cannot be reached or breaks [`CONTRACT.md`](CONTRACT.md), the app
  posts no notification and writes no substitute text.

Retired: tip-source-1 … tip-source-11 (the app stopped generating content, 2026-09-10).

## 3 · What the user can change

> **Prerequisites**: [`DESIGN.md`](DESIGN.md) "Where a notification's content comes from".
> **Decides**: the complete set of settings, and that there is nothing else to configure.

Recorded from the owner, 2026-09-10, verbatim: *"the backend credential and url, expiration and check
frequency and notification max length should be editable and thats it"*.

Five settings, and **that list is closed** — anything a later version wants to vary is a decision
recorded here, not a field quietly added to a screen.

| Setting | Unit | Starting value |
| --- | --- | --- |
| Backend URL | absolute `http` or `https` URL | empty — the app does nothing until it is set |
| Credential | opaque string, may be left empty | empty |
| Check frequency | hours | 4 |
| Expiration | minutes | 10 |
| Maximum length | characters | 120 |

**The credential stays on the device.** It lives in the app's private storage like every other
setting, the app opts out of Android backup so it is not carried off to a cloud account, and nothing
writes it to a log. There is no separate secret store: on a device that is not rooted, app-private
storage is the boundary, and pretending otherwise would be ceremony rather than protection.

The starting values are what a fresh install holds before the user touches anything, and each is
written in one place. The maximum-length starting value is a **guess pending measurement** — what the
glasses actually display is tracked in [`BACKLOG.md`](BACKLOG.md).

### Acceptance

- `settings-1` The settings screen offers exactly these five fields and no others.
- `settings-2` Each starting value appears in exactly one place in the source.
- `settings-3` A URL that is not an absolute `http` or `https` URL is refused when it is entered, not
  at the moment a run tries to use it.
- `settings-4` Changing the check frequency changes the interval of the next scheduled run without
  the app being reinstalled.
- `settings-5` The credential is held in the app's private storage, and the app opts out of Android
  backup so it cannot leave the device that way.
- `settings-6` No credential, and no part of one, is ever written to a log or into the text of a
  notification.

## 4 · What a failed run shows

> **Prerequisites**: [`DESIGN.md`](DESIGN.md) "Where a notification's content comes from".
> **Decides**: what the user sees when the backend cannot be reached or breaks the contract.

A backend this project does not run can be unreachable for ordinary reasons — one on the user's home
network is simply absent when the phone is on mobile data. That is the expected case, not an
exceptional one.

The app **tells the user once**. On the first failed run it posts a notification saying the run failed
and why; it stays quiet through every further failure, and becomes able to speak again only after a
run has succeeded. Silence would let the app stay broken for days unnoticed; a notification per
attempt would nag all day on mobile data.

That notification is **not content and does not look like it**. It carries no text from the backend's
response body, because [`CONSTRAINTS.md`](CONSTRAINTS.md) "C1 — Fail fast; no fallbacks" is what makes
the run fail in the first place — dressing the failure in the response would smuggle back exactly the
substitute the constraint exists to prevent.

**A backend with nothing to say is not a failure.** [`CONTRACT.md`](CONTRACT.md) gives it a way to say
so, and a run that ends that way posts nothing, stays silent, and leaves the failure state untouched.

### Acceptance

- `failure-1` Consecutive failed runs produce one notification between them, not one each; the app
  regains its voice only after a run succeeds.
- `failure-2` A failure notification carries no text originating from the backend's response body.
- `failure-3` A run in which the backend reports it has nothing to say posts nothing, and neither
  raises nor clears the failure state.

## 5 · The stack

> **Prerequisites**: [`DESIGN.md`](DESIGN.md) "Purpose and delivery path".
> **Decides**: what the app is written in, and what makes the interval survive Android.

**Kotlin, Jetpack Compose for the settings screen, WorkManager for the interval, DataStore for the
settings.** Minimum SDK **26**, because the notification carries its own lifetime and that arrived
there; compile and target **37**, which is the floor the current androidx libraries impose rather
than a level anything here asks for. Kotlin is not a separate Gradle plugin: the Android plugin
carries it from version 9, and adding the old one is an error.

**WorkManager is the load-bearing choice.** A repeating background job survives Doze only when the
operating system is the thing scheduling it, and WorkManager is the API that exists for that. A
hand-rolled alarm loop, a long-lived foreground service, or a shell cron all fight power management
and lose, which is why the automation tools that do this today are unreliable at it. Its floor is
fifteen minutes; the frequency setting is counted in hours, so the floor is never in reach.

There is **no database**. The app keeps nothing between runs but its settings, so DataStore is the
whole of its storage.

### Acceptance

- `stack-1` The app schedules its repeating work through WorkManager, and no alarm loop or long-lived
  service exists to do it instead.
- `stack-2` The app declares no database and no persistent store other than the settings.
- `stack-3` A release build installs and runs on a device at minimum SDK 26.
