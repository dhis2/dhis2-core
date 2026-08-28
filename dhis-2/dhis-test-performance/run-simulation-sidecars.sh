#!/usr/bin/env bash
# Wraps run-simulation.sh with measurement sidecars for DB and JVM/pool observability:
#
#   1. pg_stat_activity sampler: one CSV row per SIDECAR_INTERVAL seconds with connection
#      state counts (active / idle / idle in transaction), wait-event class counts and the
#      max idle-in-transaction / transaction age. This is the idle-in-transaction pileup
#      signal of the L2 region-lock convoy.
#   2. /api/metrics sampler: HikariCP pool + JVM thread-state series per tick, plus a full
#      Prometheus-format snapshot every SIDECAR_SNAPSHOT_INTERVAL seconds so per-region
#      ehcache stats deltas can be computed over any time window after the fact.
#
# All sidecar output is copied into every non-warmup Gatling run directory created by the
# wrapped invocation, under <run-dir>/sidecar/, together with the dhis.conf variant used.
#
# Usage: identical to run-simulation.sh (all environment variables pass through), e.g.
#
#   DHIS2_IMAGE=dhis2/core-l2truth:local \
#   DHIS_CONF_FILE=dhis-l2cache-on.conf \
#   SIMULATION_CLASS=org.hisp.dhis.test.platform.L2CacheReadHeavyRampTest \
#   ./run-simulation-sidecars.sh
#
# Additional options:
#   SIDECAR_INTERVAL           Sampler tick in seconds (default: 2)
#   SIDECAR_SNAPSHOT_INTERVAL  Full /api/metrics snapshot interval in seconds (default: 30)
#   SIDECAR_METRIC_PREFIXES    Extended-regex of series kept in the per-tick metrics TSV
#                              (default: DHIS2 pool gauges jdbc_connections_* -- HikariCP behind
#                              micrometer's generic JDBC binder -- plus JVM thread states and CPU;
#                              histograms are covered by the periodic full snapshots)
set -euo pipefail
cd "$(dirname "$0")"

SIDECAR_INTERVAL=${SIDECAR_INTERVAL:-2}
SIDECAR_SNAPSHOT_INTERVAL=${SIDECAR_SNAPSHOT_INTERVAL:-30}
SIDECAR_METRIC_PREFIXES=${SIDECAR_METRIC_PREFIXES:-"jdbc_connections\\{|jdbc_connections_(active|idle|pending|max|min|timeout)|jvm_threads_states|process_cpu_usage|system_cpu_usage"}
DHIS2_USERNAME=${DHIS2_USERNAME:-"admin"}
DHIS2_PASSWORD=${DHIS2_PASSWORD:-"district"}
METRICS_URL=${METRICS_URL:-"http://localhost:8080/api/metrics"}

SIDECAR_DIR="target/sidecar-$(date +%Y%m%d%H%M%S)"
mkdir -p "$SIDECAR_DIR/snapshots"

compose_args() {
  local args=("-f" "docker-compose.yml")
  if [ -n "${PROF_ARGS:-}" ]; then
    args+=("-f" "docker-compose.profile.yml")
  fi
  if [ -n "${COMPOSE_EXTRA_FILE:-}" ]; then
    args+=("-f" "$COMPOSE_EXTRA_FILE")
  fi
  echo "${args[@]}"
}

PG_QUERY="SELECT to_char(now() at time zone 'UTC', 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"'),
  count(*),
  count(*) FILTER (WHERE state = 'active'),
  count(*) FILTER (WHERE state = 'idle'),
  count(*) FILTER (WHERE state = 'idle in transaction'),
  count(*) FILTER (WHERE wait_event_type = 'Lock'),
  count(*) FILTER (WHERE wait_event_type = 'LWLock'),
  count(*) FILTER (WHERE wait_event_type = 'Client'),
  count(*) FILTER (WHERE wait_event_type = 'IO'),
  coalesce(round(max(extract(epoch FROM now() - state_change)) FILTER (WHERE state = 'idle in transaction')::numeric, 3), 0),
  coalesce(round(max(extract(epoch FROM now() - xact_start))::numeric, 3), 0)
FROM pg_stat_activity WHERE datname = 'dhis' AND pid <> pg_backend_pid()"

pg_sampler() {
  local out="$SIDECAR_DIR/pg_stat_activity.csv"
  echo "ts,total,active,idle,idle_in_tx,wait_lock,wait_lwlock,wait_client,wait_io,max_idle_in_tx_s,max_xact_age_s" > "$out"
  # shellcheck disable=SC2046
  while true; do
    docker compose $(compose_args) exec -T db \
      psql --username=dhis --no-align --tuples-only --field-separator=, \
      --command="$PG_QUERY" 2>/dev/null >> "$out" || true
    sleep "$SIDECAR_INTERVAL"
  done
}

metrics_sampler() {
  # Tab-separated: Prometheus label values can contain commas, quotes and spaces, so split
  # each exposition line at its LAST space (series left, value right) and emit TSV.
  local out="$SIDECAR_DIR/metrics_timeseries.tsv"
  printf "ts\tseries\tvalue\n" > "$out"
  local last_snapshot=0
  while true; do
    local now body
    now=$(date +%s)
    if body=$(curl --silent --fail --max-time 5 --header "Accept: text/plain" \
        --user "$DHIS2_USERNAME:$DHIS2_PASSWORD" "$METRICS_URL" 2>/dev/null); then
      echo "$body" | grep -E "^($SIDECAR_METRIC_PREFIXES)" \
        | awk -v ts="$now" '{ i = match($0, /[[:space:]][^[:space:]]+$/);
            if (i > 0) printf "%s\t%s\t%s\n", ts, substr($0, 1, i - 1), substr($0, i + 1) }' \
        >> "$out" || true
      if [ $((now - last_snapshot)) -ge "$SIDECAR_SNAPSHOT_INTERVAL" ]; then
        echo "$body" > "$SIDECAR_DIR/snapshots/metrics-$now.prom"
        last_snapshot=$now
      fi
    fi
    sleep "$SIDECAR_INTERVAL"
  done
}

# Record run dirs that exist before, to identify the ones this invocation creates
before_dirs=$(ls -d target/gatling/*/ 2>/dev/null || true)

pg_sampler &
PG_SAMPLER_PID=$!
metrics_sampler &
METRICS_SAMPLER_PID=$!

stop_samplers() {
  kill "$PG_SAMPLER_PID" "$METRICS_SAMPLER_PID" 2>/dev/null || true
  wait "$PG_SAMPLER_PID" "$METRICS_SAMPLER_PID" 2>/dev/null || true
}

collect() {
  local exit_code=$?
  set +e
  stop_samplers

  local after_dirs new_dirs
  after_dirs=$(ls -d target/gatling/*/ 2>/dev/null || true)
  new_dirs=$(comm -13 <(echo "$before_dirs" | sort) <(echo "$after_dirs" | sort) | grep -v warmup)

  if [ -n "$new_dirs" ]; then
    while IFS= read -r dir; do
      [ -d "$dir" ] || continue
      mkdir -p "$dir/sidecar"
      cp -r "$SIDECAR_DIR"/. "$dir/sidecar/"
      cp "docker/${DHIS_CONF_FILE:-dhis.conf}" "$dir/sidecar/"
      echo "Sidecar data: $dir/sidecar/"
    done <<< "$new_dirs"
    rm -rf "$SIDECAR_DIR"
  else
    echo "Warning: no non-warmup run directory found; sidecar data left in $SIDECAR_DIR"
  fi
  exit $exit_code
}
trap collect EXIT

./run-simulation.sh
