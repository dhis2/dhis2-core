#!/bin/bash
#
# Compare Gatling concurrent users with Elasticsearch calculation
#
# This script:
# 1. Auto-detects the latest Gatling test results
# 2. Calculates concurrent users from user start/end events
# 3. Queries Elasticsearch with cardinality aggregation
# 4. Compares Gatling concurrent users vs ES concurrent sessions
# 5. Shows analysis noting expected differences
#
# Usage:
#   ./verify-concurrent-users.sh [gatling_dir] [interval]
#
# Examples:
#   ./verify-concurrent-users.sh                                    # Auto-detect latest
#   ./verify-concurrent-users.sh target/gatling/trackertest-123    # Specific test
#   ./verify-concurrent-users.sh target/gatling/trackertest-123 5s # 5 second buckets
#

set -euo pipefail

# Configuration
ES_HOST="${ES_HOST:-https://localhost:9200}"
ES_USER="${ES_USER:-elastic}"
ES_PASS="${ES_PASS:-changeme}"
INDEX="${INDEX:-tracker-local}"
INTERVAL="${2:-1s}"

# Find Gatling test directory
if [ -n "${1:-}" ]; then
    GATLING_DIR="$1"
else
    # Auto-detect latest test
    GATLING_DIR=$(ls -td target/gatling/trackertest-* 2>/dev/null | head -1 || echo "")
    if [ -z "$GATLING_DIR" ]; then
        echo "Error: No Gatling test results found in target/gatling/"
        echo "Usage: $0 [gatling_dir] [interval]"
        exit 1
    fi
    echo "Auto-detected latest test: $GATLING_DIR"
fi

# Validate directory
if [ ! -d "$GATLING_DIR" ]; then
    echo "Error: Directory not found: $GATLING_DIR"
    exit 1
fi

CSV_FILE="$GATLING_DIR/simulation.csv"
if [ ! -f "$CSV_FILE" ]; then
    echo "simulation.csv not found in $GATLING_DIR"
    echo "Converting Gatling binary log to CSV..."
    echo ""

    if ! command -v glog &> /dev/null; then
        echo "Error: glog command not found"
        echo "Install glog from: https://github.com/dhis2/gatling/releases"
        exit 1
    fi

    glog --config src/test/resources/gatling.conf --scan-subdirs target/gatling

    if [ ! -f "$CSV_FILE" ]; then
        echo "Error: Failed to generate simulation.csv"
        exit 1
    fi

    echo "✓ Generated simulation.csv"
    echo ""
fi

# Extract test start/end time from Gatling CSV
echo "=========================================================================="
echo "STEP 1: Analyzing Gatling test results"
echo "=========================================================================="
echo ""

# Extract user start and end timestamps
USER_STARTS=$(grep "^user.*start" "$CSV_FILE" | cut -d',' -f6)
USER_ENDS=$(grep "^user.*end" "$CSV_FILE" | cut -d',' -f6)

FIRST_START=$(echo "$USER_STARTS" | head -1)
LAST_END=$(echo "$USER_ENDS" | tail -1)

if [ -z "$FIRST_START" ]; then
    echo "Error: No user start events found in $CSV_FILE"
    exit 1
fi

if [ -z "$LAST_END" ]; then
    echo "Error: No user end events found in $CSV_FILE"
    exit 1
fi

# Convert to ISO format with buffer
START_SEC=$((FIRST_START / 1000 - 5))
END_SEC=$((LAST_END / 1000 + 5))
START_TIME=$(date -u -d "@${START_SEC}" '+%Y-%m-%dT%H:%M:%S')
END_TIME=$(date -u -d "@${END_SEC}" '+%Y-%m-%dT%H:%M:%S')

echo "Gatling test:"
echo "  Directory: $GATLING_DIR"
echo "  CSV file: $CSV_FILE"
echo "  Start time: $START_TIME"
echo "  End time: $END_TIME"
echo "  Interval: $INTERVAL"
echo ""

# Calculate concurrent users from Gatling
# Method: For each second, count users that have started but not yet ended
echo "Calculating concurrent users from Gatling events..."

# Create temporary files for processing
TMP_STARTS=$(mktemp)
TMP_ENDS=$(mktemp)
trap "rm -f $TMP_STARTS $TMP_ENDS" EXIT

# Extract start and end times in seconds
echo "$USER_STARTS" | awk '{print int($1 / 1000)}' | sort -n > "$TMP_STARTS"
echo "$USER_ENDS" | awk '{print int($1 / 1000)}' | sort -n > "$TMP_ENDS"

# Calculate concurrent users per second using event-based approach
# For each second, we want to know: how many users were alive at the START of that second
GATLING_COUNTS=$(
    {
        cat "$TMP_STARTS" | awk '{print $1, 1}'   # +1 for user start
        cat "$TMP_ENDS" | awk '{print $1, -1}'    # -1 for user end
    } | sort -n -k1,1 | awk -v start_sec="$START_SEC" -v end_sec="$END_SEC" '
    BEGIN {
        concurrent = 0
        last_sec = start_sec
    }
    {
        sec = $1
        delta = $2

        # Fill in gaps with current concurrent count
        while (last_sec < sec && last_sec <= end_sec) {
            if (concurrent > 0) {
                print last_sec, concurrent
            }
            last_sec++
        }

        # Apply the event
        concurrent += delta

        # If this is the first event in this second, record the count AFTER the event
        if (sec >= start_sec && sec <= end_sec) {
            counts[sec] = concurrent
            last_sec = sec
        }
    }
    END {
        # Fill in any remaining gaps
        while (last_sec <= end_sec) {
            if (concurrent > 0) {
                print last_sec, concurrent
            }
            last_sec++
        }

        # Output any recorded counts (to handle multiple events per second)
        for (sec in counts) {
            if (sec >= start_sec && sec <= end_sec && counts[sec] > 0) {
                # Already printed above
            }
        }
    }'
)

if [ -z "$GATLING_COUNTS" ]; then
    echo "Error: Failed to calculate concurrent users from Gatling data"
    exit 1
fi

GATLING_TOTAL=$(echo "$GATLING_COUNTS" | awk '{sum+=$2} END {print sum}')
GATLING_BUCKETS=$(echo "$GATLING_COUNTS" | wc -l)
GATLING_MAX=$(echo "$GATLING_COUNTS" | awk 'BEGIN {max=0} {if ($2 > max) max=$2} END {print max}')
GATLING_AVG=$(echo "scale=2; $GATLING_TOTAL / $GATLING_BUCKETS" | bc)

echo "Found concurrent users in $GATLING_BUCKETS time buckets"
echo "  Max: $GATLING_MAX concurrent users"
echo "  Average: $GATLING_AVG concurrent users"
echo ""

# Query Elasticsearch
echo "=========================================================================="
echo "STEP 2: Querying Elasticsearch"
echo "=========================================================================="
echo ""
echo "Index: $INDEX"
echo "Time range: $START_TIME to $END_TIME"
echo "Interval: $INTERVAL"
echo ""

curl -s --insecure \
  -u "$ES_USER:$ES_PASS" \
  -X POST "$ES_HOST/$INDEX/_search" \
  -H 'Content-Type: application/json' \
  -d "{
  \"size\": 0,
  \"query\": {
    \"range\": {
      \"request_received_at\": {
        \"gte\": \"$START_TIME\",
        \"lte\": \"$END_TIME\"
      }
    }
  },
  \"aggs\": {
    \"buckets\": {
      \"date_histogram\": {
        \"field\": \"request_received_at\",
        \"fixed_interval\": \"$INTERVAL\",
        \"min_doc_count\": 0
      },
      \"aggs\": {
        \"concurrent_sessions\": {
          \"cardinality\": {
            \"field\": \"sessionid_hash.keyword\"
          }
        }
      }
    }
  }
}" > /tmp/es-concurrent-users-query.json

# Check for errors
if grep -q '"error"' /tmp/es-concurrent-users-query.json; then
    echo "Error querying Elasticsearch:"
    cat /tmp/es-concurrent-users-query.json | jq '.error.reason' 2>/dev/null || cat /tmp/es-concurrent-users-query.json
    exit 1
fi

ES_COUNTS=$(cat /tmp/es-concurrent-users-query.json | jq -r '
  .aggregations.buckets.buckets[] |
  select(.concurrent_sessions and .concurrent_sessions.value > 0) |
  "\((.key / 1000) | floor) \(.concurrent_sessions.value | floor)"
')

if [ -z "$ES_COUNTS" ]; then
    echo "Warning: No data found in Elasticsearch for the specified time range"
    echo "Check that:"
    echo "  1. The logging stack is running (docker compose --profile logging ps)"
    echo "  2. Requests went through nginx (port 8081, not 8080)"
    echo "  3. The time range is correct"
    exit 1
fi

ES_TOTAL=$(cat /tmp/es-concurrent-users-query.json | jq '[.aggregations.buckets.buckets[].concurrent_sessions.value // 0] | add | floor')
ES_BUCKETS=$(echo "$ES_COUNTS" | wc -l)
ES_MAX=$(echo "$ES_COUNTS" | awk 'BEGIN {max=0} {if ($2 > max) max=$2} END {print max}')
ES_AVG=$(echo "scale=2; $ES_TOTAL / $ES_BUCKETS" | bc)

echo "Found concurrent sessions in $ES_BUCKETS time buckets"
echo "  Max: $ES_MAX concurrent sessions"
echo "  Average: $ES_AVG concurrent sessions"
echo ""

# Compare side-by-side
echo "=========================================================================="
echo "STEP 3: Side-by-side comparison"
echo "=========================================================================="
echo ""
printf "%-20s | %-12s | %-12s | %-10s\n" "Time (UTC)" "Gatling" "Elastic" "Diff"
printf "%-20s | %-12s | %-12s | %-10s\n" "" "(all alive)" "(w/ requests)" ""
echo "---------------------+--------------+--------------+-----------"

join -a1 -a2 -e 0 -o auto <(echo "$GATLING_COUNTS") <(echo "$ES_COUNTS") | \
  head -30 | \
  awk '{
    ts = $1
    gatling = ($2 != "" ? $2 : 0)
    es = ($3 != "" ? $3 : 0)
    diff = gatling - es

    cmd = "date -u -d @" ts " +\"%Y-%m-%d %H:%M:%S\""
    cmd | getline time
    close(cmd)

    printf "%-20s | %12d | %12d | %+10d\n", time, gatling, es, diff
  }'

echo ""
echo "... (showing first 30 seconds, see full data in /tmp/es-concurrent-users-query.json)"
echo ""

# Summary and Analysis
echo "=========================================================================="
echo "STEP 4: Analysis & Interpretation"
echo "=========================================================================="
echo ""

echo "Gatling (Lifecycle-Based):"
echo "  Total user-seconds: $GATLING_TOTAL"
echo "  Time buckets: $GATLING_BUCKETS"
echo "  Max concurrent: $GATLING_MAX users"
echo "  Average concurrent: $GATLING_AVG users"
echo "  Method: Counts all users that have started but not ended"
echo ""

echo "Elasticsearch (Request-Based):"
echo "  Total session-seconds: $ES_TOTAL"
echo "  Time buckets: $ES_BUCKETS"
echo "  Max concurrent: $ES_MAX sessions"
echo "  Average concurrent: $ES_AVG sessions"
echo "  Method: Counts sessions that made requests in each time bucket"
echo ""

# Calculate ratio
if [ "$GATLING_TOTAL" -gt 0 ]; then
    RATIO=$(echo "scale=1; 100 * $ES_TOTAL / $GATLING_TOTAL" | bc)
    if [ "${RATIO:0:1}" = "." ]; then
        RATIO="0$RATIO"
    fi
    echo "ES/Gatling Ratio: $RATIO%"
    echo ""
fi

# Interpretation
echo "Interpretation:"
echo ""
echo "KEY DIFFERENCE:"
echo "  * Gatling: Snapshot count of users alive at each second boundary"
echo "  * Elasticsearch: Window count of sessions active during each second"
echo "  * Users in 'think time' (between requests) are counted by Gatling but NOT by ES"
echo ""
echo "NOTE: For very short-lived users (<1 second), ES may show MORE sessions"
echo "than Gatling in individual seconds. This happens when multiple users start"
echo "and end within the same second - ES counts all of them (window), while"
echo "Gatling only counts those alive at the second boundary (snapshot)."
echo "The overall ratio is still meaningful for understanding think time impact."
echo ""

# Assessment based on ratio
if [ "$GATLING_TOTAL" -gt 0 ]; then
    RATIO_INT=$(echo "$RATIO" | cut -d'.' -f1)

    if [ "$ES_TOTAL" -gt "$GATLING_TOTAL" ]; then
        echo "⚠ WARNING: ES shows MORE concurrent sessions than Gatling users!"
        echo "  This should not happen and indicates a potential issue:"
        echo "  * Session ID reuse or collision"
        echo "  * External traffic not from Gatling test"
        echo "  * Time range mismatch"
        EXIT_CODE=1
    elif [ "$RATIO_INT" -ge 70 ]; then
        echo "✓ EXPECTED PATTERN: ES shows ${RATIO}% of Gatling's count"
        echo "  This indicates short think times or high request rates"
        echo "  Most users are actively making requests"
        EXIT_CODE=0
    elif [ "$RATIO_INT" -ge 30 ]; then
        echo "✓ EXPECTED PATTERN: ES shows ${RATIO}% of Gatling's count"
        echo "  This is typical for tests with moderate think times"
        echo "  Users spend time between requests (reading, processing)"
        EXIT_CODE=0
    elif [ "$RATIO_INT" -ge 10 ]; then
        echo "✓ EXPECTED PATTERN: ES shows ${RATIO}% of Gatling's count"
        echo "  This indicates long think times or sparse request patterns"
        echo "  Most users are idle between requests"
        EXIT_CODE=0
    elif [ "$ES_TOTAL" -eq 0 ]; then
        echo "✗ ERROR: No data in Elasticsearch!"
        echo "  Check logging stack and ensure requests went through nginx"
        EXIT_CODE=1
    else
        echo "⚠ UNUSUAL: ES shows only ${RATIO}% of Gatling's count"
        echo "  This is lower than typical. Possible causes:"
        echo "  * Very long think times configured in test"
        echo "  * Many users failing before making requests"
        echo "  * Logging issues or dropped requests"
        EXIT_CODE=1
    fi
fi

echo ""
echo "WHEN TO BE CONCERNED:"
echo "  * ES total >> Gatling total: Overall ES higher than Gatling suggests data issues"
echo "  * ES = 0: No data reaching Elasticsearch"
echo "  * ES < 10% of Gatling: Unusually low, investigate logging pipeline"
echo "  * Individual seconds: ES may exceed Gatling for short-lived users (normal)"
echo ""
echo "Results saved to: /tmp/es-concurrent-users-query.json"
echo ""

exit ${EXIT_CODE:-0}
