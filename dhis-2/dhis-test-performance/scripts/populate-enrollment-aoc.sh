#!/usr/bin/env bash
# Gives enrollments attribute option combos the TrackerTest users can't read, for benchmarking the
# enrollment attribute option combo access check of the tracker exporters (DHIS2-22552).
#
# Creates a category combo of two categories with POPULATE_OPTIONS_PER_CATEGORY category options
# each (so POPULATE_OPTIONS_PER_CATEGORY^2 category option combos) whose options only their owner
# can read, and assigns those combos to a share of the program's enrollments (via SQL). The
# exporters then exclude every one of these combos, and the events and tracked entities in those
# enrollments, for the TrackerTest users.
#
# Run it against a running stack from dhis-test-performance, or let run-simulation.sh run it after
# the containers start:
#
#   POPULATE_SCRIPT=scripts/populate-enrollment-aoc.sh POPULATE_PROFILE=worst-case \
#   DHIS2_IMAGE=dhis2/core-dev:latest SIMULATION_CLASS=org.hisp.dhis.test.tracker.TrackerTest \
#   MVN_ARGS="-Dprofile=load" \
#   ./run-simulation.sh
#
# ENVIRONMENT:
#   POPULATE_PROFILE                regression (default) or worst-case, sets the defaults below
#                                     regression: 200 options per category (40k combos)
#                                     worst-case: 1000 options per category (1M combos), enough
#                                     that the list of excluded combos may no longer fit in
#                                     work_mem as a hash table
#   POPULATE_OPTIONS_PER_CATEGORY   category options in each of the two categories
#   POPULATE_ENROLLMENT_PERCENT     percent of the program's enrollments given one of the combos
#                                   (default: 50). Picked by a hash of the enrollment UID, so the
#                                   same enrollments on every run
#   POPULATE_PROGRAM                program whose enrollments get the combos
#                                   (default: IpHINAT79UW, Child Programme)
#   DHIS2_USERNAME                  user owning the category options (default: admin). Must not be
#                                   a user the simulation runs as, since owners can read them
#   PSQL                            command running psql against the DHIS2 database, reading the
#                                   script from stdin (default: docker compose exec -T db psql
#                                   --username=dhis --dbname=dhis)
set -euo pipefail

cd "$(dirname "$0")/.."

POPULATE_PROFILE=${POPULATE_PROFILE:-regression}
case "$POPULATE_PROFILE" in
  regression)
    default_options_per_category=200
    ;;
  worst-case)
    default_options_per_category=1000
    ;;
  *)
    echo "Error: POPULATE_PROFILE must be regression or worst-case, got: $POPULATE_PROFILE" >&2
    exit 1
    ;;
esac

POPULATE_OPTIONS_PER_CATEGORY=${POPULATE_OPTIONS_PER_CATEGORY:-$default_options_per_category}
POPULATE_ENROLLMENT_PERCENT=${POPULATE_ENROLLMENT_PERCENT:-50}
POPULATE_PROGRAM=${POPULATE_PROGRAM:-IpHINAT79UW}
DHIS2_USERNAME=${DHIS2_USERNAME:-admin}
PSQL=${PSQL:-docker compose exec -T db psql --username=dhis --dbname=dhis}

for number in POPULATE_OPTIONS_PER_CATEGORY POPULATE_ENROLLMENT_PERCENT; do
  if ! [[ "${!number}" =~ ^[0-9]+$ ]]; then
    echo "Error: $number must be a non-negative integer, got: ${!number}" >&2
    exit 1
  fi
done
if [ "$POPULATE_OPTIONS_PER_CATEGORY" -lt 1 ]; then
  echo "Error: POPULATE_OPTIONS_PER_CATEGORY must be at least 1" >&2
  exit 1
fi
if [ "$POPULATE_ENROLLMENT_PERCENT" -gt 100 ]; then
  echo "Error: POPULATE_ENROLLMENT_PERCENT must be at most 100, got: $POPULATE_ENROLLMENT_PERCENT" >&2
  exit 1
fi

echo "Populating enrollment attribute option combos (profile: $POPULATE_PROFILE):" \
  "$((POPULATE_OPTIONS_PER_CATEGORY * POPULATE_OPTIONS_PER_CATEGORY)) combos," \
  "$POPULATE_ENROLLMENT_PERCENT% of the enrollments of program $POPULATE_PROGRAM"

# shellcheck disable=SC2086 # PSQL is a command with arguments
$PSQL --quiet \
  --set=owner="$DHIS2_USERNAME" \
  --set=program="$POPULATE_PROGRAM" \
  --set=options_per_category="$POPULATE_OPTIONS_PER_CATEGORY" \
  --set=enrollment_percent="$POPULATE_ENROLLMENT_PERCENT" \
  < scripts/populate/enrollment-aoc.sql

echo "Done."
