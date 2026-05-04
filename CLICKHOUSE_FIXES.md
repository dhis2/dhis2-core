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
