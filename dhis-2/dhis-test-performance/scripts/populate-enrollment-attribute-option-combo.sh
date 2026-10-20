#!/usr/bin/env bash
# Populates the database with enrollments whose attribute option combo (AOC) is partly not readable
# by the test user, for benchmarking the tracked entity export AOC access check with
# EnrollmentAttributeOptionComboTest.
#
# Creates (via the API) an attribute category combo of two categories, partner x funding, and a
# user with national capture and search scope. Then (via SQL) assigns one of the combo's AOCs to
# every enrollment of the program, adds the user to every user group, makes the API user a superuser
# so it can replicate the user into those groups, and shares every category option with every user
# group without data access. An AOC is readable by the user only when its partner option is: the
# share of enrollments the user can read is POPULATE_READABLE_PERCENT.
#
# Run it against a running stack from dhis-test-performance, or let run-simulation.sh run it after
# the containers start:
#
#   POPULATE_SCRIPT=scripts/populate-enrollment-attribute-option-combo.sh \
#   POPULATE_PROFILE=worst-case \
#   DHIS2_IMAGE=dhis2/core-dev:latest \
#   SIMULATION_CLASS=org.hisp.dhis.test.tracker.EnrollmentAttributeOptionComboTest \
#   ./run-simulation.sh
#
# ENVIRONMENT:
#   POPULATE_PROFILE              regression (default) or worst-case, sets the defaults below
#                                   regression: 50% of the enrollments readable
#                                   worst-case: no enrollment readable, so every request checks
#                                   every enrollment in scope and returns nothing
#   POPULATE_READABLE_PERCENT     share of enrollments the user can read, a multiple of 10
#   POPULATE_PROGRAM              program whose enrollments get the AOCs (default: IpHINAT79UW,
#                                 Child Programme)
#   POPULATE_SOURCE_USER          user whose roles the created user gets (default: tracker2)
#   POPULATE_USERNAME             username of the created user (default: perf_aoc)
#   POPULATE_PASSWORD             password of the created user (default: Tracker123@)
#   POPULATE_ORG_UNIT             capture and search scope of the created user (default:
#                                 ImspTQPwCqd, Sierra Leone)
#   DHIS2_URL                     DHIS2 base URL (default: http://localhost:8080)
#   DHIS2_USERNAME/DHIS2_PASSWORD API credentials (default: admin/district)
#   PSQL                          command running psql against the DHIS2 database, reading the
#                                 script from stdin (default: docker compose exec -T db psql
#                                 --username=dhis --dbname=dhis)
set -euo pipefail

cd "$(dirname "$0")/.."

POPULATE_PROFILE=${POPULATE_PROFILE:-regression}
case "$POPULATE_PROFILE" in
  regression)
    default_readable_percent=50
    ;;
  worst-case)
    default_readable_percent=0
    ;;
  *)
    echo "Error: POPULATE_PROFILE must be regression or worst-case, got: $POPULATE_PROFILE" >&2
    exit 1
    ;;
esac

POPULATE_READABLE_PERCENT=${POPULATE_READABLE_PERCENT:-$default_readable_percent}
POPULATE_PROGRAM=${POPULATE_PROGRAM:-IpHINAT79UW}
POPULATE_SOURCE_USER=${POPULATE_SOURCE_USER:-tracker2}
POPULATE_USERNAME=${POPULATE_USERNAME:-perf_aoc}
POPULATE_PASSWORD=${POPULATE_PASSWORD:-Tracker123@}
POPULATE_ORG_UNIT=${POPULATE_ORG_UNIT:-ImspTQPwCqd}
DHIS2_URL=${DHIS2_URL:-http://localhost:8080}
DHIS2_USERNAME=${DHIS2_USERNAME:-admin}
DHIS2_PASSWORD=${DHIS2_PASSWORD:-district}
PSQL=${PSQL:-docker compose exec -T db psql --username=dhis --dbname=dhis}

if ! [[ "$POPULATE_READABLE_PERCENT" =~ ^(0|[1-9]0|100)$ ]]; then
  echo "Error: POPULATE_READABLE_PERCENT must be a multiple of 10 from 0 to 100," \
    "got: $POPULATE_READABLE_PERCENT" >&2
  exit 1
fi

# 10 partner options, so each readable partner makes 10% of the enrollments readable
partners=10
fundings=5
readable_partners=$((POPULATE_READABLE_PERCENT / 10))

api() {
  local method=$1 path=$2
  shift 2
  curl --silent --show-error --globoff --user "$DHIS2_USERNAME:$DHIS2_PASSWORD" \
    --header "Content-Type: application/json" --request "$method" "$@" "$DHIS2_URL$path"
}

import_metadata() {
  local response
  response=$(api POST "/api/metadata?importStrategy=CREATE_AND_UPDATE" --data "$1")
  if [ "$(jq --raw-output '.status' <<<"$response")" != "OK" ]; then
    echo "Error: failed to import $2" >&2
    echo "$response" >&2
    exit 1
  fi
}

echo "Populating enrollment AOCs of program $POPULATE_PROGRAM (profile: $POPULATE_PROFILE):" \
  "$POPULATE_READABLE_PERCENT% readable by $POPULATE_USERNAME"

# UIDs are 11 characters: PerfAocP001..PerfAocP010 (partners), PerfAocF001..PerfAocF005 (fundings)
options() {
  local prefix=$1 count=$2 readable=$3
  for i in $(seq 1 "$count"); do
    local access="r-------"
    if [ "$i" -le "$readable" ]; then
      access="r-r-----"
    fi
    jq --null-input --arg id "$(printf '%s%03d' "$prefix" "$i")" --arg access "$access" \
      '{id: $id, name: "Perf AOC \($id)", shortName: "Perf AOC \($id)",
        sharing: {public: $access}}'
  done | jq --slurp '.'
}
partner_options=$(options PerfAocP "$partners" "$readable_partners")
funding_options=$(options PerfAocF "$fundings" "$fundings")

metadata=$(jq --null-input --argjson partners "$partner_options" \
  --argjson fundings "$funding_options" '
{
  categoryOptions: ($partners + $fundings),
  categories: [
    {id: "PerfAocCat1", name: "Perf AOC partner", shortName: "Perf AOC partner",
     dataDimensionType: "ATTRIBUTE", categoryOptions: [$partners[] | {id}]},
    {id: "PerfAocCat2", name: "Perf AOC funding", shortName: "Perf AOC funding",
     dataDimensionType: "ATTRIBUTE", categoryOptions: [$fundings[] | {id}]}
  ],
  categoryCombos: [
    {id: "PerfAocComb", name: "Perf AOC partner x funding", dataDimensionType: "ATTRIBUTE",
     categories: [{id: "PerfAocCat1"}, {id: "PerfAocCat2"}]}
  ]
}')
import_metadata "$metadata" "the attribute category combo"

response=$(api POST /api/maintenance/categoryOptionComboUpdate/categoryCombo/PerfAocComb)
if [ "$(jq --raw-output '.status' <<<"$response")" != "OK" ]; then
  echo "Error: failed to generate the AOCs of PerfAocComb" >&2
  echo "$response" >&2
  exit 1
fi

source_roles=$(api GET "/api/users?filter=username:eq:$POPULATE_SOURCE_USER&fields=userRoles[id]" |
  jq --compact-output '.users[0].userRoles // empty')
if [ -z "$source_roles" ]; then
  echo "Error: could not find the roles of user $POPULATE_SOURCE_USER" >&2
  exit 1
fi

user=$(jq --null-input --arg username "$POPULATE_USERNAME" --arg password "$POPULATE_PASSWORD" \
  --arg orgUnit "$POPULATE_ORG_UNIT" --argjson roles "$source_roles" '
{
  users: [
    {id: "PerfAocUser", username: $username, password: $password, firstName: "Perf",
     surname: "AOC", userRoles: $roles, organisationUnits: [{id: $orgUnit}],
     teiSearchOrganisationUnits: [{id: $orgUnit}], dataViewOrganisationUnits: [{id: $orgUnit}]}
  ]
}')
import_metadata "$user" "user $POPULATE_USERNAME"

# shellcheck disable=SC2086 # PSQL is a command with arguments
$PSQL --quiet \
  --set=program_uid="$POPULATE_PROGRAM" \
  --set=category_combo_uid=PerfAocComb \
  --set=username="$POPULATE_USERNAME" \
  --set=api_username="$DHIS2_USERNAME" \
  < scripts/populate/enrollment-attribute-option-combo.sql

# the SQL changed sharing and user group members behind the caches
api POST /api/maintenance/cacheClear --output /dev/null --fail

echo "Done. Run EnrollmentAttributeOptionComboTest with -DreplicaUser=$POPULATE_USERNAME" \
  "(the default)"
