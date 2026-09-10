# Backlog — the only list of open work

> Open items only. Done means deleted; the record is the git history.
> Every item states what blocks it and what done looks like.
> 🟡 is only "waiting on a person to decide".
>
> Last swept: 2026-09-10

| Pri | Item | Blocked on / done when |
| --- | --- | --- |
| 🟡 | Decide when the store of already-shown results forgets one | Blocked on the owner deciding. Never forgetting means the store grows without bound and a keyword eventually runs dry; forgetting too soon brings repeats back. Done when [`DESIGN.md`](DESIGN.md) "Where a tip comes from" states the rule and a criterion checks it |
| 🟡 | Decide what the length setting counts, its default, and whether the limit is enforced after the LLM answers or only asked of it | Blocked on the owner deciding, and on knowing what the glasses actually display. Done when [`DESIGN.md`](DESIGN.md) states the unit and the enforcement point with a criterion |
| 🟡 | Choose the Android stack: language, minimum SDK, build system | Blocked on the owner deciding. Done when [`DESIGN.md`](DESIGN.md) records the choice, [`ORIENTATION.md`](ORIENTATION.md) names the source directory and the build entry point, and [`PROCESS.md`](PROCESS.md) carries a build procedure that runs as written |
| 🔴 | Establish the verified facts about the G2 notification path: the exact device and companion-app names, how an Android notification reaches the glasses, and what the glasses truncate it to | Blocked on access to the glasses and the companion app. The truncation length is what the length setting has to respect. Done when `docs/reference/` holds a file dated against the firmware and app version observed, each fact marked confirmed, likely, or speculative, and the `REFERENCE` row in [`README.md`](README.md) reads active |
| 🟠 | Decide where the user's credentials are stored on the device, and what guarantees it | Blocked on the Android stack choice. Done when [`DESIGN.md`](DESIGN.md) names the storage and a criterion checks that no credential reaches a log or a backup |
| 🟠 | Decide the full set of user settings and their defaults | Blocked on the rows above, which each add one. Done when [`DESIGN.md`](DESIGN.md) holds the set in one place and nothing else defines a default for any of them |
| 🟠 | Add the file-length gate that `C3` in [`CONSTRAINTS.md`](CONSTRAINTS.md) requires | Blocked on a build existing. Done when a check fails on any source file over 500 lines and the command that runs it is in [`PROCESS.md`](PROCESS.md) |
| ⚪ | Observe whether cancelling a notification on the phone also clears it from the glasses | Blocked on access to the glasses. Not load-bearing — the expiry requirement is satisfied on the phone either way — but it decides whether a stale tip can linger in the wearer's view. Done when the observation is a marked fact in `docs/reference/` |
