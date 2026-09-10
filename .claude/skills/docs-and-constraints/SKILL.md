---
name: docs-and-constraints
description: Set up and run a project's documentation system together with its constraint set — one entry index, one living design doc that is the source of truth, one backlog, one process doc, and the code constraints those docs enforce (fail fast, single source of truth, file-length gate, kill legacy on sight). Use when starting a project, recording a decision, syncing docs after a change, retiring a stale doc, auditing docs against code, or writing and enforcing project constraints. Stack- and domain-agnostic.
---

# Documentation and constraints

Docs and constraints are one system: docs state what the project **is**, constraints state what it
may never **become**. Both move in the same commit as the code.

**Before anything else: read `PREFERENCES` if it exists. It overrides §0 and every default here.**

## Index — question → where it is answered

Grep this before writing anything. **The row locates; the named section states the rule.** A question
that is not a row is not a question without an answer — grep the whole file.

| Question | Where it is answered |
| --- | --- |
| What files exist on day one, and what makes a reserved slot appear later? | `SKILL.md` "Slots" |
| I was handed a specification and told to set the project up — what do I write first? | `reference/templates.md` "Day one" |
| The project already has documents — how do I move it onto this system? | `SKILL.md` "Adopting into an existing project" |
| Where does a new kind of document go, and may I create one at all? | `SKILL.md` "The doc map" |
| Should this be a file yet, or stay a reserved slot? | `SKILL.md` "Whether to create a file yet" |
| When does one document split into two? | `SKILL.md` "The doc map" |
| Two documents disagree — which one wins? | `SKILL.md` "Which document wins — precedence" |
| Which rules hold in every document, whatever slot it sits in? | `SKILL.md` "Every document: one owner, aspirational marks, citation by name" |
| How do I cite a document, a heading, or an acceptance criterion? | `SKILL.md` "Every document: one owner, aspirational marks, citation by name" |
| How is a document retired? | `SKILL.md` "`ENTRY`, and how a document is retired" |
| Where is a decision recorded, and what moves with it? | `SKILL.md` "Operations" |
| I changed one thing — which files may that change touch? | `SKILL.md` "Motion — what one change touches" |
| What must a `DESIGN` section carry, and what goes at the top of the file? | `SKILL.md` "DESIGN" |
| What must a `STATUS` row carry? | `SKILL.md` "STATUS — what is true right now" |
| Where does open work go? | `SKILL.md` "BACKLOG — what is not started" |
| Where does unfinished thinking go while a design is being drafted? | `SKILL.md` "WIP — what is being worked on right now" |
| Where do I state which directory holds what? | `SKILL.md` "The code map — `ORIENTATION`" |
| Where do the numbers behind a decision go, and where does an audit's output go? | `SKILL.md` "REPORT — the latest snapshot" |
| Where does a fact about something I do not control go? | `SKILL.md` "REFERENCE — facts you depend on but do not own" |
| What must a constraint carry, and what happens when one has to be broken? | `SKILL.md` "CONSTRAINTS" |
| Which code constraints does a project start with? | `SKILL.md` "Default code constraints — adopt as `C1…C9`" |
| A document is getting long and unreadable — what am I allowed to delete? | `SKILL.md` "DESIGN", and the `DOC_MAX` row in "Values" |
| How long does a struck-through decision stay, and when does it go? | `SKILL.md` "DESIGN" |
| A warning says not to do something that no longer exists — keep it or cut it? | `SKILL.md` "DESIGN" |
| What sends an agent here in the first place, without anyone asking it to? | `SKILL.md` "Slots", `AGENT_ENTRY` |
| When do I open `templates.md`, and when `cleanup-audit.md`? | `SKILL.md` "References" |

## 0 · Parameters

Defaults. Per-project overrides live in `PREFERENCES`, never inline in the sections below.

### Slots

**Every slot is declared on day one, in `ENTRY`'s map — including the ones with no file yet.**
Three states:

| State | Means |
| --- | --- |
| **active** | the file exists and its rules apply |
| **reserved** | no file yet — but the slot, its rule, and its path are already declared in `ENTRY`. The file appears the moment it has one real item |
| **off** | deliberately not used in this project, **with the reason stated in `PREFERENCES`**. Open only to a slot whose day-one state below is `reserved`; the **active** rows are never off |

| Slot | Default path | Holds | Day-one state, and what activates it |
| --- | --- | --- | --- |
| `ROOT_README` | `README.md` | what the project **is**, for someone who has never seen it; not the map | **active** |
| `AGENT_ENTRY` | `CLAUDE.md` (or the agent-config file the harness loads by itself) | **the instruction to read `ENTRY` before touching anything**, and the obligations that hold without being asked. The harness loads it unprompted, which is the whole point: this is what makes the system self-starting instead of dependent on someone saying "follow the docs skill" every session | **active** |
| `ENTRY` | `docs/README.md` | the only entrance to the docs; carries the slot map, the authority order, and the lifecycle rule | **active** |
| `ORIENTATION` | `docs/ORIENTATION.md` | **the shape of the codebase**: which directory holds what, which way dependencies point | **active** |
| `DESIGN` | `docs/DESIGN.md` → `docs/design/` when a section's subject falls outside the file's title | current design — decisions only | **active** |
| `PROCESS` | `docs/PROCESS.md` → `docs/process/` when a section's subject falls outside the file's title | how work is done today: run, deploy, release, review | **active** |
| `STATUS` | `docs/STATUS.md` | what is **true right now**: what exists, what runs, what stage each piece is at | **active** |
| `BACKLOG` | `docs/BACKLOG.md` | work **not started yet**, and nothing else | **active** |
| `CONSTRAINTS` | `docs/CONSTRAINTS.md` | constraints, negative space, lessons | **active** — first entry is the first mistake that cost something |
| `PREFERENCES` | `docs/PREFERENCES.md` | project-specific overrides and standing instructions | **reserved** — first override of anything in §0, or first standing instruction |
| `DECISIONS` | `docs/decisions/NNNN-slug.md` | ADRs — append-only, never retired | **reserved** — first "we should have written down why" |
| `RETIRED` | `docs/retired/` + `INDEX.md` | superseded design, finished plans, dead process, evidence, in `design/ process/ plans/ evidence/`; `INDEX.md` is its only entrance | **reserved** — first doc goes stale |
| `REFERENCE` | `docs/reference/` | facts about things you do not control: hardware, wire protocols, third-party APIs, another team's doors | **reserved** — first fact you depend on but do not control |
| `CONTRACT` | `docs/CONTRACT.md` | the interface across a boundary that both sides depend on — wire format, API surface, cross-team doors | **reserved** — first interface another party depends on |
| `GLOSSARY` | `docs/GLOSSARY.md` | the closed vocabulary: one definition per term, and no term used elsewhere with a second meaning | **reserved** — first term used with two meanings in one week |
| `MEASUREMENTS` | `docs/measurements/<campaign>-<date>/` | raw data behind any number a decision rests on, plus that campaign's write-up. **Accumulates**: one folder per campaign, kept | **reserved** — first decision backed by numbers |
| `REPORT` | `docs/REPORT.md` | the latest snapshot — an audit, a nightly run, a review result. A reusable surface: the next report overwrites it. Never authority | **reserved** — first audit, nightly run, or review whose output someone will want to re-read |
| `WIP` | `docs/WIP.md` | the working material of what is **being worked on right now** — the design being drafted, what has been tried. A reusable surface: the next thread overwrites it. `docs/wip/` only if threads genuinely run in parallel | **reserved** — first design that takes more than one sitting to settle |
| `PARKED` | `docs/PARKED.md` | complete but unwired components | **reserved** — first near-miss deleting live code |
| `SHIPPED` | `docs/SHIPPED.md` | append-only record of what was delivered and when | **reserved** — first time "what changed between these two dates" cannot be answered from git log |
| `NEGATIVE_SPACE` | a section inside `CONSTRAINTS` | what is deliberately not done, and why | **reserved** — first "we discussed this, we are not doing it" |
| `LESSONS` | a section inside `CONSTRAINTS` | mistakes worth their own entry, each with its cost | **reserved** — first mistake worth its own entry |
| `SRC` | `src/` | source root — not a doc slot; the path the audit scans read | — |

**`ROOT_README` and `AGENT_ENTRY` are the only slots outside the docs root, and they own nothing.**
Both exist to route — the first for a person who found the repository, the second for an agent about
to change it — and neither appears in the authority order, because a file that owns nothing can never
be the thing that wins. **A changeable fact restated in either is a defect**: it is a second copy of
something a real document is keeping current, living where nothing will. A permanent one may appear
and links to the document that owns it. This is the most reliably violated rule in the set — the two
routing files are the ones nobody thinks to sweep, so they rot first and they rot worst, while every
document that actually owns something stays correct.

`LESSONS` off means: keep the rule in `CONSTRAINTS`, discard the incident narrative.
`GLOSSARY` is commonly folded into `DESIGN` as one section; that is a slot placement, still declared.

### Values

| Name | Default | Governs |
| --- | --- | --- |
| `FILE_MAX` | **500 lines** | source files. **A gate**: past it a line-count check in CI fails and the file is split. Raising it means editing `CONSTRAINTS` with the reason |
| `DOC_MAX` | **400 lines** | prose documents. **Not a split gate — a compaction trigger.** Past it, the file is read whole and compacted: spent strikes deleted, dead prohibitions deleted, the changelog that accreted inside it dropped to the current ruling plus at most one overturn. If it is still over afterwards and every line is currently true, it is simply a long document and that is fine — **length is a prompt to re-read, never a reason to delete something true** |
| `DOC_SPLIT` | **one topic per file** | prose documents. A file splits when a section's subject falls outside the file's title. **Never split prose on a line count.** A lookup table read by section, an append-only log, and a single-source-of-truth table or tree that other files cite are each one topic however long |
| `PRIORITIES` | 🔴 blocks other work · 🟠 important · ⚪ normal · 🟡 waiting on a person | backlog markers |
| `DOC_LANG` | the language the code comments are in | what all docs are written in |

**There are no time-based rules here.** Every check is triggered by an event you can observe:

| Trigger | Re-check |
| --- | --- |
| the code a `STATUS` row describes has commits newer than that row's verification date | that row |
| a rename, a move, a split, a retirement | inbound links, link labels, prose mentions, and every citation outside the docs tree |
| a decision lands | the index row, the concept it replaced |
| before a release, a rebuild, or a migration | the whole audit |
| a document told someone something untrue | that document, and why nothing caught it |
| a prose document passes `DOC_MAX` | read it whole and compact it — this is the only length rule, and it never splits the file |
| a second overturn lands on a section that already carries a strike | delete the oldest strike in that commit |
| the subject of a prohibition no longer exists in the code | delete the prohibition, or promote it to `CONSTRAINTS` with a verdict |
| a `BACKLOG` item closes, or a `RETIRED` document is filed | the strikes and warnings that were only holding its context |

Filenames are defaults; the **slots** are what matter. Rename freely, but keep one file per slot.
A convention that earns its place in one project and not in others belongs in that project's
`PREFERENCES` — it is never promoted up here.

## 1 · Layout

### The doc map

The map is the **Slots** table in §0, complete from day one. A `reserved` slot is declared there and
in `ENTRY`, and its file appears when it has its first real item — not before, and not somewhere
else.

**No document is created outside this map.** Amending the map is a deliberate act: record the
decision, add the slot to `ENTRY`, place it in the authority order, in one commit.

A slot splits into a folder — `DESIGN.md` → `design/00-index.md` + `NN-*.md`, `PROCESS.md` the
same — **when a section's subject falls outside the file's title**: that section moves to a file
whose title covers it. **Split by topic, never by length.** The slot, its rule, and its rank stay
exactly as declared.

### The code map — `ORIENTATION`

- **One line per directory, saying what it is responsible for.** Not why — why is `DESIGN`.
- **State which way dependencies point, and what enforces it** — a module boundary, a compiler
  error, a lint rule.
- **Mark what does not exist yet.**
- **A module entry states what that module is responsible for, in one paragraph at most.** The moment an entry starts
  explaining how the module works, that content is `DESIGN` — move it there.
- **Name the entry points**: how it is built, what runs first, where the seam for tests is.
- It never repeats a value that another document owns — it points.

*Verified by `ls`*: a directory named here that does not exist, or a top-level source directory
missing from here, is a defect — **but only during a cleanup sweep, never as a daily gate.**

## 2 · DESIGN

- Latest only. Every design update is written *into* it. No second copy, no summary, no overview.
- Decisions only. Options weighed, approaches rejected, migration steps, review threads → `RETIRED`
  or an ADR.
  *A project whose rulings arrive with the evidence that forced them may declare a combined
  decision-plus-basis form in `PREFERENCES` — but the basis stays compressed to what makes the
  ruling checkable, and the full evidence still lives in `MEASUREMENTS` or `REPORT`.*
- Sections are self-contained: copy in a conclusion from another section rather than writing "see §4".
- Every section ends in acceptance criteria — checkable conditions ("the table gains 0 rows", "the
  id is unchanged", "this path never reads that field"), **each carrying a short stable id**
  (`<section-slug>-<n>`, written beside it). An id is assigned once and never renumbered; a deleted
  criterion's id is retired, never reused. No criteria = not decided = a draft.
- **Retired criteria collapse into a roster, not a graveyard of struck paragraphs.** The id must
  survive so it is never reused — the *text* need not. Once a criterion is retired, its full struck
  wording earns one commit's worth of readability and no more; after that the whole set becomes a
  single line at the end of the section — `Retired: calibration-4, -7, -24, -25 (shape step,
  2026-09-09)` — which protects every id at a cost that does not grow with the number of them. **A
  section whose retired criteria outweigh its live ones is unreadable in the exact place a reader
  most needs certainty**, and the id-reuse rule is what people wrongly cite for keeping it that way.
- **A superseded decision is struck through in place with its date and its replacement — and the
  strike is a guard, not a memorial.** It exists for one reason: to stop a reader re-proposing
  something already ruled out. It is written to be *deleted later*, and it goes the moment it can no
  longer prevent that mistake — when the replaced concept is gone from the code, no open `BACKLOG`
  item turns on it, and the replacement has itself been stable through a later ruling. **git history
  is the permanent record; the strike is a cache of it for the reader who will not run `git log`.**
  Deleting a spent one is not losing a decision.
- **Never stack strikes. A section shows the current ruling and at most the one it replaced.** The
  moment a third ruling lands on the same section, the oldest strike is deleted in that commit — a
  strike inside a strike, or a chain of "it showed X, then Y, then Z", is a changelog, and a
  changelog in a design document is the thing that makes it unreadable. One overturn is context; two
  is history, and history has a home.
- **A prohibition dies with its subject.** "Do not use X", "never call Y", "this must not be
  reintroduced" earns its place only while X still exists somewhere a reader could reach for. Once
  the thing is gone from the code, the warning costs every future reader the time to work out what it
  was protecting and buys nothing — delete it. If it must survive because the mistake is repeatable
  in a way the code cannot show, it is a constraint with a verdict, and it moves to `CONSTRAINTS`.
- Top of the file: a decision index — **`question → where it is decided`, and nothing else.** The
  row locates; the section states. If a row must carry a hint, it is a **verbatim fragment** of the
  section, and the section wins on any disagreement. Phrase the question in words someone would
  actually search for. **Grep it before designing anything.** Absent from the index ≠ undecided —
  grep the whole file.

## 3 · PROCESS · STATUS · REPORT

### PROCESS — how work is done

- Current state only, same as `DESIGN`: how the project is built, run, deployed, released, reviewed.
- If it answers "how do I run this", it belongs here, not in `DESIGN`.
- Steps must be executable as written: exact commands, exact paths, exact preconditions.
- A step that no longer reflects reality is a defect. Fix or delete it.
- Superseded procedures move to `RETIRED/process/`; the current file never carries "we used to".

### STATUS — what is true right now

The forward view is `BACKLOG` (what is not done). The present view is `STATUS`: what exists, what
runs, what stage each piece is at. **It is maintained as promptly as `DESIGN` — same commit as the
change.**

- **One row per component, never per event.** A dated report of one day's work is a snapshot: it
  goes to `REPORT`. `STATUS` is overwritten in place, never appended to.
- **Every row states how it was verified and on what date.** Verify the running thing — the
  artifact, the endpoint, the test.
- **A row is suspect the moment what it describes changes**. Record the path or artifact each row
  covers, and the check is mechanical: commits to that path newer than the row's verification date
  mean the row is unverified again. **Re-verify or delete it**; do not carry it with a warning
  label.
- **No history, no plans, no percentages.** Not "80% done" — state which parts are live, which are
  built but unwired (`PARKED`), which do not exist.
- **`STATUS` never outranks what it describes.** When it disagrees with the running system, `STATUS`
  is stale — fix it, do not argue from it. It ranks last in the authority order, just above code.
- A "Status:" banner at the top of a design or reference document is state leaking out of its slot.
  It belongs in `STATUS`; the design says what is decided.
- When `SHIPPED` is active, a component leaving `STATUS` as complete gets one dated row there. That
  record is append-only and is never used to answer what is true now — that is `STATUS`.

### REPORT — the latest snapshot

An audit, a nightly run, a review result, an A/B write-up. **Never authority at any point in its
life.**

- **One file, reused.** The next report overwrites it; git holds the rest. No index row, no "why
  retired", no successor — that ceremony belongs to `RETIRED`.
- **File the conclusion before you file the report.** Whatever the report established goes into
  `STATUS`, `DESIGN`, `DECISIONS`, or `BACKLOG` first. **A fact whose only home is a report is a
  defect.**
- **Nothing cites it, and it has no rank.** It is not in the precedence order at all. A live document
  may mention that a report exists; it never cites one as its reason. A citation from code, tests, or
  another repository is the same violation: move the content into the slot that owns it.
- Raw data that lets a number be recomputed is `MEASUREMENTS`, not `REPORT`. The report is the
  reading; the measurement is what was read. `MEASUREMENTS` accumulates — one folder per campaign,
  kept.
- **A write-up that belongs to a data set lives with the data** — `MEASUREMENTS/<campaign>/report.md`,
  not here; the two are deleted together. `REPORT` holds the snapshot that has no data set behind
  it: a nightly run, an audit, a review result. One write-up, one home; never both.

## 4 · REFERENCE · CONTRACT · GLOSSARY

### REFERENCE — facts you depend on but do not own

Hardware behaviour, a wire protocol, a third-party API, another team's endpoints.

- Separate **what exists** from **what you want**. Doors that exist, measured, belong here; doors
  you need do not — those are `BACKLOG` items owned by whoever must build them.
- **Where a fact was observed rather than published** — measured, reverse-engineered, inferred from
  behaviour — mark its confidence at the point of use. A published specification needs no such
  marking; do not ritualise it.
- When one fact comes from several sources that disagree, say which wins and why, once.
- **Every fact has exactly one owning document; everywhere else links to it.**
- Mark anything aspirational as such — planned structure, assumed capability, untested claim.
- Re-measure rather than re-read: a reference doc records what was observed, with the date and the
  version it was observed against.

### CONTRACT — the interface across a boundary

- **One owner repo holds the file; the other side links to it and never copies it.** If the consumer
  needs local notes, they are notes about *using* the contract, and they cite it rather than
  repeating its clauses.
- It states the shape, not the implementation behind it.
- **A breaking change is a decision**, recorded and coordinated before it ships — never a doc edit.
- What the other side still owes you is `BACKLOG`, not `CONTRACT`. `CONTRACT` holds what is agreed
  and live.
- Each clause states how it was verified and against which revision.

### GLOSSARY — the closed vocabulary

- One definition per term, and no term is used anywhere with a second meaning.
- A retired term stays listed as retired, with its replacement.
- When a term's meaning changes, that is a decision — annotate the old definition, do not overwrite.

## 5 · BACKLOG · WIP

### BACKLOG — what is not started

- The only list of open work.
- Done means deleted. No ✅ rows; the completion record is the git history.
- Every item states what blocks it and what done looks like.
- 🟡 is only "waiting on a person to decide". Waiting on data, an environment, or an unrun
  measurement is work.
- The backlog holds *what to do*; `DESIGN` holds *why it is decided that way*. Never both.
- **The backlog is the future.** A row stays until the work is done, but the *material* of something
  actively being worked — a design being drafted, what has been tried — is `WIP`. Tracking stays
  here; thinking happens there.
- **No other document keeps a list of open items.** A doc may state that something is unresolved
  and link to the row; it may not keep its own copy.
- **Every "must exist", "required", or "enforced" in any doc has a backlog row, in the same commit.**

### WIP — what is being worked on right now

**The only place in the system that holds unfinished thinking.**

- **One file, reused.** Starting a thread overwrites what the last one left; git holds the rest.
- The item is still tracked in `BACKLOG` until it is done.
- **Nothing cites it, and it has no rank.** It is not in the precedence order at all.
- **Landing a thread promotes, it does not clean up**: the decision goes to `DESIGN` plus its index
  row, whatever is left to do goes to `BACKLOG`, the new state goes to `STATUS`, the evidence goes to
  `REPORT` or `MEASUREMENTS`. **Leave the draft where it is.** The next thread overwrites it.
- **A thread stops being in progress when you stop.** If you abandoned it, hoist what survives into
  `BACKLOG`.

## 6 · The laws that hold in every document · `ENTRY` · retirement

### `ENTRY`, and how a document is retired

`ENTRY` carries three things: the **slot map** (§1, including every `[reserved]` row), the
**authority order**, and the **lifecycle rule**. It links every live doc; anything unreachable gets a
link or gets retired.

1. A live doc contains only open, currently-true content.
2. Before retiring, move surviving items into `BACKLOG`.
3. Retire with `git mv` into the matching `RETIRED` subfolder, add one row to `RETIRED/INDEX.md`
   (**why + where the live truth is now**, never "outdated"), fix inbound links in the same commit.
4. Live docs link only to `RETIRED/INDEX.md`, never to a file inside it.
5. ADRs are exempt from retirement; they take supersession notes instead.
6. Retire in groups: module, tests, generator, output, references.
7. **A rename, a split, a move, and a retirement are each finished only when every citation of the
   old name is fixed in that same commit** — the link targets, the link *labels*, and the plain-text
   mentions. **Before any of the four, grep the whole workspace,
   not just the docs tree** — source comments, tests, and sibling repositories cite documents. A doc
   that a test cites as its acceptance source is a live contract however finished it reads.
   *Detected by: the workspace grep in `reference/cleanup-audit.md` under "Before a rename, a split,
   a move, or a retirement, grep the whole workspace" returning the old name after the commit.*

### Which document wins — precedence

**Precedence ranks slots, not documents.** `ENTRY` states the order once —
`CONSTRAINTS` → `DECISIONS` → `DESIGN` → `CONTRACT` → `REFERENCE` → `PROCESS` → `STATUS` → code —
`WIP` and `REPORT` are deliberately absent: neither is authority, ever —
and every file inherits its slot's rank. Only a conflict *inside* one slot needs a local tiebreak,
stated where it applies.
Code never overrules a document: code contradicting a doc is a bug or an unrecorded decision, and
one of the two is fixed in that commit.

### Every document: one owner, aspirational marks, citation by name

**Every value has exactly one owning document; everywhere else links to it.** This applies to every
slot, not just reference facts.

**Mark anything aspirational at the point of use** — a planned directory, an assumed capability, an
untested claim. Not in a footnote, and not left for the reader to infer from tense.

**Cite by name, never by position.** A citation names the file, and inside the file a heading or an
id. **No position is ever a citation**: not a section number, not an acceptance-criterion ordinal
("the second criterion"), not a table-row index, not a list position. *Check: grep the doc set for
`§`, and for "section", "acceptance", "criterion", "row", "step" followed by a number; a hit that
points at a place inside a document is a defect.*

- **A cited heading is quoted verbatim** — the heading's exact text, in quotes. *Check: grep the
  quoted string in the file the citation names; zero hits is a broken citation.*
- **An acceptance criterion is cited by its text, or by the short stable id written beside it in
  the section that owns it.** *Check: grep the id, or the quoted text, in the file the citation
  names.*

Both checks run in `reference/cleanup-audit.md` under "Citations by position".

**The acceptance test for the doc set is a cold read.** Give the docs to someone with no context and
a real task, and forbid reading the source or sibling repos. Every time they must look outside the
docs is a documentation defect, recorded as one. Run it after any structural change. Three questions
decide it: can they say what the project is, can they start the task, did they have to guess.

## 7 · CONSTRAINTS

- Format: number, one line, verdict. The verdict states how a violation is detected, ideally a
  failing test.
- **Breaking a constraint appends a dated row beneath it, in the same PR — `date · what broke it ·
  what would let it be restored` — and never edits the constraint's own wording.** No other path,
  and **that row is the whole record**: do not also write an ADR saying the same thing. An ADR is
  for when the reasoning is too long to sit in a constraint file, and then the row carries a link,
  not a summary. The row is deleted by the commit that makes the constraint true again, and by
  nothing else.
  *Detected by: the breach commit's diff touching any line of the constraint itself; a row missing
  the date or the restore condition; or a constraint line carrying "except", "unless", "apart
  from", "one exception", "for now" — read the hits, and an exception standing inside the rule's
  own sentence is a breach that was written into the rule instead of beneath it.*
- **Every numeric limit here is a gate** — file length, prompt length, query count. Exceeding one is
  a breach: it takes a breach row like any other, and the number in the rule does not move. Moving
  the number is a decision — recorded where decisions are recorded, the old number struck through
  and replaced in place, never a per-case exception hung off the rule. *Detected by: a limit line
  naming a case ("except the generated file", "800 for the parser") instead of one number.*
- Negative space: a table of what is deliberately not done, and why. Anything in it is closed.
  Reopening appends a dated row beneath the closed one — `date · what changed · what is being done
  instead` — and leaves the closed row's wording as it was. *Detected by: a negative-space row that
  vanished or gained a qualifier between two commits with no dated reopening row under it.*
- Lessons (`LESSONS` on): each entry states its cost — hours, wrong data, a false premise carried
  for a week. No cost stated = drop the entry, keep only the rule.

### Default code constraints — adopt as `C1…C9`

1. **Fail fast; no fallbacks.** A value any calculation consumes is never defaulted: a missing one is
   an error, not a zero. A stand-in value is written in exactly two places — reading a stored
   preference, and parsing external input at the point it enters — and external input is validated
   at that same point, before anything computes with it. Nothing downstream of it supplies a value
   the input did not carry. *Verdict: no silent excepts, no `or {}`. Grep the source for `or `,
   `??`, `?:`, `getOrDefault`, `.get(` with a second argument, `default=`, and `except`/`catch`
   blocks that return a value; read every hit. A hit in a file that computes, rather than reads a
   stored preference or parses input at its entry point, is a breach — and a constant a formula
   reads is a breach wherever it carries a default.*
2. **Single source of truth.** One fact, one definition, in code as in docs. *Verdict: the value
   appears in exactly one file.*
3. **Keep files readable.** `FILE_MAX` is a gate: past it the file is split. *Verdict: a line-count
   check in CI.*
4. **Solve it structurally, not by stacking cases.** A growing list of special cases means the
   structure is wrong. *Verdict: does the fix add a branch per case, or remove the need for it?*
5. **Independent axes stay independent.** Never derive one orthogonal dimension from another. A
   tendency is a cross-check, never a derivation rule. *Verdict: one axis's value appearing in the
   expression that produces another's is a breach; a check that writes, clamps, scales, or replaces
   the value it checks is a derivation whatever it is named.*
6. **Kill legacy on sight.** Delete confirmed legacy in the session you find it. *Confirm three
   ways: production reality; unfiltered grep with the **matching lines read**; run it on the machine
   it belongs on.*
7. **Small batch before the full run, and read the output.** Read the rows actually written.
8. **Zero callers is a question.** *rot* (what it served is gone → delete with its orphans) ·
   *parked* (complete, target exists, wiring missing → keep, record in `PARKED`) · *tombstone*
   (comment naming a deleted symbol → fix the comment, keep the code). *Verdict: a `parked` verdict
   names, in its `PARKED` row, the target that exists and the wiring that is missing; a row missing
   either name is rot, and the code is deleted.*
9. **Verify the running thing.** *Verdict: the artifact, the endpoint, or the running code was
   exercised — not a build log, and not a status endpoint reporting what is on disk.*

## 8 · Operations

**Record a decision** — ① quote verbatim, with who and when; ② write it into the section that owns
it, not a new doc; ③ add a decision-index row in searchable words; ④ strike through what it
supersedes, in place — **and if that section already carried a strike, delete the older one now**,
so the section never holds more than one overturn; ⑤ remove the replaced concept in the same commit
— code, columns, config, tests, seed data, comments, **and any prohibition that existed only to
guard it**; ⑥ re-verify status labels elsewhere claiming that mechanism is implemented.
**If unsure whether it was a decision, do not write it as one, and never attribute an inference to a
person.**

**Step ③ is never deferred: a commit that adds a decision to `DESIGN` and no row to the index is
incomplete**, and the audit reconciles rulings against index rows.

**Sync after a change** — doc and acceptance criteria move in the same commit as the code; never ask
whether docs need updating. On disagreement: for what runs today the code wins; for what it should
become `DESIGN` wins. A legacy identifier in code is migration residue, not license to design around.

**Audit on an event, never on a calendar** — before a release, a rebuild, or a migration; after a
rename or a large merge; and whenever a document turned out to have lied to someone.
`reference/cleanup-audit.md`.

### Motion — what one change touches

If an action below makes you edit more files than its row, something is in the wrong slot. **Nothing
in this table writes the same sentence twice.**

| Action | Touches |
| --- | --- |
| start designing something | overwrite `WIP` |
| a design settles | `DESIGN` section · its index row |
| a ruling overturns an earlier one | the same `DESIGN` section (strike + replace) · index row revised — no new file |
| code change, behaviour unchanged | code only |
| code change, behaviour changed | code · the `DESIGN` section **and its acceptance criteria** · `STATUS` row · a `BACKLOG` row only if it closed one |
| a piece of work finishes | delete the `BACKLOG` row · update the `STATUS` row |
| a problem is found | one `BACKLOG` row — `STATUS` holds component state, and a bug is not one |
| a run produces numbers | `MEASUREMENTS/<campaign>/` (data + its write-up) · the conclusion into whichever slot owns it |
| a run produces no data set | `REPORT.md`, overwritten · the conclusion into its slot first |
| a fact is learned about something you do not control | one `REFERENCE` file |
| a boundary clause changes | `CONTRACT` (owner repo only) · a decision record · the other side's `BACKLOG` |
| a constraint has to be broken | a dated breach row beneath that constraint, its wording untouched |
| a directory appears or moves | code · one `ORIENTATION` line · a `DESIGN` edit only if the module's job changed |
| a doc stops being true | `git mv` to `RETIRED` · one index row · inbound links |
| **a document gets harder to read than it is to re-derive** | that document only — delete spent strikes, dead prohibitions, and stacked history. **A compaction commit changes no ruling**: if it would, it is a decision and goes through "Operations" instead |

## 9 · Whether to create a file yet

**Declare every slot; create a file only when it has content.**

- Which slots are files on day one and which are reserved: the **Slots** table in §0. Every
  reserved slot is a row in `ENTRY`'s map marked `[reserved]`.
- Activating a reserved slot is not a decision — the trigger fired, write the file, drop the marker.
- **Adding a slot that is not in the map is a decision**: it needs a recorded reason, a row in
  `ENTRY`, and a rank in the authority order. Do it deliberately or not at all.
- **Only a slot whose day-one state in the Slots table is `reserved` may be turned off**, and
  turning it off needs a reason in `PREFERENCES`. The eight slots the table marks **active** —
  `ROOT_README`, `ENTRY`, `ORIENTATION`, `DESIGN`, `PROCESS`, `STATUS`, `BACKLOG`, `CONSTRAINTS` —
  are on in every project, and a project that wants one of them narrower states the narrowing in
  `PREFERENCES` and keeps the file. *Detected by: an `off` marker in `ENTRY` or `PREFERENCES`
  against any of those eight names; an `off` marker with no reason beside it in `PREFERENCES`; or
  one of those eight paths having no file.*
- Never split one fact across two slots to make the structure look complete.
- **Reachability is the test.** A live doc that cannot be reached from `ENTRY` is either missing a
  link or is not live.

## 10 · Adopting into an existing project

Five commits.

1. Label every doc with exactly one slot from §1. Unlabelable means one of four things: a
   design/plan hybrid (split it), a second copy (delete one), a doc whose subject no longer exists
   (retire it), or **a slot the map is genuinely missing** (add it deliberately — that is the one
   case where the taxonomy changes).
2. Write `ENTRY`.
3. Create `RETIRED`, move the dead in, hoisting live items to `BACKLOG` first.
4. Sweep git history for decisions living only inside a paragraph; pull them into the decision
   index. `git log -S` finds the ones dropped rather than overturned.
5. Write `CONSTRAINTS` from incident history — anything complained about twice.

## 11 · References

- `reference/templates.md` — skeletons for every file above. **Open it whenever a file in the map is
  created**: day-one setup, and every later activation of a reserved slot. Copy the skeleton; do not
  invent a format.
- `reference/cleanup-audit.md` — the six audit scans, runnable. **Open it whenever an audit is
  triggered** — before a release, a rebuild, or a migration; after a rename or a large merge;
  whenever a document turned out to have lied to someone — and **before renaming, splitting, moving,
  or retiring any document**, for the workspace-wide citation grep.
