# Backlog — the only list of open work

> Open items only. Done means deleted; the record is the git history.
> Every item states what blocks it and what done looks like.
> 🟡 is only "waiting on a person to decide".
>
> Last swept: 2026-09-10

| Pri | Item | Blocked on / done when |
| --- | --- | --- |
| 🔴 | Confirm a notification reaches the glasses and is readable there | Blocked on the glasses. One has been seen on the phone; which screen the owner was looking at is not recorded, and `C9` counts the glasses. Done when [`STATUS.md`](STATUS.md) carries it as observed, with what the glasses actually showed |
| 🔴 | Prove WorkManager wakes the app unattended, hours after it was last opened | Blocked on leaving a configured phone alone. Every run so far came from a button, so the load-bearing claim of [`DESIGN.md`](DESIGN.md) "The stack" is still only reasoning. No test can reach it. Done when a run has fired with nobody touching the phone and [`STATUS.md`](STATUS.md) records it |
| 🔴 | Establish how much text the glasses actually display | Blocked on the glasses. The maximum-length starting value of 120 and the `title_max_length` of 32 in [`CONTRACT.md`](CONTRACT.md) were both picked without this. Done when `docs/reference/` holds a file dated against the firmware and app version observed, each fact marked confirmed, likely, or speculative, and the `REFERENCE` row in [`README.md`](README.md) reads active |
| 🟠 | Watch a notification clear itself | Blocked on nothing but a spare three seconds. The expiry is the app's whole reason for existing and nobody has yet seen one work. Done when [`STATUS.md`](STATUS.md) records it observed at a known setting |
| 🟠 | Decide whether the failure notification should also expire | Blocked on the owner deciding. It is the one notification that stays until dismissed, which is the opposite of what the owner asked for everywhere else — deliberate, because it is the only sign the app is broken, but never recorded as a decision |
| 🟠 | Cover the three contract clauses that no test asserts: `Content-Type`, the timeouts, and that there is exactly one attempt per run | Blocked on nothing. [`CONTRACT.md`](CONTRACT.md) names them as unasserted, so this is a known hole rather than a hidden one. Done when each row there names a test |
