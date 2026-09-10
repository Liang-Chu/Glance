# Backlog — the only list of open work

> Open items only. Done means deleted; the record is the git history.
> Every item states what blocks it and what done looks like.
> 🟡 is only "waiting on a person to decide".
>
> Last swept: 2026-09-10

| Pri | Item | Blocked on / done when |
| --- | --- | --- |
| 🔴 | Prove WorkManager wakes the app unattended, hours after it was last opened | Blocked on leaving a configured phone alone. Every run so far came from a button, so the load-bearing claim of [`DESIGN.md`](DESIGN.md) "The stack" is still only reasoning, and no test can reach it. The test backend now logs the gap since the app last called, so a run arriving roughly one interval later is the proof. Done when [`STATUS.md`](STATUS.md) records it observed |
| 🔴 | Run two watchers side by side on the phone and confirm they do not collide | Blocked on nothing but adding a second watcher. `watchers-1` through `watchers-5` in [`DESIGN.md`](DESIGN.md) "Many watchers, each named" are all reasoning so far: separate notification slots, independent failure, deletion cancelling only its own work. Done when two have posted at once and [`STATUS.md`](STATUS.md) records it |
| 🟡 | Decide whether the notification title should be the watcher's name or the backend's title | Blocked on the owner deciding, and on seeing whether the glasses show the sub-text. The header line on the glasses is the app label and cannot vary per notification; the line under it is ours and currently carries the backend's title. Done when [`DESIGN.md`](DESIGN.md) records which wins |
| 🟠 | Measure how much text the glasses actually display | Blocked on someone lengthening the text until it stops being readable. 86 characters is a floor, not a limit, and the `120` maximum-length starting value and `32` `title_max_length` in [`CONTRACT.md`](CONTRACT.md) were both picked without it. Done when [`reference/g2-notifications.md`](reference/g2-notifications.md) states the ceiling as `confirmed` |
| 🟠 | Record the G2 firmware and companion-app versions against the observations already made | Blocked on reading them off the device. Every claim in [`reference/g2-notifications.md`](reference/g2-notifications.md) is weaker for their absence, since nothing can be re-checked against a known baseline. Done when that file names both |
| 🟠 | Observe whether a notification cleared on the phone also clears from the glasses | Blocked on watching the glasses at the moment the expiry fires. It decides whether a stale notification can linger in the wearer's view. Done when [`reference/g2-notifications.md`](reference/g2-notifications.md) marks it `confirmed` either way |
| 🟠 | Decide whether the failure notification should also expire | Blocked on the owner deciding. It is the one notification that stays until dismissed, which is the opposite of what the owner asked for everywhere else — deliberate, because it is the only sign the app is broken, but never recorded as a decision |
| 🟠 | Cover the three contract clauses that no test asserts: `Content-Type`, the timeouts, and that there is exactly one attempt per run | Blocked on nothing. [`CONTRACT.md`](CONTRACT.md) names them as unasserted, so this is a known hole rather than a hidden one. Done when each row there names a test |
