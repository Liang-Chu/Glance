# Backlog — the only list of open work

> Open items only. Done means deleted; the record is the git history.
> Every item states what blocks it and what done looks like.
> 🟡 is only "waiting on a person to decide".
>
> Last swept: 2026-09-10

| Pri | Item | Blocked on / done when |
| --- | --- | --- |
| 🟡 | Decide what makes the app post a notification, and what the notification says | Blocked on the owner deciding. Done when [`DESIGN.md`](DESIGN.md) has a section for it with acceptance criteria and a decision-index row. Until this lands, nothing below it can be built |
| 🟡 | Decide what "expire automatically" means mechanically: who withdraws the notification, after how long, and whether the wearer can hold it | Blocked on the owner deciding, and on knowing whether the glasses honour a withdrawal from the phone at all. Done when [`DESIGN.md`](DESIGN.md) states the mechanism and the timeout with acceptance criteria |
| 🟡 | Choose the Android stack: language, minimum SDK, build system | Blocked on the owner deciding. Done when [`DESIGN.md`](DESIGN.md) records the choice, [`ORIENTATION.md`](ORIENTATION.md) names the source directory and the build entry point, and [`PROCESS.md`](PROCESS.md) carries a build procedure that runs as written |
| 🔴 | Establish the verified facts about the G2 notification path: the exact device and companion-app names, how an Android notification reaches the glasses, what the glasses truncate it to, and whether a withdrawal from the phone removes it | Blocked on access to the glasses and the companion app. Done when `docs/reference/` holds a file dated against the firmware and app version observed, each fact marked confirmed, likely, or speculative, and the `REFERENCE` row in [`README.md`](README.md) reads active |
| 🔴 | Define "concise" as a number, and name what that number is a limit on | Blocked on the measurement above. Done when the limit is a single value in [`DESIGN.md`](DESIGN.md) with a criterion that checks it, and the raw observation lives under `docs/measurements/` |
| 🟠 | Add the file-length gate that `C3` in [`CONSTRAINTS.md`](CONSTRAINTS.md) requires | Blocked on a build existing. Done when a check fails on any source file over 500 lines and the command that runs it is in [`PROCESS.md`](PROCESS.md) |
| ⚪ | Decide whether the app needs a settings surface, and what it holds | Blocked on the trigger decision above. Done when [`DESIGN.md`](DESIGN.md) either records what it holds or records that there is none |
