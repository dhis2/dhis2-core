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




#
## tomcat tags
#

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

setup () {
    if [ -d "${DIR}/artifacts" ]; then
        pushd "${DIR}/artifacts"

        echo "Checksum valiations:"

        if [ -f sha256sum.txt ]; then
            sha256sum -c sha256sum.txt
        else
            echo "Skipping... No SHA256 checksum found."
        fi

        if [ -f md5sum.txt ]; then
            md5sum -c md5sum.txt
        else
            echo "Skipping... No MD5 checksum found."
        fi

        popd
    else
        echo "No existing artifacts found, attempting extraction"
        . "${DIR}/extract-artifacts.sh ${CORE_IMAGE}"
    fi
}

main () {
    for TOMCAT_TAG in "${tomcat_debian_tags[@]}"; do
        docker build \
            --tag "${CORE_IMAGE}-${TOMCAT_TAG}" \
            --file "${DIR}/tomcat-debian/Dockerfile" \
            --build-arg TOMCAT_IMAGE="tomcat:${TOMCAT_TAG}" \
            "$DIR"
    done

    for TOMCAT_TAG in "${tomcat_alpine_tags[@]}"; do
        docker build \
            --tag "${CORE_IMAGE}-${TOMCAT_TAG}" \
            --file "${DIR}/tomcat-alpine/Dockerfile" \
            --build-arg TOMCAT_IMAGE="tomcat:${TOMCAT_TAG}" \
            "$DIR"
    done
}

cleanup () {
}





#
## hook up cleanup trap func
#

trap cleanup EXIT





#
## run the script
#


setup
main
