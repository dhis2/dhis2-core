#!/bin/bash
# Run Gatling simulations against a DHIS2 instance running in Docker
set -euo pipefail

################################################################################
# USAGE
################################################################################

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
  echo "  DB_TYPE               Database type (default: sierra-leone)"
  echo "                        Valid values: sierra-leone, hmis"
  echo "  DB_VERSION            Database version (default: dev)"
  echo "                        Must be alphanumeric, dots, hyphens, underscores only"
  echo "                        Pattern: s3://databases.dhis2.org/<type>/<version>/dhis2-db-<type>.sql.gz"
  echo "  DHIS2_USERNAME        DHIS2 username for API authentication (default: admin)"
  echo "  DHIS2_PASSWORD        DHIS2 password for API authentication (default: district)"
  echo "  ANALYTICS_GENERATE    Generate analytics tables before running tests (default: false)"
  echo "                        Required for analytics endpoints that query pre-computed tables"
  echo "  ANALYTICS_TIMEOUT     Max wait time for analytics generation in seconds (default: 900 = 15min)"
  echo "  HEALTHCHECK_TIMEOUT   Max wait time for DHIS2 startup in seconds (default: 300 = 5min)"
  echo "  WARMUP                Number of warmup iterations before actual test (default: 1)"
  echo "  REPORT_SUFFIX         Suffix to append to Gatling report directory name (default: empty)"
  echo "  CAPTURE_SQL_LOGS      Capture and analyze SQL logs for non-warmup runs"
  echo "                        Set to any non-empty value to enable (default: disabled)"
  echo "                        Analysis requires pgbadger: https://github.com/darold/pgbadger"
  echo "  PROF_ARGS             Async-profiler arguments (enables profiling)"
  echo "                        Options: https://github.com/async-profiler/async-profiler/blob/master/docs/ProfilerOptions.md"
  echo "  MVN_ARGS              Additional Maven arguments passed to mvn gatling:test"
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
  echo "  WARMUP=2 \\"
  echo "  REPORT_SUFFIX=\"baseline\" \\"
  echo "  DHIS2_IMAGE=dhis2/core-dev:latest \\"
  echo "  SIMULATION_CLASS=org.hisp.dhis.test.tracker.TrackerTest $0"
  echo ""
  echo "  # With analytics table generation (required for analytics endpoints)"
  echo "  SIMULATION_CLASS=org.hisp.dhis.test.raw.GetRawSpeedTest \\"
  echo "  DHIS2_IMAGE=dhis2/core:2.42.1 \\"
  echo "  ANALYTICS_GENERATE=true \\"
  echo "  ANALYTICS_TIMEOUT=7200 \\"
  echo "  DB_TYPE=hmis \\"
  echo "  DB_VERSION=2.42 \\"
  echo "  DHIS2_USERNAME=qadmin \\"
  echo "  DHIS2_PASSWORD='!Qadmin123S' \\"
  echo "  MVN_ARGS=\"-Dscenario=test-scenarios/hmis/analytics-ev-query-speed-get-test.json -Dversion=42.1 -Dbaseline=42.0\" \\"
  echo "  $0"
  echo ""
}

################################################################################
# VALIDATE REQUIRED ENVIRONMENT VARIABLES
################################################################################

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

################################################################################
# ENVIRONMENT SETUP
################################################################################

DB_TYPE=${DB_TYPE:-"sierra-leone"}
DB_VERSION=${DB_VERSION:-"dev"}
DHIS2_USERNAME=${DHIS2_USERNAME:-"admin"}
DHIS2_PASSWORD=${DHIS2_PASSWORD:-"district"}
ANALYTICS_GENERATE=${ANALYTICS_GENERATE:-"false"}
ANALYTICS_TIMEOUT=${ANALYTICS_TIMEOUT:-900} # default of 15min
HEALTHCHECK_TIMEOUT=${HEALTHCHECK_TIMEOUT:-300} # default of 5min
WARMUP=${WARMUP:-1}
REPORT_SUFFIX=${REPORT_SUFFIX:-""}
CAPTURE_SQL_LOGS=${CAPTURE_SQL_LOGS:-""}
PROF_ARGS=${PROF_ARGS:=""}
MVN_ARGS=${MVN_ARGS:-""}

# Validate DB_TYPE (only allow sierra-leone or hmis)
case "$DB_TYPE" in
  sierra-leone|hmis)
    # Valid
    ;;
  *)
    echo "Error: DB_TYPE must be 'sierra-leone' or 'hmis', got: $DB_TYPE" >&2
    echo "Run '$0' without arguments to see usage" >&2
    exit 1
    ;;
esac

# Validate DB_VERSION (no slashes, no special characters that could be malicious)
# Allow only alphanumeric, dots, hyphens, and underscores
if ! echo "$DB_VERSION" | grep -qE '^[a-zA-Z0-9._-]+$'; then
  echo "Error: DB_VERSION contains invalid characters: $DB_VERSION" >&2
  echo "DB_VERSION must contain only alphanumeric characters, dots, hyphens, and underscores" >&2
  echo "Run '$0' without arguments to see usage" >&2
  exit 1
fi

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

################################################################################
# CLEANUP & TRAP
################################################################################

# set -e ensures the script fails with proper exit code which is important for CI steps to fail.
# This cleanup trap ensures post-processing and container cleanup happen even if the script exits early.
cleanup() {
  local exit_code=$?

  # Disable exit on error for cleanup to ensure it completes
  set +e

  # Post-processing that has to be done after all runs complete
  echo ""
  echo "========================================"
  echo "PHASE: Post-processing"
  echo "========================================"
  post_process_gatling_logs || echo "Warning: Failed to post-process Gatling logs"

  echo ""
  echo "Cleaning up containers..."
  local compose_files=("-f" "docker-compose.yml")
  if [ -n "$PROF_ARGS" ]; then
    compose_files+=("-f" "docker-compose.profile.yml")
  fi
  docker compose "${compose_files[@]}" down --volumes 2>/dev/null || true

  echo ""
  echo "========================================"
  echo "PHASE: Complete"
  echo "========================================"
  if [ $exit_code -eq 0 ]; then
    echo -e "\033[0;32m✓\033[0m Test for $DHIS2_IMAGE ran successfully"
  else
    echo -e "\033[0;31m✗\033[0m Test for $DHIS2_IMAGE failed"
  fi

  exit $exit_code
}

trap cleanup EXIT

################################################################################
# FUNCTIONS
################################################################################

pull_mutable_image() {
  # Pull images with mutable tags to ensure we get the latest version. See
  # https://github.com/dhis2/dhis2-core/blob/master/docker/DOCKERHUB.md for tag types. Mutable tags
  # (dhis2/core-dev:*, dhis2/core-pr:*) are overwritten multiple times a day. Immutable tags
  # (dhis2/core:2.42.1) are never rebuilt once published. Docker caches images locally, so without
  # an explicit pull, we may run an outdated version even when using tags like 'latest' or 'master'.
  # This is especially important on our self-hosted runner as devs will expect their latest change
  # to be tested.

  if [[ "$DHIS2_IMAGE" =~ ^dhis2/core-(dev|pr): ]]; then
    echo "========================================"
    echo "PHASE: Image Pull"
    echo "========================================"
    echo "Pulling mutable image tag: $DHIS2_IMAGE"
    docker pull "$DHIS2_IMAGE"
  fi
}

dump_container_logs() {
  echo ""
  echo "========================================"
  echo "PHASE: Container Logs (Debug Info)"
  echo "========================================"
  echo ""
  echo "Collecting diagnostic information to help debug startup failure..."
  echo ""

  # Determine which compose files to use
  local compose_files=("-f" "docker-compose.yml")
  if [ -n "$PROF_ARGS" ]; then
    compose_files+=("-f" "docker-compose.profile.yml")
  fi

  echo "================================================================"
  echo "Container Status:"
  echo "================================================================"
  docker compose "${compose_files[@]}" ps || true

  echo ""
  echo "================================================================"
  echo "DHIS2 Web Container Logs (last 500 lines):"
  echo "================================================================"
  docker compose "${compose_files[@]}" logs --tail=500 web 2>&1 || echo "Failed to retrieve web logs"

  echo ""
  echo "================================================================"
  echo "Database Container Logs (last 50 lines):"
  echo "================================================================"
  docker compose "${compose_files[@]}" logs --tail=50 db 2>&1 || echo "Failed to retrieve db logs"
}

start_containers() {
  echo "Testing with image: $DHIS2_IMAGE"
  echo "Waiting for containers to be ready..."

  local start_time
  start_time=$(date +%s)

  # Determine which compose files to use
  local compose_files=("-f" "docker-compose.yml")
  if [ -n "$PROF_ARGS" ]; then
    compose_files+=("-f" "docker-compose.profile.yml")
  fi

  docker compose "${compose_files[@]}" down --volumes
  if ! docker compose "${compose_files[@]}" up --detach --wait --wait-timeout "$HEALTHCHECK_TIMEOUT"; then
    echo "Error: Failed to start containers"
    dump_container_logs
    exit 1
  fi

  echo "All containers ready! (took $(($(date +%s) - start_time))s)"
}

save_profiler_data() {
  local gatling_dir="$1"

  if [ -z "$PROF_ARGS" ]; then
    return 0
  fi

  if [ ! -d "$gatling_dir" ]; then
    echo "Warning: Cannot save profiler data - directory does not exist: $gatling_dir"
    return 1
  fi

  echo ""
  echo "Saving profiler data..."
  if ! docker compose cp web:/profiler-output/. "$gatling_dir/" 2>/dev/null; then
    echo "Warning: Failed to copy profiler data from container"
    return 1
  fi
  echo "Profiler data saved to $gatling_dir"
}

save_sql_logs() {
  local gatling_dir="$1"
  local warmup_num="${2:-0}"

  if [ -z "$CAPTURE_SQL_LOGS" ] || [ "$warmup_num" -gt 0 ]; then
    return 0
  fi

  if [ ! -d "$gatling_dir" ]; then
    echo "Warning: Cannot save SQL logs - directory does not exist: $gatling_dir"
    return 1
  fi

  echo ""
  echo "Saving SQL logs..."
  if ! docker compose cp db:/var/lib/postgresql/data/log/postgresql.log "$gatling_dir/postgresql.log" 2>/dev/null; then
    echo "Warning: Failed to copy SQL logs from container"
    return 1
  fi
  echo "SQL logs saved to $gatling_dir"
}

post_process_profiler_data() {
  local gatling_dir="$1"

  if [ -z "$PROF_ARGS" ]; then
    return 0
  fi

  if [ ! -d "$gatling_dir" ]; then
    echo "Warning: Cannot post-process profiler data - directory does not exist: $gatling_dir"
    return 1
  fi

  if [ ! -f "$gatling_dir/profile.jfr" ]; then
    echo "Warning: Cannot post-process profiler data - profile.jfr not found"
    return 1
  fi

  echo ""
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
  if ! docker compose exec --workdir /profiler-output web jfrconv $jfrconv_flags --dot --title "$title" profile.jfr profile.html 2>/dev/null; then
    echo "Warning: Failed to generate flamegraph"
    return 1
  fi
  # shellcheck disable=SC2086
  docker compose exec --workdir /profiler-output web jfrconv $jfrconv_flags --dot profile.jfr profile.collapsed 2>/dev/null || true

  if ! docker compose cp web:/profiler-output/. "$gatling_dir/" 2>/dev/null; then
    echo "Warning: Failed to copy post-processed profiler data"
    return 1
  fi

  echo "Post-processing profiler data complete. Files saved to $gatling_dir"
}

post_process_sql_logs() {
  local gatling_dir="$1"
  local warmup_num="${2:-0}"

  if [ -z "$CAPTURE_SQL_LOGS" ] || [ "$warmup_num" -gt 0 ]; then
    return 0
  fi

  if [ ! -d "$gatling_dir" ]; then
    echo "Warning: Cannot post-process SQL logs - directory does not exist: $gatling_dir"
    return 1
  fi

  if [ ! -f "$gatling_dir/postgresql.log" ]; then
    echo "Warning: Cannot post-process SQL logs - postgresql.log not found"
    return 1
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
  if ! pgbadger \
    --title "$SIMULATION_CLASS on $DHIS2_IMAGE" \
    --prefix '%t [%p]: user=%u,db=%d,app=%a ' \
    --dbname dhis \
    --outfile "$gatling_dir/pgbadger.html" "$gatling_dir/postgresql.log" 2>/dev/null; then
    echo "Warning: Failed to generate pgbadger report"
    return 1
  fi
  echo "Post-processing SQL logs complete. File saved to $gatling_dir/pgbadger.html"
}

post_process_gatling_logs() {
  # In 3.12 https://github.com/gatling/gatling/issues/4596 Gatling started to write the test
  # results into a binary format. Gatling OSS does not support exporting that into an
  # accessible format for us. The serializer/deserializer are OSS though. Our fork at
  # https://github.com/dhis2/gatling/tree/glog-cli uses them to provide a CLI to extract the
  # binary simulation.log into a simulation.csv. CLI releases can be downloaded from
  # https://github.com/dhis2/gatling/releases.
  local gatling_dir="target/gatling"

  if [ ! -d "$gatling_dir" ]; then
    echo "Warning: Cannot post-process Gatling logs - directory does not exist: $gatling_dir"
    return 1
  fi

  if ! command -v glog &> /dev/null; then
    echo ""
    echo "Warning: glog not found in PATH. Skipping binary simulation.log conversion."
    echo "Install glog to enable conversion to CSV: https://github.com/dhis2/gatling/releases"
    echo ""
    return 0
  fi

  echo ""
  echo "Post-processing Gatling logs..."
  if ! glog --config ./src/test/resources/gatling.conf --scan-subdirs "$gatling_dir" 2>/dev/null; then
    echo "Warning: Failed to convert simulation.log to simulation.csv"
    return 1
  fi
  echo "Post-processing Gatling logs complete. CSV files saved in subdirectories."
}

prepare_database() {
  echo ""
  echo "Preparing database..."
  docker compose exec db psql --username=dhis --quiet --command='vacuum analyze;' > /dev/null

  if [ "$ANALYTICS_GENERATE" = "true" ]; then
    echo ""
    echo "Generating analytics tables (this may take several minutes)..."

    local start_time
    start_time=$(date +%s)

    local response
    response=$(curl --silent --user "$DHIS2_USERNAME:$DHIS2_PASSWORD" --request POST http://localhost:8080/api/resourceTables/analytics)

    local job_config_id
    job_config_id=$(echo "$response" | jq --raw-output '.response.id // empty')

    if [ -z "$job_config_id" ]; then
      echo "Error: Failed to trigger analytics generation"
      echo "Response: $response"
      exit 1
    fi

    echo "Analytics job triggered (config ID: $job_config_id)"
    echo "Waiting for job to start and monitoring progress..."

    while true; do
      local elapsed=$(($(date +%s) - start_time))

      if [ "$elapsed" -gt "$ANALYTICS_TIMEOUT" ]; then
        echo "Error: Analytics generation timed out after ${ANALYTICS_TIMEOUT}s"
        echo "Consider increasing ANALYTICS_TIMEOUT if needed"
        exit 1
      fi

      # Get all analytics tasks - the running task might have a different ID than the job config
      local all_tasks
      all_tasks=$(curl --silent --user "$DHIS2_USERNAME:$DHIS2_PASSWORD" "http://localhost:8080/api/system/tasks/ANALYTICS_TABLE")

      # Get the first (most recent) task ID
      local task_id
      task_id=$(echo "$all_tasks" | jq --raw-output 'keys[0] // empty')

      if [ -z "$task_id" ]; then
        echo "Status (${elapsed}s): Job scheduled but not started yet"
        sleep 5
        continue
      fi

      # Get the task details for the running task
      local task_response
      task_response=$(echo "$all_tasks" | jq --raw-output ".\"$task_id\"")

      local has_completed
      has_completed=$(echo "$task_response" | jq '[.[] | select(.completed == true)] | length')

      local has_error
      has_error=$(echo "$task_response" | jq '[.[] | select(.level == "ERROR")] | length')

      if [ "$has_error" -gt 0 ]; then
        echo "Error: Analytics generation failed"
        echo "$task_response" | jq '[.[] | select(.level == "ERROR")] | .[0].message' --raw-output
        exit 1
      fi

      if [ "$has_completed" -gt 0 ]; then
        local duration_msg
        duration_msg=$(echo "$task_response" | jq --raw-output '[.[] | select(.completed == true and (.message | contains("Analytics tables updated")))] | .[0].message // empty')
        if [ -n "$duration_msg" ]; then
          echo "$duration_msg"
        else
          echo "Analytics tables generated successfully! (took ${elapsed}s)"
        fi
        break
      fi

      local latest_msg
      local msg_count
      msg_count=$(echo "$task_response" | jq 'length')
      if [ "$msg_count" -gt 0 ]; then
        latest_msg=$(echo "$task_response" | jq --raw-output '.[0].message')
        echo "Status (${elapsed}s): $latest_msg"
      else
        echo "Status (${elapsed}s): Job running (no status messages available yet)"
      fi

      sleep 5
    done
  fi
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
  local simulation_run_file="$gatling_run_dir/run-simulation.env"

  echo ""
  echo "Generating run metadata..."

  # Get DHIS2 image digest for reproducibility
  # DHIS2 images are only pushed to Docker Hub, so RepoDigests[0] is the Docker Hub digest
  local dhis2_image_digest=""
  local dhis2_labels=""

  # Get DHIS2 image RepoDigest (registry digest that can be pulled for exact reproduction)
  dhis2_image_digest=$(docker inspect --format '{{index .RepoDigests 0}}' "$DHIS2_IMAGE" 2>/dev/null || echo "unknown")

  # Extract DHIS2 labels from image (DHIS2_BUILD_BRANCH, DHIS2_BUILD_REVISION, DHIS2_VERSION, etc.)
  dhis2_labels=$(docker inspect --format '{{json .Config.Labels}}' "$DHIS2_IMAGE" 2>/dev/null | \
    jq --raw-output 'to_entries | map(select(.key | startswith("DHIS2_"))) | sort_by(.key) | .[] | "\(.key)=\(.value)"' 2>/dev/null || echo "")

  # Build reproducible command using RepoDigest
  # DB image is reproducible via DB_TYPE and DB_VERSION args
  local dhis2_image_immutable="$DHIS2_IMAGE"
  if [ "$dhis2_image_digest" != "unknown" ] && [ -n "$dhis2_image_digest" ]; then
    dhis2_image_immutable="$dhis2_image_digest"
  fi

  # Get git commit for header resolution
  local git_commit
  git_commit=$(git rev-parse HEAD 2>/dev/null || echo 'unknown')

  {
    echo "# Reproduce this test run:"
    echo "#   git checkout $git_commit"
    echo "#   set -a && source run-simulation.env && set +a && ./run-simulation.sh"
    echo "#"
    echo "# Use COMMAND_IMMUTABLE for exact reproduction with pinned image digest"
    echo ""
    echo "# Args"
    echo "DHIS2_IMAGE=$DHIS2_IMAGE"
    echo "SIMULATION_CLASS=$SIMULATION_CLASS"
    echo "DB_TYPE=$DB_TYPE"
    echo "DB_VERSION=$DB_VERSION"
    echo "DHIS2_USERNAME=$DHIS2_USERNAME"
    echo "DHIS2_PASSWORD=$DHIS2_PASSWORD"
    echo "ANALYTICS_GENERATE=$ANALYTICS_GENERATE"
    echo "ANALYTICS_TIMEOUT=$ANALYTICS_TIMEOUT"
    echo "HEALTHCHECK_TIMEOUT=$HEALTHCHECK_TIMEOUT"
    echo "WARMUP=$WARMUP"
    echo "REPORT_SUFFIX=$REPORT_SUFFIX"
    echo "CAPTURE_SQL_LOGS=$CAPTURE_SQL_LOGS"
    echo "PROF_ARGS=\"$PROF_ARGS\""
    echo "MVN_ARGS=\"$MVN_ARGS\""
    echo ""
    echo "# Additional metadata"
    echo "GIT_BRANCH_PERFORMANCE_TESTS=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')"
    echo "GIT_COMMIT_PERFORMANCE_TESTS=$git_commit"
    echo "GIT_DIRTY_PERFORMANCE_TESTS=$([ -n "$(git status --porcelain 2>/dev/null)" ] && echo 'true' || echo 'false')"
    echo "DHIS2_IMAGE_DIGEST=$dhis2_image_digest"
    if [ -n "$dhis2_labels" ]; then
      echo "$dhis2_labels"
    fi
    echo "COMMAND=DHIS2_IMAGE=$DHIS2_IMAGE DB_TYPE=$DB_TYPE DB_VERSION=$DB_VERSION DHIS2_USERNAME=$DHIS2_USERNAME DHIS2_PASSWORD=$DHIS2_PASSWORD SIMULATION_CLASS=$SIMULATION_CLASS${ANALYTICS_GENERATE:+ ANALYTICS_GENERATE=$ANALYTICS_GENERATE}${ANALYTICS_TIMEOUT:+ ANALYTICS_TIMEOUT=$ANALYTICS_TIMEOUT}${HEALTHCHECK_TIMEOUT:+ HEALTHCHECK_TIMEOUT=$HEALTHCHECK_TIMEOUT}${WARMUP:+ WARMUP=$WARMUP}${REPORT_SUFFIX:+ REPORT_SUFFIX=$REPORT_SUFFIX}${CAPTURE_SQL_LOGS:+ CAPTURE_SQL_LOGS=$CAPTURE_SQL_LOGS}${PROF_ARGS:+ PROF_ARGS=\"$PROF_ARGS\"}${MVN_ARGS:+ MVN_ARGS=\"$MVN_ARGS\"} $0"
    echo "COMMAND_IMMUTABLE=DHIS2_IMAGE=$dhis2_image_immutable DB_TYPE=$DB_TYPE DB_VERSION=$DB_VERSION DHIS2_USERNAME=$DHIS2_USERNAME DHIS2_PASSWORD=$DHIS2_PASSWORD SIMULATION_CLASS=$SIMULATION_CLASS${ANALYTICS_GENERATE:+ ANALYTICS_GENERATE=$ANALYTICS_GENERATE}${ANALYTICS_TIMEOUT:+ ANALYTICS_TIMEOUT=$ANALYTICS_TIMEOUT}${HEALTHCHECK_TIMEOUT:+ HEALTHCHECK_TIMEOUT=$HEALTHCHECK_TIMEOUT}${WARMUP:+ WARMUP=$WARMUP}${REPORT_SUFFIX:+ REPORT_SUFFIX=$REPORT_SUFFIX}${CAPTURE_SQL_LOGS:+ CAPTURE_SQL_LOGS=$CAPTURE_SQL_LOGS}${PROF_ARGS:+ PROF_ARGS=\"$PROF_ARGS\"}${MVN_ARGS:+ MVN_ARGS=\"$MVN_ARGS\"} $0"
  } > "$simulation_run_file"

  echo "Gatling run metadata is in: $gatling_run_dir/run-simulation.env"
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
  # Allow Maven to fail (e.g., assertion failures) but continue to post-process results
  set +e
  # shellcheck disable=SC2086
  mvn gatling:test \
    -Dgatling.simulationClass="$SIMULATION_CLASS" \
    -Dusername="$DHIS2_USERNAME" \
    -Dpassword="$DHIS2_PASSWORD" \
    $MVN_ARGS $extra_mvn_args
  local mvn_exit_code=$?
  set -e

  stop_profiler

  # Find the gatling report directory
  local gatling_run_dir=""
  if [ -f target/gatling/lastRun.txt ]; then
    gatling_run_dir="target/gatling/$(head -n 1 target/gatling/lastRun.txt | tr -d '\n')"
  fi

  # If we found a gatling directory, process it
  if [ -n "$gatling_run_dir" ] && [ -d "$gatling_run_dir" ]; then
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

    echo ""
    echo "Gatling test results are in: $gatling_run_dir"

    # Post-process results for this run
    save_profiler_data "$gatling_run_dir" || echo "Warning: Failed to save profiler data"
    save_sql_logs "$gatling_run_dir" "$warmup_num" || echo "Warning: Failed to save SQL logs"
    post_process_profiler_data "$gatling_run_dir" || echo "Warning: Failed to post-process profiler data"
    post_process_sql_logs "$gatling_run_dir" "$warmup_num" || echo "Warning: Failed to post-process SQL logs"
    generate_metadata "$gatling_run_dir" || echo "Warning: Failed to generate metadata"
  else
    echo "Warning: Could not find Gatling report directory"
  fi

  # Return the Maven exit code so failures propagate correctly
  return $mvn_exit_code
}

################################################################################
# MAIN EXECUTION
################################################################################
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
# run_simulation may fail (e.g., assertion failures). Any code that must always run
# should be placed in the cleanup trap to ensure execution even on failure.
run_simulation

