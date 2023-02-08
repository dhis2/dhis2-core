[//]: # (Note: this is the source of truth for DHIS2 Docker images. Any changes to this document need to be manually synced to Dockerhub since we do not have a paid account.)

# Quick reference

-	**Maintained by**:  
	[the DHIS2 core team](https://github.com/dhis2/dhis2-core)

-	**Where to get help**:  
	[the DHIS2 Community of Practice - Tag docker](https://community.dhis2.org/tag/docker)

# Supported tags

* [`dhis2/core`](https://hub.docker.com/r/dhis2/core) - images of the release and release-candidate
  DHIS2 versions. These images represent the **stable** DHIS2 versions, meaning they won't be
  rebuilt in the future.

* [`dhis2/core-dev`](https://hub.docker.com/r/dhis2/core-dev) - images of _the latest development_
  DHIS2 versions - branches `master` (tagged as `latest`) and the previous 3 supported major
  versions. Image tags in this repository will be overwritten multiple times a day.

* [`dhis2/core-canary`](https://hub.docker.com/r/dhis2/core-canary) - images of _the latest daily
  development_ DHIS2 versions. We tag the last `core-dev` images for the day and add an extra tag
  with a "yyyyMMdd"-formatted date, like `core-canary:latest-20230124`.

* [`dhis2/core-pr`](https://hub.docker.com/r/dhis2/core-pr) - images of PRs made from
  https://github.com/dhis2/dhis2-core and not from forks. As forks do not have access to our
  organizations/repos secrets.

## Multi-architecture images

Multi-platform images for `linux/amd64` and `linux/arm64` are published starting with

* 2.39.1-rc
* the release after 2.38.2.0
* the release after 2.37.9

# What is DHIS2?

[DHIS2](https://dhis2.org/about) is an open source, web-based platform most commonly used as a
health management information system (HMIS). It allows for data capture through clients ranging from
web browsers, Android devices, Java feature phones and SMS. DHIS2 features data visualization apps
for dashboards, pivot tables, charting and GIS. It provides metadata management and configuration.
The data model and services are exposed through a RESTful Web API.

# How to use this image

## Using Docker Compose

The easiest way to get familiar with DHIS2 in Docker is to start running it locally using [Docker
Compose](https://docs.docker.com/compose/install/).

https://github.com/dhis2/dhis2-core/blob/master/docker-compose.yml has an up-to-date
docker-compose.yml that you can run using with

```sh
DHIS2_IMAGE=dhis2/core-dev:latest docker compose up
```

The above will start the latest development version of DHIS2 with PostgreSQL.

[dhis-core/README](https://github.com/dhis2/dhis2-core#readme) has more details on how to run DHIS2
using Docker Compose for development.

## Using Docker

The following command is the bare minimum you need to run DHIS2 in Docker

```sh
docker run \
    --volume $HOME/code/dhis2/core/docker/dhis.conf:/opt/dhis2/dhis.conf:ro \
    dhis2/core-dev:latest
```

This assumes that you have a PostgreSQL DB that is accessible by DHIS2 with the appropriate settings
in `dhis.conf`.

Please refer to the [Docker](https://www.docker.com/) documentation for more details on how to run
Docker containers.

# How to configure this image

## Context path

To deploy DHIS2 under a different context then root (`/`) configure the context path by setting the
environment variable

`CATALINA_OPTS: "-Dcontext.path='/dhis2'"`

DHIS2 should be available at `http://localhost:8080/dhis2`.

## DHIS2_HOME

[Previously](https://github.com/dhis2/dhis2-core/blob/b4d4242fb30d974254de2a72b86cc5511f70c9c0/docker/tomcat-debian/Dockerfile#L9),
the Docker image was built with the environment variable `DHIS2_HOME` set to `/DHIS2_home`. This is
not the case anymore, instead `DHIS2_HOME` will [fallback to its
default](https://github.com/dhis2/dhis2-core/blob/b4d4242fb30d974254de2a72b86cc5511f70c9c0/dhis-2/dhis-support/dhis-support-external/src/main/java/org/hisp/dhis/external/location/DefaultLocationManager.java#L58)
`/opt/dhis2`. You can still run the Docker image with the old behavior by setting the environment
variable `DHIS2_HOME` to `/DHIS2_home`.

# Caveats

We cannot recommend the images for use in production. At this point we don’t have enough experience
and therefore can’t vouch for their stability for “mission critical” production use. We are not
saying that someone experienced with running Docker in production shouldn’t use them. In other
words, anyone deciding to use DHIS2 in Docker should be aware that they are doing so at their own
risk. If you decide to go for it, make sure you perform enough security, performance and stress
testing.

# License

View [license](https://github.com/dhis2/dhis2-core/blob/master/LICENSE) information for the software
contained in this image.

As with all Docker images, these likely also contain other software which may be under other
licenses (such as Bash, etc from the base distribution, along with any direct or indirect
dependencies of the primary software being contained).

