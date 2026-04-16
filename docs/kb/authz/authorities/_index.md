---
title: Authority catalogue
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
  - authz/overview.md
tags: [authz, authority, catalogue]
confidence: high
open_questions:
  - Individual per-authority entries are not yet written. The table below is a seed inventory.
  - `M_dhis-web-app-management` is defined in `Authorities.java:119` but is prefix `M_` (not `F_`) — document its semantics. Excluded from the `F_*` table below.
  - Some authorities appear in both the enum and as literal strings elsewhere; confirm the two sources always agree on spelling.
  - Sanity-check whether every schema-descriptor-declared authority is actually enforced by the schema ACL layer (it is granted automatically, but where is it *checked*?).
---

# Authority catalogue

Every distinct `F_*` authority string found anywhere in Java code under `dhis-2/`, at commit `0fcfa39f1cbe7c3e5912fdb12e55eb924b3b398a`. 192 authorities total.

## Definition sources

Authorities are declared in two places. An authority can be in one, the other, or both:

- **Enum**: `Authorities.java` enum values ([`Authorities.java:64-119`](../../../../dhis-2/dhis-api/src/main/java/org/hisp/dhis/security/Authorities.java#L64-L119)). 52 operational authorities (verbs like `F_APPROVE_DATA`, cross-cutting flags like `F_SKIP_DATA_IMPORT_AUDIT`).
- **Schema descriptors**: per-metadata-type CRUD authorities are declared in the schema descriptor for each `IdentifiableObject` subclass under [`dhis-2/dhis-services/dhis-service-schema/src/main/java/org/hisp/dhis/schema/descriptors/`](../../../../dhis-2/dhis-services/dhis-service-schema/src/main/java/org/hisp/dhis/schema/descriptors). Each descriptor typically registers `F_<TYPE>_PUBLIC_ADD`, `F_<TYPE>_PRIVATE_ADD`, `F_<TYPE>_DELETE` via `new Authority(AuthorityType.CREATE_PUBLIC, …)` etc. 140 authorities defined this way.

**Implication**: the enum is **not** the complete authority catalogue. Any inventory that only reads `Authorities.java` will miss ~73% of the authority surface.

## Group files

Per-functional-group indexes — use these as landing pages for per-authority entries. See each group's file for the precise authority list.

- [`user/`](user/_index.md) — users, user roles, user groups, impersonation
- [`metadata/`](metadata/_index.md) — data elements, indicators, categories, org units, option sets, validation rules, predictors, etc.
- [`data/`](data/_index.md) — data values, approvals, data sets, data import controls
- [`tracker/`](tracker/_index.md) — programs, tracked entities, enrollments, relationships, program rules
- [`analytics/`](analytics/_index.md) — analytics, visualisations, dashboards, maps, reports
- [`system/`](system/_index.md) — settings, scheduling, routes, SQL views, event hooks, data exchange, messaging, maintenance
- [`other/`](other/_index.md) — catch-all (empty at this enumeration)

## Complete list (alphabetical)

| Authority | Definition source | Group |
|---|---|---|
| F_ACCEPT_DATA_LOWER_LEVELS | enum | data |
| F_AGGREGATE_DATA_EXCHANGE_DELETE | schema (`AggregateDataExchangeSchemaDescriptor`) | system |
| F_AGGREGATE_DATA_EXCHANGE_PRIVATE_ADD | schema (`AggregateDataExchangeSchemaDescriptor`) | system |
| F_AGGREGATE_DATA_EXCHANGE_PUBLIC_ADD | schema (`AggregateDataExchangeSchemaDescriptor`) | system |
| F_ANALYTICSTABLEHOOK_ADD | schema (`AnalyticsTableHookSchemaDescriptor`) | analytics |
| F_ANALYTICSTABLEHOOK_DELETE | schema (`AnalyticsTableHookSchemaDescriptor`) | analytics |
| F_APPROVE_DATA | enum | data |
| F_APPROVE_DATA_LOWER_LEVELS | enum | data |
| F_ATTRIBUTE_DELETE | schema (`AttributeSchemaDescriptor`) | metadata |
| F_ATTRIBUTE_PRIVATE_ADD | schema (`AttributeSchemaDescriptor`) | metadata |
| F_ATTRIBUTE_PUBLIC_ADD | schema (`AttributeSchemaDescriptor`) | metadata |
| F_CAPTURE_DATASTORE_UPDATE | enum | system |
| F_CATEGORY_COMBO_DELETE | schema (`CategoryComboSchemaDescriptor`) | metadata |
| F_CATEGORY_COMBO_MERGE | enum | metadata |
| F_CATEGORY_COMBO_PRIVATE_ADD | schema (`CategoryComboSchemaDescriptor`) | metadata |
| F_CATEGORY_COMBO_PUBLIC_ADD | schema (`CategoryComboSchemaDescriptor`) | metadata |
| F_CATEGORY_DELETE | schema (`CategorySchemaDescriptor`) | metadata |
| F_CATEGORY_MERGE | enum | metadata |
| F_CATEGORY_OPTION_COMBO_MERGE | enum | metadata |
| F_CATEGORY_OPTION_DELETE | schema (`CategoryOptionSchemaDescriptor`) | metadata |
| F_CATEGORY_OPTION_GROUP_DELETE | schema (`CategoryOptionGroupSchemaDescriptor`) | metadata |
| F_CATEGORY_OPTION_GROUP_PRIVATE_ADD | schema (`CategoryOptionGroupSchemaDescriptor`) | metadata |
| F_CATEGORY_OPTION_GROUP_PUBLIC_ADD | schema (`CategoryOptionGroupSchemaDescriptor`) | metadata |
| F_CATEGORY_OPTION_GROUP_SET_DELETE | schema (`CategoryOptionGroupSetSchemaDescriptor`) | metadata |
| F_CATEGORY_OPTION_GROUP_SET_PRIVATE_ADD | schema (`CategoryOptionGroupSetSchemaDescriptor`) | metadata |
| F_CATEGORY_OPTION_GROUP_SET_PUBLIC_ADD | schema (`CategoryOptionGroupSetSchemaDescriptor`) | metadata |
| F_CATEGORY_OPTION_MERGE | enum | metadata |
| F_CATEGORY_OPTION_PRIVATE_ADD | schema (`CategoryOptionSchemaDescriptor`) | metadata |
| F_CATEGORY_OPTION_PUBLIC_ADD | schema (`CategoryOptionSchemaDescriptor`) | metadata |
| F_CATEGORY_PRIVATE_ADD | schema (`CategorySchemaDescriptor`) | metadata |
| F_CATEGORY_PUBLIC_ADD | schema (`CategorySchemaDescriptor`) | metadata |
| F_CONSTANT_ADD | schema (`ConstantSchemaDescriptor`) | metadata |
| F_CONSTANT_DELETE | schema (`ConstantSchemaDescriptor`) | metadata |
| F_DASHBOARD_PUBLIC_ADD | schema (`DashboardSchemaDescriptor`) | analytics |
| F_DATA_APPROVAL_LEVEL | schema (`DataApprovalLevelSchemaDescriptor`) | data |
| F_DATA_APPROVAL_WORKFLOW | schema (`DataApprovalWorkflowSchemaDescriptor`) | data |
| F_DATAELEMENT_DELETE | schema (`DataElementSchemaDescriptor`) | metadata |
| F_DATAELEMENTGROUP_DELETE | schema (`DataElementGroupSchemaDescriptor`) | metadata |
| F_DATAELEMENTGROUP_PRIVATE_ADD | schema (`DataElementGroupSchemaDescriptor`) | metadata |
| F_DATAELEMENTGROUP_PUBLIC_ADD | schema (`DataElementGroupSchemaDescriptor`) | metadata |
| F_DATAELEMENTGROUPSET_DELETE | schema (`DataElementGroupSetSchemaDescriptor`) | metadata |
| F_DATAELEMENTGROUPSET_PRIVATE_ADD | schema (`DataElementGroupSetSchemaDescriptor`) | metadata |
| F_DATAELEMENTGROUPSET_PUBLIC_ADD | schema (`DataElementGroupSetSchemaDescriptor`) | metadata |
| F_DATA_ELEMENT_MERGE | enum | metadata |
| F_DATAELEMENT_PRIVATE_ADD | schema (`DataElementSchemaDescriptor`) | metadata |
| F_DATAELEMENT_PUBLIC_ADD | schema (`DataElementSchemaDescriptor`) | metadata |
| F_DATASET_DELETE | schema (`DataSetSchemaDescriptor`) | data |
| F_DATASET_PRIVATE_ADD | schema (`DataSetSchemaDescriptor`) | data |
| F_DATASET_PUBLIC_ADD | schema (`DataSetSchemaDescriptor`) | data |
| F_DATAVALUE_ADD | enum | data |
| F_DOCUMENT_DELETE | schema (`DocumentSchemaDescriptor`) | metadata |
| F_DOCUMENT_PRIVATE_ADD | schema (`DocumentSchemaDescriptor`) | metadata |
| F_DOCUMENT_PUBLIC_ADD | schema (`DocumentSchemaDescriptor`) | metadata |
| F_EDIT_EXPIRED | enum | data |
| F_ENROLLMENT_CASCADE_DELETE | enum | tracker |
| F_EVENTCHART_PUBLIC_ADD | schema (`EventChartSchemaDescriptor`) | analytics |
| F_EVENT_HOOK_DELETE | schema (`EventHookSchemaDescriptor`) | system |
| F_EVENT_HOOK_PRIVATE_ADD | schema (`EventHookSchemaDescriptor`) | system |
| F_EVENT_HOOK_PUBLIC_ADD | schema (`EventHookSchemaDescriptor`) | system |
| F_EVENTREPORT_PUBLIC_ADD | schema (`EventReportSchemaDescriptor`) | analytics |
| F_EVENT_VISUALIZATION_PUBLIC_ADD | schema (`EventVisualizationSchemaDescriptor`) | analytics |
| F_EXPORT_DATA | enum | data |
| F_EXTERNAL_MAP_LAYER_DELETE | schema (`ExternalMapLayerSchemaDescriptor`) | system |
| F_EXTERNAL_MAP_LAYER_PRIVATE_ADD | schema (`ExternalMapLayerSchemaDescriptor`) | system |
| F_EXTERNAL_MAP_LAYER_PUBLIC_ADD | schema (`ExternalMapLayerSchemaDescriptor`) | system |
| F_GENERATE_MIN_MAX_VALUES | enum | data |
| F_IMPERSONATE_USER | enum | user |
| F_INDICATOR_DELETE | schema (`IndicatorSchemaDescriptor`) | metadata |
| F_INDICATORGROUP_DELETE | schema (`IndicatorGroupSchemaDescriptor`) | metadata |
| F_INDICATORGROUP_PRIVATE_ADD | schema (`IndicatorGroupSchemaDescriptor`) | metadata |
| F_INDICATORGROUP_PUBLIC_ADD | schema (`IndicatorGroupSchemaDescriptor`) | metadata |
| F_INDICATORGROUPSET_DELETE | schema (`IndicatorGroupSetSchemaDescriptor`) | metadata |
| F_INDICATORGROUPSET_PRIVATE_ADD | schema (`IndicatorGroupSetSchemaDescriptor`) | metadata |
| F_INDICATORGROUPSET_PUBLIC_ADD | schema (`IndicatorGroupSetSchemaDescriptor`) | metadata |
| F_INDICATOR_MERGE | enum | metadata |
| F_INDICATOR_PRIVATE_ADD | schema (`IndicatorSchemaDescriptor`) | metadata |
| F_INDICATOR_PUBLIC_ADD | schema (`IndicatorSchemaDescriptor`) | metadata |
| F_INDICATORTYPE_ADD | schema (`IndicatorTypeSchemaDescriptor`) | metadata |
| F_INDICATORTYPE_DELETE | schema (`IndicatorTypeSchemaDescriptor`) | metadata |
| F_INDICATOR_TYPE_MERGE | enum | metadata |
| F_INSERT_CUSTOM_JS_CSS | enum | system |
| F_JOB_LOG_READ | enum | system |
| F_LEGEND_SET_DELETE | enum | metadata |
| F_LEGEND_SET_PRIVATE_ADD | enum | metadata |
| F_LEGEND_SET_PUBLIC_ADD | enum | metadata |
| F_LOCALE_ADD | enum | system |
| F_LOCALE_DELETE | enum | system |
| F_MANAGE_TICKETS | schema (`InterpretationSchemaDescriptor`?) | system |
| F_MAP_PUBLIC_ADD | schema (`MapSchemaDescriptor`) | analytics |
| F_METADATA_EXPORT | enum | metadata |
| F_METADATA_IMPORT | enum | metadata |
| F_METADATA_MANAGE | enum | metadata |
| F_MINMAX_DATAELEMENT_ADD | enum | data |
| F_MOBILE_SENDSMS | enum | system |
| F_MOBILE_SETTINGS | enum | system |
| F_OPTIONGROUP_DELETE | schema (`OptionGroupSchemaDescriptor`) | metadata |
| F_OPTIONGROUP_PRIVATE_ADD | schema (`OptionGroupSchemaDescriptor`) | metadata |
| F_OPTIONGROUP_PUBLIC_ADD | schema (`OptionGroupSchemaDescriptor`) | metadata |
| F_OPTIONGROUPSET_DELETE | schema (`OptionGroupSetSchemaDescriptor`) | metadata |
| F_OPTIONGROUPSET_PRIVATE_ADD | schema (`OptionGroupSetSchemaDescriptor`) | metadata |
| F_OPTIONGROUPSET_PUBLIC_ADD | schema (`OptionGroupSetSchemaDescriptor`) | metadata |
| F_OPTIONSET_DELETE | schema (`OptionSetSchemaDescriptor`) | metadata |
| F_OPTIONSET_PRIVATE_ADD | schema (`OptionSetSchemaDescriptor`) | metadata |
| F_OPTIONSET_PUBLIC_ADD | schema (`OptionSetSchemaDescriptor`) | metadata |
| F_ORGANISATIONUNIT_ADD | schema (`OrganisationUnitSchemaDescriptor`) | metadata |
| F_ORGANISATIONUNIT_DELETE | schema (`OrganisationUnitSchemaDescriptor`) | metadata |
| F_ORGANISATIONUNITLEVEL_UPDATE | schema (`OrganisationUnitLevelSchemaDescriptor`) | metadata |
| F_ORGANISATION_UNIT_MERGE | enum | metadata |
| F_ORGANISATIONUNIT_MOVE | enum | metadata |
| F_ORGANISATION_UNIT_SPLIT | enum | metadata |
| F_ORGUNITGROUP_DELETE | schema (`OrganisationUnitGroupSchemaDescriptor`) | metadata |
| F_ORGUNITGROUP_PRIVATE_ADD | schema (`OrganisationUnitGroupSchemaDescriptor`) | metadata |
| F_ORGUNITGROUP_PUBLIC_ADD | schema (`OrganisationUnitGroupSchemaDescriptor`) | metadata |
| F_ORGUNITGROUPSET_DELETE | schema (`OrganisationUnitGroupSetSchemaDescriptor`) | metadata |
| F_ORGUNITGROUPSET_PRIVATE_ADD | schema (`OrganisationUnitGroupSetSchemaDescriptor`) | metadata |
| F_ORGUNITGROUPSET_PUBLIC_ADD | schema (`OrganisationUnitGroupSetSchemaDescriptor`) | metadata |
| F_ORG_UNIT_PROFILE_ADD | enum | metadata |
| F_PERFORM_ANALYTICS_EXPLAIN | enum | analytics |
| F_PERFORM_MAINTENANCE | enum | system |
| F_PREDICTOR_ADD | schema (`PredictorSchemaDescriptor`) | metadata |
| F_PREDICTOR_DELETE | schema (`PredictorSchemaDescriptor`) | metadata |
| F_PREDICTORGROUP_ADD | schema (`PredictorGroupSchemaDescriptor`) | metadata |
| F_PREDICTORGROUP_DELETE | schema (`PredictorGroupSchemaDescriptor`) | metadata |
| F_PREDICTOR_RUN | enum | system |
| F_PREVIOUS_IMPERSONATOR_AUTHORITY | enum | user |
| F_PROGRAM_DELETE | schema (`ProgramSchemaDescriptor`) | tracker |
| F_PROGRAM_INDICATOR_DELETE | schema (`ProgramIndicatorSchemaDescriptor`) | tracker |
| F_PROGRAM_INDICATOR_GROUP_DELETE | schema (`ProgramIndicatorGroupSchemaDescriptor`) | tracker |
| F_PROGRAM_INDICATOR_GROUP_PRIVATE_ADD | schema (`ProgramIndicatorGroupSchemaDescriptor`) | tracker |
| F_PROGRAM_INDICATOR_GROUP_PUBLIC_ADD | schema (`ProgramIndicatorGroupSchemaDescriptor`) | tracker |
| F_PROGRAM_INDICATOR_PRIVATE_ADD | schema (`ProgramIndicatorSchemaDescriptor`) | tracker |
| F_PROGRAM_INDICATOR_PUBLIC_ADD | schema (`ProgramIndicatorSchemaDescriptor`) | tracker |
| F_PROGRAM_PRIVATE_ADD | schema (`ProgramSchemaDescriptor`) | tracker |
| F_PROGRAM_PUBLIC_ADD | schema (`ProgramSchemaDescriptor`) | tracker |
| F_PROGRAM_RULE_ADD | schema (`ProgramRuleSchemaDescriptor`) | tracker |
| F_PROGRAM_RULE_DELETE | schema (`ProgramRuleSchemaDescriptor`) | tracker |
| F_PROGRAM_RULE_MANAGEMENT | schema (`ProgramRuleSchemaDescriptor`?) | tracker |
| F_PROGRAMSTAGE_ADD | schema (`ProgramStageSchemaDescriptor`) | tracker |
| F_PROGRAMSTAGE_DELETE | schema (`ProgramStageSchemaDescriptor`) | tracker |
| F_RELATIONSHIPTYPE_DELETE | schema (`RelationshipTypeSchemaDescriptor`) | tracker |
| F_RELATIONSHIPTYPE_PRIVATE_ADD | schema (`RelationshipTypeSchemaDescriptor`) | tracker |
| F_RELATIONSHIPTYPE_PUBLIC_ADD | schema (`RelationshipTypeSchemaDescriptor`) | tracker |
| F_REPLICATE_USER | enum | user |
| F_REPORT_DELETE | schema (`ReportSchemaDescriptor`) | analytics |
| F_REPORT_PRIVATE_ADD | schema (`ReportSchemaDescriptor`) | analytics |
| F_REPORT_PUBLIC_ADD | schema (`ReportSchemaDescriptor`) | analytics |
| F_ROUTE_DELETE | schema (`RouteSchemaDescriptor`) | system |
| F_ROUTE_PRIVATE_ADD | schema (`RouteSchemaDescriptor`) | system |
| F_ROUTE_PUBLIC_ADD | schema (`RouteSchemaDescriptor`) | system |
| F_RUN_VALIDATION | enum | system |
| F_SCHEDULING_ADMIN | schema (`JobConfigurationSchemaDescriptor`?) | system |
| F_SECTION_ADD | schema (`SectionSchemaDescriptor`) | metadata |
| F_SECTION_DELETE | schema (`SectionSchemaDescriptor`) | metadata |
| F_SEND_EMAIL | enum | system |
| F_SKIP_DATA_IMPORT_AUDIT | enum | data |
| F_SQLVIEW_DELETE | schema (`SqlViewSchemaDescriptor`) | system |
| F_SQLVIEW_PRIVATE_ADD | schema (`SqlViewSchemaDescriptor`) | system |
| F_SQLVIEW_PUBLIC_ADD | schema (`SqlViewSchemaDescriptor`) | system |
| F_SYSTEM_SETTING | enum | system |
| F_TEI_CASCADE_DELETE | enum | tracker |
| F_TEST | literal (test fixtures) | system |
| F_TRACKED_ENTITY_ADD | schema (`TrackedEntityTypeSchemaDescriptor`) | tracker |
| F_TRACKED_ENTITY_ATTRIBUTE_DELETE | schema (`TrackedEntityAttributeSchemaDescriptor`) | tracker |
| F_TRACKED_ENTITY_ATTRIBUTE_PRIVATE_ADD | schema (`TrackedEntityAttributeSchemaDescriptor`) | tracker |
| F_TRACKED_ENTITY_ATTRIBUTE_PUBLIC_ADD | schema (`TrackedEntityAttributeSchemaDescriptor`) | tracker |
| F_TRACKED_ENTITY_DELETE | schema (`TrackedEntityTypeSchemaDescriptor`) | tracker |
| F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS | enum | tracker |
| F_TRACKED_ENTITY_MERGE | enum | tracker |
| F_TRACKED_ENTITY_UPDATE | schema (`TrackedEntityTypeSchemaDescriptor`) | tracker |
| F_UNCOMPLETE_EVENT | enum | tracker |
| F_USER_ADD | schema (`UserSchemaDescriptor`) | user |
| F_USER_ADD_WITHIN_MANAGED_GROUP | schema (`UserSchemaDescriptor`) | user |
| F_USER_DELETE | schema (`UserSchemaDescriptor`) | user |
| F_USER_DELETE_WITHIN_MANAGED_GROUP | schema (`UserSchemaDescriptor`) | user |
| F_USERGROUP_DELETE | schema (`UserGroupSchemaDescriptor`) | user |
| F_USERGROUP_PRIVATE_ADD | schema (`UserGroupSchemaDescriptor`) | user |
| F_USERGROUP_PUBLIC_ADD | schema (`UserGroupSchemaDescriptor`) | user |
| F_USER_GROUPS_READ_ONLY_ADD_MEMBERS | enum | user |
| F_USERROLE_DELETE | schema (`UserRoleSchemaDescriptor`) | user |
| F_USERROLE_PRIVATE_ADD | schema (`UserRoleSchemaDescriptor`) | user |
| F_USERROLE_PUBLIC_ADD | schema (`UserRoleSchemaDescriptor`) | user |
| F_USER_VIEW | enum | user |
| F_VALIDATIONRULE_DELETE | schema (`ValidationRuleSchemaDescriptor`) | metadata |
| F_VALIDATIONRULEGROUP_DELETE | schema (`ValidationRuleGroupSchemaDescriptor`) | metadata |
| F_VALIDATIONRULEGROUP_PRIVATE_ADD | schema (`ValidationRuleGroupSchemaDescriptor`) | metadata |
| F_VALIDATIONRULEGROUP_PUBLIC_ADD | schema (`ValidationRuleGroupSchemaDescriptor`) | metadata |
| F_VALIDATIONRULE_PRIVATE_ADD | schema (`ValidationRuleSchemaDescriptor`) | metadata |
| F_VALIDATIONRULE_PUBLIC_ADD | schema (`ValidationRuleSchemaDescriptor`) | metadata |
| F_VIEW_EVENT_ANALYTICS | enum | analytics |
| F_VIEW_SERVER_INFO | enum | system |
| F_VIEW_UNAPPROVED_DATA | enum | data |
| F_VISUALIZATION_PUBLIC_ADD | schema (`VisualizationSchemaDescriptor`) | analytics |

**Note** — a handful of schema-descriptor cells above end with `?`. Those are high-confidence guesses from the authority-string naming convention; the specific descriptor file has not yet been opened and verified. Resolve these when individual authority entries are written.

## Invariants

- Authorities with suffix `_PUBLIC_ADD` / `_PRIVATE_ADD` / `_DELETE` (and sometimes `_ADD`) are always produced by schema descriptors.
- Authorities with "verb-style" names (`F_APPROVE_DATA`, `F_SKIP_DATA_IMPORT_AUDIT`, `F_METADATA_IMPORT`) are always in the `Authorities` enum.
- Merge authorities (`F_*_MERGE`) are in the enum.
- The enum has one bundled authority with an `M_` prefix (`M_DHIS_WEB_APP_MANAGEMENT`) — unique, not an `F_*`. Excluded from this table.

## Conventions

- **File name per authority**: lowercase, kebab-case of the token. `F_USER_ADD` → `f-user-add.md`. Place in the correct group subfolder.
- **Use [`templates/authority.md`](../../templates/authority.md)** for new entries.
- **When creating a new entry**, update the group `_index.md` table as well.

## Open questions

- Individual per-authority entries are not yet written. The table above is a seed inventory.
- `M_dhis-web-app-management` is defined in `Authorities.java:119` but with the `M_` prefix (not `F_`) — document its semantics separately. Excluded here.
- Some authorities appear in both the enum and as literal strings elsewhere; confirm the two sources always agree on spelling.
- Verify every schema-descriptor-declared authority is actually enforced by the schema ACL layer — granted, but *checked* where?
