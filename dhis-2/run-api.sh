#!/usr/bin/env sh

# Requires JDK11

# Define DHIS2_HOME folder here or set it before you run this script
# DHIS2_HOME=<directory>
# Port number for Jetty to serve the API
DHIS2_PORT=9090
DHIS2_HOSTNAME_OR_IP=localhost

echo "JAVA_HOME: $JAVA_HOME"
echo "DHIS2_HOME: $DHIS2_HOME"
echo "Hostname: $DHIS2_HOSTNAME_OR_IP"

# Compile API only and start the API server with embedded Jetty
mvn clean install -Pdev -Pjdk11 -T 100C
java -Djetty.host=$DHIS2_HOSTNAME_OR_IP -Djetty.http.port=$DHIS2_PORT -jar ./dhis-web-embedded-jetty/target/dhis-web-embedded-jetty.jar

