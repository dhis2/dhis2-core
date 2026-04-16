#!/usr/bin/env python3
"""
Analyze Gatling simulation.csv results across multiple runs to derive calibrated
assertion thresholds for UsersPerformanceTest.java.

Usage:
    python3 analyze-user-perf-results.py [--dir ./gatling-downloads] [--slack 1.5]

    --dir    Root directory containing per-run subdirectories (default: ./gatling-downloads)
    --slack  Multiplier applied to observed p95/max to produce suggested thresholds (default: 1.5)

The script walks all subdirectories, finds simulation.csv files, parses request rows,
and prints:
  1. Per-run summary table
  2. Aggregate stats across all runs
  3. Suggested assertion thresholds (observed + slack headroom)
"""

import argparse
import csv
import sys
from collections import defaultdict
from pathlib import Path


def percentile(sorted_vals, p):
    if not sorted_vals:
        return 0
    idx = int(len(sorted_vals) * p / 100)
    idx = min(idx, len(sorted_vals) - 1)
    return sorted_vals[idx]


def load_csv(path: Path) -> dict[str, list[int]]:
    """Parse a simulation.csv and return {request_name: [response_time_ms, ...]} for OK requests."""
    times: dict[str, list[int]] = defaultdict(list)
    try:
        with path.open(newline="") as f:
            reader = csv.DictReader(f)
            for row in reader:
                if row.get("record_type") != "request":
                    continue
                if row.get("status") != "OK":
                    continue
                name = row.get("request_name", "").strip()
                rt = row.get("response_time_ms", "").strip()
                if name and rt:
                    try:
                        times[name].append(int(rt))
                    except ValueError:
                        pass
    except Exception as e:
        print(f"  Warning: could not parse {path}: {e}", file=sys.stderr)
    return times


def stats(vals: list[int]) -> dict:
    if not vals:
        return {"n": 0, "min": 0, "p50": 0, "p75": 0, "p95": 0, "p99": 0, "max": 0}
    s = sorted(vals)
    return {
        "n": len(s),
        "min": s[0],
        "p50": percentile(s, 50),
        "p75": percentile(s, 75),
        "p95": percentile(s, 95),
        "p99": percentile(s, 99),
        "max": s[-1],
    }


def find_csvs(root: Path) -> list[tuple[str, Path]]:
    """Find all simulation.csv files, skipping warmup runs, returning (run_label, path) pairs sorted by label."""
    found = []
    for csv_path in sorted(root.rglob("simulation.csv")):
        # Skip warmup runs — they reflect cold JVM state, not steady-state performance
        if "warmup" in csv_path.parts[-2].lower():
            continue
        # Label: the top-level subdir under root (date_runid)
        parts = csv_path.relative_to(root).parts
        label = parts[0] if parts else str(csv_path.parent.name)
        found.append((label, csv_path))
    return found


def print_run_table(label: str, run_data: dict[str, list[int]]):
    print(f"\n### Run: {label}")
    header = f"{'Scenario':<38} {'n':>4} {'min':>5} {'p50':>5} {'p75':>5} {'p95':>5} {'p99':>5} {'max':>5}"
    print(header)
    print("-" * len(header))
    for name in sorted(run_data):
        s = stats(run_data[name])
        print(
            f"{name:<38} {s['n']:>4} {s['min']:>5} {s['p50']:>5} {s['p75']:>5} "
            f"{s['p95']:>5} {s['p99']:>5} {s['max']:>5}"
        )


def suggest_threshold(observed: int, slack: float) -> int:
    """Round up to the nearest 50ms after applying slack."""
    raw = observed * slack
    return int((raw + 49) // 50 * 50)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--dir", default="./gatling-downloads", help="Root directory of downloaded results")
    parser.add_argument("--slack", type=float, default=1.5, help="Slack multiplier for suggested thresholds (default: 1.5)")
    args = parser.parse_args()

    root = Path(args.dir)
    if not root.exists():
        print(f"Error: directory not found: {root}", file=sys.stderr)
        sys.exit(1)

    csvs = find_csvs(root)
    if not csvs:
        print(f"No simulation.csv files found under {root}", file=sys.stderr)
        sys.exit(1)

    print(f"Found {len(csvs)} simulation.csv file(s) under {root}")
    print(f"Slack multiplier: {args.slack}x\n")

    # Per-run tables
    all_times: dict[str, list[int]] = defaultdict(list)
    for label, csv_path in csvs:
        run_data = load_csv(csv_path)
        print_run_table(label, run_data)
        for name, times in run_data.items():
            all_times[name].extend(times)

    # Aggregate table
    print("\n\n" + "=" * 70)
    print("## AGGREGATE STATISTICS (all runs combined)")
    print("=" * 70)
    header = f"{'Scenario':<38} {'n':>5} {'min':>5} {'p50':>5} {'p75':>5} {'p95':>5} {'p99':>5} {'max':>5}"
    print(header)
    print("-" * len(header))

    agg_stats = {}
    for name in sorted(all_times):
        s = stats(all_times[name])
        agg_stats[name] = s
        print(
            f"{name:<38} {s['n']:>5} {s['min']:>5} {s['p50']:>5} {s['p75']:>5} "
            f"{s['p95']:>5} {s['p99']:>5} {s['max']:>5}"
        )

    # Suggested thresholds
    print("\n\n" + "=" * 70)
    print(f"## SUGGESTED ASSERTION THRESHOLDS (slack: {args.slack}x observed p95/max)")
    print("## Copy these into UsersPerformanceTest.java assertions block")
    print("=" * 70)

    # Map scenario names to Java constants
    java_const = {
        "POST User - create":          ("POST_REQUEST",         "post"),
        "GET User - by uid":           ("GET_REQUEST",          "get"),
        "PUT User - full update":      ("PUT_REQUEST",          "put"),
        "PATCH User - partial update": ("PATCH_REQUEST",        "patch"),
        "PATCH User - replace userGroups": ("PATCH_GROUPS_REQUEST", "patchGroups"),
        "POST User - replicate":       ("REPLICA_REQUEST",      "replica"),
        "DELETE User - delete":        ("DELETE_REQUEST",       "delete"),
    }

    for name in sorted(agg_stats):
        s = agg_stats[name]
        p95_thresh = suggest_threshold(s["p95"], args.slack)
        max_thresh = suggest_threshold(s["max"], args.slack)
        const, _ = java_const.get(name, (f'"{name}"', "?"))
        print(f"\n// {name}  [observed p95={s['p95']}ms, max={s['max']}ms across {s['n']} requests]")
        print(f"details({const}).responseTime().percentile(95).lt({p95_thresh}),")
        print(f"details({const}).responseTime().max().lt({max_thresh}),")
        print(f"details({const}).successfulRequests().percent().is(100D),")


if __name__ == "__main__":
    main()
