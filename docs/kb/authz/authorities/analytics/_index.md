---
title: Analytics authorities
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
tags: [authz, authority, analytics]
confidence: high
open_questions:
  - Several analytics-adjacent object types (`Dashboard`, `Map`, `Visualization`, `EventChart`, `EventReport`, `EventVisualization`) register only `*_PUBLIC_ADD` in their schema descriptor, not `*_PRIVATE_ADD` or `*_DELETE`. Confirm and document the rationale.
  - `F_PERFORM_ANALYTICS_EXPLAIN` — which endpoints does it gate? Expected to expose SQL explain plans.
  - `F_ANALYTICSTABLEHOOK_ADD` / `_DELETE` — who can register analytics-table hooks, and what code is executed on table generation?
---

# Analytics authorities

Authorities over analytics queries, visualisations, dashboards, maps, reports, and analytics infrastructure.

| Authority | File | Definition source |
|---|---|---|
| `F_ANALYTICSTABLEHOOK_ADD` | — | `AnalyticsTableHookSchemaDescriptor` |
| `F_ANALYTICSTABLEHOOK_DELETE` | — | `AnalyticsTableHookSchemaDescriptor` |
| `F_DASHBOARD_PUBLIC_ADD` | — | `DashboardSchemaDescriptor` |
| `F_EVENTCHART_PUBLIC_ADD` | — | `EventChartSchemaDescriptor` |
| `F_EVENTREPORT_PUBLIC_ADD` | — | `EventReportSchemaDescriptor` |
| `F_EVENT_VISUALIZATION_PUBLIC_ADD` | — | `EventVisualizationSchemaDescriptor` |
| `F_MAP_PUBLIC_ADD` | — | `MapSchemaDescriptor` |
| `F_PERFORM_ANALYTICS_EXPLAIN` | — | enum |
| `F_REPORT_DELETE` | — | `ReportSchemaDescriptor` |
| `F_REPORT_PRIVATE_ADD` | — | `ReportSchemaDescriptor` |
| `F_REPORT_PUBLIC_ADD` | — | `ReportSchemaDescriptor` |
| `F_VIEW_EVENT_ANALYTICS` | — | enum |
| `F_VISUALIZATION_PUBLIC_ADD` | — | `VisualizationSchemaDescriptor` |

## Open questions

- Several analytics-adjacent object types (`Dashboard`, `Map`, `Visualization`, `EventChart`, `EventReport`, `EventVisualization`) register only `*_PUBLIC_ADD` in their schema descriptor, not `*_PRIVATE_ADD` or `*_DELETE`. Confirm and document the rationale.
- `F_PERFORM_ANALYTICS_EXPLAIN` — which endpoints does it gate? Expected to expose SQL explain plans.
- `F_ANALYTICSTABLEHOOK_ADD` / `_DELETE` — who can register analytics-table hooks, and what code is executed on table generation?
