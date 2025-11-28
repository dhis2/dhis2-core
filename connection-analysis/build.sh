#!/usr/bin/env bash
# Build DHIS2 with embedded Tomcat for standalone execution

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$SCRIPT_DIR/.."

echo "Building DHIS2 with embedded Tomcat..."
mvn package --file "$REPO_ROOT/dhis-2/pom.xml" --projects dhis-web-server --also-make \
    -DskipTests --activate-profiles embedded

echo ""
echo "Build complete. Run: connection-analysis/start.sh"
