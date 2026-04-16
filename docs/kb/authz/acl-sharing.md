---
title: Object-level sharing and AclService
type: component
status: draft
last_verified:
  commit: 
  date: 
  by: 
sources: []
see_also:
  - authz/overview.md
  - authz/authorities/_index.md
tags: [authz, acl, sharing]
confidence: low
open_questions:
  - `Sharing` storage format (jsonb column) and deserialization path.
  - Every `aclService.can*` variant — semantics and call sites.
  - Default `publicAccess` per schema type.
  - Sharing self-escalation — can a non-owner modify `Sharing` on an object they can read?
---

# Object-level sharing and AclService

## Summary

## Responsibility

## Collaborators

## Public surface

## Threading & state

## Open questions

- `Sharing` storage format (jsonb column) and deserialization path.
- Every `aclService.can*` variant — semantics and call sites.
- Default `publicAccess` per schema type.
- Sharing self-escalation — can a non-owner modify `Sharing` on an object they can read?
