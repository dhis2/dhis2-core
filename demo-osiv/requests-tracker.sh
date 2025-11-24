#!/usr/bin/env bash
# OSIV Demo - TrackedEntities Request Generator
#
# Demonstrates how the TrackedEntityAggregate's async query pattern
# combined with OSIV can exhaust the connection pool.
#
# With pool_size=2:
# - OSIV holds 1 connection on HTTP thread
# - TrackedEntityAggregate dispatches 4+ async queries (each needs a connection)
# - Async queries block waiting for connections → requests hang/timeout

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BASE_URL="${BASE_URL:-http://localhost:8080}"
AUTH="${AUTH:-admin:district}"
TRACKER_PROGRAM="${TRACKER_PROGRAM:-ur1Edk5Oe2n}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  OSIV Demo - TrackedEntities Connection Exhaustion${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${YELLOW}Configuration:${NC}"
echo "  Pool size: 2 connections"
echo "  TrackedEntityAggregate: dispatches ~4 async queries per request"
echo ""
echo -e "${YELLOW}Expected behavior:${NC}"
echo "  - First request: OSIV grabs 1 conn, async queries need more → blocks"
echo "  - Second request: immediate timeout (no connections available)"
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"

# Test 1: searchTEByName request (from TrackerTest.java)
echo ""
echo -e "${CYAN}[Test 1] searchTEByName request (like operator - slow!)${NC}"
echo "Testing TE search by name with 'like:joh' (should return results and trigger async aggregate)..."
echo ""

REQUEST_ID="$(date +%s%3N)"
echo "Request ID: $REQUEST_ID"
echo ""

time curl -s -w "\nHTTP Status: %{http_code}\nTime: %{time_total}s\n" \
    -u "$AUTH" \
    -H "X-Request-ID: $REQUEST_ID" \
    "$BASE_URL/api/tracker/trackedEntities?filter=w75KJ2mc4zz:like:joh&fields=attributes,enrollments,trackedEntity,orgUnit&program=$TRACKER_PROGRAM&page=1&pageSize=5&orgUnitMode=ACCESSIBLE" \
    | head -20

# echo ""
# echo -e "${BLUE}───────────────────────────────────────────────────────────────${NC}"
#
# # Test 2: Two concurrent notFoundTEByName requests
# echo ""
# echo -e "${CYAN}[Test 2] Two concurrent notFoundTEByName requests${NC}"
# echo "Starting two requests simultaneously..."
# echo ""
#
# curl -s -w "\n[Request 1] HTTP: %{http_code}, Time: %{time_total}s\n" \
#     -u "$AUTH" \
#     "$BASE_URL/api/tracker/trackedEntities?filter=w75KJ2mc4zz:like:notfoundname&fields=attributes,enrollments,trackedEntity,orgUnit&program=$TRACKER_PROGRAM&page=1&pageSize=5&orgUnitMode=ACCESSIBLE" &
# PID1=$!
#
# curl -s -w "\n[Request 2] HTTP: %{http_code}, Time: %{time_total}s\n" \
#     -u "$AUTH" \
#     "$BASE_URL/api/tracker/trackedEntities?filter=w75KJ2mc4zz:like:notfoundname&fields=attributes,enrollments,trackedEntity,orgUnit&program=$TRACKER_PROGRAM&page=1&pageSize=5&orgUnitMode=ACCESSIBLE" &
# PID2=$!
#
# echo "Waiting for requests to complete (may timeout after 30s)..."
# wait $PID1 $PID2 2>/dev/null
#
# echo ""
# echo -e "${BLUE}───────────────────────────────────────────────────────────────${NC}"
#
# # Test 3: notFoundTEByName + simple request
# echo ""
# echo -e "${CYAN}[Test 3] notFoundTEByName + /api/me (simple request)${NC}"
# echo "Does a heavy tracker request block simple requests?"
# echo ""
#
# curl -s -o /dev/null \
#     -u "$AUTH" \
#     "$BASE_URL/api/tracker/trackedEntities?filter=w75KJ2mc4zz:like:notfoundname&fields=attributes,enrollments,trackedEntity,orgUnit&program=$TRACKER_PROGRAM&page=1&pageSize=5&orgUnitMode=ACCESSIBLE" &
# PID1=$!
#
# sleep 0.5
#
# echo "notFoundTEByName running in background, trying /api/me..."
# time curl -s -w "HTTP: %{http_code}\n" \
#     -u "$AUTH" \
#     "$BASE_URL/api/me" \
#     | head -5
#
# wait $PID1 2>/dev/null
#
# echo ""
# echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
# echo -e "${GREEN}Done. Check monitor.sh output for connection states.${NC}"
