# DHIS2 Organisation Unit Tree Generator

A Python script for generating large organisation unit hierarchies for performance testing. Creates DHIS2-compatible metadata JSON that can be imported via the `/api/metadata` endpoint.

## Overview

The Sierra Leone demo database contains ~1,500 org units. For performance testing scenarios that require much larger hierarchies (500k+), this script generates realistic org unit trees with:

- Valid DHIS2 UIDs (11-char alphanumeric, starting with a letter)
- Proper parent-child relationships
- Realistic naming patterns (Country → Region → District → Facility → CHW)
- Hierarchical codes for easy identification
- Configurable tree shapes (wide, deep, realistic)
- **Optional**: Organisation Unit Groups and Group Sets (Sierra Leone style)

## Prerequisites

- Python 3.10+
- No external dependencies (uses only standard library)

## Quick Start

```bash
cd /path/to/dhis-2/doc

# Preview what will be generated (dry run)
python3 generate_org_units.py --preset realistic --target 500000 --dry-run

# Generate 500k org units
python3 generate_org_units.py --preset realistic --target 500000 --output orgunits_500k.json
```

## Usage

```
python3 generate_org_units.py [OPTIONS]

Required (one of):
  --preset {realistic,wide,deep}  Use a predefined tree structure
  --levels LEVELS                 Custom cumulative counts, e.g., '1,50,1000,50000'
  --branching BRANCHING           Custom branching factors, e.g., '10,20,50'

Options:
  --target TARGET        Target number of org units (default: 100000)
  --output, -o FILE      Output file path (default: org_units.json)
  --batch-size SIZE      Split output into multiple files
  --include-groups       Generate OrganisationUnitGroups and OrganisationUnitGroupSets
  --no-groups            Explicitly disable group generation (default)
  --opening-date DATE    Opening date for org units (default: 2020-01-01)
  --seed SEED            Random seed for reproducible generation
  --dry-run              Show tree structure without generating
```

## Tree Presets

### Realistic (recommended for general testing)

Mimics real-world health system hierarchies:
- Country → Region → District → Sub-district → Facility → CHW

```bash
python3 generate_org_units.py --preset realistic --target 500000 --dry-run
```

Output:
```
Tree Structure:
  Branching factors: [10, 20, 10, 50, 3]
  Number of levels: 6
  Estimated total org units: 402,211

  Level breakdown:
    Level 1: 1 (root)
    Level 2: 10 (10 children per parent)
    Level 3: 200 (20 children per parent)
    Level 4: 2,000 (10 children per parent)
    Level 5: 100,000 (50 children per parent)
    Level 6: 300,000 (3 children per parent)
```

### Wide (for breadth/sibling queries)

Few levels with many children per node. Good for testing:
- Queries that fetch many siblings
- Pagination of children
- Large flat structures

```bash
python3 generate_org_units.py --preset wide --target 500000 --dry-run
```

Output:
```
Tree Structure:
  Branching factors: [100, 50, 100]
  Number of levels: 4
  Estimated total org units: 505,101

  Level breakdown:
    Level 1: 1 (root)
    Level 2: 100 (100 children per parent)
    Level 3: 5,000 (50 children per parent)
    Level 4: 500,000 (100 children per parent)
```

### Deep (for ancestry/path queries)

Many levels with few children per node. Good for testing:
- Ancestor/descendant queries
- Path calculations
- Deep hierarchy traversal

```bash
python3 generate_org_units.py --preset deep --target 500000 --dry-run
```

Output:
```
Tree Structure:
  Branching factors: [4, 4, 4, 4, 4, 4, 4, 4, 4]
  Number of levels: 10
  Estimated total org units: 349,525

  Level breakdown:
    Level 1: 1 (root)
    Level 2: 4 (4 children per parent)
    Level 3: 16 (4 children per parent)
    ...
    Level 10: 262,144 (4 children per parent)
```

## Custom Tree Structures

### Using `--branching`

Specify children per parent at each level:

```bash
# 1 root → 20 regions → 400 districts → 8000 facilities
python3 generate_org_units.py --branching "20,20,20" --output custom.json
```

### Using `--levels`

Specify cumulative org unit counts per level:

```bash
# 1 at L1, 50 at L2, 1000 at L3, 50000 at L4
python3 generate_org_units.py --levels "1,50,1000,50000" --output custom.json
```

## Organisation Unit Groups

Use `--include-groups` to generate OrganisationUnitGroups and OrganisationUnitGroupSets along with the org units. This is useful for testing endpoints like `/api/organisationUnitGroups` which can trigger performance issues (see DHIS2-20613).

```bash
# Generate org units with groups (Sierra Leone style)
python3 generate_org_units.py --branching "8,10,15,50" --include-groups --output orgunits.json
```

### Generated Groups (18 total)

The script generates groups based on the Sierra Leone DHIS2 instance:

| Category | Groups |
|----------|--------|
| **Facility Type** | Hospital, Clinic, CHP, CHC, MCHP |
| **Facility Ownership** | Public facilities, Private Clinic, NGO, Mission |
| **Area** | Northern Area, Southern Area, Eastern Area, Western Area |
| **Location** | Urban, Rural |
| **Admin Level** | Country, District, Chiefdom |

### Generated Group Sets (4 total)

| Group Set | Compulsory | Data Dimension | Groups |
|-----------|------------|----------------|--------|
| Facility Type | Yes | Yes | Hospital, Clinic, CHP, CHC, MCHP |
| Facility Ownership | Yes | Yes | Public facilities, Private Clinic, NGO, Mission |
| Area | No | Yes | Northern, Southern, Eastern, Western Area |
| Location Rural/Urban | Yes | Yes | Urban, Rural |

### Assignment Logic

Org units are automatically assigned to groups based on their level:

- **Level 1** (Country): → "Country" group
- **Level 2** (Region): → Area groups (Northern/Southern/Eastern/Western)
- **Level 3** (District): → "District" group  
- **Level 4** (Sub-District): → "Chiefdom" group
- **Level 5+** (Facilities): → Facility type + Ownership + Location (randomly distributed)

### Batch Files with Groups

When using `--batch-size` with `--include-groups`, the groups and group sets are only included in the **first batch file** (e.g., `orgunits_0000.json`). Subsequent files contain only org units.

```bash
python3 generate_org_units.py --branching "8,10,15,50" \
  --include-groups --batch-size 10000 --output orgunits.json
```

This creates:
- `orgunits_0000.json` - Org units + Groups + Group Sets
- `orgunits_0001.json` - Org units only
- `orgunits_0002.json` - Org units only
- ...

**Important**: Import the first file first to create the groups before importing subsequent org unit files.

## Batch Output

For very large trees (500k+), split into multiple files for easier handling:

```bash
python3 generate_org_units.py --preset realistic --target 500000 \
  --batch-size 10000 --output orgunits.json
```

This creates:
- `orgunits_0000.json` (10,000 org units)
- `orgunits_0001.json` (10,000 org units)
- ...
- `orgunits_0049.json` (remaining org units)

## Importing into DHIS2

### Single File Import

```bash
curl -X POST -H 'Content-Type: application/json' \
  -u admin:district \
  -d @orgunits_500k.json \
  'http://localhost:8080/api/metadata?importStrategy=CREATE_AND_UPDATE'
```

### Batch Import (multiple files)

```bash
for f in orgunits_*.json; do
  echo "Importing $f..."
  curl -X POST -H 'Content-Type: application/json' \
    -u admin:district \
    -d @"$f" \
    'http://localhost:8080/api/metadata?importStrategy=CREATE_AND_UPDATE'
  
  # Optional: wait between batches to reduce server load
  sleep 2
done
```

### Async Import (for large files)

For very large imports, use async mode:

```bash
curl -X POST -H 'Content-Type: application/json' \
  -u admin:district \
  -d @orgunits_500k.json \
  'http://localhost:8080/api/metadata?importStrategy=CREATE_AND_UPDATE&async=true'
```

## Output Format

### Basic Output (without groups)

```json
{
  "organisationUnits": [
    {
      "id": "OgTpigTHKbf",
      "name": "National Country",
      "shortName": "National Country",
      "code": "L01-00001",
      "openingDate": "2020-01-01T00:00:00.000"
    },
    {
      "id": "oFbmOHnKYaX",
      "name": "National Northern Region",
      "shortName": "National Northern Region",
      "code": "L01-00001-00001",
      "openingDate": "2020-01-01T00:00:00.000",
      "parent": {
        "id": "OgTpigTHKbf"
      }
    }
  ]
}
```

### Output with Groups (`--include-groups`)

```json
{
  "organisationUnits": [
    {
      "id": "OgTpigTHKbf",
      "name": "National Country",
      "shortName": "National Country",
      "code": "L01-00001",
      "openingDate": "2020-01-01T00:00:00.000"
    }
  ],
  "organisationUnitGroups": [
    {
      "id": "xYz123AbCdE",
      "name": "Hospital",
      "shortName": "Hospital",
      "code": "OU_GROUP_HOSPITAL",
      "organisationUnits": [
        {"id": "abc123..."},
        {"id": "def456..."}
      ]
    },
    {
      "id": "fGh789IjKlM",
      "name": "Country",
      "shortName": "Country",
      "code": "OU_GROUP_COUNTRY",
      "organisationUnits": [
        {"id": "OgTpigTHKbf"}
      ]
    }
  ],
  "organisationUnitGroupSets": [
    {
      "id": "nOp012QrStU",
      "name": "Facility Type",
      "shortName": "Facility Type",
      "code": "OU_GROUPSET_FACILITY_TYPE",
      "compulsory": true,
      "dataDimension": true,
      "includeSubhierarchyInAnalytics": false,
      "organisationUnitGroups": [
        {"id": "xYz123AbCdE"},
        {"id": "..."}
      ]
    }
  ]
}
```

## Reproducible Generation

Use `--seed` for reproducible output:

```bash
# These will produce identical output
python3 generate_org_units.py --preset realistic --target 10000 --seed 42 --output run1.json
python3 generate_org_units.py --preset realistic --target 10000 --seed 42 --output run2.json
```

## Performance

The script uses streaming/generators to handle large trees efficiently:

| Target | Time | Rate |
|--------|------|------|
| 10,000 | ~0.1s | ~100k/s |
| 100,000 | ~1s | ~100k/s |
| 500,000 | ~5s | ~100k/s |
| 1,000,000 | ~10s | ~100k/s |

Memory usage stays constant regardless of tree size due to streaming writes.

## Examples

### Generate for Performance Testing

```bash
# Standard performance test with 500k org units
python3 generate_org_units.py \
  --preset realistic \
  --target 500000 \
  --batch-size 50000 \
  --output perf_test_orgunits.json

# Import all batches
for f in perf_test_orgunits_*.json; do
  echo "Importing $f..."
  curl -s -X POST -H 'Content-Type: application/json' \
    -u admin:district \
    -d @"$f" \
    'http://localhost:8080/api/metadata?importStrategy=CREATE_AND_UPDATE' | jq .status
done
```

### Test Specific Scenarios

```bash
# Test deep hierarchy queries (10 levels)
python3 generate_org_units.py --preset deep --target 100000 --output deep_test.json

# Test wide queries (many siblings)
python3 generate_org_units.py --preset wide --target 100000 --output wide_test.json

# Test specific branching pattern
python3 generate_org_units.py --branching "5,10,20,50,100" --output specific_test.json
```

### Generate with Organisation Unit Groups (DHIS2-20613 testing)

For testing `/api/organisationUnitGroups` performance issues:

```bash
# Generate 60k org units with groups in batch files
python3 generate_org_units.py --branching "8,10,15,50" \
  --include-groups --batch-size 10000 --seed 42 \
  --output orgunits.json

# Import files in order (first file has groups)
for f in orgunits_*.json; do
  echo "Importing $f..."
  curl -s -X POST -H 'Content-Type: application/json' \
    -u admin:district \
    -d @"$f" \
    'http://localhost:8080/api/metadata?importStrategy=CREATE_AND_UPDATE&atomicMode=NONE' | jq .status
done
```

## Troubleshooting

### Import Timeout

For large imports, increase the timeout or use async mode:

```bash
# Use async import
curl -X POST ... '...?async=true'

# Or increase client timeout
curl --max-time 600 -X POST ...
```

### Memory Issues on Import

Split into smaller batches:

```bash
python3 generate_org_units.py --preset realistic --target 500000 \
  --batch-size 5000 --output orgunits.json
```

### Duplicate UID Errors

If re-importing, use `CREATE_AND_UPDATE` strategy:

```bash
curl ... 'http://localhost:8080/api/metadata?importStrategy=CREATE_AND_UPDATE'
```

Or generate with a different seed:

```bash
python3 generate_org_units.py --preset realistic --target 500000 --seed 12345 --output new_orgunits.json
```
