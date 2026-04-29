# DHIS2 Core

## Tracker

### Performance

A PR claiming a performance improvement must back it with evidence against a paired baseline,
produced on the shared perf-test runner (link the GitHub Actions run IDs in the PR body). Local
runs are for iterating, not for the claim. Pick the shape that matches the change:

* **Hot-path code change with no I/O** (serialization, parsing, in-memory transforms): JMH
  microbenchmark. Pattern to follow:
  [`FieldFilterSerializationBenchmarkTest`](dhis-2/dhis-test-web-api/src/test/java/org/hisp/dhis/webapi/controller/tracker/FieldFilterSerializationBenchmarkTest.java)
  from [PR #21761](https://github.com/dhis2/dhis2-core/pull/21761).
* **SQL / query change**: iterate locally with `EXPLAIN ANALYZE` and
  [`pgbench`](https://www.postgresql.org/docs/current/pgbench.html) on the before/after queries to
  confirm the direction (planning time, execution time, TPS, latency). This is for iteration
  speed, not the published claim. See [PR #22948](https://github.com/dhis2/dhis2-core/pull/22948)
  for the full pattern (pgbench results plus Gatling runs in one PR body).
* **End-to-end change** (request handling, DB queries, connection pooling, anything that only
  shows under load): Gatling baseline vs candidate via
  [`performance-tests-compare.yml`](.github/workflows/performance-tests-compare.yml), analyzed
  with [`gstat`](https://github.com/dhis2/gatling-statistics). Helpers in
  [`dhis-test-performance/scripts/`](dhis-2/dhis-test-performance/scripts/) trigger the run and
  turn a run ID into a `gstat compare` markdown table; paste it (p50/p95 with deltas) into the PR
  body. See the [perf module README](dhis-2/dhis-test-performance/README.md) for details.

#### Release notes

Each major release gets a performance write-up in `dhis2-releases`. The [2.43
notes](https://github.com/dhis2/dhis2-releases/blob/master/releases/2.43/tracker-performance.md) are
the reference shape: method section, per-version tables, p95 from `gstat`, and any non-obvious
metric (e.g. how req/s is computed for the simulation at hand) defined explicitly so readers can
reproduce it from `simulation.csv`.

