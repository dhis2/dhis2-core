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

To build using a custom tag run

```sh
mvn -DskipTests -Dmaven.test.skip=true -f dhis-2/dhis-web/dhis-web-portal/pom.xml jib:dockerBuild -Djib.to-image-tag=custom-tag
```

To deploy DHIS2 under a different context then root (`/`) configure the context path by setting the
environment variable

`JAVA_TOOL_OPTIONS: "-Dcontext.path='/dhis2'"`

DHIS2 should be available at `http://localhost:8080/dhis2`.
