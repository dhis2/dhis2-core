# ETag API cache: memory evidence

**Date:** 2026-07-10 (wording reconciled 2026-07-11)  
**Feature:** `cache.api.etag.enabled` (PR #23200 / DHIS2-20736)  
**Audience:** reviewers worried the ETag cache will bloat server RAM  
**Author:** Morten Svanaes

**Bottom line for other devs:** the ETag cache does **not** hold response bodies. It holds a small map of version counters (one `AtomicLong` per entity type and per named key) plus short-lived per-connection DML batches. **Retained footprint** is bounded by design to the order of **tens of kilobytes** (see §1.1 arithmetic). Separately, under load the **alloc-profiler JFR file** was smaller with the cache ON (more 304 short-circuits). That JFR size is a **sampled-event-count proxy**, not measured allocation bytes, and is only directional for allocation *rate* / GC pressure, not for retained heap.

---

## 1. What is stored (structure) — retained footprint

| Structure | Location | Bound |
|---|---|---|
| Entity-type versions | `LocalETagService.entityTypeVersions` | One entry per **distinct FQCN** that has been bumped (observed metadata types) |
| Named versions | `LocalETagService.namedVersions` | One entry per named key (`installedApps`, `staticContent`, …) |
| Global bump | `allCacheVersion` | Single `AtomicLong` |
| Pending DML batches | `DmlObserverListener.pendingBatches` | One batch per open JDBC connection; events **deduped by table+operation**; stale batches swept after 5 minutes |

**Not stored:** response bodies, ETag strings per client, or per-row versions.

So retained RAM does **not** scale with “how many GETs” or “how many data values”. It scales with “how many entity types / named keys have ever been bumped since process start” (and briefly with concurrent open write connections).

### 1.1 Design-bound arithmetic (retained)

Sources:

- Observed types: any class assignable to `MetadataObject`, plus 8 additional FQCNs in `ETagObservedEntityTypes` (`Configuration`, `FileResource`, `UserSetting`, `DatastoreEntry`, `UserDatastoreEntry`, `DataStatistics`, `DataStatisticsEvent`, `SystemSetting`). A source scan of `dhis-api` finds on the order of **~66** `MetadataObject` implementors, so an upper bound of roughly **~74** entity-type keys if every type is bumped at least once.
- Named keys in production call sites today: **`installedApps`** and **`staticContent`** (2 keys).
- Map payload (order-of-magnitude, HotSpot 64-bit compressed oops): each `ConcurrentHashMap` entry is roughly one `Node` (~40 B) + FQCN `String` (~150–250 B for a ~50-char name) + `AtomicLong` (~24 B) plus table overhead → about **~350 B per entry** as a conservative working estimate (not a precise `jmap` measurement).

| Component | N | ≈ bytes | ≈ KiB |
|---|---:|---:|---:|
| `entityTypeVersions` at full observed universe | ~74 | 74 × 350 | **~25** |
| `namedVersions` (current call sites) | 2 | 2 × 350 | **&lt;1** |
| `allCacheVersion` | 1 | ~24 | negligible |
| **Steady-state process-local maps (upper bound)** | | | **~30 KiB** |

Pending DML batches are **transient** (one set of table/op events per open connection, deduped). They do not grow with request rate after dedupe; abandoned batches are swept (query-count gated). Even a pessimistic “dozens of connections × dozens of distinct tables” stays in the low hundreds of KiB, still far from response-body cache scale.

**This is the retained-heap argument.** It rests on structure + cardinality unit tests, not on the alloc JFR file sizes below.

---

## 2. Code changes (this PR)

| Item | Detail |
|---|---|
| Micrometer gauges | `dhis2_etag_entity_versions_size`, `dhis2_etag_named_versions_size`, `dhis2_dml_observer_pending_batches`, `dhis2_dml_observer_pending_events_total` |
| When registered | Only if `monitoring.cache.etag.enabled=on` (same gate as other ETag metrics) |
| Cardinality / concurrency tests | `LocalETagServiceCardinalityTest`: **4** `@Test` methods (e.g. 10k bumps of one type → map size still 1; named keys separate). `LocalETagServiceConcurrencyTest`: 2× `@RepeatedTest(5)` + 2× `@Test` → **12** executions if you count expansions. Together that is **16** JUnit *executions*, not 16 methods on the cardinality class alone. |

Commit: `e63efa82aef` on `api_cache_dml_wip`.

---

## 3. Allocation-rate proxy under load (measured 2026-07-10) — not retained heap

This section answers a **different** question than §1: “does the cache increase transient allocation / GC pressure under page-load traffic?”

### Environment

| Item | Value |
|---|---|
| Host | minibox, stock Docker perf stack, 10G heap |
| Image | `dhis2/core-pr:etag-ab-local` @ **`e63efa82aef`** |
| Profile | load, 10 users, realistic think-time, `WARMUP=1 MEASURED=1` |
| Profiler | `PROF_ARGS="-e alloc"` (async-profiler allocation sampling) |
| Stamp | `target/etag-ab/20260710T105350Z` |
| Host RAM headroom | **not recorded at run time** |

### Latency (sanity during alloc run)

| Side | mean | p50 | p95 |
|---|---:|---:|---:|
| OFF measured | 14 ms | 9 | 43 |
| ON measured | 8 ms | 7 | 12 |

### Alloc profile artifacts (JFR **file size**, not allocated bytes)

| Side | `profile.html` (alloc) | `profile.jfr` size |
|---|---|---:|
| OFF m2 | `…/pageloadsimulation-20260710105840938-etag-off-m2/profile.html` | **~11.0 MB** |
| ON m2 | `…/pageloadsimulation-20260710110720511-etag-on-m2/profile.html` | **~3.7 MB** |

**Read carefully:** the column is the **size of the async-profiler JFR recording file**. File size scales with the number of sampled allocation events and distinct stack traces, **not** with total bytes allocated by the JVM. No sampling interval, recording duration, or per-side request count is pinned in this doc, and the run is **n=1** measured sample per side. Treat the ~3× smaller ON file as a **directional proxy** only (“fewer alloc samples / quieter profile under the same load shape”), consistent with more 304 short-circuits, **not** as a measured allocation-volume or retained-heap result.

### GC log size (same runs)

| Side | `gc.log` lines | size |
|---|---:|---:|
| OFF m2 | 741 | 101 KB |
| ON m2 | 704 | 96 KB |

Slightly quieter GC log with ON; not a large difference over a few minutes of load (expected: both sides still allocate for the miss path and for non-cached work).

Reproduce:

```sh
cd dhis-2/dhis-test-performance
PROF_ARGS="-e alloc" CAPTURE_SQL=1 \
DHIS2_IMAGE=dhis2/core-pr:etag-ab-local \
PROFILE=load WARMUP=1 MEASURED=1 FAST=false \
./scripts/etag-ab-benchmark.sh
```

---

## 4. What this does **not** yet claim

| Gap | Notes |
|---|---|
| Multi-hour soak | No 4h capacity soak yet |
| Live gauge values | Gauges are **designed and registered** when `monitoring.cache.etag.enabled=on`, but **no live scrape has ever observed a non-empty value** in this evidence set. Treat them as **designed-but-unverified** until a real `/api/metrics` scrape is recorded. They are **not** yet a continuous production bound. |
| Cardinality / concurrency unit tests | These pin map growth under synthetic load (4 cardinality tests + concurrency suite). They support the §1 design argument; they do not replace a live scrape. |
| `jmap -histo` inside distroless image | Not available without a debug sidecar |
| True sampled-bytes from async-profiler | Not extracted; only JFR **file size** was recorded |
| Import-storm pending-batch chart | Follow-up; design already dedupes by table+op |

---

## 5. How reviewers can verify in 10 minutes

1. Read `LocalETagService` — two small maps, no body cache. Re-check the §1.1 arithmetic.  
2. Run `LocalETagServiceCardinalityTest` (4 methods) and optionally the concurrency suite.  
3. Open the two alloc `profile.html` files above (OFF vs ON) as a **directional** allocation-rate proxy only.  
4. Optional (not yet done in this evidence pack): enable `monitoring.cache.etag.enabled=on` and scrape  
   `dhis2_etag_entity_versions_size` / `dhis2_etag_named_versions_size` after traffic; expect small, flat values. Until that scrape exists, do not treat gauges as verified.

---

## Related

- Latency/SQL: `BENCHMARKS-etag.md` (canonical load p95: pooled glog, stamp `20260710T084534Z`, OFF 36 ms / ON 7 ms / −81%)  
- Product: multi-node force-off (`ApiETagCacheActivation`: clustering or Redis cache invalidation)
