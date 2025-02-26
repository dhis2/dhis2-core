#!/usr/bin/env bash

# Command for running DHIS2 in an embedded Tomcat

# Requires JDK 11

# Supported options:
#
#   -d <directory> DHIS2 home directory
#   -p <port>      Port number (default is 9090)
#   -s             Skip compilation of source code
#   -m             Print this manual

set -e

# DHIS2 home directory
DHIS2_HOME_DIR="/opt/dhis2"
# Port number for DHIS2 to listen to
DHIS2_PORT=9090
# Skip compilation of DHIS2 source code
SKIP_COMPILE=0

# Set DHIS2 home directory to DHIS2_HOME_DIR env variable if set
if [[ -n "$DHIS2_HOME" ]]; then
  DHIS2_HOME_DIR=$DHIS2_HOME
fi

# Print usage help
function print_usage() {
  echo "Usage: $0 [-d dhis2_home] [-p port] [-s]" >&2
  echo "  -d <directory> DHIS2 home directory" >&2
  echo "  -s             Skip compilation of source code" >&2
  echo "  -p <port>      Port number (default is 9090)" >&2
  echo "  -m             Print this manual" >&2
}

# Print variables
function print_variables() {
  echo "JAVA_HOME: $JAVA_HOME"
  echo "DHIS2_HOME_DIR: $DHIS2_HOME_DIR"
  echo "PORT: $DHIS2_PORT"
  echo "SKIP_COMPILE: $SKIP_COMPILE"
  echo ""
}

# Start DHIS2 in embedded Tomcat
function start_dhis2() {
  DHIS2_HTTP_PORT="$DHIS2_PORT" \
  DHIS2_HOME="$DHIS2_HOME_DIR" \
  java \
    -Ddhis2.home="$DHIS2_DHIS2_HOME_DIR" \
    -Dserver.port="$DHIS2_PORT" \
    -jar "$(dirname "$0")/dhis-web-server/target/dhis.war"
}

# Build and install DHIS2 source code without running tests
function build_dhis2() {
  mvn clean package \
    --file "$(dirname "$0")/pom.xml" \
    --batch-mode --threads 100C \
    -DskipTests -Dmaven.test.skip=true -Dmaven.site.skip=true -Dmaven.javadoc.skip=true \
    --activate-profiles embedded
}

# Read command line options
while getopts "d:p:sm" OPT; do
  case "$OPT" in
    d)
      DHIS2_HOME_DIR=$OPTARG
      ;;
    p)
      DHIS2_PORT=$OPTARG
      ;;
    s)
      SKIP_COMPILE=1
      ;;
    m)
      print_usage
      exit 0
      ;;
    ?)
      print_usage
      exit 1
      ;;
  esac
done
shift "$(($OPTIND -1))"

echo -e "Note: JDK 11 or later is required!\n"

print_variables

# Verify DHIS2_HOME
[ ! -d "$DHIS2_HOME_DIR" ] && echo "DHIS2_HOME directory '$DHIS2_HOME_DIR' does not exist, aborting." && exit 1;
[ ! -f "$DHIS2_HOME_DIR/dhis.conf" ] && echo "dhis.conf in directory '$DHIS2_HOME_DIR' does not exist, aborting." && exit 1;

# Compile unless skip compile flag is set
if [[ $SKIP_COMPILE == 0 ]]; then
  build_dhis2
fi

start_dhis2
exit 0
