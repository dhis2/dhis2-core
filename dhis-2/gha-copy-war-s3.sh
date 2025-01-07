#!/usr/bin/env bash

# This script is used in the pipelines for all the supported dhis2-core branches, including
# master and 4 major versions before that and for all the build types - dev, canary, eos and stable.

set -euo pipefail

BUILD_DATE=$(date -I'date')

IS_PATCH_VERSION=0

VERSIONS_JSON="https://releases.dhis2.org/v1/versions/stable.json"

BUCKET="s3://releases.dhis2.org"

S3_CMD="aws s3 cp --metadata git-commit=$GIT_COMMIT --no-progress"

S3_DESTINATIONS=()

RELEASE_TYPE="$1"

if [[ -z "$1" ]]; then
  echo "Error: Release type is required."
  exit 1
fi

RELEASE_VERSION="$2"

if [[ -z "$2" ]]; then
  echo "Error: Release version is required."
  exit 1
fi

MAJOR_VERSION=$(cut -d '.' -f 2 <<< "$RELEASE_VERSION")

echo "Release version: $RELEASE_VERSION"
echo "Major version: $MAJOR_VERSION"
if [[ "$RELEASE_VERSION" == "master" || $MAJOR_VERSION -ge 42  ]]; then
  WAR_LOCATION="${WORKSPACE}/dhis-2/dhis-web-server/target/dhis.war"
else
  WAR_LOCATION="${WORKSPACE}/dhis-2/dhis-web/dhis-web-portal/target/dhis.war"
fi

if [[ ! -f "$WAR_LOCATION" ]]; then
  echo "Error: WAR file not found."
  exit 1
fi

case $RELEASE_TYPE in
  "canary")
    S3_DESTINATIONS+=("$BUCKET/$RELEASE_VERSION/$RELEASE_TYPE/dhis2-$RELEASE_TYPE-$RELEASE_VERSION.war")

    S3_DESTINATIONS+=("$BUCKET/$RELEASE_VERSION/$RELEASE_TYPE/dhis2-$RELEASE_TYPE-$RELEASE_VERSION-$BUILD_DATE.war")
    ;;

  "dev")
    S3_DESTINATIONS+=("$BUCKET/$RELEASE_VERSION/$RELEASE_TYPE/dhis2-$RELEASE_TYPE-$RELEASE_VERSION.war")

    if [[ "$RELEASE_VERSION" == "master" ]]; then
      RELEASE_VERSION="dev"
    fi

    S3_DESTINATIONS+=("$BUCKET/$RELEASE_VERSION/dhis.war")
    ;;

  "eos")
    S3_DESTINATIONS+=("$BUCKET/$RELEASE_VERSION/dhis2-stable-$RELEASE_VERSION-$RELEASE_TYPE.war")
    ;;

  "stable")
    if [[ "$RELEASE_VERSION" =~ ^patch\/.*  ]]; then
      IS_PATCH_VERSION=1
      RELEASE_VERSION=${RELEASE_VERSION#"patch/"}
    fi

    SHORT_VERSION=$(cut -d '.' -f 1,2 <<< "$RELEASE_VERSION")

    PATCH_VERSION=$(cut -d '.' -f 3 <<< "$RELEASE_VERSION")

    # Add an extra destination in S3 for the new semantic version schema without leading "2."
    IFS=. read -ra RELEASE_VERSION_SEGMENTS <<< "$RELEASE_VERSION"

    NEW_RELEASE_VERSION=$(cut -d '.' -f 2- <<< "$RELEASE_VERSION")
    NEW_SHORT_VERSION=$(cut -d '.' -f 2 <<< "$RELEASE_VERSION")

    if [[ "$IS_PATCH_VERSION" -eq 1 ]]; then
      RELEASE_VERSION+="-rc"
      # for release candidates, also add a version with the git hash
      S3_DESTINATIONS+=("$BUCKET/$SHORT_VERSION/dhis2-$RELEASE_TYPE-$RELEASE_VERSION-${GIT_COMMIT:0:7}.war")
    fi

    S3_DESTINATIONS+=("$BUCKET/$SHORT_VERSION/dhis2-$RELEASE_TYPE-$RELEASE_VERSION.war")
    S3_DESTINATIONS+=("$BUCKET/$SHORT_VERSION/$RELEASE_VERSION/dhis.war")

    LATEST_PATCH_VERSION=$(
      curl -fsSL "$VERSIONS_JSON" |
      jq -r --arg VERSION "$SHORT_VERSION" '.versions[] | select(.name == $VERSION ) | .latestPatchVersion'
    )

    if [[ "$IS_PATCH_VERSION" -eq 0 && -n "${LATEST_PATCH_VERSION-}" && "$PATCH_VERSION" -ge "$LATEST_PATCH_VERSION" ]]; then
      S3_DESTINATIONS+=("$BUCKET/$SHORT_VERSION/dhis2-$RELEASE_TYPE-latest.war")
      S3_DESTINATIONS+=("$BUCKET/$NEW_SHORT_VERSION/dhis2-$RELEASE_TYPE-latest.war")
    fi


    if [[ ${#RELEASE_VERSION_SEGMENTS[@]} -lt 4 ]]; then
      NEW_RELEASE_VERSION+=".0"
    fi

    if [[ "$IS_PATCH_VERSION" -eq 1 ]]; then
      NEW_RELEASE_VERSION+="-rc"
      # for release candidates, also add a version with the git hash
      S3_DESTINATIONS+=("$BUCKET/$NEW_SHORT_VERSION/dhis2-$RELEASE_TYPE-$NEW_RELEASE_VERSION-${GIT_COMMIT:0:7}.war")
    fi

    S3_DESTINATIONS+=("$BUCKET/$NEW_SHORT_VERSION/dhis2-$RELEASE_TYPE-$NEW_RELEASE_VERSION.war")

    ;;

  *)
    echo "Error: Unknown Release type."
    exit 1
    ;;
esac

for destination in "${S3_DESTINATIONS[@]}"
do
  #echo "$WAR_LOCATION" "$destination"
  $S3_CMD "$WAR_LOCATION" "$destination"
done
