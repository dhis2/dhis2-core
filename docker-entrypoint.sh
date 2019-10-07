#!/bin/bash
set -e

WARFILE=/usr/local/tomcat/webapps/ROOT.war
TOMCATDIR=/usr/local/tomcat
DHIS2HOME=/DHIS2_home

if [ "$(id -u)" = "0" ]; then
    if [ -f $WARFILE ]; then
        unzip $WARFILE -d $TOMCATDIR/webapps/ROOT
        rm $WARFILE
    fi
    
    chown -R root:tomcat $TOMCATDIR
    chmod -R u+rwX,g+rX,o-rwx $TOMCATDIR
    chown -R tomcat:tomcat $TOMCATDIR/temp \
        $TOMCATDIR/work \
        $TOMCATDIR/logs

    chown -R tomcat:tomcat $DHIS2HOME
    exec setpriv --reuid=tomcat --regid=tomcat --init-groups "$0" "$@"
fi

exec "$@"
