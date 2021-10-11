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
## config
#

. "${DIR}/containers-list.sh"





#
## funcs
#

publish () {
    local IMAGE=$1

    echo "Pushing $IMAGE"
    docker push "$IMAGE"
}

publish_debian_containers () {
    # publish the default container image
    publish "$CORE_IMAGE"

    # publish the variants
    for TOMCAT_TAG in "${TOMCAT_DEBIAN_TAGS[@]}"; do
        publish "${CORE_IMAGE}-${TOMCAT_IMAGE}-${TOMCAT_TAG}"
    done
}

publish_alpine_containers () {
    # publish the variants
    for TOMCAT_TAG in "${TOMCAT_ALPINE_TAGS[@]}"; do
        publish "${CORE_IMAGE}-${TOMCAT_IMAGE}-${TOMCAT_TAG}"
    done
}

main () {
    publish_debian_containers
    publish_alpine_containers
}





#
## run the script
#

main
