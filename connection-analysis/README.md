# Connection Analysis - OSIV Experiment

This directory contains tools to analyze database connection usage during performance tests, specifically for comparing OSIV (Open Session in View) behavior.

## Quick Start - Running OSIV ON vs OFF Tests

### 1. Setup Infrastructure

```bash
cd connection-analysis
docker compose up --detach
```

### 2. Build Once

```bash
./build.sh
```

### 3. Run OSIV ON Test (tracker endpoints WITH OSIV)

```bash
# Terminal 1: Start DHIS2 with tracker in OSIV
OSIV_EXCLUDE_TRACKER=false ./start.sh

# Terminal 2: Run the performance test
cd ../dhis-2/dhis-test-performance
mvn gatling:test -Dgatling.simulationClass=org.hisp.dhis.test.tracker.TrackerTest

# After test completes: Save the results
cd ../..
mkdir -p connection-analysis/results/on
cp -r dhis-2/dhis-test-performance/target/gatling/trackertest-* \
  connection-analysis/results/on/

# Extract timestamps and analyze
./connection-analysis/gatling-test-times.sh \
  connection-analysis/results/on/trackertest-*

# Run the command it suggests with results/on as output dir
```

### 4. Run OSIV OFF Test (tracker endpoints WITHOUT OSIV)

```bash
# Terminal 1: Restart DHIS2 without tracker in OSIV (default)
# Stop previous instance (Ctrl+C), then:
./connection-analysis/start.sh

# Terminal 2: Clean and run the performance test
rm -rf dhis-2/dhis-test-performance/target/gatling/*
cd dhis-2/dhis-test-performance
mvn gatling:test -Dgatling.simulationClass=org.hisp.dhis.test.tracker.TrackerTest

# After test completes: Save the results
cd ../..
mkdir -p connection-analysis/results/off
cp -r dhis-2/dhis-test-performance/target/gatling/trackertest-* \
  connection-analysis/results/off/

# Extract timestamps and analyze
./connection-analysis/gatling-test-times.sh \
  connection-analysis/results/off/trackertest-*

# Run the command it suggests with results/off as output dir
```

## Configuration

### OSIV Control

The filter can be toggled at runtime via environment variable in `start.sh`:

* `OSIV_EXCLUDE_TRACKER=true` (default) - Excludes /api/tracker/** from OSIV (OFF)
* `OSIV_EXCLUDE_TRACKER=false` - Keeps /api/tracker/** in OSIV (ON)

### Files

* `dhis.conf` - Database connection config
* `log4j2.xml` - Logging config with connection timing enabled
* `docker-compose.yml` - PostgreSQL and Prometheus setup
* `build.sh` - Build DHIS2 war file
* `start.sh` - Start DHIS2 with custom config
* `gatling-test-times.sh` - Extract test timestamps from Gatling results
* `analyze-connections.sh` - Analyze connection logs

## Output Structure

```
connection-analysis/results/
├── on/                              # WITH OSIV on tracker
│   ├── trackertest-TIMESTAMP/       # Gatling test results
│   │   ├── simulation.log
│   │   ├── simulation.csv
│   │   └── ...
│   ├── connection-raw.csv           # All connection events
│   ├── connection-stats.md          # Summary statistics
│   └── per-request-breakdown.csv    # Per-request analysis
└── off/                             # WITHOUT OSIV on tracker
    ├── trackertest-TIMESTAMP/
    ├── connection-raw.csv
    ├── connection-stats.md
    └── per-request-breakdown.csv
```

## Example Output

```
===== Connection Acquisition Statistics =====

Total connections: 14890
Unique requests: 4723

Wait Time (ms):
  Mean: 1957.87
  P90: 196
  P99: 29992
  Max: 30003

Held Time (ms):
  Mean: 1075.94
  P90: 51
  P99: 35163
  Max: 242429
```

## Key Metrics to Compare

From `connection-stats.md`:

* **Wait Time** - Time waiting for connection from pool (indicates pool exhaustion)
  * P90, P99, Max - Look for high values when OSIV holds connections too long
* **Held Time** - Time connection was held before release
  * P90, P99, Max - Expected to be higher with OSIV ON (connections held during serialization)

From `per-request-breakdown.csv`:

* Connection count per request
* Total wait time per request
* Total held time per request

## Interpreting Results

* **High wait times (P90 > 100ms)**: Connection pool exhaustion, requests blocked waiting for
  available connections
* **High held times (P99 > 10s)**: Connections held during non-DB work, likely OSIV keeping
  connections open during response serialization
* **Expected outcome**: OSIV OFF should show lower P99 held times and lower wait times compared to
  OSIV ON

## Logs

* `logs/dhis.log` - DHIS2 logs with connection timing data
  * Timestamps keep multiple test runs separated
  * No need to clear between runs
