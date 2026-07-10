#!/usr/bin/env bash
# A/B protocol for the API ETag cache (PageLoadSimulation).
#
# Runs the same simulation with cache.api.etag.enabled on vs off against identical
# Docker images + Sierra Leone DB, so reviewers can reproduce hit-path win and miss-path tax.
#
# Requires: Docker, Maven, a build of this branch as DHIS2_IMAGE.
# Optional: gstat (https://github.com/dhis2/gatling-statistics) for markdown compare tables.
# Optional: glog for simulation.csv (enables per-run percentile tables in results.md).
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

# First measured run uses this index (shared loop: warmups 1..WARMUP, then WARMUP+1..)
FIRST_MEASURED_INDEX=$((WARMUP + 1))

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

    log "=== side=${side} run=${i}/${total} warmup=${is_warmup} conf=${conf} etag.expect=${side} ==="

    local sql_flag=()
    if [[ -n "$CAPTURE_SQL" && "$is_warmup" -eq 0 ]]; then
      sql_flag=(CAPTURE_SQL_LOGS=1)
    fi

    # run-simulation always does its own internal warmups via WARMUP=; we want one container
    # lifecycle per measured sample, so set WARMUP=0 here and control warmups externally.
    # -Detag.expect=on|off enables 304-share assertions in PageLoadSimulation (default none).
    env \
      DHIS2_IMAGE="$DHIS2_IMAGE" \
      SIMULATION_CLASS="$SIMULATION_CLASS" \
      DHIS_CONF_FILE="dhis-etag-${side}.conf" \
      WARMUP=0 \
      REPORT_SUFFIX="$suffix" \
      MVN_ARGS="-Dprofile=${PROFILE} -Dfast=${FAST} -Detag.expect=${side} ${MVN_EXTRA}" \
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

# Resolve first measured runpath: prefer exact m$((WARMUP+1)), else newest etag-<side>-m*.runpath
first_measured_runpath() {
  local side="$1"
  local exact="$OUT_DIR/etag-${side}-m${FIRST_MEASURED_INDEX}.runpath"
  if [[ -f "$exact" ]]; then
    cat "$exact"
    return 0
  fi
  local newest
  newest="$(ls -1 "$OUT_DIR"/etag-"${side}"-m*.runpath 2>/dev/null | sort -V | tail -1 || true)"
  if [[ -n "$newest" && -f "$newest" ]]; then
    cat "$newest"
    return 0
  fi
  return 1
}

# Prefer RepoDigest (registry pin), else local image Id. Dependency-free (docker CLI only).
image_pin() {
  local ref="$1"
  if ! command -v docker >/dev/null 2>&1; then
    echo "unknown(no-docker)"
    return
  fi
  local digest id
  digest="$(docker image inspect --format '{{if .RepoDigests}}{{index .RepoDigests 0}}{{end}}' "$ref" 2>/dev/null || true)"
  if [[ -n "$digest" ]]; then
    echo "$digest"
    return
  fi
  id="$(docker image inspect --format '{{.Id}}' "$ref" 2>/dev/null || true)"
  if [[ -n "$id" ]]; then
    echo "$id"
    return
  fi
  echo "unknown(image-not-local:$ref)"
}

resolve_git_pin() {
  local repo
  for repo in "$ROOT/../.." "$ROOT/.." "$ROOT"; do
    if git -C "$repo" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
      GIT_HEAD="$(git -C "$repo" rev-parse HEAD 2>/dev/null || echo unknown)"
      GIT_DIRTY_COUNT="$(git -C "$repo" status --porcelain 2>/dev/null | wc -l | tr -d ' ')"
      if [[ "${GIT_DIRTY_COUNT:-0}" -gt 0 ]]; then
        GIT_DIRTY=yes
      else
        GIT_DIRTY=no
      fi
      GIT_REPO="$repo"
      return
    fi
  done
  GIT_HEAD=unknown
  GIT_DIRTY_COUNT=0
  GIT_DIRTY=unknown
  GIT_REPO=unknown
}

# Emit markdown tables from glog simulation.csv (if present) or note missing data.
# Writes $OUT_DIR/results.md so doc tables can be copied rather than hand-typed.
write_results_md() {
  local results="$OUT_DIR/results.md"
  {
    echo "# ETag A/B results"
    echo
    echo "- stamp: \`$STAMP\`"
    echo "- profile: \`$PROFILE\` fast=\`$FAST\`"
    echo "- warmup: $WARMUP measured: $MEASURED (first measured index m${FIRST_MEASURED_INDEX})"
    echo "- dhis2_image: \`$DHIS2_IMAGE\`"
    echo "- dhis2_image_pin: \`${DHIS2_IMAGE_PIN:-unknown}\`"
    echo "- db_image: \`${DB_IMAGE_REF:-unknown}\`"
    echo "- db_image_pin: \`${DB_IMAGE_PIN:-unknown}\`"
    echo "- git_head: \`${GIT_HEAD:-unknown}\`"
    echo "- git_dirty: \`${GIT_DIRTY:-unknown}\` (porcelain lines: ${GIT_DIRTY_COUNT:-0})"
    echo "- method: per-run OK latencies from \`simulation.csv\` (glog) when present; else runpath only"
    echo
  } >"$results"

  local side
  for side in off on; do
    echo "## Side \`$side\` (measured runs)" >>"$results"
    echo >>"$results"
    echo "| run | n | mean | p50 | p95 | runpath |" >>"$results"
    echo "|---|---:|---:|---:|---:|---|" >>"$results"

    local rp
    for rp in "$OUT_DIR"/etag-"${side}"-m*.runpath; do
      [[ -f "$rp" ]] || continue
      local run_label dir
      run_label="$(basename "$rp" .runpath)"
      dir="$(cat "$rp" 2>/dev/null || true)"
      if [[ -z "$dir" || ! -d "$dir" ]]; then
        echo "| \`$run_label\` | | | | | (missing dir) |" >>"$results"
        continue
      fi
      local csv="$dir/simulation.csv"
      if [[ -f "$csv" ]]; then
        python3 - "$csv" "$run_label" "$dir" >>"$results" <<'PY'
import csv, sys
from pathlib import Path

path, label, rundir = sys.argv[1], sys.argv[2], sys.argv[3]
times = []
with open(path, newline="", encoding="utf-8", errors="replace") as f:
    sample = f.read(4096)
    f.seek(0)
    # Prefer header-aware parse; fall back to last numeric column heuristics.
    try:
        reader = csv.DictReader(f)
        fields = reader.fieldnames or []
        # Common glog / Gatling CSV names
        candidates = [
            "responseTime",
            "Response Time",
            "response time",
            "latency",
            "OK",
            "responseTimeInMs",
        ]
        col = next((c for c in candidates if c in fields), None)
        status_col = next(
            (c for c in ("status", "Status", "responseCode", "Response Code") if c in fields),
            None,
        )
        if col is None:
            # last field that looks numeric in first row
            rows = list(reader)
            if not rows:
                print(f"| `{label}` | 0 | | | | `{Path(rundir).name}` |")
                raise SystemExit
            for k, v in rows[0].items():
                try:
                    float(v)
                    col = k
                except (TypeError, ValueError):
                    pass
            f.seek(0)
            reader = csv.DictReader(f)
        for row in reader:
            if status_col is not None:
                st = str(row.get(status_col, "")).strip()
                if st and st not in ("200", "304", "OK", "ok"):
                    # keep 200/304 only when status known
                    if st.isdigit() and int(st) not in (200, 304):
                        continue
            try:
                times.append(float(row[col]))
            except (KeyError, TypeError, ValueError):
                continue
    except SystemExit:
        raise
    except Exception:
        f.seek(0)
        for line in f:
            parts = line.strip().split(",")
            if len(parts) < 2:
                continue
            for token in reversed(parts):
                try:
                    times.append(float(token))
                    break
                except ValueError:
                    continue

if not times:
    print(f"| `{label}` | 0 | | | | `{Path(rundir).name}` (no times parsed) |")
else:
    times.sort()
    n = len(times)
    mean = sum(times) / n
    p50 = times[int(0.50 * (n - 1))]
    p95 = times[int(0.95 * (n - 1))]
    print(
        f"| `{label}` | {n} | {mean:.2f} | {p50:.1f} | {p95:.1f} | `{Path(rundir).name}` |"
    )
PY
      else
        echo "| \`$run_label\` | | | | | \`$(basename "$dir")\` (no simulation.csv) |" >>"$results"
      fi
    done
    echo >>"$results"
  done

  echo "## Notes" >>"$results"
  echo >>"$results"
  echo "- Canonical multi-run headline: pool measured \`simulation.csv\` OK samples per side (see BENCHMARKS-etag.md)." >>"$results"
  echo "- PageLoadSimulation uses \`-Detag.expect=on|off\` so a broken cache (no 304s on ON) fails the run." >>"$results"
  echo "- First measured suffix for this protocol: \`m${FIRST_MEASURED_INDEX}\` (not m1 when WARMUP>=1)." >>"$results"
  log "Wrote $results"
}

meta() {
  resolve_git_pin
  # Match docker-compose.yml defaults for the postgres service image.
  DB_TYPE="${DB_TYPE:-sierra-leone}"
  DB_VERSION="${DB_VERSION:-dev}"
  DB_IMAGE_REF="${DB_IMAGE_REF:-localhost/dhis2-postgres:14-3.5-${DB_TYPE}-${DB_VERSION}}"
  DHIS2_IMAGE_PIN="$(image_pin "$DHIS2_IMAGE")"
  DB_IMAGE_PIN="$(image_pin "$DB_IMAGE_REF")"

  {
    echo "stamp=$STAMP"
    echo "dhis2_image=$DHIS2_IMAGE"
    echo "dhis2_image_pin=$DHIS2_IMAGE_PIN"
    echo "db_image=$DB_IMAGE_REF"
    echo "db_image_pin=$DB_IMAGE_PIN"
    echo "simulation=$SIMULATION_CLASS"
    echo "profile=$PROFILE"
    echo "fast=$FAST"
    echo "warmup=$WARMUP"
    echo "measured=$MEASURED"
    echo "first_measured_index=$FIRST_MEASURED_INDEX"
    echo "git_head=$GIT_HEAD"
    echo "git_dirty=$GIT_DIRTY"
    echo "git_dirty_count=$GIT_DIRTY_COUNT"
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

write_results_md

# Optional gstat compare of first measured dirs per side (fixed m$((WARMUP+1)) index)
if command -v gstat >/dev/null 2>&1; then
  off_path="$(first_measured_runpath off || true)"
  on_path="$(first_measured_runpath on || true)"
  if [[ -n "${off_path:-}" && -n "${on_path:-}" && -d "$off_path" && -d "$on_path" ]]; then
    log "gstat compare OFF vs ON (first measured run each: m${FIRST_MEASURED_INDEX})"
    gstat compare "$off_path" "$on_path" | tee "$OUT_DIR/gstat-compare-m${FIRST_MEASURED_INDEX}.md" || true
  else
    log "gstat compare skipped (missing first measured runpath for one or both sides)"
  fi
fi

log "Done. Artifacts: $OUT_DIR"
log "See $OUT_DIR/results.md (and gstat-compare-m${FIRST_MEASURED_INDEX}.md if gstat ran)"
