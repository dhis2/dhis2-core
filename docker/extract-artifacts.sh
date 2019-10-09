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
TEMP="extract-$(date +%s)"
TARGET="${DIR}/artifacts"





#
## funcs
#

setup () {
    if [ -d "$TARGET" ]; then
        echo "Purging existing artifacts directory: ${TARGET}"
        rm -rf "$TARGET"
        echo "Done"
    fi

    echo "Creating artifacts directory: ${TARGET}"
    mkdir -p "$TARGET"
    echo "Done"
}

main () {
    echo "Creating temporary image: ${TEMP}"
    docker create --name "$TEMP" "$CORE_IMAGE"
    echo "Done"

    echo "Extracting artifacts..."
    docker cp "$TEMP":/dhis.war "${TARGET}/dhis.war"
    docker cp "$TEMP":/sha256sum.txt "${TARGET}/sha256sum.txt"
    docker cp "$TEMP":/md5sum.txt "${TARGET}/md5sum.txt"
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
