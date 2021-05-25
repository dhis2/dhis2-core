#!/usr/bin/env sh

# Requires JDK 11

# Define DHIS2_HOME folder here or set it before you run this script
# DHIS2_HOME=<directory>
# Hostname or IP for Jetty to listen
DHIS2_HOSTNAME=localhost
# Port number for Jetty to listen
DHIS2_PORT=9090

echo "Starting build.."
echo "Note: JDK 11 or later is required"
echo "JAVA_HOME: $JAVA_HOME"
echo "DHIS2_HOME: $DHIS2_HOME"
echo "Hostname: $DHIS2_HOSTNAME"
echo "Port: $DHIS2_PORT"

# Compile API only and start the API server with embedded Jetty
mvn clean install -Pdev -Pjdk11 -T 100C
java -Djetty.host=$DHIS2_HOSTNAME -Djetty.http.port=$DHIS2_PORT -jar ./dhis-web-embedded-jetty/target/dhis-web-embedded-jetty.jar

