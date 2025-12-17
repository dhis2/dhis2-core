#!/bin/bash

set -euo pipefail

# Script to analyze Hikari connection acquisition times from DHIS2 logs
# Extracts CONN_RELEASED logs within a time range and generates analysis

if [ $# -ne 4 ]; then
  echo "Usage: $0 <start_time> <end_time> <simulation_csv> <output_dir>"
  echo "  start_time: ISO-8601 timestamp (e.g., 2025-11-25T12:43:30+01:00)"
  echo "  end_time: ISO-8601 timestamp"
  echo "  simulation_csv: Path to Gatling simulation.csv"
  echo "  output_dir: Directory to write analysis files"
  exit 1
fi

START_TIME="$1"
END_TIME="$2"
SIMULATION_CSV="$3"
OUTPUT_DIR="$4"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_FILE="$SCRIPT_DIR/logs/dhis.log"

echo "Start time: $START_TIME"
echo "End time: $END_TIME"
echo "Simulation CSV: $SIMULATION_CSV"
echo "Output directory: $OUTPUT_DIR"
echo "Log file: $LOG_FILE"
echo ""

# Convert ISO-8601 timestamps to epoch for comparison (handles timezone)
START_EPOCH=$(date -d "$START_TIME" +%s)
END_EPOCH=$(date -d "$END_TIME" +%s)

# Output files
RAW_CSV="$OUTPUT_DIR/connection-raw.csv"
STATS_TXT="$OUTPUT_DIR/connection-stats.txt"
PER_REQUEST_CSV="$OUTPUT_DIR/per-request-breakdown.csv"

# Extract CONN_RELEASED logs within time range
echo "Extracting CONN_RELEASED logs from $LOG_FILE..."

# Create raw CSV header
echo "timestamp,request_id,thread,wait_ms,held_ms" > "$RAW_CSV"

# Parse logs and filter by timestamp
# Log format: 2025-11-25T12:43:30.123+01:00 DEBUG org.hisp.dhis.datasource.ConnectionAcquisitionTimingDataSource [http-nio-8080-exec-17] request_id=abc-123 CONN_RELEASED wait_ms=0 held_ms=866
grep "CONN_RELEASED" "$LOG_FILE" | while IFS= read -r line; do
  # Extract timestamp (first field)
  timestamp=$(echo "$line" | cut -d' ' -f1)

  # Convert to epoch for comparison
  log_epoch=$(date -d "$timestamp" +%s 2>/dev/null || echo "0")

  # Skip if outside time range
  if [ "$log_epoch" -lt "$START_EPOCH" ] || [ "$log_epoch" -gt "$END_EPOCH" ]; then
    continue
  fi

  # Extract request_id
  request_id=$(echo "$line" | grep -oP 'request_id=\K[^ ]+' || echo "")

  # Skip if no request_id (shouldn't happen with our changes)
  if [ -z "$request_id" ]; then
    continue
  fi

  # Extract thread name
  thread=$(echo "$line" | grep -oP '\[.*?\]' | tr -d '[]')

  # Extract wait_ms and held_ms
  wait_ms=$(echo "$line" | grep -oP 'wait_ms=\K[0-9]+' || echo "0")
  held_ms=$(echo "$line" | grep -oP 'held_ms=\K[0-9]+' || echo "0")

  # Write to CSV
  echo "$timestamp,$request_id,$thread,$wait_ms,$held_ms" >> "$RAW_CSV"
done

# Count lines (excluding header)
RAW_COUNT=$(($(wc -l < "$RAW_CSV") - 1))
echo "Extracted $RAW_COUNT connection log entries"
echo ""

if [ "$RAW_COUNT" -eq 0 ]; then
  echo "WARNING: No connection logs found in time range"
  echo "This might mean:"
  echo "  1. DHIS2 was not running during the test"
  echo "  2. The log file is empty or not being written"
  echo "  3. Timestamps are in a different timezone"
  echo "  4. X-Request-ID headers are not being sent"
  exit 0
fi

# Generate statistics
echo "Calculating statistics..."

# Calculate totals and generate per-request aggregation
awk -F',' '
NR > 1 {
  total_wait += $4
  total_held += $5
  count++

  # Track per-request aggregation
  req_id = $2
  req_wait[req_id] += $4
  req_held[req_id] += $5
  req_count[req_id]++
}
END {
  print "TOTAL_CONNECTIONS=" count
  print "UNIQUE_REQUESTS=" length(req_wait)
  print "TOTAL_WAIT=" total_wait
  print "TOTAL_HELD=" total_held
  print "MEAN_WAIT=" (count > 0 ? total_wait / count : 0)
  print "MEAN_HELD=" (count > 0 ? total_held / count : 0)

  # Write per-request breakdown to temp file for sorting
  for (req_id in req_wait) {
    print req_id "," req_count[req_id] "," req_wait[req_id] "," req_held[req_id] > "/tmp/per_request.csv"
  }
}
' "$RAW_CSV" > /tmp/stats_vars.txt

# Source the variables
source /tmp/stats_vars.txt

# Calculate percentiles by sorting
WAIT_P50=$(tail -n +2 "$RAW_CSV" | cut -d',' -f4 | sort -n | awk -v count=$TOTAL_CONNECTIONS 'NR==int(count*0.50){print; exit}')
WAIT_P90=$(tail -n +2 "$RAW_CSV" | cut -d',' -f4 | sort -n | awk -v count=$TOTAL_CONNECTIONS 'NR==int(count*0.90){print; exit}')
WAIT_P99=$(tail -n +2 "$RAW_CSV" | cut -d',' -f4 | sort -n | awk -v count=$TOTAL_CONNECTIONS 'NR==int(count*0.99){print; exit}')
WAIT_MAX=$(tail -n +2 "$RAW_CSV" | cut -d',' -f4 | sort -n | tail -1)

HELD_P50=$(tail -n +2 "$RAW_CSV" | cut -d',' -f5 | sort -n | awk -v count=$TOTAL_CONNECTIONS 'NR==int(count*0.50){print; exit}')
HELD_P90=$(tail -n +2 "$RAW_CSV" | cut -d',' -f5 | sort -n | awk -v count=$TOTAL_CONNECTIONS 'NR==int(count*0.90){print; exit}')
HELD_P99=$(tail -n +2 "$RAW_CSV" | cut -d',' -f5 | sort -n | awk -v count=$TOTAL_CONNECTIONS 'NR==int(count*0.99){print; exit}')
HELD_MAX=$(tail -n +2 "$RAW_CSV" | cut -d',' -f5 | sort -n | tail -1)

# Write statistics file
cat > "$STATS_TXT" <<EOF
===== Connection Acquisition Statistics =====

Total connections: $TOTAL_CONNECTIONS
Unique requests: $UNIQUE_REQUESTS

Wait Time (ms):
  Total: $TOTAL_WAIT
  Mean: $MEAN_WAIT
  P50: $WAIT_P50
  P90: $WAIT_P90
  P99: $WAIT_P99
  Max: $WAIT_MAX

Held Time (ms):
  Total: $TOTAL_HELD
  Mean: $MEAN_HELD
  P50: $HELD_P50
  P90: $HELD_P90
  P99: $HELD_P99
  Max: $HELD_MAX

EOF

# Create per-request CSV with header and sorted data
echo "request_id,connection_count,total_wait_ms,total_held_ms,avg_wait_ms,avg_held_ms" > "$PER_REQUEST_CSV"

if [ -f /tmp/per_request.csv ]; then
  sort -t',' -k3 -rn /tmp/per_request.csv | awk -F',' '{
    avg_wait = ($2 > 0 ? $3 / $2 : 0)
    avg_held = ($2 > 0 ? $4 / $2 : 0)
    printf "%s,%d,%d,%d,%.2f,%.2f\n", $1, $2, $3, $4, avg_wait, avg_held
  }' >> "$PER_REQUEST_CSV"
  rm /tmp/per_request.csv
fi

# Display statistics
cat "$STATS_TXT"

# Show top 10 requests by wait time
echo "Top 10 Requests by Total Wait Time:"
head -n 11 "$PER_REQUEST_CSV" | column -t -s','
echo ""

# Correlation with simulation.csv (if exists)
if [ -f "$SIMULATION_CSV" ]; then
  echo "===== Request Timing Correlation ====="
  echo ""

  # Count request records in simulation.csv
  REQUEST_COUNT=$(awk -F',' '$1 == "request"' "$SIMULATION_CSV" | wc -l)
  echo "Gatling requests: $REQUEST_COUNT"
  echo "Unique request IDs with connections: $(wc -l < "$PER_REQUEST_CSV" | awk '{print $1 - 1}')"
  echo ""
  echo "Note: Correlation analysis requires matching request IDs between"
  echo "      Gatling and DHIS2 logs. Check that X-Request-ID headers are working."
fi

echo ""
echo "Analysis complete!"
echo "  Raw data: $RAW_CSV"
echo "  Statistics: $STATS_TXT"
echo "  Per-request: $PER_REQUEST_CSV"
