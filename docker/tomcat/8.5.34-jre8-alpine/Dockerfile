# Build argument used to customize the base DHIS2 core image.
# Needs to be an image that has the file: `/dhis.war`

ARG image
FROM $image as build

# Application server

FROM tomcat:8.5.34-jre8-alpine

ENV DHIS2_HOME=/DHIS2_home

COPY ./docker/tomcat/8.5.34-jre8-alpine/docker-entrypoint.sh /usr/local/bin/

RUN rm -rf /usr/local/tomcat/webapps/* && \
    mkdir /usr/local/tomcat/webapps/ROOT && \
    chmod +rx /usr/local/bin/docker-entrypoint.sh && \
    mkdir $DHIS2_HOME && \
    addgroup -S tomcat && \
    addgroup root tomcat && \
    adduser -S -D -G tomcat tomcat

RUN apk add --update --no-cache \
        bash  \
        su-exec

COPY ./docker/shared/server.xml /usr/local/tomcat/conf
COPY --from=build /dhis.war /usr/local/tomcat/webapps/ROOT.war

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]

CMD ["catalina.sh", "run"]
