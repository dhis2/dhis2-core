FROM maven:3.5.3-jdk-8-slim
LABEL stage=intermediate

ARG IDENTIFIER=unknown
LABEL identifier=${IDENTIFIER}

#NB - maven-surefire-plugin fails with maven:3.5.4-jdk-8-slim and later.
#     This is a recent issue possibly traced to an OpenJDK bug
#     https://github.com/carlossg/docker-maven/issues/90

RUN apt-get update && \
    apt-get install --no-install-recommends -y \
        git=1:2.11.0-3+deb9u4 && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /src

#NB - web-apps build uses `git rev-parse` to tag the build, so just copy over the whole tree for now
COPY dhis-2 .

# TODO: We should be able to achieve much faster incremental builds and cached dependencies using
RUN mvn clean install -T1C -f pom.xml -DskipTests
RUN mvn clean install -T1C -U -f dhis-web/pom.xml -DskipTests

RUN cp dhis-web/dhis-web-portal/target/dhis.war /dhis.war
