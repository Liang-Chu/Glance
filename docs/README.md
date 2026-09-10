# Documentation index

The only entrance. Every live document is linked from here.

## Read first

1. **[`DESIGN.md`](DESIGN.md)** — current design, decisions only, the source of truth.
   Grep its decision index before designing anything.
2. **[`STATUS.md`](STATUS.md)** — what is true right now: what exists, what runs.
3. **[`PROCESS.md`](PROCESS.md)** — how work is done today: run, deploy, release, review.
4. **[`CONTRACT.md`](CONTRACT.md)** — the shape of the call to your backend. Read this before
   writing one.
5. **[`BACKLOG.md`](BACKLOG.md)** — the only list of open work.

## The map

Every slot is listed, including the ones with no file yet. Nothing is created outside this table: a
thing that fits no slot means the map is short one, which is a decision. A `reserved` slot has no
file — its path is where the file will go when the trigger in the last column fires, and reserved
paths are written plainly here rather than linked, because there is nothing to link to.

| Slot | Path | Holds | State |
| --- | --- | --- | --- |
| `ROOT_README` | [`../README.md`](../README.md) | what the project is, for someone who has never seen it; not the map | active |
| `AGENT_ENTRY` | [`../CLAUDE.md`](../CLAUDE.md) | the instruction to read this file before touching anything, and the obligations that hold without being asked | active |
| `ENTRY` | `docs/README.md` (this file) | the only entrance to the docs; the slot map, the authority order, the lifecycle rule | active |
| `ORIENTATION` | [`ORIENTATION.md`](ORIENTATION.md) | the shape of the codebase: which directory holds what, which way dependencies point | active |
| `DESIGN` | [`DESIGN.md`](DESIGN.md) | current design — decisions only | active |
| `PROCESS` | [`PROCESS.md`](PROCESS.md) | how work is done today: run, deploy, release, review | active |
| `STATUS` | [`STATUS.md`](STATUS.md) | what is true right now: what exists, what runs, what stage each piece is at | active |
| `BACKLOG` | [`BACKLOG.md`](BACKLOG.md) | work not started yet, and nothing else | active |
| `CONSTRAINTS` | [`CONSTRAINTS.md`](CONSTRAINTS.md) | constraints, negative space, lessons | active |
| `PREFERENCES` | `docs/PREFERENCES.md` | project-specific overrides and standing instructions | reserved — first override of a docs-and-constraints default, or first standing instruction |
| `DECISIONS` | `docs/decisions/NNNN-slug.md` | ADRs — append-only, never retired | reserved — first "we should have written down why" |
| `RETIRED` | `docs/retired/` + `INDEX.md` | superseded design, finished plans, dead process, evidence | reserved — first doc goes stale |
| `REFERENCE` | `docs/reference/` | facts about things we do not control: the G2, its companion app, Android's notification behaviour | reserved — first fact we depend on but do not control |
| `CONTRACT` | [`CONTRACT.md`](CONTRACT.md) | the shape of the call between this app and the backend the user runs | active |
| `GLOSSARY` | `docs/GLOSSARY.md` | the closed vocabulary: one definition per term | reserved — first term used with two meanings in one week |
| `MEASUREMENTS` | `docs/measurements/<campaign>-<date>/` | raw data behind any number a decision rests on, plus that campaign's write-up | reserved — first decision backed by numbers |
| `REPORT` | `docs/REPORT.md` | the latest snapshot — an audit, a nightly run, a review result. Never authority | reserved — first audit or run whose output someone will re-read |
| `WIP` | `docs/WIP.md` | the working material of what is being worked on right now | reserved — first design that takes more than one sitting to settle |
| `PARKED` | `docs/PARKED.md` | complete but unwired components | reserved — first near-miss deleting live code |
| `SHIPPED` | `docs/SHIPPED.md` | append-only record of what was delivered and when | reserved — first time git log cannot answer "what changed between these two dates" |
| `NEGATIVE_SPACE` | a section inside [`CONSTRAINTS.md`](CONSTRAINTS.md) | what is deliberately not done, and why | reserved — first "we discussed this, we are not doing it" |
| `LESSONS` | a section inside [`CONSTRAINTS.md`](CONSTRAINTS.md) | mistakes worth their own entry, each with its cost | reserved — first mistake worth its own entry |
| `SRC` | decided with the stack; [`ORIENTATION.md`](ORIENTATION.md) names it once it exists | source root — not a doc slot; the path the audit scans read | not a doc slot |

## Precedence

Every document inherits its slot's rank:

CONSTRAINTS → DECISIONS → DESIGN → CONTRACT → REFERENCE → PROCESS → STATUS → code

`WIP` and `REPORT` are deliberately absent: neither is authority, ever.

Code never overrules a document; code that contradicts one is a bug or an unrecorded decision, and
one of the two is fixed in that commit. A conflict inside one slot gets a tiebreak stated where it
applies.

## Lifecycle

1. A live doc contains only open, currently-true content.
2. Before retiring, move surviving items into [`BACKLOG.md`](BACKLOG.md).
3. Retire with `git mv` into `docs/retired/<kind>/`, add one row to `docs/retired/INDEX.md` (why +
   where the live truth is), fix inbound links in the same commit.
4. Live docs link only to `docs/retired/INDEX.md`, never into it.
5. ADRs are never retired; they take supersession notes.
