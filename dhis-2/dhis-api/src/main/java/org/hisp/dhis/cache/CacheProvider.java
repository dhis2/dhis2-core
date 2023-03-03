/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.cache;

import java.time.Duration;

import org.hisp.dhis.common.event.ApplicationCacheClearedEvent;

/**
 * The {@link CacheProvider} has a factory method for each {@link Cache} use
 * case in DHIS2.
 *
 * The {@link Cache} value type is kept generic to not bind this interface to
 * numerous domain types.
 *
 * Names of the factory methods follow the pattern: {@code create{region}Cache}.
 *
 * @author Jan Bernitt
 */
public interface CacheProvider
{
    <V> Cache<V> createAnalyticsResponseCache( Duration initialExpirationTime );

    <V> Cache<V> createAnalyticsCache();

    <V> Cache<V> createDefaultObjectCache();

    <V> Cache<V> createIsDataApprovedCache();

    <V> Cache<V> createAllConstantsCache();

    <V> Cache<V> createInUserOrgUnitHierarchyCache();

    <V> Cache<V> createInUserViewOrgUnitHierarchyCache();

    <V> Cache<V> createInUserSearchOrgUnitHierarchyCache();

    <V> Cache<V> createUserCaptureOrgUnitThresholdCache();

    <V> Cache<V> createPeriodIdCache();

    <V> Cache<V> createUserFailedLoginAttemptCache( V defaultValue );

    <V> Cache<V> createUserAccountRecoverAttemptCache( V defaultValue );

    <V> Cache<V> createProgramOwnerCache();

    <V> Cache<V> createProgramTempOwnerCache();

    <V> Cache<V> createUserIdCache();

    <V> Cache<V> createCurrentUserGroupInfoCache();

    <V> Cache<V> createUserSettingCache();

    <V> Cache<V> createAttrOptionComboIdCache();

    <V> Cache<V> createSystemSettingCache();

    <V> Cache<V> createGoogleAccessTokenCache();

    <V> Cache<V> createDataItemsPaginationCache();

    <V> Cache<V> createMetadataAttributesCache();

    <V> Cache<V> createCanDataWriteCocCache();

    <V> Cache<V> createAnalyticsSqlCache();

    <V> Cache<V> createDataElementCache();

    <V> Cache<V> createPropertyTransformerCache();

    <V> Cache<V> createProgramHasRulesCache();

    <V> Cache<V> createProgramRuleVariablesCache();

    <V> Cache<V> createUserGroupNameCache();

    <V> Cache<V> createUserDisplayNameCache();

    void handleApplicationCachesCleared( ApplicationCacheClearedEvent event );

    <V> Cache<V> createProgramWebHookNotificationTemplateCache();

    <V> Cache<V> createProgramStageWebHookNotificationTemplateCache();

    <V> Cache<V> createProgramOrgUnitAssociationCache();

    <V> Cache<V> createCatOptOrgUnitAssociationCache();

    <V> Cache<V> createDataSetOrgUnitAssociationCache();

    <V> Cache<V> createApiKeyCache();

    <V> Cache<V> createProgramCache();

    <V> Cache<V> createTeiAttributesCache();

    <V> Cache<V> createProgramTeiAttributesCache();

    <V> Cache<V> createUserGroupUIDCache();

    <V> Cache<V> createSecurityCache();

    <V> Cache<V> createRunningJobsInfoCache();

    <V> Cache<V> createCompletedJobsInfoCache();

    <V> Cache<V> createJobCancelRequestedCache();

    <V> Cache<V> createDataIntegritySummaryCache();

    <V> Cache<V> createDataIntegrityDetailsCache();

    <V> Cache<V> createSubExpressionCache();

    <V> Cache<V> createQueryAliasCache();
}
