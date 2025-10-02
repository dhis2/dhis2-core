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
* `simulation.log` - Binary response times
* `simulation.csv` - Response times (automated in CI only, [see below](#simulationcsv))
* `simulation-run.txt` - Run metadata
* `profile.html` - Flamegraph visualization (if profiling enabled with PROF_ARGS)
* `profile.jfr` - JFR profiler data (if profiling enabled with PROF_ARGS)
* `profile.collapsed` - Collapsed stack traces (if profiling enabled with PROF_ARGS)

### simulation.csv

If `index.html` doesn't provide the analysis you need, convert `simulation.log` to `simulation.csv`
for advanced analysis with [gatling-statistics](https://github.com/dhis2/gatling-statistics).

Since Gatling 3.12, test results are written in binary format. Use
[glog](https://github.com/dhis2/gatling/releases) (a CLI from our Gatling fork) to convert:

```sh
glog --config ./src/test/resources/gatling.conf --scan-subdirs target/gatling
```

## CI Usage

`./run-simulation.sh` is used in
[`../../.github/workflows/run-performance-tests.yml`](../../.github/workflows/run-performance-tests.yml)
to compare performance between baseline and candidate DHIS2 versions.

