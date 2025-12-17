# OSIV Experiment Results - TrackerTest

## Experiment Configuration

* **Test**: TrackerTest (Gatling performance test)
* **Duration**: ~3.5 minutes per test
* **Baseline**: OSIV OFF for /api/tracker/** (OSIV_EXCLUDE_TRACKER=true)
* **Candidate**: OSIV ON for /api/tracker/** (OSIV_EXCLUDE_TRACKER=false)
* **Date**: 2025-12-03

## Results Summary

### Connection Usage

| Metric | Baseline (OSIV OFF) | Candidate (OSIV ON) | Change |
|--------|---------------------|---------------------|---------|
| **Unique Requests** | 25,741 | 25,741 | Same ✓ |
| **Total Connections** | 201,063 | 172,867 | **-28,196 (-14%)** 🎉 |
| **Avg Connections/Request** | 7.81 | 6.72 | **-1.09 (-14%)** 🎉 |

### Wait Time (Pool Exhaustion)

| Metric | Baseline (OSIV OFF) | Candidate (OSIV ON) | Change |
|--------|---------------------|---------------------|---------|
| P50 | 0ms | 0ms | Same ✓ |
| P90 | 0ms | 0ms | Same ✓ |
| P99 | 0ms | 0ms | Same ✓ |
| Max | 159ms | 136ms | **-23ms better** ✓ |

### Held Time (Connection Duration)

| Metric | Baseline (OSIV OFF) | Candidate (OSIV ON) | Change |
|--------|---------------------|---------------------|---------|
| Mean | 19.54ms | 20.38ms | +0.84ms |
| P50 | 5ms | 6ms | +1ms |
| P90 | 35ms | 30ms | -5ms |
| P99 | 292ms | 310ms | +18ms |
| Max | 2,024ms | 880ms | -1,144ms (single outlier) |

### Connection Distribution

| Connections per Request | Baseline (OSIV OFF) | Candidate (OSIV ON) |
|-------------------------|---------------------|---------------------|
| 1 connection | 1,980 requests | **17,820 requests** 🎉 |
| 2 connections | 11,880 requests | 0 requests |
| 3 connections | 3,960 requests | 0 requests |
| 5 connections | 0 requests | 3,960 requests |
| 6 connections | 3,960 requests | 0 requests |
| 17-18 connections | 1,980 requests | 1,980 requests |
| 50-51 connections | 1,980 requests | 1,980 requests |

## Key Findings

### No Significant Performance Difference

The results show **minimal practical difference** between OSIV ON vs OFF for /api/tracker/**
endpoints in this test:

**Connection Usage:**
* OSIV ON uses 14% fewer connections (172k vs 201k)
* This is primarily due to connection reuse within requests

**Held Times (P90/P99):**
* Differences are small (5-18ms) and likely within measurement noise
* P90: 30ms vs 35ms (5ms difference)
* P99: 310ms vs 292ms (18ms difference)

**Max values are single data points:**
* Max held time: 880ms vs 2,024ms
* These are outliers and could vary significantly between test runs
* Not statistically meaningful without multiple runs

### Why OSIV ON Uses Fewer Connections

**Connection Reuse Within Request:**

* **With OSIV OFF**: Each @Transactional method acquires a new connection
  * Multiple connection acquisitions per request
  * More connection pool churn
  * Simple request = 2 connections

* **With OSIV ON**: One connection held for entire request
  * Connection acquired once and reused
  * Less pool churn
  * Simple request = 1 connection

### OSIV Downsides Not Visible in This Test

The classic OSIV problems (long-held connections during response serialization) are **not showing
up** in this test because:

1. **Fast Test Duration**: 3.5 minute test with rapid requests
2. **No Slow Serialization**: TrackerTest responses serialize quickly
3. **Adequate Pool Size**: 80 connections sufficient for this load
4. **No Connection Exhaustion**: P99 wait time = 0ms in both tests

### When OSIV Problems Would Appear

OSIV issues would manifest under:

* **Slow Response Serialization**: Large JSON responses with lazy loading
* **High Concurrency**: Many concurrent requests exceeding pool size
* **Long-Running Requests**: Requests with significant processing after DB queries
* **Small Connection Pool**: Pool exhaustion causing wait times

## Interpretation

**The test shows no clear winner:**

1. **OSIV ON** uses fewer connections (14% reduction) but this may not matter if the pool is
   adequately sized
2. **Held time differences are negligible** (5-18ms at P90/P99) - likely within measurement noise
3. **No connection exhaustion** in either configuration (P99 wait = 0ms)
4. **Single test run** - results could vary with different runs

**This TrackerTest may not represent production conditions where OSIV causes problems:**
* Production has slower response times
* Real users have different concurrency patterns
* Actual JSON serialization may be slower
* Pool sizes may be smaller relative to load

## Recommendations

1. **Multiple test runs needed**: Single runs are not sufficient to draw conclusions given the small
   differences observed
2. **Production monitoring**: Real-world connection pool metrics would be more informative than
   synthetic tests
3. **Consider other factors**: The decision to exclude tracker from OSIV should weigh:
   * Code maintainability (reducing OSIV dependency)
   * Risk mitigation (avoiding connection pool exhaustion scenarios)
   * Production patterns (which may differ significantly from TrackerTest)

**This test alone does not provide strong evidence for or against excluding tracker from OSIV.**

## Test Data Location

* **Baseline**: `connection-analysis/results/baseline/`
* **Candidate**: `connection-analysis/results/candidate/`
* **Raw Logs**: `connection-analysis/logs/dhis.log`
