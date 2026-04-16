---
title: System authorities
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
tags: [authz, authority, system]
confidence: high
open_questions:
  - `F_SCHEDULING_ADMIN` — precise scope; which job types are reachable?
  - `F_SQLVIEW_*` — intersection with SQL injection posture; can a non-admin with `F_SQLVIEW_PUBLIC_ADD` craft a view that exfiltrates data they cannot directly read?
  - `F_ROUTE_*` — Route is the outbound HTTP proxy feature; very SSRF-sensitive. Check IP allow-list and host-block enforcement.
  - `F_INSERT_CUSTOM_JS_CSS` — who holds it by default? It effectively grants stored XSS.
  - `F_EVENT_HOOK_*` — event hooks are outbound webhooks; SSRF-sensitive, same concern as Route.
  - `F_TEST` — looks like a test fixture. Confirm it never ships in seeded roles.
  - `F_MANAGE_TICKETS` — source schema not identified; locate.
  - `F_AGGREGATE_DATA_EXCHANGE_*` — data exchange pulls/pushes aggregate data to external DHIS2 instances; check credential handling and URL allow-listing.
---

# System authorities

Authorities over system configuration, scheduling, routes, SQL views, event hooks, data exchange, messaging, server info, and maintenance.

| Authority | File | Definition source |
|---|---|---|
| `F_AGGREGATE_DATA_EXCHANGE_DELETE` | — | `AggregateDataExchangeSchemaDescriptor` |
| `F_AGGREGATE_DATA_EXCHANGE_PRIVATE_ADD` | — | `AggregateDataExchangeSchemaDescriptor` |
| `F_AGGREGATE_DATA_EXCHANGE_PUBLIC_ADD` | — | `AggregateDataExchangeSchemaDescriptor` |
| `F_CAPTURE_DATASTORE_UPDATE` | — | enum |
| `F_EVENT_HOOK_DELETE` | — | `EventHookSchemaDescriptor` |
| `F_EVENT_HOOK_PRIVATE_ADD` | — | `EventHookSchemaDescriptor` |
| `F_EVENT_HOOK_PUBLIC_ADD` | — | `EventHookSchemaDescriptor` |
| `F_EXTERNAL_MAP_LAYER_DELETE` | — | `ExternalMapLayerSchemaDescriptor` |
| `F_EXTERNAL_MAP_LAYER_PRIVATE_ADD` | — | `ExternalMapLayerSchemaDescriptor` |
| `F_EXTERNAL_MAP_LAYER_PUBLIC_ADD` | — | `ExternalMapLayerSchemaDescriptor` |
| `F_INSERT_CUSTOM_JS_CSS` | — | enum |
| `F_JOB_LOG_READ` | — | enum |
| `F_LOCALE_ADD` | — | enum |
| `F_LOCALE_DELETE` | — | enum |
| `F_MANAGE_TICKETS` | — | literal (source unverified) |
| `F_MOBILE_SENDSMS` | — | enum |
| `F_MOBILE_SETTINGS` | — | enum |
| `F_PERFORM_MAINTENANCE` | — | enum |
| `F_PREDICTOR_RUN` | — | enum |
| `F_ROUTE_DELETE` | — | `RouteSchemaDescriptor` |
| `F_ROUTE_PRIVATE_ADD` | — | `RouteSchemaDescriptor` |
| `F_ROUTE_PUBLIC_ADD` | — | `RouteSchemaDescriptor` |
| `F_RUN_VALIDATION` | — | enum |
| `F_SCHEDULING_ADMIN` | — | literal (source unverified) |
| `F_SEND_EMAIL` | — | enum |
| `F_SQLVIEW_DELETE` | — | `SqlViewSchemaDescriptor` |
| `F_SQLVIEW_PRIVATE_ADD` | — | `SqlViewSchemaDescriptor` |
| `F_SQLVIEW_PUBLIC_ADD` | — | `SqlViewSchemaDescriptor` |
| `F_SYSTEM_SETTING` | — | enum |
| `F_TEST` | — | literal (test fixtures) |
| `F_VIEW_SERVER_INFO` | — | enum |

## Open questions

- `F_SCHEDULING_ADMIN` — precise scope; which job types are reachable?
- `F_SQLVIEW_*` — intersection with SQL injection posture; can a non-admin with `F_SQLVIEW_PUBLIC_ADD` craft a view that exfiltrates data they cannot directly read?
- `F_ROUTE_*` — Route is the outbound HTTP proxy feature; very SSRF-sensitive. Check IP allow-list and host-block enforcement.
- `F_INSERT_CUSTOM_JS_CSS` — who holds it by default? It effectively grants stored XSS.
- `F_EVENT_HOOK_*` — event hooks are outbound webhooks; SSRF-sensitive, same concern as Route.
- `F_TEST` — looks like a test fixture. Confirm it never ships in seeded roles.
- `F_MANAGE_TICKETS` — source schema not identified; locate.
- `F_AGGREGATE_DATA_EXCHANGE_*` — data exchange pulls/pushes aggregate data to external DHIS2 instances; check credential handling and URL allow-listing.
