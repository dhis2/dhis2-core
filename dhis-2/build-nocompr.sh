#!/bin/sh

# Requires maven to be on the classpath
# Invokes the dev profile which skips tests and disables compression of war artifacts for a speedy build

mvn clean install --batch-mode --no-transfer-progress
mvn clean install --batch-mode --no-transfer-progress -f dhis-web/pom.xml -U
