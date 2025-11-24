# OSIV (Open Session in View) Anti-Pattern Demo

This demo shows how OSIV and the tracker `/trackedEntities` aggregate pattern impact database
connection pool usage. The demo uses `connection.pool.max_size = 2` to make connection exhaustion
immediately visible - with only 2 connections available, threads must wait for connections, forcing
the N+1 queries to execute more sequentially rather than in parallel. This makes it easier to see
how OSIV keeps connections open for entire request duration, and how the N+1 aggregate pattern
attempts to execute hundreds of queries in parallel.

## OSIV Demo

### Problem

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

### Endpoints

| Endpoint | OSIV | Description |
|----------|------|-------------|
| `/api/debug/osiv/sleep` | Yes | Pure sleep, no DB work. Watch HikariCP logs. |
| `/api/debug/hibernate/sleep` | Yes | Tags the OSIV-held connection, visible in pg_stat_activity |
| `/api/debug/noOsiv/sleep` | **No** | Exempt from OSIV - no connection held |

### Running the Demo

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

### What You'll See

#### With OSIV (`/api/debug/osiv/sleep`)

HikariCP logs show `active` connections increasing during the sleep:

```
Pool stats (total=2, active=1, idle=1, waiting=0)
```

#### Without OSIV (`/api/debug/noOsiv/sleep`)

HikariCP logs show no active connections during the sleep:

```
Pool stats (total=2, active=0, idle=2, waiting=0)
```

#### Pool Exhaustion Test

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

### Key Files

* `OsivSleepController.java` - Pure sleep endpoint (with OSIV)
* `OsivHibernateController.java` - Tags connection via Hibernate session
* `NoOsivController.java` - OSIV-exempt endpoint
* `ExcludableOpenEntityManagerInViewFilter.java` - OSIV filter with URL exclusion
* `CspFilter.java` - First DB call happens here (getCorsWhitelist)
* `dhis.conf` - Config with `connection.pool.max_size = 2`

## /trackedEntities Demo

The tracker API demonstrates severe connection pool pressure caused primarily by **N+1 query
patterns in the aggregate layer**, not OSIV. While OSIV holds one connection idle for the request
duration, the real issue is that each tracked entity triggers separate queries for enrollments and
events, attempting to execute hundreds of queries in parallel and overwhelming the connection pool.

### Connection Scaling with Result Size

The analysis below is based the analysis script `./analyze-trackedentities-request.sh martha` making

```bash
curl -u admin:district \
  'http://localhost:8080/api/tracker/trackedEntities?filter=w75KJ2mc4zz:like:martha&fields=attributes,enrollments,trackedEntity,orgUnit&program=ur1Edk5Oe2n&page=1&pageSize=50&orgUnitMode=ACCESSIBLE'
```

with different `PAGE_SIZE`s to get different amount of TEs:

**Fields requested**: `attributes,enrollments,trackedEntity,orgUnit`

| TEs | Connections | Connections/TEs | SQL Queries | Total Time |
|-----|-------------|-----------------|-------------|------------|
| 5   | 25          | 5.0             | 51          | 490ms      |
| 25  | 105         | 4.2             | 231         | 855ms      |
| 50  | 205         | 4.1             | 456         | 1226ms     |
| 100 | 405         | 4.05            | 956         | 1901ms     |

### Explaining the behavior

The aggregate pattern fetches TE data through a mix of sync and async code each acquiring DB
connections from the DB connection pool:

| Data | Async? | Connections (Scales With) | Controlled By `fields=` | Included in default fields? | Code Location |
|-----------|--------|---------------------------|------------------------|----------------------------|---------------|
| TE IDs | No | 1 (batched) | N/A (runs always) | Yes | `DefaultTrackedEntityService:224` |
| TE core data (uid, created, orgUnit, type, etc.) | Yes | 1 (batched) | `trackedEntity` | Yes | `TrackedEntityAggregate:178` |
| TE attributes | Yes | 1 (batched) | `attributes` | Yes (via `*`) | `TrackedEntityAggregate:184` |
| Program owners | Yes | 1 (batched) | `programOwners` | No | `TrackedEntityAggregate:169` |
| Enrollments | Yes | N (per TE) | `enrollments` | No | `EnrollmentAggregate:68-78` |
| Events | Yes | N×M (per enrollment) | `enrollments.events` | No | `DefaultEnrollmentService:183-192` |

All async queries in the aggregate are kicked off concurrently via `allOf(trackedEntitiesAsync,
attributesAsync, enrollmentsAsync)` at `TrackedEntityAggregate`.

We observe ~4 connections/TE above because the program/TEs we fetch have only 1 EN with no events.
So the NxM does not even show but would make matters worse even quicker.

### Production Impact

With the fields used in this analysis (`attributes,enrollments,trackedEntity,orgUnit`), we observe
**~4 connections per TE**. At the default pool size (80 connections), the system can only handle
**~20 TEs being processed concurrently across all requests** - for example, one request fetching 20
TEs, or four concurrent requests each fetching 5 TEs.

This is the **theoretical maximum throughput** for this specific field/data combination. Real-world
throughput is likely lower because:
* TEs with more enrollments and events require more connections (N×M does not even show in the
example)
* Other requests and system operations like our background jobs also hold connections
* Requests exceeding the pool size experience self-contention, serializing parallel work and
increasing duration

The N+1 aggregate pattern is the core bottleneck. OSIV's contribution (holding 1 idle connection
per request) is minor compared to the N+1 pattern requiring hundreds of connections per request.

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
