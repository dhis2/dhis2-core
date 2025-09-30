#!/bin/bash
# Run Gatling simulations against a DHIS2 instance running in Docker
set -euo pipefail

show_usage() {
  echo ""
  echo "USAGE:"
  echo "  DHIS2_IMAGE=<tag> SIMULATION_CLASS=<class> [OPTIONS] $0"
  echo ""
  echo "REQUIRED:"
  echo "  DHIS2_IMAGE           Docker image tag for DHIS2"
  echo "                        Available tags: https://github.com/dhis2/dhis2-core/blob/master/docker/DOCKERHUB.md"
  echo "  SIMULATION_CLASS      Fully qualified Gatling Simulation class name"
  echo ""
  echo "OPTIONS:"
  echo "  DHIS2_DB_DUMP_URL     Database dump URL"
  echo "                        Available database dumps: https://databases.dhis2.org"
  echo "                        Default: https://databases.dhis2.org/sierra-leone/dev/dhis2-db-sierra-leone.sql.gz"
  echo "  DHIS2_DB_IMAGE_SUFFIX Docker image suffix for DB (default: sierra-leone-dev)"
  echo "                        WARNING: Must match the version in DHIS2_DB_DUMP_URL"
  echo "  PROF_ARGS             Async-profiler arguments (enables profiling)"
  echo "                        Options: https://github.com/async-profiler/async-profiler/blob/master/docs/ProfilerOptions.md"
  echo "  MVN_ARGS              Additional Maven arguments passed to mvn gatling:test"
  echo "  HEALTHCHECK_TIMEOUT   Max wait time for DHIS2 startup in seconds (default: 300)"
  echo "  HEALTHCHECK_INTERVAL  Check interval for DHIS2 startup in seconds (default: 10)"
  echo ""
  echo "EXAMPLES:"
  echo "  # Basic test run"
  echo "  DHIS2_IMAGE=dhis2/core-dev:latest \\"
  echo "  SIMULATION_CLASS=org.hisp.dhis.test.tracker.TrackerTest $0"
  echo ""
  echo "  # With CPU profiling"
  echo "  PROF_ARGS=\"-e cpu\" \\"
  echo "  DHIS2_IMAGE=dhis2/core-dev:latest \\"
  echo "  SIMULATION_CLASS=org.hisp.dhis.test.tracker.TrackerTest $0"
  echo ""
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
PROF_ARGS=${PROF_ARGS:=""}

parse_prof_args() {
  if [ -z "$PROF_ARGS" ]; then
    EVENT_FLAG=""
    THREAD_FLAG=""
    return 0
  fi

  [[ $PROF_ARGS =~ -e[[:space:]]+([^[:space:]]+) ]] || return 1
  EVENT_FLAG="${BASH_REMATCH[1]}"

  if [[ $PROF_ARGS =~ -t|--threads ]]; then
    THREAD_FLAG="threads"
  else
    THREAD_FLAG=""
  fi
}

parse_prof_args

cleanup() {
  echo ""
  echo "Cleaning up..."
  if [ -n "$PROF_ARGS" ]; then
    docker compose -f docker-compose.yml -f docker-compose.profile.yml down --volumes
  else
    docker compose down --volumes
  fi
}

trap cleanup EXIT INT

start_containers() {
  echo "Testing with image: $DHIS2_IMAGE"

  if [ -n "$PROF_ARGS" ]; then
    docker compose -f docker-compose.yml -f docker-compose.profile.yml down --volumes
    docker compose -f docker-compose.yml -f docker-compose.profile.yml up --detach
  else
    docker compose down --volumes
    docker compose up --detach
  fi
}

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

save_profiler_data() {
  local gatling_dir="$1"

  if [ -z "$PROF_ARGS" ]; then
    return 0
  fi

  echo "Saving profiler data..."

  docker compose cp web:/profiler-output/. "$gatling_dir/"

  echo "Profiler data saved to $gatling_dir"
}

post_process_profiler_data() {
  local gatling_dir="$1"

  if [ -z "$PROF_ARGS" ]; then
    return 0
  fi

  echo "Post-processing profiler data..."

  local jfrconv_flags="--${EVENT_FLAG}"
  if [[ -n "$THREAD_FLAG" ]]; then
    jfrconv_flags="$jfrconv_flags --${THREAD_FLAG}"
  fi
  if [[ "$EVENT_FLAG" == "alloc" || "$EVENT_FLAG" == "lock" ]]; then
    jfrconv_flags="$jfrconv_flags --total"
  fi

  local title="$SIMULATION_CLASS on $DHIS2_IMAGE (async-profiler $PROF_ARGS)"
  # generate flamegraph and collapsed stack traces using jfrconv from async-profiler
  docker compose exec --workdir /profiler-output web \
    jfrconv "$jfrconv_flags" --dot --title "$title" profile.jfr profile.html
  docker compose exec --workdir /profiler-output web \
    jfrconv "$jfrconv_flags" --dot profile.jfr profile.collapsed

  docker compose cp web:/profiler-output/. "$gatling_dir/"

  echo "Post-processing profiler data complete. Files saved to $gatling_dir"
}

prepare_database() {
  echo "Preparing database..."
  docker compose exec db psql -U dhis -c 'VACUUM;'
}

start_profiler() {
  if [ -n "$PROF_ARGS" ]; then
    docker compose exec --workdir /profiler-output web asprof start $PROF_ARGS -f profile.jfr 1 > /dev/null
  fi
}

run_simulation() {
  echo "Running $SIMULATION_CLASS..."
  mvn gatling:test \
    -Dgatling.simulationClass="$SIMULATION_CLASS" \
    $MVN_ARGS
}

stop_profiler() {
  if [ -n "$PROF_ARGS" ]; then
    echo "Stopping profiler..."
    docker compose exec web asprof stop 1 > /dev/null
  fi
}

generate_metadata() {
  local gatling_run_dir="$1"
  local simulation_run_file="$gatling_run_dir/simulation-run.txt"

  echo "Generating run metadata..."
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
    echo "GIT_DIRTY=\$([ -n \"\$(git status --porcelain 2>/dev/null)\" ] && echo 'true' || echo 'false')"
  } > "$simulation_run_file"
}

start_containers
wait_for_health
prepare_database
start_profiler
run_simulation
stop_profiler

gatling_run_dir="target/gatling/$(head -n 1 target/gatling/lastRun.txt)"
save_profiler_data "$gatling_run_dir"
post_process_profiler_data "$gatling_run_dir"
generate_metadata "$gatling_run_dir"

echo "Completed test for $DHIS2_IMAGE"
echo "Gatling test results are in: $gatling_run_dir"
echo "Gatling run metadata is in: $gatling_run_dir/simulation-run.txt"

