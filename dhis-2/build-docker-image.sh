#!/usr/bin/env bash

set -euo pipefail

BASE_IMAGE=${BASE_IMAGE}
IMAGE_REPOSITORY=${IMAGE_REPOSITORY:-'dhis2/core'}
IMAGE_APP_ROOT=${IMAGE_APP_ROOT:-'/usr/local/tomcat/webapps/ROOT'}
IMAGE_USER=${IMAGE_USER:-'65534'}
WAR_PATH=${WAR_PATH:-'dhis-2/dhis-web-server/target/dhis.war'}
UNARCHIVED_WAR_DIR=${UNARCHIVED_WAR_DIR:-'dhis2-war'}
JIB_BUILD_FILE=${JIB_BUILD_FILE:-'jib.yaml'}

downloaded_war_name='downloaded-dhis2.war'
old_version_schema_prefix='2'
stable_versions_json="$(curl -fsSL "https://releases.dhis2.org/v1/versions/stable.json")"

function help() {
   echo 'Available options:'
   echo '-t <tag>    DHIS2 version tag to build.'
   echo '-d          Create image from a "dev" version.'
   echo '-r          Rebuild image with an existing WAR for the given DHIS2 version. Without this option the image will be built with a new WAR.'
   echo '-h          Print this help message.'
   echo
   echo 'Example: build-docker-image.sh -t 40.1.1 -r'
}

function create_immutable_tag() {
  current_date="$(date -u +'%Y%m%dT%H%M%SZ')" # ISO 8601 format

  main_image_tag="$image_tag-$current_date"
}

function create_rolling_tags() {
  IFS=. read -ra image_tag_segments <<< "$image_tag"

  major="${image_tag_segments[0]}"
  minor="${image_tag_segments[1]}"
  patch="${image_tag_segments[2]}"

  # Always create M.m.p tags.
  rolling_tags=(
    "$image_tag"
    "$old_version_schema_prefix.$image_tag"
  )

  latest_patch_version="$(
    echo "$stable_versions_json" |
    jq -r --argjson major "$major" \
    '.versions[] | select(.version == $major) .latestHotfixVersion'
  )"

  latest_minor_version="$(
    echo "$stable_versions_json" |
    jq -r --argjson major "$major" \
    '.versions[] | select(.version == $major) | .latestPatchVersion'
  )"

  latest_major_version="$(
    echo "$stable_versions_json" |
    jq -r '.versions[] | select(.latest == true) .version'
  )"

  if [[ "$major" -gt "$latest_major_version" ]] || \
     [[ "$minor" -gt "$latest_minor_version" ]] || \
     [[ "$patch" -ge "$latest_patch_version" && "$minor" -eq "$latest_minor_version" ]]; then
    echo "New major, new minor or new/existing patch for the latest minor version, creating M.m and M tags."
    rolling_tags+=(
      "$major.$minor"
      "$major"
      "$old_version_schema_prefix.$major.$minor"
      "$old_version_schema_prefix.$major"
    )
  fi

  if [[ "$patch" -gt "$latest_patch_version" && "$minor" -lt "$latest_minor_version" ]]; then
    echo "New patch for a non-latest minor version, creating M.m tags."
    rolling_tags+=(
      "$major.$minor"
      "$old_version_schema_prefix.$major.$minor"
    )
  fi
}

function list_tags() {
  echo 'Immutable tag:'
  echo "$main_image_tag"

  echo 'Rolling tags:'
  echo "${rolling_tags[@]}"
}

function use_existing_war() {
  echo 'Image will be rebuilt with existing WAR'

  war_url="$(
    echo "$stable_versions_json" |
    jq -r --arg image_tag "$image_tag" '.versions[] | select(.supported == true) .patchVersions[] | select(.displayName == $image_tag) .url'
  )"
  echo "Downloading from $war_url ..."
  rm -rf "$downloaded_war_name"
  curl -o "$downloaded_war_name" "$war_url"

  known_war_sha256="$(
    echo "$stable_versions_json" |
    jq -r --arg image_tag "$image_tag" '.versions[] | select(.supported == true) .patchVersions[] | select(.displayName == $image_tag) .sha256'
  )"

  sha256sum_output="$(sha256sum "$downloaded_war_name")"
  downloaded_war_sha256="${sha256sum_output%% *}" # strip file name from sha256sum output with variable expansion

  if [[ "$downloaded_war_sha256" != "$known_war_sha256" ]]; then
    echo "Downloaded WAR sha256 sum ($downloaded_war_sha256) doesn't match known sha256 sum ($known_war_sha256)!"
    exit 1
  fi

  echo "Unarchiving WAR to $UNARCHIVED_WAR_DIR ..."
  rm -rf "./$UNARCHIVED_WAR_DIR"
  unzip -q -o "$downloaded_war_name" -d "./$UNARCHIVED_WAR_DIR"
}

function use_new_war() {
  echo "Image will be built with new WAR from $WAR_PATH"
  rm -rf "./$UNARCHIVED_WAR_DIR"
  unzip -q -o "$WAR_PATH" -d "./$UNARCHIVED_WAR_DIR"
}

function build_main_image() {
  echo "Building image for version $image_tag, based on $BASE_IMAGE ..."

  jib build \
    --build-file "$JIB_BUILD_FILE" \
    --target "$IMAGE_REPOSITORY:$main_image_tag" \
    --parameter unarchivedWarDir="$UNARCHIVED_WAR_DIR" \
    --parameter imageAppRoot="$IMAGE_APP_ROOT" \
    --parameter baseImage="$BASE_IMAGE" \
    --parameter imageUser="$IMAGE_USER" \
    --parameter gitCommit="$GIT_COMMIT" \
    --parameter gitBranch="$GIT_BRANCH" \
    --parameter dhis2Version="$DHIS2_VERSION" \
    --parameter timestamp="$(date +'%s000')" # Unix time with zeroed milliseconds; will be shown as "2023-11-02T14:40:32Z"
}

# To create additional rolling tags for a multi-architecture image (manifest list) we have to create a new manifest list,
# based on the supported image architectures' sha256 digests.
function create_additional_manifests() {
  echo "Pulling $IMAGE_REPOSITORY:$main_image_tag for tagging ..."
  docker image pull "$IMAGE_REPOSITORY:$main_image_tag"

  for tag in "${rolling_tags[@]}"; do
    # shellcheck disable=SC2207
    manifest_digests=($(docker manifest inspect "$IMAGE_REPOSITORY:$main_image_tag" | jq -r '.manifests[] .digest'))

    # shellcheck disable=SC2145
    echo "Creating new manifest with digests ${manifest_digests[@]} ..."

    # No double quotes for the $manifest_digests, as we want them treated as separate elements and prefixed with the repository.
    # shellcheck disable=SC2068
    docker manifest create "$IMAGE_REPOSITORY:$tag" ${manifest_digests[@]/#/ $IMAGE_REPOSITORY@}

    echo "Pushing new manifest $IMAGE_REPOSITORY:$tag ..."
    docker manifest push "$IMAGE_REPOSITORY:$tag"
  done
}

while getopts 'ht:dr' option; do
   case $option in
      h)
        help
        exit;;
      t)
        image_tag=$OPTARG;;
      d)
        dev_image=1;;
      r)
        rebuild_image=1;;
      \?)
        echo 'Error: Invalid option'
        exit;;
   esac
done

if [[ ${dev_image:-} -eq 1 ]]; then
  echo 'Creating image from a dev version ...'

  # We're not creating an immutable tag with a timestamp for dev images.
  main_image_tag="$image_tag"

  use_new_war

  build_main_image

  if [[ "$image_tag" =~ -rc$ ]]; then
    echo 'Version is Release Candidate, creating extra old version format tag.'
    rolling_tags=("$old_version_schema_prefix.$main_image_tag")
    create_additional_manifests
  fi

  echo 'Done with building dev image. Exiting.'
  echo
  exit 0
fi

create_immutable_tag

create_rolling_tags

list_tags

if [[ ${rebuild_image:-} -eq 1 ]]; then
  use_existing_war
else
  use_new_war
fi

build_main_image

create_additional_manifests

echo "Done with building $image_tag."
echo
