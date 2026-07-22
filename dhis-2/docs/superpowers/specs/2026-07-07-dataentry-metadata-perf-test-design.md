# DataEntryMetadataPerformanceTest design

## Goal

Add a focused Gatling performance test for `GET /api/dataEntry/metadata`, then use it to compare
`fix/hibernate-fetch-size` against `master` on the Sierra Leone (SL) demo DB, which has a richer
metadata set than the default local dev DB. The endpoint takes no parameters, so there isn't
currently a way to isolate its cost in the existing test suite.

## Endpoint under test

`DataSetMetadataController.getMetadata` (`/api/dataEntry/metadata`), backed by
`DefaultDataSetMetadataExportService.getDataSetMetadata()`. Confirmed no server-side result
caching — every request rebuilds the payload from the DB (datasets, data elements, indicators,
category combos/categories/options, option sets) unless the client sends a matching
`If-None-Match`, which handles the 304 path via `ResponseEntityUtils.withEtagCaching`. Since the
test never sends that header, every iteration is a genuine cold fetch — no cache-busting needed.

This is also why it's a relevant test for `fix/hibernate-fetch-size`: the payload assembly pulls
in several related collections per data set (elements, sections, category combos → categories →
options), which is exactly the shape of query Hibernate's batch-fetch/collection-fetch settings
affect.

## Test design

New file: `dhis-test-performance/src/test/java/org/hisp/dhis/test/platform/DataEntryMetadataPerformanceTest.java`

Follows the `platform` package conventions, closest to `DataIntegrityPerformanceTest`'s
single-user-baseline style:

- Configurable via `-DconfigFile=<props>` or individual `-D` flags:
  - `baseUrl` (default `http://localhost:8080`)
  - `username` / `password` (default `admin` / `district`)
  - `iterations` (default `5`)
- No login chain, no `MetadataImporter` setup — the endpoint only needs auth; payload richness
  comes from the SL demo DB itself (`DB_TYPE=sierra-leone`, `run-simulation.sh`'s default), not
  from anything the test imports.
- `HttpProtocolBuilder`: `basicAuth(username, password)`, `warmUp(baseUrl + "/api/ping")`,
  `disableCaching()`.
- Single scenario: `rampConcurrentUsers(0).to(1).during(1)`, `repeat(ITERATIONS)` issuing
  `GET /api/dataEntry/metadata`, checking `status().is(200)`.
- Assertions: p95/max response time thresholds + 100% success rate. Thresholds start as
  placeholders and are set from a real measured baseline (see below) before the test is
  considered final.

### Docs

Add a `### DataEntryMetadataPerformanceTest` subsection to `dhis-test-performance/README.md` under
"Platform Tests", matching the existing entries' format (description, run command, properties
table).

## Branch comparison workflow

Goal: get comparable timings for this endpoint on `fix/hibernate-fetch-size` vs `master`, both
against the SL demo DB, to validate the fetch-size fix doesn't regress (or ideally improves) this
endpoint.

Since neither branch has a published Docker tag, build local images from source for both branches
using `build-dev.sh` (which tags `dhis2/core-dev:local` by default, overridable via `IMAGE=`):

1. Build an image for the current branch (`fix/hibernate-fetch-size`):
   `IMAGE=dhis2/core-dev:hibernate-fetch-size ./dhis-2/build-dev.sh`
2. Build an image for `master` from a separate git worktree (keeps the current working tree
   untouched): `IMAGE=dhis2/core-dev:master-baseline ./dhis-2/build-dev.sh` run from the worktree.
3. Run the new simulation against each image via `run-simulation.sh` (default `DB_TYPE=sierra-leone`):
   ```sh
   DHIS2_IMAGE=dhis2/core-dev:hibernate-fetch-size \
   SIMULATION_CLASS=org.hisp.dhis.test.platform.DataEntryMetadataPerformanceTest \
   ./run-simulation.sh
   ```
   and again with `DHIS2_IMAGE=dhis2/core-dev:master-baseline`.
4. Compare the two `target/gatling/<sim>-<timestamp>/` results with `gstat compare` (per README),
   or read percentiles directly off each run's `index.html` if `gstat` isn't installed.
5. Use whichever numbers come out of this comparison (the worse of the two, with headroom) to set
   the final assertion thresholds in the test — so the checked-in test reflects a real
   double-checked baseline rather than a guess.

## Out of scope

- No changes to the endpoint or service code itself — this is a test-only + measurement task.
- No CI workflow wiring (`performance-tests-compare.yml`) — this is an ad hoc local comparison to
  inform the fetch-size PR, not a permanent CI gate. Can be revisited later if the team wants this
  endpoint tracked continuously.
