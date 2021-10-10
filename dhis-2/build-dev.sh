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

D2CLUSTER="${1:-}"
IMAGE=dhis2/core
TAG=local

print() {
    echo -e "\033[1m$1\033[0m" 1>&2
}

print "Creating Docker image $IMAGE:$TAG..."

docker build --tag $IMAGE:$TAG .

print "Successfully created Docker image $IMAGE:$TAG"

if test -z $D2CLUSTER; then
    print "No cluster name specified, skipping deploy"
else
    print "Deploying to d2 cluster $D2CLUSTER..."

    d2 cluster up $D2CLUSTER --image $IMAGE:$TAG
    d2 cluster logs $D2CLUSTER
fi
