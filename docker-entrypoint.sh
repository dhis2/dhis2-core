#!/bin/bash

set -e
if [ "$(id -u)" = "0" ]; then
    unzip /usr/local/tomcat/webapps/ROOT.war -d /usr/local/tomcat/webapps/ROOT
    rm /usr/local/tomcat/webapps/ROOT.war
    chown -R root:tomcat /usr/local/tomcat
    chmod -R u+rwX,g+rX,o-rwx /usr/local/tomcat
    chown -R tomcat:tomcat /usr/local/tomcat/temp \
        /usr/local/tomcat/work \
        /usr/local/tomcat/logs

    chown -R tomcat:tomcat /DHIS2_home
    exec su-exec tomcat "$0" "$@"
fi

exec "$@"
