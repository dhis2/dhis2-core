#!/bin/sh

# Requires maven to be on the classpath
# Invokes the dev profile which skips tests and disables compression of war artifacts for a speedy build

mvn clean install -Pdev -Pjdk11
mvn clean install -Pdev -Pjdk11 -f dhis-web/pom.xml -U
