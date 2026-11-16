#!/usr/bin/env bash
# Seeds the organisation unit search fixture (OrganisationUnitSearchFixture) into a fresh, empty
# database for OrganisationUnitSearchPerformanceTest, then restarts the application so warmup and
# measured runs start without the caches the seeding warmed. The database is kept.
#
# Author: Morten Svanæs <msvanaes@dhis2.org>
#
# The fixture reads the same properties as the simulation, so MVN_ARGS (config file, scale,
# translation density, manifest path) is passed to both. POPULATE_MVN_ARGS holds arguments for
# seeding only and must confirm it, as seeding refuses to run otherwise. Seeding is create-only and
# refuses a database that already has organisation units.
#
# Run it against a running stack from dhis-test-performance, or let run-simulation.sh run it after
# the containers start:
#
#   POPULATE_SCRIPT=scripts/populate-org-unit-search.sh \
#   POPULATE_MVN_ARGS=-DouSearch.setup=CREATE_DISPOSABLE_FIXTURE \
#   DB_TYPE=empty DHIS2_IMAGE=dhis2/core-dev:latest \
#   SIMULATION_CLASS=org.hisp.dhis.test.platform.OrganisationUnitSearchPerformanceTest \
#   MVN_ARGS="-DconfigFile=/absolute/path/ou-search.properties" \
#   ./run-simulation.sh
#
# ENVIRONMENT:
#   POPULATE_MVN_ARGS             Maven arguments for seeding only, after MVN_ARGS so they override
#                                 it (default: empty). Must confirm seeding with
#                                 -DouSearch.setup=CREATE_DISPOSABLE_FIXTURE unless the config file
#                                 sets ouSearch.setup
#   MVN_ARGS                      Maven arguments shared with the simulation (default: empty)
#   OU_SEARCH_PASSWORD            password of the synthetic users, unless the config file sets
#                                 ouSearch.password
#   DHIS2_URL                     DHIS2 base URL (default: http://localhost:8080)
#   DHIS2_USERNAME/DHIS2_PASSWORD API credentials (default: admin/district)
#   HEALTHCHECK_TIMEOUT           max wait in seconds for DHIS2 after the restart (default: 300)
set -euo pipefail

cd "$(dirname "$0")/.."

POPULATE_MVN_ARGS=${POPULATE_MVN_ARGS:-}
MVN_ARGS=${MVN_ARGS:-}
DHIS2_URL=${DHIS2_URL:-http://localhost:8080}
DHIS2_USERNAME=${DHIS2_USERNAME:-admin}
DHIS2_PASSWORD=${DHIS2_PASSWORD:-district}
HEALTHCHECK_TIMEOUT=${HEALTHCHECK_TIMEOUT:-300}

echo "Seeding the organisation unit search fixture into $DHIS2_URL"
# The password is passed in the environment, never as a command argument.
# shellcheck disable=SC2086 # MVN_ARGS and POPULATE_MVN_ARGS are lists of arguments
DHIS2_PASSWORD="$DHIS2_PASSWORD" mvn test-compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
  -Dusername="$DHIS2_USERNAME" \
  -DbaseUrl="$DHIS2_URL" \
  $MVN_ARGS \
  -DouSearch.operation=seed \
  $POPULATE_MVN_ARGS \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=org.hisp.dhis.test.platform.OrganisationUnitSearchFixture

# Restart only the application; the seeded database stays. The healthcheck sidecar shares the web
# container's network, so it restarts with it. restart keeps the containers, so this works with the
# profiling and extra compose files run-simulation.sh started them with.
echo "Restarting DHIS2 to drop the caches seeding warmed"
docker compose restart web web-healthcheck

deadline=$((SECONDS + HEALTHCHECK_TIMEOUT))
until curl --fail --silent --output /dev/null "$DHIS2_URL/api/ping"; do
  if [ "$SECONDS" -ge "$deadline" ]; then
    echo "Error: DHIS2 did not become ready within ${HEALTHCHECK_TIMEOUT}s after the restart" >&2
    docker compose logs --tail=500 web >&2 || true
    exit 1
  fi
  sleep 5
done

echo "Done. Run OrganisationUnitSearchPerformanceTest with the same MVN_ARGS"
