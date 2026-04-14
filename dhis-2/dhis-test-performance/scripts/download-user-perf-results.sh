#!/usr/bin/env bash
# Downloads gatling-report-users-load artifacts from the last N scheduled performance test runs.
#
# Usage:
#   ./download-user-perf-results.sh [--runs N] [--out-dir DIR]
#
# Defaults: 10 runs, output to ./gatling-downloads/
#
# Requires: gh CLI (authenticated), jq or python3

set -euo pipefail

RUNS=10
OUT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/gatling-downloads"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --runs) RUNS="$2"; shift 2 ;;
    --out-dir) OUT_DIR="$2"; shift 2 ;;
    *) echo "Unknown argument: $1" >&2; exit 1 ;;
  esac
done

mkdir -p "$OUT_DIR"

echo "Fetching last $RUNS runs from dhis2/dhis2-core performance-tests-scheduled.yml ..."

RUN_IDS=$(gh run list \
  --repo dhis2/dhis2-core \
  --workflow performance-tests-scheduled.yml \
  --limit "$RUNS" \
  --json databaseId,createdAt \
  | python3 -c "
import json, sys
runs = json.load(sys.stdin)
for r in runs:
    print(r['databaseId'], r['createdAt'])
")

echo "Run IDs (newest first):"
echo "$RUN_IDS"
echo

while IFS=' ' read -r run_id created_at; do
  date_tag="${created_at:0:10}"
  artifact_name="gatling-report-users-load-${run_id}-attempt-1"
  dest="$OUT_DIR/${date_tag}_${run_id}"

  if [[ -d "$dest" ]]; then
    echo "[$date_tag] $run_id — already downloaded, skipping."
    continue
  fi

  echo -n "[$date_tag] $run_id — downloading... "

  # Check artifact exists before trying to download
  count=$(gh api "repos/dhis2/dhis2-core/actions/runs/${run_id}/artifacts" \
    | python3 -c "
import json, sys
d = json.load(sys.stdin)
matches = [a for a in d.get('artifacts', []) if a['name'] == '${artifact_name}']
print(len(matches))
")

  if [[ "$count" == "0" ]]; then
    echo "artifact not found (skipping)."
    continue
  fi

  mkdir -p "$dest"
  if gh run download \
      --repo dhis2/dhis2-core \
      "$run_id" \
      --name "$artifact_name" \
      --dir "$dest" 2>/dev/null; then
    echo "done."
  else
    rmdir "$dest" 2>/dev/null || true
    echo "download failed (skipping)."
  fi
done <<< "$RUN_IDS"

echo
echo "Downloads complete. Results in: $OUT_DIR"
echo "Run analyze-user-perf-results.py to compute metrics."