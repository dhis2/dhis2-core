#!/usr/bin/env bash
# Start DHIS2 with OSIV demo configuration
#
# Prerequisites:
#   1. Build DHIS2: ./dhis-2/build-dev.sh
#   2. Start PostgreSQL: docker compose up -d db

cd "$(dirname "$0")/.." || exit 1

DHIS2_HOME=$(pwd)/demo-osiv java \
  -Dlog4j2.configurationFile=$(pwd)/demo-osiv/log4j2.xml \
  -Dcom.zaxxer.hikari.housekeeping.periodMs=5000 \
  -Ddhis.skip.startup=true \
  -jar dhis-2/dhis-web-server/target/dhis.war
