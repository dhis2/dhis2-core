# OSIV (Open Session in View) Anti-Pattern Demo

This demo shows how OSIV keeps database connections open for the entire
HTTP request duration, even when the endpoint does no database work.

## Problem

OSIV binds the Hibernate session (and thus the DB connection) to the HTTP
request lifecycle. This means:

1. Connection acquired at request start (in CspFilter when it calls the DB)
2. Connection held for entire request duration
3. Connection released only when response is sent

With slow endpoints, this wastes DB connections even when no DB work is happening.

## Setup

1. **Start PostgreSQL** (from repo root):
   ```bash
   docker compose up -d db
   ```

2. **Build DHIS2** (with the DebugController):
   ```bash
   mvn package --file dhis-2/pom.xml --projects dhis-web-server --also-make \
       -DskipTests --activate-profiles embedded --threads 2C
   ```

3. **Start DHIS2** with small pool and HikariCP logging:
   ```bash
   DHIS2_HOME=$(pwd)/demo-osiv JAVA_OPTS="-Dlog4j2.configurationFile=$(pwd)/demo-osiv/log4j2.xml" ./dhis-2/run-api.sh -s
   ```

## Running the Demo

**Terminal 1 - Monitor:**
```bash
./demo-osiv/osiv-demo-monitor.sh
```

**Terminal 2 - Requests:**
```bash
./demo-osiv/osiv-demo-requests.sh
```

## What You'll See

1. **Initial state**: 1-2 idle connections
2. **After SLOW1**: Connection tagged `demo-SLOW1` in `pg_stat_activity`, state=idle
3. **After SLOW2**: Both connections held (pool exhausted)
4. **Third request**: Waits 30s for connection, then fails with:
   ```
   Connection is not available, request timed out after 30000ms
   (total=2, active=2, idle=0, waiting=1)
   ```

## Key Files

- `DebugController.java` - Endpoint that sleeps and tags connections
- `dhis.conf` - Config with `connection.pool.max_size = 2`
- `osiv-demo-monitor.sh` - Watch connections in real-time
- `osiv-demo-requests.sh` - Generate test requests
- `osiv-demo.sh` - All-in-one automated demo

## References

- [The Open Session In View Anti-Pattern](https://vladmihalcea.com/the-open-session-in-view-anti-pattern/)
- OSIV configured in: `DhisWebApiWebAppInitializer.java:126-133`
- First DB call: `CspFilter.java:87` (getCorsWhitelist)
