#!/bin/sh

# Requires maven to be on the classpath
# Skips test phase

mvn clean install -DskipTests=true -pl -dhis-web-embedded-jetty
mvn clean install -DskipTests=true -f dhis-web/pom.xml -U


