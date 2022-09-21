#!/usr/bin/env bash

set -e

# Requires JDK 11

# Hostname or IP for DHIS2/Jetty to listen
DHIS2_HOSTNAME=localhost
# Port number for DHIS2/Jetty to listen
DEFAULT_DHIS2_PORT=9090
DEFAULT_DHIS2_HOME=/opt/dhis2

DHIS2_PORT=${2:-$DEFAULT_DHIS2_PORT}
#DHIS2_HOME=${1:-$DEFAULT_DHIS2_HOME}

echo -e "Usage: run-api.sh ([DHIS2_HOME_FOLDER] [DHIS2_PORT])\n"
echo -e "Note: JDK 11 or later is required!\n"
# Define DHIS2_HOME folder here or set it before you run this script
if [[ -z "${DHIS2_HOME}" ]]; then
  if [ -n "$1" ]; then
    echo "DHIS2_HOME environment variable is not set, using supplied argument nr 1."
    DHIS2_HOME=$1
  else
    echo -e "DHIS2_HOME is not set and no argument supplied either, using default '/opt/dhis2' as the DHIS2 home folder.\n"
    DHIS2_HOME=$DEFAULT_DHIS2_HOME
  fi
else
  echo -e "Environment variable DHIS2_HOME is set.\n"
fi

echo "JAVA_HOME: $JAVA_HOME"
echo "DHIS2_HOME: $DHIS2_HOME"
echo "Hostname: $DHIS2_HOSTNAME"
echo -e "Port: $DHIS2_PORT\n"

[ ! -d $DHIS2_HOME ] && echo "DHIS2_HOME directory '$DHIS2_HOME' DOES NOT exists, aborting..." && exit 1;
[ ! -f "$DHIS2_HOME/dhis.conf" ] && echo "dhis.conf in directory '$DHIS2_HOME' DOES NOT exists, aborting..." && exit 1;

read -p "Do you want to skip compile? (if yes press y/Y) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
  java \
    -Ddhis2.home=$DHIS2_HOME \
    -Djetty.host=$DHIS2_HOSTNAME \
    -Djetty.http.port=$DHIS2_PORT \
    -jar "$(dirname "$0")/dhis-web-embedded-jetty/target/dhis-web-embedded-jetty.jar"
  exit 0;
fi

mvn clean install \
    -f "$(dirname "$0")/pom.xml" \
    --batch-mode \
    -Pdev -T 100C \
    -DskipTests -Dmaven.test.skip=true -Dmaven.site.skip=true -Dmaven.javadoc.skip=true
java \
    -Ddhis2.home=$DHIS2_HOME \
    -Djetty.host=$DHIS2_HOSTNAME \
    -Djetty.http.port=$DHIS2_PORT \
    -jar "$(dirname "$0")/dhis-web-embedded-jetty/target/dhis-web-embedded-jetty.jar"
