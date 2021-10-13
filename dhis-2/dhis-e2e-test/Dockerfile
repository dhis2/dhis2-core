FROM maven:3.6.3-openjdk-11-slim

COPY config/dhis2_home/dhis.conf /config/dhis2_home/dhis.conf

WORKDIR /src

COPY wait-for-it.sh pom.xml .
RUN chmod +x wait-for-it.sh

RUN mvn dependency:go-offline --batch-mode

# comes before copy/compile tests since it takes longer so invalidating this
# cache due to changing tests would be a bigger cost than the other way round.
COPY src/main src/main
RUN mvn --offline compile

COPY src/test src/test
RUN mvn --offline test-compile

VOLUME /target
