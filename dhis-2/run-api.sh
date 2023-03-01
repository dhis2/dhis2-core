#!/usr/bin/env bash

# Command for running DHIS 2 in an embedded Jetty container

# Requires JDK 11

# Supported options:
#
#   -d <directory> DHIS 2 home directory
#   -h <hostname>  Hostname (default is localhost)
#   -p <port>      Port number (default is 9090)
#   -s             Skip compilation of source code

set -e

# DHIS 2 home directory
DHIS2_HOME_DIR="/opt/dhis2"
# Hostname or IP for DHIS2/Jetty to listen
DHIS2_HOSTNAME="localhost"
# Port number for DHIS2/Jetty to listen
DHIS2_PORT=9090
# Skip compilation of DHIS 2 source code
SKIP_COMPILE=0

# Set DHIS 2 home directory to DHIS2_HOME env variable if set
if [[ -v "$DHIS2_HOME" ]]; then
  DHIS2_HOME_DIR=$DHIS2_HOME
fi

# Read command line options
while getopts "d:h:p:s" OPT; do
  case "$OPT" in
    d)
      DHIS2_HOME_DIR=$OPTARG
      ;;
    h)
      DHIS2_HOSTNAME=$OPTARG
      ;;
    p)
      DHIS2_PORT=$OPTARG
      ;;
    s)
      SKIP_COMPILE=1
      ;;
    ?)
      echo "Usage: $0 [-d dhis2_home] [-h hostname] [-p port] [-s]" >&2
      echo "  -d <directory> DHIS 2 home directory" >&2
      echo "  -h <hostname>  Hostname (default is localhost)" >&2
      echo "  -p <port>      Port number (default is 9090)" >&2
      echo "  -s             Skip compilation of source code" >&2
      exit 1
      ;;
  esac
done
shift "$(($OPTIND -1))"

# Start DHIS 2 in embedded Jetty container
function start_dhis2() {
  java \
    -Ddhis2.home=$DHIS2_HOME_DIR \
    -Djetty.host=$DHIS2_HOSTNAME \
    -Djetty.http.port=$DHIS2_PORT \
    -jar "$(dirname "$0")/dhis-web-embedded-jetty/target/dhis-web-embedded-jetty.jar"
}

# Build and install DHIS 2 source code without running tests
function build_dhis2() {
  mvn clean install \
    -f "$(dirname "$0")/pom.xml" \
    --batch-mode \
    -Pdev -T 100C \
    -DskipTests -Dmaven.test.skip=true -Dmaven.site.skip=true -Dmaven.javadoc.skip=true
}

echo -e "Note: JDK 11 or later is required!\n"

# Print variables
echo "JAVA_HOME: $JAVA_HOME"
echo "DHIS2_HOME_DIR: $DHIS2_HOME"
echo "HOSTNAME: $DHIS2_HOSTNAME"
echo "PORT: $DHIS2_PORT"
echo "SKIP_COMPILE: $SKIP_COMPILE"
echo ""

# Verify DHIS2_HOME variable
[ ! -d $DHIS2_HOME_DIR ] && echo "DHIS2_HOME directory '$DHIS2_HOME' does not exist, aborting." && exit 1;
[ ! -f "$DHIS2_HOME_DIR/dhis.conf" ] && echo "dhis.conf in directory '$DHIS2_HOME_DIR' does not exist, aborting." && exit 1;

# Compile unless skip compile flag is given
if [[ $SKIP_COMPILE == 0 ]]; then
  build_dhis2
fi

# Start DHIS 2
start_dhis2
exit 0;
