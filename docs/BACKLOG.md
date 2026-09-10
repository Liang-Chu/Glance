# Backlog — the only list of open work

> Open items only. Done means deleted; the record is the git history.
> Every item states what blocks it and what done looks like.
> 🟡 is only "waiting on a person to decide".
>
> Last swept: 2026-09-10

| Pri | Item | Blocked on / done when |
| --- | --- | --- |
| 🔴 | Build the app: settings, the scheduled call, the expiring notification, the failure notice | Blocked on nothing. Done when every criterion in [`DESIGN.md`](DESIGN.md) holds and the `Verified` column in [`CONTRACT.md`](CONTRACT.md) names how each clause was exercised |
| 🔴 | Verify a notification this app posts actually reaches the glasses and is readable there | Blocked on the app existing and on access to the glasses. `C9` says the running thing is what counts, and here that is the display — not a notification Android accepted. Done when it has been seen on the glasses and [`STATUS.md`](STATUS.md) records how and when |
| 🔴 | Establish the verified facts about the G2 notification path: the exact device and companion-app names, and how much text the glasses actually display | Blocked on access to the glasses and the companion app. The character count is what the maximum-length starting value in [`DESIGN.md`](DESIGN.md) is currently guessing at. Done when `docs/reference/` holds a file dated against the firmware and app version observed, each fact marked confirmed, likely, or speculative, and the `REFERENCE` row in [`README.md`](README.md) reads active |
| 🟠 | Add the file-length gate that `C3` in [`CONSTRAINTS.md`](CONSTRAINTS.md) requires | Blocked on the build existing. Done when a check fails on any source file over 500 lines and the command that runs it is in [`PROCESS.md`](PROCESS.md) |
| ⚪ | Write a minimal example backend that satisfies [`CONTRACT.md`](CONTRACT.md) | Blocked on nothing. Not part of the app, but the contract has never been exercised and a twenty-line example would prove it is implementable. Done when the example answers the `curl` in `CONTRACT.md` and the `Verified` column can cite it |
| ⚪ | Observe whether cancelling a notification on the phone also clears it from the glasses | Blocked on access to the glasses. Not load-bearing — the expiry requirement is satisfied on the phone either way — but it decides whether a stale notification can linger in the wearer's view. Done when the observation is a marked fact in `docs/reference/` |
