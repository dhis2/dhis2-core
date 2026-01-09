#!/bin/bash

set -euo pipefail

# Script to generate simulation.csv from Gatling logs and extract test time range

if [ $# -ne 1 ]; then
  echo "Usage: $0 <gatling_test_dir>"
  echo "  gatling_test_dir: Path to the Gatling test directory containing simulation.log"
  echo ""
  echo "Example: $0 dhis-2/dhis-test-performance/target/gatling/trackertest-20251128141726086"
  exit 1
fi

GATLING_TEST_DIR="$1"

# Validate directory exists
if [ ! -d "$GATLING_TEST_DIR" ]; then
  echo "Error: Directory not found: $GATLING_TEST_DIR"
  exit 1
fi

# Check if simulation.log exists
SIMULATION_LOG="$GATLING_TEST_DIR/simulation.log"
if [ ! -f "$SIMULATION_LOG" ]; then
  echo "Error: simulation.log not found in $GATLING_TEST_DIR"
  exit 1
fi

echo "Gatling test directory: $GATLING_TEST_DIR"
echo "Simulation log: $SIMULATION_LOG"
echo ""

# Get absolute path to simulation.csv
ABS_GATLING_TEST_DIR="$(cd "$GATLING_TEST_DIR" && pwd)"
SIMULATION_CSV="$ABS_GATLING_TEST_DIR/simulation.csv"

# Generate simulation.csv if it doesn't exist
if [ ! -f "$SIMULATION_CSV" ]; then
  echo "Generating simulation.csv from simulation.log..."

  # Find the script directory to get project root
  SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
  TEST_MODULE="$PROJECT_ROOT/dhis-2/dhis-test-performance"

  if [ ! -d "$TEST_MODULE" ]; then
    echo "Error: Cannot find dhis-test-performance module at: $TEST_MODULE"
    exit 1
  fi

  cd "$TEST_MODULE"

  # glog can process any directory containing simulation.log
  glog \
    --config src/test/resources/gatling.conf \
    "$ABS_GATLING_TEST_DIR"

  if [ ! -f "$SIMULATION_CSV" ]; then
    echo "Error: Failed to generate simulation.csv"
    exit 1
  fi

  echo "Generated: $SIMULATION_CSV"
else
  echo "Using existing simulation.csv: $SIMULATION_CSV"
fi

echo ""

# Extract start and end times from simulation.csv
echo "Extracting test time range..."

# Get earliest and latest timestamps in a single awk pass
read START_MS END_MS <<< $(awk -F',' '
$1 == "user" && $6 ~ /^[0-9]+$/ {
  ts = $6
  if (min == "" || ts < min) min = ts
  if (max == "" || ts > max) max = ts
}
END {
  print min, max
}
' "$SIMULATION_CSV")

if [ -z "$START_MS" ] || [ -z "$END_MS" ]; then
  echo "Error: Could not extract timestamps from simulation.csv"
  exit 1
fi

# Convert to seconds for date command
START_SEC=$((START_MS / 1000))
END_SEC=$((END_MS / 1000))

# Convert to ISO-8601 format
START_TIME=$(date -d "@$START_SEC" -Iseconds)
END_TIME=$(date -d "@$END_SEC" -Iseconds)

# Calculate duration
DURATION_SEC=$((END_SEC - START_SEC))
DURATION_MIN=$((DURATION_SEC / 60))
DURATION_REMAINING_SEC=$((DURATION_SEC % 60))

# Determine output directory based on input directory
# Extract parent directory from the gatling test dir (e.g., "connection-analysis/results/off")
OUTPUT_DIR=$(dirname "$GATLING_TEST_DIR")

# Human-readable times (local timezone)
START_HUMAN=$(date -d "@$START_SEC" "+%Y-%m-%d %H:%M:%S")
END_HUMAN=$(date -d "@$END_SEC" "+%Y-%m-%d %H:%M:%S")

# Prometheus UI format (UTC, 12h with AM/PM)
START_PROM_UI=$(date -u -d "@$START_SEC" "+%Y-%m-%d %I:%M:%S %p")
END_PROM_UI=$(date -u -d "@$END_SEC" "+%Y-%m-%d %I:%M:%S %p")

# Colors
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo ""
echo -e "${CYAN}Test Times (local)${NC}"
echo "  Start:    $START_HUMAN"
echo "  End:      $END_HUMAN"
echo "  Duration: ${DURATION_MIN}m ${DURATION_REMAINING_SEC}s"
echo ""
echo -e "${CYAN}Prometheus (UTC)${NC}"
echo "  Start: $START_PROM_UI"
echo "  End:   $END_PROM_UI"
echo ""
echo -e "${CYAN}analyze-connections.sh${NC}"
echo "./connection-analysis/analyze-connections.sh \\"
echo "  \"$START_TIME\" \\"
echo "  \"$END_TIME\" \\"
echo "  \"$SIMULATION_CSV\" \\"
echo "  \"$OUTPUT_DIR\""
echo ""
