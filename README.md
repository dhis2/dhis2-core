# DHIS2

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=dhis2_dhis2-core&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=dhis2_dhis2-core)
[![Tests](https://github.com/dhis2/dhis2-core/actions/workflows/run-tests.yml/badge.svg)](https://github.com/dhis2/dhis2-core/actions/workflows/run-tests.yml)
[![API tests](https://github.com/dhis2/dhis2-core/actions/workflows/run-api-tests.yml/badge.svg)](https://github.com/dhis2/dhis2-core/actions/workflows/run-api-tests.yml)

DHIS2 is a flexible information system for data capture, management, validation, analytics and visualization. It allows for data capture through clients ranging from Web browsers, Android devices, Java feature phones and SMS. DHIS2 features data visualization apps for dashboards, pivot tables, charting and GIS. It provides metadata management and configuration. The data model and services are exposed through a RESTful Web API.

## Overview

Issues can be reported and browsed in [JIRA](https://jira.dhis2.org).

For documentation visit the [documentation portal](https://docs.dhis2.org/).

You can download pre-built WAR files from the [release site](https://releases.dhis2.org/).

You can explore various demos in the [play environment](https://play.dhis2.org/).

For support and discussions visit the [community forum](https://community.dhis2.org/).

For general info visit the [project web page](https://www.dhis2.org/).

For OpenAPI documentation visit the [Stoplight workspace](https://dhis2.stoplight.io/).

For software developer resources visit the [developer portal](https://developers.dhis2.org/).

To contribute to the software read the [contributor guidelines](https://developers.dhis2.org/community/contribute).

The software is open source and released under the [BSD 3-Clause license](https://opensource.org/license/bsd-3-clause).

## Run DHIS2 in Docker

The following guides runs DHIS2 with [Docker Compose](https://docs.docker.com/compose/install/).

Our Docker Compose file depends on various environment variables. See .env.example for a list of these. You can copy the file to .env and set the variables there.

A database dump is downloaded automatically the first time you start DHIS2. If you switch between different DHIS2 versions or need to download a different DB dump, you will need to remove the shared volume `db-dump` with the following command.

```sh
docker compose down --volumes
```

### Pre-built images

We push pre-built DHIS2 Docker images to Dockerhub. You can pick an `<image name>` from one of the following
repositories:

* [`dhis2/core`](https://hub.docker.com/r/dhis2/core) - images of the release and release-candidate DHIS2 versions. These images represent the stable DHIS2 versions, meaning they won't be rebuilt in the future.

* [`dhis2/core-dev`](https://hub.docker.com/r/dhis2/core-dev) - images of _the latest development_ DHIS2 versions - branches `master` (tagged as `latest`) and the previous 3 supported major versions. Image tags in this repository will be overwritten multiple times a day.

* [`dhis2/core-canary`](https://hub.docker.com/r/dhis2/core-canary) - images of _the latest daily development_ DHIS2 versions. We tag the last `core-dev` images for the day and add an extra tag with a "yyyyMMdd"-formatted date, like `core-canary:latest-20230124`.

* [`dhis2/core-pr`](https://hub.docker.com/r/dhis2/core-pr) - images of PRs made from [dhis2-core](https://github.com/dhis2/dhis2-core/) and not from forks, as forks do not have access to our organizational secrets.

To run DHIS2 from latest `master` branch (as it is on GitHub) run the command below.

```sh
DHIS2_IMAGE=dhis2/core-dev:latest docker compose up
```

### Local image

Build a DHIS2 Docker image as described in [Docker image](#docker-image) and execute the following command.

```sh
docker compose up
```

DHIS2 will become available at `http://localhost:8080` with the Sierra Leone Demo DB.

### Demo DB

If you want to start DHIS2 with a specific demo DB you can pass a URL like the below.

```sh
DHIS2_DB_DUMP_URL=https://databases.dhis2.org/sierra-leone/2.39/dhis2-db-sierra-leone.sql.gz \
docker compose up
```

### Launch with Apache Doris

```sh
DHIS2_IMAGE=dhis2/core-dev:local DORIS_VERSION=3.0.4 \
docker compose -f docker-compose.yml -f docker-compose.doris.yml up
```

When running compose up with multiple compose files, you need to pass the same files to compose down, e.g.

```
docker compose -f docker-compose.yml -f docker-compose.doris.yml down
```

### Synchronization between DHIS2 instances

You can run multiple DHIS2 instances to test data and metadata [synchronization](https://docs.dhis2.org/en/use/user-guides/dhis-core-version-master/exchanging-data/metadata-synchronization.html) by running the following command.

```sh
docker compose --profile sync up
```

After that follow the [guide](https://github.com/dhis2/wow-backend/blob/master/guides/testing/metadata_sync_testing.md).

## Build process

This repository contains the source code for the server-side component of DHIS2, written in [Java](https://www.java.com/en/) and built with [Maven](https://maven.apache.org/). See the [contributing](./CONTRIBUTING.md) page to learn how to run the software locally.

### Docker image

The DHIS2 Docker image is built using [Jib](https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin). Start by building DHIS2 by executing the script below.

```sh
./dhis-2/build-dev.sh
```

Start the image.

```sh
docker compose up
```

DHIS2 should now be available at `http://localhost:8080`.

#### Customizations

##### Docker tag

To build using a custom tag.

```sh
mvn clean package -DskipTests -Dmaven.test.skip=true --file dhis-2/pom.xml \
  --projects dhis-web-server --also-make --activate-profiles jibDockerBuild \
  -Djib.to.image=dhis2/core-dev:mytag
```

For more configuration options related to Jib or Docker go to the [Jib documentation](https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin).

##### Context path

To deploy DHIS2 under a different context then root (`/`) configure the context path by setting the environment variable.

`CATALINA_OPTS: "-Dcontext.path='/dhis2'"`

DHIS2 should be available at `http://localhost:8080/dhis2`.

##### Overriding default values

You can create a local file called `docker-compose.override.yml` and override values from the main `docker-compose.yml` file. As an example, you might want to use a different version of the Postgres database and run it on a different port.

More extensive documentation of this feature is available at the Docker web [pages](https://docs.docker.com/compose/extends/). Using the override file you can customize values for your local environment.

```yaml
version: "3.8"

services:
  db:
    image: postgis/postgis:14-3.3-alpine
    ports:
      - 127.0.0.1:6432:5432
```

##### DHIS2_HOME

Previously, the Docker image was built with environment variable `DHIS2_HOME` set to `/DHIS2_home`. This is not the case anymore, instead `DHIS2_HOME` will fallback to `/opt/dhis2`. You can still run the Docker image with the old behavior by setting the environment variable `DHIS2_HOME` in a `docker-compose.override.yml` file.

```yaml
environment:
  DHIS2_HOME: /DHIS2_home
```

Alternatively, you can pass the system property `-Ddhis2.home` from the command line. You need to ensure that the `DHIS2_HOME` directory is writeable by the DHIS2 process.
