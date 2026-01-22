Backport of https://github.com/dhis2/dhis2-core/pull/22796

## Performance Results

### 2.41 Comparison

**Note:** Assertions may fail for both baseline and candidate on 2.41 as thresholds are tuned for
master.

**Note:** Smoke test in 2.41 did not see much change as 2.41 did not have the N+1 query issues we
had in 2.42 and master (see https://github.com/dhis2/dhis2-core/pull/22747). The main change
applicable to the 2.41 backport is removing `@Transactional` from `DefaultTrackedEntityService`,
which should show improvements in the load test due to concurrent use.

#### /trackedEntities Collection Requests (P95 Response Time)

| Request | Smoke Baseline | Smoke Candidate | Smoke Improvement | Load Baseline | Load Candidate | Load Improvement |
|---------|----------------|-----------------|-------------------|---------------|----------------|------------------|
| Get first page of TEs of program ur1Edk5Oe2n | 80 | 83 | -4% | 63 | 63 | 0% |
| Get tracked entities from events | 9 | 9 | 0% | 7 | 7 | 0% |
| Not found TE by name with like operator | 18 | 19 | -6% | 16 | 16 | 0% |
| Not found TE by national id with eq operator | 46 | 48 | -4% | 43 | 43 | 0% |
| Search TE by name with like operator | 17 | 17 | 0% | 15 | 15 | 0% |
| Search TE by national id with eq operator | 53 | 53 | 0% | 50 | 50 | 0% |

#### Test Configuration

* **Smoke**: Single user, 100 iterations ([workflow run](https://github.com/dhis2/dhis2-core/actions/runs/21255016302))
* **Load**: 3 users/sec, 30s ramp, 3min sustained ([workflow run](https://github.com/dhis2/dhis2-core/actions/runs/21255793130))
* **Test data**: Sierra Leone 2.41.7 database with default HikariCP pool (80 connections)

```bash
# Smoke
gh workflow run performance-tests-compare.yml \
  --repo dhis2/dhis2-core \
  --field perf_tests_git_ref="aaa745aedb8c781e028b10f798b19f647a4b877b" \
  --field baseline_env="DHIS2_IMAGE=dhis2/core:2.41.7
SIMULATION_CLASS=org.hisp.dhis.test.tracker.TrackerTest
DB_VERSION=2.41.7" \
  --field candidate_env="DHIS2_IMAGE=dhis2/core-pr:22827
SIMULATION_CLASS=org.hisp.dhis.test.tracker.TrackerTest
DB_VERSION=2.41.7"

# Load
gh workflow run performance-tests-compare.yml \
  --repo dhis2/dhis2-core \
  --field perf_tests_git_ref="aaa745aedb8c781e028b10f798b19f647a4b877b" \
  --field baseline_env="DHIS2_IMAGE=dhis2/core:2.41.7
SIMULATION_CLASS=org.hisp.dhis.test.tracker.TrackerTest
DB_VERSION=2.41.7
MVN_ARGS='-Dprofile=load -DusersPerSec=3'" \
  --field candidate_env="DHIS2_IMAGE=dhis2/core-pr:22827
SIMULATION_CLASS=org.hisp.dhis.test.tracker.TrackerTest
DB_VERSION=2.41.7
MVN_ARGS='-Dprofile=load -DusersPerSec=3'"
```
