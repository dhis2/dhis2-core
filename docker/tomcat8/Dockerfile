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
ARG image=dhis2/core:master
FROM $image as build

##########
# BUILD STAGE 2
# Serve the packaged .war file (Tomcat)
# Use a second build stage in a fresh container so we don't bloat it with the Maven build tools
##########
FROM tomcat:8.5.46-jdk8-openjdk-slim as serve

ENV DHIS2_HOME=/DHIS2_home

COPY ./docker/shared/docker-entrypoint.sh /usr/local/bin/

RUN rm -rf /usr/local/tomcat/webapps/* && \
    mkdir /usr/local/tomcat/webapps/ROOT && \
    chmod +rx /usr/local/bin/docker-entrypoint.sh && \
    mkdir $DHIS2_HOME && \
    adduser --system --disabled-password --group tomcat && \
    echo 'tomcat' >> /etc/cron.deny && \
    echo 'tomcat' >> /etc/at.deny

RUN apt-get update && \
    apt-get install --no-install-recommends -y \
        util-linux \
        bash \
        unzip

COPY ./docker/shared/server.xml /usr/local/tomcat/conf
COPY --from=build /dhis.war /usr/local/tomcat/webapps/ROOT.war

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]

CMD ["catalina.sh", "run"]
