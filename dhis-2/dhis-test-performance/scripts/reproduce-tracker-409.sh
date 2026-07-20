#!/usr/bin/env bash
# Reproduce the intermittent tracker-import 409 CONFLICT storm seen in the
# "Performance tests compare" workflow (e.g. run 29728194277).
#
# The failure is environmental/flaky: on a bad run EVERY `POST /api/tracker`
# returns 409 for the whole lifetime of the DHIS2 container (warmup + measured
# run), which also empties the export scenarios (0 events). It is NOT tied to a
# specific code change - it has hit both the baseline and the candidate side.
#
# Gatling's simulation.log records only the status code, not the response body,
# so the actual import error is normally lost. logback-test.xml now enables the
# `io.gatling.http.engine.response` logger at DEBUG, which dumps the full
# request + response (including the 409 body) for every FAILED request. This
# script runs the LOAD profile in a loop and keeps the full console output of
# each iteration, stopping as soon as a run fails so the captured 409 body can
# be inspected.
#
# Usage:
#   [ITERATIONS=n] [DHIS2_IMAGE=tag] bash scripts/reproduce-tracker-409.sh
#
# Defaults: 20 iterations against dhis2/core-dev:master, LOAD profile.
set -uo pipefail

cd "$(dirname "$0")/.."

ITERATIONS=${ITERATIONS:-20}
LOG_DIR=${LOG_DIR:-"$(pwd)/target/repro-409"}
mkdir -p "$LOG_DIR"

# Env for run-simulation.sh. LOAD profile is what CI compare uses and what
# exhibits the storm (4 concurrent import users x 15 requests x 3 programs).
export DHIS2_IMAGE=${DHIS2_IMAGE:-dhis2/core-dev:master}
export SIMULATION_CLASS=org.hisp.dhis.test.tracker.TrackerTest
export DB_TYPE=${DB_TYPE:-sierra-leone}
export DB_VERSION=${DB_VERSION:-dev}
export WARMUP=${WARMUP:-1}
export MVN_ARGS=${MVN_ARGS:-"-Dprofile=load"}
export REPORT_SUFFIX=${REPORT_SUFFIX:-repro}
# Make sure failed-response bodies are dumped even if the default ever changes.
export GATLING_RESPONSE_LOG_LEVEL=${GATLING_RESPONSE_LOG_LEVEL:-DEBUG}

echo "Repro loop: $ITERATIONS iteration(s) of $SIMULATION_CLASS"
echo "  image=$DHIS2_IMAGE  MVN_ARGS=$MVN_ARGS"
echo "  logs -> $LOG_DIR"
echo

for i in $(seq 1 "$ITERATIONS"); do
  ts=$(date +%Y%m%d-%H%M%S)
  log="$LOG_DIR/iter-$(printf '%03d' "$i")-$ts.log"
  echo "===== iteration $i/$ITERATIONS ($ts) -> $log ====="
  # run-simulation.sh exits non-zero when the forAll(successfulRequests>=100%)
  # assertion fails, i.e. when any request (incl. the import 409s) is KO.
  ./run-simulation.sh >"$log" 2>&1
  rc=$?
  if [ $rc -ne 0 ]; then
    echo
    echo ">>> Reproduced on iteration $i (exit $rc). Full log: $log"
    echo ">>> First captured failed-request body (the 409 payload):"
    # The Gatling response logger prints a block starting with 'HTTP request:'
    # / 'HTTP response:' for each KO. Show the first response block.
    grep -n -A 40 "engine.response" "$log" | head -80 || true
    exit $rc
  fi
  echo "    iteration $i passed"
done

echo
echo "No failure reproduced in $ITERATIONS iteration(s). Increase ITERATIONS and retry."
