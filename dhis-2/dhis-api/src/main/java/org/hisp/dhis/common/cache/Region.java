package org.hisp.dhis.common.cache;

/**
 * Enum is used to make sure we do not use same region twice. Each method should have its own
 * constant.
 */
@SuppressWarnings("squid:S115") // allow non enum-ish names
public enum Region {
    analyticsResponse,
    defaultObjectCache,
    isDataApproved,
    allConstantsCache,
    inUserOrgUnitHierarchy,
    inUserSearchOrgUnitHierarchy,
    periodIdCache,
    userAccountRecoverAttempt,
    userFailedLoginAttempt,
    twoFaDisableFailedAttempt,
    programOwner,
    programTempOwner,
    currentUserGroupInfoCache,
    attrOptionComboIdCache,
    googleAccessToken,
    dataItemsPagination,
    metadataAttributes,
    canDataWriteCocCache,
    analyticsSql,
    propertyTransformerCache,
    programHasRulesCache,
    userGroupNameCache,
    userDisplayNameCache,
    pgmOrgUnitAssocCache,
    catOptOrgUnitAssocCache,
    dataSetOrgUnitAssocCache,
    apiTokensCache,
    teAttributesCache,
    programTeAttributesCache,
    userGroupUIDCache,
    securityCache,
    dataIntegritySummaryCache,
    dataIntegrityDetailsCache,
    queryAliasCache
}
