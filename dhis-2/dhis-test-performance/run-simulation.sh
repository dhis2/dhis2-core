#!/bin/bash
# Run Gatling simulations against a DHIS2 instance running in Docker
#
# Usage: DHIS2_IMAGE=<docker-image-tag> SIMULATION_CLASS=<fully.qualified.ClassName> [DHIS2_DB_DUMP_URL=<url>] [MVN_ARGS=<args>] ./run-simulation.sh
# Example: DHIS2_IMAGE=dhis2/core-dev:local SIMULATION_CLASS=org.hisp.dhis.test.EnrollmentsTest MVN_ARGS="-DpageSize=100" ./run-simulation.sh
# Available Docker image tags: https://github.com/dhis2/dhis2-core/blob/master/docker/DOCKERHUB.md
set -euo pipefail

show_usage() {
  echo "Usage: DHIS2_IMAGE=<docker-image-tag> SIMULATION_CLASS=<fully.qualified.ClassName> [DHIS2_DB_DUMP_URL=<url>] $0"
  echo "Example: DHIS2_IMAGE=dhis2/core-dev:latest SIMULATION_CLASS=org.hisp.dhis.test.EnrollmentsTest $0"
  echo "Optional: DHIS2_DB_DUMP_URL defaults to https://databases.dhis2.org/sierra-leone/dev/dhis2-db-sierra-leone.sql.gz"
  echo "Available Docker image tags: https://github.com/dhis2/dhis2-core/blob/master/docker/DOCKERHUB.md"
}

if [ -z "${DHIS2_IMAGE:-}" ]; then
  echo "Error: DHIS2_IMAGE environment variable is required"
  show_usage
  exit 1
fi

if [ -z "${SIMULATION_CLASS:-}" ]; then
  echo "Error: SIMULATION_CLASS environment variable is required"
  show_usage
  exit 1
fi

MVN_ARGS=${MVN_ARGS:-""}
DHIS2_DB_DUMP_URL=${DHIS2_DB_DUMP_URL:-"https://databases.dhis2.org/sierra-leone/dev/dhis2-db-sierra-leone.sql.gz"}
DHIS2_DB_IMAGE_SUFFIX=${DHIS2_DB_IMAGE_SUFFIX:-"sierra-leone-dev"}
HEALTHCHECK_TIMEOUT=${HEALTHCHECK_TIMEOUT:-300} # default of 5min
HEALTHCHECK_INTERVAL=${HEALTHCHECK_INTERVAL:-10} # default of 10s

cleanup() {
  echo ""
  echo "Cleaning up..."
  docker compose down --volumes
}

trap cleanup EXIT INT

wait_for_health() {
  echo "Waiting for DHIS2 to start..."
  local start_time
  start_time=$(date +%s)

  while ! docker compose ps web-healthcheck | grep -q "healthy"; do
    sleep "$HEALTHCHECK_INTERVAL"
    echo "Still waiting..."
    if [ $(($(date +%s) - start_time)) -gt "$HEALTHCHECK_TIMEOUT" ]; then
      echo "Timeout waiting for DHIS2 to start"
      exit 1
    fi
  done
  echo "DHIS2 is ready! (took $(($(date +%s) - start_time))s)"
}

echo "Testing with image: $DHIS2_IMAGE"

docker compose down --volumes
docker compose up --detach

wait_for_health

# vacuum to get up to date PostgreSQL statistics
docker compose exec db psql -U dhis -c 'VACUUM;'

echo "Running $SIMULATION_CLASS..."
mvn gatling:test \
  -Dgatling.simulationClass="$SIMULATION_CLASS" \
  $MVN_ARGS

gatling_run_dir="target/gatling/$(cat target/gatling/lastRun.txt)"

# Create simulation run metadata file in key=value format
simulation_run_file="$gatling_run_dir/simulation-run.txt"
{
  echo "RUN_DIR=$gatling_run_dir"
  echo "COMMAND=DHIS2_IMAGE=$DHIS2_IMAGE DHIS2_DB_DUMP_URL=$DHIS2_DB_DUMP_URL SIMULATION_CLASS=$SIMULATION_CLASS${MVN_ARGS:+ MVN_ARGS=$MVN_ARGS}${HEALTHCHECK_TIMEOUT:+ HEALTHCHECK_TIMEOUT=$HEALTHCHECK_TIMEOUT}${HEALTHCHECK_INTERVAL:+ HEALTHCHECK_INTERVAL=$HEALTHCHECK_INTERVAL} $0"
  echo "SCRIPT_NAME=$0"
  echo "SCRIPT_ARGS=$*"
  echo "DHIS2_IMAGE=$DHIS2_IMAGE"
  echo "DHIS2_DB_DUMP_URL=$DHIS2_DB_DUMP_URL"
  echo "DHIS2_DB_IMAGE_SUFFIX=$DHIS2_DB_IMAGE_SUFFIX"
  echo "SIMULATION_CLASS=$SIMULATION_CLASS"
  echo "MVN_ARGS=$MVN_ARGS"
  echo "HEALTHCHECK_TIMEOUT=$HEALTHCHECK_TIMEOUT"
  echo "HEALTHCHECK_INTERVAL=$HEALTHCHECK_INTERVAL"
  echo "GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')"
  echo "GIT_COMMIT=$(git rev-parse HEAD 2>/dev/null || echo 'unknown')"
  echo "GIT_DIRTY=$([ -n "$(git status --porcelain 2>/dev/null)" ] && echo 'true' || echo 'false')"
} > "$simulation_run_file"

echo "Completed test for $DHIS2_IMAGE"
echo "Gatling test results are in: $gatling_run_dir"
echo "Gatling run metadata is in: $simulation_run_file"

