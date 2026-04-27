## Background

`GET /tracker/events` was producing slow queries on production-scale systems. Investigation found three structural bugs in the 2.41 tracker store.

### Root cause 1 — non-sargable COALESCE join

The store joined `organisationunit` via an unconditional COALESCE across TPO and the event row:

```sql
LEFT JOIN trackedentityprogramowner po
  ON (en.trackedentityid = po.trackedentityid AND en.programid = po.programid)
INNER JOIN organisationunit ou
  ON (COALESCE(po.organisationunitid, ev.organisationunitid) = ou.organisationunitid)
```

The COALESCE prevents the planner from pushing any org unit predicate to the underlying columns — no index on either column can drive the event scan. For `WITHOUT_REGISTRATION` programs this join is also semantically dead: no TPO row ever exists for single-event programs, so `po` is always NULL.

### Root cause 2 — enrollment as unconditional plan driver

Enrollment was an unconditional `INNER JOIN` before the program-type branch. For `WITHOUT_REGISTRATION` programs the planner chose enrollment as the Nested Loop outer, producing a catastrophic BitmapAnd via the `enrollmentid` index. On one production system a single enrollment owns 97% of the event table (12.3M of 12.4M rows) — the planner estimated 674 events per enrollment but found 12.3M.

### Root cause 3 — no direct org unit predicate for SELECTED mode

`createSelectedSql()` emitted only path-based access-scope EXISTS clauses — no direct `organisationunitid = :ou_id` equality predicate. Without it the planner has no selective entry point for the OU filter.

---

## Changes

### Fix 1 — correct join structure per program type

`getFromWhereClause()` now branches on program type:

```sql
-- WITHOUT_REGISTRATION (new)
-- Program derived from programstage FK; enrollment demoted to LEFT JOIN to prevent
-- it from driving the plan. Same pattern as master's JdbcSingleEventStore.
INNER JOIN programstage ps ON ps.programstageid = ev.programstageid
INNER JOIN program p ON p.programid = ps.programid
LEFT JOIN enrollment en ON en.enrollmentid = ev.enrollmentid
INNER JOIN organisationunit ou ON ev.organisationunitid = ou.organisationunitid

-- WITH_REGISTRATION with program filter (new)
-- TPO record is always created at enrollment. INNER JOIN is sargable.
INNER JOIN enrollment en ON en.enrollmentid = ev.enrollmentid
INNER JOIN program p ON p.programid = en.programid
INNER JOIN trackedentityprogramowner po
  ON (en.trackedentityid = po.trackedentityid AND en.programid = po.programid)
INNER JOIN organisationunit ou ON po.organisationunitid = ou.organisationunitid

-- No program filter (unchanged)
LEFT JOIN trackedentityprogramowner po ...
INNER JOIN organisationunit ou ON (COALESCE(...) = ou.organisationunitid)
```

### Fix 2 — direct org unit predicates for SELECTED, DESCENDANTS, and CHILDREN

Each method now emits a direct predicate before the access-scope EXISTS clauses so the planner can seek by org unit before evaluating correlated subqueries.

| Mode | WITHOUT_REGISTRATION | WITH_REGISTRATION |
|---|---|---|
| SELECTED | `ev.organisationunitid = :ou_id` | `ou.organisationunitid = :ou_id` → pushed to TPO index |
| DESCENDANTS | `ev.organisationunitid IN (:ou_ids)` — descendant IDs pre-resolved via JDBC query on `organisationunit`. Falls back to path-LIKE if subtree > 1 000 org units | `ou.path LIKE CONCAT(:ou_path, '%')` |
| CHILDREN | `ev.organisationunitid IN (:ou_ids)` — parent + direct children pre-resolved. Falls back to scalar anchor + subquery if > 1 000 | `ou.organisationunitid = :ou_id OR ...` |

`ACCESSIBLE` and `CAPTURE` scope is defined by the user's own org units inside the EXISTS subqueries — no equivalent outer predicate is possible for those modes.

---

## ⚠️ Known limitation — very large DESCENDANTS subtrees

For `ouMode=DESCENDANTS`, descendant org unit IDs are now pre-resolved by a JDBC query on the `organisationunit` table before the main event query is built. This eliminates the `IN (subquery)` that previously forced PostgreSQL to materialise a hash join and abandon `idx_psi_lastupdated_desc`.

For subtrees with ≤ 1 000 org units (covers virtually all real-world district and regional queries), the planner receives an exact cardinality and can choose between `idx_psi_lastupdated_desc` (ORDER BY scan, stops at LIMIT) and `idx_event_ou_ps_occurreddate` (date-bounded scan) based on the actual query shape.

For subtrees > 1 000 org units (e.g. a national-root query), the pre-resolved list exceeds the JDBC threshold and the code falls back to the path-LIKE strategy. This relies on `idx_psi_lastupdated_desc` and has the same behaviour as the pre-branch baseline.

**The correct long-term fix is to require at least one date parameter (`occurredAfter`, `occurredBefore`, `updatedAfter`, or `updatedBefore`) for `DESCENDANTS` queries on the API layer, and return HTTP 400 if none is provided.** This has been raised with the tracker team and declined. The risk is documented here for visibility.

### Integration test

`shouldReturnEventsForWithoutRegistrationProgramGivenOrgUnitModeSelected` in `EventExporterTest` exercises `orgUnitMode=SELECTED` with a `WITHOUT_REGISTRATION` program, closing the test gap that allowed all three bugs to go undetected.

### Flyway migration V2\_41\_58

- **New index** `idx_event_ou_ps_occurreddate(organisationunitid, programstageid, occurreddate)` — 3-column composite gives the planner a selective entry point for `WITHOUT_REGISTRATION` queries. The `programstageid` column is included because Capture app queries always specify a program stage; master made the same decision in V2_43_50.
- **Drop** `idx_programstageinstance_lastupdated` — duplicate of `in_event_lastupdated` left over from the `programstageinstance` → `event` rename.
- **Drop** `programstageinstance_organisationunitid` — single-column OU index superseded by the composite above. Master precedent: V2_43_50 dropped the equivalent `in_singleevent_organisationunitid`.
- **Statistics** `enrollmentid` column raised to 500 so the planner better estimates skewed distributions.
- **Autovacuum** thresholds tightened (`scale_factor=0.01`, `threshold=1000`); the default `scale_factor=0.2` never fires on a 10M+ row table.

---

## Performance results

Results from two production databases running stock 2.41 before this branch.

### Large scale EMIS — ~95M events, WITH_REGISTRATION

Improvement comes entirely from Fix 1 (join structure). The sargable TPO join lets the planner drive from `in_unique_trackedentityprogramowner_teiid_programid_ouid` instead of scanning the event table.

| | Stock 2.41 | This branch |
|---|---|---|
| Execution time | ~38,000 ms | ~531 ms cold / ~234 ms warm |
| Event rows scanned | 2,232,057 per loop | 0 (TPO drives the scan) |
| Shared buffers read | 1,033,269 pages | 2,276 pages (cold) / 0 (warm) |

### Single-event program — ~12.7M events, WITHOUT_REGISTRATION

Fix 1 eliminates the BitmapAnd entirely by demoting enrollment to LEFT JOIN.

| | Stock 2.41 | This branch (1-month) | This branch (1-year) |
|---|---|---|---|
| Execution time | ~41,000 ms | ~7 ms | ~860 ms |
| Event scan | BitmapAnd, 12.3M entries | Index Scan Backward, 131 rows | BitmapAnd, 626 rows |
| Rows entering access-check subplans | 10,661,309 | 131 | 620 |

### Tracker performance results

Clear winner here was the "Search Birth events" scenario.

### Median Response Time (p50) (ms)

| Scenario | Baseline | Feature | Diff | Change |
|:---|---:|---:|---:|:---|
| Get ANC events|Go to first page | 9 | 6 | -3 | :arrow_down: -33.3% |
| Get ANC events|Search not assigned | 9 | 6 | -3 | :arrow_down: -33.3% |
| Get Child Programme TEs|Get TEs from events | 6 | 5 | -1 | :arrow_down: -16.7% |
| Get Child Programme TEs|Get TEs with enrollment status | 41 | 41 | +0 | +0.0% |
| Get Child Programme TEs|Get first page of TEs | 38 | 37 | -1 | :arrow_down: -2.6% |
| Get Child Programme TEs|Not found TE by name with eq operator | 11 | 11 | +0 | +0.0% |
| Get Child Programme TEs|Not found TE by name with like operator | 13 | 12 | -1 | :arrow_down: -7.7% |
| Get Child Programme TEs|Search Birth events | 93 | 8 | -85 | :arrow_down: -91.4% |
| Get Child Programme TEs|Search TE by name with eq operator | 17 | 17 | +0 | +0.0% |
| Get Child Programme TEs|Search TE by name with like operator | 22 | 21 | -1 | :arrow_down: -4.5% |
| Get Child Programme TEs|Go to single enrollment|Get first enrollment | 11 | 11 | +0 | +0.0% |
| Get Child Programme TEs|Go to single enrollment|Get first tracked entity | 21 | 21 | +0 | +0.0% |
| Get Child Programme TEs|Go to single enrollment|Get relationships for first tracked entity | 3 | 2 | -1 | :arrow_down: -33.3% |
| Get Child Programme TEs|Go to single enrollment|Get one event|Get first event from enrollment | 12 | 12 | +0 | +0.0% |
| Get Child Programme TEs|Go to single enrollment|Get one event|Get relationships for first event | 2 | 2 | +0 | +0.0% |
| Login | 70 | 72 | +2 | :arrow_up: +2.9% |

### 95th Percentile Response Time (p95) (ms)

| Scenario | Baseline | Feature | Diff | Change |
|:---|---:|---:|---:|:---|
| Get ANC events|Go to first page | 11 | 10 | -1 | :arrow_down: -9.1% |
| Get ANC events|Search not assigned | 11 | 8 | -3 | :arrow_down: -27.3% |
| Get Child Programme TEs|Get TEs from events | 7 | 7 | +0 | +0.0% |
| Get Child Programme TEs|Get TEs with enrollment status | 50 | 49 | -1 | :arrow_down: -2.0% |
| Get Child Programme TEs|Get first page of TEs | 43 | 46 | +3 | :arrow_up: +7.0% |
| Get Child Programme TEs|Not found TE by name with eq operator | 18 | 13 | -5 | :arrow_down: -27.8% |
| Get Child Programme TEs|Not found TE by name with like operator | 19 | 14 | -5 | :arrow_down: -26.3% |
| Get Child Programme TEs|Search Birth events | 109 | 14 | -95 | :arrow_down: -87.2% |
| Get Child Programme TEs|Search TE by name with eq operator | 26 | 21 | -5 | :arrow_down: -19.2% |
| Get Child Programme TEs|Search TE by name with like operator | 28 | 26 | -2 | :arrow_down: -7.1% |
| Get Child Programme TEs|Go to single enrollment|Get first enrollment | 14 | 13 | -1 | :arrow_down: -7.1% |
| Get Child Programme TEs|Go to single enrollment|Get first tracked entity | 28 | 23 | -5 | :arrow_down: -17.9% |
| Get Child Programme TEs|Go to single enrollment|Get relationships for first tracked entity | 4 | 3 | -1 | :arrow_down: -25.0% |
| Get Child Programme TEs|Go to single enrollment|Get one event|Get first event from enrollment | 14 | 14 | +0 | +0.0% |
| Get Child Programme TEs|Go to single enrollment|Get one event|Get relationships for first event | 3 | 3 | +0 | +0.0% |
| Login | 75 | 83 | +8 | :arrow_up: +10.7% |

_:arrow_down: = faster (improvement), :arrow_up: = slower (regression)_

---

## Scope

All org unit modes benefit from the join structure fix when a program filter is present. For unfiltered queries (no program specified) the join is unchanged. In practice virtually all clients always specify a program.

Disclaimer: :robot: AI was used for portions of this PR.