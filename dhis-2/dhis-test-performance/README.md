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

`./run-simulation.sh` is used in CI just like you do locally

* [`../../.github/workflows/run-performance-tests.yml`](../../.github/workflows/run-performance-tests.yml)
to compare performance between baseline and candidate DHIS2 versions

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

* look at Gatling's own `index.html`
* if it doesn't provide the analysis you need, try
[gatling-statistics](https://github.com/dhis2/gatling-statistics)

Since Gatling 3.12, test results are written in binary format. The `run-simulation.sh` script
automatically converts `simulation.log` to `simulation.csv` if
[glog](https://github.com/dhis2/gatling/releases) is installed like in CI.

## Raw Tests (JSON-driven)

The `raw` package (`org.hisp.dhis.test.raw`) contains JSON-driven performance tests ported from
the [performance-tests-gatling](https://github.com/dhis2/performance-tests-gatling) repository.
These tests are self-contained within the `raw` package and do not affect other test packages.

### Running Raw Tests

```sh
DHIS2_IMAGE=dhis2/core-dev:latest \
SIMULATION_CLASS=org.hisp.dhis.test.raw.GetRawSpeedTest \
MVN_ARGS="-Dscenario=test-scenarios/sierra-leone/analytics-ev-query-speed-get-test.json -Dversion=43 -Dbaseline=41" \
ANALYTICS_GENERATE=true \
./run-simulation.sh
```

> [!NOTE]
> Analytics endpoints require `ANALYTICS_GENERATE=true` to pre-generate analytics tables before
> running tests. This adds ~10 minutes to test setup but is required for analytics queries to
> succeed.

### Available Scenarios

* `test-scenarios/sierra-leone/*.json` - Sierra Leone database tests
* `test-scenarios/hmis/*.json` - HMIS database tests

> [!WARNING]
> Currently only the sierra-leone databases are available for testing. The HMIS database scenarios
> exist but require a corresponding database dump that is not yet available for you locally or in
> CI. We are working on making additional databases available.

### Configuration

Raw tests accept system properties via `MVN_ARGS`. Key parameters:

* `version` - DHIS2 version being tested (filters scenarios by comparing to `version.min`/`version.max` in JSON)
* `baseline` - DHIS2 version to use for performance expectations (selects which `expectations[].release` to assert against)
* `scenario` - Path to scenario file
* `query` - Optional: run only a specific query URL

See [ConfigLoader.java](src/test/org/hisp/dhis/test/raw/ConfigLoader.java) for all options and
defaults.

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

