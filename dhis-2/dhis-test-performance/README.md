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

### ETag cache A/B (`PageLoadSimulation`)

Protocol, dated numbers, and scripts:

* [`ETAG-CACHE-TEAM-REPORT.html`](./ETAG-CACHE-TEAM-REPORT.html) — **pretty team handout** (open in a browser)  
* [`ETAG-CACHE-CHARTS-REPORT.html`](./ETAG-CACHE-CHARTS-REPORT.html) — **charts from real Gatling CSVs**  
* [`BENCHMARKS-etag.md`](./BENCHMARKS-etag.md) — latency/SQL/flamegraph results + how to reproduce  
* [`MEMORY-etag.md`](./MEMORY-etag.md) — RAM design bounds, gauges, alloc A/B  
* `scripts/etag-ab-benchmark.sh` — Docker ON vs OFF via `docker/dhis-etag-on.conf` / `dhis-etag-off.conf`  
* `scripts/etag-ab-live.sh` — against an already-running instance (e.g. minibox)

**Charts report offline limitation:** `ETAG-CACHE-CHARTS-REPORT.html` loads Chart.js from a CDN.
Open it with network access (or vendor `chart.umd.min.js` next to the HTML); offline/CSP-blocked
environments render the page chrome but leave the chart canvases blank. The underlying numbers
live in the inline JSON in that HTML and in `BENCHMARKS-etag.md`.

### Metadata mutation benchmark (`MetadataMutationSimulation`)

Measures the ETag cache under concurrent metadata WRITES (the page-load suite above is
read-only, leaving the DML-observer -> version-bump -> ETag-rotation path idle). A dedicated
pool of objects with name prefix `PERF_` is seeded before the run and deleted afterwards
(startup also cleans leftovers from crashed runs), so runs are repeatable. Writers cycle
UPDATE, and every 10th iteration CREATE + DELETE, so the observer sees all three DML ops.

Profiles (`-Dprofile`):

* `writeload` — page-load readers + writers at `writeRate` writes/sec; asserts the 304 share
  stays above `assertMin304`. With `-DwriteTarget=control` the writers mutate `constants`
  (which readers never fetch) and the assertion proves per-type isolation.
* `staleness` — after each mutation, immediately re-GET the affected list with the
  pre-mutation ETag and count attempts until 200; asserts p99 <= 2 attempts.
* `writecost` — writers only, unpaced; run once per `cache.api.etag.enabled` side and compare
  throughput/p95 to get the observer's write-path cost.

> [!NOTE]
> All load tests here log every virtual user in as the same account. DHIS2 evicts the
> oldest session beyond `max.sessions.per_user` (default 10), which shows up as a 401
> storm once concurrent users exceed the cap (seen with the `capacity` profile). Raise
> it in dhis.conf on the target instance when driving >10 concurrent virtual users.

Wrapper (staircase over `RATES` for writeload, CSV summary `rate,requests,rps,share304,p95ms,ko`
under `target/gatling/`):

```sh
INSTANCE=http://127.0.0.1:8280 ADMIN_PASSWORD=district \
  PROFILE=writeload ./scripts/etag-mutation-bench.sh

# writecost A/B:
INSTANCE=http://127.0.0.1:8280 PROFILE=writecost SIDE=on  ./scripts/etag-mutation-bench.sh
#   flip cache.api.etag.enabled=off in dhis.conf + restart, then:
INSTANCE=http://127.0.0.1:8280 PROFILE=writecost SIDE=off ./scripts/etag-mutation-bench.sh
```

System properties:

| Property | Default | Description |
|:---|:---|:---|
| `instance` | `http://localhost:8080` | DHIS2 base URL |
| `apiVersion` | `44` | Versioned API prefix used by readers/writers (set 43 when targeting a 2.43 instance) |
| `adminUser` / `adminPassword` | `admin` / `district` | Credentials (session-cookie login) |
| `profile` | `writeload` | `writeload`, `staleness` or `writecost` |
| `writeRate` | `1.0` | Aggregate writes/sec across all writers (0 = readers only) |
| `writeTarget` | `hot` | `hot` = dataElements (read by readers), `control` = constants |
| `readers` | `10` | Concurrent page-load readers (writeload) |
| `writers` | `2` | Concurrent writers / staleness probers |
| `durationSec` | `120` | Steady-state duration |
| `poolSize` | `200` | Seeded objects per type |
| `assertMin304` | `0.5` hot / `0.75` control | Minimum 304 share for writeload |

## CI

CI workflows use `./run-simulation.sh` the same way as local runs:

* [`performance-tests-scheduled.yml`](../../.github/workflows/performance-tests-scheduled.yml) - Daily scheduled tests
* [`performance-tests.yml`](../../.github/workflows/performance-tests.yml) - Manual single test
* [`performance-tests-compare.yml`](../../.github/workflows/performance-tests-compare.yml) - Manual baseline vs candidate comparison

Performance tests run on a single shared [self-hosted
runner](https://github.com/dhis2/dhis2-core/actions/runners?tab=self-hosted) with exclusive access.
Iterate locally; use CI only to publish numbers for PRs or release notes.

## Results

Test results are saved to `target/gatling/<simulation-class>-<timestamp>/`:

* `index.html` - Gatling HTML report
* `simulation.log` - Gatling binary response times and user injection profile
* `simulation.csv` - CSV version of `simulation.log` (automated if `glog` is installed)
* `run-simulation.env` - Complete test run metadata (read it on how to reproduce a run)
* `profile.html` - Flamegraph visualization (if profiling enabled with `PROF_ARGS`)
* `profile.jfr` - JFR profiler data (if profiling enabled with `PROF_ARGS`)
* `profile.collapsed` - Collapsed stack traces (if profiling enabled with `PROF_ARGS`)
* `dhis.log` - DHIS2 application log (if `CAPTURE_DHIS2_LOGS` enabled)
* `postgresql.log` - SQL logs (if enabled with `CAPTURE_SQL_LOGS`)
* `pgbadger.html` - SQL analysis report (if `CAPTURE_SQL_LOGS` enabled and `pgbadger` installed)
* `gc.log` - JVM GC and safepoint logs (always captured)

### Analysis

* Look at Gatling's own `index.html`
* Analyze `gc.log` with [Eclipse Jifa](https://github.com/eclipse-jifa/jifa) (`bash jifa.sh gc.log`)
* If `index.html` doesn't provide the analysis you need, try [gatling-statistics](https://github.com/dhis2/gatling-statistics)
* To compare two runs (e.g. baseline vs feature branch), use
[gstat](https://github.com/dhis2/gatling-statistics) directly:

```sh
gstat compare \
  target/gatling/usersperformancetest-20260217072013445 \
  target/gatling/usersperformancetest-20260217073019128
```

This prints a GitHub markdown table of p50/p95 differences between the two runs. This table can be
useful to include in your PR review. Each side may also be a directory containing multiple
`<simulation>-<timestamp>` runs; gstat then computes percentiles over the combined sample for that
side automatically (same behavior as `gstat --combine` for non-compare output, but always on for
`compare` since a row per request needs a single value per side). Pass `--exclude warmup` to drop
warmup runs.

`gstat` percentiles are computed over the full sample, not Gatling's `index.html` t-digest, so they
may differ slightly from the numbers shown in Gatling's HTML report. If exact parity with Gatling's
UI matters, use `index.html` as the source of truth.

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

### OrganisationUnitUsersFieldFilterPerformanceTest

Regression guard for DHIS2-21867: `GET /api/organisationUnits/{uid}?fields=id,name,users[id,name,userRoles[id,name]]`
used to force an N+1 lazy-load storm (250,054 JDBC queries / ~70s on the platform-perf DB's root org
unit) because `FieldPathHelper.visitFieldPath` walked into every requested sub-path under a
`@PropertyTransformer` property, invoking `getUserRoles()` on every member user while looking for
`access`/`sharing` segments a transformer-backed subtree can never contain. Fixed in #24514.

```sh
mvn gatling:test -Dgatling.simulationClass=org.hisp.dhis.test.platform.OrganisationUnitUsersFieldFilterPerformanceTest \
  --file dhis-2/pom.xml -pl dhis-test-performance
```

Available properties:

| Property | Default | Description |
|:---|:---|:---|
| `configFile` | — | Path to a `.properties` file |
| `baseUrl` | `http://localhost:8080` | DHIS2 base URL |
| `username` | `admin` | API username |
| `password` | `district` | API password |
| `orgUnitUid` | `VCCdfC9pvMA` | Org unit UID (platform-perf root org unit, ~250k users) |
| `fields` | `id,name,users[id,name,userRoles[id,name]]` | `fields` query param (the DHIS2-21867 repro) |
| `iterations` | `3` | Requests to run |

## Tracker Tests

The `tracker` package (`org.hisp.dhis.test.tracker`) tests the Tracker API using three Sierra Leone
demo DB programs:

* **MNCH / PNC (Adult Woman)** (`uy2gU8kT1jF`) -- tracker program with 4 stages
* **Child Programme** (`IpHINAT79UW`) -- tracker program with 2 stages
* **Antenatal care visit** (`lxAQ7Zs9VYR`) -- event program

Import data is pre-generated from [Synthea](https://github.com/synthetichealth/synthea) synthetic
patient data (ndjson.gz, one JSON object per line). Files are stored in S3
(`s3://databases.dhis2.org/tracker/synthea/import/`) and fetched automatically by TrackerTest
with ETag-based caching (`~/.cache/dhis2/perf/tracker/`).

### Generating import payloads

Requires Synthea 4.0.0 installed to local Maven repo:

```sh
cd ~/code/dhis2/synthea && git checkout v4.0.0 && ./gradlew publishToMavenLocal
```

Then generate:

```sh
mvn test-compile exec:java \
  -Dexec.mainClass=org.hisp.dhis.test.tracker.SyntheaToNdjson \
  -Dexec.classpathScope=test \
  -Dexec.args="--population 30000 --seed 12345 --output-dir src/test/resources/tracker"
```

To update the files used by CI, upload to S3:

```sh
for f in mnch child anc; do
  aws s3 cp src/test/resources/tracker/$f.ndjson.gz \
    s3://databases.dhis2.org/tracker/synthea/import/
done
```

### Running

```sh
mvn gatling:test \
  -Dgatling.simulationClass=org.hisp.dhis.test.tracker.TrackerTest \
  -Dprofile=smoke
```

Run export only (skip import, DB must be seeded): `-DtestMode=export`

See `TrackerTest.java` javadoc for all available profiles and parameters.

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

## Recording Traffic

Gatling Recorder captures HTTP requests as you interact with DHIS2 and generates a simulation that
replays them. Use it to capture production workflows and replay them in a testing environment, for
example to evaluate performance after a DHIS2 upgrade or to conduct load testing.

See [Recording DHIS2 Traffic with Gatling Recorder](RECORDING.md) for the full guide.
