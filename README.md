# DHIS 2

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=dhis2_dhis2-core&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=dhis2_dhis2-core)
[![Tests](https://github.com/dhis2/dhis2-core/actions/workflows/run-tests.yml/badge.svg)](https://github.com/dhis2/dhis2-core/actions/workflows/run-tests.yml)
[![API tests](https://github.com/dhis2/dhis2-core/actions/workflows/run-api-tests.yml/badge.svg)](https://github.com/dhis2/dhis2-core/actions/workflows/run-api-tests.yml)

DHIS 2 is a flexible information system for data capture, management, validation, analytics and visualization. It allows for data capture through clients ranging from Web browsers, Android devices, Java feature phones and SMS. DHIS 2 features data visualization apps for dashboards, pivot tables, charting and GIS. It provides metadata management and configuration. The data model and services are exposed through a RESTful Web API.

## Overview

Issues can be reported and browsed in [JIRA](https://jira.dhis2.org).

For documentation visit the [documentation portal](https://docs.dhis2.org/).

You can download pre-built WAR files from the [continuous integration server](https://ci.dhis2.org/).

You can explore various demos of DHIS 2 in the [play environment](https://play.dhis2.org/).

For support and discussions visit the [community forum](https://community.dhis2.org/).

For general info visit the [project web page](https://www.dhis2.org/).

For software developer resources visit the [developer portal](https://developers.dhis2.org/).

To contribute to the software read the [contributor guidelines](https://developers.dhis2.org/community/contribute).

The software is open source and released under the [BSD license](https://opensource.org/licenses/BSD-2-Clause).

## Run DHIS2 in Docker

The following guides use [Docker Compose](https://docs.docker.com/compose/install/) to run DHIS2
using Docker.

A DB dump is downloaded automatically the first time you start DHIS2. If you switch between
different DHIS2 versions and/or need to download a different DB dump you will need to remove the
shared volume `db-dump` using

```sh
docker compose down --volumes
```

### Pre-built Image

We push pre-built DHIS2 Docker images to Dockerhub. You can pick an `<image name>` from one of the following
repositories

* releases or release candidates [dhis2/core](https://hub.docker.com/r/dhis2/core/tags)
* development (branches master and the previous 3 supported major versions) [dhis2/core-dev](https://hub.docker.com/r/dhis2/core-dev/tags)
* PRs labeled with `publish-docker-image` [dhis2/core-pr](https://hub.docker.com/r/dhis2/core-pr/tags)

To run DHIS2 from latest master (as it is on GitHub) run

```sh
DHIS2_IMAGE=dhis2/core-dev:master docker compose up
```

### Local Image

Build a DHIS2 Docker image first as described in [Docker image](#docker-image). Then execute

```sh
docker compose up
```

DHIS2 should become available at `http://localhost:8080` with the Sierra Leone Demo DB.

### Demo DB

If you want to start DHIS2 with a specific demo DB you can pass a URL like

```sh
DHIS2_DB_DUMP_URL=https://databases.dhis2.org/sierra-leone/2.39/dhis2-db-sierra-leone.sql.gz docker compose up
```

using versions we for example publish to https://databases.dhis2.org/

## Build process

This repository contains the source code for the server-side component of DHIS 2, which is developed in [Java](https://www.java.com/en/) and built with [Maven](https://maven.apache.org/). 

To build it you must first install the root `POM` file, navigate to the `dhis-web` directory and then build the web `POM` file.

See the [contributing](https://github.com/dhis2/dhis2-core/blob/master/CONTRIBUTING.md) page to learn how to run locally.

### Docker image

The DHIS2 Docker image is built using
[Jib](https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin). To build make sure
to build DHIS2 and the web project first

```sh
mvn clean install --threads 2C -DskipTests -Dmaven.test.skip=true -f dhis-2/pom.xml -pl -dhis-web-embedded-jetty,-dhis-test-integration,-dhis-test-coverage
mvn clean install --threads 2C -DskipTests -Dmaven.test.skip=true -f dhis-2/dhis-web/pom.xml
```

Then build the Docker image

```sh
mvn -DskipTests -Dmaven.test.skip=true -f dhis-2/dhis-web/dhis-web-portal/pom.xml jib:dockerBuild
```

Run the image using

```sh
docker compose up
```

It should now be available at `http://localhost:8080`.

#### Customizations

##### Docker tag

To build using a custom tag run

```sh
mvn -DskipTests -Dmaven.test.skip=true -f dhis-2/dhis-web/dhis-web-portal/pom.xml jib:dockerBuild -Djib.to.image=dhis2/core-dev:mytag
```

For more configuration options related to Jib or Docker go to the
[Jib documentation](https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin).

##### Context path

To deploy DHIS2 under a different context then root (`/`) configure the context path by setting the
environment variable

`CATALINA_OPTS: "-Dcontext.path='/dhis2'"`

DHIS2 should be available at `http://localhost:8080/dhis2`.

##### Overriding default values

You can create a local file called `docker-compose.override.yml` and override values from the main `docker-compose.yml`
file. As an example, you might want to use a different version of the Postgres database and run it on a different port.
More extensive documentation of this feature is available [here](https://docs.docker.com/compose/extends/). Using the override
file you can easily customize values for your local situation.

```yaml
version: "3.8"

services:
  db:
    image: postgis/postgis:14-3.3-alpine
    ports:
      - 127.0.0.1:6432:5432
```

##### DHIS2_HOME

[Previously](https://github.com/dhis2/dhis2-core/blob/b4d4242fb30d974254de2a72b86cc5511f70c9c0/docker/tomcat-debian/Dockerfile#L9),
the Docker image was built with environment variable `DHIS2_HOME` set to `/DHIS2_home`. This is not
the case anymore, instead `DHIS2_HOME` will [fallback to its default](https://github.com/dhis2/dhis2-core/blob/b4d4242fb30d974254de2a72b86cc5511f70c9c0/dhis-2/dhis-support/dhis-support-external/src/main/java/org/hisp/dhis/external/location/DefaultLocationManager.java#L58)
`/opt/dhis2`. You can still run the Docker image with the old behavior by setting the environment
variable `DHIS2_HOME` like

```yaml
    environment:
      DHIS2_HOME: /DHIS2_home
```

in a `docker-compose.override.yml` file. Alternatively, you can pass the system property `-Ddhis2.home` directly from the command line. You need to ensure that this `DHIS2_HOME` is writeable yourself!
