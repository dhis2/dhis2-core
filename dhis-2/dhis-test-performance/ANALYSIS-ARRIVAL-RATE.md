# User Arrival Rate from Nginx Logs

## Overview

This guide shows you how to extract user arrival rates from your nginx access logs using
Elasticsearch and Kibana dashboards. User arrival rate (λ in Little's Law) tells you how many new
users are arriving at your system per unit of time, which is essential for:

* Understanding actual production load patterns
* Creating realistic load tests that match production traffic
* Capacity planning and scaling decisions
* Performance troubleshooting during peak periods

The dashboard visualizes arrival rates by tracking when unique session IDs first appear in your
nginx logs, giving you a real-time view of how users are arriving at your system.

## Method

### Data Pipeline: Nginx → Vector → Elasticsearch

The arrival rate calculation depends on specific nginx log fields being captured and processed:

**Nginx logs must include:**

* `$cookie_JSESSIONID` - The session ID cookie from each request
* `$time_iso8601` - Response sent timestamp in ISO 8601 format
* `$msec` - Unix timestamp with millisecond precision (when nginx processed the request)
* `$request_time` - Total request processing time in seconds
* Standard fields: method, URI, status, response times, etc.

**Vector processing:**

* Parses nginx access logs
* Extracts the JSESSIONID cookie value as `sessionid_hash` field
* Calculates `request_received_at` = `$msec - $request_time`
  * This gives the actual time when the request arrived (before processing)
  * Critical for accurate arrival rate tracking
* Converts timestamps to Elasticsearch-compatible format
* Indexes each request as a document in the `tracker-local` index

### How Arrival Rate Is Calculated

User arrival rate is computed using a two-step Elasticsearch aggregation:

1. **[Cumulative Cardinality](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-pipeline-cumulative-cardinality-aggregation.html)**: Count cumulative unique sessions over time
   * Tracks when each unique `sessionid_hash` first appears across all buckets
   * Builds a running total of all sessions seen so far
   * **Critical**: A session ID is only counted once, even if it appears in multiple buckets
   * Uses HyperLogLog++ algorithm for efficient approximate counting

2. **[Derivative](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-pipeline-derivative-aggregation.html)**: Calculate rate of change between time buckets
   * Takes the difference between consecutive cumulative counts
   * Result is the number of *new* sessions that started in each bucket
   * Because cumulative cardinality never double-counts, the derivative only shows first appearances
   * This derivative value represents the arrival rate

**Example:**

```
Time Bucket | Active Sessions | Cumulative | New Sessions (Arrival Rate)
------------|-----------------|------------|---------------------------
12:00:00    | 10              | 10         | 10  (first bucket)
12:00:01    | 8               | 15         | 5   (15 - 10)
12:00:02    | 12              | 20         | 5   (20 - 15)
12:00:03    | 10              | 24         | 4   (24 - 20)
```

The "New Sessions" column is your arrival rate (λ).

### Why This Works

* **Session-based tracking**: Each user session has a unique JSESSIONID cookie
* **First appearance = arrival**: When a session ID appears for the first time, a user arrived
* **Time bucketing**: Aggregating by time intervals (1s, 5s, 1m, etc.) shows arrival rate over time

### Limitations

* **Cookie dependency**: Users must have cookies enabled. Sessionless requests are not counted in
the arrival rate.
* **Session stability**: Session IDs must remain stable throughout a user's session. If a user's
session ID changes (e.g., after logout/login), they will be counted as a new arrival.
* **Timestamp accuracy**: Arrival times depend on nginx's `$msec` timestamp being accurate. System
clock skew or time synchronization issues will affect results.
* **Approximation**: HyperLogLog++ is an approximate counting algorithm.

### Elasticsearch Query

The dashboard uses this ES|QL query pattern:

```esql
FROM tracker-local
| WHERE @timestamp >= ?_tstart AND @timestamp <= ?_tend
| EVAL interval_seconds = TO_LONG(COALESCE(?interval_seconds, "1"))
| STATS active = CARDINALITY(`sessionid_hash.keyword`)
  BY time_bucket = BUCKET(@timestamp, interval_seconds SECONDS, ?_tstart, ?_tend)
| STATS cumulative = CUMULATIVE_CARDINALITY(active)
  BY time_bucket
| STATS arrival_rate = DERIVATIVE(cumulative)
  BY time_bucket
```

## Using the Dashboard

### Prerequisites

* Docker Compose with logging profile running
* DHIS2 instance accessible at `http://localhost:8081` (via nginx)
* Nginx logs being captured by Vector and indexed into Elasticsearch

### Step 1: Start the Logging Stack

```bash
docker compose --profile logging up
```

Wait for all services to be healthy, you should see:

* `nginx` - running
* `vector` - running
* `es` - healthy
* `kibana` - healthy

### Step 2: Access the Dashboard

Open Kibana and navigate to the user arrival rate dashboard:

```bash
open http://localhost:5601/app/dashboards
```

Look for the dashboard named "Arrival Rate & Load".

### Step 3: Select Time Range

Use Kibana's time picker (top right) to select your analysis period:

* **For load tests**: Set to exact test duration
* **For production analysis**: Set to peak hours or relevant time period

### Step 4: Read the Metrics

The dashboard shows:

* **User Arrival Rate - λ (Arrival)**: Visual representation of new users/sec over time (1-second intervals)
* **Total Unique Sessions**: Total distinct sessions in the selected period

### Step 5: Use in Load Tests

Use the arrival rate in your Gatling test. If the average is 25 users/sec:

```java
setUp(
  scn.injectOpen(
    constantUsersPerSec(25).during(testDuration)
  )
)
```

To replicate the exact arrival pattern from the dashboard, see
[Gatling injection profiles](https://docs.gatling.io/reference/current/core/injection/) for
ramps, spikes, and custom rate patterns.

## Verify Using Gatling

You can validate that the dashboard correctly calculates arrival rate by running a Gatling test and
comparing it with Elasticsearch using the `verify-arrival-rates.sh` script.

### Run Test and Compare

1. Ensure the [logging stack is running](#step-1-start-the-logging-stack)

2. Run a Gatling test:

```bash
mvn gatling:test \
  -Dgatling.simulationClass=org.hisp.dhis.test.tracker.TrackerTest \
  -Dinstance=http://localhost:8081 \
  -Dusers=4 \
  -DrampDuration=2 \
  -Dduration=60
```

3. Run the verification script:

```bash
./verify-arrival-rates.sh
```

The script automatically:

* Detects the latest Gatling test results
* Converts the binary log to CSV (if needed)
* Extracts user start times from Gatling
* Queries Elasticsearch for arrival rates in the same time period
* Shows side-by-side comparison with accuracy report

### Understanding the Results

**Why they match:**

* Each Gatling virtual user creates HTTP requests with a JSESSIONID cookie
* When that session ID first appears in nginx logs, it's counted as a new session arrival
* This perfectly matches Gatling's user injection timing

**Why there might be small differences:**

1. **Timing precision**: Requests might cross second boundaries slightly differently
2. **Network latency**: Small delays between Gatling → nginx → DHIS2
3. **First request**: The very first request might not have a session cookie yet
4. **HyperLogLog approximation**: Cardinality uses approximate counting (<3% error)

Differences of 1-2 sessions out of hundreds are expected and acceptable.

### Troubleshooting

**No data in Elasticsearch:**

Check that:

```bash
# Verify nginx is logging
docker compose logs nginx | grep tracker

# Check Elasticsearch index
curl --insecure --user elastic:changeme https://localhost:9200/tracker-local/_count
```

Make sure requests go through nginx (port 8081), not directly to DHIS2 (port 8080).

**Wrong time range:**

Check Gatling start time from directory name:

```bash
ls -la target/gatling/
# trackertest-20251113123038 = 2025-11-13T12:30:38
```

**Large differences (>10%):**

* Verify Vector is processing logs: `docker compose logs vector`
* Check for dropped logs or errors in Vector
* Ensure sufficient time buffer (±5 seconds) around test period

## Advanced Usage

### Custom Time Range and Interval

You can manually specify parameters when running the comparison script:

```bash
./compare-arrival-rates.sh target/gatling/trackertest-TIMESTAMP 5s
```

This uses 5-second buckets instead of the default 1-second.

### Analyzing Production Traffic

For production analysis:

1. Use larger intervals (5m or 10m) to smooth out noise
2. Focus on time periods with known issues or peak traffic
3. Compare arrival rates across different time periods (e.g., weekday vs weekend)
4. Look for patterns: daily peaks, weekly cycles, seasonal trends

### Export Data for Further Analysis

The verification script saves full Elasticsearch results to `/tmp/es-arrival-rate-query.json`. You
can process this with `jq` or other tools:

```bash
# Extract just the arrival rates
cat /tmp/es-arrival-rate-query.json | jq '.aggregations.buckets.buckets[] |
  select(.new_sessions) |
  {time: .key_as_string, arrival_rate: .new_sessions.value}'
```

## References

* **Little's Law**: L = λ × W
  * L = Concurrent users
  * λ = Arrival rate (users/sec) ← This metric
  * W = Time in system (response + think time)

* **Gatling injection profiles**:
  https://docs.gatling.io/reference/current/core/injection/
