#!/usr/bin/env bash
# Populates the database with an org unit group holding many members, for benchmarking the
# collection items endpoints (POST/DELETE /api/organisationUnitGroups/{uid}/organisationUnits)
# with CollectionItemsPerformanceTest.
#
# Creates a parent org unit and the group (via the API), then the member org units and the group
# membership (via SQL), and clears the application caches so DHIS2 reads the new membership.
#
# Run it against a running stack from dhis-test-performance, or let run-simulation.sh run it after
# the containers start:
#
#   POPULATE_SCRIPT=scripts/populate-collection-items.sh \
#   DHIS2_IMAGE=dhis2/core-dev:latest \
#   SIMULATION_CLASS=org.hisp.dhis.test.platform.CollectionItemsPerformanceTest \
#   ./run-simulation.sh
#
# Author: Morten Svanæs <msvanaes@dhis2.org>
#
# ENVIRONMENT:
#   POPULATE_MEMBERS              org units in the group (default: 100000)
#   POPULATE_NON_MEMBERS          org units not in the group, which the simulation adds to it
#                                 (default: 10, must be at least the simulation's -Dadditions)
#   POPULATE_GROUP_UID            UID of the org unit group to create (default: PfCollItems)
#   POPULATE_PARENT_UID           UID of the parent org unit to create (default: PfCollItmOu)
#   POPULATE_ROOT_ORG_UNIT        org unit the parent is created under
#                                 (default: ImspTQPwCqd, Sierra Leone)
#   DHIS2_URL                     DHIS2 base URL (default: http://localhost:8080)
#   DHIS2_USERNAME/DHIS2_PASSWORD API credentials (default: admin/district)
#   PSQL                          command running psql against the DHIS2 database, reading the
#                                 script from stdin (default: docker compose exec -T db psql
#                                 --username=dhis --dbname=dhis)
set -euo pipefail

cd "$(dirname "$0")/.."

POPULATE_MEMBERS=${POPULATE_MEMBERS:-100000}
POPULATE_NON_MEMBERS=${POPULATE_NON_MEMBERS:-10}
POPULATE_GROUP_UID=${POPULATE_GROUP_UID:-PfCollItems}
POPULATE_PARENT_UID=${POPULATE_PARENT_UID:-PfCollItmOu}
POPULATE_ROOT_ORG_UNIT=${POPULATE_ROOT_ORG_UNIT:-ImspTQPwCqd}
DHIS2_URL=${DHIS2_URL:-http://localhost:8080}
DHIS2_USERNAME=${DHIS2_USERNAME:-admin}
DHIS2_PASSWORD=${DHIS2_PASSWORD:-district}
PSQL=${PSQL:-docker compose exec -T db psql --username=dhis --dbname=dhis}

for number in POPULATE_MEMBERS POPULATE_NON_MEMBERS; do
  if ! [[ "${!number}" =~ ^[0-9]+$ ]] || [ "${!number}" -gt 9999999 ]; then
    echo "Error: $number must be an integer from 0 to 9999999, got: ${!number}" >&2
    exit 1
  fi
done

api() {
  local method=$1 path=$2
  shift 2
  curl --silent --show-error --user "$DHIS2_USERNAME:$DHIS2_PASSWORD" \
    --header "Content-Type: application/json" --request "$method" "$@" "$DHIS2_URL$path"
}

echo "Populating org unit group $POPULATE_GROUP_UID: $POPULATE_MEMBERS members," \
  "$POPULATE_NON_MEMBERS non members"

metadata=$(cat <<JSON
{
  "organisationUnits": [
    {
      "id": "$POPULATE_PARENT_UID",
      "name": "Performance test collection items",
      "shortName": "Perf collection items",
      "openingDate": "2020-01-01",
      "parent": {"id": "$POPULATE_ROOT_ORG_UNIT"}
    }
  ],
  "organisationUnitGroups": [
    {
      "id": "$POPULATE_GROUP_UID",
      "name": "Performance test collection items",
      "shortName": "Perf collection items",
      "sharing": {"public": "rw------"}
    }
  ]
}
JSON
)
response=$(api POST "/api/metadata?importStrategy=CREATE_AND_UPDATE" --data "$metadata")
if [ "$(jq --raw-output '.status' <<<"$response")" != "OK" ]; then
  echo "Error: failed to create org unit $POPULATE_PARENT_UID and group $POPULATE_GROUP_UID" >&2
  echo "$response" >&2
  exit 1
fi

# shellcheck disable=SC2086 # PSQL is a command with arguments
$PSQL --quiet \
  --set=group_uid="$POPULATE_GROUP_UID" \
  --set=parent_uid="$POPULATE_PARENT_UID" \
  --set=members="$POPULATE_MEMBERS" \
  --set=non_members="$POPULATE_NON_MEMBERS" \
  < scripts/populate/collection-items.sql

# The group was created through the API, so its (empty) members may be in the second-level cache
api POST /api/maintenance/cache --output /dev/null --fail

echo "Done. Run CollectionItemsPerformanceTest with -DgroupUid=$POPULATE_GROUP_UID" \
  "-Dmembers=$POPULATE_MEMBERS"
