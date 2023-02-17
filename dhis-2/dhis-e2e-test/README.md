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
    -Dinstance.url=http://localhost:8080/api
    -Dtest.cleanup=true
    -Duser.default.username=admin
    -Duser.default.password=district
    -Dtest.track_called_endpoints=true
```

### Inside Docker

The following describes 2 options for you to run and test DHIS2 using Docker. Refer to [Run DHIS2 in
Docker](../../README.md#run-dhis2-in-docker) on how to build or pick a Docker image.

#### Only DHIS2 In Docker

If you only want to run DHIS2 in Docker but the tests outside of Docker do

```sh
docker compose up --detach
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
docker compose -f docker-compose.yml -f docker-compose.e2e.yml up --exit-code-from test
```

Note: running everything outside of Docker will be faster. Option 1. has the advantage of running
e2e tests against an image that was built in our pipelines. Option 2. is mostly used within our
pipelines so we can run a matrix of versions in an isolated fashion. When developing tests locally
option 2. will be slow as you will need to not only recompile but also rebuild the test Docker
image.

### Configuration

The following show some test configurations you can adjust. All properties should be defined in
config.properties file (src/main/resources). Properties can be overwritten by system or environment
variables.

### Required properties

  - `instance.url` - points to the DHIS2 instance that should be tested

    Example: https://play.dhis2.org/dev/api

## Other properties

  - `user.super.username` - super user username to use when running tests. Default: user created during test run
  - `user.super.password` - super user password to use when running tests. Default: user created during test run
  - `user.default.username` - user to use for preconditions, like setting up metadata used in tests. Default: `admin`
  - `user.default.password` - user to use for preconditions, like setting up metadata used in tests. Default: `district`

### Test clean up

After every test class, created data will be cleaned up starting from latest created object to avoid as much references as possible.

If more controlled cleanup order is required - it can be explicitly specified. Just call one of the methods in TestCleanUp class.
*Example: testCleanUp.deleteCreatedEntities("/users", "/dataElements")*

## Debugging Tests

### Failing On CI

If e2e tests fail on GitHub/Jenkins a few things might help figuring out whats wrong.

#### Logs

Logs can be retrieved from both `web` and `db` containers. Like so

```sh
docker compose logs web
```

Check our pipeline code for where the logs are uploaded. We usually store them as pipeline
artifacts.

#### Running Tests Locally

We build/push the DHIS2 Docker image of PRs to [Dockerhub
dhis2/core-pr](https://hub.docker.com/r/dhis2/core-pr/tags).

To run PR [12065](https://github.com/dhis2/dhis2-core/pull/12065) locally you need to run

```sh
DHIS2_IMAGE=dhis2/core-pr:12065 docker compose -f docker-compose.yml -f docker-compose.e2e.yml up --exit-code-from test
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

Note: if you run the tests inside of a Docker container you will need to make sure to either write
the file to a mounted volume or copy it out of the container. Otherwise it will be deleted with the
container.
 
## Writing Tests

### Actions
 
For convenience, every REST endpoint should be represented by object of type RestApiActions. RestApiActions class will provide way of sending different types of requests and will control keeping track of created or deleted data.
 
 *Examples*: 
 1) endpoint that doesn't require any specific actions:
 
 > private RestApiActions optionSetActions = new RestApiActions("/optionSets");

