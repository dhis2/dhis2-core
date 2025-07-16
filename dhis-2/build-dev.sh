#!/usr/bin/env bash
# Builds DHIS2 war and Docker image for development use

set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"

IMAGE=${IMAGE:="dhis2/core-dev:local"}
BUILD_REVISION=$(git --git-dir "$DIR/../.git" rev-parse HEAD)
BUILD_BRANCH=$(git --git-dir "$DIR/../.git" branch --show-current)

# Choosing this approach over automatically activating a maven profile based on the architecture.
# This is to not risk running both the jibBuild and jibBuildArmOnly profiles in our pipelines.
# There might be ways like using https://maven.apache.org/enforcer/enforcer-rules/requireActiveProfile.html
# to prevent that but they would require more work.
ARCH=$(mvn help:system | grep "os\.arch")
JIB_PROFILE="jibDockerBuild"
if [[ "$ARCH" == *arm64* || "$ARCH" == *aarch64* ]]; then
  JIB_PROFILE="$JIB_PROFILE,jibBuildArmOnly"
fi

echo "Building dhis2-core and Docker image..."

export MAVEN_OPTS="-Dhttp.keepAlive=false -Dmaven.wagon.http.pool=false -Dmaven.wagon.http.retryHandler.class=standard -Dmaven.wagon.http.retryHandler.count=3 -Dmaven.wagon.httpconnectionManager.ttlSeconds=25"
mvn clean package --threads 2C -DskipTests -Dmaven.test.skip=true --file "${DIR}/pom.xml" --projects dhis-web-server --also-make \
  --activate-profiles "$JIB_PROFILE" -Djib.to.image="$IMAGE" -Djib.container.labels=DHIS2_BUILD_REVISION="${BUILD_REVISION}",DHIS2_BUILD_BRANCH="${BUILD_BRANCH}"
