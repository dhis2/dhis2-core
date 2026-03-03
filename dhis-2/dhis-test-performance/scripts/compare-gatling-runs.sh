#!/usr/bin/env bash
# Compares two Gatling simulation results and prints GitHub markdown tables.
#
# Usage:
#   ./compare-gatling-runs.sh <baseline-dir> <feature-dir>
#
# Requires: [gstat] (https://github.com/dhis2/gatling-statistics)
#           [glog] (https://github.com/dhis2/gatling/releases)
#
# Example:
#   ./compare-gatling-runs.sh \
#     target/gatling/usersperformancetest-20260217072013445 \
#     target/gatling/usersperformancetest-20260217073019128

set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <baseline-dir> <feature-dir>"
  exit 1
fi

command -v gstat &>/dev/null || {
  echo "Error: gstat not found. Install with: uv tool install git+https://github.com/dhis2/gatling-statistics" >&2
  exit 1
}

BASELINE_DIR="$1"
FEATURE_DIR="$2"

BTMP=$(mktemp)
FTMP=$(mktemp)
trap 'rm -f "$BTMP" "$FTMP"' EXIT

# gstat CSV columns:
#   directory,simulation,run_timestamp,request_name,count,min,50th,75th,95th,99th,max
#   1         2          3              4            5     6   7    8    9    10   11
gstat "$BASELINE_DIR" | tail -n +2 > "$BTMP"
gstat "$FEATURE_DIR"  | tail -n +2 > "$FTMP"

echo
echo "> Baseline: \`$(basename "$BASELINE_DIR")\`"
echo "> Feature:  \`$(basename "$FEATURE_DIR")\`"

print_table() {
  local col="$1"
  local heading="$2"

  echo
  echo "### ${heading} (ms)"
  echo
  echo "| Scenario | Baseline | Feature | Diff | Change |"
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
