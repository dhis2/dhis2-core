# HikariCP Histogram/Percentile Metrics Implementation Plan

## Current State

* Custom `PoolMetrics` implementation exposing only gauges: `jdbc.connections.{active,idle,max,min}`
* No histogram or percentile metrics for connection acquisition time, usage time, or creation time
* Missing critical performance indicators for database connection pool behavior under load

## Goal

Enable histogram/percentile metrics for HikariCP to monitor connection acquisition latency and other timing metrics without knowing specific SLOs (software is deployed on customer hardware with varying characteristics).

## Available Approaches

### Approach 1: Server-Side Histograms Only (publishPercentileHistogram)

Use Prometheus/monitoring system to calculate percentiles from histogram buckets.

#### Configuration

```java
@Bean
public MeterFilter hikariHistogramConfig() {
  return new MeterFilter() {
    @Override
    public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
      if (id.getName().startsWith("hikaricp.connections")) {
        return DistributionStatisticConfig.builder()
            .publishPercentileHistogram(true)
            .build()
            .merge(config);
      }
      return config;
    }
  };
}
```

#### Performance Impact

**Pros:**

* Percentiles aggregatable across multiple dimensions (pool name, instance, datacenter)
* Calculations offloaded to monitoring system
* Lower memory overhead in application (just bucket counters)
* Lock-free counter increments (atomic operations)

**Cons:**

* Default 73 buckets per timer metric → 219 time series for 3 HikariCP timer metrics
* Each bucket is a separate time series in Prometheus
* Network bandwidth: all buckets scraped on every Prometheus scrape (typically 15-60s intervals)
* Storage: each bucket time series stored separately in TSDB

**Memory Overhead (per timer):**

* 73 buckets × 8 bytes (long counter) = 584 bytes per timer
* 3 timers × 584 bytes = ~1.75 KB per datasource
* Negligible for single datasource, but multiplied by number of instances

**Accuracy:**

* Percentiles calculated from bucket boundaries (approximation)
* Default buckets: exponential from 1ms to ~1min
* Accuracy depends on bucket distribution:
  * Good accuracy for values near bucket boundaries
  * Poor accuracy for values between sparse buckets
* Example: p99 might be "between 100ms and 200ms" rather than exact value
* Cannot calculate exact percentiles, only estimate within bucket range

**Network/Storage Impact:**

* High: 73 time series per timer scraped every interval
* Prometheus storage grows linearly with cardinality
* Query performance degrades with high cardinality

---

### Approach 2: Server-Side Histograms with Custom Buckets

Define custom bucket boundaries to reduce cardinality and improve accuracy for expected ranges.

#### Configuration

```java
@Bean
public MeterFilter hikariHistogramConfig() {
  return new MeterFilter() {
    @Override
    public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
      if (id.getName().startsWith("hikaricp.connections")) {
        return DistributionStatisticConfig.builder()
            .publishPercentileHistogram(true)
            .minimumExpectedValue(Duration.ofMillis(1))
            .maximumExpectedValue(Duration.ofSeconds(10))
            .build()
            .merge(config);
      }
      return config;
    }
  };
}
```

#### Performance Impact

**Pros:**

* Reduced bucket count from 73 to ~40-50 buckets (depends on min/max range)
* Better accuracy in relevant range (1ms-10s for DB connections)
* Still aggregatable across dimensions
* Lower cardinality than default

**Cons:**

* Still 40-50 time series per timer
* Values outside min/max range clamped (loss of outlier visibility)
* Network/storage impact still significant
* Requires understanding of typical value ranges

**Memory Overhead (per timer):**

* ~50 buckets × 8 bytes = 400 bytes per timer
* 3 timers × 400 bytes = ~1.2 KB per datasource

**Accuracy:**

* Better than default in specified range
* Buckets concentrated in relevant range (1ms-10s)
* Still approximate (bucket-based)
* Outliers beyond max value all counted in +Inf bucket (lose granularity)
* Cannot detect if connections occasionally take >10s (no visibility)

**Network/Storage Impact:**

* Medium-High: ~50 time series per timer
* Better than default but still substantial

---

### Approach 3: Service Level Objectives (SLO) Buckets Only

Define specific SLO thresholds as buckets instead of full histogram.

#### Configuration

```java
@Bean
public MeterFilter hikariHistogramConfig() {
  return new MeterFilter() {
    @Override
    public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
      if (id.getName().startsWith("hikaricp.connections")) {
        return DistributionStatisticConfig.builder()
            .serviceLevelObjectives(
                Duration.ofMillis(10),
                Duration.ofMillis(50),
                Duration.ofMillis(100),
                Duration.ofMillis(250),
                Duration.ofMillis(500),
                Duration.ofSeconds(1),
                Duration.ofSeconds(5)
            )
            .build()
            .merge(config);
      }
      return config;
    }
  };
}
```

#### Performance Impact

**Pros:**

* Minimal cardinality: only 7 buckets per timer = 21 time series total
* Very low memory overhead
* Fast queries (fewer time series)
* Still aggregatable across dimensions
* Clear alignment with operational thresholds

**Cons:**

* Cannot calculate arbitrary percentiles (only at defined SLO boundaries)
* No visibility between bucket boundaries
* Requires choosing meaningful thresholds without knowing customer deployments

**Memory Overhead (per timer):**

* 7 buckets × 8 bytes = 56 bytes per timer
* 3 timers × 56 bytes = 168 bytes per datasource
* ~10x reduction vs custom buckets, ~35x vs default

**Accuracy:**

* Only provides counts at specific thresholds
* Can answer: "How many acquisitions took <50ms?" but not "What is p99?"
* No interpolation between buckets
* Good for SLO monitoring: "Are 95% of acquisitions <100ms?"
* Poor for exploratory analysis and troubleshooting
* Must predict useful thresholds in advance (impossible without customer data)

**Network/Storage Impact:**

* Low: only 7 time series per timer
* Minimal impact on Prometheus

---

### Approach 4: Client-Side Percentiles

Calculate percentiles in application and export pre-computed values.

#### Configuration

```java
@Bean
public MeterFilter hikariHistogramConfig() {
  return new MeterFilter() {
    @Override
    public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
      if (id.getName().startsWith("hikaricp.connections")) {
        return DistributionStatisticConfig.builder()
            .publishPercentiles(0.5, 0.95, 0.99)
            .build()
            .merge(config);
      }
      return config;
    }
  };
}
```

#### Performance Impact

**Pros:**

* Very low cardinality: only 3 percentile gauges per timer = 9 time series total
* No histogram buckets sent to monitoring system
* Minimal network bandwidth
* Exact percentile values (not approximations from buckets)

**Cons:**

* **NOT aggregatable across dimensions** - percentiles from different instances cannot be combined
* Uses HdrHistogram internally: significant memory overhead per timer per registry
* CPU overhead: percentile calculation on every scrape
* Useless in multi-instance deployments (cannot get fleet-wide p99)
* Micrometer docs explicitly discourage in library code

**Memory Overhead (per timer):**

* HdrHistogram structure: ~32 KB per timer (internal bucketing for accuracy)
* 3 timers × 32 KB = ~96 KB per datasource
* 50-100x higher than server-side histograms
* Memory scales with number of datasources and registry instances

**Accuracy:**

* High accuracy for local instance (HdrHistogram precision)
* Exact percentile values (not bucket approximations)
* **Completely inaccurate for distributed system view**
* Cannot answer: "What is p99 latency across all instances?"
* Can only answer: "What is p99 on this specific instance?"

**Network/Storage Impact:**

* Very low: only 3 time series per timer

---

### Approach 5: Hybrid - SLO Buckets + Summary Statistics

Combine SLO buckets with built-in count/sum/max for basic percentile estimation.

#### Configuration

```java
@Bean
public MeterFilter hikariHistogramConfig() {
  return new MeterFilter() {
    @Override
    public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
      if (id.getName().startsWith("hikaricp.connections")) {
        return DistributionStatisticConfig.builder()
            .serviceLevelObjectives(
                Duration.ofMillis(10),
                Duration.ofMillis(25),
                Duration.ofMillis(50),
                Duration.ofMillis(100),
                Duration.ofMillis(250),
                Duration.ofMillis(500),
                Duration.ofMillis(1000),
                Duration.ofMillis(2500),
                Duration.ofSeconds(5),
                Duration.ofSeconds(10)
            )
            .minimumExpectedValue(Duration.ofMillis(1))
            .maximumExpectedValue(Duration.ofSeconds(30))
            .build()
            .merge(config);
      }
      return config;
    }
  };
}
```

#### Performance Impact

**Pros:**

* Moderate cardinality: 10 buckets per timer = 30 time series
* Aggregatable across dimensions
* Can estimate percentiles with reasonable accuracy from 10 buckets
* Count/sum/max provided automatically by Micrometer timers
* Can calculate average latency from sum/count

**Cons:**

* More buckets than minimal SLO approach
* Still approximate percentiles (bucket-based)
* Requires choosing meaningful bucket boundaries

**Memory Overhead (per timer):**

* 10 buckets × 8 bytes = 80 bytes per timer
* 3 timers × 80 bytes = 240 bytes per datasource

**Accuracy:**

* Moderate: 10 carefully chosen buckets provide decent distribution visibility
* Can estimate p50, p95, p99 with interpolation
* Average latency exactly calculable (sum/count)
* Max latency tracked exactly
* Better than 7-bucket SLO for percentile estimation
* Worse than 73-bucket default for precision

**Network/Storage Impact:**

* Low-Medium: 10 time series per timer
* Acceptable for most deployments

---

### Approach 6: No Histogram Configuration (Use Defaults)

Let Micrometer use default histogram settings without customization.

#### Configuration

No custom MeterFilter - just enable HikariCP's MicrometerMetricsTrackerFactory.

#### Performance Impact

**Pros:**

* Zero configuration overhead
* Works out-of-box with Spring Boot auto-configuration
* Timers still provide count, sum, max even without histogram
* Can calculate average latency (sum/count)

**Cons:**

* No histogram buckets exported to Prometheus
* No percentile calculation possible
* Only summary statistics available (count, sum, max)
* Cannot answer "p99 latency" questions

**Memory Overhead:**

* Minimal: only count, sum, max values tracked
* ~24 bytes per timer (3 values × 8 bytes)
* 3 timers × 24 bytes = 72 bytes per datasource

**Accuracy:**

* Average: exact (sum/count)
* Max: exact (but resets between scrapes)
* Percentiles: not available
* Distribution shape: unknown

**Network/Storage Impact:**

* Very low: only 3 time series per timer (count, sum, max)
* Plus gauges for connections (active, idle, etc.)

---

### Approach 7: PrometheusHistogramMetricsTrackerFactory (Alternative)

Use HikariCP's Prometheus-specific histogram tracker instead of default Micrometer tracker.

#### Configuration

```java
@Bean
public HikariConfigCustomizer hikariPrometheusMetrics(CollectorRegistry collectorRegistry) {
  return config -> config.setMetricsTrackerFactory(
      new PrometheusHistogramMetricsTrackerFactory(collectorRegistry)
  );
}
```

#### Performance Impact

**Pros:**

* Avoids Summary metric locking issues (uses Histogram instead)
* Better performance under high concurrency
* Native Prometheus integration
* Predetermined bucket distribution optimized for connection pool metrics

**Cons:**

* Bypasses Micrometer abstraction (couples to Prometheus)
* Cannot use other monitoring systems (Datadog, New Relic, etc.)
* Still exports full histogram (high cardinality)
* Requires managing separate CollectorRegistry

**Memory Overhead:**

* Similar to Approach 1 (server-side histograms)
* Bucket count determined by PrometheusHistogramMetricsTracker implementation

**Accuracy:**

* Same as Approach 1 (bucket-based approximation)
* Bucket distribution may be better tuned for connection pools

**Network/Storage Impact:**

* High: full histogram exported
* Similar to Approach 1

---

## Comparison Matrix

| Approach | Cardinality | Memory/Timer | Aggregatable | Percentiles | Accuracy | Complexity |
|----------|-------------|--------------|--------------|-------------|----------|------------|
| 1. Server Histogram (default) | High (73) | 584 B | ✅ Yes | Approx | Medium | Low |
| 2. Custom Range Histogram | Med-High (50) | 400 B | ✅ Yes | Approx | Medium-High | Medium |
| 3. SLO Buckets Only | Low (7) | 56 B | ✅ Yes | No | Low | Medium |
| 4. Client Percentiles | Very Low (3) | 32 KB | ❌ No | Exact | Local only | Low |
| 5. Hybrid (10 SLO + stats) | Low-Med (10) | 80 B | ✅ Yes | Approx | Medium | Medium |
| 6. No Histogram (default) | Very Low (3) | 24 B | ✅ Yes | No | Avg only | Very Low |
| 7. Prometheus Native | High (varies) | ~500 B | ✅ Yes | Approx | Medium | High |

## Performance Considerations

### Memory Impact per Instance

Assuming 1 datasource per instance:

* **Approach 1**: ~1.75 KB + 219 time series
* **Approach 2**: ~1.2 KB + 150 time series
* **Approach 3**: ~168 B + 21 time series
* **Approach 4**: ~96 KB + 9 time series (but not useful in production)
* **Approach 5**: ~240 B + 30 time series
* **Approach 6**: ~72 B + 9 time series (no percentiles)

### Prometheus Impact (100 instances)

* **Approach 1**: 21,900 time series for HikariCP alone
* **Approach 2**: 15,000 time series
* **Approach 3**: 2,100 time series
* **Approach 5**: 3,000 time series
* **Approach 6**: 900 time series

### CPU Overhead

* All approaches: negligible overhead for counter increments
* Timer recording: ~50-100ns per operation
* Client percentiles (Approach 4): additional HdrHistogram update overhead
* Query-time cost shifts to Prometheus for server-side histograms

## Accuracy Considerations

### Without Knowing Customer SLOs

* **Problem**: Cannot define meaningful SLO buckets without understanding customer workloads
* **Fast customer deployment**: Connection acquisition might be <5ms at p99
* **Slow customer deployment**: Connection acquisition might be >500ms at p99
* **Approach 1-2** (full histogram): captures full range, allows post-hoc analysis
* **Approach 3, 5** (SLO buckets): risk missing important thresholds

### Multi-Tenant Deployment Reality

* Different customers have vastly different database performance characteristics
* Impossible to define universal SLOs that work for everyone
* Need visibility into distribution shape to understand outliers

## Recommendations

### Option A: Start with No Histogram (Approach 6), Add Later

**When to use:** Conservative approach, unsure about monitoring system capacity.

**Rationale:**

* Enable HikariCP metrics to get count, sum, max immediately
* Monitor cardinality impact of basic metrics first
* Add histogram configuration later based on operational needs
* Can always add histograms in a minor release

**Pros:**

* Safe, minimal impact
* Provides basic visibility (average, max latency)
* Can identify if connection pool is bottleneck without percentiles

**Cons:**

* No percentile visibility initially
* May miss critical performance issues hidden in distribution tail
* Requires second deployment to add histograms

---

### Option B: Hybrid SLO Buckets (Approach 5) - RECOMMENDED

**When to use:** Need percentile visibility without knowing exact SLOs, moderate monitoring capacity.

**Rationale:**

* 10 carefully chosen buckets cover wide range (1ms - 10s)
* Low enough cardinality for most Prometheus deployments
* Enables percentile estimation with acceptable accuracy
* Aggregatable across customer deployments
* Count/sum/max included automatically

**Recommended Buckets:**

```
10ms, 25ms, 50ms, 100ms, 250ms, 500ms, 1s, 2.5s, 5s, 10s
```

**Pros:**

* Balanced performance/accuracy trade-off
* Works across diverse customer environments
* Low monitoring system impact (30 series per datasource)
* Can identify slow outliers vs fast majority

**Cons:**

* Percentiles are approximations
* May miss granularity in specific ranges

---

### Option C: Default Server-Side Histogram (Approach 1)

**When to use:** Large monitoring infrastructure, need maximum visibility, exploring performance issues.

**Rationale:**

* Highest accuracy without client-side percentiles
* No need to predict useful ranges
* Best for troubleshooting unknown performance issues
* Standard Micrometer behavior

**Pros:**

* Maximum visibility into latency distribution
* Works without any configuration
* Can calculate any percentile post-hoc

**Cons:**

* High cardinality (219 series per datasource)
* Significant Prometheus storage/query cost at scale
* May need to reduce later if monitoring system struggles

---

## Implementation Recommendations

### Phase 1: Enable Basic Metrics

1. Ensure HikariCP's `MicrometerMetricsTrackerFactory` is configured (Spring Boot does this)
2. Deploy without histogram configuration (Approach 6)
3. Validate metrics are being collected: `hikaricp_connections_acquire_seconds_count`
4. Monitor for 1-2 weeks to establish baseline

### Phase 2: Add Histogram Configuration

Choose based on monitoring capacity:

* **Conservative**: Approach 5 (Hybrid SLO with 10 buckets)
* **Aggressive**: Approach 1 (Default 73 buckets)

### Phase 3: Tune Based on Observations

* If 73 buckets cause Prometheus issues → reduce to 10-bucket SLO
* If 10 buckets insufficient → add more targeted buckets based on observed ranges
* If customer deployments vary wildly → consider custom buckets per deployment class

### Configuration Flag

Make histogram configuration controllable via dhis.conf:

```properties
# Options: none, slo, full
monitoring.hikaricp.histogram=slo
```

This allows operators to tune based on their monitoring capacity.

## Conclusion

**Recommended Approach:** Start with **Approach 5 (Hybrid SLO)** using 10 buckets covering 10ms-10s range.

**Rationale:**

* Provides percentile visibility without excessive cardinality
* Works across diverse customer hardware profiles
* Low risk to monitoring infrastructure
* Aggregatable across dimensions
* Can be tuned later based on real-world observations

**Alternative:** If monitoring capacity is very limited, start with **Approach 6 (No Histogram)** and add configuration later once impact is understood.

**Avoid:**

* Approach 4 (Client Percentiles): Not aggregatable, useless in distributed deployments
* Approach 7 (Prometheus Native): Couples to specific monitoring system, similar cardinality to Approach 1

---

## Renaming HikariCP Metrics Prefix

### Current State

HikariCP's `MicrometerMetricsTrackerFactory` registers metrics with the `hikaricp.*` prefix, which
becomes `hikaricp_*` in Prometheus format.

### Goal

Change metric prefix from `hikaricp_` to `jdbc_` for consistency with custom
`jdbc.connections.*` gauges.

### Solution

Add a `MeterFilter` bean to rename metrics at registration time:

```java
@Bean
public MeterFilter hikariToJdbcPrefixFilter() {
  return MeterFilter.map(id -> {
    if (id.getName().startsWith("hikaricp.")) {
      return id.withName(id.getName().replace("hikaricp.", "jdbc."));
    }
    return id;
  });
}
```

Add this to `PrometheusMonitoringConfig.java` or appropriate metrics configuration class.

### Performance Impact

**Negligible.** The filter runs only at metric registration time (startup), not on every
collection:

* **Registration overhead**: Single string comparison and replace per metric (~microseconds)
* **Runtime overhead**: Zero - metrics are registered once with the transformed name
* **Memory overhead**: None - no additional data structures
* **Collection overhead**: None - metrics collection uses the registered name directly

MeterFilters are designed for production use and are applied before metrics enter the registry.
