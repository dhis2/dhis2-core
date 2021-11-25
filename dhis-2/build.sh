#!/bin/sh

# Requires maven to be on the classpath
# Skips test phase

mvn clean install --batch-mode --no-transfer-progress -DskipTests=true -pl -dhis-web-embedded-jetty
mvn clean install --batch-mode --no-transfer-progress -DskipTests=true -f dhis-web/pom.xml -U


