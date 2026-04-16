---
title: Authorization overview
type: concept
status: draft
last_verified:
  commit: 
  date: 
  by: 
sources: []
see_also:
  - authz/acl-sharing.md
  - authz/org-unit-scoping.md
  - authz/tracker-access.md
  - authz/authorities/_index.md
tags: [authz]
confidence: low
open_questions:
  - Canonical three-layer mental model (authorities × sharing × OU scope) with one cited example per layer.
  - Superuser semantics and every short-circuit site.
---

# Authorization overview

## Summary

Three stacked layers decide whether a request succeeds: (1) authority-based RBAC, (2) object-level sharing/ACL, (3) org-unit + program scoping. Tracker adds a fourth layer via program access level and ownership.

## How it works

## Entry points

## Invariants

## Edge cases

## Gotchas

## Open questions

- Canonical three-layer mental model (authorities × sharing × OU scope) with one cited example per layer.
- Superuser semantics and every short-circuit site.
