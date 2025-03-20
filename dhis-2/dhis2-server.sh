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
#   logs     - View the server logs
#   version  - Display the DHIS2 version
#   upgrade  - Upgrade the DHIS2 server (requires additional parameters)
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
LOG_FILE="$SCRIPT_DIR/dhis2-server.log"
VERBOSE=0
DHIS2_WAR_PATH="$SCRIPT_DIR/dhis-web-server/target/dhis.war"

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

# Start DHIS2 server
start_server() {
    if is_running; then
        echo "DHIS2 server is already running with PID $(cat "$PID_FILE")"
        return
    fi

    check_dhis2_home
    check_war_file

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
        if command -v ps > /dev/null && command -v awk > /dev/null; then
            local START_TIME=$(ps -p "$PID" -o lstart= 2>/dev/null)
            if [ -n "$START_TIME" ]; then
                echo "Started at: $START_TIME"
            fi
        fi

        # Try to get the port it's listening on
        if command -v lsof > /dev/null; then
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

    if command -v tail > /dev/null; then
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
        if command -v grep > /dev/null && command -v sed > /dev/null; then
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
    echo "Upgrading DHIS2 server..."

    # Check if the server is running
    if is_running; then
        echo "Stopping the server before upgrade..."
        stop_server
    fi

    # Build the application
    echo "Building latest version..."
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

    echo "DHIS2 server upgraded successfully."
    echo "You can now start the server with: $0 start"
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
  logs     - View the server logs (use 'logs N' to show last N lines)
  version  - Display the DHIS2 version
  upgrade  - Upgrade the DHIS2 server

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
        check_status
        ;;
    logs)
        view_logs "${COMMAND_ARGS[0]}"
        ;;
    version)
        get_version
        ;;
    upgrade)
        upgrade_server
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