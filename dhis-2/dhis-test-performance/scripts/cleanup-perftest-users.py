#!/usr/bin/env python3
"""
Deletes all perftest_* users from a DHIS2 instance.

Usage:
    python3 scripts/cleanup-perftest-users.py --config scripts/remote-perf.properties
    python3 scripts/cleanup-perftest-users.py --config scripts/remote-perf.properties --dry-run
"""

import argparse
import base64
import json
import sys
import urllib.error
import urllib.request


def load_props(path):
    props = {}
    with open(path) as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, _, v = line.partition("=")
                props[k.strip()] = v.strip()
    return props


def api(url, headers, method="GET"):
    req = urllib.request.Request(url, headers=headers, method=method)
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


def main():
    parser = argparse.ArgumentParser(description="Delete all perftest_* users from a DHIS2 instance.")
    parser.add_argument("--config", required=True, help="Path to .properties file")
    parser.add_argument("--dry-run", action="store_true", help="List users to delete without deleting them")
    args = parser.parse_args()

    props = load_props(args.config)
    base_url = props.get("baseUrl", "http://localhost:8080").rstrip("/")
    username = props.get("username", "admin")
    password = props.get("password", "district")

    auth = base64.b64encode(f"{username}:{password}".encode()).decode()
    headers = {"Authorization": f"Basic {auth}", "Accept": "application/json"}

    print(f"Fetching perftest_* users from {base_url} ...")
    url = f"{base_url}/api/users?filter=username:like:perftest_&fields=id,username&paging=false"
    data = api(url, headers)
    users = data.get("users", [])

    if not users:
        print("No perftest_* users found.")
        return

    print(f"Found {len(users)} perftest_* user(s).")
    if args.dry_run:
        for u in users:
            print(f"  would delete: {u['username']} ({u['id']})")
        return

    deleted = 0
    failed = 0
    for u in users:
        uid, uname = u["id"], u["username"]
        req = urllib.request.Request(
            f"{base_url}/api/users/{uid}", headers=headers, method="DELETE"
        )
        try:
            with urllib.request.urlopen(req):
                print(f"  deleted: {uname} ({uid})")
                deleted += 1
        except urllib.error.HTTPError as e:
            print(f"  FAILED:  {uname} ({uid}) — HTTP {e.code}", file=sys.stderr)
            failed += 1

    print(f"\nDone: {deleted} deleted, {failed} failed.")
    if failed:
        sys.exit(1)


if __name__ == "__main__":
    main()
