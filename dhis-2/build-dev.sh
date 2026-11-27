#!/usr/bin/env bash
# Builds DHIS2 war and Docker image for development use

set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"

IMAGE=${IMAGE:="dhis2/core-dev:local"}
BUILD_REVISION=$(git -C "$DIR" rev-parse HEAD)
BUILD_BRANCH=$(git -C "$DIR" branch --show-current)

# Jib builds the image for linux/amd64 unless told otherwise. Build for the architecture of this machine, so the image
# runs without emulation, for example on a Mac with Apple silicon.
ARCH=$(uname -m)
PLATFORM="linux/amd64"
if [[ "$ARCH" == *arm64* || "$ARCH" == *aarch64* ]]; then
  PLATFORM="linux/arm64"
fi

echo "Building dhis2-core and Docker image..."

export MAVEN_OPTS="-Dhttp.keepAlive=false -Dmaven.wagon.http.pool=false -Dmaven.wagon.http.retryHandler.class=standard -Dmaven.wagon.http.retryHandler.count=3 -Dmaven.wagon.httpconnectionManager.ttlSeconds=25"
# The embedded profile makes the WAR executable, the image starts it with "java -jar".
mvn ${GOALS:-clean package} --threads 2C -DskipTests -Dmaven.test.skip=true --file "${DIR}/pom.xml" --projects dhis-web-server --also-make \
  --activate-profiles embedded

# The image is built from the WAR, see docker/image/pom.xml.
mvn --file "${DIR}/../docker/image/pom.xml" antrun:run@prepare jib:dockerBuild \
  -Djib.from.platforms="$PLATFORM" -Djib.to.image="$IMAGE" -Djib.container.labels=DHIS2_BUILD_REVISION="${BUILD_REVISION}",DHIS2_BUILD_BRANCH="${BUILD_BRANCH}"
