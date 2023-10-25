#!/usr/bin/env bash

set -euo pipefail

# TODO add labels when rebuilding
# TODO rebuild also for arm64

IMAGE_REPOSITORY=${IMAGE_REPOSITORY:-'dhis2/core'}
IMAGE_APP_ROOT=${IMAGE_APP_ROOT:-'/usr/local/tomcat/webapps/ROOT'}
IMAGE_USER=${IMAGE_USER:-'65534'}

#"https://releases.dhis2.org/v1/versions/stable.json"
STABLE_VERSIONS_JSON=$(curl -fsSL "https://raw.githubusercontent.com/dhis2/dhis2-releases/master/downloads/v1/versions/stable.json")
OLD_SCHEMA_PREFIX=2

function help() {
   echo 'Available options:'
   echo '-t <tag>    DHIS2 version tag to build.'
   echo '-r          Rebuild image with an existing WAR for the given DHIS2 version. Without this option the image will be built with a new WAR.'
   echo '-h          Print this help message.'
   echo
   echo 'Usage: build-docker-image.sh -t 40.1.1 -r'
}

function list_tags() {
  echo 'Immutable tag:'
  echo "$IMMUTABLE_IMAGE_TAG"

  echo 'Rolling tags:'
  echo "${ROLLING_TAGS[@]}"
}

function build_image() {
  echo "Building image with new WAR for version $IMAGE_TAG, based on $BASE_IMAGE ..."
  list_tags

  mvn --batch-mode --no-transfer-progress -DskipTests -Dmaven.test.skip=true \
      -f dhis-2/dhis-web/dhis-web-portal/pom.xml jib:build -PjibBuild \
      -Djib.from.image="$BASE_IMAGE" -Djib.to.image="${IMAGE_REPOSITORY}:${IMAGE_TAG}" \
      -Djib.container.labels=DHIS2_VERSION="$DHIS2_VERSION",DHIS2_BUILD_REVISION="$GIT_COMMIT",DHIS2_BUILD_BRANCH="$GIT_BRANCH"
}

function rebuild_image() {
  echo "Rebuilding image with existing WAR for version $IMAGE_TAG, based on $BASE_IMAGE ..."
  list_tags

  WAR_URL=$(
    echo "$STABLE_VERSIONS_JSON" |
    jq -r --arg image_tag "$IMAGE_TAG" '.versions[] | select(.supported == true) .patchVersions[] | select(.displayName == $image_tag) .url'
  )
  curl -o dhis2.war "$WAR_URL"

  jib war --from="$BASE_IMAGE" --target="docker://$IMAGE_REPOSITORY:$IMMUTABLE_IMAGE_TAG" --app-root="$IMAGE_APP_ROOT" --user="$IMAGE_USER" dhis2.war
}

function create_immutable_tag() {
  # Current date in ISO 8601 format
  CURRENT_DATE=$(date -u +"%Y%m%dT%H%M%SZ")

  IMMUTABLE_IMAGE_TAG="${IMAGE_TAG}-${CURRENT_DATE}"
}

function create_rolling_tags() {
  # Split image tag by .
  IFS=. read -ra IMAGE_TAG_SEGMENTS <<< "$IMAGE_TAG"

  MAJOR="${IMAGE_TAG_SEGMENTS[0]}"
  MINOR="${IMAGE_TAG_SEGMENTS[1]}"
  PATCH="${IMAGE_TAG_SEGMENTS[2]}"

  # Always create M.m.p
  ROLLING_TAGS=("$IMAGE_TAG")

  # If non-zero patch(hotfix) create 2.M.m.p
  # TODO Do we want this or just go with consistency and create it like M.m.p?
  if [[ "$PATCH" != "0" ]]; then
    echo "This is a non-zero patch(hotfix)."
    ROLLING_TAGS+=("$OLD_SCHEMA_PREFIX.$IMAGE_TAG")
  fi

  # If patch(hotfix) is the latest for given minor(patch) create M.m + 2.M.m
  LATEST_HOTFIX_VERSION=$(
    echo "$STABLE_VERSIONS_JSON" |
    jq -r --argjson major "$MAJOR" --argjson minor "$MINOR" \
    '.versions[] | select(.version == $major) .patchVersions[] | select(.version == $minor) .hotfixVersion' |
    sort -n |
    tail -1
  )
  if [[ "$PATCH" == "$LATEST_HOTFIX_VERSION" ]]; then
    echo "The provided patch version is the latest for the minor version."
    ROLLING_TAGS+=(
      "$MAJOR.$MINOR"
      "$OLD_SCHEMA_PREFIX.$MAJOR.$MINOR"
    )
  fi

  # If version is latest create M + 2.M
  LATEST_VERSION=$(
    echo "$STABLE_VERSIONS_JSON" |
    jq -r --argjson major "$MAJOR" \
    '.versions[] | select(.version == $major) | "\(.version).\(.latestPatchVersion).\(.latestHotfixVersion)"'
  )
  if [[ "$IMAGE_TAG" == "$LATEST_VERSION" ]]; then
      echo "The provided version is the latest version."
      ROLLING_TAGS+=(
        "$MAJOR"
        "$OLD_SCHEMA_PREFIX.$MAJOR"
      )
  fi
}

function tag_image() {
  echo "Tagging image ..."
  for TAG in "${ROLLING_TAGS[@]}"; do
    docker image tag "${IMAGE_REPOSITORY}:${IMMUTABLE_IMAGE_TAG}" "${IMAGE_REPOSITORY}:${TAG}"
  done
}

while getopts "ht:r" option; do
   case $option in
      h)
        help
        exit;;
      t)
        IMAGE_TAG=$OPTARG;;
      r)
        REBUILD_IMAGE=1;;
      \?)
        echo "Error: Invalid option"
        exit;;
   esac
done

create_immutable_tag
create_rolling_tags

JDK_VERSION=$(
  echo "$STABLE_VERSIONS_JSON" |
  jq -r --argjson major "$MAJOR" '.versions[] | select(.version == $major) .jdk'
)
BASE_IMAGE=${BASE_IMAGE:-"tomcat:9.0-jre${JDK_VERSION}"}

#if [[ "${REBUILD_IMAGE:-}" -eq 1 ]]; then
#  rebuild_image
#else
  build_image
#fi

tag_image

# Push the tagged images to the registry
echo "Pushing tags to $IMAGE_REPOSITORY ..."
docker image push --all-tags "${IMAGE_REPOSITORY}"

echo
