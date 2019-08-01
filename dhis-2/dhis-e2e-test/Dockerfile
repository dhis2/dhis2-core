FROM maven:3.5.3-jdk-8-slim

COPY pom.xml wait-for-it.sh /
RUN mvn dependency:go-offline

COPY config/dhis2_home/dhis.conf /config/dhis2_home/dhis.conf
COPY src /src

VOLUME /target

RUN chmod +x wait-for-it.sh
RUN mvn compile
