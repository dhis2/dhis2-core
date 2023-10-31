#!/usr/bin/env bash

ORGANIZATION=dhis2
REPOSITORY=core
TAG=3.1.2

HUB_TOKEN=$(curl -s -H "Content-Type: application/json" -X POST -d "{\"username\": \"$HUB_USERNAME\", \"password\": \"$HUB_PASSWORD\"}" https://hub.docker.com/v2/users/login/ | jq -r .token)

echo Delete $ORGANIZATION/$REPOSITORY:$TAG?
read

curl -H "Authorization: JWT $HUB_TOKEN" -X "DELETE" https://hub.docker.com/v2/repositories/$ORGANIZATION/$REPOSITORY/tags/$TAG/
