#!/bin/bash
set -e

WARFILE=/usr/local/tomcat/webapps/ROOT.war
BINDIR=/usr/local/bin
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
    exec su-exec tomcat "$0" "$@"
fi

if [[ ! -z "$WAIT_FOR_DB_CONTAINER" ]]; then
    $BINDIR/wait-for-it.sh $WAIT_FOR_DB_CONTAINER
fi

exec "$@"
