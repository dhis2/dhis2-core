#!/bin/bash

set -euo pipefail

# Script to analyze requests with abnormally long connection held times
# This helps identify requests holding connections while doing non-DB work

if [ $# -ne 1 ]; then
  echo "Usage: $0 <connection-raw.csv>"
  echo "  Analyzes requests that hold database connections for unusually long periods"
  exit 1
fi

RAW_CSV="$1"

if [ ! -f "$RAW_CSV" ]; then
  echo "ERROR: File not found: $RAW_CSV"
  exit 1
fi

echo "===== Requests with Long Connection Held Times ====="
echo ""
echo "Analyzing requests where connections are held for >10 seconds..."
echo "This often indicates:"
echo "  - OSIV (Open Session In View) anti-pattern"
echo "  - Application logic running while holding DB connection"
echo "  - Waiting for locks/resources while holding connection"
echo ""

# Find connections held for more than 10 seconds
awk -F',' '
NR > 1 && $5 > 10000 {
  req_id = $2
  held = $5
  wait = $4
  timestamp = $1

  if (held > max_held[req_id]) {
    max_held[req_id] = held
    max_held_time[req_id] = timestamp
  }

  total_held[req_id] += held
  total_wait[req_id] += wait
  count[req_id]++

  # Track all held times for this request
  held_times[req_id] = held_times[req_id] "," held
}
END {
  print "Found", length(max_held), "requests with connections held >10s"
  print ""

  # Sort by max held time
  for (req_id in max_held) {
    print max_held[req_id], req_id
  }
}
' "$RAW_CSV" | sort -rn | head -20 | while read max_held req_id; do
  echo "======================================================================"
  echo "Request ID: $req_id"
  echo "Max single connection held: ${max_held}ms ($(echo "scale=1; $max_held / 1000" | bc)s)"

  # Get full details for this request
  grep "$req_id" "$RAW_CSV" | awk -F',' '
  BEGIN {
    total_held = 0
    total_wait = 0
    count = 0
  }
  {
    total_held += $5
    total_wait += $4
    count++

    if ($5 > 1000) {
      long_held++
    }
  }
  END {
    print "Total connections: " count
    print "Connections held >1s: " long_held
    print "Total wait time: " total_wait "ms (" sprintf("%.1f", total_wait/1000) "s)"
    print "Total held time: " total_held "ms (" sprintf("%.1f", total_held/1000) "s)"
    print "Avg held per connection: " sprintf("%.0f", total_held/count) "ms"
  }
  '

  # Show timeline of connections
  echo ""
  echo "Connection timeline (first 10 and last 5):"
  echo "Timestamp                      Wait(ms)  Held(ms)"
  grep "$req_id" "$RAW_CSV" | awk -F',' '{print $1, $4, $5}' | head -10
  echo "..."
  grep "$req_id" "$RAW_CSV" | awk -F',' '{print $1, $4, $5}' | tail -5
  echo ""
done

echo ""
echo "===== Summary ====="
awk -F',' '
NR > 1 && $5 > 10000 {
  print $5
}
' "$RAW_CSV" | sort -n | awk '
BEGIN {
  count = 0
}
{
  values[count++] = $1
  sum += $1
}
END {
  print "Total long-held connections (>10s): " count
  print "Total time: " sprintf("%.1f", sum/1000) "s"
  print "Mean: " sprintf("%.1f", sum/count/1000) "s"

  if (count > 0) {
    p50 = values[int(count * 0.50)]
    p90 = values[int(count * 0.90)]
    p99 = values[int(count * 0.99)]
    max = values[count-1]

    print "P50: " sprintf("%.1f", p50/1000) "s"
    print "P90: " sprintf("%.1f", p90/1000) "s"
    print "P99: " sprintf("%.1f", p99/1000) "s"
    print "Max: " sprintf("%.1f", max/1000) "s"
  }
}
'
