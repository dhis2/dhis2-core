#!/usr/bin/env sh

# You can define your DHIS2 home folder here or set it before you run this script.
#DHIS2_HOME=
DHIS2_PORT=9090
DHIS2_HOSTNAME_OR_IP=localhost
# Compile API only and start the API server with embedded Jetty
mvn clean install -Pdev -Pjdk11 -T 100C
java -Djetty.host=$DHIS2_HOSTNAME_OR_IP -Djetty.http.port=$DHIS2_PORT -jar ./dhis-web-embedded-jetty/target/dhis-web-embedded-jetty.jar

