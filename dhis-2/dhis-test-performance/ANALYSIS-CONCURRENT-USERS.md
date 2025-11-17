# Concurrent Users from Nginx Logs

## Overview

This guide shows you how to measure concurrent users from your nginx access logs using
Elasticsearch and Kibana dashboards. Concurrent users (L in Little's Law) tells you how many
users are actively using your system at any given moment, which is essential for:

* Understanding system capacity and resource requirements
* Identifying peak load periods
* Capacity planning and horizontal scaling decisions
* Performance troubleshooting during high-concurrency periods
* Validating load test scenarios match production patterns

The dashboard visualizes concurrent users by counting unique session IDs that made requests
within each time bucket, giving you a real-time view of active user load on your system.

## Method

### Data Pipeline: Nginx → Vector → Elasticsearch

The concurrent users calculation depends on the same nginx log fields used for arrival rate:

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
* Converts timestamps to Elasticsearch-compatible format
* Indexes each request as a document in the `tracker-local` index

### How Concurrent Users Are Calculated

Concurrent users are computed using Elasticsearch's cardinality aggregation:

**[Cardinality Aggregation](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-metrics-cardinality-aggregation.html)**: Count unique sessions per time bucket

* Groups all requests into time buckets (typically 1-second intervals)
* For each bucket, counts the number of unique `sessionid_hash` values
* A session is considered "concurrent" if it made at least one request during that time window
* Uses HyperLogLog++ algorithm for efficient approximate counting

**Example:**

```
Time Bucket | Requests | Unique Session IDs | Concurrent Users
------------|----------|-------------------|------------------
12:00:00    | 45       | 10                | 10
12:00:01    | 38       | 8                 | 8
12:00:02    | 52       | 12                | 12
12:00:03    | 41       | 10                | 10
```

The "Concurrent Users" column shows how many distinct users were active in each second.

### Elasticsearch Query

The dashboard uses this aggregation pattern:

```json
{
  "aggs": {
    "time_buckets": {
      "date_histogram": {
        "field": "@timestamp",
        "fixed_interval": "1s"
      },
      "aggs": {
        "concurrent_sessions": {
          "cardinality": {
            "field": "sessionid_hash.keyword"
          }
        }
      }
    }
  }
}
```

### Key Difference: Concurrent Users vs Arrival Rate

**Arrival Rate (λ):**

* Tracks NEW sessions appearing for the first time
* Uses cumulative_cardinality + derivative
* Answers: "How many users are arriving per second?"

**Concurrent Users (L):**

* Tracks ALL active sessions in each time window
* Uses simple cardinality aggregation
* Answers: "How many users are active right now?"

### Little's Law Connection

These metrics are related through Little's Law:

```
L = λ × W
```

Where:

* L = Concurrent users (this metric)
* λ = Arrival rate (see ANALYSIS-ARRIVAL-RATE.md)
* W = Average time in system (response time + think time)

You can derive any metric from the other two. For example, if you know concurrent users and
arrival rate, you can calculate average session duration.

### Important: Elasticsearch vs Gatling Concurrent Users

There is a fundamental difference between how these systems measure concurrent users:

**Elasticsearch (Request-Based):**

* Counts sessions that made at least one request during the time bucket
* A session must be actively sending requests to be counted
* Sessions in "think time" (user reading, idle) are NOT counted

**Gatling (Lifecycle-Based):**

* Counts all virtual users that have started but not yet ended
* Includes users during think time, waiting for responses, or idle between requests
* Represents total "alive" users regardless of activity

**Expected Difference:**

Elasticsearch will typically report LOWER concurrent user counts than Gatling because:

* Users spend time thinking, reading content, or being idle between requests
* During these periods, they are "alive" in Gatling but not "active" in Elasticsearch
* Only when a user makes a request do they appear in the ES time bucket

This is normal and expected behavior. The two metrics answer different questions:

* **ES**: "How many sessions are generating load right now?"
* **Gatling**: "How many users are in the system right now?"

### Limitations

* **Cookie dependency**: Users must have cookies enabled. Sessionless requests are not counted.
* **Activity-based**: Only counts sessions that made requests during the time window. Idle users
are not included.
* **Session stability**: Session IDs must remain stable throughout a user's session. If a user's
session ID changes (e.g., after logout/login), they will be counted separately.
* **Timestamp accuracy**: Depends on nginx's `$msec` timestamp being accurate. System clock skew
or time synchronization issues will affect results.
* **Approximation**: HyperLogLog++ is an approximate counting algorithm (<3% error for typical
cardinalities).
* **Think time invisibility**: Users between requests (during think time) are not counted, which
is different from total concurrent users in the system.

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

Open Kibana and navigate to the concurrent users dashboard:

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

* **Concurrent Sessions - L (Load)**: Visual representation of active sessions over time
(1-second intervals)
* **Statistics**: Depending on test type:
  * Load tests: Average concurrent users
  * Stress tests: P95 concurrent users
  * Spike tests: Maximum concurrent users

### Step 5: Use in Capacity Planning

Use concurrent user metrics to:

1. **Right-size infrastructure**: Ensure your servers can handle peak concurrent load
2. **Set performance baselines**: Track how response times degrade with concurrent users
3. **Plan scaling triggers**: Set autoscaling thresholds based on concurrent user counts
4. **Validate load tests**: Ensure test concurrent users match production patterns

## Verify Using Gatling

You can validate that the dashboard correctly calculates concurrent users by running a Gatling
test and comparing it with Elasticsearch using the `verify-concurrent-users.sh` script.

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
./verify-concurrent-users.sh
```

The script automatically:

* Detects the latest Gatling test results
* Converts the binary log to CSV (if needed)
* Extracts user start and end times from Gatling simulation.csv
* Calculates concurrent users per second (users started but not yet ended)
* Queries Elasticsearch for concurrent sessions in the same time period
* Shows side-by-side comparison with analysis

### Understanding the Results

**Why Elasticsearch typically shows LOWER counts:**

* Gatling counts ALL users that are "alive" (started but not ended)
* Elasticsearch only counts sessions that made requests during the time bucket
* During "think time" (pauses between requests), Gatling users are alive but not sending
requests
* This means ES won't count them during those idle periods

**Example scenario:**

```
Time: 12:00:00
* Gatling: 10 users started, 2 ended = 8 concurrent users
* ES: Only 5 of those 8 users made requests during this second = 5 concurrent sessions
* The other 3 users are in "think time" (between requests)
```

**This is expected and normal.** The two metrics answer different questions:

* **Gatling**: Total users in the system
* **ES**: Active users generating load

**When to be concerned:**

* **ES shows MORE concurrent sessions than Gatling users**: This should not happen and indicates
a problem (session ID reuse, duplicate counting, etc.)
* **ES shows ZERO when Gatling shows users**: This indicates requests aren't reaching nginx or
aren't being logged properly

**Acceptable differences:**

* ES showing 30-70% of Gatling's count is typical, depending on think time configuration
* Longer think times = larger difference
* Shorter think times = smaller difference

### Troubleshooting

**No data in Elasticsearch:**

Check that:

```bash
# Verify nginx is logging
docker compose logs nginx | grep tracker

# Check Elasticsearch index
curl --insecure --user elastic:elastic123 https://localhost:9200/tracker-local/_count
```

Make sure requests go through nginx (port 8081), not directly to DHIS2 (port 8080).

**Wrong time range:**

Check Gatling start time from directory name:

```bash
ls -la target/gatling/
# trackertest-20251113123038 = 2025-11-13T12:30:38
```

**Large unexpected patterns:**

* Verify Vector is processing logs: `docker compose logs vector`
* Check for dropped logs or errors in Vector
* Ensure sufficient time buffer (±5 seconds) around test period

## Advanced Usage

### Calculating Expected Concurrent Users

Using Little's Law, you can predict concurrent users:

```bash
# If arrival rate = 25 users/sec and average session duration = 120 sec
L = λ × W = 25 × 120 = 3000 concurrent users
```

### Analyzing Production Traffic

For production analysis:

1. Use larger intervals (5m or 10m) to smooth out noise
2. Focus on time periods with known issues or peak traffic
3. Compare concurrent users across different time periods (e.g., weekday vs weekend)
4. Look for patterns: daily peaks, weekly cycles, seasonal trends
5. Correlate with system metrics (CPU, memory) to find capacity limits

### Comparing Test Types

Different load test types focus on different concurrent user patterns:

* **Load test**: Steady concurrent users over time (validate average capacity)
* **Stress test**: Gradually increasing concurrent users (find breaking point)
* **Spike test**: Sudden jump in concurrent users (test elasticity and recovery)
* **Soak test**: Sustained high concurrent users (detect memory leaks, degradation)

## References

* **Little's Law**: L = λ × W
  * L = Concurrent users ← This metric
  * λ = Arrival rate (users/sec)
  * W = Time in system (response + think time)

* **Elasticsearch Cardinality**:
  https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-metrics-cardinality-aggregation.html

* **Related documentation**: ANALYSIS-ARRIVAL-RATE.md
