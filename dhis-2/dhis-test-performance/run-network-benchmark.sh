#!/bin/bash
# Run tracker import benchmarks against multiple DHIS2 instances
# comparing original vs minimal JSON files.
#
# Usage: ./run-network-benchmark.sh

set -e

INSTANCES=(
    "http://localhost:8080"
    # "https://play.im.dhis2.org/stable-2-41-6-1"
    "https://play.im.dhis2.org/stable-2-42-3-1"
    # "https://play.im.dhis2.org/dev"
)

FILES=(
    "tracker.json"
    "tracker-minimal.json"
)

WARMUP=10
REPEAT=50
TRACKER_ARGS="skipRuleEngine=true&skipSideEffects=true"
SIMULATION_CLASS="org.hisp.dhis.test.tracker.TrackerImportTest"

RESULTS_DIR="target/benchmark-results"
mkdir -p "$RESULTS_DIR"

echo "=== Tracker Import Network Benchmark ==="
echo "Warmup: $WARMUP"
echo "Repeat: $REPEAT"
echo "TrackerArgs: $TRACKER_ARGS"
echo ""

for INSTANCE in "${INSTANCES[@]}"; do
    INSTANCE_NAME=$(echo "$INSTANCE" | sed 's|https\?://||; s|/|-|g')

    for FILE in "${FILES[@]}"; do
        FILE_NAME=$(basename "$FILE" .json)
        RUN_NAME="${INSTANCE_NAME}-${FILE_NAME}"

        echo "----------------------------------------"
        echo "Running: $RUN_NAME"
        echo "Instance: $INSTANCE"
        echo "File: $FILE"
        echo "----------------------------------------"

        # Warmup run (discard)
        echo "Warmup run ($WARMUP iterations)..."
        mvn gatling:test -q \
            -Dgatling.simulationClass="$SIMULATION_CLASS" \
            -Dinstance="$INSTANCE" \
            -DtrackerFile="$FILE" \
            -Drepeat="$WARMUP" \
            -DtrackerArgs="$TRACKER_ARGS" \
            -Dgatling.resultsFolder="$RESULTS_DIR/warmup-$RUN_NAME" \
            2>&1 | tail -5 || echo "Warmup failed (may be expected for localhost if not running)"

        # Actual run
        echo "Benchmark run ($REPEAT iterations)..."
        mvn gatling:test \
            -Dgatling.simulationClass="$SIMULATION_CLASS" \
            -Dinstance="$INSTANCE" \
            -DtrackerFile="$FILE" \
            -Drepeat="$REPEAT" \
            -DtrackerArgs="$TRACKER_ARGS" \
            -Dgatling.resultsFolder="$RESULTS_DIR/$RUN_NAME" \
            2>&1 | tee "$RESULTS_DIR/$RUN_NAME.log" | tail -20

        echo ""
    done
done

echo "=== Benchmark Complete ==="
echo "Results in: $RESULTS_DIR"
echo ""
echo "Extract P95 values with:"
echo "  python3 ~/code/dhis2/notes/issues/DHIS2-20411/extract_p95_html.py $RESULTS_DIR"
