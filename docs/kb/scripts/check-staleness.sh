#!/usr/bin/env bash
# Detect KB entries whose sources have changed since last_verified.commit.
# Usage:
#   check-staleness.sh                   # scan every entry under docs/kb/
#   check-staleness.sh docs/kb/foo.md    # scan a single entry
#
# Exit codes:
#   0  all entries up-to-date
#   1  one or more entries are stale
#   2  usage or parse error
#
# Dependencies: bash, git, awk, grep. No yq needed — frontmatter parsed inline.

set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

scan_file() {
  local entry="$1"
  [[ -f "$entry" ]] || { echo "not-found: $entry" >&2; return 2; }

  # Extract frontmatter between the first two --- markers.
  local fm
  fm="$(awk '/^---$/{n++; next} n==1{print} n==2{exit}' "$entry")"
  [[ -n "$fm" ]] || { echo "no-frontmatter: $entry" >&2; return 2; }

  local commit
  commit="$(awk '/^last_verified:/{in_lv=1; next} in_lv && /^  commit:/{gsub(/^  commit: */,""); gsub(/^["'\'']|["'\'']$/,""); print; exit}' <<<"$fm")"
  [[ -n "$commit" ]] || { echo "stale: $entry (no last_verified.commit set)"; return 1; }

  # Collect source paths: lines under `sources:` of form `  - path: <value>`.
  local -a paths=()
  while IFS= read -r path; do paths+=("$path"); done < <(
    awk '/^sources:/{in_s=1; next} in_s && /^[^ ]/{in_s=0} in_s && /^  - path:/{gsub(/^  - path: */,""); gsub(/^["'\'']|["'\'']$/,""); print}' <<<"$fm"
  )

  [[ ${#paths[@]} -gt 0 ]] || { echo "ok: $entry (no sources)"; return 0; }

  # Verify the commit exists locally.
  if ! git rev-parse --quiet --verify "$commit^{commit}" >/dev/null; then
    echo "stale: $entry (last_verified.commit $commit not found in repo)"
    return 1
  fi

  local changed=""
  for p in "${paths[@]}"; do
    if ! git cat-file -e "$commit:$p" 2>/dev/null; then
      changed+="  - source missing at $commit: $p"$'\n'
      continue
    fi
    if [[ -n "$(git log --oneline "$commit..HEAD" -- "$p")" ]]; then
      changed+="  - changed since $commit: $p"$'\n'
    fi
  done

  if [[ -n "$changed" ]]; then
    echo "stale: $entry"
    printf '%s' "$changed"
    return 1
  fi
  echo "ok: $entry"
  return 0
}

rc=0
if [[ $# -eq 0 ]]; then
  while IFS= read -r f; do
    scan_file "$f" || rc=1
  done < <(find docs/kb -type f -name '*.md' ! -path 'docs/kb/templates/*' ! -name 'AGENTS.md' ! -name 'README.md' ! -name 'INDEX.md' ! -name 'GLOSSARY.md' ! -name 'CHANGELOG.md')
else
  for f in "$@"; do scan_file "$f" || rc=1; done
fi
exit $rc
