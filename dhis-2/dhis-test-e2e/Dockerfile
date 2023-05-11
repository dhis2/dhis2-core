FROM maven:3.6.3-openjdk-11-slim

COPY pom.xml wait-for-it.sh /
RUN chmod +x wait-for-it.sh

COPY src /src
