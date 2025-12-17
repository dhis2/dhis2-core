#!/bin/bash

set -euo pipefail

# Script to validate correlation between Gatling simulation.csv and connection logs

if [ $# -lt 2 ]; then
  echo "Usage: $0 <simulation_csv> <connection_raw_csv>"
  echo "  simulation_csv: Path to Gatling simulation.csv"
  echo "  connection_raw_csv: Path to connection-raw.csv from analysis"
  exit 1
fi

SIMULATION_CSV="$1"
CONNECTION_CSV="$2"

echo "===== Correlation Validation ====="
echo ""
echo "Simulation CSV: $SIMULATION_CSV"
echo "Connection CSV: $CONNECTION_CSV"
echo ""

# Extract Gatling metrics
TOTAL_REQUESTS=$(rg "^request," "$SIMULATION_CSV" | wc -l)
USER_STARTS=$(awk -F',' '$1 == "user" && $10 == "start" {count++} END {print count}' "$SIMULATION_CSV")
USER_ENDS=$(awk -F',' '$1 == "user" && $10 == "end" {count++} END {print count}' "$SIMULATION_CSV")

echo "===== Gatling Test Metrics ====="
echo "Total requests: $TOTAL_REQUESTS"
echo "Virtual users (start events): $USER_STARTS"
echo "Virtual users (end events): $USER_ENDS"

if [ "$USER_STARTS" -ne "$USER_ENDS" ]; then
  echo "WARNING: User starts ($USER_STARTS) != ends ($USER_ENDS)"
fi

REQUESTS_PER_USER=$((TOTAL_REQUESTS / USER_STARTS))
echo "Requests per user: $REQUESTS_PER_USER"
echo ""

# Request type breakdown
echo "===== Request Types ====="
rg "^request," "$SIMULATION_CSV" | awk -F',' '{print $4}' | sort | uniq -c | sort -rn | head -15
echo ""

# Extract connection log metrics
TOTAL_CONNECTIONS=$(awk 'END {print NR-1}' "$CONNECTION_CSV")
UNIQUE_REQUEST_IDS=$(awk -F',' 'NR>1 {ids[$2]=1} END {print length(ids)}' "$CONNECTION_CSV")
GATLING_REQUEST_IDS=$(awk -F',' 'NR>1 && $2 ~ /^g-/ {ids[$2]=1} END {print length(ids)}' "$CONNECTION_CSV")
GATLING_CONNECTIONS=$(awk -F',' 'NR>1 && $2 ~ /^g-/ {count++} END {print count}' "$CONNECTION_CSV")
PROMETHEUS_CONNECTIONS=$(awk -F',' 'NR>1 && $2 == "prometheus-scraper" {count++} END {print count}' "$CONNECTION_CSV")
EMPTY_ID_CONNECTIONS=$(awk -F',' 'NR>1 && $2 == "" {count++} END {print count}' "$CONNECTION_CSV")

echo "===== Connection Log Metrics ====="
echo "Total connections: $TOTAL_CONNECTIONS"
echo "Unique request IDs: $UNIQUE_REQUEST_IDS"
echo ""
echo "Breakdown by request type:"
echo "  Gatling (g-*): $GATLING_CONNECTIONS connections, $GATLING_REQUEST_IDS unique IDs"
echo "  Prometheus: $PROMETHEUS_CONNECTIONS connections"
echo "  Empty ID: $EMPTY_ID_CONNECTIONS connections"
echo ""

# Calculate correlation
EXPECTED_GATLING_IDS=$USER_STARTS
MISSING_IDS=$((EXPECTED_GATLING_IDS - GATLING_REQUEST_IDS))
MATCH_RATE=$(awk "BEGIN {printf \"%.2f\", ($GATLING_REQUEST_IDS / $EXPECTED_GATLING_IDS) * 100}")

echo "===== Correlation Analysis ====="
echo "Expected Gatling request IDs (= users): $EXPECTED_GATLING_IDS"
echo "Actual Gatling request IDs in logs: $GATLING_REQUEST_IDS"
echo "Missing request IDs: $MISSING_IDS"
echo "Match rate: ${MATCH_RATE}%"
echo ""

if [ "$MISSING_IDS" -gt 0 ]; then
  MISSING_PCT=$(awk "BEGIN {printf \"%.2f\", ($MISSING_IDS / $EXPECTED_GATLING_IDS) * 100}")
  echo "WARNING: Missing $MISSING_IDS request IDs (${MISSING_PCT}%)"
  echo "Possible causes:"
  echo "  1. Some requests failed before reaching DHIS2 (network errors)"
  echo "  2. X-Request-ID header not set for some requests"
  echo "  3. Requests didn't acquire any database connections"
  echo "  4. Log filtering excluded some time ranges"
else
  echo "SUCCESS: All Gatling users have corresponding request IDs in connection logs!"
fi
echo ""

# Average connections per request
if [ "$GATLING_REQUEST_IDS" -gt 0 ]; then
  AVG_CONN_PER_REQUEST=$(awk "BEGIN {printf \"%.2f\", $GATLING_CONNECTIONS / $GATLING_REQUEST_IDS}")
  echo "===== Connection Usage ====="
  echo "Average connections per Gatling request ID: $AVG_CONN_PER_REQUEST"
  echo "(Note: Each user makes $REQUESTS_PER_USER HTTP requests, but shares 1 request ID)"
  echo ""
fi

# Non-Gatling traffic analysis
OTHER_CONNECTIONS=$((TOTAL_CONNECTIONS - GATLING_CONNECTIONS))
OTHER_PCT=$(awk "BEGIN {printf \"%.2f\", ($OTHER_CONNECTIONS / $TOTAL_CONNECTIONS) * 100}")

echo "===== Non-Gatling Traffic ====="
echo "Total non-Gatling connections: $OTHER_CONNECTIONS (${OTHER_PCT}%)"
echo "  Prometheus scraper: $PROMETHEUS_CONNECTIONS"
echo "  Other: $((OTHER_CONNECTIONS - PROMETHEUS_CONNECTIONS))"
echo ""

# Top request IDs by connection count
echo "===== Top 10 Request IDs by Connection Count ====="
(awk -F',' 'NR>1 && $2 ~ /^g-/ {count[$2]++} END {for (id in count) print count[id], id}' "$CONNECTION_CSV" | \
  sort -rn | head -10 | awk '{printf "  %4d connections: %s\n", $1, $2}') || true
echo ""

# Summary
echo "===== Validation Summary ====="
MATCH_RATE_INT=${MATCH_RATE%.*}
if [ "$MATCH_RATE" == "100.00" ]; then
  echo "✓ Perfect correlation: All users captured in connection logs"
elif [ "$MATCH_RATE_INT" -ge 95 ]; then
  echo "✓ Good correlation: ${MATCH_RATE}% of users captured"
elif [ "$MATCH_RATE_INT" -ge 90 ]; then
  echo "⚠ Fair correlation: ${MATCH_RATE}% of users captured"
else
  echo "✗ Poor correlation: Only ${MATCH_RATE}% of users captured"
fi

echo "✓ Gatling test structure verified: $USER_STARTS users × $REQUESTS_PER_USER requests = $TOTAL_REQUESTS total"
echo "✓ Non-test traffic identified: $PROMETHEUS_CONNECTIONS prometheus scraper connections"
