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

. "${DIR}/containers-list.sh"





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
            echo "No SHA256 checksum found. Creating it..."
            sha256sum dhis.war > sha256sum.txt
        fi

        if [ -f "md5sum.txt" ]; then
            md5sum -c md5sum.txt
        else
            echo "No MD5 checksum found. Creating it..."
            md5sum dhis.war > md5sum.txt
        fi

        popd
    else
        echo "No existing artifact directory found"
        echo "Either run ./extract-artifacts.sh or provide a dhis.war"
        exit 1
    fi
}

build_default_container () {
    docker build \
        --tag "${CORE_IMAGE}-${TOMCAT_IMAGE}-${DEFAULT_TOMCAT_TAG}" \
        --file "${DIR}/../Dockerfile" \
        --build-arg IDENTIFIER="${IDENTIFIER}" \
        --build-arg WAR_SOURCE=local \
        "${DIR}/.."
}

build_debian_containers () {
    for TOMCAT_TAG in "${TOMCAT_DEBIAN_TAGS[@]}"; do
        docker build \
            --tag "${CORE_IMAGE}-${TOMCAT_IMAGE}-${TOMCAT_TAG}" \
            --file "${DIR}/../Dockerfile" \
            --build-arg DEBIAN_TOMCAT_IMAGE="${TOMCAT_IMAGE}:${TOMCAT_TAG}" \
            --build-arg IDENTIFIER="${IDENTIFIER}" \
            --build-arg WAR_SOURCE=local \
            "${DIR}/.."
    done
}

build_alpine_containers () {
    for TOMCAT_TAG in "${TOMCAT_ALPINE_TAGS[@]}"; do
        docker build \
            --tag "${CORE_IMAGE}-${TOMCAT_IMAGE}-${TOMCAT_TAG}" \
            --file "${DIR}/../Dockerfile" \
            --build-arg ALPINE_TOMCAT_IMAGE="${TOMCAT_IMAGE}:${TOMCAT_TAG}" \
            --build-arg IDENTIFIER="${IDENTIFIER}" \
            --build-arg WAR_SOURCE=local \
            "${DIR}/.."
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
