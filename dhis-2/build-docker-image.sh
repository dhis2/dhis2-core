#!/usr/bin/env bash

set -euo pipefail

# The image is built by the Jib Maven plugin from an existing WAR, see docker/image/pom.xml.
IMAGE_POM=${IMAGE_POM:-'docker/image/pom.xml'}
# Image profile of docker/image/pom.xml: "tomcat" (Tomcat base image) or "embedded" (embedded Tomcat).
IMAGE_PROFILE=${IMAGE_PROFILE:-'tomcat'}
# Tomcat base image of the "tomcat" profile. The default is defined in docker/image/pom.xml.
BASE_IMAGE=${BASE_IMAGE:-}
IMAGE_REPOSITORY=${IMAGE_REPOSITORY:-'dhis2/core'}
WAR_PATH=${WAR_PATH:-'dhis-2/dhis-web-server/target/dhis.war'}
DOWNLOADED_WAR_DIR=${DOWNLOADED_WAR_DIR:-'dhis2-war'}

ARCHITECTURES=${ARCHITECTURES:-'amd64'}

# docker/image/pom.xml expects the WAR to have this name.
war_name='dhis.war'
old_version_schema_prefix='2'
stable_versions_json="$(curl -fsSL "https://releases.dhis2.org/v1/versions/stable.json")"

function help() {
   echo 'Available options:'
   echo '-t <tag>    DHIS2 version tag to build.'
   echo '-d          Create image from a "dev" version.'
   echo '-r          Rebuild image with an existing WAR for the given DHIS2 version. Without this option the image will be built with a new WAR.'
   echo '-p <image>  Promote an existing image, e.g. one built and tested elsewhere, instead of building one.'
   echo '-h          Print this help message.'
   echo
   echo 'Environment variables:'
   echo 'ARCHITECTURES   Space-separated list of architectures to build. Defaults to "amd64".'
   echo '                Example: ARCHITECTURES="amd64 arm64" build-docker-image.sh -t 40.1.1 -d'
   echo 'IMAGE_PROFILE   Image profile of docker/image/pom.xml: "tomcat" or "embedded". Defaults to "tomcat".'
   echo 'BASE_IMAGE      Tomcat base image of the "tomcat" profile. Defaults to the one in docker/image/pom.xml.'
   echo 'WAR_PATH        WAR to build the image from, must be named dhis.war. Not used with -r.'
   echo
   echo 'Example: build-docker-image.sh -t 40.1.1 -r'
   echo 'Example: build-docker-image.sh -t 40.1.1 -p registry.example.org/dhis2/core@sha256:<digest>'
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
  rm -rf "./$DOWNLOADED_WAR_DIR"
  mkdir -p "./$DOWNLOADED_WAR_DIR"
  curl -o "$DOWNLOADED_WAR_DIR/$war_name" "$war_url"

  known_war_sha256="$(
    echo "$stable_versions_json" |
    jq -r --arg image_tag "$image_tag" '.versions[] | select(.supported == true) .patchVersions[] | select(.displayName == $image_tag) .sha256'
  )"

  sha256sum_output="$(sha256sum "$DOWNLOADED_WAR_DIR/$war_name")"
  downloaded_war_sha256="${sha256sum_output%% *}" # strip file name from sha256sum output with variable expansion

  if [[ "$downloaded_war_sha256" != "$known_war_sha256" ]]; then
    echo "Downloaded WAR sha256 sum ($downloaded_war_sha256) doesn't match known sha256 sum ($known_war_sha256)!"
    exit 1
  fi

  war_dir="$(cd "$DOWNLOADED_WAR_DIR" && pwd)"
}

function use_new_war() {
  echo "Image will be built with new WAR from $WAR_PATH"

  if [[ ! -f "$WAR_PATH" ]]; then
    echo "Error: WAR not found at $WAR_PATH"
    exit 1
  fi

  if [[ "$(basename "$WAR_PATH")" != "$war_name" ]]; then
    echo "Error: the WAR must be named $war_name, got $WAR_PATH"
    exit 1
  fi

  war_dir="$(cd "$(dirname "$WAR_PATH")" && pwd)"
}

# Builds the image for all architectures and pushes it as one multi-architecture image (manifest list).
function build_main_image() {
  echo "Building image for version $image_tag with image profile $IMAGE_PROFILE ..."

  local -a arch_list
  read -ra arch_list <<< "$ARCHITECTURES"

  local platforms
  platforms="$(printf 'linux/%s,' "${arch_list[@]}")"
  platforms="${platforms%,}"

  local -a mvn_args=(
    --batch-mode --no-transfer-progress
    --file "$IMAGE_POM"
    --activate-profiles "$IMAGE_PROFILE"
    antrun:run@prepare jib:build
    "-Dwar.dir=$war_dir"
    "-Djib.to.image=$IMAGE_REPOSITORY:$main_image_tag"
    "-Djib.from.platforms=$platforms"
    "-Djib.container.labels=DHIS2_VERSION=$DHIS2_VERSION,DHIS2_BUILD_REVISION=$GIT_COMMIT,DHIS2_BUILD_BRANCH=$GIT_BRANCH"
  )

  if [[ -n "$BASE_IMAGE" ]]; then
    mvn_args+=("-Dimage.from.tomcat=$BASE_IMAGE")
  fi

  mvn "${mvn_args[@]}"
}

# Copies the manifest list and the image manifests it references as they are, so the promoted image has the digests of the source.
function copy_main_image() {
  echo "Copying $source_image to $IMAGE_REPOSITORY:$main_image_tag ..."
  docker buildx imagetools create --tag "$IMAGE_REPOSITORY:$main_image_tag" "$source_image"
}

# Adds the rolling tags to the main image. The manifests are copied as they are in the registry, so all tags have the
# digests of the main image. This works for multi-architecture images (manifest lists) and single-architecture images.
function create_additional_manifests() {
  local -a tag_args=()
  for tag in "${rolling_tags[@]}"; do
    tag_args+=(--tag "$IMAGE_REPOSITORY:$tag")
  done

  echo "Tagging $IMAGE_REPOSITORY:$main_image_tag as ${rolling_tags[*]} ..."
  docker buildx imagetools create "${tag_args[@]}" "$IMAGE_REPOSITORY:$main_image_tag"
}

while getopts 'ht:drp:' option; do
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
      p)
        source_image=$OPTARG;;
      \?)
        echo 'Error: Invalid option'
        exit;;
   esac
done

if [[ -n ${source_image:-} && ( ${dev_image:-} -eq 1 || ${rebuild_image:-} -eq 1 ) ]]; then
  echo 'Error: -p cannot be combined with -d or -r'
  exit 1
fi

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

if [[ -n ${source_image:-} ]]; then
  copy_main_image
else
  if [[ ${rebuild_image:-} -eq 1 ]]; then
    use_existing_war
  else
    use_new_war
  fi

  build_main_image
fi

create_additional_manifests

echo "Done with building $image_tag."
echo
