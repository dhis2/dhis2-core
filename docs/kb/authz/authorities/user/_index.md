---
title: User / role / group authorities
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
  - path: dhis-2/dhis-services/dhis-service-schema/src/main/java/org/hisp/dhis/schema/descriptors/UserSchemaDescriptor.java
    lines: ""
    role: primary
  - path: dhis-2/dhis-services/dhis-service-schema/src/main/java/org/hisp/dhis/schema/descriptors/UserGroupSchemaDescriptor.java
    lines: ""
    role: primary
  - path: dhis-2/dhis-services/dhis-service-schema/src/main/java/org/hisp/dhis/schema/descriptors/UserRoleSchemaDescriptor.java
    lines: ""
    role: primary
see_also:
  - authz/authorities/_index.md
  - authn/impersonation.md
tags: [authz, authority, user]
confidence: high
open_questions:
  - `F_USER_ADD_WITHIN_MANAGED_GROUP` vs `F_USER_ADD` — precedence and interaction with managed-group membership.
  - `F_PREVIOUS_IMPERSONATOR_AUTHORITY` — where is this checked, and is it revoked on impersonation termination?
  - `F_USER_GROUPS_READ_ONLY_ADD_MEMBERS` — intended narrow exception to read-only sharing; confirm enforcement sites.
  - Escalation: does `F_USERROLE_PRIVATE_ADD` + sharing-edit on an existing role let a non-admin attach `ALL` to their own role?
---

# User / role / group authorities

Authorities controlling users, user groups, user roles, and impersonation.

| Authority | File | Definition source |
|---|---|---|
| `F_IMPERSONATE_USER` | — | enum |
| `F_PREVIOUS_IMPERSONATOR_AUTHORITY` | — | enum |
| `F_REPLICATE_USER` | — | enum |
| `F_USER_ADD` | — | `UserSchemaDescriptor` |
| `F_USER_ADD_WITHIN_MANAGED_GROUP` | — | `UserSchemaDescriptor` |
| `F_USER_DELETE` | — | `UserSchemaDescriptor` |
| `F_USER_DELETE_WITHIN_MANAGED_GROUP` | — | `UserSchemaDescriptor` |
| `F_USERGROUP_DELETE` | — | `UserGroupSchemaDescriptor` |
| `F_USERGROUP_PRIVATE_ADD` | — | `UserGroupSchemaDescriptor` |
| `F_USERGROUP_PUBLIC_ADD` | — | `UserGroupSchemaDescriptor` |
| `F_USER_GROUPS_READ_ONLY_ADD_MEMBERS` | — | enum |
| `F_USERROLE_DELETE` | — | `UserRoleSchemaDescriptor` |
| `F_USERROLE_PRIVATE_ADD` | — | `UserRoleSchemaDescriptor` |
| `F_USERROLE_PUBLIC_ADD` | — | `UserRoleSchemaDescriptor` |
| `F_USER_VIEW` | — | enum |

**Per-authority entries** (`f-*.md` files in this folder): none yet. Use [`templates/authority.md`](../../../templates/authority.md).

## Open questions

- `F_USER_ADD_WITHIN_MANAGED_GROUP` vs `F_USER_ADD` — precedence and interaction with managed-group membership.
- `F_PREVIOUS_IMPERSONATOR_AUTHORITY` — where is this checked, and is it revoked on impersonation termination?
- `F_USER_GROUPS_READ_ONLY_ADD_MEMBERS` — intended narrow exception to read-only sharing; confirm enforcement sites.
- Escalation: does `F_USERROLE_PRIVATE_ADD` + sharing-edit on an existing role let a non-admin attach `ALL` to their own role?
