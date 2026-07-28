# DHIS2 performance tests (2.42)

Standalone Gatling module (not part of the main Maven reactor).

## UserRoles JSON Patch regression

```bash
cd dhis-2/dhis-test-performance
mvn gatling:test -Dgatling.simulationClass=org.hisp.dhis.test.platform.UserRolesPerformanceTest
```

Optional: `-DbaseUrl=... -DuserRoleUid=<large-role-uid> -Diterations=10`

See `UserRolesPerformanceTest` class javadoc for thresholds and platform-perf defaults.
