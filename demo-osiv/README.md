# OSIV (Open Session in View) Anti-Pattern Demo

This demo shows how OSIV keeps database connections open for the entire HTTP request duration, even
when the endpoint does no database work.

## Problem

OSIV binds the Hibernate session (and thus the DB connection) to the HTTP request lifecycle:

1. Connection acquired when first DB call happens (in CspFilter when it calls getCorsWhitelist)
2. Connection held for entire request duration
3. Connection released only when response is sent

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
cd demo-osiv && docker compose up -d db && cd ..
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

## References

* [The Open Session In View Anti-Pattern](https://vladmihalcea.com/the-open-session-in-view-anti-pattern/)
* OSIV configured in: `DhisWebApiWebAppInitializer.java`
* First DB call: `CspFilter.java` (getCorsWhitelist)
