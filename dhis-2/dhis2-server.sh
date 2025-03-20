#!/usr/bin/env bash


set -x
set -e

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
#   logs     - View the server logs (use 'logs N' to show last N lines)
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
# the upgrade_server wont work **in** production.

set -e

# Default configuration
DHIS2_HOME_DIR="/opt/dhis2"
DHIS2_PORT=8080
JAVA_CMD="java"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERBOSE=0
USE_PARAM_HOME=false  # Flag to track if -d parameter was used

# Initialize path-dependent variables
initialize_paths() {
    # Create standard directories within DHIS2_HOME_DIR
    DHIS2_LOGS_DIR="$DHIS2_HOME_DIR/logs"
    DHIS2_RUN_DIR="$DHIS2_HOME_DIR/run"

    # Define files using these directories
    PID_FILE="$DHIS2_RUN_DIR/dhis2-server.pid"
    LOG_FILE="$DHIS2_LOGS_DIR/dhis.log"
    DHIS2_WAR_PATH="$SCRIPT_DIR/dhis-web-server/target/dhis.war"

    # Create necessary directories
    mkdir -p "$DHIS2_LOGS_DIR" "$DHIS2_RUN_DIR" 2>/dev/null || true
}

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
HAVE_JQ=0

# Check if required tools are available for the given command
# Usage: check_required_tools command
check_required_tools() {
    local command=$1
    local missing_tools=()
    local required_tools=()

    # Define required tools for each command
    case "$command" in
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

# Common function to check Java compatibility
# Usage: check_java_compatibility [target_version]
# If target_version is not provided, the current version will be detected
check_java_compatibility() {
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

    # For non-upgrade operations, we use default modern version requirement
    REQUIRED_JAVA=$JAVA_MIN_VERSION_MODERN

    # Check if Java version meets requirements
    if [ "$JAVA_VERSION" -lt "$REQUIRED_JAVA" ]; then
        echo "Error: Java version $JAVA_VERSION is not supported."
        echo "DHIS2 requires Java $REQUIRED_JAVA or higher."
        echo "Please install a compatible Java version and/or set the correct path with --java option."
        [ "$EXIT_ON_ERROR" = "true" ] && exit 1
        return 1
    fi

    echo "Java version check passed. Using Java $JAVA_VERSION."
    return 0
}

# Start DHIS2 server
start_server() {
    if is_running; then
        echo "DHIS2 server is already running with PID $(cat "$PID_FILE")"
        return
    fi

    check_dhis2_home
    check_war_file
    check_java_compatibility

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
    echo "============================================"
    echo "        DHIS2 Server Smart Upgrade         "
    echo "============================================"

    # Check if Python is available
    if ! command -v python3 &> /dev/null; then
        echo "Error: Python 3 is required for the upgrade functionality."
        echo "Please install Python 3 and try again."
        exit 1
    fi

    # Check if the upgrade script exists
    PYTHON_UPGRADE_SCRIPT="$SCRIPT_DIR/dhis2_upgrade.py"
    if [ ! -f "$PYTHON_UPGRADE_SCRIPT" ]; then
        echo "Error: Upgrade script not found at $PYTHON_UPGRADE_SCRIPT"
        exit 1
    fi

    # Make the script executable if it's not already
    chmod +x "$PYTHON_UPGRADE_SCRIPT"

    # Run the Python upgrade script with all necessary parameters
    python3 "$PYTHON_UPGRADE_SCRIPT" \
        --dhis2-home "$DHIS2_HOME_DIR" \
        --war-path "$DHIS2_WAR_PATH" \
        --pid-file "$PID_FILE" \
        --java "$JAVA_CMD" \
        --script-dir "$SCRIPT_DIR" \
        --pom-path "$SCRIPT_DIR/pom.xml"

    # Return the exit code from the Python script
    return $?
}

# Display available versions
_show_available_versions() {
    echo "Fetching and displaying available DHIS2 versions..."

    local TEMP_FILE=$(_fetch_available_versions)
    if [ $? -ne 0 ]; then
        return 1
    fi

    echo "============================================"
    echo "         Available DHIS2 Versions          "
    echo "============================================"

    # Process with jq if available
    if [ $HAVE_JQ -eq 1 ]; then
        echo "Supported Versions:"
        echo "-----------------"
        jq -r '.versions[] | select(.supported == true) | .name' "$TEMP_FILE" | sort -Vr | while read -r VERSION; do
            # Get version details using jq
            VERSION_DATA=$(jq -r --arg ver "$VERSION" '.versions[] | select(.name == $ver)' "$TEMP_FILE")
            LATEST_PATCH=$(echo "$VERSION_DATA" | jq -r '.latestPatchVersion')
            RELEASE_DATE=$(echo "$VERSION_DATA" | jq -r '.releaseDate')
            JAVA_VERSION=$(echo "$VERSION_DATA" | jq -r '.jdk')

            echo -n "  • DHIS2 $VERSION"
            if [ "$LATEST_PATCH" != "null" ]; then
                echo -n " (Latest patch version: $VERSION.$LATEST_PATCH)"
            fi
            echo -n " - Released: $RELEASE_DATE"

            if [ "$JAVA_VERSION" != "null" ]; then
                echo " - Requires Java $JAVA_VERSION"
            elif [ "$(echo "$VERSION" | cut -d. -f2)" -ge 41 ]; then
                echo " - Requires Java 17+"
            else
                echo " - Requires Java 8"
            fi
        done

        echo
        echo "End-of-Support Versions:"
        echo "---------------------"
        jq -r '.versions[] | select(.supported == false) | .name' "$TEMP_FILE" | sort -Vr | while read -r VERSION; do
            # Get version details using jq
            VERSION_DATA=$(jq -r --arg ver "$VERSION" '.versions[] | select(.name == $ver)' "$TEMP_FILE")
            RELEASE_DATE=$(echo "$VERSION_DATA" | jq -r '.releaseDate')
            JAVA_VERSION=$(echo "$VERSION_DATA" | jq -r '.jdk')

            echo -n "  • DHIS2 $VERSION - Released: $RELEASE_DATE"

            if [ "$JAVA_VERSION" != "null" ]; then
                echo " - Requires Java $JAVA_VERSION"
            elif [ "$(echo "$VERSION" | cut -d. -f2)" -ge 41 ]; then
                echo " - Requires Java 17+"
            else
                echo " - Requires Java 8"
            fi
        done
    # Fallback to grep and sed if jq is not available
    elif [ $HAVE_GREP -eq 1 ] && [ $HAVE_SED -eq 1 ]; then
        echo "Warning: jq not available, using limited version information."
        grep -o '"name":"[0-9]\+\.[0-9]\+"' "$TEMP_FILE" | sed 's/"name":"//;s/"//' | sort -V | while read -r VERSION; do
            local LATEST=$(grep -A 5 "\"name\":\"$VERSION\"" "$TEMP_FILE" | grep -o '"latestPatchVersion":[0-9]\+' | sed 's/"latestPatchVersion"://')
            local SUPPORTED=$(grep -A 5 "\"name\":\"$VERSION\"" "$TEMP_FILE" | grep -o '"supported":true' | wc -l)

            if [ "$SUPPORTED" -eq 1 ]; then
                echo "  $VERSION (Latest: $VERSION.$LATEST) - SUPPORTED"
            else
                echo "  $VERSION (Latest: $VERSION.$LATEST)"
            fi
        done
    else
        echo "Error: Required command-line tools (grep, sed or jq) not found."
        rm -f "$TEMP_FILE"
        return 1
    fi

    # Clean up
    rm -f "$TEMP_FILE"
}

# Fetch available versions from DHIS2 release server
_fetch_available_versions() {
    # Create temp file for processing
    local TEMP_FILE=$(mktemp)

    # Display status with stderr to avoid capturing in command substitution
    echo "Fetching available DHIS2 versions..." >&2

    # Download the JSON data directly to a temp file
    if [ $HAVE_CURL -eq 1 ]; then
        curl -s "https://releases.dhis2.org/v1/versions/stable.json" > "$TEMP_FILE"
    elif [ $HAVE_WGET -eq 1 ]; then
        wget -q -O "$TEMP_FILE" "https://releases.dhis2.org/v1/versions/stable.json"
    else
        echo "Error: Neither curl nor wget found. Cannot fetch available versions." >&2
        rm -f "$TEMP_FILE"
        return 1
    fi

    # Check if file has content
    if [ ! -s "$TEMP_FILE" ]; then
        echo "Error: Failed to download version information." >&2
        rm -f "$TEMP_FILE"
        return 1
    fi

    # Return the path to the temp file - caller is responsible for cleanup
    echo "$TEMP_FILE"
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
                    USE_PARAM_HOME=true
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
                    USE_PARAM_HOME=true
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
              * Requires Python 3 to be installed
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

Python Requirements:
  - The upgrade functionality requires Python 3 to be installed
  - No additional Python packages are needed

Important Notes for Upgrading:
  - Always backup your database before upgrading
  - Ensure no users are connected to the database during upgrade
  - For major version upgrades, test on a staging environment first
  - The upgrade process automatically creates a backup of the current WAR file
EOF
}

# Parse command line options
parse_options "$@"

# Use DHIS2_HOME env variable if -d parameter was not specified
if [ "$USE_PARAM_HOME" = false ] && [ -n "$DHIS2_HOME" ]; then
    DHIS2_HOME_DIR="$DHIS2_HOME"
fi

# Use DHIS2_PORT env variable if set (always respects env var for port)
if [ -n "$DHIS2_PORT" ]; then
    DHIS2_PORT="$DHIS2_PORT"
fi

# Initialize path-dependent variables now that HOME_DIR is set
initialize_paths

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

# python3 /Users/netromsb/develop/dhis2/dhis2-core/dhis-2/dhis2_upgrade.py --dhis2-home /opt/dhis2 --war-path /Users/netromsb/develop/dhis2/dhis2-core/dhis-2/dhis-web-server/target/dhis.war --pid-file /opt/dhis2/run/dhis2-server.pid --java java --script-dir /Users/netromsb/develop/dhis2/dhis2-core/dhis-2 --pom-path /Users/netromsb/develop/dhis2/dhis2-core/dhis-2/pom.xml