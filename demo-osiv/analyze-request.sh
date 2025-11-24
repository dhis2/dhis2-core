#!/usr/bin/env bash
# OSIV Demo - Complete Request Analysis
#
# Demonstrates connection pool behavior by analyzing a single tracker API request.
# Shows:
# - Curl timing (total request time)
# - All connection acquisitions (wait_ms, held_ms per connection)
# - SQL query count and timings from PostgreSQL logs

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BASE_URL="${BASE_URL:-http://localhost:8080}"
AUTH="${AUTH:-admin:district}"
TRACKER_PROGRAM="${TRACKER_PROGRAM:-ur1Edk5Oe2n}"
DHIS2_LOG="${DHIS2_LOG:-$SCRIPT_DIR/logs/dhis.log}"

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
echo -e "${BLUE}  OSIV Demo - Complete Request Analysis${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${CYAN}Request ID: ${YELLOW}$REQUEST_ID${NC}"
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
    "$BASE_URL/api/tracker/trackedEntities?filter=w75KJ2mc4zz:like:joh&fields=attributes,enrollments,trackedEntity,orgUnit&program=$TRACKER_PROGRAM&page=1&pageSize=5&orgUnitMode=ACCESSIBLE")

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

echo -e "${GREEN}✓ Request completed${NC}"
echo "  HTTP Status: $HTTP_CODE"
echo -e "  Total Time: ${YELLOW}${CURL_TIME_MS}ms${NC}"
echo "  Tracked Entities: $TE_COUNT"
echo ""

# Wait a moment for logs to flush
sleep 1

echo -e "${BLUE}───────────────────────────────────────────────────────────────${NC}"
echo ""
echo -e "${CYAN}[Step 2] Analyzing connection acquisitions...${NC}"
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
        echo -e "${GREEN}✓ Found $CONN_COUNT connection acquisitions${NC}"
        echo ""

        # Show connection details
        echo "Connection Timings:"
        echo "──────────────────────────────────────────────────────────────"
        printf "%-3s %-35s %-12s %-12s\n" "#" "Thread" "Wait (ms)" "Held (ms)"
        echo "──────────────────────────────────────────────────────────────"

        # Parse acquired connections
        INDEX=1
        echo "$CONN_LOGS" | grep "CONN_ACQUIRED" | while read line; do
            THREAD=$(echo "$line" | sed -n 's/.*\[\([^]]*\)\].*/\1/p')
            WAIT_MS=$(echo "$line" | grep -oP 'wait_ms=\K[0-9]+')

            # Find corresponding release
            RELEASE_LINE=$(echo "$CONN_LOGS" | grep "CONN_RELEASED" | grep "\[$THREAD\]" | head -1)
            if [[ -n "$RELEASE_LINE" ]]; then
                HELD_MS=$(echo "$RELEASE_LINE" | grep -oP 'held_ms=\K[0-9]+')
                printf "%-3s %-35s %-12s %-12s\n" "$INDEX" "$THREAD" "$WAIT_MS" "$HELD_MS"
            else
                printf "%-3s %-35s %-12s %-12s\n" "$INDEX" "$THREAD" "$WAIT_MS" "(not released)"
            fi
            INDEX=$((INDEX + 1))
        done

        # Highlight main thread (OSIV connection)
        echo "──────────────────────────────────────────────────────────────"
        OSIV_TIME=$(echo "$CONN_LOGS" | grep "CONN_RELEASED" | grep "http-nio" | head -1 | grep -oP 'held_ms=\K[0-9]+')
        if [[ -n "$OSIV_TIME" ]]; then
            echo -e "${YELLOW}Note: Main HTTP thread held connection for ${OSIV_TIME}ms (OSIV - entire request duration: ${CURL_TIME_MS}ms)${NC}"
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
if ! docker ps --format '{{.Names}}' | grep -q "demo-osiv-db-1"; then
    echo -e "${RED}⚠ PostgreSQL container 'demo-osiv-db-1' is not running${NC}"
    echo "  Start with: cd demo-osiv && docker compose up -d db"
else
    # Extract SQL logs for this request
    PG_LOGS=$(docker exec demo-osiv-db-1 cat /var/lib/postgresql/data/log/postgresql.log 2>/dev/null | grep "request_id=$REQUEST_ID")

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

        # Extract duration lines
        DURATIONS=$(echo "$PG_LOGS" | grep "duration:")

        if [[ -n "$DURATIONS" ]]; then
            DURATION_COUNT=$(echo "$DURATIONS" | wc -l)
            echo "Query Durations (${DURATION_COUNT} queries with timing info):"
            echo "─────────────────────────────────────────────────────────"

            echo "$DURATIONS" | while read line; do
                DURATION=$(echo "$line" | grep -oP 'duration: \K[0-9.]+')
                TIMESTAMP=$(echo "$line" | grep -oP '^\S+ \S+')
                echo "  ${DURATION}ms at $TIMESTAMP"
            done
            echo "─────────────────────────────────────────────────────────"
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
echo "  • Curl total time: ${CURL_TIME_MS}ms"
if [[ -n "$OSIV_TIME" ]]; then
    echo "  • OSIV connection held: ${OSIV_TIME}ms"
fi
echo "  • Connections acquired: $CONN_COUNT"
echo "  • SQL queries: $SQL_COUNT"
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
