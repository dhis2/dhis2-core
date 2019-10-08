ARG TOMCAT_IMAGE
FROM $TOMCAT_IMAGE

ENV DHIS2_HOME=/DHIS2_home

COPY ./docker/tomcat-debian/docker-entrypoint.sh /usr/local/bin/

RUN rm -rf /usr/local/tomcat/webapps/* && \
    mkdir /usr/local/tomcat/webapps/ROOT && \
    chmod +rx /usr/local/bin/docker-entrypoint.sh && \
    mkdir $DHIS2_HOME && \
    adduser --system --disabled-password --group tomcat

RUN apt-get update && \
    apt-get install --no-install-recommends -y \
        util-linux \
        bash \
        unzip

COPY ./docker/shared/server.xml /usr/local/tomcat/conf
COPY $WAR_FILE /usr/local/tomcat/webapps/ROOT.war

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]

CMD ["catalina.sh", "run"]
