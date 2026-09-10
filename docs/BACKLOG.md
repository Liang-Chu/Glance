# Backlog — the only list of open work

> Open items only. Done means deleted; the record is the git history.
> Every item states what blocks it and what done looks like.
> 🟡 is only "waiting on a person to decide".
>
> Last swept: 2026-09-10

| Pri | Item | Blocked on / done when |
| --- | --- | --- |
| 🔴 | Run the app once against a real backend and see the notification arrive, expire, and reach the glasses | Blocked on a phone being connected and a backend answering [`CONTRACT.md`](CONTRACT.md). Everything in [`STATUS.md`](STATUS.md) currently says "built, never run", and `C9` counts only the running thing. Done when the `Verified` column in `CONTRACT.md` names how each clause was exercised and the `STATUS.md` rows carry a real verification |
| 🔴 | Write a minimal backend that satisfies [`CONTRACT.md`](CONTRACT.md) | Blocked on nothing. The contract has never been answered by anything, so it is unproven as a specification. Done when it replies to the `curl` in `CONTRACT.md` with a 200 and with a 204 |
| 🔴 | Establish the verified facts about the G2 notification path: the exact device and companion-app names, and how much text the glasses actually display | Blocked on access to the glasses and the companion app. The character count is what the maximum-length starting value in [`DESIGN.md`](DESIGN.md) is currently guessing at, and what the `title` limit in [`CONTRACT.md`](CONTRACT.md) was picked without |
| 🟠 | Add a fake for `Backend.fetch` and tests for the outcomes it can return | Blocked on nothing. Every branch in `Backend.parse` is currently unexercised, including the over-length rejection the owner chose deliberately. Done when the outcomes are covered and the command that runs them is in [`PROCESS.md`](PROCESS.md) |
| 🟠 | Decide whether the failure notification should also expire | Blocked on the owner deciding. It is currently the one notification that stays until dismissed, which is the opposite of what the owner asked for everywhere else — deliberate, because it is the only sign the app is broken, but unrecorded as a decision |
| ⚪ | Observe whether cancelling a notification on the phone also clears it from the glasses | Blocked on access to the glasses. Not load-bearing — the expiry requirement is satisfied on the phone either way — but it decides whether a stale notification can linger in the wearer's view |
