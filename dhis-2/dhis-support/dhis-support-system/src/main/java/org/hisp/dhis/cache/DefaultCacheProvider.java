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

import lombok.AllArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hisp.dhis.commons.util.SystemUtils.isTestRun;

/**
 * The {@link DefaultCacheProvider} has the specific configuration for each of
 * the {@link Cache} factory methods as used within DHIS2.
 *
 * @author Jan Bernitt
 */
@AllArgsConstructor
@Component( "defaultCacheProvider" )
public class DefaultCacheProvider implements CacheProvider
{

    /**
     * Enum is used to make sure we do not use same region twice. Each method
     * should have its own constant.
     */
    @SuppressWarnings( "squid:S115" ) // allow non enum-ish names
    private enum Region
    {
        analyticsResponse,
        appCache,
        defaultObjectCache,
        isDataApproved,
        allConstantsCache,
        inUserOuHierarchy,
        inUserSearchOuHierarchy,
        userCaptureOuCountThreshold,
        periodIdCache,
        userAccountRecoverAttempt,
        userFailedLoginAttempt,
        programOwner,
        programTempOwner,
        userIdCache,
        currentUserGroupInfoCache,
        userSetting,
        attrOptionComboIdCache,
        systemSetting,
        googleAccessToken,
        dataItemsPagination,
        metadataAttributes,
        canDataWriteCocCache,
        analyticsSql,
        dataElementCache,
        propertyTransformerCache,
        programRulesCache,
        userGroupNameCache,
        userDisplayNameCache
    }

    private final CacheBuilderProvider cacheBuilderProvider;

    private final Environment environment;

    private int orZeroInTestRun( int value )
    {
        return isTestRun( environment.getActiveProfiles() ) ? 0 : value;
    }

    private <V> CacheBuilder<V> newBuilder()
    {
        return cacheBuilderProvider.newCacheBuilder();
    }

    @Override
    public <V> Cache<V> createAnalyticsResponseCache( Duration initialExpirationTime )
    {
        return this.<V> newBuilder()
            .forRegion( Region.analyticsResponse.name() )
            .expireAfterWrite( initialExpirationTime.toMillis(), MILLISECONDS )
            .withMaximumSize( orZeroInTestRun( 20000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createAppCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.appCache.name() )
            .build();
    }

    @Override
    public <V> Cache<V> createDefaultObjectCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.defaultObjectCache.name() )
            .expireAfterAccess( 2, TimeUnit.HOURS )
            .withInitialCapacity( 4 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 10 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createIsDataApprovedCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.isDataApproved.name() )
            .expireAfterAccess( 12, TimeUnit.HOURS )
            .withMaximumSize( orZeroInTestRun( 20000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createAllConstantsCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.allConstantsCache.name() )
            .expireAfterAccess( 2, TimeUnit.MINUTES )
            .withInitialCapacity( 1 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 1 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createInUserOrgUnitHierarchyCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.inUserOuHierarchy.name() )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 20000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createInUserSearchOrgUnitHierarchyCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.inUserSearchOuHierarchy.name() )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 20000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createUserCaptureOrgUnitThresholdCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.userCaptureOuCountThreshold.name() )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 20000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createPeriodIdCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.periodIdCache.name() )
            .expireAfterWrite( 24, TimeUnit.HOURS )
            .withInitialCapacity( 200 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 10000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createUserAccountRecoverAttemptCache( V defaultValue )
    {
        return this.<V> newBuilder()
            .forRegion( Region.userAccountRecoverAttempt.name() )
            .expireAfterWrite( 15, TimeUnit.MINUTES )
            .withDefaultValue( defaultValue )
            .build();
    }

    @Override
    public <V> Cache<V> createUserFailedLoginAttemptCache( V defaultValue )
    {
        return this.<V> newBuilder()
            .forRegion( Region.userFailedLoginAttempt.name() )
            .expireAfterWrite( 15, TimeUnit.MINUTES )
            .withDefaultValue( defaultValue )
            .build();
    }

    @Override
    public <V> Cache<V> createProgramOwnerCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.programOwner.name() )
            .expireAfterWrite( 5, TimeUnit.MINUTES )
            .withMaximumSize( orZeroInTestRun( 1000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createProgramTempOwnerCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.programTempOwner.name() )
            .expireAfterWrite( 30, TimeUnit.MINUTES )
            .withMaximumSize( orZeroInTestRun( 4000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createUserIdCacheCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.userIdCache.name() )
            .expireAfterAccess( 1, TimeUnit.HOURS )
            .withInitialCapacity( 200 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 4000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createCurrentUserGroupInfoCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.currentUserGroupInfoCache.name() )
            .expireAfterWrite( 1, TimeUnit.HOURS )
            .forceInMemory()
            .build();
    }

    @Override
    public <V> Cache<V> createUserSettingCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.userSetting.name() )
            .expireAfterWrite( 12, TimeUnit.HOURS )
            .withMaximumSize( orZeroInTestRun( 10000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createAttrOptionComboIdCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.attrOptionComboIdCache.name() )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 10000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createSystemSettingCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.systemSetting.name() )
            .expireAfterWrite( 12, TimeUnit.HOURS )
            .withMaximumSize( orZeroInTestRun( 400 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createGoogleAccessTokenCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.googleAccessToken.name() )
            .expireAfterAccess( 10, TimeUnit.MINUTES )
            .withMaximumSize( orZeroInTestRun( 1 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createDataItemsPaginationCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.dataItemsPagination.name() )
            .expireAfterWrite( 5, MINUTES )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 20000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createMetadataAttributesCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.metadataAttributes.name() )
            .expireAfterWrite( 12, TimeUnit.HOURS )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 10000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createCanDataWriteCocCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.canDataWriteCocCache.name() )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 10000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createAnalyticsSqlCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.analyticsSql.name() )
            .expireAfterAccess( 10, TimeUnit.HOURS )
            .withInitialCapacity( 10000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 50000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createDataElementCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.dataElementCache.name() )
            .expireAfterAccess( 60, TimeUnit.MINUTES )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 50000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createPropertyTransformerCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.propertyTransformerCache.name() )
            .expireAfterAccess( 12, TimeUnit.HOURS )
            .withInitialCapacity( 20 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 30000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createProgramRulesCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.programRulesCache.name() )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( 20 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 1000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createUserGroupNameCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.userGroupNameCache.name() )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( 20 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 1000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createUserDisplayNameCache()
    {
        return this.<V> newBuilder()
            .forRegion( Region.userDisplayNameCache.name() )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( 20 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 500000 ) )
            .build();
    }
}
