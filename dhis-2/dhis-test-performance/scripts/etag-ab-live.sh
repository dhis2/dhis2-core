#!/usr/bin/env bash
# Lightweight A/B against an already-running DHIS2 instance (no Docker stack).
# Intended for minibox / laptop where run-simulation.sh (16G web + 16G DB) does not fit.
#
# Operator must flip cache.api.etag.enabled in the server's dhis.conf and restart
# between sides, OR point INSTANCE_ON / INSTANCE_OFF at two different instances.
#
# Usage (from dhis-test-performance/):
#   INSTANCE=http://127.0.0.1:8280 SIDE=on  ./scripts/etag-ab-live.sh
#   INSTANCE=http://127.0.0.1:8280 SIDE=off ./scripts/etag-ab-live.sh
#
# Then compare with:
#   gstat compare target/gatling/<off-run> target/gatling/<on-run>
#
# Author: Morten Svanaes

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

INSTANCE="${INSTANCE:-http://127.0.0.1:8280}"
SIDE="${SIDE:-on}" # label only: on|off|miss|hit
PROFILE="${PROFILE:-smoke}"
FAST="${FAST:-true}"
APP_CYCLES="${APP_CYCLES:-}"
USERS="${USERS:-}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-district}"
API_VERSION="${API_VERSION:-44}"

case "$PROFILE" in
  smoke) : "${APP_CYCLES:=5}" ; : "${USERS:=1}" ;;
  load)  : "${APP_CYCLES:=0}" ; : "${USERS:=10}" ;;
  *) echo "PROFILE smoke|load" >&2; exit 1 ;;
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
log_probe="probe_etag=${etag_hdr:-none}"

STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
REPORT_NAME="pageload-etag-${SIDE}-${PROFILE}-${STAMP}"

echo "Running PageLoadSimulation side=$SIDE profile=$PROFILE cycles=$APP_CYCLES users=$USERS instance=$INSTANCE $log_probe"

MVN_ARGS=(
  -Dgatling.simulationClass=org.hisp.dhis.test.cache.PageLoadSimulation
  -Dinstance="$INSTANCE"
  -Dprofile="$PROFILE"
  -Dfast="$FAST"
  -DadminUser="$ADMIN_USER"
  -DadminPassword="$ADMIN_PASSWORD"
  -DapiVersion="$API_VERSION"
  -Dgatling.resultsFolder="target/gatling"
  -Dgatling.core.directory.results="target/gatling"
)

if [[ -n "$APP_CYCLES" && "$APP_CYCLES" != "0" ]]; then
  MVN_ARGS+=(-DappCycles="$APP_CYCLES")
fi
if [[ -n "$USERS" ]]; then
  MVN_ARGS+=(-DconcurrentUsers="$USERS")
fi

# Standalone module: build from this pom
mvn -q gatling:test "${MVN_ARGS[@]}"

latest="$(find target/gatling -maxdepth 1 -type d -name 'pageloadsimulation-*' | sort | tail -1)"
if [[ -n "$latest" ]]; then
  {
    echo "side=$SIDE"
    echo "instance=$INSTANCE"
    echo "profile=$PROFILE"
    echo "fast=$FAST"
    echo "appCycles=$APP_CYCLES"
    echo "users=$USERS"
    echo "probe_etag=${etag_hdr:-none}"
    echo "stamp=$STAMP"
    echo "run_dir=$latest"
    echo "date_utc=$(date -u -Iseconds)"
  } | tee "$latest/etag-ab-live.env"
  # Friendly symlink for compare
  ln -sfn "$(basename "$latest")" "target/gatling/etag-${SIDE}-latest"
  echo "Result: $latest"
  echo "Symlink: target/gatling/etag-${SIDE}-latest -> $(basename "$latest")"
fi
