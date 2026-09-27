# Agent entry

**Read [`docs/README.md`](docs/README.md) before touching anything in this repository.** It is the
documentation index, with the authority order and lifecycle rule. This file routes; it owns nothing.
Every fact lives in exactly one document, and a
fact restated here is a defect.

## Obligations that hold without being asked

- **Grep the decision index at the top of [`docs/DESIGN.md`](docs/DESIGN.md) before designing
  anything.** Absent from the index does not mean undecided — grep the whole file.
- **Documentation moves in the same commit as the code.** Never ask whether the docs need updating.
  A behaviour change that does not touch the owning [`docs/DESIGN.md`](docs/DESIGN.md) section, its
  acceptance criteria, and the matching [`docs/STATUS.md`](docs/STATUS.md) row is incomplete.
- **Open work goes in [`docs/BACKLOG.md`](docs/BACKLOG.md) and nowhere else.** No other file keeps a
  list of open items. Done means the row is deleted; the record is the git history.
- **[`docs/CONSTRAINTS.md`](docs/CONSTRAINTS.md) holds gates, not advice.** Breaking one appends a
  dated row beneath it in the same commit and never edits the rule's own wording.
- **Nothing that must not reach GitHub is committed.** It goes in `local/`, which is ignored whole;
  read `git status --short` before every commit and stage by path —
  [`docs/PROCESS.md`](docs/PROCESS.md) "Put down a file that must not reach GitHub".
- **Before creating, renaming, splitting, moving, or retiring any document**, read the lifecycle
  rule in [`docs/README.md`](docs/README.md) and the procedure in
  [`docs/PROCESS.md`](docs/PROCESS.md). Keep the documentation index current; a rename is finished
  only when every citation of the old
  name is fixed in the same commit — `bash tools/check-docs.sh` is what tells you.
