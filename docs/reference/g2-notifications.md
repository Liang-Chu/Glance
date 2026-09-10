# Even G2 notifications — reference

*What the glasses do with an ordinary Android notification. Read before assuming anything about what
they will show.*

Observed 2026-09-10. **The firmware and companion-app versions were not recorded**, which weakens
every claim below by exactly that much — a later observation that disagrees should be trusted over
this file, and should record the versions. Confidence is per claim, not per document.

## Source ranking

| Rank | Source | Kind | Trust |
| --- | --- | --- | --- |
| 1 | the owner's own G2, watched while Glance posted | measured | **Highest** — the running thing, seen |
| 2 | the Even Realities support pages | published | Middling — read via search summaries, not fetched; the support site returned 403 |

## The path

- **An ordinary Android notification from a third-party app reaches the glasses.** `confirmed` —
  Glance is an unremarkable Android app using `NotificationCompat`, and its notification was read on
  the G2 on 2026-09-10. Nothing about the app is special to the glasses: it does not pair with them,
  scan for them, or speak any protocol to them.
- **The app must be allowed in the Even Realities companion app, under Notifications.** `likely` —
  this is how the feature is described, and the notification did arrive once the app was allowed. It
  has not been tested in the negative, so "allowed" being the *cause* is not established.

## What was actually displayed

- **A 6-character title and an 86-character body were readable.** `confirmed` — the text shown was
  character-for-character what the backend returned, with nothing truncated or reworded.
- **The most text the glasses will display is unknown.** `speculative` — 86 characters is a floor,
  not a limit. The `120` starting value for maximum length and the `32` for `title_max_length` in
  [`CONTRACT.md`](../CONTRACT.md) were both picked without this measurement. Finding the real ceiling
  is tracked in [`BACKLOG.md`](../BACKLOG.md).

## Withdrawal

- **A notification withdrawn on the phone clears from the phone.** `confirmed` — an expiry of 63
  seconds was watched, and it went on its own.
- **Whether that withdrawal also clears it from the glasses is unknown.** `speculative` — nobody
  watched the glasses at the moment the phone cleared it. This decides whether a stale notification
  can linger in the wearer's view, and it is why
  [`DESIGN.md`](../DESIGN.md) "What the user can change" floors the expiry at three seconds rather
  than allowing zero.
