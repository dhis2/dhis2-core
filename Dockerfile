#
# Build the base DHIS2 image
#

FROM maven:3.6.3-jdk-8-slim as build

ARG IDENTIFIER=unknown
LABEL identifier=${IDENTIFIER}

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install --no-install-recommends -y \
        git && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /src

# NB: web-apps build uses `git rev-parse` to tag the build, so just copy over the whole tree for now
COPY . .

# TODO: We should be able to achieve much faster incremental builds and cached dependencies using
RUN mvn clean install -f dhis-2/pom.xml -DskipTests
RUN mvn clean install -U -f dhis-2/dhis-web/pom.xml -DskipTests

RUN cp dhis-2/dhis-web/dhis-web-portal/target/dhis.war /dhis.war && \
    cd / && \
    sha256sum dhis.war > /sha256sum.txt && \
    md5sum dhis.war > /md5sum.txt

#
# Slim final image that has the build artifacts at /srv/dhis2
#

FROM alpine:latest
COPY --from=build /dhis.war /srv/dhis2/dhis.war
COPY --from=build /sha256sum.txt /srv/dhis2/sha256sum.txt
COPY --from=build /md5sum.txt /srv/dhis2/md5sum.txt
