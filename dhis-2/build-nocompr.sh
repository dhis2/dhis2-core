#!/bin/sh

# Requires maven to be on the classpath
# Invokes the dev profile which skips tests and disables compression of war artifacts for a speedy build

mvn clean install -Pdev --update-snapshots
mvn clean install -Pdev -f dhis-web/pom.xml --update-snapshots
