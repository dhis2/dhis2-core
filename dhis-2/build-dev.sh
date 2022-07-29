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

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
ROOT="$DIR/.."
ARTIFACTS="$ROOT/docker/artifacts"

print() {
    echo -e "\033[1m$1\033[0m" 1>&2
}

#
## The Business
#

# Requires maven to be on the classpath
# Skips clean and test phases
# Also skips copying test resources and compiling tests

print "Building dhis2-core..."

export MAVEN_OPTS="-Dhttp.keepAlive=false -Dmaven.wagon.http.pool=false -Dmaven.wagon.http.retryHandler.class=standard -Dmaven.wagon.http.retryHandler.count=3 -Dmaven.wagon.httpconnectionManager.ttlSeconds=25 -Dmaven.test.skip=true"
# mvn clean install --batch-mode --no-transfer-progress --threads 1C -Pdev -f "$DIR"/pom.xml -pl -dhis-web-embedded-jetty
# mvn clean install --batch-mode --no-transfer-progress --threads 1C -Pdev -f "$DIR"/dhis-web/pom.xml

rm -rf "$ARTIFACTS/*"
mkdir -p "$ARTIFACTS"
cp -f "$DIR/dhis-web/dhis-web-portal/target/dhis.war" "$ARTIFACTS/dhis.war"

print "Build succeeded, creating Docker image $IMAGE:$TAG..."

cd $ARTIFACTS
sha256sum ./dhis.war >./sha256sum.txt
md5sum ./dhis.war >./md5sum.txt

ONLY_DEFAULT=1 $ROOT/docker/build-containers.sh $IMAGE:$TAG $TAG

print "Successfully created Docker image $IMAGE:$TAG"

# if test -z $D2CLUSTER; then
#     print "No cluster name specified, skipping deploy"
# else
#     print "Deploying to d2 cluster $D2CLUSTER..."
#
#     d2 cluster up $D2CLUSTER --image $IMAGE:$TAG
#     d2 cluster logs $D2CLUSTER
# fi
