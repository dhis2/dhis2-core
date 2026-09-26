# DHIS2 performance tests (2.42)

Minimal standalone Gatling module for the UserRoles JSON Patch regression
(DHIS2-21852 / PR #24489). Not part of the main Maven reactor.

## Prerequisites

- Java 17
- A running DHIS2 instance with a large-membership user role
  (platform-perf default role uid: `MoRvPzDH7lc`)

## Run UserRolesPerformanceTest

```bash
cd dhis-2/dhis-test-performance
mvn gatling:test \
  -Dgatling.simulationClass=org.hisp.dhis.test.platform.UserRolesPerformanceTest
```

### Useful -D flags

| Flag | Default | Meaning |
|------|---------|---------|
| `baseUrl` | `http://localhost:8080` | Target DHIS2 |
| `username` | `admin` | Basic auth user |
| `password` | `district` | Basic auth password |
| `userRoleUid` | `MoRvPzDH7lc` | Large-membership role on platform-perf |
| `iterations` | `10` | Repeats per scenario |
| `configFile` | _(none)_ | Optional `.properties` file |

Example against a remote instance:

```bash
mvn gatling:test \
  -Dgatling.simulationClass=org.hisp.dhis.test.platform.UserRolesPerformanceTest \
  -DbaseUrl=https://my-instance.example.com \
  -DuserRoleUid=<large-role-uid> \
  -Diterations=10
```

### What it measures

1. **PATCH empty role** — scalar `/description` on a zero-member control role
2. **PATCH large role** — same patch on the large-membership role
3. **GET large role** — narrow-fields read control

Both PATCH scenarios share the same p95/max thresholds (100ms / 150ms). The
invariant is that PATCH latency must not grow with membership size.

### Compare CI runs

```bash
./scripts/compare-gatling-run.sh <github-actions-run-id>
```
