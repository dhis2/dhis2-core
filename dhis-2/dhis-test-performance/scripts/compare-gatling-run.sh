#!/usr/bin/env bash
# Downloads artifacts from a `performance-tests-compare.yml` run and renders a
# Markdown comparison table by delegating to `gstat compare`.
#
# Usage:
#   ./compare-gatling-run.sh <run-id>
#
# Requires: [gh]    (https://cli.github.com)
#           [gstat] (https://github.com/dhis2/gatling-statistics)
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
gh run download "$RUN_ID" --repo "$REPO" --dir "$WORK_DIR" >&2

BASELINE_DIR=$(find "$WORK_DIR" -maxdepth 2 -type d -name '*-baseline'  | sort | tail -n1)
CANDIDATE_DIR=$(find "$WORK_DIR" -maxdepth 2 -type d -name '*-candidate' | sort | tail -n1)

if [[ -z "$BASELINE_DIR" ]]; then
  echo "Error: could not find a *-baseline directory in the downloaded artifacts" >&2
  exit 1
fi
if [[ -z "$CANDIDATE_DIR" ]]; then
  echo "Error: could not find a *-candidate directory in the downloaded artifacts" >&2
  exit 1
fi

echo "Baseline:  $BASELINE_DIR" >&2
echo "Candidate: $CANDIDATE_DIR" >&2

gstat compare \
  "$BASELINE_DIR"  --label baseline \
  "$CANDIDATE_DIR" --label candidate \
  --exclude warmup
