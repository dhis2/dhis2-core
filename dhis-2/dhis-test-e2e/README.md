# DHIS2 E2E Tests

## Tech

DHIS2 e2e tests are written and executed using the following libraries and tools

 - [REST-assured](http://rest-assured.io) - provides rest client, handles logging and validation
 - [JUnit 5](https://junit.org/junit5/) - runs the e2e tests
 - [Docker Compose](https://docs.docker.com/compose/) - defines and runs DHIS2 multi-container environment

## Running Tests

All commands in this README assume you are inside the `cd dhis2-e2e-test` directory.

Assuming you have DHIS2 running locally on port `8080` run

```sh
mvn test \
    -Dinstance.url=http://localhost:8080/api \
    -Dtest.cleanup=true \
    -Duser.default.username=admin \
    -Duser.default.password=district \
    -Dtest.track_called_endpoints=true
```

### Selenium Grid and CPU architecture

When running docker compose locally on MacOS with M1/M2/M3..., you need to use the Selenium Grid image for ARM64 architecture. 
The image is `seleniarm/standalone-chromium`. The "default" image for x86 architecture is `selenium/standalone-chrome`.
Change the `SELENIUM_IMAGE` environment variable to use the ARM64 image.


### Inside Docker

The following describes 2 options for you to run and test DHIS2 using Docker. Refer to [Run DHIS2 in
Docker](../../README.md#run-dhis2-in-docker) on how to build or pick a Docker image.

#### Only DHIS2 In Docker

If you only want to run DHIS2 in Docker but the tests outside of Docker do

```sh
SELENIUM_IMAGE=selenium/standalone-chrome:latest docker compose up --detach
```

Note: `--detach` will run the containers in the background. If you can open a separate terminal
window, run it without `--detach` to immediately stream the logs. This way you can debug any issues
right away.

Get the exposed port of the DHIS2 `web` container using `docker compose port web 8080`. Adjust the
port of the `instance.url` and run the tests

```sh
mvn test \
    -Dinstance.url=http://localhost:49176/api
    -Dtest.cleanup=true
    -Duser.default.username=admin
    -Duser.default.password=district
    -Dtest.track_called_endpoints=true
```

#### DHIS2 And Tests In Docker

If you want to run both DHIS2 and the tests inside Docker

```sh
SELENIUM_IMAGE=selenium/standalone-chrome:latest docker compose -f docker-compose.yml -f docker-compose.e2e.yml up --exit-code-from test
```

Note: running everything outside of Docker will be faster. Option 1. has the advantage of running
e2e tests against an image that was built in our pipelines. Option 2. is mostly used within our
pipelines so we can run a matrix of versions in an isolated fashion. When developing tests locally
option 2. will be slow as you will need to not only recompile but also rebuild the test Docker
image.

##### Running analytics tests against Apache Doris

If you want to run the analytics tests against an Apache Doris backend, you can do this easily with docker:

```
# first link the required Doris files from the root of the repository
source ./setup_doris_links

# then run the compose files
docker compose -f docker-compose.yml -f docker-compose.doris.yml -f docker-compose.e2e.yml -f docker-compose.e2e-analytics.yml -f docker-compose.doris-analytics.yml up --exit-code-from test

# the alternative (without Doris backend) is
# docker compose -f docker-compose.yml -f docker-compose.e2e.yml -f docker-compose.e2e-analytics.yml up --exit-code-from test

```

### Configuration

The following show some test configurations you can adjust. All properties should be defined in
config.properties file (src/main/resources). Properties can be overwritten by system or environment
variables.

### Required properties

  - `instance.url` - points to the DHIS2 instance that should be tested

    Example: https://play.dhis2.org/dev/api

## Other properties

  - `user.super.username` - superuser username to use when running tests. Default: user created during test run
  - `user.super.password` - superuser password to use when running tests. Default: user created during test run
  - `user.default.username` - user to use for preconditions, like setting up metadata used in tests. Default: `admin`
  - `user.default.password` - user to use for preconditions, like setting up metadata used in tests. Default: `district`

### Test clean up

After every test class, created data will be cleaned up starting from latest created object to avoid as many references as possible.

If more controlled cleanup order is required - it can be explicitly specified. Just call one of the methods in TestCleanUp class.
*Example: testCleanUp.deleteCreatedEntities("/users", "/dataElements")*

## Debugging Tests

### Failing On CI

If e2e tests fail on GitHub/Jenkins a few things might help figuring out what's wrong.

#### Logs

Logs can be retrieved from both `web` and `db` containers. Like so

```sh
SELENIUM_IMAGE=selenium/standalone-chrome:latest docker compose logs web
```

Check our pipeline code for where the logs are uploaded. We usually store them as pipeline
artifacts.

#### Running Tests Locally

We build/push the DHIS2 Docker image of PRs to [Dockerhub
dhis2/core-pr](https://hub.docker.com/r/dhis2/core-pr/tags).

To run PR [12065](https://github.com/dhis2/dhis2-core/pull/12065) locally you need to run

```sh
SELENIUM_IMAGE=selenium/standalone-chrome:latest DHIS2_IMAGE=dhis2/core-pr:12065 docker compose -f docker-compose.yml -f docker-compose.e2e.yml up --exit-code-from test
```

Note that the PR number `12065` is the id at the end of the PR url https://github.com/dhis2/dhis2-core/pull/12065

Make sure that you have checked out the same branch as on the PR otherwise you might run different
e2e tests.

Refer to [Running Tests - Inside Docker](#inside_docker) for all the ways you can run the e2e tests
locally.

## Test coverage

When running tests with `-Dtest.track_called_endpoints=true` `coverage.csv` in the form of

    GET,/programs/ZShpl0NB7TL,1

will be written by the e2e tests. This file lists the tested HTTP method, url including query parameters and
how often the request was made.

Note: if you run the tests inside a Docker container you will need to make sure to either write
the file to a mounted volume or copy it out of the container. Otherwise, it will be deleted with the
container.

## Writing Tests

### Actions

For convenience, every REST endpoint should be represented by object of type RestApiActions. RestApiActions class will provide way of sending different types of requests and will control keeping track of created or deleted data.

 *Examples*:
 1) endpoint that doesn't require any specific actions:

 > private RestApiActions optionSetActions = new RestApiActions("/optionSets");

## Test with Selenium Grid (without docker compose) locally on MacOS (using Docker) native silicon M1/M2/M3... image

1. Start the DHIS2 server locally on port 8080

2. Start the Selenium Grid locally on port 4444 and 7900:

```
docker run -d \
-p 4444:4444 -p 7900:7900 \
--shm-size="2g" \
seleniarm/standalone-chromium
```

3. In the tests you must substitute the host URL with "http://host.docker.internal:8080/" instead
   of "http://localhost:8080/", since the DHIS2 server is running outside the container, on the host.

### Connecting to Selenium Grid VNC (for debugging)

http://localhost:7900/?autoconnect=1&resize=scale&password=secret

## Auto-generating analytics tests

We have the capability to auto-generate analytics e2e tests.
The class located at `src/test/java/org/hisp/dhis/analytics/generator/Main.java`
can be executed in order to generate e2e tests based on the URL(s)/queries present in `src/test/java/org/hisp/dhis/analytics/generator/scenarios`

There are a few different generators available. The usage of the correct one depends on the URL/API to be tested.
Based on the URL/API, the respective generator implementation should be set at `src/test/java/org/hisp/dhis/analytics/generator/TestGenerator.java`.
Currently, the supported generators are (along with their respective accepted URL format):

```
AnalyticsAggregatedGenerator.java -> /analytics?
EnrollmentQueryGenerator.java -> /analytics/enrollments/query/{program}.json?
EnrollmentAggregatedGenerator.java -> /analytics/enrollments/aggregate/{program}.json?
EventAggregatedGenerator.java -> /analytics/events/aggregate/{program}.json?
EventQueryGenerator.java -> /analytics/events/query/{program}.json?
TrackedEntityQueryGenerator.java -> /analytics/trackedEntities/query/{trackedEntityType}.json?
OutlierDetectionGenerator.java -> /analytics/outlierDetection?
```
_**NOTE**_: The `.json` extension in some URLs above. It's mandatory for all cases where we expect and `uid` of the respective entity/object.

### How to generate the test(s)
1. Add the URL(s) into the respective `<scenario-file>.json`
2. Define the generator implementation to use, in `TestGenerator.java`, and the scenario(s) to be tested
3. Go to the class `Main.java` and run it from your IDE
4. Check the generated file(s) in the folder `src/test/java/org/hisp/dhis/analytics/generator/output`

_**NOTE**_: You need to ensure that the URL(s) you have defined is pointing to a DHIS2 instance
that is up and running. The tests are based on the request/response of each URL. The server and user/password settings are defined in `src/main/resources/config.properties`.

**Important**: This generator only supports "happy" paths at the moment. In order to test validation
errors or invalid requests, one should implement them programmatically. Remember that the implementation of the generator
must match the URL(s)/queries format expected, and we can pick only one generator at time.

### Automated Test-Data Provisioning (`@DependsOn`)

The end-to-end test suite now offers a built-in mechanism for provisioning and cleaning up DHIS 2 metadata required by individual test cases.

#### Overview
`@DependsOn` is a JUnit 5 **method-level** annotation.  
A global extension processes it as follows:

1. **Create** each referenced resource if it is not already present.
2. **Execute** the test method.
3. **Delete** the resources it created when `delete = true`.

Any failure during those phases (missing file, malformed JSON, HTTP error, duplicate UID) aborts the test immediately.

#### Usage
```java
@DependsOn(files = { "pi-birth.json", "ind-valid.json" }, delete = true)
void analyticsProducesExpectedResult(List<Resource> deps) {

    String indicatorUid =
        deps.stream()
            .filter(cr -> cr.type() == ResourceType.INDICATOR)
            .findFirst()
            .orElseThrow()
            .uid();

    // Perform test logic that relies on the created Indicator UID …
}
```

#### Resource Files
* **Location**: `src/test/resources/dependencies/`
* **Format**: Regular DHIS 2 import JSON—**identical** to what you would POST to
    * `/api/29/programIndicators` (for `type = "program_indicator"`) or
    * `/api/29/indicators`      (for `type = "indicator"`).  
      In other words, you can copy/paste the payload you already use in Postman or Swagger.
* **Additional attribute**: `"type"` (values: `"program_indicator"` or `"indicator"`).  
  The extension removes this field before forwarding the payload to DHIS 2.

Example (`ind-expected-pregnancies.json`):
```json
{
  "type": "indicator",
  "code": "EXP_PREG1",
  "denominatorDescription":"Denominator",
  "numeratorDescription":"Expected Pregnancies with offset",
  "numerator":"subExpression(if(#{h0xKKjijTdI} - #{h0xKKjijTdI}.periodOffset(-1) > 0, 1, 0))",
  "denominator":"1",
  "name":"Expected Pregnancies",
  "shortName":"Expected Pregnancies",
  "indicatorType":{
    "id":"JkWynlWMjJR"
  },
  "legendSets":[]
}
```

#### Supported Types
| `type`              | DHIS 2 endpoint              |
|---------------------|------------------------------|
| `program_indicator` | `/api/programIndicators`     |
| `indicator`         | `/api/indicators`            |

#### Accessing Created UIDs
Add a `List<Resource>` (or a single `Resource`) parameter to your test method; 
the extension injects the created objects, each exposing `type`, `code`, and `uid`.

