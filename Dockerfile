# each target (debian & alpine) recieves its own default
# so building each target works without providing it
ARG DEBIAN_TOMCAT_IMAGE=tomcat:8.5-jdk8-openjdk-slim
ARG ALPINE_TOMCAT_IMAGE=tomcat:8.5.34-jre8-alpine

FROM maven:3.8.1-jdk-11-slim as build

ARG IDENTIFIER=unknown
LABEL identifier=${IDENTIFIER}

# needed to clone DHIS2 apps
RUN apt-get update && \
    apt-get install --no-install-recommends -y git

WORKDIR /src

# NB: web-apps build uses `git rev-parse` to tag the build, so just copy over the whole tree for now
COPY . .

# TODO: We should be able to achieve much faster incremental builds and cached dependencies using
RUN mvn clean install -Pdev -Pjdk11 -f dhis-2/pom.xml -DskipTests -pl -dhis-web-embedded-jetty
RUN mvn clean install -Pdev -Pjdk11 -U -f dhis-2/dhis-web/pom.xml -DskipTests

RUN cp dhis-2/dhis-web/dhis-web-portal/target/dhis.war /dhis.war && \
    cd / && \
    sha256sum dhis.war > /sha256sum.txt && \
    md5sum dhis.war > /md5sum.txt


FROM alpine:latest as base

COPY --from=build /dhis.war /srv/dhis2/dhis.war
COPY --from=build /sha256sum.txt /srv/dhis2/sha256sum.txt
COPY --from=build /md5sum.txt /srv/dhis2/md5sum.txt

FROM $DEBIAN_TOMCAT_IMAGE as debian

ARG IDENTIFIER=unknown
LABEL identifier=${IDENTIFIER}

ENV WAIT_FOR_DB_CONTAINER=""

ENV DHIS2_HOME=/DHIS2_home

RUN rm -rf /usr/local/tomcat/webapps/* && \
    mkdir /usr/local/tomcat/webapps/ROOT && \
    mkdir $DHIS2_HOME && \
    adduser --system --disabled-password --group tomcat && \
    echo 'tomcat' >> /etc/cron.deny && \
    echo 'tomcat' >> /etc/at.deny

RUN apt-get update && \
    apt-get install --no-install-recommends -y \
        util-linux \
        bash \
        unzip \
        fontconfig

COPY ./docker/shared/wait-for-it.sh /usr/local/bin/
COPY ./docker/tomcat-debian/docker-entrypoint.sh /usr/local/bin/

RUN chmod +rx /usr/local/bin/docker-entrypoint.sh && \
    chmod +rx /usr/local/bin/wait-for-it.sh

COPY ./docker/shared/server.xml /usr/local/tomcat/conf
COPY --from=build /dhis.war /usr/local/tomcat/webapps/ROOT.war

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]

CMD ["catalina.sh", "run"]


FROM $ALPINE_TOMCAT_IMAGE as alpine

ARG IDENTIFIER=unknown
LABEL identifier=${IDENTIFIER}

ENV WAIT_FOR_DB_CONTAINER=""

ENV DHIS2_HOME=/DHIS2_home

RUN rm -rf /usr/local/tomcat/webapps/* && \
    mkdir /usr/local/tomcat/webapps/ROOT && \
    mkdir $DHIS2_HOME && \
    addgroup -S tomcat && \
    addgroup root tomcat && \
    adduser -S -D -G tomcat tomcat && \
    echo 'tomcat' >> /etc/cron.deny && \
    echo 'tomcat' >> /etc/at.deny

RUN apk add --update --no-cache \
        bash  \
        su-exec \
        fontconfig \
        ttf-dejavu

COPY ./docker/shared/wait-for-it.sh /usr/local/bin/
COPY ./docker/tomcat-alpine/docker-entrypoint.sh /usr/local/bin/

RUN chmod +rx /usr/local/bin/docker-entrypoint.sh && \
    chmod +rx /usr/local/bin/wait-for-it.sh

COPY ./docker/shared/server.xml /usr/local/tomcat/conf
COPY --from=build /dhis.war /usr/local/tomcat/webapps/ROOT.war

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]

CMD ["catalina.sh", "run"]

# This ensures we are building debian by default
FROM debian
