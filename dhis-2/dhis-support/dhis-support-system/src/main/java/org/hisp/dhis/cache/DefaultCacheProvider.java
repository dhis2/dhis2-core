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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hisp.dhis.commons.util.SystemUtils.isTestRun;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.event.ApplicationCacheClearedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.google.api.client.util.Lists;

/**
 * The {@link DefaultCacheProvider} has the specific configuration for each of
 * the {@link Cache} factory methods as used within DHIS2.
 *
 * @author Jan Bernitt
 */
@RequiredArgsConstructor
@Component( "defaultCacheProvider" )
public class DefaultCacheProvider implements CacheProvider
{
    private static final long SIZE_1 = 1;

    private static final long SIZE_100 = 100;

    private static final long SIZE_1K = 1_000;

    private static final long SIZE_10K = 10_000;

    private static final long SIZE_20K = 20_000;

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
        programRulesCache
    }

    private final List<Cache> allCaches = Lists.newArrayList();

    private final CacheBuilderProvider cacheBuilderProvider;

    private final Environment environment;

    private long orZeroInTestRun( long value )
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
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.analyticsResponse.name() )
            .expireAfterWrite( initialExpirationTime.toMillis(), MILLISECONDS )
            .withMaximumSize( orZeroInTestRun( SIZE_20K ) )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @Override
    public <V> Cache<V> createAppCache()
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.appCache.name() )
            .build();
        allCaches.add( cache );
        return cache;
    }

    /**
     * Cache for default objects such as default category combination and
     * default category option combination which are permanent and will never
     * change.
     */
    @Override
    public <V> Cache<V> createDefaultObjectCache()
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.defaultObjectCache.name() )
            .expireAfterAccess( 12, TimeUnit.HOURS )
            .withInitialCapacity( 4 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( SIZE_100 ) )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @Override
    public <V> Cache<V> createIsDataApprovedCache()
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.isDataApproved.name() )
            .expireAfterWrite( 12, TimeUnit.HOURS )
            .withMaximumSize( orZeroInTestRun( SIZE_20K ) )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @Override
    public <V> Cache<V> createAllConstantsCache()
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.allConstantsCache.name() )
            .expireAfterWrite( 2, TimeUnit.MINUTES )
            .withInitialCapacity( 1 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( SIZE_1 ) )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @Override
    public <V> Cache<V> createInUserOrgUnitHierarchyCache()
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.inUserOuHierarchy.name() )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( SIZE_20K ) )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @Override
    public <V> Cache<V> createInUserSearchOrgUnitHierarchyCache()
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.inUserSearchOuHierarchy.name() )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( SIZE_20K ) )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @Override
    public <V> Cache<V> createUserCaptureOrgUnitThresholdCache()
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.userCaptureOuCountThreshold.name() )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( SIZE_20K ) )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @Override
    public <V> Cache<V> createPeriodIdCache()
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.periodIdCache.name() )
            .expireAfterWrite( 24, TimeUnit.HOURS )
            .withInitialCapacity( 200 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( SIZE_10K ) )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @Override
    public <V> Cache<V> createUserAccountRecoverAttemptCache( V defaultValue )
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.userAccountRecoverAttempt.name() )
            .expireAfterWrite( 15, MINUTES )
            .withDefaultValue( defaultValue )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @Override
    public <V> Cache<V> createUserFailedLoginAttemptCache( V defaultValue )
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.userFailedLoginAttempt.name() )
            .expireAfterWrite( 15, MINUTES )
            .withDefaultValue( defaultValue )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @Override
    public <V> Cache<V> createProgramOwnerCache()
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.programOwner.name() )
            .expireAfterWrite( 5, TimeUnit.MINUTES )
            .withMaximumSize( orZeroInTestRun( SIZE_1K ) )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @Override
    public <V> Cache<V> createProgramTempOwnerCache()
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.programTempOwner.name() )
            .expireAfterWrite( 30, TimeUnit.MINUTES )
            .withMaximumSize( orZeroInTestRun( SIZE_10K ) )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @Override
    public <V> Cache<V> createUserIdCache()
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.userIdCache.name() )
            .expireAfterWrite( 1, TimeUnit.HOURS )
            .withInitialCapacity( 200 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( SIZE_10K ) )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @Override
    public <V> Cache<V> createCurrentUserGroupInfoCache()
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.currentUserGroupInfoCache.name() )
            .expireAfterWrite( 1, TimeUnit.HOURS )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( SIZE_10K ) )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @Override
    public <V> Cache<V> createUserSettingCache()
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.userSetting.name() )
            .expireAfterWrite( 12, TimeUnit.HOURS )
            .withMaximumSize( orZeroInTestRun( SIZE_10K ) )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @Override
    public <V> Cache<V> createAttrOptionComboIdCache()
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.attrOptionComboIdCache.name() )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( SIZE_10K ) )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @Override
    public <V> Cache<V> createSystemSettingCache()
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.systemSetting.name() )
            .expireAfterWrite( 12, TimeUnit.HOURS )
            .withMaximumSize( orZeroInTestRun( SIZE_1K ) )
            .build();
        allCaches.add( cache );
        return cache;
    }

    /**
     * Cache for Google API access tokens. Expiration is set to 10 minutes after
     * access to match the expiration of Google service tokens.
     */
    @Override
    public <V> Cache<V> createGoogleAccessTokenCache()
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.googleAccessToken.name() )
            .expireAfterAccess( 10, MINUTES )
            .withMaximumSize( orZeroInTestRun( 1 ) )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @Override
    public <V> Cache<V> createDataItemsPaginationCache()
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.dataItemsPagination.name() )
            .expireAfterWrite( 5, MINUTES )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( SIZE_10K ) )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @Override
    public <V> Cache<V> createMetadataAttributesCache()
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.metadataAttributes.name() )
            .expireAfterWrite( 12, TimeUnit.HOURS )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( SIZE_1K ) )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @Override
    public <V> Cache<V> createCanDataWriteCocCache()
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.canDataWriteCocCache.name() )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( SIZE_10K ) )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @Override
    public <V> Cache<V> createAnalyticsSqlCache()
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.analyticsSql.name() )
            .expireAfterWrite( 10, TimeUnit.HOURS )
            .withInitialCapacity( 10000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( SIZE_10K ) )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @Override
    public <V> Cache<V> createDataElementCache()
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.dataElementCache.name() )
            .expireAfterWrite( 60, TimeUnit.MINUTES )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( SIZE_10K ) )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @Override
    public <V> Cache<V> createPropertyTransformerCache()
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.propertyTransformerCache.name() )
            .expireAfterWrite( 12, TimeUnit.HOURS )
            .withInitialCapacity( 20 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( SIZE_20K ) )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @Override
    public <V> Cache<V> createProgramRulesCache()
    {
        Cache<V> cache = this.<V> newBuilder()
            .forRegion( Region.programRulesCache.name() )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( 20 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( SIZE_1K ) )
            .build();
        allCaches.add( cache );
        return cache;
    }

    @EventListener
    public void handleApplicationCachesCleared( ApplicationCacheClearedEvent event )
    {
        allCaches.forEach( Cache::invalidateAll );
    }
}
