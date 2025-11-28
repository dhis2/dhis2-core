#!/usr/bin/env bash
# Start DHIS2
#
# Prerequisites:
#   1. Build DHIS2: ./build.sh
#   2. Start PostgreSQL/Prometheus: docker compose up --detach

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

DHIS2_HOME="$SCRIPT_DIR" java \
  -Dlog4j2.configurationFile="$SCRIPT_DIR/log4j2.xml" \
  -Dcom.zaxxer.hikari.housekeeping.periodMs=5000 \
  -Ddhis.skip.startup=true \
  -jar "$SCRIPT_DIR/../dhis-2/dhis-web-server/target/dhis.war"
