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

  TEST_DIR_NAME="$(basename "$ABS_GATLING_TEST_DIR")"

  # Find the project root (where dhis-2 is located)
  PROJECT_ROOT="$(cd "$GATLING_TEST_DIR" && cd ../../../../.. && pwd)"
  TEST_MODULE="$PROJECT_ROOT/dhis-2/dhis-test-performance"

  cd "$TEST_MODULE"

  # Process only the specific test directory
  glog \
    --config src/test/resources/gatling.conf \
    "target/gatling/$TEST_DIR_NAME"

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

echo "===== Test Time Range ====="
echo ""
echo "Start: $START_TIME ($START_MS ms)"
echo "End:   $END_TIME ($END_MS ms)"
echo "Duration: ${DURATION_MIN}m ${DURATION_REMAINING_SEC}s"
echo ""
echo "Use these timestamps with analyze-connections.sh:"
echo ""
echo "  ./connection-analysis/analyze-connections.sh \\"
echo "    \"$START_TIME\" \\"
echo "    \"$END_TIME\" \\"
echo "    \"$SIMULATION_CSV\" \\"
echo "    \"connection-analysis/output\""
echo ""
