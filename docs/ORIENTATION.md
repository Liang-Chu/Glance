# Orientation — the shape of the codebase

*What lives where, and which way dependencies point. Read second, after the project README.
Why anything is this way is [`DESIGN.md`](DESIGN.md); what is built is [`STATUS.md`](STATUS.md).*

## Layout

```
<repo root>/
  docs/       the documentation set; docs/README.md is its only entrance
  .claude/    agent configuration: the skills this repository loads
```

There is no source directory yet. Where the application's code will live is settled together with
the stack, and this section gains a line for it in that same commit — the work is tracked in
[`BACKLOG.md`](BACKLOG.md).

## Dependencies

No code exists, so there is no dependency direction to state and nothing enforcing one. The first
source directory to land brings this section with it.

## Entry points

| | |
|---|---|
| build | does not exist yet — tracked in [`BACKLOG.md`](BACKLOG.md) |
| runs first | does not exist yet — tracked in [`BACKLOG.md`](BACKLOG.md) |
| the seam for tests | does not exist yet — tracked in [`BACKLOG.md`](BACKLOG.md) |
