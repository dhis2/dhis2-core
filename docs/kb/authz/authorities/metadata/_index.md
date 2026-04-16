---
title: Metadata authorities
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
tags: [authz, authority, metadata]
confidence: high
open_questions:
  - `F_METADATA_MANAGE` — what does it unlock that `F_METADATA_IMPORT` + `F_METADATA_EXPORT` do not?
  - Merge authorities (`F_*_MERGE`) are enum-declared separately from the schema CRUD triple; how do they interact with the target type's CRUD authorities?
  - `F_ORGANISATIONUNIT_MOVE` / `F_ORGANISATION_UNIT_SPLIT` / `F_ORGANISATION_UNIT_MERGE` — note the inconsistent token naming (ORGANISATIONUNIT vs ORGANISATION_UNIT). Verify both spellings are actually checked in matching sites.
  - `F_ORG_UNIT_PROFILE_ADD` — singular authority with no matching DELETE — confirm.
---

# Metadata authorities

Authorities controlling CRUD (and merge) on configuration objects: data elements, indicators, categories, organisation units, option sets, validation rules, predictors, legends, documents, attributes, etc.

| Authority | File | Definition source |
|---|---|---|
| `F_ATTRIBUTE_DELETE` | — | `AttributeSchemaDescriptor` |
| `F_ATTRIBUTE_PRIVATE_ADD` | — | `AttributeSchemaDescriptor` |
| `F_ATTRIBUTE_PUBLIC_ADD` | — | `AttributeSchemaDescriptor` |
| `F_CATEGORY_COMBO_DELETE` | — | `CategoryComboSchemaDescriptor` |
| `F_CATEGORY_COMBO_MERGE` | — | enum |
| `F_CATEGORY_COMBO_PRIVATE_ADD` | — | `CategoryComboSchemaDescriptor` |
| `F_CATEGORY_COMBO_PUBLIC_ADD` | — | `CategoryComboSchemaDescriptor` |
| `F_CATEGORY_DELETE` | — | `CategorySchemaDescriptor` |
| `F_CATEGORY_MERGE` | — | enum |
| `F_CATEGORY_OPTION_COMBO_MERGE` | — | enum |
| `F_CATEGORY_OPTION_DELETE` | — | `CategoryOptionSchemaDescriptor` |
| `F_CATEGORY_OPTION_GROUP_DELETE` | — | `CategoryOptionGroupSchemaDescriptor` |
| `F_CATEGORY_OPTION_GROUP_PRIVATE_ADD` | — | `CategoryOptionGroupSchemaDescriptor` |
| `F_CATEGORY_OPTION_GROUP_PUBLIC_ADD` | — | `CategoryOptionGroupSchemaDescriptor` |
| `F_CATEGORY_OPTION_GROUP_SET_DELETE` | — | `CategoryOptionGroupSetSchemaDescriptor` |
| `F_CATEGORY_OPTION_GROUP_SET_PRIVATE_ADD` | — | `CategoryOptionGroupSetSchemaDescriptor` |
| `F_CATEGORY_OPTION_GROUP_SET_PUBLIC_ADD` | — | `CategoryOptionGroupSetSchemaDescriptor` |
| `F_CATEGORY_OPTION_MERGE` | — | enum |
| `F_CATEGORY_OPTION_PRIVATE_ADD` | — | `CategoryOptionSchemaDescriptor` |
| `F_CATEGORY_OPTION_PUBLIC_ADD` | — | `CategoryOptionSchemaDescriptor` |
| `F_CATEGORY_PRIVATE_ADD` | — | `CategorySchemaDescriptor` |
| `F_CATEGORY_PUBLIC_ADD` | — | `CategorySchemaDescriptor` |
| `F_CONSTANT_ADD` | — | `ConstantSchemaDescriptor` |
| `F_CONSTANT_DELETE` | — | `ConstantSchemaDescriptor` |
| `F_DATA_ELEMENT_MERGE` | — | enum |
| `F_DATAELEMENT_DELETE` | — | `DataElementSchemaDescriptor` |
| `F_DATAELEMENT_PRIVATE_ADD` | — | `DataElementSchemaDescriptor` |
| `F_DATAELEMENT_PUBLIC_ADD` | — | `DataElementSchemaDescriptor` |
| `F_DATAELEMENTGROUP_DELETE` | — | `DataElementGroupSchemaDescriptor` |
| `F_DATAELEMENTGROUP_PRIVATE_ADD` | — | `DataElementGroupSchemaDescriptor` |
| `F_DATAELEMENTGROUP_PUBLIC_ADD` | — | `DataElementGroupSchemaDescriptor` |
| `F_DATAELEMENTGROUPSET_DELETE` | — | `DataElementGroupSetSchemaDescriptor` |
| `F_DATAELEMENTGROUPSET_PRIVATE_ADD` | — | `DataElementGroupSetSchemaDescriptor` |
| `F_DATAELEMENTGROUPSET_PUBLIC_ADD` | — | `DataElementGroupSetSchemaDescriptor` |
| `F_DOCUMENT_DELETE` | — | `DocumentSchemaDescriptor` |
| `F_DOCUMENT_PRIVATE_ADD` | — | `DocumentSchemaDescriptor` |
| `F_DOCUMENT_PUBLIC_ADD` | — | `DocumentSchemaDescriptor` |
| `F_INDICATOR_DELETE` | — | `IndicatorSchemaDescriptor` |
| `F_INDICATOR_MERGE` | — | enum |
| `F_INDICATOR_PRIVATE_ADD` | — | `IndicatorSchemaDescriptor` |
| `F_INDICATOR_PUBLIC_ADD` | — | `IndicatorSchemaDescriptor` |
| `F_INDICATOR_TYPE_MERGE` | — | enum |
| `F_INDICATORGROUP_DELETE` | — | `IndicatorGroupSchemaDescriptor` |
| `F_INDICATORGROUP_PRIVATE_ADD` | — | `IndicatorGroupSchemaDescriptor` |
| `F_INDICATORGROUP_PUBLIC_ADD` | — | `IndicatorGroupSchemaDescriptor` |
| `F_INDICATORGROUPSET_DELETE` | — | `IndicatorGroupSetSchemaDescriptor` |
| `F_INDICATORGROUPSET_PRIVATE_ADD` | — | `IndicatorGroupSetSchemaDescriptor` |
| `F_INDICATORGROUPSET_PUBLIC_ADD` | — | `IndicatorGroupSetSchemaDescriptor` |
| `F_INDICATORTYPE_ADD` | — | `IndicatorTypeSchemaDescriptor` |
| `F_INDICATORTYPE_DELETE` | — | `IndicatorTypeSchemaDescriptor` |
| `F_LEGEND_SET_DELETE` | — | enum |
| `F_LEGEND_SET_PRIVATE_ADD` | — | enum |
| `F_LEGEND_SET_PUBLIC_ADD` | — | enum |
| `F_METADATA_EXPORT` | — | enum |
| `F_METADATA_IMPORT` | — | enum |
| `F_METADATA_MANAGE` | — | enum |
| `F_OPTIONGROUP_DELETE` | — | `OptionGroupSchemaDescriptor` |
| `F_OPTIONGROUP_PRIVATE_ADD` | — | `OptionGroupSchemaDescriptor` |
| `F_OPTIONGROUP_PUBLIC_ADD` | — | `OptionGroupSchemaDescriptor` |
| `F_OPTIONGROUPSET_DELETE` | — | `OptionGroupSetSchemaDescriptor` |
| `F_OPTIONGROUPSET_PRIVATE_ADD` | — | `OptionGroupSetSchemaDescriptor` |
| `F_OPTIONGROUPSET_PUBLIC_ADD` | — | `OptionGroupSetSchemaDescriptor` |
| `F_OPTIONSET_DELETE` | — | `OptionSetSchemaDescriptor` |
| `F_OPTIONSET_PRIVATE_ADD` | — | `OptionSetSchemaDescriptor` |
| `F_OPTIONSET_PUBLIC_ADD` | — | `OptionSetSchemaDescriptor` |
| `F_ORG_UNIT_PROFILE_ADD` | — | enum |
| `F_ORGANISATION_UNIT_MERGE` | — | enum |
| `F_ORGANISATION_UNIT_SPLIT` | — | enum |
| `F_ORGANISATIONUNIT_ADD` | — | `OrganisationUnitSchemaDescriptor` |
| `F_ORGANISATIONUNIT_DELETE` | — | `OrganisationUnitSchemaDescriptor` |
| `F_ORGANISATIONUNIT_MOVE` | — | enum |
| `F_ORGANISATIONUNITLEVEL_UPDATE` | — | `OrganisationUnitLevelSchemaDescriptor` |
| `F_ORGUNITGROUP_DELETE` | — | `OrganisationUnitGroupSchemaDescriptor` |
| `F_ORGUNITGROUP_PRIVATE_ADD` | — | `OrganisationUnitGroupSchemaDescriptor` |
| `F_ORGUNITGROUP_PUBLIC_ADD` | — | `OrganisationUnitGroupSchemaDescriptor` |
| `F_ORGUNITGROUPSET_DELETE` | — | `OrganisationUnitGroupSetSchemaDescriptor` |
| `F_ORGUNITGROUPSET_PRIVATE_ADD` | — | `OrganisationUnitGroupSetSchemaDescriptor` |
| `F_ORGUNITGROUPSET_PUBLIC_ADD` | — | `OrganisationUnitGroupSetSchemaDescriptor` |
| `F_PREDICTOR_ADD` | — | `PredictorSchemaDescriptor` |
| `F_PREDICTOR_DELETE` | — | `PredictorSchemaDescriptor` |
| `F_PREDICTORGROUP_ADD` | — | `PredictorGroupSchemaDescriptor` |
| `F_PREDICTORGROUP_DELETE` | — | `PredictorGroupSchemaDescriptor` |
| `F_SECTION_ADD` | — | `SectionSchemaDescriptor` |
| `F_SECTION_DELETE` | — | `SectionSchemaDescriptor` |
| `F_VALIDATIONRULE_DELETE` | — | `ValidationRuleSchemaDescriptor` |
| `F_VALIDATIONRULE_PRIVATE_ADD` | — | `ValidationRuleSchemaDescriptor` |
| `F_VALIDATIONRULE_PUBLIC_ADD` | — | `ValidationRuleSchemaDescriptor` |
| `F_VALIDATIONRULEGROUP_DELETE` | — | `ValidationRuleGroupSchemaDescriptor` |
| `F_VALIDATIONRULEGROUP_PRIVATE_ADD` | — | `ValidationRuleGroupSchemaDescriptor` |
| `F_VALIDATIONRULEGROUP_PUBLIC_ADD` | — | `ValidationRuleGroupSchemaDescriptor` |

## Open questions

- `F_METADATA_MANAGE` — what does it unlock that `F_METADATA_IMPORT` + `F_METADATA_EXPORT` do not?
- Merge authorities (`F_*_MERGE`) are enum-declared separately from the schema CRUD triple; how do they interact with the target type's CRUD authorities?
- `F_ORGANISATIONUNIT_MOVE` / `F_ORGANISATION_UNIT_SPLIT` / `F_ORGANISATION_UNIT_MERGE` — note the inconsistent token naming (ORGANISATIONUNIT vs ORGANISATION_UNIT). Verify both spellings are actually checked in matching sites.
- `F_ORG_UNIT_PROFILE_ADD` — singular authority with no matching DELETE — confirm.
