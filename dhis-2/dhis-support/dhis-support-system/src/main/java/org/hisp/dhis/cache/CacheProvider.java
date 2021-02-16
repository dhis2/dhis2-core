/*
 * Copyright (c) 2004-2021, University of Oslo
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

    <V> Cache<V> createAnalyticsResponseCache( Class<V> valueType, Duration initialExpirationTime );

    <V> Cache<V> createAppCache( Class<V> valueType );

    <V> Cache<V> createDefaultObjectCache( Class<V> valueType );

    <V> Cache<V> createIsDataApprovedCache( Class<V> valueType );

    <V> Cache<V> createAllConstantsCache( Class<V> valueType );

    <V> Cache<V> createInUserOrgUnitHierarchyCache( Class<V> valueType );

    <V> Cache<V> createInUserSearchOrgUnitHierarchyCache( Class<V> valueType );

    <V> Cache<V> createUserCaptureOrgUnitThresholdCache( Class<V> valueType );

    <V> Cache<V> createPeriodIdCache( Class<V> valueType );

    <V> Cache<V> createUserFailedLoginAttemptCache( Class<V> valueType, V defaultValue );

    <V> Cache<V> createUserAccountRecoverAttemptCache( Class<V> valueType, V defaultValue );

    <V> Cache<V> createProgramOwnerCache( Class<V> valueType );

    <V> Cache<V> createProgramTempOwnerCache( Class<V> valueType );

    <V> Cache<V> createUserIdCacheCache( Class<V> valueType );

    <V> Cache<V> createCurrentUserGroupInfoCache( Class<V> valueType );

    <V> Cache<V> createUserSettingCache( Class<V> valueType );

    <V> Cache<V> createAttrOptionComboIdCache( Class<V> valueType );

    <V> Cache<V> createSystemSettingCache( Class<V> valueType );

    <V> Cache<V> createGoogleAccessTokenCache( Class<V> valueType );

    <V> Cache<V> createDataItemsPaginationCache( Class<V> valueType );

    <V> Cache<V> createMetadataAttributesCache( Class<V> valueType );

    <V> Cache<V> createCanDataWriteCocCache( Class<V> valueType );

    <V> Cache<V> createAnalyticsSqlCache( Class<V> valueType );

    <V> Cache<V> createDataElementCache( Class<V> valueType );

    <V> Cache<V> createPropertyTransformerCache( Class<V> valueType );

}
