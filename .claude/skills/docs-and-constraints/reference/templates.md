# Skeletons

Copy verbatim; do not invent a format. Placeholders are `<>`. Names and markers are the defaults
under `SKILL.md` "Slots" and "Values" — change them in `PREFERENCES`, not here. Each heading below is
`<slot> — <path>`, in the order of `SKILL.md` "Slots".

---

## Day one

### `ROOT_README` — `README.md`

```markdown
# <project name>

<One or two sentences: what this project is and who it is for, in the words someone who has never
seen it would use.>

## What it does

- <capability, stated as what it does for whoever uses it>
- <capability>

## Start here

1. **Requirements**: <runtime, toolchain, hardware, accounts — each with the version that works>
2. **Run it**: `<exact command from a clean checkout>`
3. **Worked when**: <what a successful run prints or produces>

Everything else starts at [`docs/README.md`](docs/README.md) — the only entrance to the docs.
```

What is true right now is `docs/STATUS.md`; why anything is built this way is `docs/DESIGN.md`; the
slot map is `docs/README.md`. Link them; do not restate them here.

### `ENTRY` — `docs/README.md`

```markdown
# Documentation index

The only entrance. Every live document is linked from here.

## Read first

1. **[`PREFERENCES.md`](PREFERENCES.md)** — project-specific overrides and standing instructions.
   Overrides every default.
2. **[`DESIGN.md`](DESIGN.md)** — current design, decisions only, the source of truth.
   Grep its decision index before designing anything.
3. **[`PROCESS.md`](PROCESS.md)** — how work is done today: run, deploy, release, review.
4. **[`BACKLOG.md`](BACKLOG.md)** — the only list of open work.

## The map

Every slot is listed, including the ones with no file yet. Nothing is created outside this table:
a thing that fits no slot means the map is short one, which is a decision.

| Slot | Path | Holds | State |
| --- | --- | --- | --- |
| `<SLOT>` | `<path, or "a section inside <file>">` | <what it holds, one line> | active / reserved / off |

## Precedence

Every document inherits its slot's rank:

CONSTRAINTS → DECISIONS → DESIGN → CONTRACT → REFERENCE → PROCESS → STATUS → code

Code never overrules a document; code that contradicts one is a bug or an unrecorded decision, and
one of the two is fixed in that commit. A conflict inside one slot gets a tiebreak stated where it
applies.

## Lifecycle

1. A live doc contains only open, currently-true content.
2. Before retiring, move surviving items into `BACKLOG.md`.
3. Retire with `git mv` into `retired/<kind>/`, add one row to `retired/INDEX.md` (why + where the
   live truth is), fix inbound links in the same commit.
4. Live docs link only to `retired/INDEX.md`, never into it.
5. ADRs are never retired; they take supersession notes.
```

Fill the map with one row per slot in `SKILL.md` "Slots", in that order: `Default path` becomes this
project's real path, `Holds` is copied across, and `State` is that slot's day-one state until
something changes it — an `off` state also carries its reason in `PREFERENCES`.

### `ORIENTATION` — `docs/ORIENTATION.md`

````markdown
# Orientation — the shape of the codebase

*What lives where, and which way dependencies point. Read second, after the project README.
Why anything is this way is [`DESIGN.md`](DESIGN.md); what is built is [`STATUS.md`](STATUS.md).*

## Layout

```
<root>/
  <dir>/     <one line: what this directory is responsible for>
  <dir>/     <one line>   [does not exist yet — <where it is tracked>]
```

## Dependencies

<direction>, and what enforces it: <module boundary / compile error / lint rule>.
An arrow nothing enforces is marked unenforced.

## Entry points

| | |
|---|---|
| build | `<command>` |
| runs first | `<file or symbol>` |
| the seam for tests | `<interface, and its fake>` |
````

Verified by `ls` during a cleanup sweep: a directory named here that does not exist, or a
top-level source directory missing from here, is a defect.

### `DESIGN` — `docs/DESIGN.md`

```markdown
# Design

> Latest design only. Decisions only — no process, no plans, no change log.

## 0 · Decision index — grep before designing anything

| Question | Decided in |
| --- | --- |
| <in words someone would search for> | <section> |

Grep the whole file before concluding a question is undecided.

## 1 · <Section>

> **Prerequisites**: <sections to read first>.
> **Decides**: <one line>.

<Body. Self-contained: copy in conclusions from other sections rather than pointing at them.>

### Acceptance

- `<section-slug>-1` <checkable condition>
- `<section-slug>-2` <checkable condition>
```

### `DESIGN` after a split — `docs/design/NN-<slug>.md`

```markdown
# NN · <Title>

> **Prerequisites**: [`01-xxx.md`](01-xxx.md) · [`02-yyy.md`](02-yyy.md).
> **Decides**: <one line>.

## 1 · <Section>

<Body, self-contained.>

## Acceptance

- `<section-slug>-1` <"the table gains 0 rows and the pointer targets the row">
- `<section-slug>-2` <"the sort path never reads that field" — a static check>
```

### `PROCESS` — `docs/PROCESS.md`

```markdown
# Process

> How this project is built, run, deployed, and reviewed **today**. Current state only.
> Superseded procedures move to `retired/process/`; this file never says "we used to".

## 1 · <Procedure name>

**When**: <the trigger>
**Preconditions**: <what must be true first>

1. `<exact command>`
2. `<exact command>`

**Verify**: <how to confirm it worked — the artifact, not the log>
**On failure**: <what to do; never a silent retry>
```

### `STATUS` — `docs/STATUS.md`

```markdown
# Status — what is true right now

*The present view. `BACKLOG.md` is what is not done; this is what is. Overwritten, never appended —
a dated snapshot of one day belongs in [`REPORT.md`](REPORT.md).*

Stage: **<current stage>** · last full sweep <YYYY-MM-DD>

| Component | Covers | State | Verified how, when |
| --- | --- | --- | --- |
| <component> | `<path or artifact>` | **live** / **built, unwired** / **not started** | `<command or endpoint>`, <YYYY-MM-DD> |

A row is suspect when its path has commits newer than its verification date — then re-verify it or
delete it. No percentages: name which parts are live, which are built but unwired, which do not
exist.
```

### `BACKLOG` — `docs/BACKLOG.md`

```markdown
# Backlog — the only list of open work

> Open items only. Done means deleted; the record is the git history.
> Every item states what blocks it and what done looks like.
> 🟡 is only "waiting on a person to decide".
>
> Last swept: <YYYY-MM-DD>

| Pri | Item | Blocked on / done when |
| --- | --- | --- |
| 🔴 | | |
```

### `CONSTRAINTS` — `docs/CONSTRAINTS.md`

```markdown
# Constraints

> Numbered, one line each, every one carrying the verdict that detects a violation.
> Breaking one appends a dated row beneath it; the rule's own wording stays as it was.

## C<N> — <one line>

<Boundary in two or three sentences. List the specific "must never" behaviours.>

**Verdict / test:** <how a violation is detected>

**Breaches**

| Date | What broke it | What would restore it |
| --- | --- | --- |
| <YYYY-MM-DD> | <the specific case, named> | <the condition that ends it> |
```

Everything above the breach table is byte-identical to what it was before the breach — no "except",
no "for now", no exception folded into the boundary or the verdict. A row is deleted by the commit
that satisfies its restore condition, and by nothing else.

Numeric-limit variant:

```markdown
## C<N> — <limit> is a gate

<What it covers>, and the number. Source-file default is `FILE_MAX`.

**Verdict / test:** <the check that fails past it>
```

Exceeding it takes a breach row like any other constraint. The number itself moves only as a
recorded decision — struck through and replaced in place — and never gains a per-case exception.

---

## Reserved slots — write the file when the trigger fires

### `PREFERENCES` — `docs/PREFERENCES.md`

```markdown
# Project preferences

Overrides the docs-and-constraints defaults for this project. Read before acting; this file wins.

## Parameter overrides

One row per parameter this project changes. A parameter left at its default gets no row; when
nothing is changed, this section says so in one line and carries no table.

| Parameter | This project | Why |
| --- | --- | --- |
| `<parameter>` | `<value>` | <what forced the change> |

## Slots

One row per slot whose state differs from the default. `off` needs a reason.

| Slot | State | Why |
| --- | --- | --- |
| `<slot>` | <active / reserved / off> | <reason> |

## Standing instructions

- <Things to always do in this project, one line each.>
- <Things to never do.>
- <Conventions that would otherwise be guessed: naming, commit style, review expectations.>

## Owner decisions that are not design

<Rulings that govern how work happens rather than what is built — deletion authority, what may be
deployed without asking, what always needs a decision first.>
```

### `DECISIONS` — `docs/decisions/NNNN-slug.md`

```markdown
# ADR NNNN — <the conclusion in one line, not a topic label>

Status: **<status>** · <YYYY-MM-DD> · prompted by <who / what incident>.
<Evidence: verified on what data or environment, with numbers. N tests pass / 0 fail.>
Supersedes ADR 00XX <which part>.
Governs `<table>`, `<module path>`.

---

## 1 · Root cause

> *"<verbatim quote>"*

**Root cause:** <one paragraph>

## 2 · What is decided

## 3 · Blast radius / migration
```

Status ∈ `proposed` · `accepted` · `accepted (staged)` · `accepted and implemented` ·
`proposed; implementation pending`. Never write "implemented" without evidence in the Status block.

### `RETIRED` — `docs/retired/INDEX.md`

```markdown
# Retired — the only entrance

> A finished or abandoned doc is retired, not deleted. Reachable only from `../README.md` → here.
> Live docs never link to a file inside this folder.

| Doc | Why retired | Where the live truth is |
| --- | --- | --- |
| [design/<file>](design/<file>) | <specific: "acts entirely on a schema dropped <date>; `a` and `b` are absent from the source"> | <successor> |
```

The reason must let a reader judge whether the retirement was correct. *Detected by: a reason that
names no specific dead thing — "outdated", "stale", "superseded", "no longer relevant".*

### `REFERENCE` — `docs/reference/<subject>.md`

```markdown
# <Subject> — reference

*<What it is and when to read it, in one line.>*

Observed <YYYY-MM-DD> against <version / revision / firmware>. Confidence is per claim, not per
document.

## Source ranking

| Rank | Source | Kind | Trust |
| --- | --- | --- | --- |
| 1 | <source> | <generated / measured / community> | **Highest** — <why> |

## <Facts>

- **<fact>** — <value>. `confirmed` / `likely` / `speculative`. <how it was established>
```

Owns its numbers: any value stated here is linked from elsewhere, never restated.

### `CONTRACT` — `docs/CONTRACT.md`

```markdown
# Contract — <boundary>

*<Who the two sides are and what depends on this. One italic line.>*

Owner: <side that defines the shape> · verified <YYYY-MM-DD> against <revision>.
What the other side still owes is [`BACKLOG.md`](BACKLOG.md), not this file.

## <Surface>

| Clause | Shape | Verified |
| --- | --- | --- |
| <endpoint / message / field> | <exact shape> | <how, against which revision> |

**Breaking a clause is a decision**, recorded and coordinated before it ships.
```

### `GLOSSARY` — `docs/GLOSSARY.md`

```markdown
# Glossary

*One definition per term. No term appears anywhere with a second meaning.*

| Term | Means |
| --- | --- |
| <term> | <definition> |
| ~~<retired term>~~ | **retired <YYYY-MM-DD>** → <replacement> |
```

### `MEASUREMENTS` — `docs/measurements/<campaign>-<YYYY-MM-DD>/`

```
docs/measurements/<campaign>-<YYYY-MM-DD>/
  README.md    method, self-check table, the command that recomputes the numbers
  report.md    numbers and examples only; policy is decided by a person
  <data>       raw outputs, samples, gold set, baseline
```

`docs/measurements/README.md` gets one paragraph per campaign: what it is, which conclusion cites
it, where the script lives, how to recompute.

### `REPORT` — `docs/REPORT.md`

```markdown
# <What was run> — <YYYY-MM-DD>

*A snapshot. Its conclusions were filed in <slot> before this was written; if anything here exists
only here, that is a defect.*

**Ran**: `<command or procedure>` · **against**: `<environment / revision>`

## Numbers

## What it changed

| Conclusion | Filed in |
| --- | --- |
| <finding> | [`STATUS.md`](STATUS.md) / [`BACKLOG.md`](BACKLOG.md) / … |
```

The next report overwrites this file; git holds the rest. No index row, no successor, no retirement.
**Nothing cites it** — a citation from code, tests, or another repository is fixed by moving the
content into the slot that owns it, in the same commit.

### `WIP` — `docs/WIP.md`

```markdown
# WIP · <thread>

*Unfinished thinking. Nothing cites this file, and it is not in the precedence order. Tracked as
<BACKLOG row>. Opened <YYYY-MM-DD>, last touched <YYYY-MM-DD>.*

## The question

<What has to be decided before this can be built.>

## Draft

<The design as it currently stands. It will move to DESIGN when it settles.>

## Tried and rejected

| Tried | Why it failed |
| --- | --- |

## Open

- <what still has to be answered>
```

**Landing this thread promotes**: decision → `DESIGN` + its index row · leftovers → `BACKLOG` ·
new state → `STATUS` · evidence → `REPORT`/`MEASUREMENTS`. **Leave the draft.** The next thread
overwrites this file. If the thread was abandoned, hoist what survives into `BACKLOG`.

### `PARKED` — `docs/PARKED.md`

```markdown
# Parked — complete but unwired

*Components that are built and work, waiting on wiring. Every row names the target that exists and
the wiring that is missing; a row missing either name is rot, and the code is deleted.*

| Component | Why unwired | What wiring it requires |
| --- | --- | --- |
| `<path>` | <why unwired — quote the original note if there is one> | <what wiring it requires> |
```

### `SHIPPED` — `docs/SHIPPED.md`

```markdown
# Shipped — append-only

*What was delivered and when. Never used to answer what is true now — that is [`STATUS.md`](STATUS.md).
Never a list of open items.*

| Date | Delivered | Evidence |
| --- | --- | --- |
| <YYYY-MM-DD> | <one line> | <commit, test, or endpoint that proves it> |
```

### `NEGATIVE_SPACE` — a section inside `CONSTRAINTS`

```markdown
## Negative space — deliberately not done

Everything listed is closed. Reopening appends a dated row beneath the closed one — `date · what
changed · what is being done instead` — and leaves the closed row's wording as it was.

| What is not done | Why | Which part owns it |
| --- | --- | --- |
| <what is not done> | <why — one line, with a number if there is one> | <which part owns it> |
```

### `LESSONS` — a section inside `CONSTRAINTS`

```markdown
## LESSON-<N> — <what was walked into>

<What happened: when, what action, what followed.>

**Cost:** <hours, how long wrong data stood, how many conclusions rested on it>

**Rule:** <one line, executable>
```

With `LESSONS` off: keep the rule as a constraint, discard the narrative.

---

## Forms — written into a file that already exists

### Decision-index row

It **locates**; it does not restate. Cite the document and a **heading quoted verbatim**. Never
cite a position — no section number, no criterion ordinal, no table-row index.

```markdown
| <question, in words someone would search for> | **[<file>](<file>) "<named heading>"** (<who>, <YYYY-MM-DD>) |
```

If a row needs a hint so the table is skimmable, it is a **verbatim fragment** of the section, and
the section wins on any disagreement:

```markdown
| <question> | **[<file>](<file>) "<heading>"** (<who>, <YYYY-MM-DD>: "<exact words from the section>") |
```

### Superseding annotation — edit in place, never delete the line

```markdown
| `<key>` | ~~<old wording>~~ → **<new wording>** (<who>, <YYYY-MM-DD>: <reason>; `<replaced concept>` is void) |
```

Invalid measurement:

```markdown
> [Correction <YYYY-MM-DD>: **the <mechanism> under test never took effect; the table above and the
> conclusions below are void as measurements.** Cause: <one line>. Re-run: <where>.]
```
