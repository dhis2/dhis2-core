# syntax=docker/dockerfile:1.3

FROM maven:3.6.3-openjdk-11-slim

COPY pom.xml wait-for-it.sh /
COPY config/dhis2_home/dhis.conf /config/dhis2_home/dhis.conf
COPY src /src

RUN --mount=type=cache,target=/root/.m2/repository chmod +x wait-for-it.sh && \
    mvn --batch-mode --no-transfer-progress compile

VOLUME /target


