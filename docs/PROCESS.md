# Process

> How this project is built, run, deployed, and reviewed **today**. Current state only.
> Superseded procedures move to `docs/retired/process/`; this file carries no history.

There is no build and no run procedure yet, because there is no source. What exists is
[`STATUS.md`](STATUS.md); the work that would add a build is in [`BACKLOG.md`](BACKLOG.md).

## 1 · Land a change

**When**: every commit.
**Preconditions**: the change is understood well enough to say which document owns it.

1. Make the change.
2. Update the documents that own what changed, in this same commit — behaviour changed means the
   owning [`DESIGN.md`](DESIGN.md) section **and its acceptance criteria**, plus the
   [`STATUS.md`](STATUS.md) row for that component. A decision means the `DESIGN.md` section plus a
   row in its decision index.
3. Delete the [`BACKLOG.md`](BACKLOG.md) row if the change closed one. Add a row if it opened one.
4. `git add -A`
5. `git commit` — the message says what changed and why, not which files moved.

**Verify**: `git show --stat HEAD` lists the documents alongside the change, not a later commit.
**On failure**: fix the commit before moving on; a doc update deferred to "later" is the failure this
step exists to prevent.

## 2 · Record a decision

**When**: something is settled that a future reader would otherwise re-litigate.
**Preconditions**: it is genuinely a decision. If unsure, it is not one — leave it in
[`BACKLOG.md`](BACKLOG.md) and do not write it as decided.

1. Quote what was decided verbatim, with who said it and the date.
2. Write it into the [`DESIGN.md`](DESIGN.md) section that owns the subject — never a new document.
3. Add a decision-index row at the top of `DESIGN.md`, phrased in words someone would search for.
4. Strike through what it supersedes, in place. If that section already carried a strike, delete the
   older one in this same commit.
5. Remove the replaced concept from the code, config, tests, and comments in this same commit.

**Verify**: `grep -n "<a word from the new decision>" docs/DESIGN.md` returns both the index row and
the section.
**On failure**: a decision in the section with no index row is an incomplete commit — add the row.

## 3 · Audit the documentation

**When**: before a release, a rebuild, or a migration; after a rename or a large merge; whenever a
document turned out to have told someone something untrue. Never on a calendar.
**Preconditions**: run from the repository root, on a clean working tree.

1. Read `.claude/skills/docs-and-constraints/reference/cleanup-audit.md`.
2. Run its seven scans with `DOCS=docs`, `RET=docs/retired`, `ROOT_README=README.md`,
   `AGENT_ENTRY=CLAUDE.md`, and `SRC` set to the source root named in
   [`ORIENTATION.md`](ORIENTATION.md).
3. Fix every finding in one commit, whose message says why each document was changed or retired.

**Verify**: re-run the scans; each returns nothing, or a stated reason why a scan had no input.
**On failure**: a scan reporting a missing input is not a pass — it produced no verdict. Say so.

## 4 · Activate a reserved slot

**When**: the trigger written in the slot's row in [`README.md`](README.md) fires.
**Preconditions**: the slot has one real item to hold. Not before.

1. Copy that slot's skeleton from `.claude/skills/docs-and-constraints/reference/templates.md`. Do
   not invent a format.
2. Write the file at the path the map gives it.
3. Change that row's state in [`README.md`](README.md) from `reserved` to `active`, and turn the
   plain path into a link, in this same commit.

**Verify**: the new file is reachable by following links from [`README.md`](README.md).
**On failure**: a file that no link reaches is either not linked or not live — fix one of the two.
