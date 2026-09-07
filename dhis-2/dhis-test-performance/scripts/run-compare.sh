#!/usr/bin/env bash
# Trigger a performance comparison run on GitHub Actions.
# Edit the variables below, then run: bash scripts/run-compare.sh

PERF_TESTS_GIT_REF="master"

BASELINE_IMAGE="dhis2/core-dev:latest"   # master
CANDIDATE_IMAGE="dhis2/core-dev:latest"   # perf-oumode-selected-master

SIMULATION_CLASS="org.hisp.dhis.test.tracker.TrackerTest"
WARMUP=1
MVN_ARGS="-Dprofile=smoke"

DB_TYPE="sierra-leone"
DB_VERSION="dev"

# ---------------------------------------------------------------------------

BASELINE_ENV="DHIS2_IMAGE=${BASELINE_IMAGE}
SIMULATION_CLASS=${SIMULATION_CLASS}
DB_TYPE=${DB_TYPE}
DB_VERSION=${DB_VERSION}
WARMUP=${WARMUP}
MVN_ARGS=\"${MVN_ARGS}\""

CANDIDATE_ENV="DHIS2_IMAGE=${CANDIDATE_IMAGE}
SIMULATION_CLASS=${SIMULATION_CLASS}
DB_TYPE=${DB_TYPE}
DB_VERSION=${DB_VERSION}
WARMUP=${WARMUP}
MVN_ARGS=\"${MVN_ARGS}\""

gh workflow run performance-tests-compare.yml \
  --repo dhis2/dhis2-core \
  --field perf_tests_git_ref="${PERF_TESTS_GIT_REF}" \
  --field baseline_env="${BASELINE_ENV}" \
  --field candidate_env="${CANDIDATE_ENV}"
