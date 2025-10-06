#!/bin/bash
# Run Gatling simulations against a DHIS2 instance running in Docker

# Note: We don't use -e (exit on error) because Maven assertion failures are expected outcomes
# that we want to capture and analyze. Post-processing (logs, profiler data) must always run.
set -uo pipefail

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
  echo "  WARMUP                Number of warmup iterations before actual test (default: 0)"
  echo "  REPORT_SUFFIX         Suffix to append to Gatling report directory name (default: empty)"
  echo "  CAPTURE_SQL_LOGS      Capture and analyze SQL logs for non-warmup runs"
  echo "                        Set to any non-empty value to enable (default: disabled)"
  echo "                        Analysis requires pgbadger: https://github.com/darold/pgbadger"
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
  echo "  # With warmup and custom report suffix"
  echo "  WARMUP=1 \\"
  echo "  REPORT_SUFFIX=\"baseline\" \\"
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
PROF_ARGS=${PROF_ARGS:=""}
WARMUP=${WARMUP:-0}
REPORT_SUFFIX=${REPORT_SUFFIX:-""}
CAPTURE_SQL_LOGS=${CAPTURE_SQL_LOGS:-""}

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

pull_mutable_image() {
  # Pull images with mutable tags to ensure we get the latest version. See
  # https://github.com/dhis2/dhis2-core/blob/master/docker/DOCKERHUB.md for tag types. Mutable tags
  # (dhis2/core-dev:*, dhis2/core-pr:*) are overwritten multiple times a day. Immutable tags
  # (dhis2/core:2.42.1) are never rebuilt once published. Docker caches images locally, so without
  # an explicit pull, we may run an outdated version even when using tags like 'latest' or 'master'.
  # This is especially important on our self-hosted runner as devs will expect their latest change
  # to be tested.

  if [[ "$DHIS2_IMAGE" =~ ^dhis2/core-(dev|pr): ]]; then
    echo "Pulling mutable image tag: $DHIS2_IMAGE"
    docker pull "$DHIS2_IMAGE"
  fi
}

start_containers() {
  echo "Testing with image: $DHIS2_IMAGE"
  echo "Waiting for containers to be ready..."

  local start_time
  start_time=$(date +%s)

  if [ -n "$PROF_ARGS" ]; then
    docker compose -f docker-compose.yml -f docker-compose.profile.yml down --volumes
    if ! docker compose -f docker-compose.yml -f docker-compose.profile.yml up --wait --wait-timeout "$HEALTHCHECK_TIMEOUT"; then
      echo "Error: Failed to start containers"
      exit 1
    fi
  else
    docker compose down --volumes
    if ! docker compose up --detach --wait --wait-timeout "$HEALTHCHECK_TIMEOUT"; then
      echo "Error: Failed to start containers"
      exit 1
    fi
  fi

  echo "All containers ready! (took $(($(date +%s) - start_time))s)"
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

save_sql_logs() {
  local gatling_dir="$1"
  local warmup_num="${2:-0}"

  if [ -z "$CAPTURE_SQL_LOGS" ] || [ "$warmup_num" -gt 0 ]; then
    return 0
  fi

  echo ""
  echo "Saving SQL logs..."
  docker compose cp db:/var/lib/postgresql/data/log/postgresql.log "$gatling_dir/postgresql.log"
  echo "SQL logs saved to $gatling_dir"
  echo ""
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
  # shellcheck disable=SC2086
  docker compose exec --workdir /profiler-output web \
    jfrconv $jfrconv_flags --dot --title "$title" profile.jfr profile.html
  # shellcheck disable=SC2086
  docker compose exec --workdir /profiler-output web \
    jfrconv $jfrconv_flags --dot profile.jfr profile.collapsed

  docker compose cp web:/profiler-output/. "$gatling_dir/"

  echo "Post-processing profiler data complete. Files saved to $gatling_dir"
}

post_process_sql_logs() {
  local gatling_dir="$1"
  local warmup_num="${2:-0}"

  if [ -z "$CAPTURE_SQL_LOGS" ] || [ "$warmup_num" -gt 0 ]; then
    return 0
  fi

  if ! command -v pgbadger &> /dev/null; then
    echo ""
    echo "Warning: pgbadger not found in PATH. Skipping SQL log post-processing."
    echo "Install pgbadger to enable SQL log analysis: https://github.com/darold/pgbadger"
    echo ""
    return 0
  fi

  echo ""
  echo "Post-processing SQL logs..."
  pgbadger \
    --title "$SIMULATION_CLASS on $DHIS2_IMAGE" \
    --prefix '%t [%p]: user=%u,db=%d,app=%a ' \
    --dbname dhis \
    --outfile "$gatling_dir/pgbadger.html" "$gatling_dir/postgresql.log"
  echo "Post-processing SQL logs complete. File saved to $gatling_dir/pgbadger.html"
  echo ""
}

prepare_database() {
  echo "Preparing database..."
  docker compose exec db psql --username=dhis --quiet --command='VACUUM;' > /dev/null
}

start_profiler() {
  if [ -n "$PROF_ARGS" ]; then
    # shellcheck disable=SC2086
    docker compose exec --workdir /profiler-output web asprof start $PROF_ARGS -f profile.jfr 1 > /dev/null
  fi
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
    echo "COMMAND=DHIS2_IMAGE=$DHIS2_IMAGE DHIS2_DB_DUMP_URL=$DHIS2_DB_DUMP_URL DHIS2_DB_IMAGE_SUFFIX=$DHIS2_DB_IMAGE_SUFFIX SIMULATION_CLASS=$SIMULATION_CLASS${MVN_ARGS:+ MVN_ARGS=$MVN_ARGS}${PROF_ARGS:+ PROF_ARGS=$PROF_ARGS}${HEALTHCHECK_TIMEOUT:+ HEALTHCHECK_TIMEOUT=$HEALTHCHECK_TIMEOUT}${WARMUP:+ WARMUP=$WARMUP}${REPORT_SUFFIX:+ REPORT_SUFFIX=$REPORT_SUFFIX}${CAPTURE_SQL_LOGS:+ CAPTURE_SQL_LOGS=$CAPTURE_SQL_LOGS} $0"
    echo "SCRIPT_NAME=$0"
    echo "SCRIPT_ARGS=$*"
    echo "DHIS2_IMAGE=$DHIS2_IMAGE"
    echo "DHIS2_DB_DUMP_URL=$DHIS2_DB_DUMP_URL"
    echo "DHIS2_DB_IMAGE_SUFFIX=$DHIS2_DB_IMAGE_SUFFIX"
    echo "SIMULATION_CLASS=$SIMULATION_CLASS"
    echo "MVN_ARGS=$MVN_ARGS"
    echo "PROF_ARGS=$PROF_ARGS"
    echo "HEALTHCHECK_TIMEOUT=$HEALTHCHECK_TIMEOUT"
    echo "WARMUP=$WARMUP"
    echo "REPORT_SUFFIX=$REPORT_SUFFIX"
    echo "CAPTURE_SQL_LOGS=$CAPTURE_SQL_LOGS"
    echo "GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')"
    echo "GIT_COMMIT=$(git rev-parse HEAD 2>/dev/null || echo 'unknown')"
    echo "GIT_DIRTY=\$([ -n \"\$(git status --porcelain 2>/dev/null)\" ] && echo 'true' || echo 'false')"
  } > "$simulation_run_file"

  echo "Gatling run metadata is in: $gatling_run_dir/simulation-run.txt"
}

enable_sql_logs() {
  local warmup_num="${1:-0}"

  if [ -z "$CAPTURE_SQL_LOGS" ] || [ "$warmup_num" -gt 0 ]; then
    return 0
  fi

  # enable logging of all queries only on demand as it is expensive
  echo "Enabling SQL query logging..."
  docker compose exec db psql --username=dhis --quiet --command="ALTER SYSTEM SET log_min_duration_statement = 0;" > /dev/null
  docker compose exec db psql --username=dhis --quiet --command="SELECT pg_reload_conf();" > /dev/null

  # let postgres create a new log file so we only capture queries related to the test run
  docker compose exec db rm -f /var/lib/postgresql/data/log/postgresql.log
  docker compose exec db psql --username=dhis --quiet --command="SELECT pg_rotate_logfile();" > /dev/null
}

run_simulation() {
  local warmup_num="${1:-0}"
  local extra_mvn_args=""

  if [ "$warmup_num" -gt 0 ]; then
    extra_mvn_args="-Dgatling.failOnError=false"
  fi

  enable_sql_logs "$warmup_num"
  start_profiler

  echo "Running $SIMULATION_CLASS..."
  # shellcheck disable=SC2086
  mvn gatling:test \
    -Dgatling.simulationClass="$SIMULATION_CLASS" \
    $MVN_ARGS $extra_mvn_args

  stop_profiler
  gatling_run_dir="target/gatling/$(head -n 1 target/gatling/lastRun.txt)"

  # Build suffix from REPORT_SUFFIX and warmup indicator
  local suffix_parts=()
  if [ -n "$REPORT_SUFFIX" ]; then
    suffix_parts+=("$REPORT_SUFFIX")
  fi
  if [ "$warmup_num" -gt 0 ]; then
    # Calculate padding width based on total warmup count
    local padding_width=${#WARMUP}
    # Zero-pad the warmup number
    local padded_num
    padded_num=$(printf "%0${padding_width}d" "$warmup_num")
    suffix_parts+=("warmup-${padded_num}")
  fi

  # Only rename if we have a suffix
  if [ ${#suffix_parts[@]} -gt 0 ]; then
    local combined_suffix
    combined_suffix=$(IFS=- ; echo "${suffix_parts[*]}")
    local new_dir="${gatling_run_dir}-${combined_suffix}"
    mv "$gatling_run_dir" "$new_dir"
    gatling_run_dir="$new_dir"
  fi

  echo "Gatling test results are in: $gatling_run_dir"
  save_profiler_data "$gatling_run_dir"
  save_sql_logs "$gatling_run_dir" "$warmup_num"
  post_process_profiler_data "$gatling_run_dir"
  post_process_sql_logs "$gatling_run_dir" "$warmup_num"
  generate_metadata "$gatling_run_dir"
}

echo "========================================"
echo "PHASE: Image Pull"
echo "========================================"
pull_mutable_image

echo ""
echo "========================================"
echo "PHASE: Container Startup"
echo "========================================"
start_containers
prepare_database

if [ "$WARMUP" -gt 0 ]; then
  for i in $(seq 1 "$WARMUP"); do
    echo ""
    echo "========================================"
    echo "PHASE: Warmup Run $i/$WARMUP"
    echo "========================================"
    run_simulation "$i"
  done
  echo "Warmup complete."
fi

echo ""
echo "========================================"
echo "PHASE: Performance Test"
echo "========================================"
run_simulation

echo ""
echo "========================================"
echo "PHASE: Complete"
echo "========================================"
echo "Completed test for $DHIS2_IMAGE"

