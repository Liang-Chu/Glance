# Backlog — the only list of open work

> Open items only. Done means deleted; the record is the git history.
> Every item states what blocks it and what done looks like.
> 🟡 is only "waiting on a person to decide".
>
> Last swept: 2026-09-10

| Pri | Item | Blocked on / done when |
| --- | --- | --- |
| 🟡 | Decide how a keyword is chosen each round, and how a tip already shown is kept from coming back | Blocked on the owner deciding. Done when [`DESIGN.md`](DESIGN.md) "Where a tip comes from" states both and carries a criterion that checks the second. A fixed keyword set queried every few hours returns the same top results, so without this the app repeats itself |
| 🟡 | Decide what the length setting counts, its default, and whether the limit is enforced after the LLM answers or only asked of it | Blocked on the owner deciding, and on knowing what the glasses actually display. Done when [`DESIGN.md`](DESIGN.md) states the unit and the enforcement point with a criterion |
| 🟡 | Decide what "expire automatically" means mechanically: who withdraws the notification, and whether the wearer can hold it | Blocked on the owner deciding, and on knowing whether the glasses honour a withdrawal from the phone at all. Done when [`DESIGN.md`](DESIGN.md) states the mechanism with a criterion |
| 🟡 | Decide how a failed run tells the user anything, given `C1` forbids substituting text for a tip | Blocked on the owner deciding. Done when [`DESIGN.md`](DESIGN.md) records what the user sees when a provider is unreachable, and a criterion checks that it is not a tip-shaped notification |
| 🟡 | Decide what happens when the user's own endpoint is reachable on some networks and not others | Blocked on the owner deciding. A self-run LLM on a home network is unreachable from mobile data unless the user has a tunnel. Done when [`DESIGN.md`](DESIGN.md) records whether the app skips, retries, or reports |
| 🟡 | Choose the Android stack: language, minimum SDK, build system | Blocked on the owner deciding. Done when [`DESIGN.md`](DESIGN.md) records the choice, [`ORIENTATION.md`](ORIENTATION.md) names the source directory and the build entry point, and [`PROCESS.md`](PROCESS.md) carries a build procedure that runs as written |
| 🔴 | Establish the verified facts about the G2 notification path: the exact device and companion-app names, how an Android notification reaches the glasses, what the glasses truncate it to, and whether a withdrawal from the phone removes it | Blocked on access to the glasses and the companion app. Done when `docs/reference/` holds a file dated against the firmware and app version observed, each fact marked confirmed, likely, or speculative, and the `REFERENCE` row in [`README.md`](README.md) reads active |
| 🟠 | Decide where the user's credentials are stored on the device, and what guarantees it | Blocked on the Android stack choice. Done when [`DESIGN.md`](DESIGN.md) names the storage and a criterion checks that no credential reaches a log or a backup |
| 🟠 | Decide the full set of user settings and their defaults | Blocked on the rows above, which each add one. Done when [`DESIGN.md`](DESIGN.md) holds the set in one place and nothing else defines a default for any of them |
| 🟠 | Add the file-length gate that `C3` in [`CONSTRAINTS.md`](CONSTRAINTS.md) requires | Blocked on a build existing. Done when a check fails on any source file over 500 lines and the command that runs it is in [`PROCESS.md`](PROCESS.md) |
