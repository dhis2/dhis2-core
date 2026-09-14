# E2E SQL seed scripts

Every `*.sql` file in this directory is applied to the runtime database of the instance under test at
the start of an analytics e2e run, immediately before the analytics tables are exported. Drop a file
here and it is picked up.

Applied by `org.hisp.dhis.test.e2e.db.SqlSeeder`, called from
`org.hisp.dhis.helpers.extensions.AnalyticsSetupExtension`. Only the analytics suite runs that
extension, so these scripts do not affect any other e2e test.

## Contract

- Files are applied in file-name order. Prefix them so the order is explicit: `010-`, `020-`.
- Each file is sent as a single multi-statement query. PostgreSQL wraps that in one implicit
  transaction, so a file that contains no transaction-control statements is applied atomically: if
  any statement fails, none of that file's changes remain.
- A file that issues its own `begin`/`commit`/`rollback` controls
  its own transaction boundaries, and anything committed before a later failure stays committed.
- Earlier files are **not** rolled back when a later one fails. The run stops at the first bad file,
  leaving the database half-seeded for the rest of the run.
- A failing script aborts the run: the first analytics test class fails in `beforeAll` with the
  script name and PostgreSQL's error.
- Scripts run against the **migrated** schema, not the schema of the Sierra Leone 2.39.6 dump. Write
  them against the current data model.
- Resolve internal ids by UID. Never hardcode them, they differ between dumps:

  ```sql
  insert into ...
  select ou.organisationunitid, de.dataelementid
  from organisationunit ou, dataelement de
  where ou.uid = 'ImspTQPwCqd' and de.uid = 'fbfJHSPpUQD';
  ```

- When at least one script is applied, `/api/maintenance/cacheClear` is called before the export, so
  metadata the instance cached at startup is re-read. Anything cached outside
  `clearApplicationCaches()` will still be stale.
- Seeded data reaches the analytics tables because the export runs afterwards. This holds for the
  Postgres, Doris and ClickHouse jobs alike — all three seed the same runtime database.
- It does **not** hold when you pass `-Danalytics.run.export=false`. The scripts are still applied,
  but analytics results come from whatever the dump already contained.

## What SQL you can use

The scripts go through JDBC, not `psql`. That means no `\copy` or other meta-commands, no
`COPY … FROM stdin`, and nothing that refuses to run inside a transaction, such as
`CREATE INDEX CONCURRENTLY` or `VACUUM`.

A single script may run for at most `db.seed.timeout.seconds` (default 1800). Past that it is
cancelled and the run fails, so a script blocked on a lock cannot stall the job indefinitely.

## Turning seeding off

`db.seed.enabled=false` skips every script without deleting it:

```sh
mvn -Panalytics test -Ddb.seed.enabled=false ...
```

Nothing is read and no database connection is attempted, so this also works when the database is
unreachable. Use it to check whether a failing test is caused by a seed script, or to run the suite
against an instance you have no database access to while scripts are committed.

## Connecting to the right database

`db.url` defaults to `jdbc:postgresql://db/dhis`, the `db` service of the compose stacks. That is the
runtime database for the Doris and ClickHouse runs too — those backends only change where analytics
is exported to. For a run against a locally started instance, pass your own:

```sh
mvn -Panalytics test -Dinstance.url=http://localhost:8080/api \
  -Duser.admin.username=admin -Duser.admin.password=district \
  -Ddb.url=jdbc:postgresql://localhost:5432/e2e -Ddb.username=dhis -Ddb.password=dhis
```

If no scripts are present, no connection is attempted and these properties are ignored — which is
what lets the suite keep running against instances whose database you cannot reach.

## Warning

The `*AutoTest` classes assert exact numbers. A script that adds data to metadata those tests
already query will change their expected values and break them. Prefer introducing your own metadata
over extending Sierra Leone's.
