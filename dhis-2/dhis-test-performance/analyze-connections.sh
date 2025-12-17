#!/bin/bash

# Note: Not using pipefail because this script uses many pipelines with head/tail
# which can cause SIGPIPE and fail unnecessarily
set -eu

# Script to analyze Hikari connection acquisition times from DHIS2 logs
# Extracts CONN_RELEASED logs within a time range and generates analysis

if [ $# -lt 4 ] || [ $# -gt 5 ]; then
  echo "Usage: $0 <start_time> <end_time> <simulation_csv> <output_dir> [exclude_request_id]"
  echo "  start_time: ISO-8601 timestamp (e.g., 2025-11-25T12:43:30+01:00)"
  echo "  end_time: ISO-8601 timestamp"
  echo "  simulation_csv: Path to Gatling simulation.csv"
  echo "  output_dir: Directory to write analysis files"
  echo "  exclude_request_id: Optional X-Request-ID to exclude from analysis (e.g., prometheus-scraper)"
  exit 1
fi

START_TIME="$1"
END_TIME="$2"
SIMULATION_CSV="$3"
OUTPUT_DIR="$4"
EXCLUDE_REQUEST_ID="${5:-}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Look for dhis.log in output directory first (for archived results), then fall back to default location
if [ -f "$OUTPUT_DIR/dhis.log" ]; then
  LOG_FILE="$OUTPUT_DIR/dhis.log"
else
  LOG_FILE="$SCRIPT_DIR/logs/dhis.log"
fi

echo "Start time: $START_TIME"
echo "End time: $END_TIME"
echo "Simulation CSV: $SIMULATION_CSV"
echo "Output directory: $OUTPUT_DIR"
echo "Log file: $LOG_FILE"
if [ -n "$EXCLUDE_REQUEST_ID" ]; then
  echo "Excluding request ID: $EXCLUDE_REQUEST_ID"
fi
echo ""

# Convert ISO-8601 timestamps to epoch for comparison (handles timezone)
START_EPOCH=$(date -d "$START_TIME" +%s)
END_EPOCH=$(date -d "$END_TIME" +%s)

# Output files
RAW_CSV="$OUTPUT_DIR/connection-raw.csv"
STATS_TXT="$OUTPUT_DIR/connection-stats.md"
PER_REQUEST_CSV="$OUTPUT_DIR/per-request-breakdown.csv"

# Extract CONN_RELEASED logs within time range
echo "Extracting CONN_RELEASED logs from $LOG_FILE..."

# Create raw CSV header
echo "timestamp,request_id,thread,wait_ms,held_ms" > "$RAW_CSV"

# Parse logs and filter by timestamp using rg with regex for time range (much faster)
# Log format: 2025-11-25T12:43:30.123+01:00 DEBUG org.hisp.dhis.datasource.ConnectionAcquisitionTimingDataSource [http-nio-8080-exec-17] request_id=abc-123 CONN_RELEASED wait_ms=0 held_ms=866

# Extract date and hour for regex filtering (approximation to narrow down results)
START_DATE=$(date -d "$START_TIME" +"%Y-%m-%d")
START_HOUR=$(date -d "$START_TIME" +"%H")

# Export exclude pattern for perl to use
export EXCLUDE_REQUEST_ID

# Use rg to filter by date/hour first (fast), then filter precisely with perl
rg "${START_DATE}T${START_HOUR}:.*CONN_RELEASED" "$LOG_FILE" --no-heading --no-line-number | perl -ne '
  # Extract fields directly without date conversion
  /^(\S+)/ or next;
  $timestamp = $1;

  /request_id=(\S+)/ or next;
  $request_id = $1;

  # Skip empty request_id
  next if ($request_id eq "");

  # Skip excluded request_id if specified
  next if ($ENV{EXCLUDE_REQUEST_ID} && $request_id eq $ENV{EXCLUDE_REQUEST_ID});

  /\[([^\]]+)\]/ and $thread = $1;
  /wait_ms=(\d+)/ and $wait_ms = $1;
  /held_ms=(\d+)/ and $held_ms = $1;

  print "$timestamp,$request_id,$thread,$wait_ms,$held_ms\n";
' >> "$RAW_CSV"

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
# Use int(count*percentile + 0.5) for proper rounding (nearest-rank method)
WAIT_P50=$(tail -n +2 "$RAW_CSV" | cut -d',' -f4 | sort -n | awk -v count=$TOTAL_CONNECTIONS 'NR==int(count*0.50+0.5){print; exit}')
WAIT_P90=$(tail -n +2 "$RAW_CSV" | cut -d',' -f4 | sort -n | awk -v count=$TOTAL_CONNECTIONS 'NR==int(count*0.90+0.5){print; exit}')
WAIT_P99=$(tail -n +2 "$RAW_CSV" | cut -d',' -f4 | sort -n | awk -v count=$TOTAL_CONNECTIONS 'NR==int(count*0.99+0.5){print; exit}')
WAIT_MAX=$(tail -n +2 "$RAW_CSV" | cut -d',' -f4 | sort -n | tail -1)

HELD_P50=$(tail -n +2 "$RAW_CSV" | cut -d',' -f5 | sort -n | awk -v count=$TOTAL_CONNECTIONS 'NR==int(count*0.50+0.5){print; exit}')
HELD_P90=$(tail -n +2 "$RAW_CSV" | cut -d',' -f5 | sort -n | awk -v count=$TOTAL_CONNECTIONS 'NR==int(count*0.90+0.5){print; exit}')
HELD_P99=$(tail -n +2 "$RAW_CSV" | cut -d',' -f5 | sort -n | awk -v count=$TOTAL_CONNECTIONS 'NR==int(count*0.99+0.5){print; exit}')
HELD_MAX=$(tail -n +2 "$RAW_CSV" | cut -d',' -f5 | sort -n | tail -1)

# Write statistics file as markdown
cat > "$STATS_TXT" <<EOF
# Connection Acquisition Statistics

## Test Information

* **Time Range**: $START_TIME to $END_TIME
* **Log File**: $LOG_FILE
* **Analysis Date**: $(date -Iseconds)
$(if [ -n "$EXCLUDE_REQUEST_ID" ]; then echo "* **Excluded Request ID**: $EXCLUDE_REQUEST_ID"; fi)

## Summary

* **Unique request IDs**: $UNIQUE_REQUESTS (each ID may make multiple HTTP requests)
* **Total connections**: $TOTAL_CONNECTIONS
* **Avg connections per request ID**: $(awk "BEGIN {printf \"%.2f\", $TOTAL_CONNECTIONS / $UNIQUE_REQUESTS}")

## Wait Time (ms)

Connection pool wait time - time spent waiting to acquire a connection from the pool.

* **Total**: $TOTAL_WAIT
* **Mean**: $MEAN_WAIT
* **P50**: $WAIT_P50
* **P90**: $WAIT_P90
* **P99**: $WAIT_P99
* **Max**: $WAIT_MAX

EOF

# Add worst 10 by wait time (sort by wait, then by held as tiebreaker)
echo "### Worst 10 Connections by Wait Time" >> "$STATS_TXT"
echo "" >> "$STATS_TXT"
echo '```sh' >> "$STATS_TXT"
tail -n +2 "$RAW_CSV" | sort -t',' -k4,4rn -k5,5rn | head -10 | \
  awk -F',' '{printf "%-35s  wait: %5d ms  held: %5d ms  [%s]\n", $2, $4, $5, $3}' >> "$STATS_TXT"
echo '```' >> "$STATS_TXT"
echo "" >> "$STATS_TXT"

# Held time section
cat >> "$STATS_TXT" <<EOF
## Held Time (ms)

Connection held time - time the connection was held before being released.

* **Total**: $TOTAL_HELD
* **Mean**: $MEAN_HELD
* **P50**: $HELD_P50
* **P90**: $HELD_P90
* **P99**: $HELD_P99
* **Max**: $HELD_MAX

EOF

# Add worst 10 by held time (sort by held, then by wait as tiebreaker)
echo "### Worst 10 Connections by Held Time" >> "$STATS_TXT"
echo "" >> "$STATS_TXT"
echo '```sh' >> "$STATS_TXT"
tail -n +2 "$RAW_CSV" | sort -t',' -k5,5rn -k4,4rn | head -10 | \
  awk -F',' '{printf "%-35s  wait: %5d ms  held: %5d ms  [%s]\n", $2, $4, $5, $3}' >> "$STATS_TXT"
echo '```' >> "$STATS_TXT"
echo "" >> "$STATS_TXT"

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

# Display statistics (moved here after file is complete)
echo ""
echo "Statistics written to: $STATS_TXT"
echo "Displaying summary..."
echo ""
cat "$STATS_TXT" | head -50

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
