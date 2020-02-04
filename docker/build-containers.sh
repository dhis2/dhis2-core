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
IDENTIFIER="$2"

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ARTIFACTS="${DIR}/artifacts"





#
## config
#

. "${DIR}/shared/containers-list.sh"





#
## funcs
#

setup () {
    if [ -d "$ARTIFACTS" ]; then
        pushd "$ARTIFACTS"

        if [ -f "dhis.war" ]; then
            echo "Using existing 'dhis.war' to build."
        else
            echo "No 'dhis.war' file found."
            echo "Either run ./extract-artifacts.sh or provide a dhis.war"
            exit 1
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
        echo "Either run ./extract-artifacts.sh or provide a dhis.war"
        exit 1
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
        --build-arg IDENTIFIER="${IDENTIFIER}" \
        "$DIR"
}

build_default_container () {
    build "$CORE_IMAGE" "$DEFAULT_TOMCAT_TAG" "debian"
}

build_debian_containers () {
    for TOMCAT_TAG in "${TOMCAT_DEBIAN_TAGS[@]}"; do
        build "${CORE_IMAGE}-${TOMCAT_IMAGE}-${TOMCAT_TAG}" "$TOMCAT_TAG" "debian"
    done
}

build_alpine_containers () {
    for TOMCAT_TAG in "${TOMCAT_ALPINE_TAGS[@]}"; do
        build "${CORE_IMAGE}-${TOMCAT_IMAGE}-${TOMCAT_TAG}" "$TOMCAT_TAG" "alpine"
    done
}

main () {
    build_default_container

    # checks if ONLY_DEFAULT is unset
    if [ -z ${ONLY_DEFAULT+x} ]; then
        build_debian_containers
        build_alpine_containers
    fi
}





#
## run the script
#

setup
main
