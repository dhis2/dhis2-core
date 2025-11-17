#!/bin/bash

##
## Calculate think times from nginx access logs in Elasticsearch
##
## This script processes tracker-local index and creates tracker-think-times index
## with think time calculations for use in Kibana dashboards.
##

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPTS_DIR="$SCRIPT_DIR/scripts"
PYTHON_SCRIPT="$SCRIPTS_DIR/calculate-think-times.py"
REQUIREMENTS="$SCRIPTS_DIR/requirements.txt"
VENV_PYTHON="$SCRIPTS_DIR/.venv/bin/python3"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default values (can be overridden by environment variables)
ES_URL="${ES_URL:-https://localhost:9200}"
ES_USER="${ES_USER:-elastic}"
ES_PASSWORD="${ES_PASSWORD:-elastic123}"
SOURCE_INDEX="${SOURCE_INDEX:-tracker-local}"
DEST_INDEX="${DEST_INDEX:-tracker-think-times}"

echo "============================================"
echo "Think Time Calculator for Gatling Tests"
echo "============================================"
echo ""

# Check if virtual environment exists
if [ ! -f "$VENV_PYTHON" ]; then
    echo -e "${YELLOW}Virtual environment not found${NC}"
    echo "Creating virtual environment and installing dependencies..."

    # Check if uv is available
    if ! command -v uv &> /dev/null; then
        echo -e "${RED}Error: uv not found${NC}"
        echo "Please install uv: https://docs.astral.sh/uv/"
        exit 1
    fi

    # Create venv and install dependencies
    cd "$SCRIPTS_DIR"
    uv venv
    uv pip install -r requirements.txt
    cd "$SCRIPT_DIR"

    echo -e "${GREEN}✓ Virtual environment created${NC}"
    echo ""
fi

# Check if Elasticsearch is accessible
echo "Checking Elasticsearch connection..."
if ! curl -k -s -u "$ES_USER:$ES_PASSWORD" "$ES_URL" > /dev/null 2>&1; then
    echo -e "${RED}Error: Cannot connect to Elasticsearch at $ES_URL${NC}"
    echo "Make sure the logging stack is running:"
    echo "  docker compose --profile logging up -d"
    exit 1
fi
echo -e "${GREEN}✓ Connected to Elasticsearch${NC}"
echo ""

# Run the Python script
echo "Running think time calculation..."
echo "  Source: $SOURCE_INDEX"
echo "  Destination: $DEST_INDEX"
echo ""

"$VENV_PYTHON" "$PYTHON_SCRIPT" \
    --es-url "$ES_URL" \
    --es-user "$ES_USER" \
    --es-password "$ES_PASSWORD" \
    --source-index "$SOURCE_INDEX" \
    --dest-index "$DEST_INDEX" \
    "$@"

echo ""
echo -e "${GREEN}✓ Think time calculation complete!${NC}"
echo ""
echo "Next steps:"
echo "1. Open Kibana: http://localhost:5601"
echo "2. Go to Analytics → Dashboard"
echo "3. Open 'Gatling Think Time Analysis' dashboard"
echo "4. Copy .pause() values directly into your Gatling scenarios"
echo ""
