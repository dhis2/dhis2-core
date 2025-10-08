# DHIS2 Performance Tests

Run Gatling performance tests against DHIS2 Docker instances locally and in CI.

## Quick Start

```sh
DHIS2_IMAGE=dhis2/core-dev:latest \
SIMULATION_CLASS=org.hisp.dhis.test.tracker.TrackerTest \
./run-simulation.sh
```

Run `./run-simulation.sh` for full usage including profiling and database options.

## Results

Test results are saved to `target/gatling/<simulation-class>-<timestamp>/`:

* `index.html` - Gatling HTML report
* `simulation.log` - Gatling binary response times and user injection profile
* `simulation.csv` - CSV version of `simulation.log` (automated if `glog` is installed)
* `simulation-run.txt` - Metadata to reproduce the run
* `profile.html` - Flamegraph visualization (if profiling enabled with PROF_ARGS)
* `profile.jfr` - JFR profiler data (if profiling enabled with PROF_ARGS)
* `profile.collapsed` - Collapsed stack traces (if profiling enabled with PROF_ARGS)
* `postgresql.log` - SQL logs (if enabled with CAPTURE_SQL_LOGS)
* `pgbadger.html` - SQL analysis report (if CAPTURE_SQL_LOGS enabled and `pgbadger` installed)

### Analysis

* look at Gatlings own `index.html`
* if it doesn't provide the analysis you need, try
[gatling-statistics](https://github.com/dhis2/gatling-statistics)

Since Gatling 3.12, test results are written in binary format. The `run-simulation.sh` script
automatically converts `simulation.log` to `simulation.csv` if
[glog](https://github.com/dhis2/gatling/releases) is installed.

## CI Usage

`./run-simulation.sh` is used in
[`../../.github/workflows/run-performance-tests.yml`](../../.github/workflows/run-performance-tests.yml)
to compare performance between baseline and candidate DHIS2 versions.

