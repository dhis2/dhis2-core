#!/usr/bin/env bash
# Metadata mutation benchmark driver for MetadataMutationSimulation.
# Measures the API ETag cache under concurrent metadata WRITES against an
# already-running DHIS2 instance (no Docker stack), companion to etag-ab-live.sh.
#
# Profiles (PROFILE env):
#   writeload - page-load readers + rate-controlled writers; loops over RATES
#               (writes/sec), one Gatling run per rate, and appends a CSV row
#               per run: rate,requests,rps,share304,p95ms,ko
#   staleness - after each mutation, probe with the pre-mutation ETag until 200;
#               asserts p99 attempts <= 2 (prompt invalidation)
#   writecost - writers only, unpaced; run once per cache.api.etag.enabled side
#               (SIDE=on|off) and compare the two labelled report dirs
#
# Usage (from dhis-test-performance/):
#   INSTANCE=http://127.0.0.1:8280 PROFILE=writeload ./scripts/etag-mutation-bench.sh
#   INSTANCE=http://127.0.0.1:8280 PROFILE=staleness ./scripts/etag-mutation-bench.sh
#   INSTANCE=http://127.0.0.1:8280 PROFILE=writecost SIDE=on  ./scripts/etag-mutation-bench.sh
#   # flip cache.api.etag.enabled=off in dhis.conf + restart, then:
#   INSTANCE=http://127.0.0.1:8280 PROFILE=writecost SIDE=off ./scripts/etag-mutation-bench.sh
#
# Author: Morten Svanaes

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

INSTANCE="${INSTANCE:-http://127.0.0.1:8280}"
SIDE="${SIDE:-on}" # label only: on|off
PROFILE="${PROFILE:-writeload}"
RATES="${RATES:-0 0.1 1 10}"
DURATION="${DURATION:-120}"
READERS="${READERS:-10}"
WRITERS="${WRITERS:-2}"
WRITE_TARGET="${WRITE_TARGET:-hot}"
POOL_SIZE="${POOL_SIZE:-200}"
WRITE_RATE="${WRITE_RATE:-1}" # staleness/single-run rate
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-district}"
API_VERSION="${API_VERSION:-44}"

case "$PROFILE" in
  writeload | staleness | writecost) ;;
  *)
    echo "PROFILE must be one of: writeload staleness writecost" >&2
    exit 1
    ;;
esac

# Sanity: instance up
code="$(curl -s -o /dev/null -w '%{http_code}' -u "$ADMIN_USER:$ADMIN_PASSWORD" "$INSTANCE/api/system/info" || true)"
if [[ "$code" != "200" ]]; then
  echo "Instance not ready at $INSTANCE (HTTP $code)" >&2
  exit 1
fi

# Probe whether ETags are present on a cached metadata list (rough side check)
probe="$(curl -sI -u "$ADMIN_USER:$ADMIN_PASSWORD" "$INSTANCE/api/dataElements?pageSize=1" | tr -d '\r' || true)"
etag_hdr="$(printf '%s\n' "$probe" | awk 'BEGIN{IGNORECASE=1} /^ETag:/{print $2; exit}')"
if [[ "$SIDE" == "on" && -z "$etag_hdr" && "$PROFILE" != "writecost" ]]; then
  echo "WARNING: no ETag header on $INSTANCE/api/dataElements - is cache.api.etag.enabled=on?" >&2
fi

STAMP="$(date -u +%Y%m%dT%H%M%SZ)"

base_mvn_args() {
  local rate="$1"
  MVN_ARGS=(
    -Dgatling.simulationClass=org.hisp.dhis.test.cache.MetadataMutationSimulation
    -Dinstance="$INSTANCE"
    -Dprofile="$PROFILE"
    -DwriteRate="$rate"
    -DwriteTarget="$WRITE_TARGET"
    -Dreaders="$READERS"
    -Dwriters="$WRITERS"
    -DdurationSec="$DURATION"
    -DpoolSize="$POOL_SIZE"
    -DadminUser="$ADMIN_USER"
    -DadminPassword="$ADMIN_PASSWORD"
    -DapiVersion="$API_VERSION"
    -Dgatling.resultsFolder="target/gatling"
    -Dgatling.core.directory.results="target/gatling"
  )
}

# Parse one run's console log into a CSV row: rate,requests,rps,share304,p95ms,ko
append_csv_row() {
  local rate="$1" logfile="$2" csv="$3"
  local requests rps share p95 ko
  requests="$(awk '/> request count/{gsub(/,/,"",$5); print $5; exit}' "$logfile")"
  rps="$(awk '/> mean throughput/{gsub(/,/,"",$6); print $6; exit}' "$logfile")"
  p95="$(awk '/response time 95th percentile/{gsub(/,/,"",$8); print $8; exit}' "$logfile")"
  # The Requests summary row is: > request count | total | ok | ko  ("-" when zero)
  ko="$(awk '/> request count/{gsub(/,/,"",$9); print $9; exit}' "$logfile")"
  if [[ -z "$ko" || "$ko" == "-" ]]; then ko=0; fi
  share="$(grep -oE 'share=[0-9.]+%' "$logfile" | tail -1 | tr -d 'share=%')"
  echo "$rate,${requests:-NA},${rps:-NA},${share:-NA},${p95:-NA},${ko}" >>"$csv"
}

run_once() {
  local rate="$1" label="$2"
  local logfile="target/gatling/mutation-bench-${label}-${STAMP}.log"
  mkdir -p target/gatling
  base_mvn_args "$rate"
  echo ">>> ${PROFILE} run: writeRate=${rate}/s writeTarget=${WRITE_TARGET} readers=${READERS} writers=${WRITERS} duration=${DURATION}s"
  # Do not fail the whole staircase if one run's assertion trips; the CSV shows it.
  if ! mvn -q gatling:test "${MVN_ARGS[@]}" 2>&1 | tee "$logfile"; then
    echo "WARNING: run writeRate=${rate} failed (assertion or error) - see $logfile" >&2
  fi
  local latest
  latest="$(find target/gatling -maxdepth 1 -type d -name 'metadatamutationsimulation-*' | sort | tail -1)"
  if [[ -n "$latest" ]]; then
    ln -sfn "$(basename "$latest")" "target/gatling/mutation-${PROFILE}-${label}-latest"
    echo "Report: $latest/index.html"
  fi
  LAST_LOG="$logfile"
}

case "$PROFILE" in
  writeload)
    CSV="target/gatling/mutation-bench-${STAMP}.csv"
    mkdir -p target/gatling
    echo "rate,requests,rps,share304,p95ms,ko" >"$CSV"
    for rate in $RATES; do
      run_once "$rate" "rate${rate}"
      append_csv_row "$rate" "$LAST_LOG" "$CSV"
    done
    echo ""
    echo "=== writeload staircase summary (writeTarget=${WRITE_TARGET}) ==="
    column -s, -t <"$CSV"
    echo "CSV: $CSV"
    ;;
  staleness)
    run_once "$WRITE_RATE" "staleness"
    echo ""
    echo "=== staleness summary ==="
    grep -E 'staleness (attempts distribution|p99)' "$LAST_LOG" || true
    ;;
  writecost)
    run_once "$WRITE_RATE" "side-${SIDE}"
    echo ""
    echo "=== writecost side=${SIDE} done ==="
    grep -E '> request count|> mean throughput|95th percentile' "$LAST_LOG" || true
    if [[ "$SIDE" == "on" ]]; then
      cat <<'EOT'
Next: flip cache.api.etag.enabled=off in the server's dhis.conf, restart DHIS2,
then rerun:  PROFILE=writecost SIDE=off ./scripts/etag-mutation-bench.sh
Compare the two report dirs:
  target/gatling/mutation-writecost-side-on-latest
  target/gatling/mutation-writecost-side-off-latest
The throughput/p95 delta between sides is the DML observer's write-path cost.
EOT
    fi
    ;;
esac
