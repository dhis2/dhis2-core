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

CORE_IMAGE="$1"

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ARTIFACTS="${DIR}/artifacts"

TOMCAT_IMAGE="tomcat"



#
## tomcat tags
#

default_tomcat_tag="8.5.46-jdk8-openjdk-slim"

tomcat_debian_tags=(
    "9.0-jdk8-openjdk-slim"
    "8.5-jdk8-openjdk-slim"
    "8.0-jre8-slim"
)

tomcat_alpine_tags=(
    "8.5.34-jre8-alpine"
)





#
## funcs
#

extraction () {
    . $DIR/extract-artifacts.sh "$CORE_IMAGE" |
        while IFS= read -r LINE; do
            echo "$LINE"
        done

    cd $(pwd)
}

setup () {
    if [ -d "$ARTIFACTS" ]; then
        pushd "$ARTIFACTS"

        if [ -f "dhis.war" ]; then
            echo "Using existing 'dhis.war' to build."
        else
            echo "No 'dhis.war' file found."
            echo "Attempting extraction from '${CORE_IMAGE}'"

            extraction
        fi

        echo "Checksum valiations:"

        if [ -f "sha256sum.txt" ]; then
            sha256sum -c sha256sum.txt
        else
            echo "Skipping... No SHA256 checksum found."
        fi

        if [ -f "md5sum.txt" ]; then
            md5sum -c md5sum.txt
        else
            echo "Skipping... No MD5 checksum found."
        fi

        popd
    else
        echo "No existing artifact directory found"
        echo "Attempting extraction from '${CORE_IMAGE}'"

        extraction
    fi
}

build () {
    local TAG=$1
    local TC_TAG=$2
    local TYPE=$3

    docker build \
        --tag "${TAG}" \
        --file "${DIR}/tomcat-${TYPE}/Dockerfile" \
        --build-arg TOMCAT_IMAGE="${TOMCAT_IMAGE}:${TC_TAG}" \
        "$DIR"
}

main () {
    build "$CORE_IMAGE" "$default_tomcat_tag" "debian"

    for TOMCAT_TAG in "${tomcat_debian_tags[@]}"; do
        build "${CORE_IMAGE}-${TOMCAT_IMAGE}-${TOMCAT_TAG}" "$TOMCAT_TAG" "debian"
    done

    for TOMCAT_TAG in "${tomcat_alpine_tags[@]}"; do
        build "${CORE_IMAGE}-${TOMCAT_IMAGE}-${TOMCAT_TAG}" "$TOMCAT_TAG" "alpine"
    done
}





#
## run the script
#


setup
main
