#!/usr/bin/env bash

set -e

KIBANA_URL="${KIBANA_URL:-http://localhost:5601}"
KIBANA_USER="${KIBANA_USER:-elastic}"
KIBANA_PASSWORD="${KIBANA_PASSWORD:-elastic123}"
DASHBOARDS_DIR="docker/kibana-dashboards"

echo "Reimporting dashboards from $DASHBOARDS_DIR..."

if [ ! -d "$DASHBOARDS_DIR" ]; then
  echo "Error: Directory $DASHBOARDS_DIR not found"
  exit 1
fi

for dashboard in "$DASHBOARDS_DIR"/*.ndjson; do
  if [ ! -f "$dashboard" ]; then
    echo "No .ndjson files found in $DASHBOARDS_DIR"
    exit 1
  fi

  echo "Importing $(basename "$dashboard")..."
  curl -X POST "$KIBANA_URL/api/saved_objects/_import?overwrite=true" \
    -H "kbn-xsrf: true" \
    -u "$KIBANA_USER:$KIBANA_PASSWORD" \
    --form file=@"$dashboard" \
    --silent --show-error
  echo ""
done

echo "All dashboards reimported successfully!"
echo "Refresh your browser to see the changes."
