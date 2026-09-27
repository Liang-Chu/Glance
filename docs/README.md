# Documentation index

Read the current design and verification status before changing behavior.

| Document | Purpose |
| --- | --- |
| [Project README](../README.md) | Install, setup and backend quickstart |
| [DESIGN.md](DESIGN.md) | Current decisions and acceptance criteria; search its decision index first |
| [CONTRACT.md](CONTRACT.md) | Backend request, response, push and QR formats |
| [PROCESS.md](PROCESS.md) | Build, signing, setup, verification and contribution steps |
| [STATUS.md](STATUS.md) | Observed behavior and local verification evidence |
| [BACKLOG.md](BACKLOG.md) | The only list of open work |
| [ORIENTATION.md](ORIENTATION.md) | Source layout and entry points |
| [CONSTRAINTS.md](CONSTRAINTS.md) | Engineering gates and their checks |
| [G2 reference](reference/g2-notifications.md) | Device behavior observed outside this app |
| [Backend examples](../examples/test-backend/README.md) | Running the sample endpoints and FCM sender |
| [Agent entry](../CLAUDE.md) | Repository instructions for coding agents |

## Precedence

CONSTRAINTS → DESIGN → CONTRACT → REFERENCE → PROCESS → STATUS → code.
Keep code and its owning document consistent in the same change. User instructions take precedence.

## Lifecycle

- Keep only current, useful content. Git history preserves superseded material.
- Update an existing document before creating another. Add new documents to this index only when
  they have a distinct purpose; do not create empty templates, plans or status reports.
- Before deleting a document, move any surviving open work to BACKLOG and fix all inbound links.
- Keep release binaries, private configuration, logs and temporary notes under ignored `local/`,
  or outside the repository. Release descriptions belong on GitHub, not in another permanent MD file.
- `docs/PARKED.md` is reserved only for a confirmed complete-but-unwired component under C8.
  Create it through PROCESS's "Activate a reserved slot" procedure if that case actually occurs.
- Run `bash tools/check-docs.sh` after documentation cleanup.
