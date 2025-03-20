#!/usr/bin/env bash

# DHIS2 Server Management Script
#
# A CLI tool to start, stop, restart and monitor the DHIS2 embedded Tomcat server
#
# Usage: ./dhis2-server.sh [command] [options]
#
# Commands:
#   start    - Start the DHIS2 server
#   stop     - Stop the DHIS2 server
#   restart  - Restart the DHIS2 server
#   status   - Check the status of the DHIS2 server
#   health   - Check the health of the server via the health endpoint
#   logs     - View the server logs
#   version  - Display the DHIS2 version
#   upgrade  - Upgrade the DHIS2 server (requires additional parameters)
#   install-service - Install DHIS2 as a systemd service
#
# Options:
#   -d, --home DIR   Set DHIS2 home directory (default: /opt/dhis2)
#   -p, --port PORT  Set the server port (default: 8080)
#   -j, --java JAVA  Set the Java executable path
#   -h, --help       Display this help message
#   -v, --verbose    Enable verbose output

set -e

# Default configuration
DHIS2_HOME_DIR="/opt/dhis2"
DHIS2_PORT=8080
JAVA_CMD="java"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="$SCRIPT_DIR/dhis2-server.pid"
LOG_FILE="$DHIS2_HOME_DIR/logs/dhis.log"
VERBOSE=0
DHIS2_WAR_PATH="$SCRIPT_DIR/dhis-web-server/target/dhis.war"

# Java version requirements
JAVA_MIN_VERSION_LEGACY=8  # For DHIS2 < 2.41
JAVA_MIN_VERSION_MODERN=17 # For DHIS2 >= 2.41
DHIS2_VERSION_THRESHOLD="2.41"

# Tool availability flags
HAVE_CURL=0
HAVE_WGET=0
HAVE_GREP=0
HAVE_SED=0
HAVE_AWK=0
HAVE_TAIL=0
HAVE_CAT=0
HAVE_LSOF=0
HAVE_JAR=0
HAVE_PSQL=0
HAVE_PG_DUMP=0
HAVE_GZIP=0
HAVE_PS=0

# Check if required tools are available for the given command
# Usage: check_required_tools command
check_required_tools() {
    local command=$1
    local missing_tools=()
    local required_tools=()

    # Define required tools for each command
    case "$command" in
        upgrade)
            required_tools=("grep" "sed" "curl|wget" "jar")
            ;;
        health)
            required_tools=("curl|wget")
            ;;
        status)
            required_tools=("ps" "lsof" "awk")
            ;;
        version)
            required_tools=("grep" "sed")
            ;;
        logs)
            required_tools=("tail|cat")
            ;;
        _backup_database)
            required_tools=("grep" "sed" "pg_dump" "gzip")
            ;;
        _fetch_available_versions)
            required_tools=("curl|wget")
            ;;
        _show_available_versions)
            required_tools=("grep" "sed")
            ;;
        _check_db_connections)
            required_tools=("grep" "sed" "psql")
            ;;
    esac

    if [ ${#required_tools[@]} -eq 0 ]; then
        return 0  # No tools required for this command
    fi

    echo "Checking for required tools..."

    # Check all tools and mark as available/unavailable
    command -v curl > /dev/null && HAVE_CURL=1
    command -v wget > /dev/null && HAVE_WGET=1
    command -v grep > /dev/null && HAVE_GREP=1
    command -v sed > /dev/null && HAVE_SED=1
    command -v awk > /dev/null && HAVE_AWK=1
    command -v tail > /dev/null && HAVE_TAIL=1
    command -v cat > /dev/null && HAVE_CAT=1
    command -v lsof > /dev/null && HAVE_LSOF=1
    command -v jar > /dev/null && HAVE_JAR=1
    command -v psql > /dev/null && HAVE_PSQL=1
    command -v pg_dump > /dev/null && HAVE_PG_DUMP=1
    command -v gzip > /dev/null && HAVE_GZIP=1
    command -v ps > /dev/null && HAVE_PS=1

    # Check for missing tools
    for tool in "${required_tools[@]}"; do
        if [[ "$tool" == *"|"* ]]; then
            # Alternative tools (tool1|tool2) - at least one must be available
            local alternatives=(${tool//|/ })
            local any_available=0
            local available_tool=""

            for alt in "${alternatives[@]}"; do
                local var_name="HAVE_$(echo "$alt" | tr '[:lower:]' '[:upper:]')"
                if [ "${!var_name}" -eq 1 ]; then
                    any_available=1
                    available_tool="$alt"
                    break
                fi
            done

            if [ $any_available -eq 0 ]; then
                missing_tools+=("$tool")
            fi
        else
            # Single required tool
            local var_name="HAVE_$(echo "$tool" | tr '[:lower:]' '[:upper:]')"
            if [ "${!var_name}" -eq 0 ]; then
                missing_tools+=("$tool")
            fi
        fi
    done

    # If there are missing tools, show an error
    if [ ${#missing_tools[@]} -gt 0 ]; then
        echo "Error: The following required tools are missing for the '$command' command:"
        for tool in "${missing_tools[@]}"; do
            echo "  - $tool"
        done
        echo "Please install the missing tools and try again."
        return 1
    fi

    echo "All required tools are available."
    return 0
}

# Check if the WAR file exists at the provided path
check_war_file() {
    if [ ! -f "$DHIS2_WAR_PATH" ]; then
        echo "Error: DHIS2 WAR file not found at $DHIS2_WAR_PATH"
        echo "Please build the application first using: mvn clean package -DskipTests --activate-profiles embedded"
        exit 1
    fi
}

# Common function to check Java compatibility
# Usage: _check_java_compatibility [target_version]
# If target_version is not provided, the current version will be detected
_check_java_compatibility() {
    local TARGET_VERSION="$1"
    local EXIT_ON_ERROR=${2:-true}

    # Check Java existence
    if ! command -v "$JAVA_CMD" > /dev/null; then
        echo "Error: Java not found. Please install Java and/or set the correct path with --java option."
        [ "$EXIT_ON_ERROR" = "true" ] && exit 1
        return 1
    fi

    # Extract Java version
    JAVA_VERSION_OUTPUT=$("$JAVA_CMD" -version 2>&1)

    # Different patterns to match Java version strings
    if [[ "$JAVA_VERSION_OUTPUT" =~ version\ \"([0-9]+) ]]; then
        # Java 9 and above uses simple versioning (e.g., "11.0.12")
        JAVA_VERSION="${BASH_REMATCH[1]}"
    elif [[ "$JAVA_VERSION_OUTPUT" =~ version\ \"1\.([0-9]+) ]]; then
        # Java 8 and below uses 1.x versioning (e.g., "1.8.0_292")
        JAVA_VERSION="${BASH_REMATCH[1]}"
    else
        echo "Error: Unable to determine Java version."
        [ "$EXIT_ON_ERROR" = "true" ] && exit 1
        return 1
    fi

    # Get DHIS2 version if not provided
    if [ -z "$TARGET_VERSION" ]; then
        TARGET_VERSION=$(_get_current_version)
    fi

    if [ -z "$TARGET_VERSION" ] || [ "$TARGET_VERSION" = "local" ]; then
        echo "Warning: Unable to detect DHIS2 version. Will use default minimum Java version requirement (Java $JAVA_MIN_VERSION_MODERN)."
        REQUIRED_JAVA=$JAVA_MIN_VERSION_MODERN
        EXACT_JAVA=false
    else
        # Extract major.minor from DHIS2 version
        if [[ "$TARGET_VERSION" =~ ^([0-9]+\.[0-9]+) ]]; then
            DHIS2_MAJOR_MINOR="${BASH_REMATCH[1]}"

            # Compare version with threshold
            if [ "$(printf '%s\n' "$DHIS2_VERSION_THRESHOLD" "$DHIS2_MAJOR_MINOR" | sort -V | head -n1)" = "$DHIS2_VERSION_THRESHOLD" ] ||
               [ "$DHIS2_MAJOR_MINOR" = "$DHIS2_VERSION_THRESHOLD" ]; then
                # Version is equal to or greater than threshold
                REQUIRED_JAVA=$JAVA_MIN_VERSION_MODERN
                EXACT_JAVA=false
                echo "DHIS2 version $TARGET_VERSION requires Java $REQUIRED_JAVA or higher."
            else
                # Version is less than threshold - requires exactly Java 8
                REQUIRED_JAVA=$JAVA_MIN_VERSION_LEGACY
                EXACT_JAVA=true
                echo "DHIS2 version $TARGET_VERSION requires exactly Java $REQUIRED_JAVA (not higher or lower)."
            fi
        else
            echo "Warning: Unable to parse DHIS2 version format. Will use default minimum Java version requirement (Java $JAVA_MIN_VERSION_MODERN)."
            REQUIRED_JAVA=$JAVA_MIN_VERSION_MODERN
            EXACT_JAVA=false
        fi
    fi

    # Check if Java version meets requirements
    if [ "$EXACT_JAVA" = "true" ]; then
        # For versions < 2.41, require exactly Java 8
        if [ "$JAVA_VERSION" -ne "$REQUIRED_JAVA" ]; then
            echo "Error: Java version $JAVA_VERSION is not supported."
            echo "DHIS2 version $TARGET_VERSION requires exactly Java $REQUIRED_JAVA (not higher or lower)."
            echo "Please install Java 8 and/or set the correct path with --java option."
            [ "$EXIT_ON_ERROR" = "true" ] && exit 1
            return 1
        fi
    else
        # For versions >= 2.41, require Java 17 or higher
        if [ "$JAVA_VERSION" -lt "$REQUIRED_JAVA" ]; then
            echo "Error: Java version $JAVA_VERSION is not supported."
            echo "DHIS2 version $([ -z "$TARGET_VERSION" ] && echo "unknown" || echo "$TARGET_VERSION") requires Java $REQUIRED_JAVA or higher."
            echo "Please install a compatible Java version and/or set the correct path with --java option."
            [ "$EXIT_ON_ERROR" = "true" ] && exit 1
            return 1
        fi
    fi

    echo "Java version check passed. Using Java $JAVA_VERSION."
    return 0
}

# Check Java version against DHIS2 version requirements
check_java_version() {
    # Use the common function
    _check_java_compatibility
}

# Check DHIS2 home directory and configuration
check_dhis2_home() {
    if [ ! -d "$DHIS2_HOME_DIR" ]; then
        echo "Error: DHIS2_HOME directory '$DHIS2_HOME_DIR' does not exist."
        exit 1
    fi

    if [ ! -f "$DHIS2_HOME_DIR/dhis.conf" ]; then
        echo "Error: dhis.conf file not found in '$DHIS2_HOME_DIR'"
        exit 1
    fi
}

# Check if the server is running
is_running() {
    if [ -f "$PID_FILE" ]; then
        local PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null; then
            return 0  # Running
        fi
        # Process not found but PID file exists, clean up
        rm -f "$PID_FILE"
    fi
    return 1  # Not running
}

# Start DHIS2 server
start_server() {
    if is_running; then
        echo "DHIS2 server is already running with PID $(cat "$PID_FILE")"
        return
    fi

    check_dhis2_home
    check_war_file
    check_java_version

    echo "Starting DHIS2 server on port $DHIS2_PORT..."

    # Create log directory if it doesn't exist
    mkdir -p "$(dirname "$LOG_FILE")"

    # Start the server in the background
    DHIS2_HTTP_PORT="$DHIS2_PORT" \
    DHIS2_HOME="$DHIS2_HOME_DIR" \
    nohup "$JAVA_CMD" \
        -Ddhis2.home="$DHIS2_HOME_DIR" \
        -Dserver.port="$DHIS2_PORT" \
        -jar "$DHIS2_WAR_PATH" > "$LOG_FILE" 2>&1 &

    # Save PID
    echo $! > "$PID_FILE"

    # Wait a moment and check if the process is still running
    sleep 2
    if is_running; then
        echo "DHIS2 server started successfully (PID: $(cat "$PID_FILE"))"
        echo "Logs available at $LOG_FILE"
    else
        echo "Failed to start DHIS2 server. Check the logs at $LOG_FILE"
        exit 1
    fi
}

# Stop DHIS2 server
stop_server() {
    if ! is_running; then
        echo "DHIS2 server is not running."
        return
    fi

    local PID=$(cat "$PID_FILE")
    echo "Stopping DHIS2 server (PID: $PID)..."

    # Try graceful shutdown first
    kill "$PID"

    # Wait for the process to stop
    local TIMEOUT=60
    local COUNT=0
    while [ $COUNT -lt $TIMEOUT ] && ps -p "$PID" > /dev/null; do
        echo "Waiting for server to shutdown... ($COUNT/$TIMEOUT)"
        sleep 1
        COUNT=$((COUNT+1))
    done

    # If still running, force kill
    if ps -p "$PID" > /dev/null; then
        echo "Server not responding, sending SIGKILL..."
        kill -9 "$PID"
        sleep 1
    fi

    # Clean up PID file
    rm -f "$PID_FILE"
    echo "DHIS2 server stopped."
}

# Restart the server
restart_server() {
    echo "Restarting DHIS2 server..."
    stop_server
    sleep 2  # Small delay to ensure clean shutdown
    start_server
}

# Check server status
check_status() {
    if is_running; then
        local PID=$(cat "$PID_FILE")
        echo "DHIS2 server is running (PID: $PID)"

        # Show some process details
        echo "Process information:"
        ps -f -p "$PID"

        # Get uptime of the process
        if [ $HAVE_AWK -eq 1 ] && [ $HAVE_PS -eq 1 ]; then
            local START_TIME=$(ps -p "$PID" -o lstart= 2>/dev/null)
            if [ -n "$START_TIME" ]; then
                echo "Started at: $START_TIME"
            fi
        fi

        # Try to get the port it's listening on
        if [ $HAVE_LSOF -eq 1 ]; then
            echo "Listening ports:"
            lsof -Pan -p "$PID" -i | grep LISTEN
        fi
    else
        echo "DHIS2 server is not running."
    fi
}

# View the logs
view_logs() {
    if [ ! -f "$LOG_FILE" ]; then
        echo "Log file not found: $LOG_FILE"
        exit 1
    fi

    if [ $HAVE_TAIL -eq 1 ]; then
        # If a line count is specified, use it
        if [ -n "$1" ] && [ "$1" -eq "$1" ] 2>/dev/null; then
            tail -n "$1" "$LOG_FILE"
        else
            # Default to following the logs
            echo "Showing logs (Ctrl+C to exit):"
            tail -f "$LOG_FILE"
        fi
    else
        # Fallback if tail is not available
        cat "$LOG_FILE"
    fi
}

# Get DHIS2 version
get_version() {
    # Try to extract version from WAR file or POM
    local VERSION=""

    if [ -f "$SCRIPT_DIR/pom.xml" ]; then
        # Try to extract version from POM file if available
        if [ $HAVE_GREP -eq 1 ] && [ $HAVE_SED -eq 1 ]; then
            VERSION=$(grep -m 1 "<version>" "$SCRIPT_DIR/pom.xml" | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
        fi
    fi

    if [ -n "$VERSION" ]; then
        echo "DHIS2 Version: $VERSION"
    else
        echo "Unable to determine DHIS2 version."
    fi

    # Show Java version if available
    if command -v "$JAVA_CMD" > /dev/null; then
        echo "Java version:"
        "$JAVA_CMD" -version
    fi
}

# Upgrade DHIS2 server
upgrade_server() {
    # Check required tools first
    check_required_tools "upgrade" || exit 1

    echo "============================================"
    echo "        DHIS2 Server Smart Upgrade         "
    echo "============================================"

    # Check if the server is running
    if is_running; then
        echo "Warning: Server is currently running."
        read -p "Do you want to continue? Server will be stopped for upgrade (y/n): " response
        if [[ ! "$response" =~ ^[Yy]$ ]]; then
            echo "Upgrade aborted."
            exit 0
        fi
    fi

    # Detect current version and show information
    local CURRENT_VERSION=$(_get_current_version)
    if [ -z "$CURRENT_VERSION" ]; then
        echo "Error: Unable to detect current DHIS2 version."
        echo "You can still proceed with upgrade to a specific version."
        CURRENT_VERSION="unknown"
    else
        echo "Current DHIS2 version: $CURRENT_VERSION"
    fi

    # Determine upgrade mode
    echo
    echo "Available upgrade modes:"
    echo "  1. Download and upgrade to latest stable version"
    echo "  2. Download and upgrade to a specific version"
    echo "  3. Upgrade with locally built WAR file (developer option)"
    echo "  4. View available versions and exit"
    echo
    read -p "Select upgrade mode (1-4): " UPGRADE_MODE

    case $UPGRADE_MODE in
        1)
            _auto_upgrade
            ;;
        2)
            _manual_version_upgrade
            ;;
        3)
            _local_build_upgrade
            ;;
        4)
            _show_available_versions
            exit 0
            ;;
        *)
            echo "Invalid option. Upgrade aborted."
            exit 1
            ;;
    esac
}

# Get the current DHIS2 version
_get_current_version() {
    local VERSION=""

    # Try to get version from the WAR file
    if [ -f "$DHIS2_WAR_PATH" ]; then
        # Try using Java's jar command to extract the version from MANIFEST.MF
        if [ $HAVE_JAR -eq 1 ] && [ $HAVE_GREP -eq 1 ]; then
            VERSION=$(jar xf "$DHIS2_WAR_PATH" META-INF/MANIFEST.MF 2>/dev/null && grep "Implementation-Version" META-INF/MANIFEST.MF 2>/dev/null | cut -d' ' -f2)
            rm -f META-INF/MANIFEST.MF 2>/dev/null
            rmdir META-INF 2>/dev/null
        fi
    fi

    # If version still not found, try from POM
    if [ -z "$VERSION" ] && [ -f "$SCRIPT_DIR/pom.xml" ]; then
        if [ $HAVE_GREP -eq 1 ] && [ $HAVE_SED -eq 1 ]; then
            VERSION=$(grep -m 1 "<version>" "$SCRIPT_DIR/pom.xml" | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
        fi
    fi

    echo "$VERSION"
}

# Fetch available versions from DHIS2 release server
_fetch_available_versions() {
    echo "Fetching available DHIS2 versions..."

    # Check if we have curl or wget available
    if [ $HAVE_CURL -eq 1 ]; then
        local VERSIONS_JSON=$(curl -s "https://releases.dhis2.org/v1/versions/stable.json")
    elif [ $HAVE_WGET -eq 1 ]; then
        local VERSIONS_JSON=$(wget -q -O - "https://releases.dhis2.org/v1/versions/stable.json")
    else
        echo "Error: Neither curl nor wget found. Cannot fetch available versions."
        return 1
    fi

    # Check if we successfully downloaded the versions
    if [ -z "$VERSIONS_JSON" ]; then
        echo "Error: Failed to download version information."
        return 1
    fi

    echo "$VERSIONS_JSON"
}

# Display available versions
_show_available_versions() {
    echo "Fetching and displaying available DHIS2 versions..."

    local VERSIONS_JSON=$(_fetch_available_versions)
    if [ $? -ne 0 ]; then
        return 1
    fi

    echo "============================================"
    echo "      Available DHIS2 Version Groups       "
    echo "============================================"

    # Extract and display major.minor versions
    # Use awk, sed and grep to parse the JSON (simpler than requiring jq)
    if [ $HAVE_GREP -eq 1 ] && [ $HAVE_SED -eq 1 ]; then
        echo "$VERSIONS_JSON" | grep -o '"name":"[0-9]\+\.[0-9]\+"' | sed 's/"name":"//;s/"//' | sort -V | while read -r VERSION; do
            local LATEST=$(echo "$VERSIONS_JSON" | grep -A 5 "\"name\":\"$VERSION\"" | grep -o '"latestPatchVersion":[0-9]\+' | sed 's/"latestPatchVersion"://')
            local SUPPORTED=$(echo "$VERSIONS_JSON" | grep -A 5 "\"name\":\"$VERSION\"" | grep -o '"supported":true' | wc -l)

            if [ "$SUPPORTED" -eq 1 ]; then
                echo "  $VERSION (Latest: $VERSION.$LATEST) - SUPPORTED"
            else
                echo "  $VERSION (Latest: $VERSION.$LATEST)"
            fi
        done
    else
        echo "Error: Required command-line tools (grep, sed) not found."
        return 1
    fi
}

# Automatic upgrade to the latest appropriate version
_auto_upgrade() {
    local CURRENT_VERSION="$(_get_current_version)"
    local VERSIONS_JSON=$(_fetch_available_versions)
    if [ $? -ne 0 ]; then
        return 1
    fi

    # Parse major.minor.patch from current version
    local CURRENT_MAJOR=$(echo "$CURRENT_VERSION" | cut -d. -f1)
    local CURRENT_MINOR=$(echo "$CURRENT_VERSION" | cut -d. -f2)
    local CURRENT_PATCH=$(echo "$CURRENT_VERSION" | cut -d. -f3 | sed 's/[^0-9].*//')  # Remove anything after the number

    echo "Analyzing upgrade options..."

    # Find available upgrade options
    echo "============================================"
    echo "          Available Upgrade Paths          "
    echo "============================================"

    # 1. Check for patch/hotfix upgrades within same major.minor
    local SAME_VERSION_GROUP="$CURRENT_MAJOR.$CURRENT_MINOR"

    # Extract latest patch version for the current major.minor
    if [ $HAVE_GREP -eq 1 ] && [ $HAVE_SED -eq 1 ]; then
        local LATEST_PATCH=$(echo "$VERSIONS_JSON" | grep -A 5 "\"name\":\"$SAME_VERSION_GROUP\"" | grep -o '"latestPatchVersion":[0-9]\+' | sed 's/"latestPatchVersion"://')
        local LATEST_HOTFIX=$(echo "$VERSIONS_JSON" | grep -A 5 "\"name\":\"$SAME_VERSION_GROUP\"" | grep -o '"latestHotfixVersion":[0-9]\+' | sed 's/"latestHotfixVersion"://')

        # If LATEST_HOTFIX is empty, set it to 0
        if [ -z "$LATEST_HOTFIX" ]; then
            LATEST_HOTFIX=0
        fi

        # Check if there's a newer patch or hotfix version
        if [ "$LATEST_PATCH" -gt "$CURRENT_PATCH" ] || [ "$LATEST_PATCH" -eq "$CURRENT_PATCH" -a "$LATEST_HOTFIX" -gt 0 ]; then
            local LATEST_VERSION="$SAME_VERSION_GROUP.$LATEST_PATCH"
            if [ "$LATEST_HOTFIX" -gt 0 ]; then
                LATEST_VERSION="$LATEST_VERSION.$LATEST_HOTFIX"
            fi

            echo "1. RECOMMENDED: Update to $LATEST_VERSION (patch/hotfix update)"
            echo "   This is a low-risk update with bug fixes and security patches."

            # Extract download URL
            local DOWNLOAD_URL=$(echo "$VERSIONS_JSON" | grep -A 50 "\"name\":\"$SAME_VERSION_GROUP\"" | grep -A 300 "\"name\":\"$LATEST_VERSION\"" | grep -m 1 -o '"url":"[^"]*' | sed 's/"url":"//')

            UPGRADE_OPTIONS[1]="$LATEST_VERSION|$DOWNLOAD_URL"
        else
            echo "1. No patch/hotfix updates available for $SAME_VERSION_GROUP"
            UPGRADE_OPTIONS[1]=""
        fi

        # 2. Find next minor version upgrade
        local NEXT_MINOR="$CURRENT_MAJOR.$(($CURRENT_MINOR + 1))"
        local NEXT_MINOR_EXISTS=$(echo "$VERSIONS_JSON" | grep -o "\"name\":\"$NEXT_MINOR\"" | wc -l)

        if [ "$NEXT_MINOR_EXISTS" -gt 0 ]; then
            local NEXT_MINOR_LATEST_PATCH=$(echo "$VERSIONS_JSON" | grep -A 5 "\"name\":\"$NEXT_MINOR\"" | grep -o '"latestPatchVersion":[0-9]\+' | sed 's/"latestPatchVersion"://')
            local NEXT_MINOR_VERSION="$NEXT_MINOR.$NEXT_MINOR_LATEST_PATCH"

            # Extract download URL
            local DOWNLOAD_URL=$(echo "$VERSIONS_JSON" | grep -A 5 "\"name\":\"$NEXT_MINOR\"" | grep -o '"latestStableUrl":"[^"]*' | sed 's/"latestStableUrl":"//')

            echo "2. Update to $NEXT_MINOR_VERSION (minor version upgrade)"
            echo "   This update includes new features along with bug fixes and improvements."
            echo "   May require database schema updates."

            UPGRADE_OPTIONS[2]="$NEXT_MINOR_VERSION|$DOWNLOAD_URL"
        else
            echo "2. No minor version upgrade available"
            UPGRADE_OPTIONS[2]=""
        fi

        # 3. Find latest stable version
        local LATEST_SUPPORTED=$(echo "$VERSIONS_JSON" | grep -A 5 '"supported":true' | grep -o '"name":"[0-9]\+\.[0-9]\+"' | sed 's/"name":"//;s/"//' | sort -V | tail -1)

        if [ -n "$LATEST_SUPPORTED" ]; then
            local LATEST_VERSION_PATCH=$(echo "$VERSIONS_JSON" | grep -A 5 "\"name\":\"$LATEST_SUPPORTED\"" | grep -o '"latestPatchVersion":[0-9]\+' | sed 's/"latestPatchVersion"://')
            local LATEST_STABLE="$LATEST_SUPPORTED.$LATEST_VERSION_PATCH"

            # Extract download URL
            local DOWNLOAD_URL=$(echo "$VERSIONS_JSON" | grep -A 5 "\"name\":\"$LATEST_SUPPORTED\"" | grep -o '"latestStableUrl":"[^"]*' | sed 's/"latestStableUrl":"//')

            if [ "$LATEST_SUPPORTED" != "$SAME_VERSION_GROUP" ] && [ "$LATEST_SUPPORTED" != "$NEXT_MINOR" ]; then
                echo "3. Update to $LATEST_STABLE (major upgrade - CAUTION)"
                echo "   This is the latest stable version with major new features."
                echo "   Major upgrades require careful testing and may have significant changes."

                UPGRADE_OPTIONS[3]="$LATEST_STABLE|$DOWNLOAD_URL"
            else
                echo "3. No major upgrade available (current version is in latest supported track)"
                UPGRADE_OPTIONS[3]=""
            fi
        else
            echo "3. Could not determine latest stable version"
            UPGRADE_OPTIONS[3]=""
        fi
    else
        echo "Error: Required command-line tools (grep, sed) not found."
        return 1
    fi

    # Select upgrade option
    echo
    read -p "Select upgrade option (1-3) or any other key to exit: " OPTION

    if [[ "$OPTION" =~ ^[1-3]$ ]] && [ -n "${UPGRADE_OPTIONS[$OPTION]}" ]; then
        # Parse selected option
        local SELECTED_VERSION=$(echo "${UPGRADE_OPTIONS[$OPTION]}" | cut -d'|' -f1)
        local DOWNLOAD_URL=$(echo "${UPGRADE_OPTIONS[$OPTION]}" | cut -d'|' -f2)

        echo "You've selected to upgrade to version $SELECTED_VERSION"
        _perform_upgrade "$SELECTED_VERSION" "$DOWNLOAD_URL"
    else
        echo "Upgrade aborted or invalid option selected."
        exit 0
    fi
}

# Upgrade to manually specified version
_manual_version_upgrade() {
    local VERSIONS_JSON=$(_fetch_available_versions)
    if [ $? -ne 0 ]; then
        return 1
    fi

    # Display available versions first
    _show_available_versions

    echo
    echo "Enter the specific version you want to install (e.g., 2.38.1):"
    read -p "Version: " TARGET_VERSION

    if [ -z "$TARGET_VERSION" ]; then
        echo "No version specified. Upgrade aborted."
        exit 1
    fi

    # Find the download URL for this version
    local DOWNLOAD_URL=""
    if command -v grep > /dev/null && command -v sed > /dev/null; then
        DOWNLOAD_URL=$(echo "$VERSIONS_JSON" | grep -A 300 "\"name\":\"$TARGET_VERSION\"" | grep -m 1 -o '"url":"[^"]*' | sed 's/"url":"//')
    fi

    if [ -z "$DOWNLOAD_URL" ]; then
        echo "Error: Could not find download URL for version $TARGET_VERSION"
        echo "Check if the version number is correct."
        exit 1
    fi

    _perform_upgrade "$TARGET_VERSION" "$DOWNLOAD_URL"
}

# Upgrade with locally built WAR file
_local_build_upgrade() {
    echo "Using locally built WAR file for upgrade."
    echo "This option is primarily for developers."
    echo

    # Warning
    echo "Warning: This option assumes you have already built the WAR file."
    read -p "Have you already built the application? (y/n): " BUILT
    if [[ ! "$BUILT" =~ ^[Yy]$ ]]; then
        echo "Building the application..."
        if [ $VERBOSE -eq 1 ]; then
            mvn clean package -DskipTests --activate-profiles embedded
        else
            mvn clean package -DskipTests --activate-profiles embedded --quiet
        fi

        # Check if build was successful
        if [ $? -ne 0 ]; then
            echo "Build failed. Upgrade aborted."
            exit 1
        fi
    fi

    # Check if the WAR file exists
    if [ ! -f "$DHIS2_WAR_PATH" ]; then
        echo "Error: DHIS2 WAR file not found at $DHIS2_WAR_PATH"
        exit 1
    fi

    # Perform the upgrade without download
    _perform_upgrade "local" ""
}

# Common upgrade procedure
_perform_upgrade() {
    local VERSION="$1"
    local DOWNLOAD_URL="$2"
    local WAR_FILE="dhis.war"
    local TEMP_DIR="$(mktemp -d)"

    echo "============================================"
    echo "          Starting Upgrade Process         "
    echo "============================================"

    # Safety checks
    echo "Performing safety checks..."

    # 1. Check Java version for target DHIS2 version
    if [ "$VERSION" != "local" ]; then
        echo "Checking Java compatibility for DHIS2 version $VERSION..."
        # Use the common function
        _check_java_compatibility "$VERSION"
    else
        # For local upgrade, check current Java version against current DHIS2 version
        _check_java_compatibility
    fi

    # 2. Check if database is in use by other clients
    _check_db_connections

    # 3. Create backup
    echo
    echo "Creating database backup is strongly recommended before upgrading."
    read -p "Do you want to create a database backup? (y/n): " DO_BACKUP
    if [[ "$DO_BACKUP" =~ ^[Yy]$ ]]; then
        _backup_database
        if [ $? -ne 0 ]; then
            echo "Database backup failed. Upgrade aborted."
            exit 1
        fi
    else
        echo "Warning: Proceeding without database backup."
        read -p "Are you sure you want to continue without a backup? (y/n): " CONFIRM
        if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
            echo "Upgrade aborted."
            exit 0
        fi
    fi

    # Stop the server if it's running
    if is_running; then
        echo "Stopping the server before upgrade..."
        stop_server
    fi

    # Create a backup of the current WAR file if it exists
    if [ -f "$DHIS2_WAR_PATH" ]; then
        local TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
        local BACKUP_WAR="${DHIS2_WAR_PATH}.backup_${TIMESTAMP}"
        echo "Creating backup of current WAR file: $BACKUP_WAR"
        cp "$DHIS2_WAR_PATH" "$BACKUP_WAR"
    fi

    # If we have a download URL, download the new version
    if [ -n "$DOWNLOAD_URL" ]; then
        echo "Downloading DHIS2 version $VERSION from $DOWNLOAD_URL..."

        # Use available download tool
        if [ $HAVE_CURL -eq 1 ]; then
            curl -L -o "${TEMP_DIR}/${WAR_FILE}" "$DOWNLOAD_URL"
        elif [ $HAVE_WGET -eq 1 ]; then
            wget -O "${TEMP_DIR}/${WAR_FILE}" "$DOWNLOAD_URL"
        else
            echo "Error: Neither curl nor wget found. Cannot download WAR file."
            rm -rf "$TEMP_DIR"
            exit 1
        fi

        # Check if download was successful
        if [ ! -f "${TEMP_DIR}/${WAR_FILE}" ]; then
            echo "Error: Failed to download WAR file."
            rm -rf "$TEMP_DIR"
            exit 1
        fi

        # Move the downloaded WAR file to the correct location
        echo "Installing new DHIS2 version..."
        mv "${TEMP_DIR}/${WAR_FILE}" "$DHIS2_WAR_PATH"
    else
        # For local builds, the WAR file is already in place
        echo "Using locally built WAR file: $DHIS2_WAR_PATH"
    fi

    # Clean up temp directory
    rm -rf "$TEMP_DIR"

    # Print success message and instructions
    echo "============================================"
    echo "            Upgrade Completed              "
    echo "============================================"
    if [ "$VERSION" != "local" ]; then
        echo "DHIS2 has been upgraded to version $VERSION."
    else
        echo "DHIS2 has been upgraded with the locally built WAR file."
    fi
    echo
    echo "Recommendations:"
    echo "1. Start the server with: $0 start"
    echo "2. Check the logs for any errors with: $0 logs"
    echo "3. If you encounter issues, you can restore the previous version from backup."
    echo
    echo "The previous WAR file was backed up and can be restored if needed."
}

# Check for active database connections
_check_db_connections() {
    echo "Checking for active database connections..."

    # Extract database connection information from dhis.conf
    if [ -f "$DHIS2_HOME_DIR/dhis.conf" ]; then
        local DB_URL=$(grep "^connection.url" "$DHIS2_HOME_DIR/dhis.conf" | cut -d'=' -f2- | tr -d ' ')
        local DB_USERNAME=$(grep "^connection.username" "$DHIS2_HOME_DIR/dhis.conf" | cut -d'=' -f2- | tr -d ' ')
        local DB_PASSWORD=$(grep "^connection.password" "$DHIS2_HOME_DIR/dhis.conf" | cut -d'=' -f2- | tr -d ' ')

        # Check if we have psql available (for PostgreSQL)
        if echo "$DB_URL" | grep -q "postgresql" && [ $HAVE_PSQL -eq 1 ]; then
            # Extract database name from URL
            local DB_NAME=$(echo "$DB_URL" | sed 's/.*\/\([^?]*\).*/\1/')

            echo "Checking active connections to PostgreSQL database '$DB_NAME'..."

            # Connect to database and check for active connections
            local CONNECTIONS=$(PGPASSWORD="$DB_PASSWORD" psql -h localhost -U "$DB_USERNAME" -d "$DB_NAME" -c "SELECT count(*) FROM pg_stat_activity WHERE datname = '$DB_NAME' AND pid <> pg_backend_pid();" -t 2>/dev/null)

            if [ $? -eq 0 ]; then
                CONNECTIONS=$(echo "$CONNECTIONS" | tr -d ' ')

                if [ "$CONNECTIONS" -gt 0 ]; then
                    echo "Warning: $CONNECTIONS active connections detected to the database."
                    echo "It is highly recommended to ensure no other applications are using the database during upgrade."
                    read -p "Do you want to continue anyway? (y/n): " CONTINUE
                    if [[ ! "$CONTINUE" =~ ^[Yy]$ ]]; then
                        echo "Upgrade aborted."
                        exit 0
                    fi
                else
                    echo "No active database connections detected. Safe to proceed."
                fi
            else
                echo "Warning: Could not check active database connections."
                echo "Please ensure no other applications are using the database before continuing."
            fi
        else
            echo "Warning: Could not check database connections (either not PostgreSQL or psql command not available)."
            echo "Please manually ensure no other applications are using the database before continuing."
        fi
    else
        echo "Warning: dhis.conf not found at $DHIS2_HOME_DIR/dhis.conf"
        echo "Could not check database connections. Please ensure no other applications are using the database."
    fi
}

# Backup the database
_backup_database() {
    echo "Creating database backup..."

    # Extract database connection information from dhis.conf
    if [ -f "$DHIS2_HOME_DIR/dhis.conf" ]; then
        local DB_URL=$(grep "^connection.url" "$DHIS2_HOME_DIR/dhis.conf" | cut -d'=' -f2- | tr -d ' ')
        local DB_USERNAME=$(grep "^connection.username" "$DHIS2_HOME_DIR/dhis.conf" | cut -d'=' -f2- | tr -d ' ')
        local DB_PASSWORD=$(grep "^connection.password" "$DHIS2_HOME_DIR/dhis.conf" | cut -d'=' -f2- | tr -d ' ')

        # For PostgreSQL
        if echo "$DB_URL" | grep -q "postgresql" && [ $HAVE_PG_DUMP -eq 1 ]; then
            # Extract database name from URL
            local DB_NAME=$(echo "$DB_URL" | sed 's/.*\/\([^?]*\).*/\1/')
            local TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
            local BACKUP_FILE="$SCRIPT_DIR/dhis2_backup_${DB_NAME}_${TIMESTAMP}.sql"

            echo "Creating PostgreSQL backup of database '$DB_NAME' to $BACKUP_FILE..."

            # Use pg_dump to create backup
            PGPASSWORD="$DB_PASSWORD" pg_dump -h localhost -U "$DB_USERNAME" -d "$DB_NAME" -f "$BACKUP_FILE" 2>/dev/null

            if [ $? -eq 0 ]; then
                echo "Database backup created successfully: $BACKUP_FILE"

                # Compress the backup if gzip is available
                if [ $HAVE_GZIP -eq 1 ]; then
                    echo "Compressing backup file..."
                    gzip "$BACKUP_FILE"
                    echo "Compressed backup file: ${BACKUP_FILE}.gz"
                fi

                return 0
            else
                echo "Error: Database backup failed."
                return 1
            fi
        else
            echo "Warning: Could not create database backup (either not PostgreSQL or pg_dump command not available)."
            echo "Please create a manual backup of your database before continuing."

            read -p "Do you confirm that you've created a manual backup? (y/n): " BACKUP_CONFIRM
            if [[ "$BACKUP_CONFIRM" =~ ^[Yy]$ ]]; then
                return 0
            else
                return 1
            fi
        fi
    else
        echo "Warning: dhis.conf not found at $DHIS2_HOME_DIR/dhis.conf"
        echo "Could not create database backup automatically. Please ensure you have a backup."

        read -p "Do you confirm that you've created a manual backup? (y/n): " BACKUP_CONFIRM
        if [[ "$BACKUP_CONFIRM" =~ ^[Yy]$ ]]; then
            return 0
        else
            return 1
        fi
    fi
}

# Install DHIS2 as a systemd service
install_systemd_service() {
    echo "============================================"
    echo "      DHIS2 Systemd Service Installation   "
    echo "============================================"

    # Check if running as root/sudo
    if [ "$EUID" -ne 0 ]; then
        echo "Error: This command must be run with sudo or as root."
        echo "Please run: sudo $0 install-service"
        exit 1
    fi

    # Get the absolute path of the script
    local SCRIPT_PATH=$(readlink -f "$0")
    local SCRIPT_DIR=$(dirname "$SCRIPT_PATH")

    # Create a name for the service
    read -p "Enter a name for the service [dhis2]: " SERVICE_NAME
    SERVICE_NAME=${SERVICE_NAME:-dhis2}

    # Ask for user to run the service as
    read -p "Enter the user to run the service as [$(whoami)]: " SERVICE_USER
    SERVICE_USER=${SERVICE_USER:-$(whoami)}

    # Validate user exists
    if ! id "$SERVICE_USER" &>/dev/null; then
        echo "Error: User $SERVICE_USER does not exist."
        exit 1
    fi

    # Warning if root user is selected
    if [ "$SERVICE_USER" = "root" ]; then
        echo "========================================================================="
        echo "                           ⚠️  WARNING  ⚠️                             "
        echo "========================================================================="
        echo "You have chosen to run the DHIS2 service as the root user."
        echo "Running services as root is strongly discouraged for security reasons."
        echo "If the application is compromised, the attacker would have full system access."
        echo
        echo "It is strongly recommended to use a dedicated non-privileged user instead."
        echo "========================================================================="
        read -p "Are you absolutely sure you want to continue with root user? (yes/no): " ROOT_CONFIRM
        if [[ ! "$ROOT_CONFIRM" = "yes" ]]; then
            echo "Installation aborted. Please restart with a different user."
            exit 1
        fi
    fi

    # Check directory permissions
    echo "Checking directory permissions for user $SERVICE_USER..."

    # 1. Check read access to DHIS2_HOME
    if ! runuser -u "$SERVICE_USER" -- test -r "$DHIS2_HOME_DIR"; then
        echo "Error: User $SERVICE_USER cannot read the DHIS2_HOME directory ($DHIS2_HOME_DIR)."
        echo "Please grant read permissions to this directory for user $SERVICE_USER:"
        echo "  sudo setfacl -m u:$SERVICE_USER:rx $DHIS2_HOME_DIR"
        echo "  OR"
        echo "  sudo chmod g+rx $DHIS2_HOME_DIR # (if $SERVICE_USER is in the same group)"
        exit 1
    fi

    # 2. Check for logs directory
    local LOGS_DIR="$DHIS2_HOME_DIR/logs"
    if [ ! -d "$LOGS_DIR" ]; then
        echo "Creating logs directory at $LOGS_DIR..."
        mkdir -p "$LOGS_DIR"
        chown "$SERVICE_USER" "$LOGS_DIR"
        chmod 750 "$LOGS_DIR"
        echo "Logs directory created and permissions set."
    else
        # Check write access to logs
        if ! runuser -u "$SERVICE_USER" -- test -w "$LOGS_DIR"; then
            echo "Warning: User $SERVICE_USER cannot write to the logs directory ($LOGS_DIR)."
            echo "DHIS2 needs write access to this directory to store application logs."
            read -p "Do you want to fix this permission now? (y/n): " FIX_LOGS
            if [[ "$FIX_LOGS" =~ ^[Yy]$ ]]; then
                chown "$SERVICE_USER" "$LOGS_DIR"
                chmod 750 "$LOGS_DIR"
                echo "Permissions updated for logs directory."
            else
                echo "Please manually ensure $SERVICE_USER can write to $LOGS_DIR."
            fi
        fi
    fi

    # 3. Check for files directory
    local FILES_DIR="$DHIS2_HOME_DIR/files"
    if [ ! -d "$FILES_DIR" ]; then
        echo "Creating files directory at $FILES_DIR..."
        mkdir -p "$FILES_DIR"
        chown "$SERVICE_USER" "$FILES_DIR"
        chmod 750 "$FILES_DIR"
        echo "Files directory created and permissions set."
    else
        # Check write access to files
        if ! runuser -u "$SERVICE_USER" -- test -w "$FILES_DIR"; then
            echo "Warning: User $SERVICE_USER cannot write to the files directory ($FILES_DIR)."
            echo "DHIS2 needs write access to this directory to store uploaded files."
            read -p "Do you want to fix this permission now? (y/n): " FIX_FILES
            if [[ "$FIX_FILES" =~ ^[Yy]$ ]]; then
                chown "$SERVICE_USER" "$FILES_DIR"
                chmod 750 "$FILES_DIR"
                echo "Permissions updated for files directory."
            else
                echo "Please manually ensure $SERVICE_USER can write to $FILES_DIR."
            fi
        fi
    fi

    # 4. Check if user has write permissions to DHIS2_HOME itself
    if runuser -u "$SERVICE_USER" -- test -w "$DHIS2_HOME_DIR"; then
        echo "Warning: User $SERVICE_USER has write access to the DHIS2_HOME directory."
        echo "This is not recommended as it contains configuration files like dhis.conf"
        echo "that should not be modified by the running service."
        read -p "Do you want to restrict write permissions to DHIS2_HOME? (y/n): " FIX_HOME
        if [[ "$FIX_HOME" =~ ^[Yy]$ ]]; then
            # Keep execution and read permissions, remove write
            chmod o-w "$DHIS2_HOME_DIR"
            echo "Write permissions to DHIS2_HOME have been restricted."
        fi
    fi

    # Get description
    read -p "Enter a description for the service [DHIS2 Server]: " SERVICE_DESC
    SERVICE_DESC=${SERVICE_DESC:-"DHIS2 Server"}

    # Create systemd service file
    local SERVICE_FILE="/etc/systemd/system/${SERVICE_NAME}.service"

    echo "Creating systemd service file at $SERVICE_FILE..."

    cat > "$SERVICE_FILE" << EOL
[Unit]
Description=${SERVICE_DESC}
After=network.target postgresql.service

[Service]
Type=simple
User=${SERVICE_USER}
Environment="DHIS2_HOME=${DHIS2_HOME_DIR}"
Environment="DHIS2_PORT=${DHIS2_PORT}"
WorkingDirectory=${SCRIPT_DIR}
ExecStart=${SCRIPT_PATH} start
ExecStop=${SCRIPT_PATH} stop
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOL

    # Set permissions
    chmod 644 "$SERVICE_FILE"

    # Reload systemd daemon
    echo "Reloading systemd daemon..."
    systemctl daemon-reload

    # Enable the service
    echo "Enabling service to start on boot..."
    systemctl enable "$SERVICE_NAME.service"

    # Ask to start the service now
    read -p "Do you want to start the service now? (y/n): " START_NOW
    if [[ "$START_NOW" =~ ^[Yy]$ ]]; then
        echo "Starting $SERVICE_NAME service..."
        systemctl start "$SERVICE_NAME.service"
        echo "Service status:"
        systemctl status "$SERVICE_NAME.service"
    fi

    echo "============================================"
    echo "      Service Installation Complete        "
    echo "============================================"
    echo "Service name: $SERVICE_NAME"
    echo "Service file: $SERVICE_FILE"
    echo "Service user: $SERVICE_USER"
    echo
    echo "You can manage the service with these commands:"
    echo "  sudo systemctl start $SERVICE_NAME    # Start the service"
    echo "  sudo systemctl stop $SERVICE_NAME     # Stop the service"
    echo "  sudo systemctl restart $SERVICE_NAME  # Restart the service"
    echo "  sudo systemctl status $SERVICE_NAME   # Check service status"
    echo "  sudo systemctl disable $SERVICE_NAME  # Disable auto-start on boot"
    echo "  sudo journalctl -u $SERVICE_NAME      # View service logs"
}

# Check server health via the health endpoint
check_health() {
    echo "Checking DHIS2 server health..."

    local HEALTH_URL="http://localhost:${DHIS2_PORT}/health"
    local HTTP_CODE
    local RESPONSE

    if is_running; then
        echo "Server is running, checking health endpoint: ${HEALTH_URL}"

        # Use available HTTP client
        if [ $HAVE_CURL -eq 1 ]; then
            # Use curl with timeout, follow redirects, and capture both response and HTTP code
            RESPONSE=$(curl -s -o - -w "%{http_code}" -m 10 -L "${HEALTH_URL}")
            HTTP_CODE=${RESPONSE: -3}
            RESPONSE=${RESPONSE%???}

            if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "202" ]; then
                echo "Health check successful (HTTP ${HTTP_CODE})"
                echo "Response: ${RESPONSE}"
                return 0
            else
                echo "Health check failed with HTTP code: ${HTTP_CODE}"
                echo "Response: ${RESPONSE}"
                return 1
            fi
        elif [ $HAVE_WGET -eq 1 ]; then
            # Use wget with timeout
            RESPONSE=$(wget -q -O - --timeout=10 --tries=1 "${HEALTH_URL}" 2>&1)
            if [ $? -eq 0 ]; then
                echo "Health check successful"
                echo "Response: ${RESPONSE}"
                return 0
            else
                echo "Health check failed"
                echo "Response: ${RESPONSE}"
                return 1
            fi
        else
            echo "Error: Neither curl nor wget is available. Cannot perform health check."
            return 1
        fi
    else
        echo "Server is not running. Start the server first with: $0 start"
        return 1
    fi
}

# Parse command line options
parse_options() {
    local OPTIND

    while getopts "d:p:j:hv-:" opt; do
        # Support for long options
        if [ "$opt" = "-" ]; then
            opt="${OPTARG%%=*}"
            OPTARG="${OPTARG#$opt}"
            OPTARG="${OPTARG#=}"

            case "$opt" in
                home)
                    DHIS2_HOME_DIR="$OPTARG"
                    ;;
                port)
                    DHIS2_PORT="$OPTARG"
                    ;;
                java)
                    JAVA_CMD="$OPTARG"
                    ;;
                help)
                    show_help
                    exit 0
                    ;;
                verbose)
                    VERBOSE=1
                    ;;
                *)
                    echo "Unknown option: --$opt"
                    show_help
                    exit 1
                    ;;
            esac
        else
            case "$opt" in
                d)
                    DHIS2_HOME_DIR="$OPTARG"
                    ;;
                p)
                    DHIS2_PORT="$OPTARG"
                    ;;
                j)
                    JAVA_CMD="$OPTARG"
                    ;;
                h)
                    show_help
                    exit 0
                    ;;
                v)
                    VERBOSE=1
                    ;;
                *)
                    echo "Unknown option: -$opt"
                    show_help
                    exit 1
                    ;;
            esac
        fi
    done

    shift $((OPTIND-1))
    COMMAND="$1"
    shift || true
    COMMAND_ARGS=("$@")
}

# Show help information
show_help() {
    cat << EOF
DHIS2 Server Management Script

Usage: $0 [command] [options]

Commands:
  start    - Start the DHIS2 server
  stop     - Stop the DHIS2 server
  restart  - Restart the DHIS2 server
  status   - Check the status of the DHIS2 server
  health   - Check the health of the server via the health endpoint
  logs     - View the server logs (use 'logs N' to show last N lines)
  version  - Display the DHIS2 version
  upgrade  - Smart upgrade for the DHIS2 server with the following features:
              * Automatic version detection
              * Selection of upgrade paths (patch, minor, major)
              * Database backup and safety checks
              * WAR file backup for rollback capability
              * Download of new versions from official DHIS2 repositories
  install-service - Install DHIS2 as a systemd service (requires root/sudo)

Options:
  -d, --home DIR   Set DHIS2 home directory (default: /opt/dhis2)
  -p, --port PORT  Set the server port (default: 8080)
  -j, --java JAVA  Set the Java executable path
  -h, --help       Display this help message
  -v, --verbose    Enable verbose output

Examples:
  $0 start -d /path/to/dhis2_home -p 8080
  $0 stop
  $0 logs 100     # Show last 100 lines of logs
  $0 status
  $0 health       # Check the health endpoint
  $0 upgrade      # Run the smart upgrade process
  sudo $0 install-service  # Install as systemd service

Java Version Requirements:
  - DHIS2 versions 2.41 and above require Java 17 or higher
  - DHIS2 versions below 2.41 require exactly Java 8 (not higher or lower)

Important Notes for Upgrading:
  - Always backup your database before upgrading
  - Ensure no users are connected to the database during upgrade
  - For major version upgrades, test on a staging environment first
  - The upgrade process automatically creates a backup of the current WAR file
EOF
}

# Set DHIS2 home from environment variable if set
if [ -n "$DHIS2_HOME" ]; then
    DHIS2_HOME_DIR="$DHIS2_HOME"
fi

# Set from environment variable if set
if [ -n "$DHIS2_PORT" ]; then
    DHIS2_PORT="$DHIS2_PORT"
fi

# Parse command line options
parse_options "$@"

# Set verbose output if needed
if [ $VERBOSE -eq 1 ]; then
    set -x
fi

# Process commands
case "$COMMAND" in
    start)
        start_server
        ;;
    stop)
        stop_server
        ;;
    restart)
        restart_server
        ;;
    status)
        check_required_tools "status" || exit 1
        check_status
        ;;
    health)
        check_required_tools "health" || exit 1
        check_health
        ;;
    logs)
        check_required_tools "logs" || exit 1
        view_logs "${COMMAND_ARGS[0]}"
        ;;
    version)
        check_required_tools "version" || exit 1
        get_version
        ;;
    upgrade)
        check_required_tools "upgrade" || exit 1
        upgrade_server
        ;;
    install-service)
        install_systemd_service
        ;;
    "")
        echo "Error: Command required."
        show_help
        exit 1
        ;;
    *)
        echo "Error: Unknown command: $COMMAND"
        show_help
        exit 1
        ;;
esac

exit 0