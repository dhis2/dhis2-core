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
## ISSUE: CTE columns retain the original table prefix in ClickHouse's scope

The new ClickHouse analyzer (default since 24.x) does **not** re-scope projected columns to the CTE alias when the SELECT list uses a table-prefixed reference without an explicit output alias. So a CTE projecting `ax.enrollment` produces a column that the analyzer still binds to `ax`, not to the CTE name. The outer query's `eb.enrollment` (where `eb` is the CTE alias) becomes unresolvable, and the error even includes ClickHouse's hint: `Maybe you meant: ['ax.enrollment']`.

Postgres and Doris implicitly drop the table prefix and re-scope under the CTE alias.

### FIX

Two emission sites in the enrollment-aggregate CTE chain were affected — both in `JdbcEnrollmentAnalyticsManager.java`:

- **CTE projection (`enrollment_aggr_base`)**, line 740. Add an explicit identity-alias on the prefixed column. The alias re-binds the column under the CTE name in ClickHouse, and is a no-op for Postgres/Doris (which already discarded the prefix).

  ```diff
  - sb.addColumn(ENROLLMENT_COL, "ax");                   // ax.enrollment
  + sb.addColumn(ENROLLMENT_COL, "ax", ENROLLMENT_COL);   // ax.enrollment as enrollment
  ```

- **Inline derived table (`evf` event-date subquery)**, line 942. Same shape — `select ev.enrollment, …` exposes the column under `ev` rather than `evf` in ClickHouse. Added the identity alias inside the SQL text block.

  ```diff
  -     ev.enrollment,
  +     ev.enrollment as enrollment,
  ```

The bare-identifier alternative (`select enrollment, …`) was rejected because the CTE has an `INNER JOIN` whose right side also has an `enrollment` column — the prefix-with-alias form is the portable one.

---
## ISSUE: `CAST(x AS Date)` throws on `NULL` in ClickHouse

When a data-element column is used as an analytics time field and the column allows NULL, the join condition

```sql
left join analytics_rs_dateperiodstructure as ps on cast(ax."<dataElementUid>" as date) = ps."dateperiod"
```

fails in ClickHouse with `CANNOT_INSERT_NULL_IN_ORDINARY_COLUMN`: `toDate(NULL)` and `CAST(NULL AS Date)` are not NULL-tolerant; they expect a non-Nullable result. Postgres and Doris return `NULL` for `CAST(NULL AS DATE)`.

### FIX

A new default method on the `AnalyticsSqlBuilder` interface:

```java
default String castAsDate(String expression) {
  return "cast(" + expression + " as date)";
}
```

Postgres and Doris keep the default behaviour. `ClickHouseAnalyticsSqlBuilder` overrides:

```java
@Override
public String castAsDate(String expression) {
  // toDateOrNull accepts only String input (not Date / DateTime / DateTime64),
  // so wrap with toString to make the cast type-agnostic and NULL-safe.
  return "toDateOrNull(toString(" + expression + "))";
}
```

`toDateOrNull` is ClickHouse's NULL-tolerant date parser, but unlike `toDate` (which accepts multiple input types and throws on NULL) it only accepts `String`. A date-typed data element stored as `DateTime64(3)` triggers `Illegal type DateTime64(3) of first argument of function toDateOrNull`. Wrapping with `toString` first normalises every input type (`Date`, `DateTime`, `DateTime64`, `String`, and their `Nullable` variants) into a string that `toDateOrNull` can parse, while still propagating `NULL` through both calls.

The inline `cast(... as date)` in `JdbcEventAnalyticsManager.java:455-462` (the `params.hasTimeField()` LEFT JOIN to the date-period-structure table) was replaced with `sqlBuilder.castAsDate(joinCol)`. Output for Postgres/Doris is byte-identical to before.

The private `castToDate` helper inside `ClickHouseAnalyticsSqlBuilder` (still emitting `toDate(...)`) was deliberately left unchanged: it is only called from period-bucket rendering paths whose input columns are real `TimeField` date columns (`enrollmentdate`, `occurreddate`, `lastupdated`) which are non-null in the analytics tables. Broadening it would mask bugs and unnecessarily widen the output type.

---
## ISSUE: Result-set column lookup uses display-cased period dimension name

After the SQL emission for period columns was lowercased (issue #1), the analytics aggregate path failed at row-building time with `org.springframework.jdbc.InvalidResultSetAccessException: Invalid column name`. The stack pointed at `AggregatedRowBuilder.addDimensionData` calling `rowSet.getString(dimension.getDimensionName())` with the display-cased name (e.g. `"Quarterly"`). The result-set column produced by the SQL is `quarterly` (lowercase), so the lookup misses on ClickHouse.

`PeriodDimensionSplitter.splitPeriodDimension` only lowercases the synthetic dimension name when the input is `null`, blank, or equal to `PERIOD_DIM_ID` (`"pe"`). When the input dimension already carries a non-trivial mixed-case name from upstream, that name is preserved verbatim, and the row builder forwards it to the JDBC layer.

The Postgres and MySQL/Doris JDBC drivers normalise column-name lookups case-insensitively, so the mismatch was invisible. ClickHouse's JDBC driver is strictly case-sensitive — `getString("Quarterly")` against a result set with `quarterly` raises `InvalidResultSetAccessException`.

### FIX (part 1)

`AggregatedRowBuilder.addDimensionData` lowercases the dimension name with `Locale.ROOT` before the result-set lookup, matching the convention used everywhere SQL is emitted.

```diff
  for (DimensionalObject dimension : PeriodDimensionSplitter.expandPeriodDimensions(...)) {
-   String dimensionValue =
-       extractStringValue(dimension.getDimensionName(), dimension.getValueType());
+   String dimensionValue =
+       extractStringValue(
+           dimension.getDimensionName().toLowerCase(Locale.ROOT), dimension.getValueType());
    row.add(dimensionValue);
  }
```

The fix is scoped to dimension lookups only. The neighbouring call to `extractStringValue` from `addQueryItemValue` (which receives UID-cased aliases such as `"Zj7UnCAulEk.oZg33kd9taw"`) is left unchanged — those names must remain case-significant.

### FIX (part 2)

The lowercase-only fix was insufficient. The deeper issue surfaced on event-aggregate queries projecting org-unit dimensions: the SELECT list emitted bare `ax."uidlevel2"` (table-prefixed, no alias). ClickHouse JDBC then reports the column with the table prefix attached (mirroring the new analyzer's column-scoping behaviour), so `rowSet.getString("uidlevel2")` cannot resolve `ax.uidlevel2`. Postgres / MySQL JDBC drivers strip the prefix in result-set metadata and the bug stayed invisible.

The fix is to add an explicit `as <col>` alias to dimension projections in the SELECT context, so the result-set column has a canonical name regardless of how the engine reports the underlying expression:

- `OrgUnitField.ouQuote` previously aliased only the special `"ou"` column. Generalised to always emit `as <col>` when the caller asked for an alias (`noColumnAlias == false`):

  ```diff
  - return sqlBuilder.quote(tableAlias, col);
  + return sqlBuilder.quote(tableAlias, col)
  +     + ((noColumnAlias) ? "" : " as " + col);
  ```

- `AbstractJdbcEventAnalyticsManager.getTableAndColumn` was changed similarly for the time-field-period branch and the catch-all fallback. Both now alias on `!isGroupByClause` and emit a bare reference on `isGroupByClause` (so the GROUP BY shape is unchanged):

  ```diff
  - return sqlBuilder.quote(DATE_PERIOD_STRUCT_ALIAS, col);
  + String expr = sqlBuilder.quote(DATE_PERIOD_STRUCT_ALIAS, col);
  + return isGroupByClause ? expr : expr + " as " + col;
    ...
  - return quoteAlias(col);
  + return isGroupByClause ? quoteAlias(col) : quoteAlias(col) + " as " + col;
  ```

The dynamic period bucket branch (`DateFieldPeriodBucketColumnResolver`) and the org-unit-by-level path already produced aliased SELECT expressions — they were left alone.

### Suggested follow-up

The deeper smell is that period dimension names are propagated in display case from `PeriodTypeEnum.getName()` and only opportunistically lowercased at use-sites. Lowercasing once at the point a dimension is constructed (so `getDimensionName()` always returns the canonical form) would remove the need for defensive lowercasing in row-building and SQL-emission code alike.

A complementary clean-up would standardise the alias pattern at every dimension SELECT-emission site (right now alias presence is decided locally in each branch of `getTableAndColumn` and in `ouQuote`). A single helper that decides "alias on SELECT, bare on GROUP BY" once would make the contract explicit and remove the JDBC-driver-dependent footgun this issue exposed.

---
## ISSUE: Legend-set companion columns missing in ClickHouse analytics tables

For data elements (and tracked entity attributes) that have a legend set attached, the analytics tables on Postgres and Doris carry a companion column named `<deUid>_<lsUid>` (or `<attrUid>_<lsUid>`) populated at table-build time with the UID of the legend whose `[startValue, endValue)` range contains the row's value. Aggregate queries that group or filter by legend bucket reference these columns directly — e.g. `select … ax."vV9UWAZohSf_OrkEzxZEH4X" as "Zj7UnCAulEk.vV9UWAZohSf-OrkEzxZEH4X" from analytics_event_<programUid> as ax …`.

On ClickHouse the column doesn't exist. The query fails with:

```
Code: 47. DB::Exception: Identifier 'ax.vV9UWAZohSf_OrkEzxZEH4X' cannot be resolved from table with name ax.
```

The schema confirms it — only the bare data-element columns are present:

```
`vV9UWAZohSf` Nullable(Int64),
`GieVkTxp4HH` Nullable(Float64),
…
```

The cause sits in `JdbcEventAnalyticsTableManager.getColumnFromDataElementWithLegendSet` (line 832) and the analogous TE-attribute method `getColumnForAttributeWithLegendSet` (line 775). Both early-return `List.of()` when `!sqlBuilder.supportsCorrelatedSubquery()`:

```java
private List<AnalyticsTableColumn> getColumnFromDataElementWithLegendSet(...) {
  if (!sqlBuilder.supportsCorrelatedSubquery()) {
    return List.of();
  }
  // … otherwise build the legend-lookup column using a correlated subquery
}
```

`ClickHouseSqlBuilder.supportsCorrelatedSubquery()` returns `false` (correctly — see issue #3 above), so the legend-set columns are never created on ClickHouse. The query-generation path, however, doesn't consult that flag — it emits `<deUid>_<lsUid>` references unconditionally whenever a query item has a legend set, and the engine fails to resolve the column.

Unlike the earlier issues, this is a **schema/feature gap** rather than a SQL-shape mismatch. The two halves of the analytics pipeline (table population vs. query generation) disagree about which columns exist on ClickHouse.

### FIX

Option 1 (inline CASE expression). The two halves of the analytics pipeline are reconciled at the table-population side: instead of skipping the legend-set columns on ClickHouse, generate them with an inline `CASE` expression that bakes the legend ranges into the SQL. The companion columns now exist in the ClickHouse analytics tables with the same name, type, and semantics as on Postgres/Doris, so the unchanged query path keeps working.

`JdbcEventAnalyticsTableManager.getColumnFromDataElementWithLegendSet` was changed to delegate to a new helper when the engine doesn't support correlated subqueries:

```java
private List<AnalyticsTableColumn> getColumnFromDataElementWithLegendSet(
    DataElement dataElement, String selectExpression, String dataFilterClause,
    ProgramType programType) {
  if (!sqlBuilder.supportsCorrelatedSubquery()) {
    return getLegendCaseColumns(dataElement, selectExpression);
  }
  // ... existing correlated-subquery path unchanged for Postgres/Doris
}
```

`getLegendCaseColumns` builds one `AnalyticsTableColumn` per legend set on the data element. The select-expression for each column is built by `buildLegendCaseExpression`, which emits:

```sql
(case
   when <valueExpr> >= <start1> and <valueExpr> < <end1> then '<legendUid1>'
   when <valueExpr> >= <start2> and <valueExpr> < <end2> then '<legendUid2>'
   ...
   else null
 end) as <deUid>_<lsUid>
```

`<valueExpr>` is the existing `columnExpression` produced by `ColumnMapper.getColumnExpression`, which already coerces non-numeric inputs to `NULL`. `NULL` propagates through the inequality comparisons and falls through to the `ELSE NULL` branch, matching the Postgres behaviour where the correlated subquery returns no rows for non-numeric values.

The boundary semantics (`startvalue <= v AND endvalue > v` in the original SQL, equivalent to `v >= startvalue AND v < endvalue`) are preserved.

Postgres and Doris are unaffected: their `supportsCorrelatedSubquery()` returns `true`, so the existing correlated-subquery path is taken unchanged.

### Suggested follow-up

- The symmetric path for tracked-entity-attribute legend sets (`JdbcEventAnalyticsTableManager.getColumnForAttributeWithLegendSet`, line 775) still early-returns `List.of()` on ClickHouse. The current failing test exercises only the data-element path, so this was deliberately left for a follow-up. The same `CASE`-expression pattern applies — the only adjustment is reading the value from the attribute column rather than the JSON-extracted data-element expression.
- The repeated `<valueExpr>` in each `WHEN` branch will balloon the SQL when the value expression is itself large (e.g. a JSON extract with regex coercion). For typical legend sets (4–10 legends) and current value expressions this is fine; if it ever becomes a hot spot, the value expression can be hoisted into a per-row CTE or a materialised subexpression at the cost of more involved emission.

---

## Patterns that recur across these issues

Several of the failures share a common root: the analytics SQL builders carry assumptions that hold on Postgres and on Doris (with `lower_case_table_names=1`), but not on ClickHouse:

1. **Identifier casing must be consistent at the emission site.** ClickHouse will not silently normalise.
2. **Column scoping inside CTEs and derived tables follows the projection's table prefix unless an explicit output alias is given.** Identity-aliases (`ax.enrollment as enrollment`) are the safe portable form.
3. **NULL handling around date casts is engine-specific.** ANSI `cast(... as date)` is not universally NULL-tolerant.
4. **Correlated subqueries are not portable.** Where a correlated subquery is convenient, an equivalent JOIN form is almost always available and preferable.

Where possible the fixes go through the SQL builder abstraction (engine-aware overrides) rather than scattering `if (clickhouse) ...` conditionals through the manager classes.
