# Agent entry

**Read [`docs/README.md`](docs/README.md) before touching anything in this repository.** It is the
only entrance to the documentation, and it carries the slot map, the authority order, and the
lifecycle rule. This file routes; it owns nothing. Every fact lives in exactly one document, and a
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
- **Before creating, renaming, splitting, moving, or retiring any document**, read the lifecycle
  rule in [`docs/README.md`](docs/README.md) and the procedure in
  [`docs/PROCESS.md`](docs/PROCESS.md). Nothing is created outside the slot map in
  [`docs/README.md`](docs/README.md), and a rename is finished only when every citation of the old
  name is fixed in the same commit — `bash tools/check-docs.sh` is what tells you.
