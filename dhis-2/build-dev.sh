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
IMAGE=dhis2/core-dev
TAG=local

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"

print() {
    echo -e "\033[1m$1\033[0m" 1>&2
}

print "Building dhis2-core and Docker image..."

export MAVEN_OPTS="-Dhttp.keepAlive=false -Dmaven.wagon.http.pool=false -Dmaven.wagon.http.retryHandler.class=standard -Dmaven.wagon.http.retryHandler.count=3 -Dmaven.wagon.httpconnectionManager.ttlSeconds=25"
mvn clean install --threads 2C -DskipTests -Dmaven.test.skip=true -f "${DIR}/pom.xml" -pl -dhis-web-embedded-jetty,-dhis-test-integration,-dhis-test-coverage
mvn clean install --threads 2C -DskipTests -Dmaven.test.skip=true -f "${DIR}/dhis-web/pom.xml"
mvn -DskipTests -Dmaven.test.skip=true -f "${DIR}/dhis-web/dhis-web-portal/pom.xml" jib:dockerBuild

if test -z "$D2CLUSTER"; then
    print "No cluster name specified, skipping deploy"
else
    print "Deploying to d2 cluster $D2CLUSTER..."

    d2 cluster up "$D2CLUSTER" --image $IMAGE:$TAG
    d2 cluster logs "$D2CLUSTER"
fi
