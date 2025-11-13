#!/bin/bash
#
# Compare Gatling user arrival rate with Elasticsearch calculation
#
# This script:
# 1. Auto-detects the latest Gatling test results
# 2. Queries Elasticsearch with cumulative_cardinality + derivative
# 3. Compares Gatling user starts vs ES new sessions
# 4. Shows accuracy report
#
# Usage:
#   ./compare-arrival-rates.sh [gatling_dir] [interval]
#
# Examples:
#   ./compare-arrival-rates.sh                                    # Auto-detect latest
#   ./compare-arrival-rates.sh target/gatling/trackertest-123    # Specific test
#   ./compare-arrival-rates.sh target/gatling/trackertest-123 5s # 5 second buckets
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

# Extract timestamps more efficiently
USER_STARTS=$(grep "^user.*start" "$CSV_FILE" | cut -d',' -f6)
FIRST_TS=$(echo "$USER_STARTS" | head -1)
LAST_TS=$(echo "$USER_STARTS" | tail -1)

if [ -z "$FIRST_TS" ] || [ -z "$LAST_TS" ]; then
    echo "Error: No user start events found in $CSV_FILE"
    exit 1
fi

# Convert to ISO format with buffer
START_SEC=$((FIRST_TS / 1000 - 5))
END_SEC=$((LAST_TS / 1000 + 5))
START_TIME=$(date -u -d "@${START_SEC}" '+%Y-%m-%dT%H:%M:%S')
END_TIME=$(date -u -d "@${END_SEC}" '+%Y-%m-%dT%H:%M:%S')

echo "Gatling test:"
echo "  Directory: $GATLING_DIR"
echo "  CSV file: $CSV_FILE"
echo "  Start time: $START_TIME"
echo "  End time: $END_TIME"
echo "  Interval: $INTERVAL"
echo ""

# Count Gatling user starts
GATLING_COUNTS=$(grep "^user.*start" "$CSV_FILE" | \
  awk -F',' '{
    bucket = int($6 / 1000)
    count[bucket]++
  }
  END {
    for (bucket in count) {
      print bucket, count[bucket]
    }
  }' | sort -n)

GATLING_TOTAL=$(echo "$GATLING_COUNTS" | awk '{sum+=$2} END {print sum}')
GATLING_BUCKETS=$(echo "$GATLING_COUNTS" | wc -l)

echo "Found $GATLING_TOTAL user starts across $GATLING_BUCKETS time buckets"
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
        \"active_sessions\": {
          \"cardinality\": {
            \"field\": \"sessionid_hash.keyword\"
          }
        },
        \"cumulative_sessions\": {
          \"cumulative_cardinality\": {
            \"buckets_path\": \"active_sessions\"
          }
        },
        \"new_sessions\": {
          \"derivative\": {
            \"buckets_path\": \"cumulative_sessions\"
          }
        }
      }
    }
  }
}" > /tmp/es-arrival-rate-query.json

# Check for errors
if grep -q '"error"' /tmp/es-arrival-rate-query.json; then
    echo "Error querying Elasticsearch:"
    cat /tmp/es-arrival-rate-query.json | jq '.error.reason' 2>/dev/null || cat /tmp/es-arrival-rate-query.json
    exit 1
fi

ES_COUNTS=$(cat /tmp/es-arrival-rate-query.json | jq -r '
  .aggregations.buckets.buckets[] |
  select(.new_sessions and .new_sessions.value > 0) |
  "\((.key / 1000) | floor) \(.new_sessions.value | floor)"
')

if [ -z "$ES_COUNTS" ]; then
    echo "Warning: No data found in Elasticsearch for the specified time range"
    echo "Check that:"
    echo "  1. The logging stack is running (docker compose --profile logging ps)"
    echo "  2. Requests went through nginx (port 8081, not 8080)"
    echo "  3. The time range is correct"
    exit 1
fi

ES_TOTAL=$(cat /tmp/es-arrival-rate-query.json | jq '[.aggregations.buckets.buckets[].new_sessions.value // 0] | add | floor')
ES_BUCKETS=$(echo "$ES_COUNTS" | wc -l)

echo "Found $ES_TOTAL new sessions across $ES_BUCKETS time buckets"
echo ""

# Compare side-by-side
echo "=========================================================================="
echo "STEP 3: Side-by-side comparison"
echo "=========================================================================="
echo ""
printf "%-20s | %-10s | %-10s | %-10s\n" "Time (UTC)" "Gatling" "Elastic" "Diff"
printf "%-20s | %-10s | %-10s | %-10s\n" "" "(starts)" "(new)" ""
echo "---------------------+------------+------------+-----------"

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

    printf "%-20s | %10d | %10d | %+10d\n", time, gatling, es, diff
  }'

echo ""
echo "... (showing first 30 seconds, see full data in /tmp/es-arrival-rate-query.json)"
echo ""

# Summary
echo "=========================================================================="
echo "STEP 4: Summary & Accuracy Report"
echo "=========================================================================="
echo ""

echo "Gatling:"
echo "  Total user starts: $GATLING_TOTAL"
echo "  Time buckets: $GATLING_BUCKETS"
if [ "$GATLING_BUCKETS" -gt 0 ]; then
    echo "  Average: $(echo "scale=2; $GATLING_TOTAL / $GATLING_BUCKETS" | bc) users/sec"
fi
echo ""

echo "Elasticsearch:"
echo "  Total new sessions: $ES_TOTAL"
echo "  Time buckets: $ES_BUCKETS"
if [ "$ES_BUCKETS" -gt 0 ]; then
    echo "  Average: $(echo "scale=2; $ES_TOTAL / $ES_BUCKETS" | bc) sessions/sec"
fi
echo ""

DIFF=$((GATLING_TOTAL - ES_TOTAL))
ABS_DIFF=${DIFF#-}  # Absolute value

echo "Difference:"
echo "  Total: $DIFF"
if [ "$GATLING_TOTAL" -gt 0 ]; then
    PERCENT=$(echo "scale=1; 100 * $DIFF / $GATLING_TOTAL" | bc)
    if [ "${PERCENT:0:1}" = "." ]; then
        PERCENT="0$PERCENT"
    fi
    echo "  Percentage: $PERCENT%"
fi
echo ""

# Accuracy assessment
if [ "$ABS_DIFF" -le 2 ]; then
    echo "✓ EXCELLENT MATCH: Difference <= 2 (within rounding error)"
    EXIT_CODE=0
elif [ "$ABS_DIFF" -le 10 ]; then
    echo "✓ GOOD MATCH: Difference <= 10 (< 3% error)"
    EXIT_CODE=0
elif [ "$ABS_DIFF" -le 50 ]; then
    echo "⚠ ACCEPTABLE: Difference <= 50 (HyperLogLog approximation)"
    EXIT_CODE=0
else
    echo "✗ SIGNIFICANT DIFFERENCE: Investigate further"
    EXIT_CODE=1
fi

echo ""
echo "Results saved to: /tmp/es-arrival-rate-query.json"
echo ""

exit $EXIT_CODE
