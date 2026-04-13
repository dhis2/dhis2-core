#!/bin/bash
# Strips optional fields from tracker import JSON to reduce file size and upload time.
#
# Removes server-generated fields: createdAt, updatedAt, deleted
# Removes display metadata: code, displayName, valueType
# Removes empty collections: relationships, notes, events (when empty)
# Removes default false flags: inactive, potentialDuplicate
#
# Required fields kept:
#   TrackedEntity: trackedEntityType, orgUnit, attributes, enrollments
#   Enrollment: program, orgUnit, enrolledAt, occurredAt, status, attributes, events
#   Event: programStage, orgUnit, occurredAt, status, dataValues
#   Attribute/DataValue: attribute/dataElement, value
#
# Optional fields kept (if present):
#   createdAtClient - client-provided timestamp (not server-generated)
#   followUp - only if true
#
# Usage: ./tracker-import-cleanup.sh input.json > output.json

set -e

if [ -z "$1" ]; then
    echo "Usage: $0 <input.json>" >&2
    exit 1
fi

if [ ! -f "$1" ]; then
    echo "Error: File '$1' not found" >&2
    exit 1
fi

jq --compact-output '{
  trackedEntities: [.trackedEntities[] | {
    trackedEntityType,
    orgUnit,
    attributes: [.attributes[] | {attribute, value}],
    enrollments: [.enrollments[] | {
      program,
      orgUnit,
      enrolledAt,
      occurredAt,
      status
    }
    + (if .followUp == true then {followUp: true} else {} end)
    + (if .attributes and (.attributes | length) > 0 then {attributes: [.attributes[] | {attribute, value}]} else {} end)
    + (if .events and (.events | length) > 0 then {events: [.events[] | {
        programStage,
        orgUnit,
        occurredAt,
        status
      }
      + (if .followUp == true then {followUp: true} else {} end)
      + (if .dataValues and (.dataValues | length) > 0 then {dataValues: [.dataValues[] | {dataElement, value}]} else {} end)
    ]} else {} end)
    ]
  }
  + (if .createdAtClient then {createdAtClient: .createdAtClient} else {} end)
  ]
}' "$1"
