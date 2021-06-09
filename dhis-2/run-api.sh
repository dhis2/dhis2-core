#!/usr/bin/env bash

# Requires JDK 11

set -e

echo -e "Usage: run-api.sh DHIS2_HOME\n"

# Define DHIS2_HOME folder here or set it before you run this script
if [[ -z "${DEPLOY_ENV}" ]]; then
  if [ -n "$1" ]; then
    echo "DHIS2_HOME is not set, using supplied argument."
    DHIS2_HOME=$1
  else
    echo -e "DHIS2_HOME is not set and no argument supplied either, using default '/opt/dhis2' as the DHIS2 home folder.\n"
    DHIS2_HOME=/opt/dhis2
  fi
else
  echo -e "Environment variable DHIS2_HOME is set.\n"
fi



# Hostname or IP for DHIS2/Jetty to listen
DHIS2_HOSTNAME=localhost

# Port number for DHIS2/Jetty to listen
DHIS2_PORT=9090

echo "Starting build.."
echo "Note: JDK 11 or later is required"
echo "JAVA_HOME: $JAVA_HOME"
echo "DHIS2_HOME: $DHIS2_HOME"
echo "Hostname: $DHIS2_HOSTNAME"
echo -e "Port: $DHIS2_PORT\n"

[ ! -d $DHIS2_HOME ] && echo "DHIS2_HOME directory '$DHIS2_HOME' DOES NOT exists, aborting..." && exit 1;
[ ! -f "$DHIS2_HOME/dhis.conf" ] && echo "dhis.conf in directory '$DHIS2_HOME' DOES NOT exists, aborting..." && exit 1;

read -p "Do you wan to compile first? (if yes press y/Y to continue) " -n 1 -r
echo    # (optional) move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]; then
  # Compile API only and start the API server with embedded Jetty
  mvn clean install -Pdev -Pjdk11 -T 100C
fi

java -Ddhis2.home=$DHIS2_HOME -Djetty.host=$DHIS2_HOSTNAME -Djetty.http.port=$DHIS2_PORT -jar ./dhis-web-embedded-jetty/target/dhis-web-embedded-jetty.jar

/home/nacom/develop/DHIS2/dhis2_homes/latest
