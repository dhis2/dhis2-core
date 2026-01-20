#!/usr/bin/env python3
"""Extract P95 from Gatling index.html for Tracker Import request.

Usage: ./extract-p95.py <gatling-report-dir>
       ./extract-p95.py target/gatling/trackerimporttest-*/
"""

import re
import sys
from pathlib import Path


def extract_p95(html_file: Path) -> dict:
    """Extract P95 for Tracker Import from Gatling index.html."""
    content = html_file.read_text()

    # Pattern matches the stats table row for a request
    pattern = (
        r'<span[^>]*class="ellipsed-name"[^>]*>([^<]+)</span>.*?'
        r'<td class="value total col-7">(\d+)</td>\s*'
        r'<td class="value total col-8">(\d+)</td>\s*'
        r'<td class="value total col-9">(\d+)</td>\s*'
        r'<td class="value total col-10">(\d+)</td>\s*'
        r'<td class="value total col-11">(\d+)</td>\s*'
        r'<td class="value total col-12">(\d+)</td>'
    )

    for match in re.finditer(pattern, content, re.DOTALL):
        name = match.group(1).strip()
        if name == "Tracker Import":
            return {
                "name": name,
                "min": int(match.group(2)),
                "p50": int(match.group(3)),
                "p75": int(match.group(4)),
                "p95": int(match.group(5)),
                "p99": int(match.group(6)),
                "max": int(match.group(7)),
            }
    return None


def main():
    if len(sys.argv) < 2:
        print("Usage: ./extract-p95.py <gatling-report-dir>", file=sys.stderr)
        sys.exit(1)

    report_dir = Path(sys.argv[1])

    # Find index.html
    if report_dir.is_file() and report_dir.name == "index.html":
        html_file = report_dir
    else:
        html_file = report_dir / "index.html"
        if not html_file.exists():
            # Try glob for trackerimporttest-* pattern
            matches = list(report_dir.glob("*/index.html"))
            if matches:
                html_file = matches[0]

    if not html_file.exists():
        print(f"Error: index.html not found in {report_dir}", file=sys.stderr)
        sys.exit(1)

    stats = extract_p95(html_file)
    if stats:
        print(f"Request: {stats['name']}")
        print(f"P95: {stats['p95']} ms")
        print(f"P50: {stats['p50']} ms")
        print(f"Min: {stats['min']} ms")
        print(f"Max: {stats['max']} ms")
    else:
        print("Error: Tracker Import request not found in report", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
