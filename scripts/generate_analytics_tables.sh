#!/bin/bash

CREDENTIALS="$USERNAME:$PASSWORD"
# Only skip tracked entities because we need outlier tables and the rest
# Only request analytics table generation for the last year to make things quicker and prevent out-of-disk-space issues on GHA
RELATIVE_POLL_ENDPOINT=$(curl --user $CREDENTIALS -X POST $BASE_URL/api/resourceTables/analytics?skipTrackedEntities=true\&lastYears=1 | jq -r '.response.relativeNotifierEndpoint')
POLL_URL="$BASE_URL$RELATIVE_POLL_ENDPOINT"
TRIES=0

while true; do
    TRIES=$((TRIES + 1))
    COMPLETED=$(curl -fsSL --user $CREDENTIALS $POLL_URL | jq '. | any(.completed == true)')
    if [[ "$COMPLETED" == "true" ]]; then
        echo "Analytics table generation complete" 1>&2
        exit 0
        break
    # Let's give up after 30 minutes (30*60/5=360)
    elif test $TRIES -ge 360; then
        echo "Analytics table generation timed out" 1>&2
        exit 1
        break
    else
        echo "Polling analytics tables generation"
    fi
    sleep 5
done