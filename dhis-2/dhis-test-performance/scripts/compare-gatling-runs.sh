#!/usr/bin/env bash
# Compares two Gatling simulation results by parsing index.html stats tables.
#
# Usage:
#   ./compare-gatling-runs.sh <baseline-dir> <feature-dir>
#
# Example:
#   ./compare-gatling-runs.sh \
#     target/gatling/usersperformancetest-20260217072013445 \
#     target/gatling/usersperformancetest-20260217073019128

set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <baseline-dir> <feature-dir>"
  echo "  Compares Gatling results from two runs."
  exit 1
fi

BASELINE_DIR="$1"
FEATURE_DIR="$2"

python3 - "$BASELINE_DIR" "$FEATURE_DIR" <<'PYEOF'
import sys, os
from html.parser import HTMLParser

# Gatling 3.14 index.html stats table has group rows (data-parent="ROOT")
# with td cells. The "value total" cells (skipping ok/ko-only cells) are:
#   [0]=Total, [1]=Cnt/s, [2]=Min, [3]=p50, [4]=p75, [5]=p95, [6]=p99,
#   [7]=Max, [8]=Mean, [9]=StdDev

# Requests we care about (setup requests like "Fetch user for PUT" are excluded)
EXCLUDED_PREFIXES = ("Fetch user", "Create user for")

class GatlingStatsParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.results = {}
        self.in_stats_body = False
        self.in_row = False
        self.in_td = False
        self.current_row = []
        self.current_td_text = ""
        self.current_name = None
        self.capture_name = False
        self.td_classes = ""

    def handle_starttag(self, tag, attrs):
        attr_dict = dict(attrs)
        if tag == "table" and "container_statistics_body" in attr_dict.get("id", ""):
            self.in_stats_body = True
        if not self.in_stats_body:
            return
        if tag == "tr":
            parent = attr_dict.get("data-parent", "")
            self.in_row = (parent == "ROOT")
            self.current_row = []
            self.current_name = None
        if tag == "td" and self.in_row:
            self.in_td = True
            self.current_td_text = ""
            self.td_classes = attr_dict.get("class", "")
        if tag == "span" and self.in_td and "ellipsed-name" in attr_dict.get("class", ""):
            self.capture_name = True

    def handle_data(self, data):
        if self.capture_name:
            self.current_name = data.strip()
            self.capture_name = False
        if self.in_td and "value" in self.td_classes and "total" in self.td_classes:
            self.current_td_text += data.strip()

    def handle_endtag(self, tag):
        if tag == "table" and self.in_stats_body:
            self.in_stats_body = False
        if tag == "td" and self.in_td:
            self.in_td = False
            if "value" in self.td_classes and "total" in self.td_classes:
                self.current_row.append(self.current_td_text)
        if tag == "tr" and self.in_row:
            self.in_row = False
            if self.current_name and len(self.current_row) >= 10:
                if not any(self.current_name.startswith(p) for p in EXCLUDED_PREFIXES):
                    try:
                        self.results[self.current_name] = {
                            "total": int(self.current_row[0]),
                            "min": int(self.current_row[2]),
                            "p50": int(self.current_row[3]),
                            "p75": int(self.current_row[4]),
                            "p95": int(self.current_row[5]),
                            "p99": int(self.current_row[6]),
                            "max": int(self.current_row[7]),
                            "mean": int(self.current_row[8]),
                            "stddev": int(self.current_row[9]),
                        }
                    except (ValueError, IndexError):
                        pass

def parse_gatling_html(gatling_dir):
    index_file = os.path.join(gatling_dir, "index.html")
    if not os.path.exists(index_file):
        print(f"Error: {index_file} not found", file=sys.stderr)
        sys.exit(1)
    with open(index_file) as f:
        html = f.read()
    parser = GatlingStatsParser()
    parser.feed(html)
    return parser.results

baseline_dir = sys.argv[1]
feature_dir = sys.argv[2]

baseline = parse_gatling_html(baseline_dir)
feature = parse_gatling_html(feature_dir)

all_names = []
seen = set()
for name in list(baseline.keys()) + list(feature.keys()):
    if name not in seen:
        all_names.append(name)
        seen.add(name)

def fmt_change(pct_str, diff):
    """Add emoji indicator for markdown output."""
    if diff < 0:
        return f":arrow_down: {pct_str}"
    elif diff > 0:
        return f":arrow_up: {pct_str}"
    return pct_str

def compute_change(base_val, feat_val):
    diff = feat_val - base_val
    if base_val > 0:
        pct = ((feat_val - base_val) / base_val) * 100
        pct_str = f"{pct:+.1f}%"
    else:
        pct_str = "N/A"
    return diff, pct_str

print()
print(f"> Baseline: `{os.path.basename(baseline_dir)}`")
print(f"> Feature: `{os.path.basename(feature_dir)}`")

# Mean response time comparison
print()
print("### Mean Response Time (ms)")
print()
print("| Scenario | Baseline | Feature | Diff | Change |")
print("|:---|---:|---:|---:|:---|")

for name in all_names:
    b = baseline.get(name, {})
    f = feature.get(name, {})
    b_mean = b.get("mean", 0)
    f_mean = f.get("mean", 0)
    diff, pct_str = compute_change(b_mean, f_mean)
    display = name.replace(" REQUEST", "")
    print(f"| {display} | {b_mean} | {f_mean} | {diff:+d} | {fmt_change(pct_str, diff)} |")

# p95 response time comparison
print()
print("### 95th Percentile Response Time (ms)")
print()
print("| Scenario | Baseline | Feature | Diff | Change |")
print("|:---|---:|---:|---:|:---|")

for name in all_names:
    b = baseline.get(name, {})
    f = feature.get(name, {})
    b_p95 = b.get("p95", 0)
    f_p95 = f.get("p95", 0)
    diff, pct_str = compute_change(b_p95, f_p95)
    display = name.replace(" REQUEST", "")
    print(f"| {display} | {b_p95} | {f_p95} | {diff:+d} | {fmt_change(pct_str, diff)} |")

print()
print("_:arrow_down: = faster (improvement), :arrow_up: = slower (regression)_")
print()
PYEOF
