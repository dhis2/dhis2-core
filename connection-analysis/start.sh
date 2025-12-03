#!/usr/bin/env bash
# Start DHIS2
#
# Prerequisites:
#   1. Build DHIS2: ./build.sh
#   2. Start PostgreSQL/Prometheus: docker compose up --detach
#
# Environment variables:
#   OSIV_EXCLUDE_TRACKER - Whether to exclude /api/tracker/** from OSIV (default: true)
#                          Set to "false" to keep tracker in OSIV (OSIV ON test)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Default to excluding tracker from OSIV (OSIV OFF)
# Set OSIV_EXCLUDE_TRACKER=false for OSIV ON test (tracker with OSIV)
OSIV_EXCLUDE_TRACKER="${OSIV_EXCLUDE_TRACKER:-true}"

echo "Starting DHIS2 with osiv.exclude.tracker=$OSIV_EXCLUDE_TRACKER"

DHIS2_HOME="$SCRIPT_DIR" java \
  -Dlog4j2.configurationFile="$SCRIPT_DIR/log4j2.xml" \
  -Dcom.zaxxer.hikari.housekeeping.periodMs=5000 \
  -Ddhis.skip.startup=true \
  -Dosiv.exclude.tracker="$OSIV_EXCLUDE_TRACKER" \
  -jar "$SCRIPT_DIR/../dhis-2/dhis-web-server/target/dhis.war"
