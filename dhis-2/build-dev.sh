#!/usr/bin/env bash
# Builds DHIS2 war and Docker image for development use

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

# Choosing this approach over automatically activating a maven profile based on the architecture.
# This is to not risk running both the jibBuild and jibBuildArmOnly profiles in our pipelines.
# There might be ways like using https://maven.apache.org/enforcer/enforcer-rules/requireActiveProfile.html
# to prevent that but they would require more work.
ARCH=$(mvn help:system | grep "os\.arch")
JIB_PROFILE=
if [[ "$ARCH" == *arm64* || "$ARCH" == *aarch64* ]]; then
  JIB_PROFILE="-P jibBuildArmOnly"
fi

print() {
    echo -e "\033[1m$1\033[0m" 1>&2
}

print "Building dhis2-core and Docker image..."

export MAVEN_OPTS="-Dhttp.keepAlive=false -Dmaven.wagon.http.pool=false -Dmaven.wagon.http.retryHandler.class=standard -Dmaven.wagon.http.retryHandler.count=3 -Dmaven.wagon.httpconnectionManager.ttlSeconds=25"
mvn clean install --threads 2C -DskipTests -Dmaven.test.skip=true -f "${DIR}/pom.xml" -pl -dhis-web-embedded-jetty,-dhis-test-integration,-dhis-test-coverage
mvn clean install --threads 2C -DskipTests -Dmaven.test.skip=true -f "${DIR}/dhis-web/pom.xml"
mvn -DskipTests -Dmaven.test.skip=true -f "${DIR}/dhis-web/dhis-web-portal/pom.xml" jib:dockerBuild $JIB_PROFILE

if test -z "$D2CLUSTER"; then
    print "No cluster name specified, skipping deploy"
else
    print "Deploying to d2 cluster $D2CLUSTER..."

    d2 cluster up "$D2CLUSTER" --image $IMAGE:$TAG
    d2 cluster logs "$D2CLUSTER"
fi
