#!/usr/bin/env bash
# OSIV Demo - Request Generator (run in Terminal 1)
#
# This script sends requests to demonstrate OSIV connection holding.
# Run osiv-demo-monitor.sh in another terminal to see the DB state.

set -e

DHIS2_URL="${DHIS2_URL:-http://localhost:9090}"
DHIS2_AUTH="${DHIS2_AUTH:-admin:district}"
SLEEP_MS="${SLEEP_MS:-45000}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  OSIV Demo - Request Generator${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo "This terminal sends requests. Watch osiv-demo-monitor.sh in another terminal."
echo ""
echo "Configuration:"
echo "  - Pool size: 2 connections"
echo "  - Sleep duration: ${SLEEP_MS}ms"
echo ""

read -p "Press Enter to start SLOW1 request..."

echo ""
echo -e "${CYAN}[$(date +%H:%M:%S)]${NC} ${YELLOW}Starting SLOW1...${NC}"
echo "  curl -u admin:district '${DHIS2_URL}/api/debug/slowNoDb?sleepMs=${SLEEP_MS}&requestId=SLOW1'"
curl -s -u "$DHIS2_AUTH" "${DHIS2_URL}/api/debug/slowNoDb?sleepMs=${SLEEP_MS}&requestId=SLOW1" &
PID1=$!
sleep 1
echo -e "${GREEN}  → SLOW1 running (PID: $PID1)${NC}"

echo ""
read -p "Press Enter to start SLOW2 request..."

echo ""
echo -e "${CYAN}[$(date +%H:%M:%S)]${NC} ${YELLOW}Starting SLOW2...${NC}"
echo "  curl -u admin:district '${DHIS2_URL}/api/debug/slowNoDb?sleepMs=${SLEEP_MS}&requestId=SLOW2'"
curl -s -u "$DHIS2_AUTH" "${DHIS2_URL}/api/debug/slowNoDb?sleepMs=${SLEEP_MS}&requestId=SLOW2" &
PID2=$!
sleep 1
echo -e "${GREEN}  → SLOW2 running (PID: $PID2)${NC}"

echo ""
echo -e "${RED}Both connections should now be held. Pool is exhausted!${NC}"
echo ""
read -p "Press Enter to try /api/me (will wait for connection or timeout)..."

echo ""
echo -e "${CYAN}[$(date +%H:%M:%S)]${NC} ${YELLOW}Attempting /api/me...${NC}"
echo "  This will block until a connection becomes available or HikariCP times out (30s)"
echo ""

START=$(date +%s)
RESULT=$(curl -s -w "\n%{http_code}" -u "$DHIS2_AUTH" "${DHIS2_URL}/api/me" --max-time 35 2>&1)
END=$(date +%s)
ELAPSED=$((END - START))

HTTP_CODE=$(echo "$RESULT" | tail -1)
BODY=$(echo "$RESULT" | head -n -1)

echo ""
echo -e "${CYAN}[$(date +%H:%M:%S)]${NC} Request completed after ${ELAPSED}s"
echo -e "  HTTP Status: ${HTTP_CODE}"

if [[ "$HTTP_CODE" == "200" ]]; then
    echo -e "${GREEN}  → Got response (a connection became available)${NC}"
elif [[ "$HTTP_CODE" == "500" ]]; then
    echo -e "${RED}  → 500 Error: Connection pool timeout!${NC}"
    echo "$BODY" | head -3
else
    echo -e "${RED}  → Unexpected status${NC}"
    echo "$BODY" | head -3
fi

echo ""
echo -e "${YELLOW}Cleaning up...${NC}"
kill $PID1 $PID2 2>/dev/null || true
wait $PID1 $PID2 2>/dev/null || true
echo -e "${GREEN}Done.${NC}"
