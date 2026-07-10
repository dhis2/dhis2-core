#!/usr/bin/env bash
# A/B protocol for the API ETag cache (PageLoadSimulation).
#
# Runs the same simulation with cache.api.etag.enabled on vs off against identical
# Docker images + Sierra Leone DB, so reviewers can reproduce hit-path win and miss-path tax.
#
# Requires: Docker, Maven, a build of this branch as DHIS2_IMAGE.
# Optional: gstat (https://github.com/dhis2/gatling-statistics) for markdown compare tables.
#
# Usage (from dhis-test-performance/):
#   DHIS2_IMAGE=dhis2/core-pr:local ./scripts/etag-ab-benchmark.sh
#
# Environment (all optional unless noted):
#   DHIS2_IMAGE     required Docker image of the candidate build
#   PROFILE         smoke|load|capacity (default: smoke)
#   WARMUP          warmup runs per side (default: 1 for smoke, 2 for load)
#   MEASURED        measured runs per side after warmup (default: 1 for smoke, 3 for load)
#   FAST            true|false think-time (default: true for smoke, false for load)
#   CAPTURE_SQL     set non-empty to enable CAPTURE_SQL_LOGS on measured runs
#   SKIP_OFF        set non-empty to only run ON side (debug)
#   SKIP_ON         set non-empty to only run OFF side
#   MVN_EXTRA       extra -D flags for Gatling (appended)
#
# Results land under target/gatling/ and are summarized into target/etag-ab/<timestamp>/.
# Author: Morten Svanaes

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ -z "${DHIS2_IMAGE:-}" ]]; then
  echo "Error: DHIS2_IMAGE is required (Docker image of the build under test)" >&2
  exit 1
fi

SIMULATION_CLASS="${SIMULATION_CLASS:-org.hisp.dhis.test.cache.PageLoadSimulation}"
PROFILE="${PROFILE:-smoke}"
FAST="${FAST:-}"
WARMUP="${WARMUP:-}"
MEASURED="${MEASURED:-}"
CAPTURE_SQL="${CAPTURE_SQL:-}"
MVN_EXTRA="${MVN_EXTRA:-}"

case "$PROFILE" in
  smoke)
    : "${WARMUP:=1}"
    : "${MEASURED:=1}"
    : "${FAST:=true}"
    ;;
  load|capacity)
    : "${WARMUP:=2}"
    : "${MEASURED:=3}"
    : "${FAST:=false}"
    ;;
  *)
    echo "Error: PROFILE must be smoke|load|capacity, got: $PROFILE" >&2
    exit 1
    ;;
esac

STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
OUT_DIR="${OUT_DIR:-$ROOT/target/etag-ab/$STAMP}"
mkdir -p "$OUT_DIR"

log() { printf '%s %s\n' "$(date -u +%H:%M:%S)" "$*"; }

run_side() {
  local side="$1" # on|off
  local conf="docker/dhis-etag-${side}.conf"
  if [[ ! -f "$conf" ]]; then
    echo "Missing $conf" >&2
    exit 1
  fi

  local total=$((WARMUP + MEASURED))
  local i
  for i in $(seq 1 "$total"); do
    local is_warmup=0
    local suffix="etag-${side}-m${i}"
    if (( i <= WARMUP )); then
      is_warmup=1
      suffix="etag-${side}-warmup${i}"
    fi

    log "=== side=${side} run=${i}/${total} warmup=${is_warmup} conf=${conf} ==="

    local sql_flag=()
    if [[ -n "$CAPTURE_SQL" && "$is_warmup" -eq 0 ]]; then
      sql_flag=(CAPTURE_SQL_LOGS=1)
    fi

    # run-simulation always does its own internal warmups via WARMUP=; we want one container
    # lifecycle per measured sample, so set WARMUP=0 here and control warmups externally.
    env \
      DHIS2_IMAGE="$DHIS2_IMAGE" \
      SIMULATION_CLASS="$SIMULATION_CLASS" \
      DHIS_CONF_FILE="dhis-etag-${side}.conf" \
      WARMUP=0 \
      REPORT_SUFFIX="$suffix" \
      MVN_ARGS="-Dprofile=${PROFILE} -Dfast=${FAST} ${MVN_EXTRA}" \
      "${sql_flag[@]}" \
      ./run-simulation.sh | tee "$OUT_DIR/${suffix}.console.log"

    # Copy latest matching gatling dir pointer
    local latest
    latest="$(find target/gatling -maxdepth 1 -type d -name "pageloadsimulation-*" | sort | tail -1 || true)"
    if [[ -n "$latest" ]]; then
      echo "$latest" >"$OUT_DIR/${suffix}.runpath"
      cp -a "$latest/run-simulation.env" "$OUT_DIR/${suffix}.run-simulation.env" 2>/dev/null || true
    fi
  done
}

meta() {
  {
    echo "stamp=$STAMP"
    echo "dhis2_image=$DHIS2_IMAGE"
    echo "simulation=$SIMULATION_CLASS"
    echo "profile=$PROFILE"
    echo "fast=$FAST"
    echo "warmup=$WARMUP"
    echo "measured=$MEASURED"
    echo "git_head=$(git -C "$ROOT/../.." rev-parse HEAD 2>/dev/null || git -C "$ROOT/.." rev-parse HEAD 2>/dev/null || echo unknown)"
    echo "host=$(hostname)"
    echo "date_utc=$(date -u -Iseconds)"
  } | tee "$OUT_DIR/meta.env"
}

meta

if [[ -z "${SKIP_OFF:-}" ]]; then
  run_side off
fi
if [[ -z "${SKIP_ON:-}" ]]; then
  run_side on
fi

# Optional gstat compare of last measured dirs per side
if command -v gstat >/dev/null 2>&1; then
  off_path="$(cat "$OUT_DIR/etag-off-m1.runpath" 2>/dev/null || true)"
  on_path="$(cat "$OUT_DIR/etag-on-m1.runpath" 2>/dev/null || true)"
  if [[ -n "$off_path" && -n "$on_path" && -d "$off_path" && -d "$on_path" ]]; then
    log "gstat compare OFF vs ON (first measured run each)"
    gstat compare "$off_path" "$on_path" | tee "$OUT_DIR/gstat-compare-m1.md" || true
  fi
fi

log "Done. Artifacts: $OUT_DIR"
log "Fill BENCHMARKS-etag.md from Gatling index.html + gstat-compare-m1.md"
