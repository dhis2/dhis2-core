#!/usr/bin/env bash

#
## bash environment
#
if test "$BASH" = "" || "$BASH" -uc "a=();true \"\${a[@]}\"" 2>/dev/null; then
    # Bash 4.4, Zsh
    set -euo pipefail
else
    # Bash 4.3 and older chokes on empty arrays with set -u.
    set -eo pipefail
fi
shopt -s nullglob globstar

#
## script environment
#

DIR="$( dirname $( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd ) )"

TOMCAT_IMAGE="tomcat"
ARTIFACT_DIR=${ARTIFACT_DIR-"${DIR}/docker/artifact"}
WAR_FILE=${WAR_FILE-""}

#
## tomcat tags
#

latest_tomcat_tags=(
    "9.0-jdk8-openjdk-slim"
    "8.5-jdk8-openjdk-slim"
    "8.0-jre8-slim"
)

#
## builds
#

if [ -z "$WAR_FILE" ]; then
    echo "No custom WAR-file passed to script, extracting from build"
    docker build --tag "${CORE_IMAGE}:${CORE_TAG}" --file "${DIR}/docker/core/Dockerfile" "$DIR"

    mkdir -p "$ARTIFACT_DIR"
    WAR_FILE="${ARTIFACT_DIR}/dhis.war"
    docker create --name temp_extract "${CORE_IMAGE}:${CORE_TAG}"
    docker cp temp_extract:/dhis.war "$WAR_FILE"
    docker rm -f temp_extract
fi

for TOMCAT_TAG in "${latest_tomcat_tags[@]}"; do
    docker build \
        --tag "${CORE_IMAGE}:${CORE_TAG}-${TOMCAT_TAG}" \
        --file "${DIR}/docker/tomcat-debian/Dockerfile" \
        --build-arg WAR_FILE="$WAR_FILE" \
        --build-arg TOMCAT_IMAGE="${TOMCAT_IMAGE}:${TOMCAT_TAG}" \
        "$DIR"
done
