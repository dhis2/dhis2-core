#!/usr/bin/env python3
"""
Synchronize data integrity i18n entries from YAML check definitions.

By default runs in dry-run mode and prints a summary. Use --write to update
"""

from __future__ import annotations

import argparse
import os
import re
import sys
from typing import List, Tuple, Dict

DEFAULT_CHECKS_LIST = (
    "dhis-2/dhis-services/dhis-service-administration/src/main/resources/"
    "data-integrity-checks.yaml"
)
DEFAULT_I18N = (
    "dhis-2/dhis-services/dhis-service-core/src/main/resources/"
    "i18n_global.properties"
)


def parse_checks_list(path: str) -> List[str]:
    paths: List[str] = []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            match = re.match(r"^\s*-\s+(.+\.yaml)\s*$", line)
            if match:
                paths.append(match.group(1))
    return paths


def _collect_block(lines: List[str], start_index: int, base_indent: int) -> Tuple[str, int]:
    parts: List[str] = []
    i = start_index
    while i < len(lines):
        line = lines[i]
        if line.strip() == "":
            i += 1
            continue
        indent = len(line) - len(line.lstrip(" "))
        if indent <= base_indent:
            break
        parts.append(line.strip())
        i += 1
    return " ".join(parts).strip(), i


def extract_value(lines: List[str], key: str) -> str | None:
    pattern = re.compile(rf"^\s*{re.escape(key)}:\s*(.*)$")
    i = 0
    while i < len(lines):
        line = lines[i]
        if line.lstrip().startswith("#"):
            i += 1
            continue
        match = pattern.match(line)
        if match:
            value = match.group(1).strip()
            if value in {"|", ">", "|-", ">-"}:
                base_indent = len(line) - len(line.lstrip(" "))
                block, next_index = _collect_block(lines, i + 1, base_indent)
                return block
            return value
        i += 1
    return None


def parse_yaml_name_desc(path: str) -> Tuple[str | None, str | None]:
    with open(path, "r", encoding="utf-8") as f:
        lines = f.readlines()
    name = extract_value(lines, "name")
    description = extract_value(lines, "description")
    return name, description


def load_properties(path: str) -> Tuple[List[str], Dict[str, int]]:
    with open(path, "r", encoding="utf-8") as f:
        lines = f.readlines()
    index: Dict[str, int] = {}
    for i, line in enumerate(lines):
        if line.lstrip().startswith("#"):
            continue
        if "=" not in line:
            continue
        key = line.split("=", 1)[0].strip()
        if key:
            index[key] = i
    return lines, index


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Sync data integrity i18n entries from YAML checks"
    )
    parser.add_argument(
        "--checks",
        default=DEFAULT_CHECKS_LIST,
        help="Path to data-integrity-checks.yaml",
    )
    parser.add_argument(
        "--i18n",
        default=DEFAULT_I18N,
        help="Path to i18n_global.properties",
    )
    parser.add_argument(
        "--write",
        action="store_true",
        help="Write updates to i18n_global.properties",
    )
    parser.add_argument(
        "--quiet",
        action="store_true",
        help="Only print errors and final summary",
    )
    args = parser.parse_args()

    checks_list_path = args.checks
    checks_dir = os.path.join(os.path.dirname(checks_list_path), "data-integrity-checks")

    if not os.path.exists(checks_list_path):
        print(f"Checks list not found: {checks_list_path}", file=sys.stderr)
        return 2
    if not os.path.exists(args.i18n):
        print(f"i18n file not found: {args.i18n}", file=sys.stderr)
        return 2

    rel_paths = parse_checks_list(checks_list_path)
    if not rel_paths:
        print("No check files found in checks list", file=sys.stderr)
        return 2

    entries: List[Tuple[str, str, str]] = []
    missing_files: List[str] = []
    missing_fields: List[str] = []

    for rel in rel_paths:
        abs_path = os.path.join(checks_dir, rel)
        if not os.path.exists(abs_path):
            missing_files.append(rel)
            continue
        name, description = parse_yaml_name_desc(abs_path)
        if not name or not description:
            missing_fields.append(rel)
            continue
        entries.append((rel, name, description))

    lines, index = load_properties(args.i18n)

    updated = 0
    unchanged = 0
    added = 0
    missing_keys: List[str] = []
    changed_keys: List[str] = []

    for _, name, description in entries:
        key = f"data_integrity.{name}.name"
        new_line = f"{key}={description}\n"
        if key in index:
            if lines[index[key]] != new_line:
                lines[index[key]] = new_line
                updated += 1
                changed_keys.append(key)
            else:
                unchanged += 1
        else:
            missing_keys.append(new_line)
            added += 1

    insert_at = None
    for i, line in enumerate(lines):
        if line.lstrip().startswith("data_integrity."):
            insert_at = i
    if insert_at is None:
        insert_at = len(lines) - 1

    if missing_keys:
        insert_pos = insert_at + 1
        for new_line in missing_keys:
            lines.insert(insert_pos, new_line)
            insert_pos += 1

    if not args.quiet:
        for key in changed_keys:
            print(f"update: {key}")
        for line in missing_keys:
            print(f"add: {line.strip()}")
        for rel in missing_files:
            print(f"missing file: {rel}")
        for rel in missing_fields:
            print(f"missing fields: {rel}")

    print(
        f"updated={updated} added={added} unchanged={unchanged} "
        f"missing_files={len(missing_files)} missing_fields={len(missing_fields)}"
    )

    if args.write:
        with open(args.i18n, "w", encoding="utf-8") as f:
            f.writelines(lines)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
