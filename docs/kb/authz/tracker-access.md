---
title: Tracker access (programs, ownership, attributes)
type: concept
status: draft
last_verified:
  commit: 
  date: 
  by: 
sources: []
see_also:
  - authz/overview.md
  - authz/org-unit-scoping.md
  - tracker/_index.md
tags: [authz, tracker, program]
confidence: low
open_questions:
  - Semantics of program access levels OPEN / AUDITED / PROTECTED / CLOSED and enforcement sites.
  - `TrackedEntityProgramOwner` reassignment — who can perform it and is it audited?
  - Break-the-glass flow and its audit trail.
  - `TrackedEntityAttribute.confidential` — every read path that must suppress it.
---

# Tracker access (programs, ownership, attributes)

## Summary

## How it works

## Entry points

## Invariants

## Edge cases

## Gotchas

## Open questions

- Semantics of program access levels OPEN / AUDITED / PROTECTED / CLOSED and enforcement sites.
- `TrackedEntityProgramOwner` reassignment — who can perform it and is it audited?
- Break-the-glass flow and its audit trail.
- `TrackedEntityAttribute.confidential` — every read path that must suppress it.
