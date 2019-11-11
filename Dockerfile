##########
# SETUP:
# There is only one pre-requisite to build and run from source:
#   - On OSX or Windows: Docker Desktop <https://www.docker.com/products/docker-desktop>
#   - On Linux: Docker Engine <https://docs.docker.com/install/>
# For a streamlined Docker DHIS2 experience, see the docker-compose setup at <https://github.com/amcgee/dhis2-backend>
##########
# USAGE:
# To build, run this command in the directory containing this Dockerfile:
#   > docker build -t <imagetag> .
# To spin up the built image, mount read-only DHIS2_home from <localdir>, and listen on local <port>:
#   > docker run -d --rm -p <port>:80 -v <localdir>:/DHIS2_home:ro <imagetag>
# This will output a <containerid>, you will need this ID to manage the running service.
#   NB: You can find the <containerid> later by running `docker ps`
# The DHIS2 Core instance should now be running in the background
#
# Once the core has finished starting up it will be accessible at http://localhost:<port>
# To tail the logs of the running service:
#   > docker logs --follow <containerid>
# To stop the process:
#   > docker kill <containerid>
##########

##########
# BUILD STAGE 1
# Build the DHIS2 Core server from source (Maven)
##########
FROM maven:3.5.3-jdk-8-slim as build
LABEL stage=intermediate

ARG IDENTIFIER=unknown
LABEL identifier=${IDENTIFIER}
#NB - maven-frontend-plugin breaks on Alpine linux, so we use Debian Slim instead
#NB - maven-surefire-plugin fails with maven:3.5.4-jdk-8-slim and later.
#     This is a recent issue possibly traced to an OpenJDK bug - https://github.com/carlossg/docker-maven/issues/90

RUN apt-get update && \
    apt-get install --no-install-recommends -y \
        git=1:2.11.0-3+deb9u4 && \
    rm -rf /var/lib/apt/lists/*

#NB - web-apps build uses `git rev-parse` to tag the build, so just copy over the whole tree for now
COPY . /src

# TODO: We should be able to achieve much faster incremental builds and cached dependencies using
#   a wrapper build script and intelligent Docker layer caching, but for now just naively build everything
RUN mvn clean install -T1C -f /src/dhis-2/pom.xml -DskipTests
RUN mvn clean install -T1C -U -f /src/dhis-2/dhis-web/pom.xml -DskipTests

##########
# BUILD STAGE 2
# Serve the packaged .war file (Tomcat)
# Use a second build stage in a fresh container so we don't bloat it with the Maven build tools
##########
FROM tomcat:8.5.34-jre8-alpine as serve

ENV WAIT_FOR_DB_CONTAINER=""
ENV DHIS2_HOME=/DHIS2_home

COPY ./docker/shared/wait-for-it.sh /usr/local/bin/
COPY docker-entrypoint.sh /usr/local/bin/

RUN rm -rf /usr/local/tomcat/webapps/* && \
    mkdir /usr/local/tomcat/webapps/ROOT && \
    chmod +rx /usr/local/bin/docker-entrypoint.sh && \
    chmod +rx /usr/local/bin/wait-for-it.sh && \
    mkdir $DHIS2_HOME && \
    addgroup -S tomcat && \
    addgroup root tomcat && \
    adduser -S -D -G tomcat tomcat

RUN apk add --update --no-cache \
        bash  \
        su-exec

COPY server.xml /usr/local/tomcat/conf
COPY --from=build /src/dhis-2/dhis-web/dhis-web-portal/target/dhis.war /usr/local/tomcat/webapps/ROOT.war

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]

CMD ["catalina.sh", "run"]


