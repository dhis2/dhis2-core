FROM maven:3.8.1-openjdk-17-slim

COPY pom.xml wait-for-it.sh /
RUN chmod +x wait-for-it.sh

COPY src /src
