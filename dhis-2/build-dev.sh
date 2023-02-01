#!/usr/bin/env bash
# Builds DHIS2 war and Docker image for development use

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"

D2CLUSTER="${1:-}"
IMAGE=dhis2/core-dev
TAG=local
BUILD_REVISION=$(git --git-dir "$DIR/../.git" rev-parse HEAD)
BUILD_BRANCH=$(git --git-dir "$DIR/../.git" branch --show-current)

# Choosing this approach over automatically activating a maven profile based on the architecture.
# This is to not risk running both the jibBuild and jibBuildArmOnly profiles in our pipelines.
# There might be ways like using https://maven.apache.org/enforcer/enforcer-rules/requireActiveProfile.html
# to prevent that but they would require more work.
ARCH=$(mvn help:system | grep "os\.arch")
JIB_PROFILE=
if [[ "$ARCH" == *arm64* || "$ARCH" == *aarch64* ]]; then
  JIB_PROFILE="-P jibBuildArmOnly"
fi

echo "Building dhis2-core and Docker image..."

export MAVEN_OPTS="-Dhttp.keepAlive=false -Dmaven.wagon.http.pool=false -Dmaven.wagon.http.retryHandler.class=standard -Dmaven.wagon.http.retryHandler.count=3 -Dmaven.wagon.httpconnectionManager.ttlSeconds=25"
mvn clean install --threads 2C -DskipTests -Dmaven.test.skip=true -f "${DIR}/pom.xml" -pl -dhis-web-embedded-jetty,-dhis-test-integration,-dhis-test-coverage
mvn clean install --threads 2C -DskipTests -Dmaven.test.skip=true -f "${DIR}/dhis-web/pom.xml"
mvn -DskipTests -Dmaven.test.skip=true -f "${DIR}/dhis-web/dhis-web-portal/pom.xml" jib:dockerBuild $JIB_PROFILE \
  -Djib.container.labels=DHIS2_BUILD_REVISION="${BUILD_REVISION}",DHIS2_BUILD_BRANCH="${BUILD_BRANCH}"

if test -z "$D2CLUSTER"; then
    echo "No cluster name specified, skipping deploy"
else
    echo "Deploying to d2 cluster $D2CLUSTER..."

    d2 cluster up "$D2CLUSTER" --image $IMAGE:$TAG
    d2 cluster logs "$D2CLUSTER"
fi
