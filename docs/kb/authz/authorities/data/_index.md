---
title: Data authorities
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
see_also:
  - authz/authorities/_index.md
tags: [authz, authority, data]
confidence: high
open_questions:
  - `F_SKIP_DATA_IMPORT_AUDIT` — implications on data-value audit trail; who is expected to hold this (jobs, superusers, importers)?
  - `F_EDIT_EXPIRED` — does it bypass data-set expiry checks entirely, or only within a grace window?
  - `F_VIEW_UNAPPROVED_DATA` scope — aggregated analytics only, or also raw data-value endpoints?
  - `F_DATA_APPROVAL_LEVEL` / `F_DATA_APPROVAL_WORKFLOW` — are these schema-descriptor authorities (CRUD) or custom gates?
---

# Data authorities

Authorities over aggregate data values, data set CRUD, approval workflow, data import controls, min/max generation.

| Authority | File | Definition source |
|---|---|---|
| `F_ACCEPT_DATA_LOWER_LEVELS` | — | enum |
| `F_APPROVE_DATA` | — | enum |
| `F_APPROVE_DATA_LOWER_LEVELS` | — | enum |
| `F_DATA_APPROVAL_LEVEL` | — | `DataApprovalLevelSchemaDescriptor` |
| `F_DATA_APPROVAL_WORKFLOW` | — | `DataApprovalWorkflowSchemaDescriptor` |
| `F_DATASET_DELETE` | — | `DataSetSchemaDescriptor` |
| `F_DATASET_PRIVATE_ADD` | — | `DataSetSchemaDescriptor` |
| `F_DATASET_PUBLIC_ADD` | — | `DataSetSchemaDescriptor` |
| `F_DATAVALUE_ADD` | — | enum |
| `F_EDIT_EXPIRED` | — | enum |
| `F_EXPORT_DATA` | — | enum |
| `F_GENERATE_MIN_MAX_VALUES` | — | enum |
| `F_MINMAX_DATAELEMENT_ADD` | — | enum |
| `F_SKIP_DATA_IMPORT_AUDIT` | — | enum |
| `F_VIEW_UNAPPROVED_DATA` | — | enum |

## Open questions

- `F_SKIP_DATA_IMPORT_AUDIT` — implications on data-value audit trail; who is expected to hold this (jobs, superusers, importers)?
- `F_EDIT_EXPIRED` — does it bypass data-set expiry checks entirely, or only within a grace window?
- `F_VIEW_UNAPPROVED_DATA` scope — aggregated analytics only, or also raw data-value endpoints?
- `F_DATA_APPROVAL_LEVEL` / `F_DATA_APPROVAL_WORKFLOW` — are these schema-descriptor authorities (CRUD) or custom gates?
