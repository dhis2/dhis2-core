#!/bin/bash

# Requires maven to be on the classpath
# Invokes the dev profile which skips tests and disables compression of war artifacts for a speedy build
DEFUALT_JDK=jdk11
JDK="${1:-$DEFUALT_JDK}"

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)

mvn clean install -f ./dhis-web-server/pom.xml -Pdev -T 100C -Dparallel=all -DperCoreThreadCount=false -DthreadCount=16 -DskipTests -Dmaven.test.skip=true -Dmaven.site.skip=true -Dmaven.javadoc.skip=true
if [ $? -eq 0 ]; then
  touch ./lastBon
#  notify-send DHIS-SERVICE DONE!
else
    notify-send -u critical "DHIS2-SERVICE FAILED!"
    printf "\n Fail! Service compile failed!\n"
    exit 1;
fi

if [ "$2" == "CORE" ]; then
    exit;
fi
#
#mvn clean install -Pdev -f dhis-web/pom.xml -T 100C -DskipTests -Dmaven.test.skip=true -Dmaven.site.skip=true -Dmaven.javadoc.skip=true
#if [ $? -eq 0 ]; then
#  touch ./lastBon
##  notify-send DHIS-WEB DONE!
#else
#  notify-send -u critical "DHIS2-WEB FAILED!"
#  printf "\n Fail! Web compile failed!\n"
#  exit 1;
#fi

notify-send -u normal "DHIS2 COMPILE DONE Branch: $CURRENT_BRANCH Date: $(date) "
