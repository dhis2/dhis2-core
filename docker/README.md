
### General idea

The `dhis2-core/Dockerfile` handles building DHIS2 from source, and then
leaves the `dhis.war` available at `/dhis.war`.

This base image can be tagged as necessary and be reused across multiple
other assemblies at this point.

See [this
comment](https://github.com/dhis2/dhis2-core/pull/3894#issuecomment-539416233)
for container image build times.

### File structure

```
./dhis2-core
├ README.md
├ dhis-2
├ docker
│   ├ shared
│   │   └ server.xml
│   ├ tomcat-alpine
│   │   ├ docker-entrypoint.sh
│   └ tomcat-debian
│       ├ docker-entrypoint.sh
├ Dockerfile
└ Jenkinsfile-publish-image
```

### Usage

#### Build the core image (a.k.a. the base image)

```sh
docker build --tag <image> . --target base
```

#### Build tomcat debian base image

Debian is used by default so you can omit the `--target`.

```sh
docker build --tag <image> .
```

If you want to build a docker image using a [specific tomcat image](https://hub.docker.com/_/tomcat/)
do

```sh
docker build --tag <image> --build-arg DEBIAN_TOMCAT_IMAGE=tomcat:9.0-jdk11-openjdk-slim .
```

Make sure to choose a **debian** and not an alpine based image from [docker hub](https://hub.docker.com/_/tomcat/).

#### Build tomcat alpine base image

```sh
docker build --tag <image> --target alpine .
```

If you want to build a docker image using a [specific tomcat image](https://hub.docker.com/_/tomcat/)
do

```sh
docker build --tag <image> --target alpine --build-arg ALPINE_TOMCAT_IMAGE=tomcat:8.5.34-jre8-alpine .
```

Make sure to choose an **alpine** not debian based image from [docker hub](https://hub.docker.com/_/tomcat/).

#### Build custom WAR file into containers

TODO provide instructions

#### Publish container images (Tomcat)

> :information_source: Uses `containers-list.sh` to get a list of containers to publish

```sh
./docker/publish-containers.sh <image>
```
