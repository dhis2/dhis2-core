#!/usr/bin/env python3
"""
Synchronize data integrity i18n entries from YAML check definitions.

Also supports syncing check definitions from a git ref (e.g. master) into
the current working tree.

By default runs in dry-run mode and prints a summary. Use --write to update
"""

from __future__ import annotations

import argparse
import os
import re
import subprocess
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


def parse_checks_list_from_text(text: str) -> List[str]:
    paths: List[str] = []
    for line in text.splitlines():
        match = re.match(r"^\s*-\s+(.+\.yaml)\s*$", line)
        if match:
            paths.append(match.group(1))
    return paths


def find_git_root() -> str | None:
    try:
        result = subprocess.run(
            ["git", "rev-parse", "--show-toplevel"],
            capture_output=True, text=True, check=True,
        )
        return result.stdout.strip()
    except (subprocess.CalledProcessError, FileNotFoundError):
        return None


def git_show(ref: str, repo_relative_path: str) -> str | None:
    try:
        result = subprocess.run(
            ["git", "show", f"{ref}:{repo_relative_path}"],
            capture_output=True, text=True, check=True,
        )
        return result.stdout
    except subprocess.CalledProcessError:
        return None


def sync_checks_from_ref(
    ref: str,
    checks_list_path: str,
    checks_dir: str,
    write: bool,
    quiet: bool,
) -> int:
    git_root = find_git_root()
    if git_root is None:
        print("Not inside a git repository", file=sys.stderr)
        return 2

    checks_list_abs = os.path.abspath(checks_list_path)
    checks_list_rel = os.path.relpath(checks_list_abs, git_root)
    checks_dir_rel = os.path.join(os.path.dirname(checks_list_rel), "data-integrity-checks")

    ref_text = git_show(ref, checks_list_rel)
    if ref_text is None:
        print(f"Cannot read checks list from ref '{ref}' at {checks_list_rel}", file=sys.stderr)
        return 2

    ref_paths = parse_checks_list_from_text(ref_text)
    local_paths = parse_checks_list(checks_list_path)

    local_set = set(local_paths)
    missing = [p for p in ref_paths if p not in local_set]

    updated_files = 0
    skipped_ref_only: List[str] = []

    shared = [p for p in ref_paths if p in local_set]
    ref_only = [p for p in ref_paths if p not in local_set]
    local_only = [p for p in local_paths if p not in set(ref_paths)]

    for rel_path in shared:
        ref_yaml_rel = os.path.join(checks_dir_rel, rel_path)
        ref_content = git_show(ref, ref_yaml_rel)
        if ref_content is None:
            continue

        local_yaml_path = os.path.join(checks_dir, rel_path)
        if not os.path.exists(local_yaml_path):
            continue

        with open(local_yaml_path, "r", encoding="utf-8") as f:
            local_content = f.read()

        if local_content != ref_content:
            if not quiet:
                print(f"update: {rel_path}")
            if write:
                with open(local_yaml_path, "w", encoding="utf-8") as f:
                    f.write(ref_content)
            updated_files += 1

    if not quiet:
        for p in ref_only:
            print(f"skip (ref only): {p}")
        for p in local_only:
            print(f"skip (local only): {p}")

    print(
        f"sync-from-ref: updated={updated_files} "
        f"ref_only={len(ref_only)} local_only={len(local_only)}"
    )

    return 0


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
    parser.add_argument(
        "--sync-from-ref",
        metavar="REF",
        help="Sync missing check definitions from a git ref (e.g. master)",
    )
    args = parser.parse_args()

    checks_list_path = args.checks
    checks_dir = os.path.join(os.path.dirname(checks_list_path), "data-integrity-checks")

    if not os.path.exists(checks_list_path):
        print(f"Checks list not found: {checks_list_path}", file=sys.stderr)
        return 2

    if args.sync_from_ref:
        rc = sync_checks_from_ref(
            args.sync_from_ref, checks_list_path, checks_dir,
            args.write, args.quiet,
        )
        if rc != 0:
            return rc

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
