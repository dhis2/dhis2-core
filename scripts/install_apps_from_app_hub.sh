#!/usr/bin/env bash

function split_on_commas() {
    local IFS=,
    # strip " before splitting
    local WORD_LIST=(${1//\"/})
    # strip ' before splitting
    WORD_LIST=(${WORD_LIST//\'/})
    for word in "${WORD_LIST[@]}"; do
        echo "$word"
    done
}

function install_app() {
    local CREDENTIALS="$USERNAME:$PASSWORD"
    local APP_NAME=$1
    local APP_ID=$(curl -fsSL --user $CREDENTIALS $BASE_URL/api/appHub | jq -r --arg name "${APP_NAME}" '.[] | select(.name==$name) | .id')
    local DHIS2_VERSION=$(curl -fsSL --user $CREDENTIALS $BASE_URL/api/system/info | jq -r '.version' | cut -d '.' -f 1,2)
    local LATEST_APP_VERSION_ID=$(curl -fsSL --user $CREDENTIALS $BASE_URL/api/appHub/v2/apps/$APP_ID/versions?minDhisVersion=lte:$DHIS2_VERSION | jq -r '.result[0].id // empty')

    if [[ -n "\$LATEST_APP_VERSION_ID" ]]; then
        curl -fsSL --user $CREDENTIALS -X POST $BASE_URL/api/appHub/$LATEST_APP_VERSION_ID
        echo "Installed $APP_NAME with ID $APP_ID"
    else
        echo "No compatible app version found for DHIS2 version $DHIS2_VERSION" 1>&2
        exit 1
    fi
}

split_on_commas "$APPS_TO_INSTALL" | while read CURRENT_APP_NAME; do
    install_app "$CURRENT_APP_NAME"
done

echo "All apps installed"
exit 0