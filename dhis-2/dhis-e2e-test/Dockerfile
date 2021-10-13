FROM maven:3.6.3-openjdk-11-slim

COPY config/dhis2_home/dhis.conf /config/dhis2_home/dhis.conf

WORKDIR /src

COPY wait-for-it.sh .
RUN chmod +x wait-for-it.sh

COPY pom.xml pom.xml
RUN mvn dependency:go-offline --batch-mode

COPY src/main src/main
RUN mvn --offline compile

COPY src/test src/test
RUN mvn --offline test-compile

VOLUME /target
