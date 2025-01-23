# Enrollment CTE refactoring

## Introduction

In DHIS2, retrieving Enrollment data often requires additional attributes—counts, last recorded values, or other derived fields—from associated Event tables. 
Historically, we used correlated subqueries in the main SELECT list and in the WHERE clause. 
However, these correlated subqueries pose two significant problems:

- **Performance Degradation in PostgreSQL**
Repeated scanning of event tables for each enrollment can be a major bottleneck.
- **Incompatibility with Certain Analytics Engines**
Engines like ClickHouse or Doris do not support queries that reference outer fields in subqueries (e.g., ax.enrollment in a subquery). 
This prevents the original correlated subquery approach from working at all in those environments.

To address both of these issues, we refactored the query to use Common Table Expressions (CTEs). 
By computing results upfront in separate CTEs and then joining the results, we eliminate the need for correlated subqueries, 
thereby improving performance and ensuring compatibility with more analytics engines.

## The Original Query (Correlated Subqueries)

```sql
SELECT
    enrollment,
    trackedentity,
    enrollmentdate,
    occurreddate,
    storedby,
    createdbydisplayname,
    lastupdatedbydisplayname,
    lastupdated,
    ST_AsGeoJSON(enrollmentgeometry),
    longitude,
    latitude,
    ouname,
    ounamehierarchy,
    oucode,
    enrollmentstatus,
    ax."ou",
    ax."Bpx0589u8y0",
    (
      SELECT COUNT((occurreddate IS NOT NULL))
      FROM analytics_event_m3xtlkyblki AS subax
      WHERE enrollment = ax.enrollment
        AND ps = 'CWaAcQYKVpq'
    ) AS "d6Sr0B2NJYv",
    (
      SELECT "Qvb7NExMqjZ"
      FROM analytics_event_M3xtLkYBlKI
      WHERE eventstatus != 'SCHEDULE'
        AND enrollment = ax.enrollment
        AND ps = 'uvMKOn1oWvd'
      ORDER BY occurreddate DESC, created DESC
      LIMIT 1
    ) AS "uvMKOn1oWvd.Qvb7NExMqjZ",
    ax."coaSpbzZiTB"
FROM analytics_enrollment_m3xtlkyblki AS ax
WHERE
    enrollmentdate >= '2021-01-01'
    AND enrollmentdate < '2022-01-01'
    AND ax."uidlevel1" = 'ImspTQPwCqd'
    AND (
      SELECT "fADIatyOu2g"
      FROM analytics_event_M3xtLkYBlKI
      WHERE eventstatus != 'SCHEDULE'
        AND enrollment = ax.enrollment
        AND ps = 'uvMKOn1oWvd'
      ORDER BY occurreddate DESC, created DESC
      LIMIT 1
    ) IS NULL
ORDER BY "lastupdated" DESC NULLS LAST
LIMIT 101 OFFSET 0;
```

## The Refactored Query (Using CTEs)

```sql
WITH pi_d6sr0b2njyv AS (
  SELECT
    enrollment,
    COUNT((occurreddate IS NOT NULL)) AS value
  FROM analytics_event_m3xtlkyblki
  WHERE ps = 'CWaAcQYKVpq'
  GROUP BY enrollment
),
ps_uvmkon1owvd_uvmkon1owvd_qvb7nexmqjz AS (
  SELECT
    enrollment,
    "Qvb7NExMqjZ" AS value,
    ROW_NUMBER() OVER (
      PARTITION BY enrollment
      ORDER BY occurreddate DESC, created DESC
    ) AS rn
  FROM analytics_event_M3xtLkYBlKI
  WHERE eventstatus != 'SCHEDULE'
    AND ps = 'uvMKOn1oWvd'
),
uvmkon1owvd_fadiatyou2g AS (
  SELECT
    enrollment,
    "fADIatyOu2g" AS value
  FROM (
    SELECT
      enrollment,
      "fADIatyOu2g",
      ROW_NUMBER() OVER (
        PARTITION BY enrollment
        ORDER BY occurreddate DESC, created DESC
      ) AS rn
    FROM analytics_event_m3xtlkyblki
    WHERE eventstatus != 'SCHEDULE'
      AND ps = 'uvMKOn1oWvd'
  ) ranked
  WHERE rn = 1
)
SELECT
  ax.enrollment,
  ax.trackedentity,
  ax.enrollmentdate,
  ax.occurreddate,
  ax.storedby,
  ax.createdbydisplayname,
  ax.lastupdatedbydisplayname,
  ax.lastupdated,
  ST_AsGeoJSON(enrollmentgeometry),
  ax.longitude,
  ax.latitude,
  ax.ouname,
  ax.ounamehierarchy,
  ax.oucode,
  ax.enrollmentstatus,
  ax."ou",
  ax."Bpx0589u8y0",
  COALESCE(mahfm.value, 0) AS d6Sr0B2NJYv,
  rvwqm_0.value AS "uvMKOn1oWvd.Qvb7NExMqjZ",
  ax."coaSpbzZiTB"
FROM analytics_enrollment_m3xtlkyblki AS ax
LEFT JOIN pi_d6sr0b2njyv AS mahfm
  ON mahfm.enrollment = ax.enrollment
LEFT JOIN ps_uvmkon1owvd_uvmkon1owvd_qvb7nexmqjz AS rvwqm_0
  ON rvwqm_0.enrollment = ax.enrollment
  AND rvwqm_0.rn = 1
LEFT JOIN uvmkon1owvd_fadiatyou2g AS cxylo
  ON cxylo.enrollment = ax.enrollment
WHERE
    enrollmentdate >= '2021-01-01'
    AND enrollmentdate < '2022-01-01'
    AND ax."uidlevel1" = 'ImspTQPwCqd'
    AND cxylo.value IS NULL
ORDER BY "lastupdated" DESC NULLS LAST
LIMIT 101 OFFSET 0;
```

### Explanation of the CTE Approach

1. `pi_d6sr0b2njyv`

- Aggregates the count of events per enrollment (`where ps = 'CWaAcQYKVpq'`).
- Eliminates the need for a correlated subquery counting `occurreddate`.

2. `ps_uvmkon1owvd_uvmkon1owvd_qvb7nexmqjz`

- Retrieves `Qvb7NExMqjZ` for each enrollment via a window function (`ROW_NUMBER()`) to find the most recent record.
- Eliminates the need for a subquery that grabbed the top 1 event by `occurreddate DESC, created DESC`.

3. `uvmkon1owvd_fadiatyou2g`

- Retrieves the latest `"fADIatyOu2g"` value (again using `ROW_NUMBER()`).
- Replaces the subquery used in the original `WHERE` clause to check if `"fADIatyOu2g"` is `NULL`.

By computing these results in independent CTEs and then joining on enrollment, we avoid referencing ax.enrollment directly in subqueries. 
This approach is compatible with additional analytics engines and often provides better performance in PostgreSQL as well.

### Overall flow

Below is a high‐level overview of how the **`buildEnrollmentQueryWithCte`** method works and how it refactors the existing logic into a CTE‐based query. 

Inside **`buildEnrollmentQueryWithCte(params)`**, the code proceeds in these key steps:

1. **Collect CTE Definitions**
    - It scans through the **`EventQueryParams`** (especially `params.getItems()`) to identify which columns need special handling. This includes:
        - **Program Indicator** items
        - **Items bound to a specific Program Stage** (sometimes with offsets, e.g., “nth event” logic)
    - For each type of item, we build (or delegate building of) a CTE definition (SQL snippet). These definitions are stored in a **`CteContext`** object.

2. **Generate CTE Filters**
    - Additional filters may be needed for query items that have filters (`item.hasFilter()`). If such items require “latest event” logic or repeated‐stage handling, a separate **filter CTE** is generated.

3. **Append the CTE Clause**
    - We gather all generated CTE definitions from the **`CteContext`** and place them in a `WITH ...` clause at the start of the SQL.

4. **Construct the Main SELECT**
    - We pick the **standard enrollment columns** (e.g., `enrollmentdate`, `trackedentity`, `lastupdated`) plus any columns derived from **CTEs**.
    - This step merges basic columns (from the main `analytics_enrollment_*` table) with additional “value” columns pulled out of the CTE definitions.

5. **FROM and JOIN Logic**
    - The **`FROM`** clause references the main enrollment analytics table (e.g. `FROM analytics_enrollment_m3xtlkyblki AS ax`).
    - The code appends **LEFT JOIN** statements for each CTE that needs to link to the main table on `enrollment`.

6. **WHERE Clause**
    - The base “where” conditions come from the original logic (`getWhereClause(params)`), covering date ranges, organization units, statuses, etc.
    - Additional filters are applied if they relate to columns computed in the CTEs (via `addCteFiltersToWhereClause`).

7. **Sorting and Paging**
    - Finally, an **ORDER BY** (if `params` is sorting on something) is appended.
    - A **LIMIT/OFFSET** is added according to the required paging settings.

When done, the **StringBuilder** contains the fully assembled SQL statement.

## 3. Key Methods / Helpers

Within **`buildEnrollmentQueryWithCte`**, you’ll see these helper calls:

1. **`getCteDefinitions(params)`**
    - Identifies all items (e.g., Program Stage columns, Program Indicators) that need a subquery.
    - Creates a **CTE definition** for each item, handling “row_number” logic or Program Indicator subqueries.
    - Stores definitions in a `CteContext`.

2. **`generateFilterCTEs(params, cteContext)`**
    - Looks for **filters** on items (e.g. `item.hasFilter()`).
    - If an item needs a subquery filter (e.g. “the last event’s value must be X”), builds a “filter CTE” specifically for that.

3. **`appendCteClause(sql, cteContext)`**
    - Aggregates all CTE SQL fragments from the context.
    - Organizes them in a `WITH cteName AS ( ... )` structure and appends to `sql`.

4. **`appendSelectClause(sql, params, cteContext)`**
    - Builds the `SELECT` part.
    - Merges the default columns (e.g., `ax.enrollment`) with columns derived from each CTE definition (e.g. `cteAlias.value AS someColumn`).

5. **`appendCteJoins(sql, cteContext)`**
    - For each CTE, inserts a `LEFT JOIN cteName alias ON cteAlias.enrollment = ax.enrollment`.
    - Enables referencing computed values in the main SELECT.

6. **`appendWhereClause(sql, params, cteContext)`**
    - Uses the original `getWhereClause(params)` for base filters.
    - Plus merges in any filter conditions from the CTE context (e.g., `cteAlias.value = X`).

7. **`appendSortingAndPaging(sql, params)`**
    - Optionally appends an `ORDER BY ...`.
    - Adds `LIMIT x OFFSET y` if relevant.

## 4. Notable Changes from Pre‐CTE Logic

1. **Elimination of Correlated Subqueries**
    - Previously:
      ```sql
      SELECT
        (SELECT ... FROM analytics_event_xxx WHERE enrollment = ax.enrollment ...)
      ```
      repeated for each column.
    - Now replaced by a single (or multiple) CTE definitions, joined once.

2. **Better Handling of Repeatable Stages**
    - The offset logic (`createOffset(...)`) and row numbering (`row_number() OVER (...)`) are centralized in specialized CTEs instead of inline subqueries.

3. **Filter Consolidation**
    - Complex filters on “the most recent event for stage X” are turned into “filter CTEs” plus a straightforward check (e.g., `cteAlias.value = 'someFilter'`).
