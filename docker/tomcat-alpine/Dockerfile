ARG TOMCAT_IMAGE
FROM $TOMCAT_IMAGE

ARG IDENTIFIER=unknown
LABEL identifier=${IDENTIFIER}

ENV DHIS2_HOME=/DHIS2_home

COPY ./tomcat-alpine/docker-entrypoint.sh /usr/local/bin/

RUN rm -rf /usr/local/tomcat/webapps/* && \
    mkdir /usr/local/tomcat/webapps/ROOT && \
    chmod +rx /usr/local/bin/docker-entrypoint.sh && \
    mkdir $DHIS2_HOME && \
    addgroup -S tomcat && \
    addgroup root tomcat && \
    adduser -S -D -G tomcat tomcat && \
    echo 'tomcat' >> /etc/cron.deny && \
    echo 'tomcat' >> /etc/at.deny


RUN apk add --update --no-cache \
        bash  \
        su-exec

COPY ./shared/server.xml /usr/local/tomcat/conf
COPY ./artifacts/dhis.war /usr/local/tomcat/webapps/ROOT.war

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]

CMD ["catalina.sh", "run"]
