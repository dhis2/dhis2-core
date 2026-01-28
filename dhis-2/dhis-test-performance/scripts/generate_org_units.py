#!/usr/bin/env python3
"""
DHIS2 Organisation Unit Tree Generator

Generates large org unit hierarchies for performance testing.
Outputs DHIS2 metadata JSON format that can be POSTed to /api/metadata.

Features:
    - Generates OrganisationUnits with proper hierarchy
    - Optionally generates OrganisationUnitGroups and OrganisationUnitGroupSets
    - Assigns org units to groups based on their level (Sierra Leone style)

Usage:
    python generate_org_units.py --preset realistic --target 500000 --output orgunits.json
    python generate_org_units.py --preset wide --target 100000 --batch-size 10000
    python generate_org_units.py --levels 1,50,200,5000 --output custom_tree.json
    python generate_org_units.py --preset realistic --target 60000 --include-groups

Tree Presets:
    - realistic: Mimics real-world hierarchies (Country > Region > District > Facility > CHW)
    - wide: Few levels, many children per node (good for testing breadth queries)
    - deep: Many levels, few children per node (good for testing depth/ancestry queries)
    - custom: User-defined branching factors per level via --levels

Author: Generated for DHIS2 performance testing
"""

import argparse
import json
import math
import os
import random
import string
import sys
from dataclasses import dataclass
from datetime import datetime
from typing import Iterator, Optional


# =============================================================================
# UID Generation (DHIS2 compatible)
# =============================================================================

# DHIS2 UID alphabet: alphanumeric, first character must be a letter
UID_ALPHABET = string.ascii_letters + string.digits
UID_FIRST_CHAR = string.ascii_letters  # First char must be letter


def generate_uid() -> str:
    """Generate a valid DHIS2 UID (11 alphanumeric chars, first must be letter)."""
    first = random.choice(UID_FIRST_CHAR)
    rest = "".join(random.choices(UID_ALPHABET, k=10))
    return first + rest


# =============================================================================
# Naming Patterns
# =============================================================================

# Realistic naming prefixes by level
LEVEL_NAMES = {
    1: ("Country", ["National", "Federal", "Republic of"]),
    2: (
        "Region",
        [
            "Northern",
            "Southern",
            "Eastern",
            "Western",
            "Central",
            "Upper",
            "Lower",
            "Greater",
        ],
    ),
    3: (
        "District",
        ["Metro", "Rural", "Urban", "Coastal", "Highland", "Valley", "Lake", "River"],
    ),
    4: ("Sub-District", ["North", "South", "East", "West", "Central"]),
    5: (
        "Facility",
        ["Health Center", "Clinic", "Hospital", "Dispensary", "Medical Post"],
    ),
    6: ("CHW", ["Community Health Worker", "Village Health Team", "Health Extension"]),
}

# Generic names for levels beyond 6
GENERIC_PREFIXES = ["Unit", "Node", "Branch", "Sector", "Zone", "Area", "Block"]

# Name suffixes for variety
NAME_SUFFIXES = [
    "Alpha",
    "Beta",
    "Gamma",
    "Delta",
    "Epsilon",
    "Zeta",
    "Eta",
    "Theta",
    "One",
    "Two",
    "Three",
    "Four",
    "Five",
    "Six",
    "Seven",
    "Eight",
    "Nine",
    "Ten",
    "A",
    "B",
    "C",
    "D",
    "E",
    "F",
    "G",
    "H",
    "I",
    "J",
    "K",
    "L",
    "M",
    "N",
    "O",
    "P",
]


def generate_name(level: int, index: int, parent_name: Optional[str] = None) -> str:
    """Generate a realistic name for an org unit."""
    if level in LEVEL_NAMES:
        level_type, prefixes = LEVEL_NAMES[level]
        prefix = prefixes[index % len(prefixes)]
        suffix = NAME_SUFFIXES[index % len(NAME_SUFFIXES)]

        if level == 1:
            return f"{prefix} {level_type}"
        elif parent_name:
            # Shorten parent name for child naming
            short_parent = (
                parent_name.split()[0] if " " in parent_name else parent_name[:8]
            )
            return f"{short_parent} {prefix} {level_type}"
        else:
            return f"{prefix} {level_type} {suffix}"
    else:
        # Generic naming for deep trees
        prefix = GENERIC_PREFIXES[(level - 1) % len(GENERIC_PREFIXES)]
        return f"{prefix} L{level}-{index + 1}"


def generate_short_name(name: str) -> str:
    """Generate short name (max 50 chars) from full name."""
    if len(name) <= 50:
        return name
    # Take first letter of each word
    words = name.split()
    if len(words) > 1:
        abbrev = "".join(w[0] for w in words[:-1]) + " " + words[-1]
        return abbrev[:50]
    return name[:50]


def generate_code(level: int, index: int, parent_code: Optional[str] = None) -> str:
    """Generate a hierarchical code."""
    level_code = f"L{level:02d}"
    index_code = f"{index + 1:05d}"

    if parent_code:
        return f"{parent_code}-{index_code}"
    return f"{level_code}-{index_code}"


# =============================================================================
# Tree Generation
# =============================================================================


@dataclass
class OrgUnit:
    """Represents an organisation unit."""

    id: str
    name: str
    short_name: str
    code: str
    opening_date: str
    parent_id: Optional[str]
    level: int
    group_ids: list[str] = None  # IDs of groups this org unit belongs to

    def __post_init__(self):
        if self.group_ids is None:
            self.group_ids = []

    def to_dict(self, include_groups: bool = False) -> dict:
        """Convert to DHIS2 metadata format."""
        result = {
            "id": self.id,
            "name": self.name,
            "shortName": self.short_name,
            "code": self.code,
            "openingDate": self.opening_date,
        }
        if self.parent_id:
            result["parent"] = {"id": self.parent_id}
        if include_groups and self.group_ids:
            result["organisationUnitGroups"] = [{"id": gid} for gid in self.group_ids]
        return result


@dataclass
class OrgUnitGroup:
    """Represents an organisation unit group."""

    id: str
    name: str
    short_name: str
    code: str
    org_unit_ids: list[str] = None  # IDs of org units in this group

    def __post_init__(self):
        if self.org_unit_ids is None:
            self.org_unit_ids = []

    def to_dict(self) -> dict:
        """Convert to DHIS2 metadata format."""
        result = {
            "id": self.id,
            "name": self.name,
            "shortName": self.short_name,
            "code": self.code,
        }
        if self.org_unit_ids:
            result["organisationUnits"] = [{"id": uid} for uid in self.org_unit_ids]
        return result


@dataclass
class OrgUnitGroupSet:
    """Represents an organisation unit group set."""

    id: str
    name: str
    short_name: str
    code: str
    compulsory: bool
    data_dimension: bool
    include_subhierarchy_in_analytics: bool
    group_ids: list[str] = None  # IDs of groups in this set

    def __post_init__(self):
        if self.group_ids is None:
            self.group_ids = []

    def to_dict(self) -> dict:
        """Convert to DHIS2 metadata format."""
        return {
            "id": self.id,
            "name": self.name,
            "shortName": self.short_name,
            "code": self.code,
            "compulsory": self.compulsory,
            "dataDimension": self.data_dimension,
            "includeSubhierarchyInAnalytics": self.include_subhierarchy_in_analytics,
            "organisationUnitGroups": [{"id": gid} for gid in self.group_ids],
        }


# =============================================================================
# Organisation Unit Groups and Group Sets (Sierra Leone style)
# =============================================================================

# Group definitions - based on Sierra Leone DHIS2 instance
# Groups are organized by category for assignment logic

GROUP_DEFINITIONS = {
    # Administrative level groups
    "Country": {"code": "OU_GROUP_COUNTRY", "category": "admin_level"},
    "District": {"code": "OU_GROUP_DISTRICT", "category": "admin_level"},
    "Chiefdom": {"code": "OU_GROUP_CHIEFDOM", "category": "admin_level"},
    # Area/Region groups
    "Northern Area": {"code": "OU_GROUP_NORTH", "category": "area"},
    "Southern Area": {"code": "OU_GROUP_SOUTH", "category": "area"},
    "Eastern Area": {"code": "OU_GROUP_EAST", "category": "area"},
    "Western Area": {"code": "OU_GROUP_WEST", "category": "area"},
    # Facility type groups
    "Hospital": {"code": "OU_GROUP_HOSPITAL", "category": "facility_type"},
    "Clinic": {"code": "OU_GROUP_CLINIC", "category": "facility_type"},
    "CHP": {"code": "OU_GROUP_CHP", "category": "facility_type"},
    "CHC": {"code": "OU_GROUP_CHC", "category": "facility_type"},
    "MCHP": {"code": "OU_GROUP_MCHP", "category": "facility_type"},
    # Ownership groups
    "Public facilities": {"code": "OU_GROUP_PUBLIC", "category": "ownership"},
    "Private Clinic": {"code": "OU_GROUP_PRIVATE", "category": "ownership"},
    "NGO": {"code": "OU_GROUP_NGO", "category": "ownership"},
    "Mission": {"code": "OU_GROUP_MISSION", "category": "ownership"},
    # Location groups
    "Urban": {"code": "OU_GROUP_URBAN", "category": "location"},
    "Rural": {"code": "OU_GROUP_RURAL", "category": "location"},
}

# Group set definitions
GROUP_SET_DEFINITIONS = {
    "Facility Type": {
        "code": "OU_GROUPSET_FACILITY_TYPE",
        "compulsory": True,
        "data_dimension": True,
        "include_subhierarchy": False,
        "groups": ["Hospital", "Clinic", "CHP", "CHC", "MCHP"],
    },
    "Facility Ownership": {
        "code": "OU_GROUPSET_OWNERSHIP",
        "compulsory": True,
        "data_dimension": True,
        "include_subhierarchy": False,
        "groups": ["Public facilities", "Private Clinic", "NGO", "Mission"],
    },
    "Area": {
        "code": "OU_GROUPSET_AREA",
        "compulsory": False,
        "data_dimension": True,
        "include_subhierarchy": True,
        "groups": ["Northern Area", "Southern Area", "Eastern Area", "Western Area"],
    },
    "Location Rural/Urban": {
        "code": "OU_GROUPSET_LOCATION",
        "compulsory": True,
        "data_dimension": True,
        "include_subhierarchy": False,
        "groups": ["Urban", "Rural"],
    },
}


class GroupManager:
    """Manages organisation unit groups and their assignments."""

    def __init__(self):
        self.groups: dict[str, OrgUnitGroup] = {}
        self.group_sets: dict[str, OrgUnitGroupSet] = {}
        self._initialized = False

    def initialize(self):
        """Create all groups and group sets with generated UIDs."""
        if self._initialized:
            return

        # Create groups
        for name, config in GROUP_DEFINITIONS.items():
            group = OrgUnitGroup(
                id=generate_uid(),
                name=name,
                short_name=name[:50],
                code=config["code"],
            )
            self.groups[name] = group

        # Create group sets
        for name, config in GROUP_SET_DEFINITIONS.items():
            group_ids = [self.groups[g].id for g in config["groups"]]
            group_set = OrgUnitGroupSet(
                id=generate_uid(),
                name=name,
                short_name=name[:50],
                code=config["code"],
                compulsory=config["compulsory"],
                data_dimension=config["data_dimension"],
                include_subhierarchy_in_analytics=config["include_subhierarchy"],
                group_ids=group_ids,
            )
            self.group_sets[name] = group_set

        self._initialized = True

    def assign_org_unit_to_groups(self, org_unit: OrgUnit, region_index: int = 0):
        """
        Assign an org unit to appropriate groups based on its level.

        Assignment logic (Sierra Leone style):
        - Level 1 (Country): "Country" group
        - Level 2 (Region): One of the Area groups (Northern/Southern/Eastern/Western)
        - Level 3 (District): "District" group
        - Level 4 (Sub-District): "Chiefdom" group
        - Level 5+ (Facilities): Facility type + Ownership + Location groups
        """
        level = org_unit.level
        group_ids = []

        if level == 1:
            # Country level
            group_ids.append(self.groups["Country"].id)

        elif level == 2:
            # Region level - assign to area group based on index
            area_groups = [
                "Northern Area",
                "Southern Area",
                "Eastern Area",
                "Western Area",
            ]
            area_idx = region_index % len(area_groups)
            group_ids.append(self.groups[area_groups[area_idx]].id)

        elif level == 3:
            # District level
            group_ids.append(self.groups["District"].id)

        elif level == 4:
            # Sub-district / Chiefdom level
            group_ids.append(self.groups["Chiefdom"].id)

        else:
            # Facility level (5+) - assign to multiple groups
            # Facility type (random distribution)
            facility_types = ["Hospital", "Clinic", "CHP", "CHC", "MCHP"]
            # Weight towards CHC/CHP/MCHP (more common) vs Hospital (rare)
            weights = [5, 15, 25, 30, 25]  # Hospital rare, CHC/CHP common
            facility_type = random.choices(facility_types, weights=weights, k=1)[0]
            group_ids.append(self.groups[facility_type].id)

            # Ownership (random distribution)
            ownership_types = ["Public facilities", "Private Clinic", "NGO", "Mission"]
            ownership_weights = [60, 20, 15, 5]  # Public most common
            ownership = random.choices(ownership_types, weights=ownership_weights, k=1)[
                0
            ]
            group_ids.append(self.groups[ownership].id)

            # Location (random distribution)
            location_types = ["Urban", "Rural"]
            location_weights = [30, 70]  # Rural more common
            location = random.choices(location_types, weights=location_weights, k=1)[0]
            group_ids.append(self.groups[location].id)

        # Update org unit with assigned groups
        org_unit.group_ids = group_ids

        # Update groups with this org unit
        for gid in group_ids:
            for group in self.groups.values():
                if group.id == gid:
                    group.org_unit_ids.append(org_unit.id)
                    break

    def get_groups_list(self) -> list[OrgUnitGroup]:
        """Return all groups as a list."""
        return list(self.groups.values())

    def get_group_sets_list(self) -> list[OrgUnitGroupSet]:
        """Return all group sets as a list."""
        return list(self.group_sets.values())


def calculate_tree_structure(
    target_count: int, branching_factors: list[int]
) -> list[int]:
    """
    Calculate how many nodes at each level to reach target count.
    Returns list of counts per level.
    """
    # Calculate cumulative counts
    counts = [1]  # Root level
    cumulative = 1

    for bf in branching_factors:
        next_count = counts[-1] * bf
        cumulative += next_count
        counts.append(next_count)

        if cumulative >= target_count:
            break

    return counts


def estimate_total_from_branching(branching_factors: list[int]) -> int:
    """Estimate total org units from branching factors."""
    total = 1  # Root
    level_count = 1
    for bf in branching_factors:
        level_count *= bf
        total += level_count
    return total


def generate_org_units_streaming(
    branching_factors: list[int],
    opening_date: str = "2020-01-01T00:00:00.000",
    progress_interval: int = 10000,
    group_manager: Optional[GroupManager] = None,
) -> Iterator[OrgUnit]:
    """
    Generate org units using streaming/iteration to handle large trees.

    Args:
        branching_factors: List of children per node at each level
        opening_date: ISO date string for openingDate field
        progress_interval: Print progress every N units
        group_manager: Optional GroupManager for assigning org units to groups

    Yields:
        OrgUnit objects
    """
    # Track generated count for progress
    generated_count = 0
    total_estimate = estimate_total_from_branching(branching_factors)

    # BFS queue: (parent_id, parent_name, parent_code, level, child_index, region_index)
    # Start with root
    root_id = generate_uid()
    root_name = generate_name(1, 0)
    root_code = generate_code(1, 0)

    root = OrgUnit(
        id=root_id,
        name=root_name,
        short_name=generate_short_name(root_name),
        code=root_code,
        opening_date=opening_date,
        parent_id=None,
        level=1,
    )

    # Assign to groups if group manager provided
    if group_manager:
        group_manager.assign_org_unit_to_groups(root, region_index=0)

    yield root
    generated_count += 1

    # Queue for BFS traversal
    # Each entry: (parent_id, parent_name, parent_code, region_index)
    # region_index tracks which area/region the subtree belongs to
    current_level_parents = [(root_id, root_name, root_code, 0)]

    for level_idx, num_children in enumerate(branching_factors):
        current_level = level_idx + 2  # Levels are 1-indexed, we start at level 2
        next_level_parents = []

        for parent_idx, (parent_id, parent_name, parent_code, region_idx) in enumerate(
            current_level_parents
        ):
            for child_idx in range(num_children):
                global_child_idx = parent_idx * num_children + child_idx

                child_id = generate_uid()
                child_name = generate_name(current_level, global_child_idx, parent_name)
                child_code = generate_code(current_level, global_child_idx, parent_code)

                # For level 2 (regions), set the region index based on child_idx
                # This ensures all descendants inherit the same region
                child_region_idx = child_idx if current_level == 2 else region_idx

                child = OrgUnit(
                    id=child_id,
                    name=child_name,
                    short_name=generate_short_name(child_name),
                    code=child_code,
                    opening_date=opening_date,
                    parent_id=parent_id,
                    level=current_level,
                )

                # Assign to groups if group manager provided
                if group_manager:
                    group_manager.assign_org_unit_to_groups(
                        child, region_index=child_region_idx
                    )

                yield child
                generated_count += 1

                # Progress reporting
                if generated_count % progress_interval == 0:
                    pct = (generated_count / total_estimate) * 100
                    print(
                        f"  Generated {generated_count:,} / ~{total_estimate:,} ({pct:.1f}%)",
                        file=sys.stderr,
                    )

                # Add to next level's parents
                next_level_parents.append(
                    (child_id, child_name, child_code, child_region_idx)
                )

        current_level_parents = next_level_parents

        if not current_level_parents:
            break


# =============================================================================
# Presets
# =============================================================================


def get_preset_branching_factors(preset: str, target: int) -> list[int]:
    """
    Get branching factors for a preset tree type.
    Adjusts to approximately reach the target count.
    """
    if preset == "realistic":
        # Mimics: Country > Regions > Districts > Sub-districts > Facilities > CHWs
        # Adjust the last level(s) to hit target
        base = [10, 20, 10, 50]  # 1 > 10 > 200 > 2000 > 100,000
        total = estimate_total_from_branching(base)

        if target > total:
            # Add another level
            remaining = target - total
            last_level_count = base[-1] * 10 * 20 * 10  # Current last level size
            extra_children = max(1, remaining // last_level_count)
            base.append(min(extra_children, 20))

        return adjust_branching_for_target(base, target)

    elif preset == "wide":
        # Few levels, many children: good for breadth testing
        # 1 > 100 > 5000 > 500,000
        base = [100, 50, 100]
        return adjust_branching_for_target(base, target)

    elif preset == "deep":
        # Many levels, few children: good for ancestry/depth testing
        # Each level has 3-5 children, creates tall narrow tree
        base = [4] * 10  # 10 levels with 4 children each ≈ 1.4M
        return adjust_branching_for_target(base, target)

    else:
        raise ValueError(f"Unknown preset: {preset}")


def adjust_branching_for_target(branching_factors: list[int], target: int) -> list[int]:
    """Adjust branching factors to approximately reach target count."""
    current_total = estimate_total_from_branching(branching_factors)

    if current_total >= target:
        # Remove levels or reduce factors
        while current_total > target * 1.5 and len(branching_factors) > 1:
            branching_factors = branching_factors[:-1]
            current_total = estimate_total_from_branching(branching_factors)

        # Fine-tune last level
        if current_total > target:
            # Reduce last branching factor
            while (
                branching_factors[-1] > 1
                and estimate_total_from_branching(branching_factors) > target * 1.1
            ):
                branching_factors[-1] -= 1

    elif current_total < target:
        # Add levels or increase factors
        while current_total < target * 0.5:
            # Try increasing last factor first
            if branching_factors[-1] < 100:
                branching_factors[-1] = min(branching_factors[-1] * 2, 100)
            else:
                # Add a new level
                branching_factors.append(5)
            current_total = estimate_total_from_branching(branching_factors)

    return branching_factors


def parse_custom_levels(levels_str: str) -> list[int]:
    """Parse custom level specification like '1,50,200,5000'."""
    parts = levels_str.split(",")
    factors = []
    prev = 1
    for part in parts:
        count = int(part.strip())
        if count < prev:
            raise ValueError(f"Level counts must be non-decreasing: {levels_str}")
        if prev > 0:
            factors.append(count // prev)
        prev = count
    return factors[1:] if factors else []


# =============================================================================
# Output Writers
# =============================================================================


def write_json_streaming(
    org_units: Iterator[OrgUnit],
    output_file: str,
    batch_size: Optional[int] = None,
    group_manager: Optional[GroupManager] = None,
    include_groups_in_orgunits: bool = False,
) -> int:
    """
    Write org units to JSON file(s).

    Args:
        org_units: Iterator of OrgUnit objects
        output_file: Output file path
        batch_size: If set, split into multiple files with this many units each
        group_manager: Optional GroupManager to include groups/group sets in output
        include_groups_in_orgunits: If True, include group references in org unit objects

    Returns:
        Total number of org units written
    """
    total_count = 0
    file_index = 0
    current_batch = []

    def write_batch(units: list[OrgUnit], filepath: str, is_first_batch: bool = False):
        """Write a batch of org units to a file."""
        data = {
            "organisationUnits": [
                u.to_dict(include_groups=include_groups_in_orgunits) for u in units
            ]
        }

        # Include groups and group sets in the first batch file only
        if group_manager and is_first_batch:
            data["organisationUnitGroups"] = [
                g.to_dict() for g in group_manager.get_groups_list()
            ]
            data["organisationUnitGroupSets"] = [
                gs.to_dict() for gs in group_manager.get_group_sets_list()
            ]

        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)

        msg_parts = [f"{len(units):,} org units"]
        if group_manager and is_first_batch:
            msg_parts.append(f"{len(group_manager.groups)} groups")
            msg_parts.append(f"{len(group_manager.group_sets)} group sets")
        print(f"  Written {', '.join(msg_parts)} to {filepath}", file=sys.stderr)

    for org_unit in org_units:
        current_batch.append(org_unit)
        total_count += 1

        if batch_size and len(current_batch) >= batch_size:
            # Write batch to numbered file
            base, ext = os.path.splitext(output_file)
            batch_file = f"{base}_{file_index:04d}{ext}"
            write_batch(current_batch, batch_file, is_first_batch=(file_index == 0))
            current_batch = []
            file_index += 1

    # Write remaining
    if current_batch:
        if batch_size and file_index > 0:
            base, ext = os.path.splitext(output_file)
            batch_file = f"{base}_{file_index:04d}{ext}"
            write_batch(current_batch, batch_file, is_first_batch=(file_index == 0))
        else:
            write_batch(current_batch, output_file, is_first_batch=True)

    return total_count


# =============================================================================
# Main
# =============================================================================


def main():
    parser = argparse.ArgumentParser(
        description="Generate large DHIS2 organisation unit trees for performance testing.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Generate ~500k org units with realistic hierarchy
  python generate_org_units.py --preset realistic --target 500000 --output orgunits.json

  # Generate with org unit groups and group sets (Sierra Leone style)
  python generate_org_units.py --preset realistic --target 60000 --include-groups --output orgunits.json

  # Generate wide tree, split into 10k-unit batches
  python generate_org_units.py --preset wide --target 100000 --batch-size 10000 --output orgunits.json

  # Generate deep tree (many levels)
  python generate_org_units.py --preset deep --target 50000 --output deep_tree.json

  # Custom branching: 1 root, then 50 children, then 200 each, then 5000 each
  python generate_org_units.py --levels "1,50,10000,500000" --output custom.json

  # Dry run to see estimated tree structure
  python generate_org_units.py --preset realistic --target 500000 --dry-run

Presets:
  realistic  - Country > Region > District > Sub-district > Facility > CHW
  wide       - Few levels, many children (tests breadth queries)
  deep       - Many levels, few children (tests ancestry/depth queries)

Groups (with --include-groups):
  18 organisation unit groups organized into 4 group sets:
  - Facility Type: Hospital, Clinic, CHP, CHC, MCHP
  - Facility Ownership: Public facilities, Private Clinic, NGO, Mission
  - Area: Northern Area, Southern Area, Eastern Area, Western Area
  - Location: Urban, Rural
  - Admin levels: Country, District, Chiefdom
        """,
    )

    # Tree structure options (mutually exclusive)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument(
        "--preset",
        choices=["realistic", "wide", "deep"],
        help="Use a predefined tree structure",
    )
    group.add_argument(
        "--levels",
        type=str,
        help="Custom cumulative counts per level, e.g., '1,50,1000,50000'",
    )
    group.add_argument(
        "--branching",
        type=str,
        help="Custom branching factors per level, e.g., '10,20,50' (10 children at L2, 20 at L3, etc.)",
    )

    # Target count
    parser.add_argument(
        "--target",
        type=int,
        default=100000,
        help="Target number of org units (approximate, default: 100000)",
    )

    # Output options
    parser.add_argument(
        "--output",
        "-o",
        type=str,
        default="org_units.json",
        help="Output file path (default: org_units.json)",
    )
    parser.add_argument(
        "--batch-size",
        type=int,
        help="Split output into multiple files with this many units each",
    )

    # Group generation options
    parser.add_argument(
        "--include-groups",
        action="store_true",
        help="Generate OrganisationUnitGroups and OrganisationUnitGroupSets (Sierra Leone style)",
    )
    parser.add_argument(
        "--no-groups",
        action="store_true",
        help="Explicitly disable group generation (default behavior)",
    )

    # Other options
    parser.add_argument(
        "--opening-date",
        type=str,
        default="2020-01-01T00:00:00.000",
        help="Opening date for all org units (ISO format, default: 2020-01-01)",
    )
    parser.add_argument(
        "--seed", type=int, help="Random seed for reproducible generation"
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Show estimated tree structure without generating",
    )

    args = parser.parse_args()

    # Set random seed if provided
    if args.seed:
        random.seed(args.seed)

    # Determine branching factors
    if args.preset:
        branching_factors = get_preset_branching_factors(args.preset, args.target)
        print(
            f"Using preset '{args.preset}' adjusted for ~{args.target:,} org units",
            file=sys.stderr,
        )
    elif args.levels:
        branching_factors = parse_custom_levels(args.levels)
        print(f"Using custom levels: {args.levels}", file=sys.stderr)
    elif args.branching:
        branching_factors = [int(x.strip()) for x in args.branching.split(",")]
        print(f"Using custom branching factors: {branching_factors}", file=sys.stderr)

    # Calculate estimates
    estimated_total = estimate_total_from_branching(branching_factors)

    print(f"\nTree Structure:", file=sys.stderr)
    print(f"  Branching factors: {branching_factors}", file=sys.stderr)
    print(f"  Number of levels: {len(branching_factors) + 1}", file=sys.stderr)
    print(f"  Estimated total org units: {estimated_total:,}", file=sys.stderr)

    # Show level breakdown
    print(f"\n  Level breakdown:", file=sys.stderr)
    level_count = 1
    print(f"    Level 1: 1 (root)", file=sys.stderr)
    for i, bf in enumerate(branching_factors):
        level_count *= bf
        print(
            f"    Level {i + 2}: {level_count:,} ({bf} children per parent)",
            file=sys.stderr,
        )

    # Show group info if enabled
    include_groups = args.include_groups and not args.no_groups
    if include_groups:
        print(f"\n  Organisation Unit Groups:", file=sys.stderr)
        print(f"    Groups: {len(GROUP_DEFINITIONS)}", file=sys.stderr)
        print(f"    Group Sets: {len(GROUP_SET_DEFINITIONS)}", file=sys.stderr)
        for gs_name, gs_config in GROUP_SET_DEFINITIONS.items():
            print(
                f"      - {gs_name}: {', '.join(gs_config['groups'])}", file=sys.stderr
            )

    if args.dry_run:
        print(
            f"\nDry run complete. Use without --dry-run to generate.", file=sys.stderr
        )
        return

    # Initialize group manager if groups are enabled
    group_manager = None
    if include_groups:
        group_manager = GroupManager()
        group_manager.initialize()
        print(
            f"\nInitialized {len(group_manager.groups)} groups and {len(group_manager.group_sets)} group sets",
            file=sys.stderr,
        )

    # Generate
    print(f"\nGenerating org units...", file=sys.stderr)
    start_time = datetime.now()

    org_units = generate_org_units_streaming(
        branching_factors=branching_factors,
        opening_date=args.opening_date,
        progress_interval=max(10000, estimated_total // 20),
        group_manager=group_manager,
    )

    total_written = write_json_streaming(
        org_units=org_units,
        output_file=args.output,
        batch_size=args.batch_size,
        group_manager=group_manager,
        include_groups_in_orgunits=False,  # Groups reference org units, not vice versa
    )

    elapsed = (datetime.now() - start_time).total_seconds()

    print(f"\nComplete!", file=sys.stderr)
    print(f"  Total org units: {total_written:,}", file=sys.stderr)
    if include_groups:
        print(f"  Total groups: {len(group_manager.groups)}", file=sys.stderr)
        print(f"  Total group sets: {len(group_manager.group_sets)}", file=sys.stderr)
    print(f"  Time elapsed: {elapsed:.1f} seconds", file=sys.stderr)
    print(f"  Rate: {total_written / elapsed:,.0f} org units/second", file=sys.stderr)

    if args.batch_size:
        num_files = math.ceil(total_written / args.batch_size)
        print(f"  Output files: {num_files} files", file=sys.stderr)
        if include_groups:
            base, ext = os.path.splitext(args.output)
            print(
                f"  Note: Groups and group sets are in the first file ({base}_0000{ext})",
                file=sys.stderr,
            )
    else:
        print(f"  Output file: {args.output}", file=sys.stderr)

    print(f"\nTo import into DHIS2:", file=sys.stderr)
    print(f"  curl -X POST -H 'Content-Type: application/json' \\", file=sys.stderr)
    print(f"    -u admin:district \\", file=sys.stderr)
    print(f"    -d @{args.output} \\", file=sys.stderr)
    print(
        f"    'http://localhost:8080/api/metadata?importStrategy=CREATE_AND_UPDATE'",
        file=sys.stderr,
    )


if __name__ == "__main__":
    main()
