#!/usr/bin/env bash
#
# The documentation checks, runnable. Run before a release, after a rename or a
# large merge, or whenever a document turned out to have told someone something
# untrue — not on a schedule, and not as part of ordinary work. Documents are
# kept correct by moving them in the same commit as the code; this catches what
# that misses.
#
#   bash tools/check-docs.sh
#
set -uo pipefail
cd "$(dirname "$0")/.."

DOCS=docs
LIVE="$DOCS README.md CLAUDE.md"
problems=0

report() {
  problems=$((problems + 1))
  printf '\n== %s ==\n%s\n' "$1" "$2"
}

# 1 · Links that go nowhere, resolved against the file that contains them.
dead=$(grep -rEo '\]\([^)#h][^)]*\.md[^)]*\)' $LIVE --include=*.md 2>/dev/null \
  | sed 's/](/\t/; s/)$//' \
  | while IFS="$(printf '\t')" read -r f p; do
      d=$(dirname "$f"); t="${p%%#*}"
      [ -e "$d/$t" ] || echo "  $f -> $p"
    done)
[ -n "$dead" ] && report "Dead links" "$dead"

# 2 · A link labelled as one file but pointing at another.
mislabelled=$(grep -rnoE '\[`?[A-Za-z_0-9]+\.md`?\]\([^)]*\)' $LIVE --include=*.md 2>/dev/null \
  | awk -F'[][()]' '{ l=$2; gsub(/`/,"",l); n=split($4,p,"/"); if (l != p[n]) print "  " $0 }')
[ -n "$mislabelled" ] && report "Link label does not match its target" "$mislabelled"

# 3 · Citations by position. A position is not a citation: name the heading, or
#     the acceptance criterion's id, so the reference survives an edit.
positional=$(grep -rnEi '(§ ?[0-9]|\b(section|acceptance|criteri(on|a)|rows?|items?|bullets?|steps?) ?#? ?[0-9]|\bthe (first|second|third|fourth|last) (criterion|row|item|bullet))' \
  $LIVE --include=*.md 2>/dev/null | sed 's/^/  /')
[ -n "$positional" ] && report "Citation names a position instead of a heading or an id" "$positional"

# 4 · A quoted heading that does not appear in the file it cites.
badheading=$(grep -rnoE '\]\([^)]*\.md\)[^"]{0,24}"[^"]+"' $LIVE --include=*.md 2>/dev/null \
  | sed -E 's/^([^:]+):[0-9]+:\]\(([^")]*)\)[^"]*"(.+)"$/\1\t\2\t\3/' \
  | while IFS="$(printf '\t')" read -r f p h; do
      d=$(dirname "$f"); t="${p%%#*}"
      [ -e "$d/$t" ] || continue
      grep -qF "$h" "$d/$t" || echo "  $f -> $t \"$h\""
    done)
[ -n "$badheading" ] && report "Quoted heading is not in the file it cites" "$badheading"

# 5 · STATUS states what is live, built-but-unwired, or absent. Never a portion.
if [ -f "$DOCS/STATUS.md" ]; then
  pct=$(grep -n "%" "$DOCS/STATUS.md" | sed 's/^/  /')
  [ -n "$pct" ] && report "STATUS uses a percentage; name what is live instead" "$pct"
else
  report "STATUS.md is missing" "  the checks that read it produced no verdict"
fi

# 6 · ORIENTATION against the tree, both directions. Checking only one of them
#     lets the code map name a directory that is not there, which reads as a
#     missing file to anyone who just cloned it.
if [ -f "$DOCS/ORIENTATION.md" ]; then
  unmapped=$(for d in */; do
      case "${d%/}" in .*|build) continue;; esac
      grep -q "${d%/}" "$DOCS/ORIENTATION.md" || echo "  ${d%/}/ is not named in ORIENTATION.md"
    done)
  [ -n "$unmapped" ] && report "A source directory is missing from the code map" "$unmapped"

  phantom=$(grep -oE '^[[:space:]]+\.?[A-Za-z_][A-Za-z_0-9.-]*/' "$DOCS/ORIENTATION.md"     | tr -d ' ' | sort -u     | while read -r d; do
        [ -d "${d%/}" ] || grep -q "${d}.*DOES NOT EXIST" "$DOCS/ORIENTATION.md"           || echo "  $d is named in ORIENTATION.md but is not in the repository"
      done)
  [ -n "$phantom" ] && report "The code map names a directory that does not exist" "$phantom"
fi

# 7 · A section may show the current ruling and at most the one it replaced.
stacked=$(grep -rn '~~.*~~.*~~' $DOCS --include=*.md 2>/dev/null | sed 's/^/  /')
[ -n "$stacked" ] && report "More than one overturn in a section; delete the oldest" "$stacked"

# 8 · Length is a prompt to re-read and compact, never to split. Not a failure.
long=$(find $DOCS -name '*.md' -exec awk 'END{if(NR>400) printf "  %5d  %s\n", NR, FILENAME}' {} \; 2>/dev/null)
[ -n "$long" ] && printf '\n== Over 400 lines: read whole and compact, do not split ==\n%s\n' "$long"

if [ "$problems" -eq 0 ]; then
  echo "Documentation checks passed."
else
  printf '\n%d check(s) found something. Nothing above is stylistic.\n' "$problems"
fi
exit "$problems"
