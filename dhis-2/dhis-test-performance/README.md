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

### OrganisationUnitSearchPerformanceTest

Shared reproducible benchmark for translated filtering (Core #25128), hierarchy scoping
(Core #25127), and Data Entry #593. Use the same benchmark branch, fixture manifest and database
snapshot for every variant. The backend baseline is **`7b38c692f5c`** (January-merged master);
compare it with translated-only, hierarchy-only, and both fixes. Do not substitute current master.
The existing Data Entry baseline already has a 200 ms debounce.

#### Explicit setup (never part of Gatling)

Run these commands **from `dhis-2/dhis-test-performance`**, with Java 17+ and Maven.
Use a fresh, disposable database (DHIS2 initialized, admin account and default categories present,
**no organisation units**). Setup is create-only and refuses a nonempty OU tree. It does not
reset a database or update existing metadata, and never runs automatically from a simulation.

Keep an untracked, permission-restricted `/absolute/path/ou-search.properties`:

```properties
baseUrl=http://localhost:8087
username=admin
ouSearch.manifest=/absolute/path/ou-search-manifest.json
ouSearch.leafCount=272000
ouSearch.translationDensity=mixed
ouSearch.batchSize=1000
iterations=3
concurrency=1
mode=sequential
```

For browser runs, optionally add `ouSearch.corsOrigins=http://localhost:3003,http://localhost:3004`.
During explicitly authorized setup these exact origins are added to the disposable instance's
CORS allowlist, preserving existing entries. Without this option configuration is unchanged.
Use origins only, matching the app URLs; do not allow every origin or target a production instance.

Supply administrator `password` and synthetic-user `ouSearch.password` in that private file,
or export `DHIS2_PASSWORD` and `OU_SEARCH_PASSWORD` through your secret manager. The latter
must satisfy the instance's password policy and is shared by the seven **synthetic** accounts;
never use a real person's password. There are no built-in credentials. `-D` values override
the config file; password environment variables are fallbacks. Do not put secrets in command
arguments, published manifests, reports, or committed files.

```sh
# Generate only: deterministic JSON manifest, no network or database mutation.
mvn test-compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=org.hisp.dhis.test.platform.OrganisationUnitSearchFixture \
  -DconfigFile=/absolute/path/ou-search.properties

# Seed once: synchronous, checked metadata API batches, then write the same manifest.
mvn test-compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=org.hisp.dhis.test.platform.OrganisationUnitSearchFixture \
  -DconfigFile=/absolute/path/ou-search.properties \
  -DouSearch.operation=seed -DouSearch.setup=CREATE_DISPOSABLE_FIXTURE
```

`ouSearch.leafCount` must be at least 200. `translationDensity=none|mixed|dense` means
zero, even-indexed, or all leaves have French NAME translations. Fixture version 1 contains
one country, two districts, 200 wards, and the configured leaves (default 272,203 OUs total).
UIDs, names, translations and contiguous leaf-to-ward assignment are deterministic. All leaves
match `Bench`; at most 20 match `Needle`/`okp`. French translated leaves match `Traduit`;
`Fallback` matches untranslated names. `ZZZ_NO_SUCH_OU_918273` never matches. Country,
districts, and ward zero also contain `okp`, making hierarchy-root inclusion observable.

Users `oubench_single`, `oubench_multi`, `oubench_overlap`, `oubench_small`,
`oubench_large`, `oubench_french`, and `oubench_client` have, respectively: district zero,
both disjoint districts, district zero plus its ward zero, ward zero, country, country,
and country capture/view/search roots. Their DB locales are explicitly English except
`oubench_french` (`keyDbLocale=fr`). A real monthly data set and aggregate data element
are created, with data-write sharing and role access. The first 200 leaves and all rare
replacement matches are assigned to the data set, so both browser pages and replacements
are usable for Data Entry. No generated giant metadata dump is committed.

**Manifest schema (`fixtureVersion: 1`):**

* Top-level `leafCount`, `wardCount`, `organisationUnitCount`, `translationDensity`, `rootId`.
* `users`: keyed by the seven account labels, each `{id, username, dbLocale, roots: [uid]}`.
* `cases`: `{name, user, endpoint, params, url, expected}`. `params` contains exact string
  query values (or string arrays for repeated parameters); `url` is a relative, encoded URL.
  `expected` contains ordered `ids`, `paths`, `displayNames`, independent `total`, and
  `checkTotal`. All expectations are calculated from generated data, never search responses.
* `client`: `{username, dataSetId, dataSetName, searchTerm, replacementTerm, pageSize,
  firstPagePaths, secondPagePaths, replacementPaths, total, replacementTotal, orgUnits}`.
  `orgUnits` is `{id,path,displayName}[]` for these results and their ancestors.
  Client pages are sorted by `id:asc`, 50 rows per page. Feed the manifest file path to
  `Cypress.env('ouSearchManifest')`; supply browser API URL/login separately using its
  normal Cypress configuration.

No credentials or target base URL are written into the manifest. Generate-only does not
certify that a database was seeded. After partial setup failure, restore a fresh database
and repeat setup; do not retry against the half-imported tree. To reset/change scale or
density, replace the disposable database, regenerate and reseed. Preserve the manifest
beside the snapshot; changes to fixture semantics require a fixture-version bump.

#### One run and comparisons

```sh
mvn gatling:test \
  -Dgatling.simulationClass=org.hisp.dhis.test.platform.OrganisationUnitSearchPerformanceTest \
  -DconfigFile=/absolute/path/ou-search.properties

# Focus on an identical pair of cases for each application variant.
mvn gatling:test \
  -Dgatling.simulationClass=org.hisp.dhis.test.platform.OrganisationUnitSearchPerformanceTest \
  -DconfigFile=/absolute/path/ou-search.properties \
  -DouSearch.cases=display-french-selective,hierarchy-single-count \
  -Diterations=10 -Dconcurrency=4
```

Defaults: all 66 distinct cases, `iterations=3`, `concurrency=1`, `mode=sequential`.
`ouSearch.cases` is `all` or comma-separated exact manifest case names; unknown names fail.
`concurrency` is virtual users **per case**, each doing exactly `iterations` searches.
`mode=parallel` starts all selected cases concurrently; sequential runs one case population
at a time. Every virtual user authenticates once under a separately named request; the
timed searches reuse its session cookie and do not send Basic authentication.

The suite covers scoped and unscoped broad/selective/no-match/translated/fallback
`displayName:ilike`, second pages, path-only Data Entry requests, hierarchy
`query=okp&withinUserSearchHierarchy=true` list/count requests, explicit roots, and
`rootJunction=OR` intersections with mandatory scopes (including an outside-scope district).
Responses must have the exact ordered IDs, paths and display names requested by each case;
path-only client cases check paths. All paged cases also require the exact `pager.total`.
Empty/incorrect responses are KOs, not performance wins.
Current Core always counts paged collections; `totalPages` is not a supported switch here.
The fixture sends only supported parameters and does not duplicate a request merely to
toggle a count assertion. Case names ending in `-count` emphasize count/scope correctness,
not a separate count-only API.
Known baseline semantic failures remain visible and make the correctness assertion fail;
record their status separately from latency, and retain the failed Gatling report.
There is no default latency threshold. Optionally set `ouSearch.p95Ms` to a deliberately
chosen positive p95 upper bound; authentication is excluded from that per-case bound.

For each image/WAR, restore the **same seeded snapshot**, start the application, and run
`iterations=1,concurrency=1` for an application-cold observation. Restart before each
individual cold case if measuring more than one: a sequential matrix shares warmed state.
Restarting the application does not clear PostgreSQL/OS caches; label that distinction.
For warm results, perform an explicitly labelled warmup then run the same command without
restarting. Repeat with `concurrency=4` (or your chosen level), preserving case order, scale,
translation density, database/JVM/cache settings and hardware. Never include seed timings.
Use normal `target/gatling` reports and `gstat compare` above; do not discard semantic KOs
or describe one request as a meaningful p95 sample.

#### Existing runner / CI

`DB_TYPE=empty` builds a fresh PostGIS database locally without downloading a dump or
requiring an S3 artifact. Leave `DB_DIR` unset; `DB_VERSION` remains an image-cache label.
The normal Sierra Leone, HMIS, and platform-perf modes are unchanged.

The optional `FIXTURE_SETUP_CLASS` / `FIXTURE_SETUP_ARGS` runner hook invokes a Java main
class with the test classpath exactly once after application readiness, before analytics,
profiling, warmup or measured requests. It forwards `MVN_ARGS` (the same config, scale,
translation-density and manifest properties as the simulation), then setup-only arguments.
It restarts only `web`/`web-healthcheck` and waits for readiness, keeping the database intact.
Warmup and measurement reuse that fixture and manifest without resetting or re-seeding.
A subsequent runner invocation starts a fresh container database and seeds deterministically
again. With no setup class, the hook does nothing; normal Gatling execution remains read-only.

Provide the private config and synthetic password as above; for example:

```sh
DHIS2_IMAGE=your-candidate-image \
SIMULATION_CLASS=org.hisp.dhis.test.platform.OrganisationUnitSearchPerformanceTest \
DB_DIR= DB_TYPE=empty DB_VERSION=ou-search-v1 \
FIXTURE_SETUP_CLASS=org.hisp.dhis.test.platform.OrganisationUnitSearchFixture \
FIXTURE_SETUP_ARGS="-DouSearch.operation=seed -DouSearch.setup=CREATE_DISPOSABLE_FIXTURE" \
WARMUP=0 REPORT_SUFFIX=application-cold HEALTHCHECK_TIMEOUT=900 \
MVN_ARGS="-DconfigFile=/absolute/path/ou-search.properties -DbaseUrl=http://localhost:8080 -DouSearch.manifest=target/gatling/ou-search-manifest.json -Diterations=1" \
./run-simulation.sh
```

Use `WARMUP=1`, an appropriate iteration count, and a warm report suffix for warm comparison.
With `WARMUP=0`, this is application-cold after seeding, not PostgreSQL/OS-cache cold.
The runner marks warmup `gatling.failOnError=false`; still retain/review those KOs.
Without `FIXTURE_SETUP_CLASS`, `DB_TYPE=empty` only provisions an empty database; it does
not manufacture a manifest or make unrelated simulations fixture-aware. The explicit
`ouSearch.setup` confirmation belongs in `FIXTURE_SETUP_ARGS` as shown; merely selecting
the fixture's class without that confirmation cannot seed a target.

Existing comparison CI's `perf_tests_git_ref` selects the **same shared benchmark branch**
for both Core PR images. Supply the command's environment settings to both `baseline_env`
and `candidate_env`, varying only the image/report identity; both get identical generated
trees and expected results without a privately published database. The existing comparison
workflow permits baseline assertion failure so it can finish both runs: preserve baseline
semantic KOs rather than treating that workflow continuation as a correctness pass.
Keep the generated manifest with the report artifacts and compare its fixture parameters.
The example places it under `target/gatling` so the existing CI artifact upload includes it.
No workflow change is needed. Keep synthetic secrets in environment or private config,
not `MVN_ARGS` or `FIXTURE_SETUP_ARGS`. The runner records its own `DHIS2_PASSWORD` in `run-simulation.env`;
redact that existing metadata before publishing any report archive.

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
