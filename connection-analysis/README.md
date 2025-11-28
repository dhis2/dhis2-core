# Connection Analysis for OSIV Performance Testing

Analyze database connection wait and held times during Gatling performance tests to measure the
impact of removing OSIV from tracker endpoints.

## What It Measures

* **wait_ms**: Time waiting to acquire a connection from the pool (indicates pool exhaustion)
* **held_ms**: Time connection was held before release (indicates actual DB work + OSIV overhead)
* **Percentiles**: P90, P99, Max for both metrics

## Quick Start

1. **Run your performance test** (TrackerTest with Gatling)

2. **Copy logs for analysis**:

```bash
# The test writes logs to docker container, copy them out
docker compose cp web:/opt/dhis2/logs/dhis2.log connection-analysis/logs/dhis.log
```

3. **Run analysis script**:

```bash
connection-analysis/analyze-connections.sh \
  "2025-11-28T10:00:00+01:00" \
  "2025-11-28T10:15:00+01:00" \
  "dhis-2/dhis-test-performance/target/gatling/trackertest-*/simulation.log" \
  "connection-analysis/output"
```

Replace timestamps with your test start/end times (check Gatling console output or simulation.log).

4. **View results**:

```bash
cat connection-analysis/output/connection-stats.txt
```

## Output Files

* `connection-stats.txt` - Summary with P90, P99, Max for wait_ms and held_ms
* `connection-raw.csv` - All connection events with timestamps
* `per-request-breakdown.csv` - Aggregated by request ID

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

## Interpreting Results

* **High wait times (P90 > 100ms)**: Connection pool exhaustion, requests blocked waiting for
  available connections
* **High held times (P99 > 10s)**: Connections held during non-DB work, likely OSIV keeping
  connections open during response serialization
* **Goal**: After removing OSIV from tracker, expect lower P99 held times and lower wait times

## Finding Long-Held Connections

To identify specific requests holding connections >10s:

```bash
connection-analysis/analyze-long-held-connections.sh \
  connection-analysis/output/connection-raw.csv
```

## Comparing Branches

Run analysis on both branches:

1. `DHIS2-20512-exclude-with-metrics` (current - with OSIV removed)
2. `DHIS2-20512-exclude-before` (baseline - with OSIV enabled)

Compare the P90, P99, Max metrics to demonstrate improvement.
