# ClickHouse Analytics — Fixes

A running log of changes made to the analytics SQL generation code to support ClickHouse as an analytics backend, alongside the existing PostgreSQL and Apache Doris paths. Each entry describes one class of failure observed when running the e2e analytics suite against ClickHouse 24.8 and the corresponding code change.

The PostgreSQL and Doris paths are unchanged in observable behaviour: every fix here is either an engine-conditional override or a strictly-equivalent SQL rewrite (e.g. adding an identity column alias) that the other engines tolerate without difference.

---

## ISSUE: Period column names emitted in mixed case

ClickHouse identifiers are case-sensitive. The analytics enrollment/event tables are created with **lowercase** period columns (`monthly`, `weekly`, `yearly`, …) — see `AbstractJdbcTableManager.java:510` (`pt.getName().toLowerCase()`). However, several SQL emission sites projected the **display-cased** form from `PeriodTypeEnum.getName()` (e.g. `"Monthly"`) directly into the SELECT list. Postgres folds unquoted identifiers to lowercase silently and Doris is configured with `lower_case_table_names=1`, so the bug went unnoticed on those engines. ClickHouse preserves case and rejected the query with `Unknown identifier`.

### FIX

Lowercase the period name with `Locale.ROOT` at every site that emits it as a SQL identifier.

- `AbstractJdbcEventAnalyticsManager.java:526` — aggregated-enrollment period dimension select projection.
- `AbstractJdbcEventAnalyticsManager.java:561` and `:570` — single-period and filter-period non-default-boundary aliases (`'2022-Q4' as monthly`).
- `EnrollmentQueryHelper.java:170` — outer query period column reference (`enr_aggr.monthly`).

---
## ISSUE: Program UID emitted in mixed case in analytics table names

DHIS2 UIDs are intentionally mixed-case (e.g. `IpHINAT79UW`). The analytics tables are created with **lowercased** names (`analytics_event_iphinat79uw`), but a long list of emission sites concatenated `"analytics_event_" + program.getUid()` without lowercasing. Postgres folds unquoted identifiers and Doris is case-insensitive, so the mismatch went unnoticed there. ClickHouse rejected the queries with `UNKNOWN_TABLE`.

### FIX

`.toLowerCase()` the UID at every emission site, matching the existing precedent at `AbstractJdbcEventAnalyticsManager.java:3220` (one of the few sites that was already correct).

- `AbstractJdbcEventAnalyticsManager.java` — lines 2449, 2793.
- `JdbcEnrollmentAnalyticsManager.java:937`.
- `EnrollmentEventSubqueryBuilder.java` — lines 75 and 92.
- `EnrollmentTimeFieldSqlRenderer.java:144`.
- `D2FunctionCteFactory.java:199`, `FilterCteFactory.java:137`, `VariableCteFactory.java:118`, `ProgramStageDataElementCteFactory.java:152`.
- `DefaultStatementBuilder.java:141`.
- `ProgramIndicatorQueryBuilder.java:265-266` — same bug class for `analytics_enrollment_<UID>`, fixed for symmetry.

### Suggested follow-up

There is no shared helper for "build the analytics table name from a program/program-stage UID" — every site assembles `"analytics_event_" + uid` inline. A small refactor adding `AnalyticsTableNames.eventTable(Program)` / `enrollmentTable(Program)` and replacing all sites with the helper would make this class of bug structurally impossible.

---
## ISSUE: Correlated scalar subqueries referencing outer columns

ClickHouse's analyzer rejects scalar subqueries that reference non-constant columns from the parent scope:

> `Resolve identifier 'eb.lastupdated' from parent scope only supported for constants and CTE.`

This affected analytics queries that look up the financial-year `financialsep` (and similar) period bucket in `analytics_rs_dateperiodstructure` from inside the SELECT or GROUP BY of the outer query, computing the join key from a column on the enclosing CTE. Postgres supports correlated subqueries; ClickHouse does not.

### FIX

`DateFieldPeriodBucketColumnResolver.java:217` already supports two emission shapes for this lookup, controlled by `sqlBuilder.useJoinForDatePeriodStructureLookup()`:

- **`false` (default)** — emits `(select financialsep from analytics_rs_dateperiodstructure as dps_period where dps_period.dateperiod = …)` inline. Works on Postgres.
- **`true`** — emits a `LEFT JOIN analytics_rs_dateperiodstructure as <alias>` in the FROM clause and replaces the SELECT/GROUP-BY expression with a column reference like `<alias>.financialsep`. Used by Doris.

`DorisAnalyticsSqlBuilder` overrides the flag to `true`. `ClickHouseAnalyticsSqlBuilder` was inheriting the default `false`.

Override `useJoinForDatePeriodStructureLookup()` to return `true` in `ClickHouseAnalyticsSqlBuilder.java`. The JOIN form works on all three engines (it is never worse than the subquery form on Postgres), so no further conditionals are needed.

---
