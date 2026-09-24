#!/usr/bin/env bash
#
# Run the Docker image built for a dhis2-core PR with docker compose.
#
# Usage: ./run-pr.sh <pr-number> [docker compose up args...]
#
# Examples:
#   ./run-pr.sh 25126
#   ./run-pr.sh 25126 -d
#
# PR images (dhis2/core-pr:<pr-number>) are overwritten on every push to the
# PR, so the image is always pulled to make sure the latest build is used.
set -euo pipefail

if [[ $# -lt 1 || ! "$1" =~ ^[0-9]+$ ]]; then
  echo "Usage: $0 <pr-number> [docker compose up args...]" >&2
  exit 1
fi

pr="$1"
shift

cd "$(dirname "$0")"

DHIS2_IMAGE="dhis2/core-pr:${pr}" exec docker compose up --pull always "$@"
