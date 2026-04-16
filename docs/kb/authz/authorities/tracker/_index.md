---
title: Tracker authorities
type: concept
status: verified
last_verified:
  commit: 0fcfa39f1cbe7c3e5912fdb12e55eb924b3b398a
  date: 2026-04-16
  by: claude-sonnet-4-6
sources:
  - path: dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/Authorities.java
    lines: 64-119
    role: primary
  - path: dhis-2/dhis-services/dhis-service-schema/src/main/java/org/hisp/dhis/schema/descriptors
    lines: ""
    role: primary
see_also:
  - authz/authorities/_index.md
  - authz/tracker-access.md
tags: [authz, authority, tracker]
confidence: high
open_questions:
  - `F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS` vs the user's `teiSearchOrganisationUnits` set — does the authority fully bypass OU scope?
  - `F_TEI_CASCADE_DELETE` vs `F_ENROLLMENT_CASCADE_DELETE` — semantics and relationship to `F_TRACKED_ENTITY_DELETE`.
  - `F_PROGRAM_RULE_MANAGEMENT` semantics and relationship to `F_PROGRAM_RULE_ADD` / `F_PROGRAM_RULE_DELETE`.
  - `F_UNCOMPLETE_EVENT` — any audit requirement on use?
---

# Tracker authorities

Authorities controlling tracker objects: programs, program stages, program indicators, program rules, tracked entities, tracked entity attributes, enrollments, relationships, events.

| Authority | File | Definition source |
|---|---|---|
| `F_ENROLLMENT_CASCADE_DELETE` | — | enum |
| `F_PROGRAM_DELETE` | — | `ProgramSchemaDescriptor` |
| `F_PROGRAM_INDICATOR_DELETE` | — | `ProgramIndicatorSchemaDescriptor` |
| `F_PROGRAM_INDICATOR_GROUP_DELETE` | — | `ProgramIndicatorGroupSchemaDescriptor` |
| `F_PROGRAM_INDICATOR_GROUP_PRIVATE_ADD` | — | `ProgramIndicatorGroupSchemaDescriptor` |
| `F_PROGRAM_INDICATOR_GROUP_PUBLIC_ADD` | — | `ProgramIndicatorGroupSchemaDescriptor` |
| `F_PROGRAM_INDICATOR_PRIVATE_ADD` | — | `ProgramIndicatorSchemaDescriptor` |
| `F_PROGRAM_INDICATOR_PUBLIC_ADD` | — | `ProgramIndicatorSchemaDescriptor` |
| `F_PROGRAM_PRIVATE_ADD` | — | `ProgramSchemaDescriptor` |
| `F_PROGRAM_PUBLIC_ADD` | — | `ProgramSchemaDescriptor` |
| `F_PROGRAM_RULE_ADD` | — | `ProgramRuleSchemaDescriptor` |
| `F_PROGRAM_RULE_DELETE` | — | `ProgramRuleSchemaDescriptor` |
| `F_PROGRAM_RULE_MANAGEMENT` | — | unverified (likely `ProgramRuleSchemaDescriptor`) |
| `F_PROGRAMSTAGE_ADD` | — | `ProgramStageSchemaDescriptor` |
| `F_PROGRAMSTAGE_DELETE` | — | `ProgramStageSchemaDescriptor` |
| `F_RELATIONSHIPTYPE_DELETE` | — | `RelationshipTypeSchemaDescriptor` |
| `F_RELATIONSHIPTYPE_PRIVATE_ADD` | — | `RelationshipTypeSchemaDescriptor` |
| `F_RELATIONSHIPTYPE_PUBLIC_ADD` | — | `RelationshipTypeSchemaDescriptor` |
| `F_TEI_CASCADE_DELETE` | — | enum |
| `F_TRACKED_ENTITY_ADD` | — | `TrackedEntityTypeSchemaDescriptor` |
| `F_TRACKED_ENTITY_ATTRIBUTE_DELETE` | — | `TrackedEntityAttributeSchemaDescriptor` |
| `F_TRACKED_ENTITY_ATTRIBUTE_PRIVATE_ADD` | — | `TrackedEntityAttributeSchemaDescriptor` |
| `F_TRACKED_ENTITY_ATTRIBUTE_PUBLIC_ADD` | — | `TrackedEntityAttributeSchemaDescriptor` |
| `F_TRACKED_ENTITY_DELETE` | — | `TrackedEntityTypeSchemaDescriptor` |
| `F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS` | — | enum |
| `F_TRACKED_ENTITY_MERGE` | — | enum |
| `F_TRACKED_ENTITY_UPDATE` | — | `TrackedEntityTypeSchemaDescriptor` |
| `F_UNCOMPLETE_EVENT` | — | enum |

## Open questions

- `F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS` vs the user's `teiSearchOrganisationUnits` set — does the authority fully bypass OU scope?
- `F_TEI_CASCADE_DELETE` vs `F_ENROLLMENT_CASCADE_DELETE` — semantics and relationship to `F_TRACKED_ENTITY_DELETE`.
- `F_PROGRAM_RULE_MANAGEMENT` semantics and relationship to `F_PROGRAM_RULE_ADD` / `F_PROGRAM_RULE_DELETE`.
- `F_UNCOMPLETE_EVENT` — any audit requirement on use?
