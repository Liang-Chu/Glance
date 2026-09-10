# Backlog — the only list of open work

> Open items only. Done means deleted; the record is the git history.
> Every item states what blocks it and what done looks like.
> 🟡 is only "waiting on a person to decide".
>
> Last swept: 2026-09-10

| Pri | Item | Blocked on / done when |
| --- | --- | --- |
| 🔴 | Run the app on a phone: see a notification arrive, expire on its own, and reach the glasses | Blocked on a device being connected and a backend answering [`CONTRACT.md`](CONTRACT.md). Everything Android-side in [`STATUS.md`](STATUS.md) still says "built, never run", and `C9` counts only the running thing. Done when those rows carry a real verification |
| 🔴 | Confirm WorkManager actually fires the run on the interval, through Doze | Blocked on the app being installed. This is the whole reason the stack was chosen and it is the one claim in [`DESIGN.md`](DESIGN.md) "The stack" that no test can reach. Done when a run has fired unattended, hours after the app was last opened, and `STATUS.md` records it |
| 🔴 | Write a minimal backend that satisfies [`CONTRACT.md`](CONTRACT.md) | Blocked on nothing. The contract is exercised by a test server that this repository wrote to its own spec, which proves it is implementable but not that it is reasonable to implement. Done when something written independently answers the `curl` in `CONTRACT.md` |
| 🔴 | Establish the verified facts about the G2 notification path: the exact device and companion-app names, and how much text the glasses actually display | Blocked on access to the glasses and the companion app. The character count is what the maximum-length starting value in [`DESIGN.md`](DESIGN.md) is currently guessing at, and what the `title` limit of 32 in [`CONTRACT.md`](CONTRACT.md) was picked without |
| 🟠 | Decide whether the failure notification should also expire | Blocked on the owner deciding. It is the one notification that stays until dismissed, which is the opposite of what the owner asked for everywhere else — deliberate, because it is the only sign the app is broken, but never recorded as a decision |
| 🟠 | Cover the three contract clauses that no test asserts: `Content-Type`, the timeouts, and that there is exactly one attempt per run | Blocked on nothing. [`CONTRACT.md`](CONTRACT.md) names them as unasserted, so this is a known hole rather than a hidden one. Done when each row there names a test |
| ⚪ | Observe whether cancelling a notification on the phone also clears it from the glasses | Blocked on access to the glasses. Not load-bearing — the expiry requirement is satisfied on the phone either way — but it decides whether a stale notification can linger in the wearer's view |
