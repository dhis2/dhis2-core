# ETag API cache: memory evidence

**Date:** 2026-07-10  
**Feature:** `cache.api.etag.enabled` (PR #23200 / DHIS2-20736)  
**Audience:** reviewers worried the ETag cache will bloat server RAM  
**Author:** Morten Svanaes

**Bottom line for other devs:** the ETag cache does **not** hold response bodies. It holds a small map of version counters (one `AtomicLong` per entity type and per named key) plus short-lived per-connection DML batches. Design bound is on the order of **kilobytes**, not megabytes of response data. Measured allocation profiles under load show **less** allocation with the cache ON (more 304 short-circuits), not more.

---

## 1. What is stored (structure)

| Structure | Location | Bound |
|---|---|---|
| Entity-type versions | `LocalETagService.entityTypeVersions` | One entry per **distinct FQCN** that has been bumped (observed metadata types) |
| Named versions | `LocalETagService.namedVersions` | One entry per named key (`installedApps`, `staticContent`, …) |
| Global bump | `allCacheVersion` | Single `AtomicLong` |
| Pending DML batches | `DmlObserverListener.pendingBatches` | One batch per open JDBC connection; events **deduped by table+operation**; stale batches swept after 5 minutes |

**Not stored:** response bodies, ETag strings per client, or per-row versions.

So RAM does **not** scale with “how many GETs” or “how many data values”. It scales with “how many entity types / named keys have ever been bumped since process start” (and briefly with concurrent open write connections).

---

## 2. Code changes (this PR)

| Item | Detail |
|---|---|
| Micrometer gauges | `dhis2_etag_entity_versions_size`, `dhis2_etag_named_versions_size`, `dhis2_dml_observer_pending_batches`, `dhis2_dml_observer_pending_events_total` |
| When registered | Only if `monitoring.cache.etag.enabled=on` (same gate as other ETag metrics) |
| Cardinality tests | `LocalETagServiceCardinalityTest` (16 tests with concurrency suite green): 10k bumps of one type → map size still 1; named keys separate |

Commit: `e63efa82aef` on `api_cache_dml_wip`.

---

## 3. Allocation A/B under load (measured 2026-07-10)

### Environment

| Item | Value |
|---|---|
| Host | minibox, stock Docker perf stack, 10G heap |
| Image | `dhis2/core-pr:etag-ab-local` @ **`e63efa82aef`** |
| Profile | load, 10 users, realistic think-time, `WARMUP=1 MEASURED=1` |
| Profiler | `PROF_ARGS="-e alloc"` (async-profiler allocation sampling) |
| Stamp | `target/etag-ab/20260710T105350Z` |

### Latency (sanity during alloc run)

| Side | mean | p50 | p95 |
|---|---:|---:|---:|
| OFF measured | 14 ms | 9 | 43 |
| ON measured | 8 ms | 7 | 12 |

### Alloc profile artifacts

| Side | `profile.html` (alloc) | `profile.jfr` size |
|---|---|---:|
| OFF m2 | `…/pageloadsimulation-20260710105840938-etag-off-m2/profile.html` | **~11.0 MB** |
| ON m2 | `…/pageloadsimulation-20260710110720511-etag-on-m2/profile.html` | **~3.7 MB** |

Alloc JFR for ON is about **3× smaller** than OFF for the same load shape. That is the opposite of “cache blows up allocations”: short-circuit 304s avoid controller/serialization allocations.

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
| Multi-hour soak | No 4h capacity soak yet; gauges + unit tests are the continuous bound |
| Live `/api/metrics` scrape in CI | Conf flags added for A/B confs; scrape depends on full monitoring stack being active in the image |
| `jmap -histo` inside distroless image | Not available without a debug sidecar; alloc JFR is the practical evidence |
| Import-storm pending-batch chart | Follow-up; design already dedupes by table+op |

---

## 5. How reviewers can verify in 10 minutes

1. Read `LocalETagService` — two small maps, no body cache.  
2. Run `LocalETagServiceCardinalityTest`.  
3. Open the two alloc `profile.html` files above (OFF vs ON).  
4. Optional: enable `monitoring.cache.etag.enabled=on` and scrape  
   `dhis2_etag_entity_versions_size` / `dhis2_etag_named_versions_size` after traffic; they should stay small and flat.

---

## Related

- Latency/SQL: `BENCHMARKS-etag.md`  
- Product: clustering forces ETag off (`ApiETagCacheActivation`)
