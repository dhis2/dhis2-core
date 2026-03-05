# DHIS2 Performance Tests

Run Gatling performance tests against DHIS2 Docker instances locally and in CI.

## Usage

### Locally

```sh
DHIS2_IMAGE=dhis2/core-dev:latest \
SIMULATION_CLASS=org.hisp.dhis.test.tracker.TrackerTest \
./run-simulation.sh
```

Run `./run-simulation.sh` for full usage including profiling and database options.

## CI

CI workflows use `./run-simulation.sh` the same way as local runs:

* [`performance-tests-scheduled.yml`](../../.github/workflows/performance-tests-scheduled.yml) - Daily scheduled tests
* [`performance-tests.yml`](../../.github/workflows/performance-tests.yml) - Manual single test
* [`performance-tests-compare.yml`](../../.github/workflows/performance-tests-compare.yml) - Manual baseline vs candidate comparison

## Results

Test results are saved to `target/gatling/<simulation-class>-<timestamp>/`:

* `index.html` - Gatling HTML report
* `simulation.log` - Gatling binary response times and user injection profile
* `simulation.csv` - CSV version of `simulation.log` (automated if `glog` is installed)
* `run-simulation.env` - Complete test run metadata (read it on how to reproduce a run)
* `profile.html` - Flamegraph visualization (if profiling enabled with `PROF_ARGS)`
* `profile.jfr` - JFR profiler data (if profiling enabled with `PROF_ARGS)`
* `profile.collapsed` - Collapsed stack traces (if profiling enabled with `PROF_ARGS)`
* `postgresql.log` - SQL logs (if enabled with `CAPTURE_SQL_LOGS)`
* `pgbadger.html` - SQL analysis report (if `CAPTURE_SQL_LOGS` enabled and `pgbadger` installed)

### Analysis

* Look at Gatling's own `index.html`
* if it doesn't provide the analysis you need, try [gatling-statistics](https://github.com/dhis2/gatling-statistics)
* To compare two runs (e.g. baseline vs feature branch), use `scripts/compare-gatling-runs.sh`:

```sh
./scripts/compare-gatling-runs.sh \
  target/gatling/usersperformancetest-20260217072013445 \
  target/gatling/usersperformancetest-20260217073019128
```


This requires [gstat](https://github.com/dhis2/gatling-statistics) to be installed
and prints a GitHub markdown table of p50/p95 differences between the two runs. This 
table can be useful to include in your PR review. 

The comparison script uses `gstat` output, not Gatling's `index.html`. The percentile values are
good for relative baseline-vs-candidate comparison when both runs are processed the same way, but
they may differ slightly from the numbers shown in Gatling's HTML report due to differences in
percentile calculation. If exact parity with Gatling's UI matters, use `index.html` as the source
of truth.

Since Gatling 3.12, test results are written in binary format. The `run-simulation.sh` script
automatically converts `simulation.log` to `simulation.csv` if
[glog](https://github.com/dhis2/gatling/releases) is installed like in CI. 


## Platform Tests

The `platform` package (`org.hisp.dhis.test.platform`) contains focused CRUD performance tests.

### UsersPerformanceTest

Tests single-user CRUD operations on `/api/users` (POST, GET, PUT, PATCH, DELETE).

All properties have defaults targeting the Sierra Leone demo DB on `localhost:8080`, so no
configuration is needed for a local run:

```sh
mvn gatling:test -Dgatling.simulationClass=org.hisp.dhis.test.platform.UsersPerformanceTest \
  --file dhis-2/pom.xml -pl dhis-test-performance
```

To run against a remote instance, create a local `.properties` file (do not commit credentials):

```properties
baseUrl=https://your-instance.example.org/dhis
username=admin
password=changeme

# UIDs from your target DB — find them via /api/userRoles, /api/organisationUnits, /api/userGroups
# Point userGroupUid at a large group (10k+ members) to amplify N+1 effects
userRoleUid=<userRoleUid>
orgUnitUid=<rootOrgUnitUid>
userGroupUid=<largeUserGroupUid>

iterations=10
mode=sequential
```

Then pass it via `-DconfigFile`:

```sh
mvn gatling:test -Dgatling.simulationClass=org.hisp.dhis.test.platform.UsersPerformanceTest \
  -DconfigFile=/path/to/my-instance.properties \
  --file dhis-2/pom.xml -pl dhis-test-performance
```

Individual `-D` flags always override values from the config file. Available properties:

| Property | Default | Description |
|:---|:---|:---|
| `configFile` | — | Path to a `.properties` file |
| `baseUrl` | `http://localhost:8080` | DHIS2 base URL |
| `username` | `admin` | API username |
| `password` | `district` | API password |
| `userRoleUid` | `Euq3XfEIEbx` | UID of the user role assigned to test users |
| `orgUnitUid` | `ImspTQPwCqd` | UID of the org unit assigned to test users |
| `userGroupUid` | `wl5cDMuUhmF` | UID of a user group to assign (leave blank to skip) |
| `iterations` | `3` | Requests per scenario |
| `mode` | `parallel` | `parallel` or `sequential` |

### UserGroupMembershipPerformanceTest

Tests the user-group membership workflow on `/api/userGroups`:

* `POST` create a new group with an initial user set
* `PATCH` replace the `users` collection with a larger set
* `PUT` full-replace the group with another user set
* `DELETE` remove the group

The test discovers a small set of existing user IDs during setup and reuses them for every
iteration, which keeps timings focused on membership updates rather than user creation.

```sh
mvn gatling:test -Dgatling.simulationClass=org.hisp.dhis.test.platform.UserGroupMembershipPerformanceTest \
  --file dhis-2/pom.xml -pl dhis-test-performance
```

Available properties:

| Property | Default | Description |
|:---|:---|:---|
| `configFile` | — | Path to a `.properties` file |
| `baseUrl` | `http://localhost:8080` | DHIS2 base URL |
| `username` | `admin` | API username |
| `password` | `district` | API password |
| `iterations` | `3` | Workflow iterations |
| `initialUserCount` | `3` | Users included in the create request |
| `patchUserCount` | `6` | Users included after the PATCH replace |
| `putUserCount` | `9` | Users included after the PUT full replace |

## Raw Tests (JSON-driven)

The `raw` package (`org.hisp.dhis.test.raw`) contains JSON-driven performance tests ported from
the [performance-tests-gatling](https://github.com/dhis2/performance-tests-gatling) repository.
These tests are self-contained within the `raw` package and do not affect other test packages.

### Running Raw Tests

Run `./run-simulation.sh` for full usage including raw test examples.

> [!NOTE]
> Analytics endpoints require `ANALYTICS_GENERATE=true` to pre-generate analytics tables before
> running tests. This adds ~10 minutes to test setup but is required for analytics queries to
> succeed.

### Available Scenarios

* `test-scenarios/sierra-leone/*.json` - Sierra Leone database tests
* `test-scenarios/hmis/*.json` - HMIS database tests

### Configuration

Raw tests accept system properties via `MVN_ARGS`. Key parameters:

* `version` - DHIS2 version being tested (filters scenarios by comparing to `version.min`/`version.max` in JSON)
* `baseline` - DHIS2 version to use for performance expectations (selects which `expectations[].release` to assert against)
* `scenario` - Path to scenario file
* `query` - Optional: run only a specific query URL

See [ConfigLoader.java](src/test/java/org/hisp/dhis/test/raw/ConfigLoader.java) for all options
and defaults.

### Scenario File Structure

Each scenario file contains a list of test scenarios with performance expectations:

```json
{
  "scenarios": [
    {
      "query": "/api/analytics?dimension=dx:D6Z8vC4lHkk,pe:LAST_12_MONTHS&filter=ou:USER_ORGUNIT",
      "expectations": [
        {
          "release": "42.0",
          "min": 85,
          "max": 220,
          "mean": 150,
          "ninetyPercentile": 180
        }
      ],
      "version": {
        "min": "42.0"
      }
    }
  ]
}
```

* `query`: API endpoint to test
* `expectations`: Expected response times (ms) for specific DHIS2 releases
* `version`: DHIS2 version range where this scenario is valid (usually only `min` is needed)

### Adding Scenarios

**Add to existing file:**

1. Navigate to `src/test/resources/test-scenarios/<database>/`
2. Edit the appropriate JSON file
3. Add a new scenario object to the `scenarios` array

**Use external scenario file:**

Point to any JSON file using an absolute path:

```sh
MVN_ARGS="-Dscenario=/path/to/my-scenarios.json -Dversion=42 -Dbaseline=42"
```

## Databases

Performance tests only support databases hosted in S3 (see `run-simulation.sh` for details). See the
[operations handbook](https://github.com/dhis2/operations-handbook?tab=readme-ov-file#modifying-the-demo-dbs)
for modifying them.

### Database Image Caching on CI

Database images are cached on the CI server to avoid restoring dumps on every run. The S3 dumps are
mutable, so cached images (e.g., `localhost/dhis2-postgres:14-3.5-sierra-leone-dev`) can become
stale when the source dump is updated.

To refresh a cached image, a `#team-devops` member must run on the CI server:

```sh
# Remove the specific cached image (adjust tag as needed)
docker rmi localhost/dhis2-postgres:14-3.5-sierra-leone-dev
docker builder prune -a
```

Use `-a` to remove all cache layers, not just unused ones.
