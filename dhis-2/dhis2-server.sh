#!/usr/bin/env bash

#set -x
set -e

# DHIS2 Server Management Script
#
# A CLI tool to start, stop, restart and monitor the DHIS2 embedded Tomcat server
#
# Usage: ./dhis2-server.sh [command] [options]
#
# Commands:
#   install  - Install a DHIS2 version from the official releases
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
DHIS2_RELEASES_URL="https://releases.dhis2.org/v1/versions/stable.json"

# Initialize path-dependent variables
initialize_paths() {
    # Create standard directories within DHIS2_HOME_DIR
    DHIS2_LOGS_DIR="$DHIS2_HOME_DIR/logs"
    DHIS2_RUN_DIR="$DHIS2_HOME_DIR/run"

    # Define files using these directories
    PID_FILE="$DHIS2_RUN_DIR/dhis2-server.pid"
    LOG_FILE="$DHIS2_LOGS_DIR/dhis.log"

    # Check for WAR file in different locations
    if [ -f "$SCRIPT_DIR/dhis.war" ]; then
        # Use downloaded WAR if available
        DHIS2_WAR_PATH="$SCRIPT_DIR/dhis.war"
    else
        # Use built WAR from target directory
        DHIS2_WAR_PATH="$SCRIPT_DIR/dhis-web-server/target/dhis.war"
    fi

    # Create necessary directories
    mkdir -p "$DHIS2_LOGS_DIR" "$DHIS2_RUN_DIR" 2>/dev/null || true

    # Create dhis-web-server/target directory if it doesn't exist
    # This is needed for installations where the WAR file is not built but downloaded
    mkdir -p "$SCRIPT_DIR/dhis-web-server/target" 2>/dev/null || true
}

# Java version requirements
JAVA_MIN_VERSION_LEGACY=8   # For DHIS2 <= 2.37
JAVA_MIN_VERSION_MIDDLE=11  # For DHIS2 >= 2.38 and < 2.41
JAVA_MIN_VERSION_MODERN=17  # For DHIS2 >= 2.41
DHIS2_VERSION_THRESHOLD_MIDDLE="2.38"
DHIS2_VERSION_THRESHOLD_MODERN="2.41"

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
        install)
            required_tools=("curl|wget")
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
    command -v jq > /dev/null && HAVE_JQ=1

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

# Function to parse the document and list versions using JSON.sh
list_dhis2_versions() {
  local file="$1"
  local show_list=${2:-true}  # Whether to print the list

  # Array to store version information
  declare -a versions_info

  # Process JSON.sh output and extract version info with awk
  while IFS=$'\t' read -r name releaseDate supported latestStableUrl; do
    versions_info+=("$name	$releaseDate	$supported	$latestStableUrl")
  done < <(./JSON.sh < "$file" | awk '
  BEGIN {
    FS = "\t"
  }
  {
    # Remove quotes from path
    gsub(/"/, "", $1)
    # Split path into array
    split($1, path, ",")
    # Remove [ and ] from path elements
    for (i in path) {
      gsub(/[\[\]]/, "", path[i])
    }
    len = length(path)
    # Version object fields (path length 3)
    if (len == 3 && path[1] == "versions") {
      ver_index = path[2]
      ver_field = path[3]
      if (ver_field == "supported") {
        versions[ver_index,"supported"] = $2
      } else if (ver_field == "name" || ver_field == "releaseDate" || ver_field == "latestStableUrl") {
        # Remove quotes from string values
        value = substr($2, 2, length($2)-2)
        versions[ver_index,ver_field] = value
      }
      # Track version indices when name is found
      if (ver_field == "name") {
        indices[ver_index] = 1
      }
    }
    # Patch version fields (path length 5)
    else if (len == 5 && path[1] == "versions" && path[3] == "patchVersions") {
      ver_index = path[2]
      patch_index = path[4]
      ver_field = path[5]
      if (ver_field == "name" || ver_field == "releaseDate") {
        # Remove quotes from string values
        value = substr($2, 2, length($2)-2)
        patches[ver_index,patch_index,ver_field] = value
      }
      # Update maximum patch index (force numeric comparison)
      if ((patch_index + 0) > (max_patch[ver_index] + 0)) {
        max_patch[ver_index] = patch_index
      }
    }
  }
  END {
    # Process each version
    for (idx in indices) {
      if (max_patch[idx] != "") {
        # Use last patch version if patches exist
        name = patches[idx,max_patch[idx],"name"]
        releaseDate = patches[idx,max_patch[idx],"releaseDate"]
      } else {
        # Use version fields if no patches
        name = versions[idx,"name"]
        releaseDate = versions[idx,"releaseDate"]
      }
      supported = versions[idx,"supported"]
      latestStableUrl = versions[idx,"latestStableUrl"]
      # Output fields tab-separated
      print name "\t" releaseDate "\t" supported "\t" latestStableUrl
    }
  }
  ')

  # Sort by version number in descending order
  sorted_versions=$(printf '%s\n' "${versions_info[@]}" | sort -k1 -r)

  # Store the sorted versions in the global variable for use in other functions
  DHIS2_VERSION_LIST="$sorted_versions"

  if [ "$show_list" = true ]; then
    # Print header and formatted list
    echo "Available DHIS2 versions:"
    echo "------------------------------------------------------------------------------"
    echo "   Version           | Release Date    | Supported"
    echo "------------------------------------------------------------------------------"

    # Print versions with numbers for selection
    local count=1
    echo "$sorted_versions" | while IFS=$'\t' read -r version date supported url; do
      printf " %2d) %-16s | %-15s | %s\n" "$count" "$version" "$date" "$supported"
      count=$((count+1))
    done
    echo "------------------------------------------------------------------------------"
  fi

  return 0
}

# Check if the WAR file exists at the provided path
check_war_file() {
    if [ ! -f "$DHIS2_WAR_PATH" ]; then
        echo "Error: DHIS2 WAR file not found at $DHIS2_WAR_PATH"
        echo "Please either:"
        echo "  1. Build the application using: mvn clean package -DskipTests --activate-profiles embedded"
        echo "  2. Install a DHIS2 version using: $0 install"
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

    # For non-upgrade operations, we extract DHIS2 version if available
    local DHIS2_VERSION=""
    if [ -f "$SCRIPT_DIR/pom.xml" ] && [ $HAVE_GREP -eq 1 ] && [ $HAVE_SED -eq 1 ]; then
        DHIS2_VERSION=$(grep -m 1 "<version>" "$SCRIPT_DIR/pom.xml" | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
    fi

    # Determine required Java version based on DHIS2 version
    REQUIRED_JAVA=$JAVA_MIN_VERSION_MODERN  # Default to latest requirement
    REQUIRED_JAVA_DESC="17 or higher"  # Default description

    if [ -n "$DHIS2_VERSION" ]; then
        # Extract major.minor version (e.g., 2.41 from 2.41.3)
        local MAJOR_MINOR=$(echo "$DHIS2_VERSION" | sed -E 's/^([0-9]+\.[0-9]+).*/\1/')

        # Version comparison using string comparison
        if [[ "$(echo -e "$MAJOR_MINOR\n$DHIS2_VERSION_THRESHOLD_MODERN" | sort -V | head -n1)" = "$MAJOR_MINOR" ]]; then
            # Version is less than threshold for modern Java (< 2.41)
            if [[ "$(echo -e "$MAJOR_MINOR\n$DHIS2_VERSION_THRESHOLD_MIDDLE" | sort -V | head -n1)" = "$MAJOR_MINOR" ]]; then
                # Version is less than middle threshold (< 2.38)
                REQUIRED_JAVA=$JAVA_MIN_VERSION_LEGACY
                REQUIRED_JAVA_DESC="8"
            else
                # Version is between 2.38 and 2.41
                REQUIRED_JAVA=$JAVA_MIN_VERSION_MIDDLE
                REQUIRED_JAVA_DESC="11 or higher"
            fi
        fi
    fi

    # Check if Java version meets requirements
    if [ "$JAVA_VERSION" -lt "$REQUIRED_JAVA" ]; then
        echo "Error: Java version $JAVA_VERSION is not supported."
        echo "DHIS2 version $DHIS2_VERSION requires Java $REQUIRED_JAVA_DESC."
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

# Download a file using available tools
download_file() {
    local url="$1"
    local output_file="$2"
    local success=false

    echo "Downloading $url to $output_file..."

    if [ $HAVE_CURL -eq 1 ]; then
        echo "Using curl to download..."
        if curl -L -o "$output_file" "$url" --progress-bar; then
            success=true
        fi
    elif [ $HAVE_WGET -eq 1 ]; then
        echo "Using wget to download..."
        if wget -O "$output_file" "$url" --show-progress; then
            success=true
        fi
    else
        echo "Error: Neither curl nor wget is available. Cannot download the file."
        return 1
    fi

    if [ "$success" = true ]; then
        echo "Download completed successfully."
        return 0
    else
        echo "Download failed."
        return 1
    fi
}

# Create a backup of a file
backup_file() {
    local file="$1"
    local backup_file="${file}.backup-$(date +%Y%m%d%H%M%S)"

    echo "Creating backup of $file as $backup_file"
    if cp "$file" "$backup_file"; then
        echo "Backup created successfully."
        return 0
    else
        echo "Failed to create backup."
        return 1
    fi
}

# Install DHIS2 from releases website
install_dhis2() {
    echo "============================================"
    echo "      DHIS2 Version Installation           "
    echo "============================================"

    # Check for required tools
    check_required_tools "install" || exit 1

    # Create temporary directory for downloads
    local TEMP_DIR="$SCRIPT_DIR/dhis2-temp"
    mkdir -p "$TEMP_DIR"

    # Download the JSON file with available versions
    local VERSION_JSON="$TEMP_DIR/versions.json"
    echo "Fetching available DHIS2 versions..."
    if ! download_file "$DHIS2_RELEASES_URL" "$VERSION_JSON"; then
        echo "Error: Failed to fetch version information."
        rm -rf "$TEMP_DIR"
        exit 1
    fi

    # Check if JSON.sh is available, if not, download it
    if [ ! -f "$SCRIPT_DIR/JSON.sh" ]; then
        echo "JSON.sh not found, downloading..."
        download_file "https://raw.githubusercontent.com/dominictarr/JSON.sh/master/JSON.sh" "$SCRIPT_DIR/JSON.sh"
        chmod +x "$SCRIPT_DIR/JSON.sh"
    fi

    # Parse and list available versions
    list_dhis2_versions "$VERSION_JSON"

    # Get the latest version (first line of sorted_versions)
    local latest_version=$(echo "$DHIS2_VERSION_LIST" | head -n1 | cut -f1)
    local latest_url=$(echo "$DHIS2_VERSION_LIST" | head -n1 | cut -f4)

    # Ask the user if they want to install the latest version or choose another one
    echo
    echo "The latest version is $latest_version"
    read -p "Do you want to install this version? (Y/n): " install_latest

    local selected_version="$latest_version"
    local download_url="$latest_url"

    if [[ "$install_latest" =~ ^[Nn] ]]; then
        # Ask the user to select a version by number
        echo "Please select a version by entering its number:"
        read -p "Enter version number: " version_num

        # Validate input
        if ! [[ "$version_num" =~ ^[0-9]+$ ]]; then
            echo "Error: Invalid input. Please enter a number."
            rm -rf "$TEMP_DIR"
            exit 1
        fi

        # Get the selected version and URL
        selected_version=$(echo "$DHIS2_VERSION_LIST" | sed -n "${version_num}p" | cut -f1)
        download_url=$(echo "$DHIS2_VERSION_LIST" | sed -n "${version_num}p" | cut -f4)

        if [ -z "$selected_version" ]; then
            echo "Error: Invalid version number."
            rm -rf "$TEMP_DIR"
            exit 1
        fi

        echo "You selected version $selected_version"
    fi

    # Set the WAR file name (the default war path is in the target directory for compiled source)
    local war_file="$SCRIPT_DIR/dhis.war"

    # Check if WAR file already exists
    if [ -f "$war_file" ]; then
        echo "Warning: A DHIS2 WAR file already exists at $war_file"
        read -p "Do you want to back it up and continue? (Y/n): " backup_continue

        if [[ ! "$backup_continue" =~ ^[Nn] ]]; then
            backup_file "$war_file" || exit 1
        else
            echo "Installation aborted by user."
            rm -rf "$TEMP_DIR"
            exit 0
        fi
    fi

    # Download the selected version
    echo "Downloading DHIS2 version $selected_version..."
    if ! download_file "$download_url" "$war_file"; then
        echo "Error: Failed to download DHIS2 version $selected_version."
        rm -rf "$TEMP_DIR"
        exit 1
    fi

    echo "============================================"
    echo "      Installation Complete                "
    echo "============================================"
    echo "DHIS2 version $selected_version has been installed to: $war_file"
    echo
    echo "To start the server, run:"
    echo "  $0 start"
    echo
    echo "Make sure you have configured dhis.conf in your DHIS2_HOME directory:"
    echo "  $DHIS2_HOME_DIR"

    # Clean up
    rm -rf "$TEMP_DIR"
    return 0
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
  install  - Install a DHIS2 version from the official releases
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
  $0 install      # Install a DHIS2 version from the official releases
  $0 start -d /path/to/dhis2_home -p 8080
  $0 stop
  $0 logs 100     # Show last 100 lines of logs
  $0 status
  $0 health       # Check the health endpoint
  $0 upgrade      # Run the smart upgrade process
  sudo $0 install-service  # Install as systemd service

Java Version Requirements:
  - DHIS2 versions 2.41 and above require Java 17 or higher
  - DHIS2 versions 2.38-2.40 require Java 11 or higher
  - DHIS2 versions 2.37 and below require Java 8

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
    install)
        install_dhis2
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