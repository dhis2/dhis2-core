#!/bin/bash
# Generate tracker import JSON with N tracked entities, clamped to max file size.
# Duplicates TEs from source file (cycling through them) until limits are reached.
#
# Usage: ./tracker-generate.sh --source <file> --count <N> [--max-size <MB>]
#
# Example: ./tracker-generate.sh --source tracker-minimal.json --count 10000 --max-size 100

set -e

SOURCE=""
COUNT=""
MAX_SIZE_MB=100

while [[ $# -gt 0 ]]; do
    case $1 in
        --source)
            SOURCE="$2"
            shift 2
            ;;
        --count)
            COUNT="$2"
            shift 2
            ;;
        --max-size)
            MAX_SIZE_MB="$2"
            shift 2
            ;;
        --help)
            echo "Usage: $0 --source <file> --count <N> [--max-size <MB>]"
            echo ""
            echo "Generate tracker import JSON with N tracked entities."
            echo "Duplicates TEs from source file until count or size limit is reached."
            echo ""
            echo "Options:"
            echo "  --source    Source tracker JSON file"
            echo "  --count     Target number of tracked entities"
            echo "  --max-size  Maximum output size in MB (default: 100)"
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            exit 1
            ;;
    esac
done

if [[ -z "$SOURCE" ]]; then
    echo "Error: --source is required" >&2
    exit 1
fi

if [[ -z "$COUNT" ]]; then
    echo "Error: --count is required" >&2
    exit 1
fi

if [[ ! -f "$SOURCE" ]]; then
    echo "Error: Source file '$SOURCE' not found" >&2
    exit 1
fi

MAX_SIZE_BYTES=$((MAX_SIZE_MB * 1024 * 1024))

# Extract TEs to temp file (one per line, compact)
TEMP_TES=$(mktemp)
trap "rm -f $TEMP_TES" EXIT

jq --compact-output '.trackedEntities[]' "$SOURCE" > "$TEMP_TES"

SRC_COUNT=$(wc -l < "$TEMP_TES")
if [[ "$SRC_COUNT" -eq 0 ]]; then
    echo "Error: No tracked entities in source file" >&2
    exit 1
fi

# Stream output
echo -n '{"trackedEntities":['

CURRENT_SIZE=22  # {"trackedEntities":[]}
WRITTEN=0
FIRST=true

while [[ $WRITTEN -lt $COUNT ]]; do
    while IFS= read -r TE; do
        TE_SIZE=${#TE}
        NEXT_SIZE=$((CURRENT_SIZE + TE_SIZE + 1))  # +1 for comma

        if [[ $NEXT_SIZE -gt $MAX_SIZE_BYTES ]]; then
            break 2
        fi

        if [[ "$FIRST" == "true" ]]; then
            FIRST=false
        else
            echo -n ','
        fi
        echo -n "$TE"

        CURRENT_SIZE=$NEXT_SIZE
        WRITTEN=$((WRITTEN + 1))

        if [[ $WRITTEN -ge $COUNT ]]; then
            break 2
        fi
    done < "$TEMP_TES"
done

echo ']}'

echo "Generated $WRITTEN TEs, ~$((CURRENT_SIZE / 1024 / 1024))MB" >&2
