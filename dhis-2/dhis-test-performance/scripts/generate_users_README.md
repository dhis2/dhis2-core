# DHIS2 User Performance Test Suite

Performance tests for the `/api/users` endpoint, investigating N+1 query issues and relationship loading performance (DHIS2-20614).

## Overview

The user API performance test generates 50,000 users distributed across org units with realistic role distributions, then runs 8 Gatling scenarios that exercise different query patterns known to trigger performance issues.

### Test Scenarios

| # | Scenario | Endpoint | Purpose |
|---|----------|----------|---------|
| 1 | Basic user list | `GET /api/users?pageSize=50` | Baseline |
| 2 | All fields expansion | `GET /api/users?fields=*&pageSize=50` | Triggers lazy loading of ALL relationships |
| 3 | User roles expansion | `GET /api/users?fields=id,name,userRoles[id,name]&pageSize=50` | N+1 on userRoles |
| 4 | User groups expansion | `GET /api/users?fields=id,name,userGroups[id,name]&pageSize=50` | N+1 on userGroups |
| 5 | Org units expansion | `GET /api/users?fields=id,name,organisationUnits[id,name]&pageSize=50` | N+1 on org units |
| 6 | Combined common fields | `GET /api/users?fields=id,name,username,userRoles[...],userGroups[...],organisationUnits[...]&pageSize=50` | Realistic admin UI query |
| 7 | Query filter (search) | `GET /api/users?query=perftest&pageSize=50` | Tests `getPreQueryMatches` + HQL path |
| 8 | Large page size | `GET /api/users?fields=id,name,userRoles[id,name]&pageSize=500` | Amplifies N+1 issues |

### Test Data

- **50,000 users** across 10 batch files (`users_0000.json` - `users_0009.json`, 5k each)
- **10,000 org units** (from `orgunits_0000.json`, used for user org unit assignments)
- Users are distributed with realistic role types:
  - Super Admin (0.1%): Level 1 org units
  - Regional Admin (1%): Level 2 org units
  - District Manager (5%): Level 3 org units
  - Facility Staff (25%): Level 4-5 org units
  - Field Worker (68.9%): Lowest level org units

## How to Run

### Using Docker (run-simulation.sh)

```bash
cd dhis-2/dhis-test-performance

# Run user API perf test against latest dev build
DHIS2_IMAGE=dhis2/core-dev:latest \
SIMULATION_CLASS=org.hisp.dhis.test.platform.UsersPerformanceTest \
MVN_ARGS="-DskipImport=false -DuserFiles=10 -DorgunitFiles=3" \
./run-simulation.sh

# Run against a specific release
DHIS2_IMAGE=dhis2/core:2.41.0 \
SIMULATION_CLASS=org.hisp.dhis.test.platform.UsersPerformanceTest \
MVN_ARGS="-DskipImport=false -DuserFiles=10 -DorgunitFiles=3" \
./run-simulation.sh

# Run with CPU profiling
PROF_ARGS="-e cpu" \
DHIS2_IMAGE=dhis2/core-dev:latest \
SIMULATION_CLASS=org.hisp.dhis.test.platform.UsersPerformanceTest \
MVN_ARGS="-DskipImport=false -DuserFiles=10 -DorgunitFiles=3" \
./run-simulation.sh

# Run with SQL logging for query analysis
CAPTURE_SQL_LOGS=true \
DHIS2_IMAGE=dhis2/core-dev:latest \
SIMULATION_CLASS=org.hisp.dhis.test.platform.UsersPerformanceTest \
MVN_ARGS="-DskipImport=false -DuserFiles=10 -DorgunitFiles=3" \
./run-simulation.sh

# Skip import if data already loaded (re-run test only)
DHIS2_IMAGE=dhis2/core-dev:latest \
SIMULATION_CLASS=org.hisp.dhis.test.platform.UsersPerformanceTest \
MVN_ARGS="-DskipImport=true" \
./run-simulation.sh
```

### Against a Running Server (no Docker)

```bash
cd dhis-2/dhis-test-performance

# Import data + run test against external server
mvn gatling:test \
  -Dgatling.simulationClass=org.hisp.dhis.test.platform.UsersPerformanceTest \
  -DbaseUrl=http://your-server:8080 \
  -Dusername=admin \
  -Dpassword=district \
  -DskipImport=false \
  -DuserFiles=10 \
  -DorgunitFiles=3
```

### Compare Two Versions

```bash
# Step 1: Run baseline
DHIS2_IMAGE=dhis2/core:2.41.0 \
REPORT_SUFFIX=baseline \
SIMULATION_CLASS=org.hisp.dhis.test.platform.UsersPerformanceTest \
MVN_ARGS="-DskipImport=false -DuserFiles=10 -DorgunitFiles=3" \
./run-simulation.sh

# Step 2: Run candidate
DHIS2_IMAGE=dhis2/core-dev:latest \
REPORT_SUFFIX=candidate \
SIMULATION_CLASS=org.hisp.dhis.test.platform.UsersPerformanceTest \
MVN_ARGS="-DskipImport=false -DuserFiles=10 -DorgunitFiles=3" \
./run-simulation.sh

# Results in target/gatling/ - compare the HTML reports
```

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `baseUrl` | `http://localhost:8080` | DHIS2 server URL |
| `username` | `admin` | Authentication username |
| `password` | `district` | Authentication password |
| `userFiles` | `10` | Number of user batch files to import (0-9 = up to 50k users) |
| `orgunitFiles` | `3` | Number of org unit files to import (users reference these) |
| `skipImport` | `false` | Skip all metadata import |
| `skipOrgUnitImport` | `false` | Skip org unit import only (if already loaded) |

## Regenerating Test Data

The test data was generated using `doc/generate_users.py`:

```bash
cd dhis-2/doc
python3 generate_users.py \
  --orgunits ../dhis-test-performance/src/test/resources/platform/orgunits/orgunits_0000.json \
  --target 50000 \
  --batch-size 5000 \
  --output ../dhis-test-performance/src/test/resources/platform/users/users.json \
  --seed 42
```

The `--seed 42` ensures reproducible output. The script assigns users to org units from the first org unit batch file with the following distribution: Super Admin (0.1%), Regional Admin (1%), District Manager (5%), Facility Staff (25%), Field Worker (68.9%).

All generated users are assigned the `Superuser` role (`yrB6vc5Ip3r`), which exists in the Sierra Leone demo database.

## Performance Thresholds

Initial thresholds (generous, to be tightened after baseline data):

| Scenario | P95 Threshold |
|----------|---------------|
| Basic list | < 5s |
| All fields | < 15s |
| Individual expansions (roles/groups/orgunits) | < 10s |
| Combined fields | < 15s |
| Query filter | < 10s |
| Large page size | < 15s |
