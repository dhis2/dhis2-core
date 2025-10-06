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
* `simulation.csv` - CSV version of `simulation.log` (automated in CI only, [see
below](#simulationcsv))
* `simulation-run.txt` - Metadata to reproduce the run
* `profile.html` - Flamegraph visualization (if profiling enabled with PROF_ARGS)
* `profile.jfr` - JFR profiler data (if profiling enabled with PROF_ARGS)
* `profile.collapsed` - Collapsed stack traces (if profiling enabled with PROF_ARGS)

### Analysis

* look at Gatlings own `index.html`
* if it doesn't provide the analysis you need, try
[gatling-statistics](https://github.com/dhis2/gatling-statistics)

Since Gatling 3.12, test results are written in binary format. For local runs you'll need
[glog](https://github.com/dhis2/gatling/releases) (a CLI from our Gatling fork) to convert:

```sh
glog --config ./src/test/resources/gatling.conf --scan-subdirs target/gatling
```

## CI Usage

`./run-simulation.sh` is used in
[`../../.github/workflows/run-performance-tests.yml`](../../.github/workflows/run-performance-tests.yml)
to compare performance between baseline and candidate DHIS2 versions.

