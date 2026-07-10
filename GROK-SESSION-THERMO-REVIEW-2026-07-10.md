# Thermonuclear review: Grok session work on `api_cache_dml_wip`

**Date:** 2026-07-10
**Author:** Morten Svanaes (review run by Claude Fable 5, multi-agent adversarial pipeline)
**Review target:** commit range `c7f4d0516d9..bac6ed8cd71` (8 commits, 26 files, ~4,700 insertions) produced by the Grok 4.5 session that ran in this checkout from 09:33 to ~22:25 local on 2026-07-10.
**Feature context:** PR #23200 (opened 2026-03-13, state: open) "feat: API ETag cache" / DHIS2-20736.
**Ground rules honored:** read-only review. No files changed (other than this report + CHANGELOG entry, approved after the fact), no tests re-run, no remote calls beyond `gh pr view`.

## Reviewed commits (all 2026-07-10)

| Commit | Subject |
|---|---|
| `2d0ce674f16` | test: wire cache e2e suite into CI as cache-api-test |
| `c540daba043` | fix: force API ETag cache off when DHIS2 clustering is enabled |
| `717e5a48fed` | test: add ETag A/B benchmark protocol and dated numbers |
| `c2f75c6d97a` | docs: record CPU flamegraph A/B results |
| `e63efa82aef` | feat: add ETag/DML memory gauges and cardinality tests |
| `86e212a1b0c` | docs: add MEMORY-etag.md with alloc A/B and design bounds |
| `21a1e9ec7f7` | docs: add static HTML team report |
| `bac6ed8cd71` | docs: add ETag charts report from real Gatling CSV data |

## Executive verdict

The work is directionally sound and unusually honest: the benchmark docs disclose most of their own gaps, no A/B labels are swapped, the CHANGELOG numbers match the benchmark doc, and the qualitative conclusion (ETag ON substantially cuts read p95) is corroborated by independent SQL-volume evidence. However:

1. **One P0 in production code**: the clustering force-off keys on a legacy config signal and misses redis-based multi-node deployments, recreating the exact stale-304 bug it was meant to prevent.
2. **The memory evidence is weaker than its headline**: JFR file size presented as an allocation measurement, retained-heap question answered with allocation-rate data, and no gauge value ever observed live.
3. **The benchmark tooling has a dead-code bug** that forced hand transcription of every result table, which is the likely root cause of the committed docs disagreeing with each other on the headline number (OFF p95 32 vs 36 ms, win 77% vs 81%).
4. **The report-artifact layer is redundant**: four overlapping deliverables including two HTML reports and an orphaned 2,239-line JSON.

Severity totals: 1 P0, 2 P1, 17 P2, 13 P3. Three plausible objections were adversarially refuted (see "What survived scrutiny").

---

## P0

### P0-1. Cluster force-off misses redis-based multi-node topologies (stale 304s across nodes)

> **Status update 2026-07-11:** fixed in `f703d0d6acd` (with P2-10 and P2-11) by the Grok session under mc orchestration; 29 tests verified independently, commit not yet pushed.

**File:** `dhis-2/dhis-support/dhis-support-external/src/main/java/org/hisp/dhis/external/conf/ApiETagCacheActivation.java:57` (commit `c540daba043`)

`isEffectivelyEnabled()` forces the ETag cache off only when `config.isClusterEnabled()` is true, and `isClusterEnabled()` (`DefaultDhisConfigurationProvider.java:225`) is defined purely as `cluster.members` AND `cluster.hostname` being set. That signal has zero other production callers in the codebase; it is legacy configuration. DHIS2's actual multi-node cache-coherence mechanism is `redis.cache.invalidation.enabled` (`REDIS_CACHE_INVALIDATION_ENABLED`), an independent key with its own Spring condition (`CacheInvalidationEnabledCondition`) and no dependency on `cluster.*`. The Redis invalidation listener processes invalidation messages from peer servers but does NOT propagate ETag version bumps.

Consequence: the common production topology of N web nodes behind a load balancer with `redis.cache.invalidation.enabled=on` and `cluster.members` unset keeps `LocalETagService` + the DML observer registered and the cache ON on every node. A metadata write on node A leaves node B's process-local `AtomicLong` counters unchanged, so node B keeps answering 304 Not Modified from stale ETags indefinitely. This is precisely the cross-node staleness bug the commit set out to prevent; the guard checks the wrong (incomplete) signal.

**Fix:** `isEffectivelyEnabled()` must also force off when `REDIS_CACHE_INVALIDATION_ENABLED` (and arguably `REDIS_ENABLED`) is on, and the WARN message should name that signal too.

**Verification:** confirmed independently twice (by the main reviewer before the agent fleet, and by an adversarial verifier that re-derived it from the code).

---

## P1

### P1-1. Memory "alloc A/B" measures JFR file size, not allocation volume

**File:** `dhis-2/dhis-test-performance/MEMORY-etag.md:60`

The load-evidence table's column is literally `profile.jfr size` (~11.0 MB OFF vs ~3.7 MB ON), the size of the async-profiler recording file. File size scales with the number of sampled allocation events and distinct stack traces, not with bytes allocated. Lines 8 and 65 then reinterpret the file-size ratio as an allocation result ("Measured allocation profiles under load show less allocation with the cache ON", "Alloc JFR for ON is about 3x smaller"). No sampling interval, recording duration, or per-side request count is stated, and the run is n=1 per side (`WARMUP=1 MEASURED=1`).

**Fix:** report async-profiler's own aggregated sampled-bytes figure (or JFR `jdk.ObjectAllocationSample` totals), state the sampling config, and run more than one measured sample.

### P1-2. Retained-heap question answered with allocation-rate data

**File:** `dhis-2/dhis-test-performance/MEMORY-etag.md:8`

The doc's stated audience is "reviewers worried the ETag cache will bloat server RAM", a retained-heap question. The only load measurement offered (alloc JFR size + gc.log size) is about transient allocation / GC pressure, a different axis. Section 4 concedes there is no soak, no heap dump, no `jmap -histo`. The "does not bloat RAM" conclusion therefore rests on code reading plus dedup unit tests, while the load numbers are presented as if they corroborate the footprint claim.

Note: the conclusion itself is almost certainly correct (two small maps, bounded by construction). Ship risk is low; the finding is that the doc's confidence outruns its evidence. See P2-10 for the missing arithmetic that would make the claim falsifiable.

---

## P2: benchmark methodology

### P2-1. Headline numbers rest on n=1 to n=2 samples, no variance/CI anywhere

`BENCHMARKS-etag.md:240`. The live smoke story-opener (OFF p95=41 / ON p95=14) is a single measured sample per side (the doc admits this). Docker smoke and load runs use MEASURED=2. The CPU flamegraph run is n=1 per side and its quoted latency (34 ms vs 8 ms) was captured with async-profiler attached, yet it is cited in the CHANGELOG as a latency result. Verifier calibration: the qualitative conclusion is corroborated by four independent runs with large concordant effects, so the direction is established; the specific percentages are soft.
**Fix:** MEASURED>=5 per side, report median-of-runs p95 with min/max or IQR, de-weight the profiler-attached numbers.

### P2-2. Write-path cost and miss tax never measured: only the cache's best case is shown

`BENCHMARKS-etag.md:243`. Every run uses a static Sierra Leone demo DB with zero writes, so the ON side gets 304s on cycles 2-5 of each virtual-user session, the ideal case. The DML observer fires on every write transaction and its cost is unmeasured; the first-request miss tax (ETag compute + store) is blended into aggregate p95. Both are honestly listed as open gaps, but the "~70-77% p95 win" is still presented as the takeaway.
**Fix:** an `APP_CYCLES=1` both-sides run for the isolated miss tax, and a metadata/tracker import A/B with observer on vs off, before framing the feature as a net win.

### P2-3. Docker A/B is not numerically reproducible despite claiming to be

`BENCHMARKS-etag.md:8` says the file "records reproducible A/B numbers". The runs used `DHIS2_IMAGE=dhis2/core-pr:etag-ab-local`, a locally built never-pushed tag with no RepoDigest (run-simulation.sh's own metadata mechanism degrades to "unknown"), and the DB is a mutable `dev` dump. The A/B delta is internally valid (both sides share the same image), but the absolute numbers are not pinned.
**Fix:** record image `@sha256` digest and DB dump version in `meta.env`, or at minimum the full commit + dirty flag + DB image digest.

### P2-4. The automated compare step in the benchmark script is dead code

`dhis-2/dhis-test-performance/scripts/etag-ab-benchmark.sh:141`. The compare reads `$OUT_DIR/etag-off-m1.runpath` / `etag-on-m1.runpath`, but `run_side` names measured iterations with the shared loop index, so with default `WARMUP=1` the first measured runpath written is `m2`. An `-m1` file is never created for any WARMUP>=1; the `cat ... || true` silently yields empty and the compare never runs. Consequently every table in BENCHMARKS-etag.md was hand-transcribed from console/HTML output, which is the most likely root cause of P2-8 (32 vs 36 discrepancy).
**Fix:** derive the suffix from the actual index (`m$((WARMUP+1))`) or glob the newest `*-m*.runpath`; better, have the script emit the compare markdown that the doc tables are filled from so numbers are never hand-typed.

---

## P2: docs and number consistency

### P2-5. Same load campaign published with two different OFF p95 values and two different headline wins

`BENCHMARKS-etag.md:116` and `etag-chart-data.json`. For the identical campaign (stamp `20260710T084534Z`, same run dirs verbatim in both), BENCHMARKS-etag.md, CHANGELOG.md, and the team HTML report state OFF p95 ~32 ms and a 77% win (average of two per-run Gatling-console percentiles), while `etag-chart-data.json` (which the charts report calls its source of truth) recomputes OFF p95 = 36.0 from raw glog CSV, and the charts report shows an 81% win (pooled-sample percentile). Neither doc explains the aggregation difference.
**Fix:** pick one canonical aggregation (pooled percentile from raw CSV is the defensible one), regenerate all four artifacts from it, and note the method.

### P2-6. The two committed HTML handouts disagree with each other

`ETAG-CACHE-TEAM-REPORT.html:437` (commit `21a1e9ec7f7`) prints OFF 32 ms / ON 7.5 ms / "about 77% lower p95"; `ETAG-CACHE-CHARTS-REPORT.html` (commit `bac6ed8cd71`, ~2h later) renders OFF 36 / ON 7 / "-81%". Both are linked side by side from `dhis-test-performance/README.md` with no supersede note. Anyone opening both gets two different headline wins for the same feature.

### P2-7. Memory "No extra RAM" claim rests on the JFR proxy plus unit tests; no live gauge value ever captured

`MEMORY-etag.md:60` and section 4/5. The four Micrometer gauges are defined and registered but were never scraped with real values anywhere in the evidence (the ~19:12 empty scrape is disclosed as an open CI gap). Section 4 still calls "gauges + unit tests" the continuous bound and section 5 tells reviewers to scrape values that have never been observed to work.

---

## P2: testing gaps

### P2-8. PageLoadSimulation cannot fail on a broken cache

`dhis-2/dhis-test-performance/src/test/java/org/hisp/dhis/test/cache/PageLoadSimulation.java:264`. The single Gatling assertion is `forAll().successfulRequests().percent().gte(95.0)` and all 23 request checks accept `status().in(200, 304)`. A run with the cache completely broken (zero 304s) passes green and is indistinguishable from a healthy run. This is the vehicle that generated every headline number.
**Fix:** assert a 304 proportion and/or a percentile budget for the ON profile so a regressed cache fails instead of silently passing.

### P2-9. New memory gauges have zero test coverage; `pendingBatchCount()` is dead code

`LocalETagService.java:111` area and `DmlObserverListener`. Nothing asserts the gauges register, report correct sizes, respect the `monitoring.cache.etag.enabled` gate, or avoid double registration, while MEMORY-etag.md calls them "the continuous bound".
**Fix:** a small `SimpleMeterRegistry` test: register, bump versions/enqueue events, assert `gauge.value()` tracks sizes, assert absence when the gate is off; delete or cover `pendingBatchCount()`.

### P2-10. The real AND(members, hostname) cluster gate is untested; a half-configured cluster fails open

`ApiETagCacheActivationTest.java:65`. The test stubs `isClusterEnabled()` as a boolean, never exercising `DefaultDhisConfigurationProvider.isClusterEnabled()` = `isNotBlank(members) && isNotBlank(hostname)`. An operator who sets only one of the two keys gets `false` and the safety gate fails open with the cache still ON. Compounds P0-1.
**Fix:** a config-driven test matrix: members-only, hostname-only, both, neither, blank vs missing.

### P2-11. Both Spring Condition classes are untested; the "exact inverse" invariant is unverified

`ApiCacheDisabledCondition.java:42`. The Disabled condition's javadoc says it "Must stay the exact inverse" of the Enabled one so exactly one of `LocalETagService` / `NoOpETagService` registers, yet no test pins the inverse relation, the `profile=test` branches, or that the WARN fires only for on+clustered.
**Fix:** a table test asserting `Enabled.matches == !Disabled.matches` across the config/profile matrix.

### P2-12. Cardinality tests prove dedup on synthetic keys, not the real key path, and enforce no ceiling

`LocalETagServiceCardinalityTest.java:81`. The 4 tests call `service.incrementEntityTypeVersion(DataElement.class)` directly, bypassing the production path (DmlObserverListener's observed-type gate, `ETagObservedEntityTypes`), and assert exact small sizes rather than a global `size <= N` bound over the ~100-type observed universe.

---

## P2: CI wiring

### P2-13. cache-api-test performs a full second image build on every PR

`.github/workflows/run-api-tests.yml:98-112` (commit `2d0ce674f16`). The job runs its own `mvn clean verify ... jibDockerBuild` into a distinct tag with no `needs:` and no image reuse, on every `pull_request` (including forks and dependabot) and every master push. The repo's heaviest CI step is now duplicated per PR even for PRs that touch nothing cache-related. The analytics precedent is label-gated for exactly this reason.
**Fix:** fold the cache run into `api-test` as a second compose step sharing one build, or a `workflow_call` reusable job parameterized by the compose override; alternatively label-gate it.

### P2-14. cache-api-test is ~40 lines of copy-paste of api-test

Same file, lines 94-137 mirror api-test's steps almost verbatim (deltas: image tag suffix, one extra `-f` compose file, artifact name). Future recipe edits must be maintained twice. Fixed by the same consolidation as P2-13.

---

## P2: redundancy (with simpler alternatives)

### P2-15. `etag-chart-data.json` (2,239 lines / 44 KB) is an orphaned duplicate

The full dataset is embedded inline in `ETAG-CACHE-CHARTS-REPORT.html` (`<script id="data" type="application/json">`, line 222) and that inline copy is what the report renders. The standalone JSON is referenced by nothing (zero grep hits in any html/md/sh; README links only the HTML).
**Simpler:** delete the standalone JSON, or invert and keep only the JSON with the HTML loading it, so the numbers exist once.

### P2-16. Four overlapping narrative deliverables for one A/B result

Two HTML reports (705 + 564 lines, the first `.html` ever committed to `dhis-test-performance`, a Markdown-only module) plus BENCHMARKS-etag.md and MEMORY-etag.md. The two HTML files and BENCHMARKS overlap on the same latency numbers (MEMORY covers a distinct axis). The charts report loads Chart.js from an external CDN, so the committed evidence renders blank offline or under CSP.
**Simpler:** keep BENCHMARKS-etag.md (and MEMORY-etag.md) as the canonical source-tree deliverables; treat HTML/charts as build output under `target/` or PR attachments.

---

## P3 nits

1. `logIfClusterIncompatible` javadoc claims the WARN is "rare (bean registration only)" but it fires on each conditional bean evaluation.
2. Both Spring conditions independently re-bootstrap config from disk; consistency is by convention.
3. `MEMORY-etag.md:33` says "LocalETagServiceCardinalityTest (16 tests...)"; that class has exactly 4 `@Test` methods. 16 is only reachable by counting `@RepeatedTest` expansions across it and `LocalETagServiceConcurrencyTest`.
4. The new CI job has no `timeout-minutes` (hung compose burns the 6h default).
5. Team-report headline numbers carry no run stamp, violating the docs' own "treat every table as dated" rule.
6. Charts report depends on the external Chart.js CDN (see P2-16).
7. Minor aggregation/coverage inconsistencies between the charts and the md/team tables.
8. `dhis-etag-on.conf` / `dhis-etag-off.conf` differ by one meaningful line and re-duplicate the shared connection block. Simpler: one template + env var.
9. The two benchmark scripts duplicate the "find latest gatling dir + dump env" tail boilerplate.
10. The "stale batches swept after 5 minutes" claim is query-count-gated in code (`queryCounter % 1000`, `DmlObserverListener.java:232`), not time-guaranteed; on a quiescent server abandoned batches persist. Untested.
11. `incrementNamedVersion(String)` accepts arbitrary keys; the "small fixed set" bound holds only by convention of the two current call sites (`staticContent`, `installedApps`).
12. Runs were done on an oversubscribed 27 GiB shared box (compose wants ~32 GB); documented min-free 15.4 GiB vs monitoring that showed ~13 GiB free shortly before. No evidence of actual impact on the headline runs.
13. The alloc and CPU-flamegraph environment tables omit the host-RAM-headroom row that the smoke/load tables disclose.

---

## What survived scrutiny (refuted objections and verified positives)

Adversarially refuted, with evidence:

- **"Cold JVM inflates the win."** Refuted: warmup containers show p95 nearly identical to measured across independent cold containers (OFF 38/39/38, ON 11/12/11), the measured windows cover thousands of requests (JIT warms in-window), the cold first-request cost is symmetric, and the SQL-volume drop corroborates independently.
- **"Real fetch-based clients won't send If-None-Match."** Refuted: the server emits `Cache-Control: max-age=0, private` + `Vary`, the exact store-but-revalidate directive; spec-compliant fetch clients in default cache mode auto-revalidate. Gatling models default browser behavior faithfully.
- **CI chain correctness held up:** compose `--activate-profiles cache` correctly deactivates the default profile, surefire `groups=cache` resolves to the tagged suites, same triggers as api-test, no path where cache tests silently never run.

Verified positives: A/B conf toggle correctly wired and labels not swapped; CHANGELOG numbers match BENCHMARKS-etag.md; the entity/named map split in `e63efa82aef` is behavior-preserving (disjoint key spaces, consistent read/write pairs); gauges register once, gated, on final never-reassigned maps; `ApiETagCacheActivation` is a legitimate DRY extraction; `docker-compose.e2e-cache.yml` follows the existing e2e-analytics convention; the docs disclose write-path, miss-tax, soak, and variance gaps explicitly.

---

## Recommended action order

1. **P0-1**: extend the force-off to redis-based multi-node signals + tests (P2-10, P2-11).
2. **P2-5/P2-6**: reconcile 32-vs-36 / 77-vs-81 to one canonical aggregation and regenerate the artifacts.
3. **P1-1/P1-2/P2-7**: reword MEMORY-etag.md to match what was measured; add the retained-bytes arithmetic (~100-150 entries x ~150-200 B = tens of KB); capture one real gauge scrape.
4. **P2-8/P2-9**: make the benchmark assert cache effectiveness; add gauge tests.
5. **P2-4**: fix the dead compare step so tables are generated, not hand-typed.
6. **P2-13 to P2-16**: consolidate CI jobs and prune the report artifacts.
7. P3 nits opportunistically.

## Final disposition (2026-07-11, after the orchestrated fix campaign)

All findings were driven to closure by the Grok session under mc orchestration, each batch verified independently by Claude. Eight commits, `f703d0d6acd..d07d154632a`.

| Finding | Disposition |
|---|---|
| P0-1 cluster force-off | Fixed `f703d0d6acd` (redis invalidation signal added; 29 tests re-run independently) |
| P1-1 JFR file-size proxy | Fixed `4e17329709a` (language corrected) |
| P1-2 retained vs alloc conflation | Fixed `4e17329709a` (claims separated, ~30 KB retained math added) |
| P2-1 no variance | Fixed `d07d154632a` (n=5/side campaign: OFF p95 39-42 median 40, ON 11-12 median 11, pooled -72.5%; new canonical headline) |
| P2-2 miss tax / write tax | Miss tax measured `d07d154632a` (~0 to -1 ms, within noise). Write/import tax remains the single open measurement |
| P2-3 reproducibility pins | Fixed `7adc3fd801f` (digest/ID + git HEAD + dirty flag in meta.env, exercised live in the campaign) |
| P2-4 dead compare step | Fixed `c79f41d98e9`; parser bug found and fixed during live dogfood `d07d154632a` |
| P2-5/P2-6 number contradictions | Fixed `4e17329709a`; superseded by the n=5 headline in `d07d154632a`, all artifacts agree |
| P2-7 gauges never observed | Wording fixed `4e17329709a`; gauge unit tests added `e9891e5444b` |
| P2-8 benchmark cannot fail | Fixed `c79f41d98e9` (etag.expect 304-share assertions); proven live: OFF 0.0%, ON 76-81% across 12 cycles |
| P2-9 gauges untested | Fixed `e9891e5444b` (7 tests, re-run independently) |
| P2-10 cluster AND semantics untested | Fixed `f703d0d6acd` (ClusterEnabledConfigMatrixTest, 10 cases) |
| P2-11 condition inverse untested | Fixed `f703d0d6acd` (ApiCacheConditionInverseTest, 9 cases) |
| P2-12 synthetic cardinality tests | Fixed `c09faa3057e` (production observed-type gate + honest ceiling) |
| P2-13/P2-14 CI double build + copy-paste | Fixed `61e28eaad1d` (single build, cache suite as steps; tradeoff: no separate GitHub check) |
| P2-15 orphan JSON | Fixed `4e17329709a` (deleted) |
| P2-16 four overlapping reports | Partially accepted: numbers reconciled, handouts kept per Morty's preference |
| P3 nits 1,3,4,5,6,10,11,13 | Fixed across batches |
| P3 nits 2 | Fixed `c09faa3057e` (loadConfig dedup) |
| P3 nits 8,9 | Deliberately skipped: cleanup value below verification risk without a live compose run |
| P3 nits 7,12 | Superseded by the reconciliation and the new campaign disclosures |

Open follow-ups (separate PRs, not this branch): write/import-path DML observer benchmark; tightening the half-configured cluster.* fail-open (product decision); Grok's structural refactor proposal (DmlEvent collapse + EndpointDependencyCatalog extraction).

## Review methodology

Seven parallel review dimensions (production code, benchmark methodology, memory methodology, test quality, CI wiring, docs/number consistency, redundancy/simplification) run as Opus agents at high effort, followed by one adversarial Opus verifier per finding instructed to refute. 31 raw findings, 18 survived verification, 3 refuted, 10 unverified P3 nits. The memory-methodology finder returned a degenerate placeholder and was re-run and hand-verified (its 8 findings are folded in above). Total: 29 agents, ~1.6M tokens, ~16 minutes wall clock for the fleet. Full agent transcripts: `~/.claude/projects/-Users-netromsb-develop-dhis2-GARAGE-SLOT1-dhis2-core/574b7ce4-6d36-4066-97ad-8d4a629dd865/subagents/workflows/wf_22400923-7b7/`.
