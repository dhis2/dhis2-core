#!/usr/bin/env bash
# OSIV (Open Session in View) Anti-Pattern Demo
#
# This script demonstrates how OSIV holds database connections for the entire
# duration of an HTTP request, even when the endpoint does no database work.
#
# Prerequisites:
# - DHIS2 running on localhost:9090 with connection.pool.max_size = 2
# - PostgreSQL accessible via docker exec core-dhis2-20411-db-1

set -e

DHIS2_URL="${DHIS2_URL:-http://localhost:9090}"
DHIS2_AUTH="${DHIS2_AUTH:-admin:district}"
DB_CONTAINER="${DB_CONTAINER:-core-dhis2-20411-db-1}"
SLEEP_MS="${SLEEP_MS:-60000}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo_header() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
}

echo_step() {
    echo ""
    echo -e "${YELLOW}▶ $1${NC}"
}

echo_info() {
    echo -e "${GREEN}  ✓ $1${NC}"
}

echo_error() {
    echo -e "${RED}  ✗ $1${NC}"
}

show_connections() {
    echo ""
    echo -e "${YELLOW}  PostgreSQL pg_stat_activity:${NC}"
    docker exec "$DB_CONTAINER" psql -U dhis -d dhis -c \
        "SELECT pid, application_name, state, left(query, 60) as query
         FROM pg_stat_activity
         WHERE datname='dhis' AND state IS NOT NULL
         ORDER BY backend_start;" 2>/dev/null | sed 's/^/  /'
}

show_pool_summary() {
    local total active idle
    result=$(docker exec "$DB_CONTAINER" psql -U dhis -d dhis -t -c \
        "SELECT
            count(*) as total,
            count(*) FILTER (WHERE state = 'active') as active,
            count(*) FILTER (WHERE state = 'idle') as idle
         FROM pg_stat_activity
         WHERE datname='dhis' AND state IS NOT NULL AND application_name != 'psql';" 2>/dev/null)
    echo -e "${GREEN}  Pool state: $result${NC}"
}

cleanup() {
    echo_step "Cleaning up background processes..."
    kill $PID1 $PID2 $PID3 2>/dev/null || true
    wait $PID1 $PID2 $PID3 2>/dev/null || true
}

trap cleanup EXIT

# ============================================================================
echo_header "OSIV Anti-Pattern Demo"
echo ""
echo "This demo shows how Open Session in View (OSIV) keeps database connections"
echo "open for the entire HTTP request duration, even when doing no DB work."
echo ""
echo "Configuration:"
echo "  - HikariCP pool size: 2 connections"
echo "  - Demo endpoint: /api/debug/slowNoDb (sleeps without DB work)"
echo "  - Connection tagged with requestId visible in pg_stat_activity"

# ============================================================================
echo_header "Step 1: Initial State"
echo_step "Checking current database connections..."
show_connections
show_pool_summary

# ============================================================================
echo_header "Step 2: Start SLOW1 Request"
echo_step "Starting first slow request (${SLEEP_MS}ms sleep)..."
echo "  curl -u $DHIS2_AUTH '$DHIS2_URL/api/debug/slowNoDb?sleepMs=${SLEEP_MS}&requestId=SLOW1'"

curl -s -u "$DHIS2_AUTH" "$DHIS2_URL/api/debug/slowNoDb?sleepMs=${SLEEP_MS}&requestId=SLOW1" > /tmp/slow1.out 2>&1 &
PID1=$!
sleep 2

echo_step "Checking connections after SLOW1 started..."
show_connections
show_pool_summary
echo_info "SLOW1 has acquired a connection and is holding it while sleeping"

# ============================================================================
echo_header "Step 3: Start SLOW2 Request"
echo_step "Starting second slow request (${SLEEP_MS}ms sleep)..."
echo "  curl -u $DHIS2_AUTH '$DHIS2_URL/api/debug/slowNoDb?sleepMs=${SLEEP_MS}&requestId=SLOW2'"

curl -s -u "$DHIS2_AUTH" "$DHIS2_URL/api/debug/slowNoDb?sleepMs=${SLEEP_MS}&requestId=SLOW2" > /tmp/slow2.out 2>&1 &
PID2=$!
sleep 2

echo_step "Checking connections after SLOW2 started..."
show_connections
show_pool_summary
echo_info "Both connections are now held by sleeping requests"

# ============================================================================
echo_header "Step 4: Attempt Third Request (Pool Exhausted)"
echo_step "Starting third request to /api/me..."
echo "  This request needs a DB connection but pool is exhausted!"
echo "  HikariCP will wait up to 30s for a connection, then fail."
echo ""
echo "  curl -u $DHIS2_AUTH '$DHIS2_URL/api/me' --max-time 35"

START_TIME=$(date +%s)
curl -s -u "$DHIS2_AUTH" "$DHIS2_URL/api/me" --max-time 35 > /tmp/request3.out 2>&1 &
PID3=$!

echo ""
echo -e "${YELLOW}  Waiting for connection timeout (checking every 5s)...${NC}"

for i in {1..7}; do
    sleep 5
    ELAPSED=$(($(date +%s) - START_TIME))
    echo ""
    echo -e "  [${ELAPSED}s elapsed] Current state:"
    show_connections

    # Check if PID3 finished
    if ! kill -0 $PID3 2>/dev/null; then
        break
    fi
done

wait $PID3 2>/dev/null || true
END_TIME=$(date +%s)
TOTAL_TIME=$((END_TIME - START_TIME))

echo ""
echo_step "Third request completed after ${TOTAL_TIME}s"
echo "  Response:"
cat /tmp/request3.out | sed 's/^/  /' | head -5

# ============================================================================
echo_header "Step 5: Root Cause Analysis"
echo ""
echo "The problem demonstrated:"
echo ""
echo "  1. OSIV filter opens EntityManager at request start"
echo "  2. CspFilter makes first DB call → connection acquired from pool"
echo "  3. Connection held for ENTIRE request duration (even during sleep)"
echo "  4. With pool size = 2, only 2 concurrent requests can be served"
echo "  5. Third request waits 30s for connection, then fails with:"
echo ""
echo -e "${RED}     Connection is not available, request timed out after 30000ms${NC}"
echo -e "${RED}     (total=2, active=2, idle=0, waiting=N)${NC}"
echo ""
echo "This is the OSIV anti-pattern: DB connections are tied to HTTP request"
echo "lifetime instead of actual database work duration."
echo ""
echo "See: https://vladmihalcea.com/the-open-session-in-view-anti-pattern/"

# ============================================================================
echo_header "Demo Complete"
echo_step "Cleaning up..."
