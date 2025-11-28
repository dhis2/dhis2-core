# Connection Pool Performance Test

This test demonstrates that the `ConditionalOpenEntityManagerInViewFilter` successfully prevents
connection pool exhaustion for excluded endpoints.

## Purpose

Show that endpoints excluded from OSIV (`/api/ping`, `/api/metrics`, `/api/tracker/**`,
`/api/system/ping`) do not hold database connections during request processing, while OSIV-enabled
endpoints continue to hold connections for the entire request duration.

## Setup

### 1. Configure DHIS2

Ensure `./home/dhis.conf` has HikariCP monitoring enabled:

```properties
monitoring.dbpool.enabled = on
monitoring.api.enabled = on
```

### 2. Start DHIS2

```bash
./home/start.sh
```

### 3. Verify Metrics Endpoint

```bash
curl -s http://localhost:8080/api/metrics | grep hikaricp_connections
```

You should see metrics like:
```
hikaricp_connections_active{pool="HikariPool-1"} 0.0
hikaricp_connections_max{pool="HikariPool-1"} 80.0
hikaricp_connections_idle{pool="HikariPool-1"} 2.0
```

## Running the Test

### Test OSIV-Excluded Endpoint (Expected: 0 active connections)

```bash
mvn gatling:test \
  -Dgatling.simulationClass=org.hisp.dhis.test.osiv.ConnectionPoolTest \
  -Dendpoint=/api/ping \
  -DconcurrentUsers=100 \
  -DdurationSec=30 \
  --file dhis-2/dhis-test-performance/pom.xml
```

### Test OSIV-Enabled Endpoint (Expected: ~100 active connections)

```bash
mvn gatling:test \
  -Dgatling.simulationClass=org.hisp.dhis.test.osiv.ConnectionPoolTest \
  -Dendpoint=/api/organisationUnits \
  -DconcurrentUsers=100 \
  -DdurationSec=30 \
  --file dhis-2/dhis-test-performance/pom.xml
```

## Monitoring Connection Pool Usage

### 1. Enable HikariCP Debug Logging

The most reliable way to observe OSIV connection holding is via HikariCP DEBUG logs, which show
real-time pool stats. Prometheus jdbc metrics are often delayed/aggregated and don't accurately
reflect connections checked out from the pool.

Update `./home/log4j2.xml`:

```xml
<Logger name="com.zaxxer.hikari.pool.HikariPool" level="DEBUG"/>
```

### 2. Monitor Logs During Test

Watch the logs while running the test:

```bash
tail -f ./home/logs/dhis.log | grep "HikariPool.*stats"
```

You should see log entries like:

```
HikariDataSource_XXX - Connection not added, stats (total=80/80, idle=0/10, active=80, waiting=25)
```

Key values:
- `active=80` - All connections held by requests (OSIV holding them)
- `idle=0` - No connections available
- `waiting=25` - Threads waiting for connections (pool exhaustion)

### 3. Optional: Prometheus Metrics

**Note:** Prometheus `jdbc_*` metrics may not accurately reflect real-time pool state. Use
HikariCP logs as the source of truth.

If you have Prometheus available:

```bash
# Query jdbc metrics (may be delayed/inaccurate)
curl -s -u admin:district http://localhost:8080/api/metrics | grep jdbc_connections
```

## Expected Results

### For OSIV-Excluded Endpoints (after applying the fix)

Test with `/api/ping`, `/api/metrics`, `/api/tracker/**`:
- All requests return 200 OK
- HikariCP logs show low `active` count (requests don't hold connections)
- No connection pool contention even with 100+ concurrent users

### For OSIV-Enabled Endpoints (baseline/master without fix)

Test with `/api/sleep?ms=5000` or `/api/organisationUnits`:
- Requests may timeout if `concurrentUsers` > pool size (80)
- HikariCP logs show `active=80` (all connections held)
- Logs show `waiting=N` threads blocked on connection acquisition
- Pool exhaustion under load

**Note:** `/api/ping` completes too quickly to demonstrate the issue. Use `/api/sleep?ms=5000` to
make connection holding visible in logs and metrics.

## Demonstrating the Fix

### Before ConditionalOpenEntityManagerInViewFilter

If you test against a build without the filter (or remove `/api/ping` from exclusions):

```bash
# This would exhaust the connection pool if concurrentUsers > pool size
mvn test -Dtest=ConnectionPoolTest \
  -Dendpoint=/api/ping \
  -DconcurrentUsers=100 \
  -DdurationSec=30
```

Expected: Requests timeout/fail when `concurrentUsers` exceeds `connection.pool.max_size`

### After ConditionalOpenEntityManagerInViewFilter

With the filter enabled (current code):

```bash
mvn test -Dtest=ConnectionPoolTest \
  -Dendpoint=/api/ping \
  -DconcurrentUsers=100 \
  -DdurationSec=30
```

Expected: All requests succeed, 0 connections active

## Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `instance` | `http://localhost:8080` | DHIS2 instance URL |
| `endpoint` | `/api/ping` | Endpoint to test |
| `concurrentUsers` | `100` | Number of concurrent virtual users |
| `durationSec` | `30` | Test duration in seconds |
| `adminUser` | `admin` | Admin username |
| `adminPassword` | `district` | Admin password |

## Interpreting Results

### Success Criteria
- 100% of requests return 200 OK
- For OSIV-excluded endpoints: `hikaricp_connections_active` stays near 0
- For OSIV-enabled endpoints: `hikaricp_connections_active` reflects concurrent load

### Gatling Report
After the test completes, view the detailed report:
```bash
open dhis-2/dhis-test-performance/target/gatling/connectionpooltest-*/index.html
```

Key metrics:
- **Response time percentiles** (p95, p99)
- **Requests per second**
- **Success rate**
