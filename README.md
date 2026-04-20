# DHIS2

DHIS2 is an open-source, flexible health information platform for data capture, management, validation, analytics, and visualization. It supports data collection across web browsers, Android devices, Java feature phones, and SMS. Analytics capabilities include dashboards, pivot tables, charts, and GIS maps. All data models and services are accessible via a RESTful Web API.

The software is released under the [BSD 3-Clause License](https://opensource.org/license/bsd-3-clause).

---

## Table of Contents

- [Resources](#resources)
- [Running DHIS2 with Docker](#running-dhis2-with-docker)
  - [Pre-built Images](#pre-built-images)
  - [Local Build](#local-build)
  - [Demo Database](#demo-database)
  - [Apache Doris](#apache-doris)
  - [Instance Synchronization](#instance-synchronization)
- [Build Process](#build-process)
  - [Docker Image](#docker-image)
  - [Customizations](#customizations)
- [Contributing](#contributing)

---

## Resources

| Resource | Link |
|---|---|
| Issue tracker | [JIRA](https://jira.dhis2.org) |
| Documentation | [docs.dhis2.org](https://docs.dhis2.org/) |
| Pre-built releases | [releases.dhis2.org](https://releases.dhis2.org/) |
| Live demo | [play.dhis2.org](https://play.dhis2.org/) |
| Community forum | [community.dhis2.org](https://community.dhis2.org/) |
| Project website | [dhis2.org](https://www.dhis2.org/) |
| OpenAPI docs | [Stoplight workspace](https://dhis2.stoplight.io/) |
| Developer portal | [developers.dhis2.org](https://developers.dhis2.org/) |
| Contributor guide | [How to contribute](https://developers.dhis2.org/community/contribute) |

---

## Running DHIS2 with Docker

> **Warning:** The Docker Compose setup in this repository is for local development only. For production deployments, use the [docker-deployment](https://github.com/dhis2/docker-deployment) repository.

Prerequisites: [Docker Compose](https://docs.docker.com/compose/install/)

Environment variables are required to run the Compose setup. Copy `.env.example` to `.env` and configure the variables before starting.

```sh
cp .env.example .env
```

A database dump is downloaded automatically on the first run. If you change the DHIS2 version or need a different database dump, remove the shared volume before restarting.

```sh
docker compose down --volumes
```

### Pre-built Images

Pre-built images are published to Docker Hub across four repositories:

| Repository | Purpose |
|---|---|
| [`dhis2/core`](https://hub.docker.com/r/dhis2/core) | Stable release and release-candidate images. Tags are immutable and will not be rebuilt. |
| [`dhis2/core-dev`](https://hub.docker.com/r/dhis2/core-dev) | Latest development builds from `master` (tagged `latest`) and the three previously supported major versions. Tags are overwritten multiple times per day. |
| [`dhis2/core-canary`](https://hub.docker.com/r/dhis2/core-canary) | Daily snapshots of `core-dev`. Each day's final image is tagged with a `yyyyMMdd` date suffix, e.g. `core-canary:latest-20230124`. |
| [`dhis2/core-pr`](https://hub.docker.com/r/dhis2/core-pr) | Images built from pull requests originating from the main repository. Fork-based PRs are excluded due to access restrictions on organization secrets. |

To start DHIS2 from the latest `master` build:

```sh
DHIS2_IMAGE=dhis2/core-dev:latest docker compose up
```

### Local Build

Build a local Docker image first (see [Docker Image](#docker-image)), then start the stack.

```sh
docker compose up
```

DHIS2 will be available at `http://localhost:8080` with the Sierra Leone demo database loaded.

### Demo Database

To start with a specific database dump, pass the URL as an environment variable.

```sh
DHIS2_DB_DUMP_URL=https://databases.dhis2.org/sierra-leone/2.39/dhis2-db-sierra-leone.sql.gz \
docker compose up
```

### Apache Doris

To run DHIS2 alongside Apache Doris, use the additional Compose file.

```sh
DHIS2_IMAGE=dhis2/core-dev:local DORIS_VERSION=3.0.4 \
docker compose -f docker-compose.yml -f docker-compose.doris.yml up
```

When using multiple Compose files, pass the same files to `down` as well.

```sh
docker compose -f docker-compose.yml -f docker-compose.doris.yml down
```

### Instance Synchronization

To run two DHIS2 instances for testing [metadata synchronization](https://docs.dhis2.org/en/use/user-guides/dhis-core-version-master/exchanging-data/metadata-synchronization.html), use the `sync` profile.

```sh
docker compose --profile sync up
```

After the instances are running, follow the [metadata sync testing guide](https://github.com/dhis2/wow-backend/blob/master/guides/testing/metadata_sync_testing.md).

---

## Build Process

The server-side component is written in Java and built with Maven. See [CONTRIBUTING.md](./CONTRIBUTING.md) for instructions on running the project locally.

### Docker Image

DHIS2 Docker images are built using [Jib](https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin).

Build the image using the provided script.

```sh
./dhis-2/build-dev.sh
```

Start the stack.

```sh
docker compose up
```

DHIS2 will be available at `http://localhost:8080`.

### Customizations

#### Custom Docker Tag

To build the image with a custom tag:

```sh
mvn clean package -DskipTests -Dmaven.test.skip=true --file dhis-2/pom.xml \
  --projects dhis-web-server --also-make --activate-profiles jibDockerBuild \
  -Djib.to.image=dhis2/core-dev:mytag
```

For additional Jib and Docker configuration options, refer to the [Jib documentation](https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin).

#### Custom Context Path

To deploy DHIS2 under a path other than `/`, set the following environment variable.

```sh
CATALINA_OPTS: "-Dcontext.path='/dhis2'"
```

DHIS2 will then be available at `http://localhost:8080/dhis2`.

#### Overriding Compose Defaults

Create a `docker-compose.override.yml` file to override values in the base `docker-compose.yml` without modifying it directly. This is the recommended approach for environment-specific configuration.

Example — using a different PostgreSQL version on a non-default port:

```yaml
version: "3.8"

services:
  db:
    image: postgis/postgis:14-3.3-alpine
    ports:
      - 127.0.0.1:6432:5432
```

Full documentation for Compose overrides is available at the [Docker docs](https://docs.docker.com/compose/extends/).

#### DHIS2_HOME

The default value for `DHIS2_HOME` is `/opt/dhis2`. If you require the previous default of `/DHIS2_home`, set the variable explicitly in your override file.

```yaml
environment:
  DHIS2_HOME: /DHIS2_home
```

Alternatively, pass it as a system property at startup.

```sh
-Ddhis2.home=/DHIS2_home
```

Ensure the configured directory is writable by the DHIS2 process.

---

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for the full contributor guide, including how to set up the development environment, run tests, and submit changes.
