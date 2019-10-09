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

IMAGE="$1"

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
TEMP="extract-$(date +%s)"

#
## funcs
#

cleanup () {
    docker rm -f "$TEMP"
}

main () {
    docker create --name "$TEMP" "$IMAGE"

    docker cp "$TEMP":/dhis.war "${DIR}/artifact/dhis.war"
    docker cp "$TEMP":/sha256sum.txt "${DIR}/artifact/sha256sum.txt"
    docker cp "$TEMP":/md5sum.txt "${DIR}/artifact/md5sum.txt"
}

trap cleanup EXIT
main
