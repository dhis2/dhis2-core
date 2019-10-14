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

BASE_IMAGE="$1"

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ARTIFACTS="${DIR}/artifacts"

TEMP="extract-$(date +%s)"




#
## funcs
#

setup () {
    # When extracting artifacts from an image we absolutely do not want
    # to run the risk of having a mix of artifacts, so if the dir
    # exists, we purge it. No exceptions.

    if [ -d "$ARTIFACTS" ]; then
        echo "Purging existing artifacts directory: ${ARTIFACTS}"
        rm -rf "$ARTIFACTS"
        echo "Done"
    fi

    echo "Creating artifacts directory: ${ARTIFACTS}"
    mkdir -p "$ARTIFACTS"
    echo "Done"
}

main () {
    echo "Creating temporary image: ${TEMP}"
    docker create --name "$TEMP" "$BASE_IMAGE"
    echo "Done"

    echo "Extracting artifacts..."
    docker cp "$TEMP":/srv/dhis2/dhis.war "${ARTIFACTS}/dhis.war"
    docker cp "$TEMP":/srv/dhis2/sha256sum.txt "${ARTIFACTS}/sha256sum.txt"
    docker cp "$TEMP":/srv/dhis2/md5sum.txt "${ARTIFACTS}/md5sum.txt"
    echo "Done"
}

cleanup () {
    echo "Removing temporary image: ${TEMP}"
    docker rm -f "$TEMP"
    echo "Done"
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
