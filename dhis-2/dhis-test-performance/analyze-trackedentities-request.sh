#!/usr/bin/env bash
# TrackedEntities DB Connection Analysis
#
# Analyzes DB connection pool behavior for the /api/tracker/trackedEntities endpoint.
# Demonstrates the N×M query pattern where:
# - N = number of tracked entities (one enrollment query per TE)
# - M = number of enrollments (one event query per enrollment)
#
# Shows:
# - Curl timing (total request time)
# - All connection acquisitions (wait_ms, held_ms per connection)
# - SQL query count and timings from PostgreSQL logs
#
# Usage:
#   ./analyze-trackedentities-request.sh [page_size]
#
# Examples:
#   ./analyze-trackedentities-request.sh       # Default: pageSize=1
#   ./analyze-trackedentities-request.sh 50    # pageSize=50
#
# Environment variables:
#   PROGRAM=IpHINAT79UW           # Program UID (default: Child Programme)
#   BASE_URL=http://localhost:8080
#   AUTH=admin:district

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BASE_URL="${BASE_URL:-http://localhost:8080}"
AUTH="${AUTH:-admin:district}"
DHIS2_LOG="${DHIS2_LOG:-$SCRIPT_DIR/logs/dhis.log}"
PAGE_SIZE="${1:-1}"
PROGRAM="${PROGRAM:-IpHINAT79UW}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

# Generate unique request ID
REQUEST_ID="$(date +%s%3N)"

echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  TrackedEntities DB Connection Analysis${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${CYAN}Request ID: ${YELLOW}$REQUEST_ID${NC}"
echo -e "${CYAN}Program: ${YELLOW}$PROGRAM${NC}"
echo -e "${CYAN}Page Size: ${YELLOW}$PAGE_SIZE${NC}"
echo ""

# Capture current log size to only analyze new entries
DHIS2_LOG_SIZE=0
if [[ -f "$DHIS2_LOG" ]]; then
    DHIS2_LOG_SIZE=$(wc -c < "$DHIS2_LOG")
fi

# Make the request
echo -e "${CYAN}[Step 1] Making tracker API request...${NC}"
echo ""

RESPONSE=$(curl -s -w "\n__CURL_TIMING__\ntime_total:%{time_total}\nhttp_code:%{http_code}\n" \
    -u "$AUTH" \
    -H "X-Request-ID: $REQUEST_ID" \
    "$BASE_URL/api/tracker/trackedEntities?fields=attributes,enrollments,trackedEntity,orgUnit&program=${PROGRAM}&page=1&pageSize=${PAGE_SIZE}&orgUnitMode=ACCESSIBLE")

# Extract timing info
CURL_TIME_SEC=$(echo "$RESPONSE" | grep "^time_total:" | cut -d: -f2)
CURL_TIME_MS=$(echo "$CURL_TIME_SEC * 1000" | bc | cut -d. -f1)
HTTP_CODE=$(echo "$RESPONSE" | grep "^http_code:" | cut -d: -f2)

# Extract response body (before timing markers)
RESPONSE_BODY=$(echo "$RESPONSE" | sed '/^__CURL_TIMING__$/,$d')

# Parse JSON and count tracked entities
TE_COUNT=$(echo "$RESPONSE_BODY" | jq -r '.trackedEntities // [] | length' 2>/dev/null)
if [[ -z "$TE_COUNT" || "$TE_COUNT" == "null" || "$TE_COUNT" == "0" ]]; then
    echo -e "${RED}✗ Failed to parse response or no tracked entities found${NC}"
    echo "  HTTP Status: $HTTP_CODE"
    echo "  Response:"
    echo "$RESPONSE_BODY" | head -20 | sed 's/^/    /'
    exit 1
fi

# Calculate entity statistics
ENTITY_STATS=$(echo "$RESPONSE_BODY" | jq -r '
def stats:
  {
    tes: (.trackedEntities | length),
    enrollments: ([.trackedEntities[].enrollments | length] | add // 0),
    events: ([.trackedEntities[].enrollments[].events | length] | add // 0)
  } | . + {entities: (.tes + .enrollments + .events)};

stats | "\(.tes) \(.enrollments) \(.events) \(.entities)"
' 2>/dev/null)

read STAT_TES STAT_EN STAT_EV STAT_TOTAL <<< "$ENTITY_STATS"

echo -e "${GREEN}✓ Request completed${NC}"
echo "  HTTP Status: $HTTP_CODE"
echo -e "  Total Time: ${YELLOW}${CURL_TIME_MS}ms${NC}"
echo "  Entities: ${STAT_TES} TE, ${STAT_EN} EN, ${STAT_EV} EV (${STAT_TOTAL} total)"
echo ""

# Show per-TE breakdown
echo -e "${CYAN}Entity Breakdown:${NC}"
printf "%-15s %3s %3s\n" "TE_ID" "EN" "EV"
echo "─────────────────────────"
echo "$RESPONSE_BODY" | jq -r '
.trackedEntities[] |
  "\(.trackedEntity) \(.enrollments | length) \([.enrollments[].events | length] | add // 0)"
' 2>/dev/null | while read te_id en_count ev_count; do
    printf "%-15s %3s %3s\n" "$te_id" "$en_count" "$ev_count"
done
echo ""

# Wait a moment for logs to flush
sleep 1

echo -e "${BLUE}───────────────────────────────────────────────────────────────${NC}"
echo ""
echo -e "${CYAN}[Step 2] Analyzing DB connection acquisitions...${NC}"
echo ""

# Extract connection logs for this request
if [[ -f "$DHIS2_LOG" ]]; then
    CONN_LOGS=$(tail -c +$((DHIS2_LOG_SIZE + 1)) "$DHIS2_LOG" | grep "request_id=$REQUEST_ID CONN_")

    if [[ -z "$CONN_LOGS" ]]; then
        echo -e "${RED}⚠ No connection logs found for request_id=$REQUEST_ID${NC}"
        echo "  This might mean:"
        echo "  - Logs haven't flushed yet (try waiting longer)"
        echo "  - Log file path is incorrect: $DHIS2_LOG"
        echo "  - Request ID wasn't propagated properly"
    else
        # Count acquisitions
        CONN_COUNT=$(echo "$CONN_LOGS" | grep "CONN_ACQUIRED" | wc -l)
        echo -e "${GREEN}✓ Found $CONN_COUNT DB connection acquisitions${NC}"
        echo ""

        # Show DB connection details - chronological order (as they were released)
        echo "DB Connection Timings (chronological):"
        echo "──────────────────────────────────────────────────────────────"
        printf "%-3s %-35s %-12s %-12s\n" "#" "Thread" "Wait (ms)" "Held (ms)"
        echo "──────────────────────────────────────────────────────────────"

        # Parse RELEASED events in log order (chronological)
        echo "$CONN_LOGS" | grep "CONN_RELEASED" | nl -w3 | while read idx line; do
            THREAD=$(echo "$line" | sed -n 's/.*\[\([^]]*\)\].*/\1/p')
            WAIT_MS=$(echo "$line" | grep -oP 'wait_ms=\K[0-9]+')
            HELD_MS=$(echo "$line" | grep -oP 'held_ms=\K[0-9]+')
            printf "%-3s %-35s %-12s %-12s\n" "$idx" "$THREAD" "$WAIT_MS" "$HELD_MS"
        done

        # Connection statistics summary
        echo "──────────────────────────────────────────────────────────────"

        # HTTP thread stats
        HTTP_TIMES=$(echo "$CONN_LOGS" | grep "CONN_RELEASED" | grep "http-nio" | grep -oP 'held_ms=\K[0-9]+')
        if [[ -n "$HTTP_TIMES" ]]; then
            HTTP_COUNT=$(echo "$HTTP_TIMES" | wc -l)
            HTTP_SUM=$(echo "$HTTP_TIMES" | paste -sd+ | bc)
            HTTP_MAX=$(echo "$HTTP_TIMES" | sort -n | tail -1)
            HTTP_MIN=$(echo "$HTTP_TIMES" | sort -n | head -1)
            echo -e "${YELLOW}HTTP thread DB connection held: min=${HTTP_MIN}ms max=${HTTP_MAX}ms sum=${HTTP_SUM}ms count=${HTTP_COUNT}${NC}"
        fi

        # Async thread stats
        ASYNC_TIMES=$(echo "$CONN_LOGS" | grep "CONN_RELEASED" | grep -E "TRACKER-TE-FETCH|ForkJoinPool" | grep -oP 'held_ms=\K[0-9]+')
        if [[ -n "$ASYNC_TIMES" ]]; then
            ASYNC_COUNT=$(echo "$ASYNC_TIMES" | wc -l)
            ASYNC_SUM=$(echo "$ASYNC_TIMES" | paste -sd+ | bc)
            ASYNC_MAX=$(echo "$ASYNC_TIMES" | sort -n | tail -1)
            ASYNC_MIN=$(echo "$ASYNC_TIMES" | sort -n | head -1)
            echo -e "${CYAN}Async threads DB connection held: min=${ASYNC_MIN}ms max=${ASYNC_MAX}ms sum=${ASYNC_SUM}ms count=${ASYNC_COUNT}${NC}"
        fi

        # Effective parallelism
        if [[ -n "$HTTP_MAX" ]] && [[ "$HTTP_MAX" -gt 0 ]] && [[ -n "$ASYNC_SUM" ]]; then
            PARALLELISM=$(echo "scale=1; $ASYNC_SUM / $HTTP_MAX" | bc)
            echo ""
            echo -e "${MAGENTA}Effective parallelism: ${PARALLELISM}x (async sum / HTTP max)${NC}"
            echo -e "${MAGENTA}Note: @Transactional on findTrackedEntities holds DB connection while waiting for async threads${NC}"
        fi
    fi
else
    echo -e "${RED}⚠ DHIS2 log file not found: $DHIS2_LOG${NC}"
fi

echo ""
echo -e "${BLUE}───────────────────────────────────────────────────────────────${NC}"
echo ""
echo -e "${CYAN}[Step 3] Analyzing SQL queries...${NC}"
echo ""

# Check if PostgreSQL container is running
PG_CONTAINER="dhis-test-performance-db-1"
if ! docker ps --format '{{.Names}}' | grep -q "$PG_CONTAINER"; then
    echo -e "${RED}⚠ PostgreSQL container '$PG_CONTAINER' is not running${NC}"
    echo "  Start with: docker compose up -d db"
else
    # Extract SQL logs for this request
    PG_LOGS=$(docker exec "$PG_CONTAINER" cat /var/lib/postgresql/data/log/postgresql.log 2>/dev/null | grep "request_id=$REQUEST_ID")

    if [[ -z "$PG_LOGS" ]]; then
        echo -e "${YELLOW}⚠ No SQL queries found for request_id=$REQUEST_ID${NC}"
        echo "  This might mean:"
        echo "  - Request didn't execute any SQL queries"
        echo "  - PostgreSQL logs haven't written yet"
        echo "  - SQL comment injection isn't working"
    else
        # Count SQL statements (lines with /* request_id= */)
        SQL_COUNT=$(echo "$PG_LOGS" | grep -c "/\* request_id=$REQUEST_ID \*/")
        echo -e "${GREEN}✓ Found $SQL_COUNT SQL queries${NC}"
        echo ""

        # Extract duration lines and show chronologically
        DURATIONS=$(echo "$PG_LOGS" | grep "duration:")

        if [[ -n "$DURATIONS" ]]; then
            DURATION_COUNT=$(echo "$DURATIONS" | wc -l)
            echo "SQL Query Durations (chronological):"
            echo "─────────────────────────────────────────────────────────"
            printf "%-5s %-12s %s\n" "#" "Duration" "Timestamp"
            echo "─────────────────────────────────────────────────────────"

            echo "$DURATIONS" | nl -w3 | while read idx line; do
                DURATION=$(echo "$line" | grep -oP 'duration: \K[0-9.]+')
                TIMESTAMP=$(echo "$line" | grep -oP '^\S+ \S+')
                printf "%-5s %-12s %s\n" "$idx" "${DURATION}ms" "$TIMESTAMP"
            done

            # Summary stats
            echo "─────────────────────────────────────────────────────────"
            SQL_TIMES=$(echo "$DURATIONS" | grep -oP 'duration: \K[0-9.]+')
            SQL_MIN=$(echo "$SQL_TIMES" | sort -n | head -1)
            SQL_MAX=$(echo "$SQL_TIMES" | sort -n | tail -1)
            SQL_SUM=$(echo "$SQL_TIMES" | paste -sd+ | bc)
            echo -e "${YELLOW}SQL query duration: min=${SQL_MIN}ms max=${SQL_MAX}ms sum=${SQL_SUM}ms count=${DURATION_COUNT}${NC}"
        else
            echo -e "${YELLOW}No query duration info found (only queries >100ms are logged with durations)${NC}"
        fi

        # Show sample queries
        echo ""
        echo "Sample Queries (first 3):"
        echo "─────────────────────────────────────────────────────────"
        echo "$PG_LOGS" | grep "LOG:.*request_id=$REQUEST_ID" | head -3 | while read line; do
            QUERY=$(echo "$line" | sed 's/.*LOG:  //' | head -c 100)
            echo "  $QUERY..."
        done
        echo "─────────────────────────────────────────────────────────"
    fi
fi

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}Analysis complete!${NC}"
echo ""
echo "Summary for request_id=$REQUEST_ID:"
echo ""
echo "  Request:"
echo "    Total time: ${CURL_TIME_MS}ms"
echo "    Entities: ${STAT_TES} TEs, ${STAT_EN} enrollments, ${STAT_EV} events"
echo ""
echo "  DB Connections:"
if [[ -n "$HTTP_MAX" ]]; then
    echo "    HTTP thread: min=${HTTP_MIN}ms max=${HTTP_MAX}ms sum=${HTTP_SUM}ms count=${HTTP_COUNT}"
fi
if [[ -n "$ASYNC_SUM" ]]; then
    echo "    Async threads: min=${ASYNC_MIN}ms max=${ASYNC_MAX}ms sum=${ASYNC_SUM}ms count=${ASYNC_COUNT}"
fi
echo "    Total acquired: $CONN_COUNT"
if [[ -n "$CONN_COUNT" ]] && [[ "$STAT_TES" -gt 0 ]]; then
    CONN_PER_TE=$(echo "scale=2; $CONN_COUNT / $STAT_TES" | bc)
    echo "    Per TE: ${CONN_PER_TE}"
fi
if [[ -n "$PARALLELISM" ]]; then
    echo "    Effective parallelism: ${PARALLELISM}x"
fi
echo ""
echo "  SQL Queries:"
echo "    Count: $SQL_COUNT"
if [[ -n "$SQL_SUM" ]]; then
    echo "    Duration: min=${SQL_MIN}ms max=${SQL_MAX}ms sum=${SQL_SUM}ms"
fi

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
