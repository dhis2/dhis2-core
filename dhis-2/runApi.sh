#!/usr/bin/env sh

# Set your DHIS2 home folder here...
#DHIS2_HOME=
# Compile API only and start the API server with embedded Jetty
mvn clean install -Pdev -Pjdk11 -T 100C
java -jar ./dhis-web-embedded-jetty/target/dhis-web-embedded-jetty.jar

