#!/usr/bin/env bash
# Populates the database with values of an org unit scoped unique tracked entity attribute, for
# benchmarking the tracker import uniqueness check (preheat + validation) with TrackerTest.
#
# Creates the attribute (via the API) and one tracked entity per value in each of the chosen org
# units (via SQL). Run TrackerTest with -DuniqueAttribute=<POPULATE_ATTRIBUTE_UID> so every
# imported tracked entity sends a value that already exists in all those org units, but not in
# the org unit it is imported into.
#
# Run it against a running stack from dhis-test-performance, or let run-simulation.sh run it after
# the containers start:
#
#   POPULATE_SCRIPT=scripts/populate-unique-attribute.sh POPULATE_PROFILE=worst-case \
#   DHIS2_IMAGE=dhis2/core-dev:latest SIMULATION_CLASS=org.hisp.dhis.test.tracker.TrackerTest \
#   MVN_ARGS="-Dprofile=smoke -DtestMode=import -DuniqueAttribute=PerfUniqOu1" \
#   ./run-simulation.sh
#
# ENVIRONMENT:
#   POPULATE_PROFILE              regression (default) or worst-case, sets the defaults below
#                                   regression: 10 org units x 25000 values (250k tracked entities)
#                                   worst-case: 200 org units x 25000 values (5M tracked entities)
#   POPULATE_ORG_UNITS            number of org units holding the values
#   POPULATE_VALUES_PER_ORG_UNIT  values '1'..'N' stored in every one of those org units. Must
#                                 cover all tracked entities imported across warmup and measured
#                                 runs (smoke: ~170 per run, load: ~11k per run), otherwise later
#                                 imported values collide with nothing
#   POPULATE_ATTRIBUTE_UID        UID of the attribute to create (default: PerfUniqOu1)
#   POPULATE_ATTRIBUTE_ORG_UNIT_SCOPE
#                                 true (default): unique within an org unit. false: unique in the
#                                 whole system, which only makes sense with POPULATE_ORG_UNITS=0,
#                                 as stored values would otherwise already be duplicates
#   POPULATE_TRACKED_ENTITY_TYPE  tracked entity type of the populated tracked entities
#                                 (default: nEenWmSyUEp, Person)
#   POPULATE_EXCLUDED_ORG_UNIT    org unit TrackerTest imports into, which gets no values
#                                 (default: DiszpKrYNg8, Ngelehun CHC)
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
    default_org_units=10
    default_values_per_org_unit=25000
    ;;
  worst-case)
    default_org_units=200
    default_values_per_org_unit=25000
    ;;
  *)
    echo "Error: POPULATE_PROFILE must be regression or worst-case, got: $POPULATE_PROFILE" >&2
    exit 1
    ;;
esac

POPULATE_ORG_UNITS=${POPULATE_ORG_UNITS:-$default_org_units}
POPULATE_VALUES_PER_ORG_UNIT=${POPULATE_VALUES_PER_ORG_UNIT:-$default_values_per_org_unit}
POPULATE_ATTRIBUTE_UID=${POPULATE_ATTRIBUTE_UID:-PerfUniqOu1}
POPULATE_ATTRIBUTE_ORG_UNIT_SCOPE=${POPULATE_ATTRIBUTE_ORG_UNIT_SCOPE:-true}
POPULATE_TRACKED_ENTITY_TYPE=${POPULATE_TRACKED_ENTITY_TYPE:-nEenWmSyUEp}
POPULATE_EXCLUDED_ORG_UNIT=${POPULATE_EXCLUDED_ORG_UNIT:-DiszpKrYNg8}
DHIS2_URL=${DHIS2_URL:-http://localhost:8080}
DHIS2_USERNAME=${DHIS2_USERNAME:-admin}
DHIS2_PASSWORD=${DHIS2_PASSWORD:-district}
PSQL=${PSQL:-docker compose exec -T db psql --username=dhis --dbname=dhis}

for number in POPULATE_ORG_UNITS POPULATE_VALUES_PER_ORG_UNIT; do
  if ! [[ "${!number}" =~ ^[0-9]+$ ]]; then
    echo "Error: $number must be a non-negative integer, got: ${!number}" >&2
    exit 1
  fi
done

if [ "$POPULATE_ATTRIBUTE_ORG_UNIT_SCOPE" != "true" ] && [ "$POPULATE_ORG_UNITS" -gt 0 ]; then
  echo "Error: a system wide unique attribute can't have the same value in several org units." >&2
  echo "Set POPULATE_ORG_UNITS=0 or POPULATE_ATTRIBUTE_ORG_UNIT_SCOPE=true." >&2
  exit 1
fi

api() {
  local method=$1 path=$2
  shift 2
  curl --silent --show-error --user "$DHIS2_USERNAME:$DHIS2_PASSWORD" \
    --header "Content-Type: application/json" --request "$method" "$@" "$DHIS2_URL$path"
}

echo "Populating unique attribute $POPULATE_ATTRIBUTE_UID (profile: $POPULATE_PROFILE," \
  "org unit scope: $POPULATE_ATTRIBUTE_ORG_UNIT_SCOPE): $POPULATE_ORG_UNITS org units x" \
  "$POPULATE_VALUES_PER_ORG_UNIT values"

metadata=$(cat <<JSON
{
  "trackedEntityAttributes": [
    {
      "id": "$POPULATE_ATTRIBUTE_UID",
      "name": "Performance test unique attribute $POPULATE_ATTRIBUTE_UID",
      "shortName": "Perf unique $POPULATE_ATTRIBUTE_UID",
      "valueType": "TEXT",
      "aggregationType": "NONE",
      "unique": true,
      "orgunitScope": $POPULATE_ATTRIBUTE_ORG_UNIT_SCOPE,
      "sharing": {"public": "rw------"}
    }
  ]
}
JSON
)
response=$(api POST "/api/metadata?importStrategy=CREATE_AND_UPDATE" --data "$metadata")
if [ "$(jq --raw-output '.status' <<<"$response")" != "OK" ]; then
  echo "Error: failed to create attribute $POPULATE_ATTRIBUTE_UID" >&2
  echo "$response" >&2
  exit 1
fi

# shellcheck disable=SC2086 # PSQL is a command with arguments
$PSQL --quiet \
  --set=attribute_uid="$POPULATE_ATTRIBUTE_UID" \
  --set=tracked_entity_type="$POPULATE_TRACKED_ENTITY_TYPE" \
  --set=excluded_org_unit="$POPULATE_EXCLUDED_ORG_UNIT" \
  --set=org_units="$POPULATE_ORG_UNITS" \
  --set=values_per_org_unit="$POPULATE_VALUES_PER_ORG_UNIT" \
  < scripts/populate/unique-attribute.sql

# TrackerTest numbers the values it sends from this key, so imports never send a value already
# imported into the same org unit (E1064). Continue after the values imported before, if any.
# shellcheck disable=SC2086 # PSQL is a command with arguments
next_value=$($PSQL --quiet --tuples-only --no-align --set=attribute_uid="$POPULATE_ATTRIBUTE_UID" <<'SQL'
select coalesce(max(v.value::bigint), 0) + 1
from trackedentityattributevalue v
join trackedentityattribute a using (trackedentityattributeid)
join trackedentity te using (trackedentityid)
where a.uid = :'attribute_uid'
  and te.uid !~ '^Pf[0-9]{9}$'
  and v.value ~ '^[0-9]{1,18}$';
SQL
)
next_value=$(tr -d '[:space:]' <<<"$next_value")
if [ "$next_value" -gt 1 ]; then
  echo "Imports will continue from value $next_value, after values imported before"
fi
api DELETE /api/dataStore/perf-tracker/uniqueAttributeValues --output /dev/null || true
api POST /api/dataStore/perf-tracker/uniqueAttributeValues --output /dev/null --fail \
  --data "{\"next\": $next_value, \"populatedValues\": $POPULATE_VALUES_PER_ORG_UNIT}"

echo "Done. Run TrackerTest with -DuniqueAttribute=$POPULATE_ATTRIBUTE_UID"
