# Constraints

> Numbered, one line each, every one carrying the verdict that detects a violation.
> Breaking one appends a dated row beneath it in the same commit; the rule's own wording stays as it
> was. A breach row is deleted by the commit that satisfies its restore condition, and by nothing
> else.
>
> These nine are the docs-and-constraints defaults, adopted whole on 2026-09-10. Several name checks
> that cannot run until there is source to run them against; each of those says so at the point of
> use, and the work to make it runnable is in [`BACKLOG.md`](BACKLOG.md).

## C1 — Fail fast; no fallbacks

A value any calculation consumes is never defaulted: a missing one is an error, not a zero. A
stand-in value is written in exactly two places — reading a stored preference, and parsing external
input at the point it enters — and external input is validated at that same point, before anything
computes with it. Nothing downstream of it supplies a value the input did not carry.

**Verdict / test:** no silent swallowed exceptions, no `or {}`. Grep the source for `?:`, `?.`,
`getOrDefault`, `.get(` with a second argument, `= null` as an initialiser, and every `catch` block
that returns a value; read every hit. A hit in a file that computes, rather than one that reads a
stored preference or parses input at its entry point, is a breach — and a constant a formula reads
is a breach wherever it carries a default.

## C2 — Single source of truth

One fact, one definition, in code as in docs.

**Verdict / test:** the value appears in exactly one file. Grep it across the repository; more than
one definition site is a breach, and the extras become links or references to the one that owns it.

## C3 — Keep files readable: 500 lines is a gate

Source files. Past 500 lines the file is split. The number moves only as a recorded decision — struck
through and replaced in place — and never gains a per-case exception.

**Verdict / test:** a line-count check that fails the build past 500 lines. *The check does not exist
yet; it is tracked in [`BACKLOG.md`](BACKLOG.md) and this constraint is unenforced until it lands.*

## C4 — Solve it structurally, not by stacking cases

A growing list of special cases means the structure is wrong.

**Verdict / test:** does the fix add a branch per case, or remove the need for one? A conditional
added per new input is a breach; read the diff before merging.

## C5 — Independent axes stay independent

Never derive one orthogonal dimension from another. A tendency is a cross-check, never a derivation
rule.

**Verdict / test:** one axis's value appearing in the expression that produces another's is a breach;
a check that writes, clamps, scales, or replaces the value it checks is a derivation whatever it is
named.

## C6 — Kill legacy on sight

Delete confirmed legacy in the session you find it, not in a follow-up.

**Verdict / test:** confirm three ways before deleting — production reality; an unfiltered grep with
every matching line read; running it on the machine it belongs on. Legacy left in place with a note
saying it is legacy is the breach.

## C7 — Small batch before the full run, and read the output

Read the rows actually written, not the count of them.

**Verdict / test:** the transcript shows a bounded run and its output read before the full run
started. A full run with no preceding sample is a breach.

## C8 — Zero callers is a question

*rot* — what it served is gone, so delete it with its orphans. *parked* — complete, target exists,
wiring missing, so keep it and record it. *tombstone* — a comment naming a deleted symbol, so fix the
comment and keep the code.

**Verdict / test:** a `parked` verdict names, in its `PARKED` row, both the target that exists and the
wiring that is missing; a row missing either name is rot, and the code is deleted. *The `PARKED` slot
is reserved and has no file yet — activating it is step one of the first `parked` verdict, per
[`PROCESS.md`](PROCESS.md) "Activate a reserved slot".*

## C9 — Verify the running thing

**Verdict / test:** the artifact, the endpoint, or the running code was exercised — not a build log,
and not a status readout describing what is on disk. For this project the running thing that counts
is a notification visible on the glasses, not one accepted by Android.
