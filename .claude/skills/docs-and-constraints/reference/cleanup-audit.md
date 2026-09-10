# The seven audit scans

Run from the repo root **on an event, never on a calendar** — before a release, a rebuild, or a
migration; after a rename or a large merge; whenever a document turned out to have lied to someone.
Variables mirror `SKILL.md` §0; if a path differs, change it in `PREFERENCES` first.

```bash
DOCS=docs                  # documentation root
SRC=src                    # SRC
RET=$DOCS/retired          # RETIRED
ROOT_README=README.md      # ROOT_README (outside $DOCS)
AGENT_ENTRY=CLAUDE.md      # AGENT_ENTRY (outside $DOCS)
OUTSIDE="$ROOT_README $AGENT_ENTRY"          # the two routing files; they own nothing
LIVE="$DOCS $OUTSIDE"      # pass this wherever a scan reads live documents
```

**Before a rename, a split, a move, or a retirement, grep the whole workspace** — source comments,
tests, and sibling repositories cite documents by name. Grep the old path and the old filename:

```bash
grep -rn "<old-doc-filename>" .. --include=*.py --include=*.kt --include=*.ts --include=*.js --include=*.md | grep -v node_modules
```

Zero hits is a verdict only when every repository that could cite this one is checked out beside
it. A document a test names as its acceptance source is a live contract — do not retire it. On a
rename, a split, or a move, every hit is repointed — at the new path, and for a split at the file
that now holds the cited section — in the same commit; re-run the grep afterwards and it returns
nothing.

Before deleting anything, confirm three ways: in production; by unfiltered grep with every matching
line read, counting only call sites; on the machine the thing belongs on.

One cleanup is one commit, and the message says why each document was retired.

---

## 1 · The subject is dead

Do the tables, modules, endpoints, and columns a document describes still exist? A document whose
whole subject is gone is retired whole, not fixed line by line.

```bash
if [ ! -d "$SRC" ]; then
  echo "NO SRC  $SRC does not exist — this scan produces no verdict"
else
  grep -ohE '`[a-z_][a-z0-9_]{3,}`' "$DOCS/<suspect>.md" | tr -d '`' | sort -u | while read -r id; do
    printf '%3d  %s\n' "$(grep -rl "\b$id\b" "$SRC" 2>/dev/null | wc -l)" "$id"
  done | sort -n
fi
```

Read the counts only when the guard printed nothing. Zero hits is dead. Non-zero: read every
matching line and count only call sites.

## 2 · The code map, finished plans, stale procedures

**Run this only in a cleanup sweep, never as a daily gate.**

`ORIENTATION` is verified against the tree, both directions. A directory it names must exist or
carry an explicit "does not exist yet" marker **on the same line as the name**; a source directory
on disk must appear in the doc.

```bash
DOC=$DOCS/ORIENTATION.md
if [ ! -f "$DOC" ]; then
  echo "NO DOC  $DOC does not exist — this scan produces no verdict"
else
  grep -oE '^[[:space:]]*[A-Za-z_][A-Za-z_0-9./-]*/' "$DOC" | tr -d ' ' | sort -u | while read -r d; do
    b=${d%/}; b=${b##*/}
    find . -type d -name "$b" -not -path "*/node_modules/*" -not -path "*/build/*" | grep -q .     || grep -q "$b/.*DOES NOT EXIST" "$DOC" || echo "PHANTOM  $b"
  done
  ls -d */ | grep -v node_modules | while read -r d; do
    grep -q "${d%/}" "$DOC" || echo "UNMAPPED $d"
  done
fi
```

`PHANTOM`: delete the name, or mark it "does not exist yet" on the same line as the name.
`UNMAPPED`: add a line for that directory saying what it is responsible for.

Then the plans and procedures:

```bash
ls $DOCS/*PLAN*.md $DOCS/*PROGRESS*.md $DOCS/*REPORT*.md $DOCS/*REVIEW*.md 2>/dev/null
grep -rn '^\s*[-*] *\(\[ \]\|🔴\|🟠\|⚪\)' $DOCS/*PLAN*.md 2>/dev/null
grep -rln 'retire when done\|superseded by\|expires' $DOCS/*.md
```

A plan expires when it is finished: hoist the unfinished into `BACKLOG`, then retire it. Honour any
expiry condition the header declares.

For `PROCESS`: run the commands it documents, or check each path and flag still exists. A step that
no longer reflects reality is a defect — fix or delete it, do not annotate it.

## 3 · A second source of truth

Check these four by name before hunting generally:

| Collision | Right answer |
| --- | --- |
| a decision-index row that paraphrases its section | the row locates, the section states |
| a measurement write-up in both `MEASUREMENTS/<campaign>/` and `REPORT.md` | with the data set |
| a broken constraint recorded in both `CONSTRAINTS` and an ADR | the dated breach row is the record |
| a contract clause restated in the consumer repo | the owner holds it; the consumer links |


Then hunt generally: the same closed vocabulary, enum, threshold, or schema written in more than one
place.

```bash
TERM='<an enum value or threshold that should have exactly one definition>'
grep -rn "$TERM" $LIVE $SRC 2>/dev/null | cut -d: -f1 | sort | uniq -c | sort -rn | head
```

Keep one authority; the rest become one-line links. If the runtime value and the documented value
disagree, that is a code defect — do not edit the doc to match.

## 4 · Reconcile `STATUS`, `ROOT_README`, and the backlog

**Check `$ROOT_README` against `STATUS` first**: every claim it makes about what the product does
has a `STATUS` row saying that is true today. Read both yourself, side by side.

**A `STATUS` row is suspect when what it describes has moved.** Ask git:

```bash
# for each row: has its path changed since the row was verified?
git log --oneline --since=<row's verification date> -- <the path that row covers>
```

Empty output means the row still holds. Non-empty means re-run the row's stated verification. A row
that cannot be re-verified is **deleted**, not annotated. A row with no single command that
re-verifies it has no verification method — give it one or delete it.

```bash
for f in $DOCS/STATUS.md $DOCS/BACKLOG.md; do
  [ -f "$f" ] || echo "MISSING  $f — the checks below produce no verdict"
done
grep -c "%" $DOCS/STATUS.md   # percentages: should be 0
```

Then the backlog:

```bash
grep -n '🔴\|🟡\|\[ \]' $DOCS/BACKLOG.md
```

- Already fixed → delete (no ✅).
- 🟡 not waiting on a person → make it a normal item.
- Missing "blocked on / done when" → write it or drop it.

## 4b · Nothing may cite the scratchpad

A citation is a markdown link into `WIP.md`, or a claim attributed to it. The second `grep -v`
exempts the row that *declares* the slot — a table row whose first cell is the slot name, in
`ENTRY`'s map or in `PREFERENCES`.

```bash
# a live document citing the scratchpad: a link into it, or a claim sourced to it
grep -rnE '\]\([^)]*WIP[^)]*\)|\b(per|see|from|in|of|decided in|agreed in|according to)\b[^.]{0,24}`?WIP`?\b' $LIVE --include=*.md \
  | grep -vE ':[0-9]+:\| *`?WIP`?(\.md)?`? *\|' \
  | grep -v "^$DOCS/WIP.md"
```

Every hit is a defect: move what the citing document depends on into the slot that owns it, then
repoint the citation there.

Hoist what survives from an abandoned thread into `BACKLOG`; the person who abandoned it says what
survives.

## 5 · Link and rename hygiene

**A rename, a split, or a move is finished when the targets resolve, every link label naming the old
file is updated, every plain-text mention of it is updated, and the grep under "Before a rename, a
split, a move, or a retirement, grep the whole workspace" returns nothing.**

```bash
# a) link label naming a file that is not what the link points at
grep -rnoE '\[`?[A-Za-z_0-9]+\.md`?\]\([^)]*\)' $LIVE --include=*.md | grep -v "^$RET/"   | awk -F'[][()]' '{ l=$2; gsub(/`/,"",l); n=split($4,p,"/"); if (l != p[n]) print }'

# b) plain-text mentions of a renamed file, invisible to every link scan
for old in <OLD_NAME>.md <OLD_NAME>.md; do
  grep -rn "$old" $LIVE --include=*.md | grep -v "^$RET/" | grep -v "]($old"
done
```

### Link targets

```bash
# live docs linking into the retirement area (only its index may be linked)
grep -rnoE '\]\([^)]*retired/[^)]*\)' $LIVE --include=*.md \
  | grep -v "^$RET/" | grep -v 'retired/INDEX.md'

# dead links, resolved against the containing file; stale links inside retired/ are expected
grep -rEo '\]\([^)#h][^)]*\.md[^)]*\)' $LIVE --include=*.md \
  | grep -v "^$RET/" \
  | sed 's/](/\t/; s/)$//' \
  | while IFS="$(printf '\t')" read -r f p; do
      d=$(dirname "$f"); t="${p%%#*}"
      [ -e "$d/$t" ] || echo "DEAD  $f  ->  $p"
    done
```

A markdown link into `$RET` is the violation: repoint it at `retired/INDEX.md`, or at the live
document that replaced it. Prose that names a retired file is left alone.

### Citations by position

```bash
# a) a citation naming a position instead of a heading or an id
grep -rnEi '(§ ?[0-9]|\b(section|acceptance|criteri(on|a)|rows?|items?|bullets?|points?|steps?) ?#? ?[0-9]|\bthe (first|second|third|fourth|fifth|sixth|seventh|last) (criterion|row|item|bullet|point))' $LIVE --include=*.md | grep -v "^$RET/"

# b) a quoted heading that does not match the file it cites
grep -rnoE '\]\([^)]*\.md\)[^"]{0,24}"[^"]+"' $LIVE --include=*.md | grep -v "^$RET/" \
  | sed -E 's/^([^:]+):[0-9]+:\]\(([^")]*)\)[^"]*"(.+)"$/\1\t\2\t\3/' \
  | while IFS="$(printf '\t')" read -r f p h; do
      d=$(dirname "$f"); t="${p%%#*}"
      [ -e "$d/$t" ] || continue   # a dead link: the check above owns it
      grep -qF "$h" "$d/$t" || echo "BAD HEADING  $f  ->  $t  \"$h\""
    done
```

Read the hits from (a): one that points at a place inside a document is a defect — replace the
position with the heading quoted verbatim, or with the criterion's text or id. Every hit from (b) is
a defect: fix the citation, or restore the heading it was written against, in the same commit.

## 6 · Reconcile the decisions

```bash
git log --oneline -60 -- "$DOCS" | grep -iE 'decid|ruling|rule|ADR'
git log -S'<a decision keyword that should still be present>' --oneline -- "$DOCS"
```

**Check the decision-index row first.** Reconcile the two directions: every ruling recorded in
`DESIGN` in this window has a row, and every row still points somewhere.

Per decision: is there a decision-index row; is the replaced concept gone from code, columns, config,
prompts, tests, seed data, and comments.

**Then check the strike in both directions, because only one of them used to be checked.** A strike
on the *most recent* overturn must still be there — a decision quietly dropped rather than recorded
is the older failure. A strike on anything *older than that* must be gone: once a section carries two
overturns, the oldest is deleted, and a strike whose replaced concept has left the code and holds no
open `BACKLOG` item is spent and goes with it. **Deleting a spent strike is the system working, not a
lost decision** — `git log` is the permanent record and this file is not.

Look for decisions **dropped rather than overturned**: run `git log -S` on each keyword that should
still be present.

---

## 7 · Accretion — what the documents have stopped being able to drop

**The failure this scan exists for: every other scan asks whether a document is *wrong*. A document
can be entirely true and still be unusable, because nobody ever deleted anything from it.** Struck
rulings pile up, warnings outlive the things they warned about, and the file grows monotonically
until the cost of reading it exceeds the cost of re-deriving it from the code — at which point the
documentation has quietly stopped working while passing every correctness check.

```bash
# a) prose documents past DOC_MAX — a prompt to read the file whole, never to split it
find $DOCS -name '*.md' -not -path "$RET/*" -exec awk 'END{if(NR>400) printf "%5d  %s\n", NR, FILENAME}' {} \;

# b) where the strikes are, and where they have stacked
for f in $(find $DOCS -name '*.md' -not -path "$RET/*"); do
  n=$(grep -o '~~' "$f" | wc -l)
  [ "$n" -gt 2 ] && printf "%3d strikes  %s\n" "$((n/2))" "$f"
done | sort -rn

# c) sections carrying more than one overturn — the oldest must go
grep -rn '~~.*~~.*~~\|~~[^~]*~~[^~]*→[^~]*~~' $DOCS --include=*.md

# d) chained history written as prose rather than as a strike
grep -rniE 'then .*, then |used to |formerly |previously |no longer (called|named)|it showed .*, then ' $LIVE --include=*.md

# e) prohibitions whose subject may already be gone — check each named symbol against $SRC
grep -rnoiE '(do not|never|must not|do not ever) [a-z ]{0,24}`[a-z_][a-z0-9_]{2,}`' $LIVE --include=*.md
```

**(a)** is a trigger to re-read, not a verdict: a long document every line of which is currently true
is a long document, and nothing is deleted to hit a number.

**(b)** ranks the files by how much history they are carrying. The top of that list is where the
accretion is, and it is usually the document covering whatever was hardest to get right — the same
reason it was overturned most is the reason it is now least readable.

**(c)** and **(d)** find the stacking directly. A strike inside a strike, or "it showed X, then Y,
then Z", is a changelog living in a design document. Cut it to the current ruling plus at most the
one it replaced.

**(e)** lists every prohibition naming a code symbol. Run each symbol against `$SRC` — the scan in
§1 does exactly this — and **a prohibition whose subject returns zero hits is deleted**, because it
now costs every reader the time to work out what it was protecting and prevents nothing. If the
mistake is genuinely repeatable in a way the code cannot show, it is not a warning in prose: it is a
constraint with a verdict, and it moves to `CONSTRAINTS`.

**A compaction commit changes no ruling.** If removing something would change what the project has
decided, it is not compaction — stop, and record it as a decision instead. The commit message says
what was dropped and why it was spent, so the deletion is itself reviewable.

## Done when

- `DESIGN` holds design only; `PROCESS` holds procedures that currently run.
- `ORIENTATION` names no phantom directory, and no top-level source directory is missing from it.
- No prose doc holds a section whose subject falls outside its title; no doc outside `BACKLOG`
  keeps a list of open items.
- Every live doc is reachable from `ENTRY`.
- Every `RETIRED/INDEX.md` row states why and where the live truth is.
- `BACKLOG` has no completed items; every 🟡 waits on a person.
- No section carries more than one overturn, and no strike outlives the concept it replaced.
- No prohibition names something that is gone from the code.
- `ROOT_README` and `AGENT_ENTRY` state what the project is and where to go, and no changeable fact.
- Every document is one somebody would rather read than re-derive from the source.
- Every `STATUS` row was re-verified this sweep, or deleted.
- No fact lives only in `REPORT.md`, and nothing — document, code, or test — cites it.
- No live document cites `WIP`; any stalled thread has been hoisted into `BACKLOG`.
- Both link-and-rename commands return nothing, and no link label or prose mention names a renamed
  file.
- The workspace-wide grep returns nothing for the old name of every document renamed, split, moved,
  or retired in this sweep.
- No citation names a position; every quoted heading matches its target verbatim.
- Every claim in `ROOT_README` about what the product does is still true per `STATUS` and `DESIGN`.
- Every scan that reads live documents ran over `$LIVE`, and no scan reported a missing input in
  place of a verdict.
