#!/usr/bin/env bash
# OSIV Demo - Connection Pool Exhaustion Analysis
#
# Demonstrates OSIV problem by showing that requests that don't need database
# still hold onto connections for the entire request duration, exhausting the pool.
#
# Demo flow:
# 1. Start SLOW1 - sleeps 35s, doesn't need DB, but holds connection
# 2. Start SLOW2 - sleeps 35s, doesn't need DB, but holds connection
# 3. Pool is now exhausted (size=2)
# 4. Try /api/me - needs DB but can't get connection, waits/times out
#
# Shows in logs:
# - Connection wait_ms and held_ms for each request
# - SLOW1/SLOW2 hold connections for full 35s despite not needing DB
# - /api/me waits 30s for connection before timeout
# - HikariCP pool stats showing exhaustion
#
# Usage:
#   ./analyze-osiv.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BASE_URL="${BASE_URL:-http://localhost:8080}"
AUTH="${AUTH:-admin:district}"
DHIS2_LOG="${DHIS2_LOG:-$SCRIPT_DIR/logs/dhis.log}"
SLEEP_MS="${SLEEP_MS:-35000}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

# Generate unique request IDs
REQUEST_ID_1="SLOW1-$(date +%s%3N)"
REQUEST_ID_2="SLOW2-$(date +%s%3N)"
REQUEST_ID_3="ME-$(date +%s%3N)"

echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  OSIV Demo - Connection Pool Exhaustion${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${CYAN}Configuration:${NC}"
echo "  • Pool size: 2 connections"
echo "  • Sleep duration: ${SLEEP_MS}ms (35s)"
echo "  • Request IDs: $REQUEST_ID_1, $REQUEST_ID_2, $REQUEST_ID_3"
echo ""
echo -e "${CYAN}Demo scenario:${NC}"
echo "  1. SLOW1: /api/debug/osiv/sleep (doesn't need DB, holds connection 35s)"
echo "  2. SLOW2: /api/debug/osiv/sleep (doesn't need DB, holds connection 35s)"
echo "  3. Pool exhausted! Only 2 connections available."
echo "  4. /api/me: needs DB but can't get connection → waits/times out"
echo ""

# Capture current log size to only analyze new entries
DHIS2_LOG_SIZE=0
if [[ -f "$DHIS2_LOG" ]]; then
    DHIS2_LOG_SIZE=$(wc -c < "$DHIS2_LOG")
fi

echo -e "${BLUE}───────────────────────────────────────────────────────────────${NC}"
echo ""
echo -e "${CYAN}[Step 1] Starting SLOW1 request...${NC}"
echo ""

START_1=$(date +%s)
echo -e "${YELLOW}Starting SLOW1 at $(date +%H:%M:%S)${NC}"
echo "  curl '${BASE_URL}/api/debug/osiv/sleep?sleepMs=${SLEEP_MS}'"
curl -s -u "$AUTH" \
    -H "X-Request-ID: $REQUEST_ID_1" \
    "${BASE_URL}/api/debug/osiv/sleep?sleepMs=${SLEEP_MS}" &
PID1=$!
sleep 2
echo -e "${GREEN}  → SLOW1 running in background (PID: $PID1)${NC}"

echo ""
echo -e "${BLUE}───────────────────────────────────────────────────────────────${NC}"
echo ""
echo -e "${CYAN}[Step 2] Starting SLOW2 request...${NC}"
echo ""

START_2=$(date +%s)
echo -e "${YELLOW}Starting SLOW2 at $(date +%H:%M:%S)${NC}"
echo "  curl '${BASE_URL}/api/debug/osiv/sleep?sleepMs=${SLEEP_MS}'"
curl -s -u "$AUTH" \
    -H "X-Request-ID: $REQUEST_ID_2" \
    "${BASE_URL}/api/debug/osiv/sleep?sleepMs=${SLEEP_MS}" &
PID2=$!
sleep 2
echo -e "${GREEN}  → SLOW2 running in background (PID: $PID2)${NC}"

echo ""
echo -e "${BLUE}───────────────────────────────────────────────────────────────${NC}"
echo ""
echo -e "${RED}⚠ Pool exhausted! Both connections held by SLOW1 and SLOW2${NC}"
echo ""
echo -e "${CYAN}[Step 3] Attempting /api/me (needs DB connection)...${NC}"
echo ""

START_3=$(date +%s)
echo -e "${YELLOW}Starting /api/me at $(date +%H:%M:%S)${NC}"
echo "  This will wait for connection or timeout (HikariCP timeout: 30s)"
echo ""

RESULT=$(curl -s -w "\n__CURL_TIMING__\nhttp_code:%{http_code}\ntime_total:%{time_total}\n" \
    -u "$AUTH" \
    -H "X-Request-ID: $REQUEST_ID_3" \
    "${BASE_URL}/api/me" \
    --max-time 35 2>&1)

END_3=$(date +%s)
ELAPSED_3=$((END_3 - START_3))

HTTP_CODE=$(echo "$RESULT" | grep "^http_code:" | cut -d: -f2)
TIME_TOTAL=$(echo "$RESULT" | grep "^time_total:" | cut -d: -f2)
TIME_TOTAL_MS=$(echo "$TIME_TOTAL * 1000" | bc | cut -d. -f1)
BODY=$(echo "$RESULT" | sed '/^__CURL_TIMING__$/,$d')

echo -e "${CYAN}[$(date +%H:%M:%S)] /api/me completed after ${ELAPSED_3}s${NC}"
echo "  HTTP Status: $HTTP_CODE"
echo "  Total Time: ${TIME_TOTAL_MS}ms"

if [[ "$HTTP_CODE" == "200" ]]; then
    echo -e "${GREEN}  → Got response (a connection became available)${NC}"
elif [[ "$HTTP_CODE" == "500" ]]; then
    echo -e "${RED}  → 500 Error: Connection pool timeout!${NC}"
    ERROR_MSG=$(echo "$BODY" | jq -r '.message' 2>/dev/null || echo "$BODY" | head -3)
    echo "  Error: $ERROR_MSG" | head -3 | sed 's/^/    /'
else
    echo -e "${RED}  → Unexpected status${NC}"
    echo "$BODY" | head -3 | sed 's/^/    /'
fi

echo ""
echo -e "${YELLOW}Waiting for SLOW1 and SLOW2 to complete...${NC}"
wait $PID1 $PID2 2>/dev/null || true
END_1=$(date +%s)
END_2=$END_1
ELAPSED_1=$((END_1 - START_1))
ELAPSED_2=$((END_2 - START_2))
echo -e "${GREEN}  → SLOW1 completed after ${ELAPSED_1}s${NC}"
echo -e "${GREEN}  → SLOW2 completed after ${ELAPSED_2}s${NC}"

# Wait for logs to flush
sleep 2

echo ""
echo -e "${BLUE}───────────────────────────────────────────────────────────────${NC}"
echo ""
echo -e "${CYAN}[Step 4] Analyzing connection acquisitions...${NC}"
echo ""

if [[ ! -f "$DHIS2_LOG" ]]; then
    echo -e "${RED}⚠ DHIS2 log file not found: $DHIS2_LOG${NC}"
    exit 1
fi

# Extract connection logs for all three requests
CONN_LOGS=$(tail -c +$((DHIS2_LOG_SIZE + 1)) "$DHIS2_LOG" | grep -E "request_id=($REQUEST_ID_1|$REQUEST_ID_2|$REQUEST_ID_3) CONN_")

if [[ -z "$CONN_LOGS" ]]; then
    echo -e "${RED}⚠ No connection logs found for request IDs${NC}"
    echo "  This might mean:"
    echo "  - Logs haven't flushed yet"
    echo "  - Log file path is incorrect: $DHIS2_LOG"
    echo "  - Request ID wasn't propagated properly"
    exit 1
fi

# Analyze each request
for REQ_ID in "$REQUEST_ID_1" "$REQUEST_ID_2" "$REQUEST_ID_3"; do
    REQ_LOGS=$(echo "$CONN_LOGS" | grep "request_id=$REQ_ID")

    if [[ -z "$REQ_LOGS" ]]; then
        continue
    fi

    # Determine request name
    if [[ "$REQ_ID" == "$REQUEST_ID_1" ]]; then
        REQ_NAME="SLOW1"
        REQ_COLOR="$YELLOW"
    elif [[ "$REQ_ID" == "$REQUEST_ID_2" ]]; then
        REQ_NAME="SLOW2"
        REQ_COLOR="$YELLOW"
    else
        REQ_NAME="/api/me"
        REQ_COLOR="$CYAN"
    fi

    echo -e "${REQ_COLOR}Request: $REQ_NAME (request_id=$REQ_ID)${NC}"
    echo "──────────────────────────────────────────────────────────────"

    # Count acquisitions
    CONN_COUNT=$(echo "$REQ_LOGS" | grep -c "CONN_ACQUIRED")

    if [[ $CONN_COUNT -eq 0 ]]; then
        echo -e "${RED}  No connections acquired (request may have timed out)${NC}"
        echo ""
        continue
    fi

    echo "  Connections acquired: $CONN_COUNT"
    echo ""

    # Show connection details
    printf "  %-3s %-35s %-12s %-12s\n" "#" "Thread" "Wait (ms)" "Held (ms)"
    echo "  ────────────────────────────────────────────────────────────"

    INDEX=1
    ACQUIRED_LINES=$(echo "$REQ_LOGS" | grep "CONN_ACQUIRED")
    if [[ -n "$ACQUIRED_LINES" ]]; then
        while IFS= read -r line; do
            THREAD=$(echo "$line" | sed -n 's/.*\[\([^]]*\)\].*/\1/p' | cut -c1-35)
            WAIT_MS=$(echo "$line" | grep -oP 'wait_ms=\K[0-9]+')

            # Find corresponding release
            RELEASE_LINE=$(echo "$REQ_LOGS" | grep "CONN_RELEASED" | grep "\[$THREAD" | head -1)
            if [[ -n "$RELEASE_LINE" ]]; then
                HELD_MS=$(echo "$RELEASE_LINE" | grep -oP 'held_ms=\K[0-9]+')
                printf "  %-3s %-35s %-12s %-12s\n" "$INDEX" "$THREAD" "$WAIT_MS" "$HELD_MS"
            else
                printf "  %-3s %-35s %-12s %-12s\n" "$INDEX" "$THREAD" "$WAIT_MS" "(not released)"
            fi
            INDEX=$((INDEX + 1))
        done <<< "$ACQUIRED_LINES"
    fi

    # Highlight OSIV connection (main HTTP thread)
    OSIV_LINE=$(echo "$REQ_LOGS" | grep "CONN_RELEASED" | grep "http-nio" | head -1)
    if [[ -n "$OSIV_LINE" ]]; then
        OSIV_WAIT=$(echo "$OSIV_LINE" | grep -oP 'wait_ms=\K[0-9]+')
        OSIV_HELD=$(echo "$OSIV_LINE" | grep -oP 'held_ms=\K[0-9]+')
        echo "  ────────────────────────────────────────────────────────────"
        echo -e "  ${MAGENTA}OSIV connection: waited ${OSIV_WAIT}ms, held for ${OSIV_HELD}ms${NC}"

        if [[ "$REQ_NAME" == "SLOW1" ]] || [[ "$REQ_NAME" == "SLOW2" ]]; then
            echo -e "  ${RED}  → Held entire request duration (~35s) despite NOT needing DB!${NC}"
        fi
    fi

    echo ""
done

echo ""
echo -e "${CYAN}[Step 5] Checking for connection timeout errors...${NC}"
echo ""

# Check for timeout errors for /api/me request
# Use grep on full log with request_id to ensure we find it
TIMEOUT_ERROR=$(grep "request_id=$REQUEST_ID_3" "$DHIS2_LOG" 2>/dev/null | grep "Connection is not available, request timed out")

if [[ -n "$TIMEOUT_ERROR" ]]; then
    echo -e "${RED}✓ Connection timeout error found:${NC}"
    echo ""
    # Print the full error line without modification for clarity
    echo "$TIMEOUT_ERROR"
    echo ""
    echo -e "${YELLOW}→ /api/me waited 30s for connection, HikariCP timeout (pool exhausted: active=2, idle=0, waiting=0)${NC}"
else
    echo -e "${GREEN}No timeout errors found (request may have succeeded)${NC}"
fi

echo ""
echo -e "${BLUE}───────────────────────────────────────────────────────────────${NC}"
echo ""
echo -e "${CYAN}[Step 6] Analyzing HikariCP pool stats...${NC}"
echo ""

# Extract HikariCP logs
HIKARI_LOGS=$(tail -c +$((DHIS2_LOG_SIZE + 1)) "$DHIS2_LOG" | grep "HikariPool" | grep -E "(total=|active=)")

if [[ -n "$HIKARI_LOGS" ]]; then
    echo "HikariCP Pool Statistics:"
    echo "──────────────────────────────────────────────────────────────"
    while IFS= read -r line; do
        TIMESTAMP=$(echo "$line" | grep -oP '^\S+ \S+')
        STATS=$(echo "$line" | grep -oP 'HikariPool.*' | sed 's/HikariPool-1 - //')
        echo "  [$TIMESTAMP] $STATS"
    done <<< "$HIKARI_LOGS"
    echo "──────────────────────────────────────────────────────────────"
else
    echo "No HikariCP pool statistics found in logs"
fi

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}Analysis complete!${NC}"
echo ""
echo -e "${CYAN}Summary:${NC}"
echo ""
echo "Request Results:"
echo "  • SLOW1: Completed in ~${ELAPSED_1}s, held connection entire time (no DB needed!)"
echo "  • SLOW2: Completed in ~${ELAPSED_2}s, held connection entire time (no DB needed!)"
echo "  • /api/me: ${HTTP_CODE} in ${TIME_TOTAL_MS}ms (needs DB, waited for connection)"
echo ""
echo -e "${RED}OSIV Problem Demonstrated:${NC}"
echo "  • Pool size: 2 connections"
echo "  • SLOW1 + SLOW2 exhausted pool by holding connections for 35s each"
echo "  • Neither SLOW1 nor SLOW2 needed database access"
echo "  • Yet both held connections for ENTIRE request duration"
echo "  • /api/me couldn't get connection, had to wait/timeout"
echo ""
echo -e "${YELLOW}Key insight:${NC}"
echo "  OSIV opens DB connection at request start and holds it until request end,"
echo "  regardless of whether the request actually needs database access."
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
