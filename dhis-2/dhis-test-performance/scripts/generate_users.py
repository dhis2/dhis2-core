#!/usr/bin/env python3
"""
DHIS2 User Generator for Performance Testing

Generates large numbers of users for performance testing.
Outputs DHIS2 metadata JSON format that can be POSTed to /api/metadata.

IMPORTANT: Passwords are bcrypt-hashed before being written to JSON files.
DHIS2 metadata import does NOT encrypt passwords during import, so the JSON
files must contain pre-hashed bcrypt passwords.

Prerequisites:
    - Python bcrypt library: pip install bcrypt
    - Run generate_org_units.py first to create org unit hierarchy
    - OR have a running DHIS2 server with org units

Usage:
    # Generate users from org unit JSON file
    python generate_users.py --orgunits orgunits.json --target 100000 --output users.json

    # Generate users with batching (for millions of users)
    python generate_users.py --orgunits orgunits.json --target 1000000 --batch-size 50000 --output users.json

    # Fetch org units from DHIS2 server
    python generate_users.py --server http://localhost:8080 --credentials admin:district --target 100000

    # Custom role distribution
    python generate_users.py --orgunits orgunits.json --target 100000 --distribution "0.5,2,10,30,57.5"

    # Dry run to see distribution
    python generate_users.py --orgunits orgunits.json --target 100000 --dry-run

User Distribution (default - realistic hierarchy):
    - Super Admin (0.1%): Assigned to root/level 1 org units
    - Regional Admin (1%): Assigned to level 2 org units
    - District Manager (5%): Assigned to level 3 org units
    - Facility Staff (25%): Assigned to level 4-5 org units
    - Field Worker (68.9%): Assigned to lowest level org units

Author: Generated for DHIS2 performance testing
"""

import argparse
import json
import math
import os
import random
import string
import sys
import urllib.request
import urllib.error
import base64
from dataclasses import dataclass, field
from datetime import datetime
from typing import Iterator, Optional

try:
    import bcrypt
    BCRYPT_AVAILABLE = True
except ImportError:
    BCRYPT_AVAILABLE = False


# =============================================================================
# UID Generation (DHIS2 compatible)
# =============================================================================

UID_ALPHABET = string.ascii_letters + string.digits
UID_FIRST_CHAR = string.ascii_letters  # First char must be letter


def generate_uid() -> str:
    """Generate a valid DHIS2 UID (11 alphanumeric chars, first must be letter)."""
    first = random.choice(UID_FIRST_CHAR)
    rest = "".join(random.choices(UID_ALPHABET, k=10))
    return first + rest


# =============================================================================
# Password Hashing (bcrypt for DHIS2 compatibility)
# =============================================================================

def hash_password(password: str) -> str:
    """
    Hash a password using bcrypt for DHIS2 import compatibility.
    
    DHIS2 metadata import does NOT encrypt passwords during import,
    so JSON files must contain pre-hashed bcrypt passwords.
    
    Args:
        password: Plain text password
        
    Returns:
        bcrypt hash string (e.g., $2a$10$...)
    """
    if not BCRYPT_AVAILABLE:
        raise RuntimeError(
            "bcrypt library is required for password hashing. "
            "Install it with: pip install bcrypt"
        )
    
    # Use cost factor 10 (DHIS2 default)
    salt = bcrypt.gensalt(rounds=10)
    hashed = bcrypt.hashpw(password.encode('utf-8'), salt)
    return hashed.decode('utf-8')


# =============================================================================
# Name Generation
# =============================================================================

FIRST_NAMES = [
    "James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda",
    "William", "Elizabeth", "David", "Barbara", "Richard", "Susan", "Joseph", "Jessica",
    "Thomas", "Sarah", "Charles", "Karen", "Christopher", "Nancy", "Daniel", "Lisa",
    "Matthew", "Betty", "Anthony", "Margaret", "Mark", "Sandra", "Donald", "Ashley",
    "Steven", "Kimberly", "Paul", "Emily", "Andrew", "Donna", "Joshua", "Michelle",
    "Kenneth", "Dorothy", "Kevin", "Carol", "Brian", "Amanda", "George", "Melissa",
    "Edward", "Deborah", "Ronald", "Stephanie", "Timothy", "Rebecca", "Jason", "Sharon",
    "Jeffrey", "Laura", "Ryan", "Cynthia", "Jacob", "Kathleen", "Gary", "Amy",
    "Nicholas", "Angela", "Eric", "Shirley", "Jonathan", "Anna", "Stephen", "Brenda",
    # African names
    "Kwame", "Amara", "Kofi", "Fatima", "Chidi", "Zainab", "Tendai", "Aisha",
    "Oluwaseun", "Chiamaka", "Thabo", "Naledi", "Jabari", "Nneka", "Sipho", "Wanjiku",
    "Emeka", "Adaeze", "Mandla", "Thandiwe", "Obinna", "Chioma", "Kagiso", "Amahle",
    # Asian names
    "Wei", "Mei", "Raj", "Priya", "Arjun", "Deepa", "Vikram", "Ananya", "Sanjay", "Neha",
    "Hiroshi", "Yuki", "Kenji", "Sakura", "Min", "Soo", "Jin", "Hana", "Tuan", "Linh",
]

LAST_NAMES = [
    "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
    "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson",
    "Thomas", "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson",
    "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker",
    "Young", "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores",
    "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell",
    "Carter", "Roberts", "Gomez", "Phillips", "Evans", "Turner", "Diaz", "Parker",
    # African surnames
    "Okonkwo", "Mensah", "Diallo", "Nkomo", "Banda", "Kamau", "Osei", "Traore",
    "Moyo", "Dlamini", "Ndlovu", "Mwangi", "Obi", "Asante", "Zulu", "Kone",
    "Adeyemi", "Okoro", "Boateng", "Sithole", "Abubakar", "Mbeki", "Kenyatta", "Mugabe",
    # Asian surnames
    "Wang", "Li", "Zhang", "Chen", "Patel", "Singh", "Kumar", "Sharma", "Gupta",
    "Kim", "Park", "Choi", "Tanaka", "Yamamoto", "Sato", "Suzuki", "Tran", "Pham",
]


def generate_name() -> tuple[str, str]:
    """Generate a random first and last name."""
    return random.choice(FIRST_NAMES), random.choice(LAST_NAMES)


# =============================================================================
# User Role Types
# =============================================================================

@dataclass
class UserRoleType:
    """Defines a user role type with distribution and level assignment."""
    name: str
    percentage: float
    min_level: int  # Minimum org unit level (1 = root)
    max_level: int  # Maximum org unit level
    multi_orgunit: bool = False  # Whether user can have multiple org units
    data_view_levels_up: int = 0  # How many levels up for data view access


# Default role distribution (realistic hierarchy)
DEFAULT_ROLE_TYPES = [
    UserRoleType("super_admin", 0.1, 1, 1, multi_orgunit=True, data_view_levels_up=0),
    UserRoleType("regional_admin", 1.0, 2, 2, multi_orgunit=True, data_view_levels_up=1),
    UserRoleType("district_manager", 5.0, 3, 3, multi_orgunit=False, data_view_levels_up=1),
    UserRoleType("facility_staff", 25.0, 4, 5, multi_orgunit=False, data_view_levels_up=1),
    UserRoleType("field_worker", 68.9, 5, 99, multi_orgunit=False, data_view_levels_up=0),
]


# =============================================================================
# User Group Configuration (deterministic UIDs from generate_usergroups.py)
# =============================================================================

# Selection weights proportional to membership counts in production data
USER_GROUP_CONFIG = [
    {"id": "YyAo1CZmkDm", "name": "All Users", "weight": 117730},
    {"id": "pbKMMbWCGxJ", "name": "Data Entry", "weight": 69524},
    {"id": "WspuBhs26O5", "name": "Data Viewers", "weight": 15760},
    {"id": "E2kz5YmxsCO", "name": "District Users", "weight": 9385},
    {"id": "jwns6A9Dwtc", "name": "Regional Team", "weight": 8739},
    {"id": "soe2Fv084uC", "name": "Facility Managers", "weight": 373},
    {"id": "YwdEQtPUf8J", "name": "Admin Team", "weight": 174},
    {"id": "mlqRJhpXwS2", "name": "Pilot Group", "weight": 50},
    {"id": "vMUip82RbNl", "name": "System Admins", "weight": 3},
    {"id": "NiCmpZd3y8F", "name": "Super Admins", "weight": 2},
]

# Groups-per-user frequency distribution (from production data)
# 99.96% of users are in exactly 1 group
GROUPS_PER_USER_DISTRIBUTION = [
    (1, 221425), (2, 68), (5, 2), (6, 1),
    (7, 16), (8, 4), (9, 1), (10, 1),
]


# =============================================================================
# Org Unit Loading
# =============================================================================

@dataclass
class OrgUnitInfo:
    """Minimal org unit info needed for user assignment."""
    id: str
    name: str
    level: int
    parent_id: Optional[str] = None


def load_orgunits_from_file(filepath: str) -> list[OrgUnitInfo]:
    """Load org units from a JSON file (output of generate_org_units.py)."""
    print(f"Loading org units from {filepath}...", file=sys.stderr)
    
    orgunits = []
    
    with open(filepath, "r", encoding="utf-8") as f:
        data = json.load(f)
    
    raw_units = data.get("organisationUnits", [])
    
    # First pass: collect all units
    units_by_id = {}
    for unit in raw_units:
        ou = OrgUnitInfo(
            id=unit["id"],
            name=unit.get("name", unit["id"]),
            level=0,  # Will calculate
            parent_id=unit.get("parent", {}).get("id") if unit.get("parent") else None
        )
        units_by_id[ou.id] = ou
    
    # Second pass: calculate levels
    def get_level(unit_id: str, visited: set) -> int:
        if unit_id in visited:
            return 1  # Cycle protection
        visited.add(unit_id)
        
        unit = units_by_id.get(unit_id)
        if not unit or not unit.parent_id:
            return 1
        return get_level(unit.parent_id, visited) + 1
    
    for unit_id, unit in units_by_id.items():
        unit.level = get_level(unit_id, set())
        orgunits.append(unit)
    
    print(f"  Loaded {len(orgunits):,} org units", file=sys.stderr)
    return orgunits


def load_orgunits_from_files(filepaths: list[str]) -> list[OrgUnitInfo]:
    """Load org units from multiple JSON files (batched output)."""
    all_units = []
    for filepath in filepaths:
        units = load_orgunits_from_file(filepath)
        all_units.extend(units)
    
    # Recalculate levels across all files
    units_by_id = {u.id: u for u in all_units}
    
    def get_level(unit_id: str, visited: set) -> int:
        if unit_id in visited:
            return 1
        visited.add(unit_id)
        unit = units_by_id.get(unit_id)
        if not unit or not unit.parent_id:
            return 1
        return get_level(unit.parent_id, visited) + 1
    
    for unit in all_units:
        unit.level = get_level(unit.id, set())
    
    return all_units


def load_orgunits_from_server(
    server_url: str,
    credentials: str,
    page_size: int = 10000
) -> list[OrgUnitInfo]:
    """Fetch org units from a DHIS2 server."""
    print(f"Fetching org units from {server_url}...", file=sys.stderr)
    
    orgunits = []
    page = 1
    
    # Setup authentication
    username, password = credentials.split(":", 1)
    auth_header = base64.b64encode(f"{username}:{password}".encode()).decode()
    
    while True:
        url = (
            f"{server_url.rstrip('/')}/api/organisationUnits"
            f"?fields=id,name,level,parent[id]"
            f"&paging=true&page={page}&pageSize={page_size}"
        )
        
        request = urllib.request.Request(url)
        request.add_header("Authorization", f"Basic {auth_header}")
        request.add_header("Accept", "application/json")
        
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                data = json.loads(response.read().decode())
        except urllib.error.URLError as e:
            print(f"Error fetching org units: {e}", file=sys.stderr)
            sys.exit(1)
        
        units = data.get("organisationUnits", [])
        if not units:
            break
        
        for unit in units:
            orgunits.append(OrgUnitInfo(
                id=unit["id"],
                name=unit.get("name", unit["id"]),
                level=unit.get("level", 1),
                parent_id=unit.get("parent", {}).get("id") if unit.get("parent") else None
            ))
        
        pager = data.get("pager", {})
        total = pager.get("total", len(orgunits))
        print(f"  Fetched page {page}: {len(orgunits):,} / {total:,} org units", file=sys.stderr)
        
        if page >= pager.get("pageCount", 1):
            break
        page += 1
    
    print(f"  Total: {len(orgunits):,} org units loaded", file=sys.stderr)
    return orgunits


def group_orgunits_by_level(orgunits: list[OrgUnitInfo]) -> dict[int, list[OrgUnitInfo]]:
    """Group org units by their level."""
    by_level: dict[int, list[OrgUnitInfo]] = {}
    for ou in orgunits:
        if ou.level not in by_level:
            by_level[ou.level] = []
        by_level[ou.level].append(ou)
    return by_level


# =============================================================================
# User Generation
# =============================================================================

def assign_user_groups() -> list[str]:
    """
    Assign user groups to a user based on production-like distribution.

    Returns a list of user group UIDs. 99.96% of users get exactly 1 group.
    """
    # Pick how many groups this user belongs to
    counts, weights = zip(*GROUPS_PER_USER_DISTRIBUTION)
    num_groups = random.choices(counts, weights=weights, k=1)[0]

    # Pick which groups (weighted, no duplicates)
    group_ids = [g["id"] for g in USER_GROUP_CONFIG]
    group_weights = [g["weight"] for g in USER_GROUP_CONFIG]

    num_groups = min(num_groups, len(group_ids))

    # Weighted sampling without replacement
    selected = []
    remaining_ids = list(group_ids)
    remaining_weights = list(group_weights)
    for _ in range(num_groups):
        if not remaining_ids:
            break
        chosen = random.choices(range(len(remaining_ids)), weights=remaining_weights, k=1)[0]
        selected.append(remaining_ids[chosen])
        remaining_ids.pop(chosen)
        remaining_weights.pop(chosen)

    return selected


@dataclass
class User:
    """Represents a DHIS2 user."""
    id: str
    username: str
    first_name: str
    surname: str
    password_hash: str  # bcrypt hashed password
    user_role_ids: list[str]
    orgunit_ids: list[str]
    data_view_orgunit_ids: list[str]
    role_type: str  # For tracking/debugging
    user_group_ids: list[str] = field(default_factory=list)

    def to_dict(self) -> dict:
        """Convert to DHIS2 metadata format."""
        result = {
            "id": self.id,
            "username": self.username,
            "firstName": self.first_name,
            "surname": self.surname,
            "password": self.password_hash,  # DHIS2 expects "password" field with bcrypt hash
            "userRoles": [{"id": rid} for rid in self.user_role_ids],
            "organisationUnits": [{"id": oid} for oid in self.orgunit_ids],
        }
        if self.data_view_orgunit_ids:
            result["dataViewOrganisationUnits"] = [
                {"id": oid} for oid in self.data_view_orgunit_ids
            ]
        if self.user_group_ids:
            result["userGroups"] = [{"id": gid} for gid in self.user_group_ids]
        return result


def calculate_user_distribution(
    target_count: int,
    role_types: list[UserRoleType],
    orgunits_by_level: dict[int, list[OrgUnitInfo]]
) -> dict[str, int]:
    """Calculate how many users of each role type to create."""
    max_level = max(orgunits_by_level.keys())
    distribution = {}
    
    # Normalize percentages
    total_pct = sum(rt.percentage for rt in role_types)
    
    for rt in role_types:
        # Calculate target count for this role
        role_count = int((rt.percentage / total_pct) * target_count)
        
        # Check if we have org units at the required levels
        available_levels = [
            lvl for lvl in range(rt.min_level, min(rt.max_level, max_level) + 1)
            if lvl in orgunits_by_level
        ]
        
        if not available_levels:
            print(f"  Warning: No org units available for {rt.name} (levels {rt.min_level}-{rt.max_level})", 
                  file=sys.stderr)
            role_count = 0
        
        distribution[rt.name] = role_count
    
    # Adjust to hit target exactly
    current_total = sum(distribution.values())
    diff = target_count - current_total
    
    # Add/remove from the largest group
    if diff != 0:
        largest_role = max(distribution, key=distribution.get)
        distribution[largest_role] += diff
    
    return distribution


def generate_users_streaming(
    target_count: int,
    orgunits: list[OrgUnitInfo],
    role_types: list[UserRoleType],
    user_role_id: str,
    username_prefix: str = "user",
    password: str = "Test123!",
    progress_interval: int = 10000,
    enable_user_groups: bool = True,
) -> Iterator[User]:
    """
    Generate users using streaming/iteration to handle large counts.

    Args:
        target_count: Number of users to generate
        orgunits: List of org units to assign users to
        role_types: Role type definitions with distribution
        user_role_id: DHIS2 UserRole ID to assign to all users
        username_prefix: Prefix for usernames
        password: Password for all users (will be bcrypt hashed)
        progress_interval: Print progress every N users
        enable_user_groups: Whether to assign user groups (default: True)

    Yields:
        User objects
    """
    # Hash the password once (same hash for all users since they share the same password)
    print(f"Hashing password with bcrypt...", file=sys.stderr)
    password_hash = hash_password(password)
    print(f"  Password hash generated: {password_hash[:20]}...", file=sys.stderr)
    
    # Group org units by level
    orgunits_by_level = group_orgunits_by_level(orgunits)
    max_level = max(orgunits_by_level.keys())
    
    # Build parent lookup for data view org unit calculation
    orgunits_by_id = {ou.id: ou for ou in orgunits}
    
    print(f"\nOrg unit levels found:", file=sys.stderr)
    for level in sorted(orgunits_by_level.keys()):
        print(f"  Level {level}: {len(orgunits_by_level[level]):,} org units", file=sys.stderr)
    
    # Calculate distribution
    distribution = calculate_user_distribution(target_count, role_types, orgunits_by_level)
    
    print(f"\nUser distribution:", file=sys.stderr)
    for role_name, count in distribution.items():
        pct = (count / target_count) * 100
        print(f"  {role_name}: {count:,} ({pct:.1f}%)", file=sys.stderr)
    
    # Generate users
    generated_count = 0
    user_index = 0
    
    for rt in role_types:
        role_count = distribution.get(rt.name, 0)
        if role_count == 0:
            continue
        
        # Get available org units for this role type
        available_levels = [
            lvl for lvl in range(rt.min_level, min(rt.max_level, max_level) + 1)
            if lvl in orgunits_by_level
        ]
        
        if not available_levels:
            continue
        
        # Collect all org units at available levels
        available_orgunits = []
        for lvl in available_levels:
            available_orgunits.extend(orgunits_by_level[lvl])
        
        for i in range(role_count):
            user_index += 1
            
            # Generate user identity
            user_id = generate_uid()
            username = f"{username_prefix}_{user_index:07d}"
            first_name, surname = generate_name()
            
            # Assign org unit(s)
            if rt.multi_orgunit and len(available_orgunits) > 1:
                # Assign 2-5 org units for multi-orgunit roles
                num_orgunits = min(random.randint(2, 5), len(available_orgunits))
                assigned_orgunits = random.sample(available_orgunits, num_orgunits)
            else:
                assigned_orgunits = [random.choice(available_orgunits)]
            
            orgunit_ids = [ou.id for ou in assigned_orgunits]
            
            # Calculate data view org units (same or parent level)
            data_view_ids = set(orgunit_ids)
            if rt.data_view_levels_up > 0:
                for ou in assigned_orgunits:
                    # Walk up the tree
                    current = ou
                    for _ in range(rt.data_view_levels_up):
                        if current.parent_id and current.parent_id in orgunits_by_id:
                            parent = orgunits_by_id[current.parent_id]
                            data_view_ids.add(parent.id)
                            current = parent
                        else:
                            break
            
            # Assign user groups (probabilistic)
            user_group_ids = assign_user_groups() if enable_user_groups else []

            user = User(
                id=user_id,
                username=username,
                first_name=first_name,
                surname=surname,
                password_hash=password_hash,
                user_role_ids=[user_role_id],
                orgunit_ids=orgunit_ids,
                data_view_orgunit_ids=list(data_view_ids),
                role_type=rt.name,
                user_group_ids=user_group_ids,
            )
            
            yield user
            generated_count += 1
            
            # Progress reporting
            if generated_count % progress_interval == 0:
                pct = (generated_count / target_count) * 100
                print(f"  Generated {generated_count:,} / {target_count:,} ({pct:.1f}%)", file=sys.stderr)


# =============================================================================
# Output Writers
# =============================================================================

def write_json_streaming(
    users: Iterator[User],
    output_file: str,
    batch_size: Optional[int] = None,
) -> int:
    """
    Write users to JSON file(s).
    
    Args:
        users: Iterator of User objects
        output_file: Output file path
        batch_size: If set, split into multiple files with this many users each
    
    Returns:
        Total number of users written
    """
    total_count = 0
    file_index = 0
    current_batch = []
    
    def write_batch(user_list: list[User], filepath: str):
        data = {"users": [u.to_dict() for u in user_list]}
        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        print(f"  Written {len(user_list):,} users to {filepath}", file=sys.stderr)
    
    for user in users:
        current_batch.append(user)
        total_count += 1
        
        if batch_size and len(current_batch) >= batch_size:
            base, ext = os.path.splitext(output_file)
            batch_file = f"{base}_{file_index:04d}{ext}"
            write_batch(current_batch, batch_file)
            current_batch = []
            file_index += 1
    
    # Write remaining
    if current_batch:
        if batch_size and file_index > 0:
            base, ext = os.path.splitext(output_file)
            batch_file = f"{base}_{file_index:04d}{ext}"
            write_batch(current_batch, batch_file)
        else:
            write_batch(current_batch, output_file)
    
    return total_count


# =============================================================================
# Main
# =============================================================================

def parse_distribution(dist_str: str) -> list[UserRoleType]:
    """Parse custom distribution string like '0.5,2,10,30,57.5'."""
    parts = [float(x.strip()) for x in dist_str.split(",")]
    
    if len(parts) != 5:
        raise ValueError(f"Distribution must have 5 values, got {len(parts)}")
    
    return [
        UserRoleType("super_admin", parts[0], 1, 1, multi_orgunit=True, data_view_levels_up=0),
        UserRoleType("regional_admin", parts[1], 2, 2, multi_orgunit=True, data_view_levels_up=1),
        UserRoleType("district_manager", parts[2], 3, 3, multi_orgunit=False, data_view_levels_up=1),
        UserRoleType("facility_staff", parts[3], 4, 5, multi_orgunit=False, data_view_levels_up=1),
        UserRoleType("field_worker", parts[4], 5, 99, multi_orgunit=False, data_view_levels_up=0),
    ]


def find_batch_files(base_path: str) -> list[str]:
    """Find all batch files matching base_path pattern (e.g., orgunits_0000.json, ...)."""
    base, ext = os.path.splitext(base_path)
    files = []
    
    # Check if the base file exists (non-batched)
    if os.path.exists(base_path):
        files.append(base_path)
    
    # Check for batch files
    index = 0
    while True:
        batch_file = f"{base}_{index:04d}{ext}"
        if os.path.exists(batch_file):
            files.append(batch_file)
            index += 1
        else:
            break
    
    return files


def main():
    parser = argparse.ArgumentParser(
        description="Generate large numbers of DHIS2 users for performance testing.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Generate 100k users from org unit JSON file
  python generate_users.py --orgunits orgunits.json --target 100000 --output users.json

  # Generate 1M users with batching
  python generate_users.py --orgunits orgunits.json --target 1000000 --batch-size 50000 --output users.json

  # Fetch org units from server
  python generate_users.py --server http://localhost:8080 --credentials admin:district --target 100000

  # Custom role distribution (super,regional,district,facility,field percentages)
  python generate_users.py --orgunits orgunits.json --target 100000 --distribution "0.5,2,10,30,57.5"

  # Dry run to see distribution
  python generate_users.py --orgunits orgunits.json --target 100000 --dry-run

Default Distribution:
  - Super Admin (0.1%): Level 1 (root), multi-orgunit
  - Regional Admin (1%): Level 2, multi-orgunit  
  - District Manager (5%): Level 3
  - Facility Staff (25%): Level 4-5
  - Field Worker (68.9%): Level 5+
        """,
    )

    # Org unit source (mutually exclusive)
    source_group = parser.add_mutually_exclusive_group(required=True)
    source_group.add_argument(
        "--orgunits",
        type=str,
        help="Path to org units JSON file (from generate_org_units.py). Supports batched files.",
    )
    source_group.add_argument(
        "--server",
        type=str,
        help="DHIS2 server URL to fetch org units from",
    )

    # Server credentials
    parser.add_argument(
        "--credentials",
        type=str,
        default="admin:district",
        help="Server credentials as user:password (default: admin:district)",
    )

    # Target count
    parser.add_argument(
        "--target",
        type=int,
        default=10000,
        help="Target number of users to generate (default: 10000)",
    )

    # Output options
    parser.add_argument(
        "--output",
        "-o",
        type=str,
        default="users.json",
        help="Output file path (default: users.json)",
    )
    parser.add_argument(
        "--batch-size",
        type=int,
        help="Split output into multiple files with this many users each",
    )

    # User generation options
    parser.add_argument(
        "--user-role-id",
        type=str,
        default="yrB6vc5Ip3r",
        help="DHIS2 UserRole ID to assign to users (default: yrB6vc5Ip3r - Superuser)",
    )
    parser.add_argument(
        "--username-prefix",
        type=str,
        default="perftest",
        help="Username prefix (default: perftest)",
    )
    parser.add_argument(
        "--password",
        type=str,
        default="Test123!",
        help="Password for all users - will be bcrypt hashed (default: Test123!)",
    )
    parser.add_argument(
        "--distribution",
        type=str,
        help="Custom percentage distribution: 'super,regional,district,facility,field'",
    )

    # User group options
    parser.add_argument(
        "--user-groups",
        action=argparse.BooleanOptionalAction,
        default=True,
        help="Assign user groups to users (default: enabled, use --no-user-groups to disable)",
    )

    # Other options
    parser.add_argument(
        "--seed",
        type=int,
        help="Random seed for reproducible generation",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Show distribution without generating users",
    )

    args = parser.parse_args()

    # Set random seed if provided
    if args.seed:
        random.seed(args.seed)

    # Load org units
    if args.orgunits:
        files = find_batch_files(args.orgunits)
        if not files:
            print(f"Error: No org unit files found matching {args.orgunits}", file=sys.stderr)
            sys.exit(1)
        
        if len(files) > 1:
            print(f"Found {len(files)} batch files", file=sys.stderr)
            orgunits = load_orgunits_from_files(files)
        else:
            orgunits = load_orgunits_from_file(files[0])
    else:
        orgunits = load_orgunits_from_server(args.server, args.credentials)

    if not orgunits:
        print("Error: No org units found!", file=sys.stderr)
        sys.exit(1)

    # Parse distribution
    if args.distribution:
        role_types = parse_distribution(args.distribution)
    else:
        role_types = DEFAULT_ROLE_TYPES

    # Group org units by level for analysis
    orgunits_by_level = group_orgunits_by_level(orgunits)

    print(f"\nConfiguration:", file=sys.stderr)
    print(f"  Target users: {args.target:,}", file=sys.stderr)
    print(f"  Total org units: {len(orgunits):,}", file=sys.stderr)
    print(f"  Org unit levels: {min(orgunits_by_level.keys())} - {max(orgunits_by_level.keys())}", file=sys.stderr)
    print(f"  User role ID: {args.user_role_id}", file=sys.stderr)
    print(f"  Username prefix: {args.username_prefix}", file=sys.stderr)

    # Show distribution preview
    distribution = calculate_user_distribution(args.target, role_types, orgunits_by_level)
    
    print(f"\nPlanned user distribution:", file=sys.stderr)
    for role_name, count in distribution.items():
        pct = (count / args.target) * 100
        print(f"  {role_name}: {count:,} ({pct:.1f}%)", file=sys.stderr)

    if args.dry_run:
        print(f"\nDry run complete. Use without --dry-run to generate.", file=sys.stderr)
        return

    # Generate
    print(f"\nGenerating users...", file=sys.stderr)
    start_time = datetime.now()

    if args.user_groups:
        print(f"  User groups: enabled ({len(USER_GROUP_CONFIG)} groups)", file=sys.stderr)
    else:
        print(f"  User groups: disabled", file=sys.stderr)

    users = generate_users_streaming(
        target_count=args.target,
        orgunits=orgunits,
        role_types=role_types,
        user_role_id=args.user_role_id,
        username_prefix=args.username_prefix,
        password=args.password,
        progress_interval=max(10000, args.target // 20),
        enable_user_groups=args.user_groups,
    )

    total_written = write_json_streaming(
        users=users,
        output_file=args.output,
        batch_size=args.batch_size,
    )

    elapsed = (datetime.now() - start_time).total_seconds()

    print(f"\nComplete!", file=sys.stderr)
    print(f"  Total users: {total_written:,}", file=sys.stderr)
    print(f"  Time elapsed: {elapsed:.1f} seconds", file=sys.stderr)
    print(f"  Rate: {total_written / elapsed:,.0f} users/second", file=sys.stderr)

    if args.batch_size:
        num_files = math.ceil(total_written / args.batch_size)
        print(f"  Output files: {num_files} files", file=sys.stderr)
    else:
        print(f"  Output file: {args.output}", file=sys.stderr)

    print(f"\nTo import into DHIS2:", file=sys.stderr)
    print(f"  curl -X POST -H 'Content-Type: application/json' \\", file=sys.stderr)
    print(f"    -u admin:district \\", file=sys.stderr)
    print(f"    -d @{args.output} \\", file=sys.stderr)
    print(f"    'http://localhost:8080/api/metadata?importStrategy=CREATE_AND_UPDATE'", file=sys.stderr)

    print(f"\nNotes:", file=sys.stderr)
    print(f"  - Passwords are bcrypt-hashed (login with: {args.password})", file=sys.stderr)
    print(f"  - Make sure the UserRole ID ({args.user_role_id}) exists in your DHIS2 instance.", file=sys.stderr)
    print(f"  - Check roles with: curl -u admin:district 'http://localhost:8080/api/userRoles?fields=id,name'", file=sys.stderr)


if __name__ == "__main__":
    main()
