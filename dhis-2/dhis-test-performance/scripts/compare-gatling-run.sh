#!/usr/bin/env bash
# Downloads artifacts from a GitHub Actions performance comparison run and
# prints a GitHub markdown comparison table.
#
# Usage:
#   ./compare-gatling-run.sh <run-id>
#
# Requires: [gh]    (https://cli.github.com)
#           [gstat] (https://github.com/dhis2/gatling-statistics)
#           [glog]  (https://github.com/dhis2/gatling/releases)
#
# Example:
#   ./compare-gatling-run.sh 24079806891

set -euo pipefail

REPO="dhis2/dhis2-core"

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <run-id>"
  exit 1
fi

RUN_ID="$1"

command -v gh    &>/dev/null || { echo "Error: gh not found"    >&2; exit 1; }
command -v gstat &>/dev/null || { echo "Error: gstat not found. Install with: uv tool install git+https://github.com/dhis2/gatling-statistics" >&2; exit 1; }

WORK_DIR=$(mktemp -d)
trap 'rm -rf "$WORK_DIR"' EXIT

echo "Downloading artifacts for run ${RUN_ID}..." >&2
gh run download "$RUN_ID" --repo "$REPO" --dir "$WORK_DIR" 2>&1 >&2

# Find the non-warmup baseline and candidate result directories
BASELINE_DIR=$(find "$WORK_DIR" -maxdepth 2 -type d -name '*-baseline' | sort | tail -n1)
CANDIDATE_DIR=$(find "$WORK_DIR" -maxdepth 2 -type d -name '*-candidate' | sort | tail -n1)

if [[ -z "$BASELINE_DIR" ]]; then
  echo "Error: could not find a baseline result directory in the downloaded artifacts" >&2
  exit 1
fi
if [[ -z "$CANDIDATE_DIR" ]]; then
  echo "Error: could not find a candidate result directory in the downloaded artifacts" >&2
  exit 1
fi

echo "Baseline:  $BASELINE_DIR" >&2
echo "Candidate: $CANDIDATE_DIR" >&2

BTMP=$(mktemp)
FTMP=$(mktemp)
trap 'rm -rf "$WORK_DIR"; rm -f "$BTMP" "$FTMP"' EXIT

# gstat CSV columns:
#   directory,simulation,run_timestamp,request_name,count,min,50th,75th,95th,99th,max
#   1         2          3              4            5     6   7    8    9    10   11
gstat "$BASELINE_DIR"  | tail -n +2 > "$BTMP"
gstat "$CANDIDATE_DIR" | tail -n +2 > "$FTMP"

echo
echo "> Baseline:  \`$(basename "$BASELINE_DIR")\`"
echo "> Candidate: \`$(basename "$CANDIDATE_DIR")\`"

print_table() {
  local col="$1"
  local heading="$2"

  echo
  echo "### ${heading} (ms)"
  echo
  echo "| Scenario | Baseline | Candidate | Diff | Change |"
  echo "|:---|---:|---:|---:|:---|"

  awk -F',' -v col="$col" '
    NR == FNR {
      if ($4 != "") baseline[$4] = $col + 0
      next
    }
    $4 != "" {
      name = $4
      bval = baseline[name] + 0
      fval = $col + 0
      diff = fval - bval
      if (bval > 0) {
        pct = (diff / bval) * 100
        pct_str = sprintf("%+.1f%%", pct)
        if      (diff < 0) change = ":arrow_down: " pct_str
        else if (diff > 0) change = ":arrow_up: "   pct_str
        else               change = pct_str
      } else {
        change = "N/A"
      }
      printf "| %s | %d | %d | %+d | %s |\n", name, bval, fval, diff, change
    }
  ' "$BTMP" "$FTMP"
}

print_table 7 "Median Response Time (p50)"
print_table 9 "95th Percentile Response Time (p95)"

echo
echo "_:arrow_down: = faster (improvement), :arrow_up: = slower (regression)_"
echo
