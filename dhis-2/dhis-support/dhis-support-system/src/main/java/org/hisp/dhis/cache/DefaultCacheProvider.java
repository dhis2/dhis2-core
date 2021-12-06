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

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hisp.dhis.commons.util.SystemUtils.isTestRun;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.hisp.dhis.cache.loader.CacheLoaderProvider;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.event.ApplicationCacheClearedEvent;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * The {@link DefaultCacheProvider} has the specific configuration for each of
 * the {@link Cache<>} factory methods as used within DHIS2.
 *
 * @author Jan Bernitt
 */
@Component( "defaultCacheProvider" )
public class DefaultCacheProvider
    implements CacheProvider
{
    private static final long SIZE_1 = 1;

    private static final long SIZE_100 = 100;

    private static final long SIZE_500 = 500;

    private static final long SIZE_1K = 1_000;

    private static final long SIZE_10K = 10_000;

    private final double cacheFactor;

    private final CacheBuilderProvider cacheBuilderProvider;

    private final Environment environment;

    public DefaultCacheProvider( CacheBuilderProvider cacheBuilderProvider, Environment environment,
        DhisConfigurationProvider dhisConfig )
    {
        this.cacheBuilderProvider = cacheBuilderProvider;
        this.environment = environment;
        this.cacheFactor = Double
            .parseDouble( dhisConfig.getProperty( ConfigurationKey.SYSTEM_CACHE_MAX_SIZE_FACTOR ) );
    }

    /**
     * Enum is used to make sure we do not use same region twice. Each method
     * should have its own constant.
     */
    @SuppressWarnings( "squid:S115" ) // allow non enum-ish names
    private enum Region
    {
        metadataCache,
        analyticsResponse,
        defaultObjectCache,
        isDataApproved,
        allConstantsCache,
        inUserOuHierarchy,
        isUserViewOuHierHierarchy,
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
        programHasRulesCache,
        programRuleVariablesCache,
        userGroupNameCache,
        userDisplayNameCache,
        programWebHookNotificationTemplateCache,
        programStageWebHookNotificationTemplateCache,
        pgmOrgUnitAssocCache,
        catOptOrgUnitAssocCache,
        apiTokensCache,
        programCache,
        teiAttributesCache,
        programTeiAttributesCache,
        userGroupUIDCache,
        securityCache,
        metadataObjectCache;
    }

    private final Map<String, Cache<?, ?>> allCaches = new ConcurrentHashMap<>();

    private long orZeroInTestRun( long value )
    {
        return isTestRun( environment.getActiveProfiles() ) ? 0 : value;
    }

    private <T, V> CacheBuilder<T, V> newBuilder()
    {
        return cacheBuilderProvider.newCacheBuilder();
    }

    @SuppressWarnings( "unchecked" )
    private <T, V> Cache<T, V> registerCache( CacheBuilder<T, V> builder )
    {
        return (Cache<T, V>) allCaches.computeIfAbsent( builder.getRegion(), region -> builder.build() );
    }

    private long getActualSize( long size )
    {
        return (long) Math.max( this.cacheFactor * size, 1 );
    }

    @EventListener
    @Override
    public void handleApplicationCachesCleared( ApplicationCacheClearedEvent event )
    {
        allCaches.values().forEach( Cache::invalidateAll );
    }

    @Override
    public <T, V> Cache<T, V> createAnalyticsResponseCache( Duration initialExpirationTime )
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.analyticsResponse.name() )
            .expireAfterWrite( initialExpirationTime.toMillis(), MILLISECONDS )
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_10K ) ) ) );
    }

    /**
     * Cache for default objects such as default category combination and
     * default category option combination which are permanent and will never
     * change.
     */
    @Override
    public <T, V> Cache<T, V> createDefaultObjectCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.defaultObjectCache.name() )
            .expireAfterAccess( 12, TimeUnit.HOURS )
            .withInitialCapacity( (int) getActualSize( 4 ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_100 ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createIsDataApprovedCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.isDataApproved.name() )
            .expireAfterWrite( 12, TimeUnit.HOURS )
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_10K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createAllConstantsCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.allConstantsCache.name() )
            .expireAfterWrite( 2, TimeUnit.MINUTES )
            .withInitialCapacity( (int) getActualSize( 1 ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_1 ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createInUserOrgUnitHierarchyCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.inUserOuHierarchy.name() )
            .expireAfterWrite( 1, TimeUnit.HOURS )
            .withInitialCapacity( (int) getActualSize( SIZE_1K ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_10K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createInUserViewOrgUnitHierarchyCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.isUserViewOuHierHierarchy.name() )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( (int) getActualSize( SIZE_1K ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_10K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createInUserSearchOrgUnitHierarchyCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.inUserSearchOuHierarchy.name() )
            .expireAfterWrite( 1, TimeUnit.HOURS )
            .withInitialCapacity( (int) getActualSize( SIZE_1K ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_10K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createUserCaptureOrgUnitThresholdCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.userCaptureOuCountThreshold.name() )
            .expireAfterWrite( 1, TimeUnit.HOURS )
            .withInitialCapacity( (int) getActualSize( SIZE_1K ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_10K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createPeriodIdCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.periodIdCache.name() )
            .expireAfterWrite( 24, TimeUnit.HOURS )
            .withInitialCapacity( (int) getActualSize( 200 ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_10K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createUserAccountRecoverAttemptCache( V defaultValue )
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.userAccountRecoverAttempt.name() )
            .expireAfterWrite( 15, MINUTES )
            .withDefaultValue( defaultValue ) );
    }

    @Override
    public <T, V> Cache<T, V> createUserFailedLoginAttemptCache( V defaultValue )
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.userFailedLoginAttempt.name() )
            .expireAfterWrite( 15, MINUTES )
            .withDefaultValue( defaultValue ) );
    }

    @Override
    public <T, V> Cache<T, V> createProgramOwnerCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.programOwner.name() )
            .expireAfterWrite( 5, TimeUnit.MINUTES )
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_1K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createProgramTempOwnerCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.programTempOwner.name() )
            .expireAfterWrite( 30, TimeUnit.MINUTES )
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_10K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createUserIdCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.userIdCache.name() )
            .expireAfterWrite( 1, TimeUnit.HOURS )
            .withInitialCapacity( (int) getActualSize( 200 ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_10K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createCurrentUserGroupInfoCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.currentUserGroupInfoCache.name() )
            .expireAfterWrite( 1, TimeUnit.HOURS )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_10K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createUserSettingCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.userSetting.name() )
            .expireAfterWrite( 12, TimeUnit.HOURS )
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_10K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createAttrOptionComboIdCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.attrOptionComboIdCache.name() )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( (int) getActualSize( SIZE_1K ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_10K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createSystemSettingCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.systemSetting.name() )
            .expireAfterWrite( 12, TimeUnit.HOURS )
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_1K ) ) ) );
    }

    /**
     * Cache for Google API access tokens. Expiration is set to 10 minutes after
     * access to match the expiration of Google service tokens.
     */
    @Override
    public <T, V> Cache<T, V> createGoogleAccessTokenCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.googleAccessToken.name() )
            .expireAfterAccess( 10, MINUTES )
            .withMaximumSize( orZeroInTestRun( 1 ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createDataItemsPaginationCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.dataItemsPagination.name() )
            .expireAfterWrite( 5, MINUTES )
            .withInitialCapacity( (int) getActualSize( SIZE_1K ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_10K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createMetadataAttributesCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.metadataAttributes.name() )
            .expireAfterWrite( 12, TimeUnit.HOURS )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_1K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createCanDataWriteCocCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.canDataWriteCocCache.name() )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( (int) getActualSize( SIZE_1K ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_10K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createAnalyticsSqlCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.analyticsSql.name() )
            .expireAfterWrite( 10, TimeUnit.HOURS )
            .withInitialCapacity( (int) getActualSize( SIZE_1K ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_10K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createDataElementCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.dataElementCache.name() )
            .expireAfterWrite( 60, TimeUnit.MINUTES )
            .withInitialCapacity( (int) getActualSize( SIZE_1K ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_10K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createPropertyTransformerCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.propertyTransformerCache.name() )
            .expireAfterWrite( 12, TimeUnit.HOURS )
            .withInitialCapacity( (int) getActualSize( 20 ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_10K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createProgramHasRulesCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.programHasRulesCache.name() )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( (int) getActualSize( 20 ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_1K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createProgramRuleVariablesCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.programRuleVariablesCache.name() )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( (int) getActualSize( 20 ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_1K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createUserGroupNameCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.userGroupNameCache.name() )
            .expireAfterWrite( 1, TimeUnit.HOURS )
            .withInitialCapacity( (int) getActualSize( 20 ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_1K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createUserDisplayNameCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.userDisplayNameCache.name() )
            .expireAfterWrite( 1, TimeUnit.HOURS )
            .withInitialCapacity( (int) getActualSize( 20 ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( SIZE_10K ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createProgramWebHookNotificationTemplateCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.programWebHookNotificationTemplateCache.name() )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( (int) getActualSize( 20 ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_500 ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createProgramStageWebHookNotificationTemplateCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.programStageWebHookNotificationTemplateCache.name() )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( (int) getActualSize( 20 ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_500 ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createProgramOrgUnitAssociationCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.pgmOrgUnitAssocCache.name() )
            .expireAfterWrite( 1, TimeUnit.HOURS )
            .withInitialCapacity( (int) getActualSize( 20 ) )
            .withMaximumSize( orZeroInTestRun( SIZE_1K ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createCatOptOrgUnitAssociationCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.catOptOrgUnitAssocCache.name() )
            .expireAfterWrite( 1, TimeUnit.HOURS )
            .withInitialCapacity( (int) getActualSize( 20 ) )
            .withMaximumSize( orZeroInTestRun( SIZE_1K ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createApiKeyCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.apiTokensCache.name() )
            .expireAfterWrite( 1, TimeUnit.HOURS )
            .withInitialCapacity( (int) getActualSize( SIZE_1K ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_10K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createProgramCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.programCache.name() )
            .expireAfterWrite( 1, TimeUnit.MINUTES )
            .withInitialCapacity( (int) getActualSize( SIZE_1K ) )
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_10K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createTeiAttributesCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.teiAttributesCache.name() )
            .expireAfterWrite( 10, TimeUnit.MINUTES )
            .withInitialCapacity( (int) getActualSize( SIZE_1 ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_1 ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createProgramTeiAttributesCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.programTeiAttributesCache.name() )
            .expireAfterWrite( 10, TimeUnit.MINUTES )
            .withInitialCapacity( (int) getActualSize( SIZE_1 ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_1 ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createUserGroupUIDCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.userGroupUIDCache.name() )
            .expireAfterWrite( 10, TimeUnit.MINUTES )
            .withInitialCapacity( (int) getActualSize( SIZE_100 ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_1K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createSecurityCache()
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.securityCache.name() )
            .expireAfterWrite( 10, TimeUnit.MINUTES )
            .withInitialCapacity( (int) getActualSize( SIZE_100 ) )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( getActualSize( SIZE_1K ) ) ) );
    }

    @Override
    public <T, V> Cache<T, V> createMetadataObjectCache( Class<? extends IdentifiableObject> klass, long maxCapacity,
        CacheLoaderProvider<T, V> cacheLoaderProvider )
    {
        return registerCache( this.<T, V> newBuilder()
            .forRegion( Region.metadataObjectCache.name().concat( klass.getCanonicalName() ) )
            .withMaximumSize( maxCapacity )
            .expireAfterWrite( 6, HOURS )
            .withLoader( cacheLoaderProvider.loader() )
            .withBulkLoader( cacheLoaderProvider.bulkLoader() )
            .withRefreshAhead( true )
            .withMaximumSize( maxCapacity ) );
    }
}
