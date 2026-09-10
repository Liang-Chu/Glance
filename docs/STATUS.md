# Status — what is true right now

*The present view. [`BACKLOG.md`](BACKLOG.md) is what is not done; this is what is. Overwritten,
never appended — a dated snapshot of one day belongs in a report.*

Stage: **before design** — the product has been stated, not yet specified · last full sweep
2026-09-10

| Component | Covers | State | Verified how, when |
| --- | --- | --- | --- |
| Documentation set | `docs/`, `README.md`, `CLAUDE.md` | **live** | `ls docs/` lists a file for every active slot the map names, and every link in `docs/README.md` resolves, 2026-09-10 |
| Version control | `.git/` | **live** | `git rev-parse --show-toplevel` names this directory, 2026-09-10 |
| docs-and-constraints skill | `.claude/skills/docs-and-constraints/` | **live** | `ls .claude/skills/docs-and-constraints/` lists `SKILL.md` and `reference/`, 2026-09-10 |
| Android application | no path yet | **not started** | no source directory exists, 2026-09-10 |
| Delivery to the G2 glasses | no path yet | **not started** | nothing in the repository posts a notification, 2026-09-10 |

A row is suspect when its path has commits newer than its verification date — then re-verify it or
delete it. No proportions: name which parts are live, which are built but unwired, which do not
exist.
