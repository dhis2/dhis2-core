#!/bin/sh

# Requires maven to be on the classpath
# Invokes the dev profile which skips tests and disables compression of war artifacts for a speedy build

mvn clean install --batch-mode --no-transfer-progress -DskipTests
mvn clean install --batch-mode --no-transfer-progress -DskipTests -f dhis-web/pom.xml -U
