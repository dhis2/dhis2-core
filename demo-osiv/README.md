# OSIV (Open Session in View) Anti-Pattern Demo

This demo shows how OSIV keeps database connections open for the entire HTTP request duration, even
when the endpoint does no database work.

## Problem

OSIV binds the Hibernate session (and thus the DB connection) to the HTTP request lifecycle:

```
HTTP Request: GET /api/debug/osiv/sleep?sleepMs=30000
                │
                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  OSIV Filter (wraps entire request)                                      │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │                                                                    │  │
│  │  ┌─────────────┐    ┌──────────────┐    ┌───────────────────────┐  │  │
│  │  │ CspFilter   │───▶│@Transactional│───▶│ HikariCP              │  │  │
│  │  │             │    │isCorsWhite-  │    │ borrows connection    │  │  │
│  │  │             │    │listed()      │    │ from pool             │  │  │
│  │  └─────────────┘    └──────────────┘    └───────────────────────┘  │  │
│  │         │                  │                       │               │  │
│  │         │                  ▼                       │               │  │
│  │         │           Transaction COMMITS            │               │  │
│  │         │           (but connection NOT returned)  │               │  │
│  │         │                                          │               │  │
│  │         ▼                                          │               │  │
│  │  ┌───────────────────────────────────────────┐     │               │  │
│  │  │        OsivSleepController                │     │               │  │
│  │  │        Thread.sleep(30000)                │◀────┘               │  │
│  │  │        (NO database work!)                │   Connection held   │  │
│  │  │                                           │   but idle          │  │
│  │  └───────────────────────────────────────────┘                     │  │
│  │                                                                    │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  Finally: OSIV closes Session → Connection returned to pool              │
└──────────────────────────────────────────────────────────────────────────┘
                │
                ▼
         HTTP Response
```

With slow endpoints, this wastes DB connections even when no DB work is happening.

## Endpoints

| Endpoint | OSIV | Description |
|----------|------|-------------|
| `/api/debug/osiv/sleep` | Yes | Pure sleep, no DB work. Watch HikariCP logs. |
| `/api/debug/hibernate/sleep` | Yes | Tags the OSIV-held connection, visible in pg_stat_activity |
| `/api/debug/noOsiv/sleep` | **No** | Exempt from OSIV - no connection held |

## Running the Demo

**Terminal 1 - Start PostgreSQL and build:**

```sh
cd demo-osiv && docker compose up --detach db && cd ..
./demo-osiv/build.sh
./demo-osiv/start.sh
```

**Terminal 2 - Monitor:**

```sh
./demo-osiv/monitor.sh
```

**Terminal 3 (optional) - PostgreSQL query logs:**

```sh
./demo-osiv/pg-logs.sh
```

**Terminal 3 - Generate requests:**

```sh
# With OSIV (holds connection)
curl -u admin:district "http://localhost:8080/api/debug/osiv/sleep?sleepMs=30000"

# Without OSIV (no connection held)
curl -u admin:district "http://localhost:8080/api/debug/noOsiv/sleep?sleepMs=30000"

# With tagged connection (visible in pg_stat_activity)
curl -u admin:district "http://localhost:8080/api/debug/hibernate/sleep?sleepMs=30000&requestId=SLOW1"
```

## What You'll See

### With OSIV (`/api/debug/osiv/sleep`)

HikariCP logs show `active` connections increasing during the sleep:

```
Pool stats (total=2, active=1, idle=1, waiting=0)
```

### Without OSIV (`/api/debug/noOsiv/sleep`)

HikariCP logs show no active connections during the sleep:

```
Pool stats (total=2, active=0, idle=2, waiting=0)
```

### Pool Exhaustion Test

Start 2 slow requests with a pool size of 2:

```sh
curl -u admin:district "http://localhost:8080/api/debug/hibernate/sleep?sleepMs=60000&requestId=SLOW1" &
curl -u admin:district "http://localhost:8080/api/debug/hibernate/sleep?sleepMs=60000&requestId=SLOW2" &
```

Then try a third request:

```sh
curl -u admin:district "http://localhost:8080/api/me"
```

It will wait 30s for a connection, then fail with:

```
Connection is not available, request timed out after 30000ms
(total=2, active=2, idle=0, waiting=1)
```

## Key Files

* `OsivSleepController.java` - Pure sleep endpoint (with OSIV)
* `OsivHibernateController.java` - Tags connection via Hibernate session
* `NoOsivController.java` - OSIV-exempt endpoint
* `ExcludableOpenEntityManagerInViewFilter.java` - OSIV filter with URL exclusion
* `CspFilter.java` - First DB call happens here (getCorsWhitelist)
* `dhis.conf` - Config with `connection.pool.max_size = 2`
* `start.sh` - Start script with HikariCP 5s housekeeping

## Tracker /trackedEntities

The tracker API demonstrates severe connection pool pressure caused primarily by **N+1 query
patterns in the aggregate layer**, not OSIV. While OSIV holds one connection idle for the request
duration, the real issue is that each tracked entity triggers separate queries for enrollments and
events, attempting to execute hundreds of queries in parallel and overwhelming the connection pool.

### Connection Scaling with Result Size

Using the analysis script `./analyze-request.sh`:

| PAGE_SIZE | Tracked Entities | Connections | SQL Queries | Total Time | Conn/TE Ratio |
|-----------|------------------|-------------|-------------|------------|---------------|
| 5         | 5                | 25          | 51          | 490ms      | 5.0           |
| 25        | 25               | 105         | 231         | 855ms      | 4.2           |
| 50        | 50               | 205         | 456         | 1226ms     | 4.1           |
| 100       | 100              | 405         | 956         | 1901ms     | 4.05          |

Approximately **4 database connections per tracked entity**. At default pool size (80 connections),
the pool can only handle **~20 TEs being processed concurrently** across all requests. This could be
1 request with 20 TEs, 4 requests with 5 TEs each, or 20 requests with 1 TE each.

This obviously oversimplifies that there are always other things happening like other requests,
jobs, ... that also hold onto DB connections.

**This assumes requests complete quickly (under 1 second).** As request duration increases,
connections are held longer, and new requests pile up waiting for available connections.

A single request needing more connections than the pool size will experience self-contention. For
example, a request for 50 TEs needs ~200 connections but only 80 exist. The request can only
process ~20 TEs in parallel at a time, with remaining work waiting for connections to be released
by earlier stages. This serializes what was designed as parallel processing, increasing request
duration while blocking other incoming requests.

### Query Chain Per Tracked Entity

The aggregate pattern fetches related data for each tracked entity through a series of queries.

**Phase 1: Batched queries** (done once for all TEs):

1. Fetch all tracked entities by ID (`TrackedEntityAggregate` - 1 query total)
2. Fetch all attributes for those TEs (`TrackedEntityAggregate` - 1 query total)

**Phase 2: Per-entity queries** (scales with result size):

3. ❌ Fetch enrollments **for each TE** (`EnrollmentAggregate` - N queries)
4. ❌ Fetch events **for each enrollment** (`DefaultEnrollmentService` - N×M queries)

With typical TB program data where each TE has 1-2 enrollments, and each enrollment has events,
this produces approximately 4 connection acquisitions per tracked entity:

* 1 for TE data (amortized across all TEs)
* 1 for attributes (amortized across all TEs)
* 1 per TE for enrollments
* 1-2 per TE for events (one per enrollment)

### Root Cause: N+1 Query Pattern in Aggregate Layer

The connection explosion happens in phases 2 above - the aggregate pattern issues **one query per
tracked entity** for enrollments, and **one query per enrollment** for events, instead of batching
these queries like it does for TEs and attributes.

**N+1 Enrollment Fetching** (`EnrollmentAggregate.java:68-78`):

```java
ids.forEach(id -> {
    EnrollmentOperationParams params = EnrollmentOperationParams.builder()
        .trackedEntity(UID.of(id.uid()))
        .build();
    result.putAll(id.uid(), enrollmentService.findEnrollments(params));  // ← One query per TE!
});
```

**N+1 Event Fetching** (`DefaultEnrollmentService.java:183-192`):

```java
private Set<TrackerEvent> getEvents(Enrollment enrollment, ...) {
    TrackerEventOperationParams eventOperationParams =
        TrackerEventOperationParams.builderForProgram(UID.of(program))
            .enrollments(Set.of(UID.of(enrollment)))  // ← Single enrollment!
            .build();
    return Set.copyOf(trackerEventService.findEvents(eventOperationParams));  // ← One query per enrollment!
}
```

These queries are designed to run in parallel using async operations, but with limited connection
pool size, they compete for connections which can force more sequential execution.

### Why This Demo Uses Pool Size = 2

This demo intentionally sets `connection.pool.max_size = 2` to make the problem immediately
visible. With only 2 connections available, threads must wait for connections, forcing the N+1
queries to execute more sequentially rather than in parallel.

In production with the default 80 connections, the same N+1 queries **attempt to execute in
parallel**, creating massive concurrent connection usage. A single request for 100 TEs would try to
acquire **405 connections concurrently** if the pool were large enough. With the standard
80-connection pool, this means:

* **A single request for 100 TEs** needs 405 connection acquisitions
* Multiple concurrent requests immediately exhaust the 80-connection pool
* Threads block waiting for available connections (30s default timeout)
* Performance degrades dramatically under load as threads compete for connections

The N+1 aggregate pattern is the core issue - it would require an impractically large connection
pool to handle even moderate concurrent traffic. OSIV's contribution (holding 1 connection idle) is
minor compared to the 404 other connections needed per request.

### OSIV's Minor Role

While OSIV does hold the main HTTP thread's connection for the entire request duration, this is a
minor issue compared to the N+1 aggregate pattern:

* Request for 100 TEs takes 1,901ms
* OSIV: Main thread holds **1 connection** idle for 1,901ms
* N+1 Pattern: **404 additional connections** acquired/released by worker threads
* Total: 405 connections acquired - OSIV accounts for only 0.25% of the problem

The real bottleneck is the aggregate pattern attempting to fetch enrollments and events with
hundreds of parallel queries. Fixing OSIV alone would barely impact this endpoint's performance.

### Testing

```sh
# Test with 1 TE (uses 5 connections)
./analyze-request.sh grace

# Test with 218+ TEs, default 50 returned (uses 205 connections)
./analyze-request.sh martha

# Test with more results
PAGE_SIZE=100 ./analyze-request.sh martha  # Uses 405 connections!
```

The script shows:

* Request timing (curl total time)
* Connection acquisitions with wait/held times per thread
* SQL query count and timings from PostgreSQL logs

### Key Files

* `analyze-request.sh` - Analyzes a single tracker request with connection/query metrics
* `EnrollmentAggregate.java:68-78` - N+1 enrollment fetching
* `DefaultEnrollmentService.java:183-192` - N+1 event fetching per enrollment
* `TrackedEntitiesExportController.java:155-180` - Entry point for tracker export

## Why Do We Use OSIV?

Despite being an anti-pattern, DHIS2 deliberately uses OSIV for several architectural reasons:

### 1. Legacy Architecture with Complex Domain Models

DHIS2 has a large, mature codebase with deeply nested Hibernate entities containing many
lazy-loaded relationships. Without OSIV, accessing these relationships outside service
transactions causes `LazyInitializationException`.

Git history shows multiple fixes for lazy initialization issues (2021-2023), requiring workarounds
like `HibernateProxyUtils.unproxy()` in cached entities. This demonstrates the pervasive dependency
on lazy loading throughout the codebase.

### 2. REST API Serialization Requirements

The REST API directly serializes Hibernate entities to JSON. Jackson needs to traverse object
graphs (relationships, collections) **after** service methods return. OSIV keeps the session open so
lazy relationships can be loaded during JSON serialization.

Configured in `DhisWebApiWebAppInitializer.java` to apply to all URLs (`/*`):

```java
FilterRegistration.Dynamic openSessionInViewFilter =
    context.addFilter("openSessionInViewFilter", ExcludableOpenEntityManagerInViewFilter.class);
openSessionInViewFilter.addMappingForUrlPatterns(
    EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC), false, "/*");
```

### 3. Async Response Streaming Support

Recent changes (May 2025) explicitly added `DispatcherType.ASYNC` to support streaming large
result sets. The OSIV filter keeps connections open for async responses, closing them only when the
streamed response completes.

This is critical for endpoints that stream data back to clients progressively.

### 4. Filter Chain Dependencies

The first database access happens in `CspFilter.getCorsWhitelist()` **before** controllers execute.
OSIV ensures this connection stays open for the entire request, avoiding multiple connection
acquisitions throughout the request lifecycle.

### 5. The Tracker Aggregate Pattern

The tracker export API uses an "aggregate" pattern that progressively fetches related data:

* `EnrollmentAggregate.java:68-78` - Issues one query per tracked entity for enrollments
* `DefaultEnrollmentService.java:183-192` - Issues one query per enrollment for events

This N+1 pattern relies on OSIV to execute all queries within the same HTTP request context.

### Known Trade-offs

The team is fully aware OSIV is problematic:

* **Connection Pool Exhaustion**: Connections held for entire request duration cause timeouts under
  load
* **Scalability Issues**: Pool exhaustion even with reasonable pool sizes (80 connections)
* **Hidden Database Operations**: Lazy loading triggers unexpected queries in view/serialization
  layer

### Why It Hasn't Been Removed

Removing OSIV would require a massive refactoring effort:

* Introduce DTO layer instead of exposing entities directly
* Add explicit fetch strategies (JOIN FETCH, EntityGraphs) throughout codebase
* Ensure all data loaded within service transaction boundaries
* Fix hundreds of potential `LazyInitializationException` issues

### Migration Strategy

DHIS2 has implemented `ExcludableOpenEntityManagerInViewFilter` allowing selective opt-out via URL
patterns. This provides a path for **incremental migration** endpoint-by-endpoint rather than a
risky big-bang rewrite.

Endpoints can be excluded from OSIV by adding them to the `excludePatterns` configuration (e.g.,
`/api/debug/noOsiv/*`).

## References

* [The Open Session In View Anti-Pattern](https://vladmihalcea.com/the-open-session-in-view-anti-pattern/)
* OSIV configured in: `DhisWebApiWebAppInitializer.java`
* First DB call: `CspFilter.java` (getCorsWhitelist)
