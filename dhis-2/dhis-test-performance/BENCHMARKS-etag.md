# ETag API cache benchmarks

**Date:** 2026-07-10  
**Feature:** `cache.api.etag.enabled` (PR #23200 / DHIS2-20736)  
**Simulation:** `org.hisp.dhis.test.cache.PageLoadSimulation`  
**Author:** Morten Svanaes

This file records reproducible A/B numbers and the protocol to regenerate them. Treat every table as dated; re-run after material changes to the interceptor, observer, or exclusions.

---

## Protocol (how to reproduce)

### Full Docker path (preferred when the machine has ~32 GB free for web+DB)

From `dhis-2/dhis-test-performance/`:

```sh
# Build a local image of this branch first, then:
DHIS2_IMAGE=dhis2/core-pr:local \
PROFILE=smoke \
./scripts/etag-ab-benchmark.sh
```

That script flips `DHIS_CONF_FILE` between `docker/dhis-etag-on.conf` and `docker/dhis-etag-off.conf`, runs warmups + measured samples via `run-simulation.sh`, and writes under `target/etag-ab/<timestamp>/`.

For a heavier steady-state sample:

```sh
DHIS2_IMAGE=dhis2/core-pr:local PROFILE=load WARMUP=2 MEASURED=3 FAST=false \
  ./scripts/etag-ab-benchmark.sh
```

Optional: `CAPTURE_SQL=1` for pgbadger on measured runs; `PROF_ARGS="-e cpu"` via wrapping `run-simulation.sh` for flamegraphs.

### Live instance path (used for the 2026-07-10 minibox smoke numbers)

When Docker compose (16G web + 16G DB) does not fit:

```sh
# Server A: cache.api.etag.enabled=on
INSTANCE=http://127.0.0.1:8280 SIDE=on  PROFILE=smoke FAST=true APP_CYCLES=5 \
  ./scripts/etag-ab-live.sh

# Restart server with cache.api.etag.enabled=off
INSTANCE=http://127.0.0.1:8280 SIDE=off PROFILE=smoke FAST=true APP_CYCLES=5 \
  ./scripts/etag-ab-live.sh
```

Compare Gatling console globals or HTML reports under `target/gatling/`.

---

## Run 2026-07-10d: minibox **CPU flamegraphs** (load + async-profiler)

### Environment

| Item | Value |
|---|---|
| Same image/stack as 2026-07-10c | `dhis2/core-pr:etag-ab-local` @ `52d8dabe6b1`, stock 10G heap |
| Profile | load, `FAST=false`, `WARMUP=1 MEASURED=1` |
| Profiler | `PROF_ARGS="-e cpu"` via `docker-compose.profile.yml` (async-profiler 4.x in container) |
| Also captured | `pgbadger.html`, `simulation.csv`, `gc.log` |
| Stamp | `target/etag-ab/20260710T102642Z` |
| Wall clock | ~18 minutes |
| Host RAM headroom | **not recorded at run time** (P3-13; unlike 2026-07-10b/c, no free-RAM row was captured for this stamp) |

### Latency (sanity, matches prior load run)

| Side | measured | mean | p50 | p95 | rps |
|---|---|---:|---:|---:|---:|
| OFF | m2 | 11 | 6 | **34** | 22.8 |
| ON | m2 | 6 | 5 | **8** | 23.2 |

### Flamegraph artifacts (open in a browser)

| Side | `profile.html` (on minibox under `dhis-test-performance/`) |
|---|---|
| OFF measured | `target/gatling/pageloadsimulation-20260710103140402-etag-off-m2/profile.html` |
| ON measured | `target/gatling/pageloadsimulation-20260710104028263-etag-on-m2/profile.html` |

Also: `profile.jfr`, `profile.collapsed` in the same dirs.

**How to read:** OFF flamegraph should spend relatively more samples under controller/service/Hibernate for the page-load paths. ON should show a larger share of short-circuit 304 work and less deep controller trees. ETag interceptor/observer frames should be visible but not dominate CPU.

Reproduce:

```sh
PROF_ARGS="-e cpu" CAPTURE_SQL=1 \
DHIS2_IMAGE=dhis2/core-pr:etag-ab-local \
PROFILE=load WARMUP=1 MEASURED=1 FAST=false \
./scripts/etag-ab-benchmark.sh
```

---

## Run 2026-07-10c: minibox **full Docker load** profile (stock stack + pgbadger + glog)

### Environment

| Item | Value |
|---|---|
| Host | `minibox` (Fedora, 16 cores, 27 GiB) |
| Available RAM | ~21 GiB at start; no OOM |
| Image | `dhis2/core-pr:etag-ab-local` @ **`52d8dabe6b1`** |
| Stack | stock compose (10G heap, SL `dev` postgres image) |
| Profile | **`load`**: 10 concurrent users, 15s ramp, 180s steady, `FAST=false` (realistic think-time) |
| Protocol | `WARMUP=1 MEASURED=2`, `CAPTURE_SQL=1` |
| Tools | **pgbadger** (docker wrapper `~/bin/pgbadger` → `local/pgbadger:13`), **glog** (`~/bin/glog` → glog-0.0.2) |
| Stamp | `target/etag-ab/20260710T084534Z` |
| Wall clock | ~26 minutes |

### Canonical headline (pooled glog samples, measured m2+m3 only)

**Method:** concatenate all OK request latencies from glog `simulation.csv` for measured runs m2 and m3, then compute mean / p50 / p95 on the combined sample. This is the canonical aggregation going forward (not the average of per-run Gatling console percentiles).  
**Stamp:** `20260710T084534Z` · **Reconciled:** 2026-07-11 (supersedes 2026-07-10 hand-averaged 32 / 7.5 ms / −77%).

| Side | n req (pooled) | mean | p50 | **p95** |
|---|---:|---:|---:|---:|
| OFF | 9 891 | 9.93 ms | 6.0 | **36.0** |
| ON | 10 102 | 5.31 ms | 4.0 | **7.0** |
| Delta ON vs OFF | | −46% | −33% | **−81%** |

With 10 users and realistic think-time, ETag **ON** cuts pooled p95 by about **four fifths** (−81%). Throughput is similar (think-time limited); the win is latency and SQL volume, not max RPS.

### Per-run Gatling console percentiles (not the canonical headline)

These are what Gatling printed per run. They differ slightly from glog pooled percentiles (console OFF p95 31/33 vs glog 36.0). Keep for audit; do not average them into the headline.

| Side | Run | n req | mean | p50 | **p95** | p99 | rps |
|---|---|---:|---:|---:|---:|---:|---:|
| OFF | warmup1 | 4954 | 10 | 5 | 31 | 87 | 23.4 |
| OFF | **m2** | 4855 | **10** | **6** | **31** | 84 | 23.0 |
| OFF | **m3** | 5036 | **10** | **6** | **33** | 85 | 23.4 |
| ON | warmup1 | 4951 | 6 | 4 | 8 | 40 | 23.6 |
| ON | **m2** | 5051 | **5** | **4** | **7** | 21 | 23.4 |
| ON | **m3** | 5051 | **5** | **4** | **8** | 38 | 23.8 |

Per-run console average of measured p95 only (historical, non-canonical): OFF ~32 ms, ON ~7.5 ms, about −77%.

### SQL volume (postgresql.log size / duration-statement lines)

| Side | Run | log lines | duration/statement hits | log size | unique normalized queries (pgbadger scrape) |
|---|---|---:|---:|---:|---:|
| OFF | m2 | 98,914 | 50,719 | 58 MB | ~25.9k |
| OFF | m3 | 110,848 | 56,967 | 66 MB | ~29.1k |
| ON | m2 | 22,868 | 12,006 | **8.3 MB** | ~6.5k |
| ON | m3 | 33,218 | 17,339 | **15 MB** | ~9.1k |

Roughly **~3–4× fewer** SQL log events with ETag ON (304s skip controller/DB work). Open `pgbadger.html` for the full statement breakdown.

### Artifacts

Measured runs produced `postgresql.log`, **`pgbadger.html`**, **`simulation.csv`** (glog), `gc.log`.

Example ON m3 on minibox:

`target/gatling/pageloadsimulation-20260710090752268-etag-on-m3/{index.html,pgbadger.html,simulation.csv,gc.log}`

---

## Run 2026-07-10b: minibox **full Docker** smoke (stock `run-simulation.sh`)

### Environment

| Item | Value |
|---|---|
| Host | `minibox` (Fedora, 16 cores, 27 GiB RAM) |
| Available RAM at start | **~21 GiB** (after stopping :8280 e2e server and postgres on 5432) |
| Min available during run | **~15.4 GiB** (no OOM; stock `-Xms10000m -Xmx10000m` held) |
| Image | `dhis2/core-pr:etag-ab-local` (jib local build) |
| Code under image | `api_cache_dml_wip` at **`52d8dabe6b1`** |
| Stack | stock `docker-compose.yml`: web 16G limit / 10G heap, PostGIS SL `dev` dump baked into `localhost/dhis2-postgres:14-3.5-sierra-leone-dev` |
| Conf | `docker/dhis-etag-off.conf` vs `docker/dhis-etag-on.conf` via `DHIS_CONF_FILE` |
| Protocol | `scripts/etag-ab-benchmark.sh` → `PROFILE=smoke WARMUP=1 MEASURED=2 FAST=true CAPTURE_SQL=1` |
| Stamp | `target/etag-ab/20260710T082736Z` |
| Wall clock | ~11 minutes (6 full stack cycles: 3 OFF + 3 ON) |

### Global HTTP response times (Gatling console; ignore warmups for A/B)

| Side | Run | n req | mean | p50 | p75 | p95 | p99 | max | rps |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|
| OFF | warmup1 | 1236 | 12 | 6 | 9 | 38 | 117 | 582 | 18.7 |
| OFF | **m2** | 1236 | **12** | **7** | **9** | **39** | 122 | 579 | 18.5 |
| OFF | **m3** | 1236 | **12** | **7** | **9** | **38** | 123 | 555 | 19.0 |
| ON | warmup1 | 1442* | 7 | 5 | 6 | 11 | 59 | 612 | 20.6 |
| ON | **m2** | 1236 | **8** | **5** | **6** | **12** | 69 | 531 | 20.3 |
| ON | **m3** | 1442* | **8** | **5** | **6** | **11** | 64 | 535 | 20.9 |

\* Fixed 60s smoke window: faster ON responses complete more app cycles → slightly higher request counts on some ON runs.

**Measured-only average (m2+m3):**

| Side | mean | p50 | p95 | rps |
|---|---:|---:|---:|---:|
| OFF | 12 ms | 7 | **38.5** | 18.7 |
| ON | **8 ms** | **5** | **11.5** | **20.6** |
| Delta ON vs OFF | −33% | −29% | **−70%** | +10% |

**Read as:** on the stock Docker perf stack + Sierra Leone `dev` DB, ETag **ON** cuts p95 by about **70%** vs OFF for this page-load mix, with two measured samples per side that agree within 1 ms on p95.

### SQL / tooling notes

- `CAPTURE_SQL=1` saved `postgresql.log` under each non-warmup Gatling dir.
- **pgbadger** and **glog** were not installed on minibox → no HTML SQL report / no CSV conversion yet.
- GC logs saved per run (`gc.log`).

### Artifact paths (minibox)

- Protocol meta: `dhis-test-performance/target/etag-ab/20260710T082736Z/`
- OFF measured: `target/gatling/pageloadsimulation-*-etag-off-m2`, `*-etag-off-m3`
- ON measured: `target/gatling/pageloadsimulation-*-etag-on-m2`, `*-etag-on-m3`
- Console: `/tmp/etag-ab-full-smoke.log`

---

## Run 2026-07-10: minibox smoke (live instance, earlier)

### Environment

| Item | Value |
|---|---|
| Host | `minibox` (Fedora, 16 cores, 27 GiB RAM) |
| Server | embedded WAR `dhis-web-server/target/dhis.war`, port **8280** |
| WAR / code tip under test | branch `api_cache_dml_wip` at **`52d8dabe6b1`** (minibox checkout; tip of origin later includes CI/cluster commits that do not change the ETag hot path) |
| DB | docker `dhis2-etag-e2e-db` PostGIS 16, Sierra Leone–class demo data used by the e2e home |
| `DHIS2_HOME` | `/home/netroms/dhis2-etag-e2e` |
| Profile | `smoke`, `fast=true`, `appCycles=5`, `concurrentUsers=1` (default smoke duration 60s) |
| Client | Gatling 3.14.3 via `dhis-test-performance` module |
| Method | `scripts/etag-ab-live.sh`; OFF side after restart with `cache.api.etag.enabled=off` (probe: no ETag on `/api/me`); ON restored after the run |

### Global HTTP response times (Gatling console)

Same request volume both sides: **1,236 OK / 0 KO**.

| Side | `cache.api.etag.enabled` | mean (ms) | p50 | p75 | p95 | p99 | max | mean rps |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| **OFF** | `off` | 14 | 10 | 13 | **41** | 93 | 507 | 17.91 |
| **ON** | `on` | **9** | **8** | **9** | **14** | **61** | 353 | **19.62** |
| Delta ON vs OFF | | −36% | −20% | −31% | **−66%** | −34% | −30% | +10% |

**Read as:** with this page-load mix, enabling the ETag cache cut p95 latency by about two thirds and raised throughput ~10% on a single virtual user smoke run. Controllers still run on first-cycle misses; later cycles benefit from 304s when the client revalidates (manual curl confirmed 304 on `/api/me`, `/api/systemSettings`, `/api/organisationUnits`, etc. while ON).

### What this run does **not** prove yet

| Gap | Notes |
|---|---|
| Multi-run variance | Single measured sample per side; re-run `MEASURED=3+` for CI-quality means |
| Isolated miss-path tax | Smoke mixes cycle-1 200s with later 304s; use `APP_CYCLES=1` both sides for miss tax (target p95 delta within noise / &lt; 1 ms aspirational) |
| Isolated hit-path only | Filter or report status-code splits once `glog`/status CSV is available on the runner |
| Write / import tax | Not measured here (metadata or tracker import A/B after exclusions fix) |
| SQL statement drop | Needs `CAPTURE_SQL_LOGS` + pgbadger on the Docker path |
| Flamegraphs | Needs `PROF_ARGS="-e cpu"` on Docker path |
| Full `run-simulation.sh` stack | Minibox did not have ~32 GiB free for compose limits; use CI self-hosted perf runner or a larger box |

### Artifact paths (minibox)

- ON: `dhis-test-performance/target/gatling/pageloadsimulation-20260710020103109/`
- OFF: `dhis-test-performance/target/gatling/pageloadsimulation-20260710020301327/`
- Console logs: `/tmp/etag-ab-on.log`, `/tmp/etag-ab-off.log`

---

## Next measurements (still open)

1. **Miss path:** `APP_CYCLES=1` ON vs OFF, same profile, report p95 delta.  
2. **Load profile:** `PROFILE=load` on a machine that can run `run-simulation.sh` (or live with higher users).  
3. **Import tax:** tracker + metadata import timings with observer on vs off.  
4. **Commit CI recipe:** document dispatch of `performance-tests-compare.yml` with ON/OFF images or confs once an image build is cheap enough.

---

## Related scripts

| Path | Role |
|---|---|
| `scripts/etag-ab-benchmark.sh` | Full Docker A/B via `run-simulation.sh` + `dhis-etag-{on,off}.conf` |
| `scripts/etag-ab-live.sh` | Live-instance A/B (minibox / laptop) |
| `docker/dhis-etag-on.conf` / `docker/dhis-etag-off.conf` | Conf toggles for compose |
