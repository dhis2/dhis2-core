#!/usr/bin/env bash
# OSIV Demo - Connection Monitor (run in Terminal 2)
#
# This script monitors PostgreSQL connections and HikariCP timeout errors.
# Run osiv-demo-requests.sh in another terminal to generate requests.

DB_CONTAINER="${DB_CONTAINER:-core-dhis2-20411-db-1}"
DHIS2_LOG="${DHIS2_LOG:-$HOME/code/dhis2/core-DHIS2-20411/demo-osiv/logs/dhis.log}"
POLL_INTERVAL="${POLL_INTERVAL:-3}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

show_pg_activity() {
    local TIMESTAMP=$(date +%H:%M:%S)

    echo ""
    echo -e "${CYAN}[$TIMESTAMP]${NC} pg_stat_activity:"

    # Show detailed connection info
    docker exec "$DB_CONTAINER" psql -U dhis -d dhis -c "
        SELECT
            pid,
            application_name as app_name,
            state,
            CASE
                WHEN query = '' THEN '-'
                ELSE left(query, 40)
            END as query
        FROM pg_stat_activity
        WHERE datname='dhis'
          AND state IS NOT NULL
          AND application_name <> 'psql'
        ORDER BY
            CASE WHEN application_name LIKE 'demo-%' THEN 0 ELSE 1 END,
            backend_start
    " 2>/dev/null | grep -v "^(" | sed 's/^/  /'
}

clear
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  OSIV Demo - Connection Monitor${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo "Monitoring PostgreSQL pg_stat_activity every ${POLL_INTERVAL}s"
echo "Press Ctrl+C to stop"
echo ""
echo -e "${YELLOW}Legend:${NC}"
echo "  - 'demo-SLOW1/SLOW2' = Connections held by demo requests (OSIV)"
echo "  - 'PostgreSQL JDBC Driver' = Idle connections in pool"
echo "  - state=idle = Connection open but not executing query"
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"

# Start log watcher in background if log file exists
if [[ -f "$DHIS2_LOG" ]]; then
    tail -f "$DHIS2_LOG" 2>/dev/null | grep --line-buffered -E "(Pool stats|Connection is not available)" | while read line; do
        echo ""
        echo -e "${YELLOW}[HikariCP] $line${NC}"
    done &
    TAIL_PID=$!
    trap "kill $TAIL_PID 2>/dev/null" EXIT
fi

# Poll pg_stat_activity
while true; do
    show_pg_activity
    sleep "$POLL_INTERVAL"
done
