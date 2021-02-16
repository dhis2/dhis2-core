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
import java.util.concurrent.TimeUnit;

import lombok.AllArgsConstructor;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

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

    private final CacheBuilderProvider cacheBuilderProvider;

    private final Environment environment;

    private int orZeroInTestRun( int value )
    {
        return isTestRun( environment.getActiveProfiles() ) ? 0 : value;
    }

    private <V> CacheBuilder<V> newBuilder( Class<V> valueType )
    {
        return cacheBuilderProvider.newCacheBuilder( valueType );
    }

    @Override
    public <V> Cache<V> createAnalyticsResponseCache( Class<V> valueType, Duration initialExpirationTime )
    {
        return newBuilder( valueType )
            .forRegion( "analyticsResponse" )
            .expireAfterWrite( initialExpirationTime.toMillis(), MILLISECONDS )
            .withMaximumSize( orZeroInTestRun( 20000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createAppCache( Class<V> valueType )
    {
        return newBuilder( valueType )
            .forRegion( "appCache" )
            .build();
    }

    @Override
    public <V> Cache<V> createDefaultObjectCache( Class<V> valueType )
    {
        return newBuilder( valueType )
            .forRegion( "defaultObjectCache" )
            .expireAfterAccess( 2, TimeUnit.HOURS )
            .withInitialCapacity( 4 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 10 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createIsDataApprovedCache( Class<V> valueType )
    {
        return newBuilder( valueType )
            .forRegion( "isDataApproved" )
            .expireAfterAccess( 12, TimeUnit.HOURS )
            .withMaximumSize( orZeroInTestRun( 20000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createAllConstantsCache( Class<V> valueType )
    {
        return newBuilder( valueType )
            .forRegion( "allConstantsCache" )
            .expireAfterAccess( 2, TimeUnit.MINUTES )
            .withInitialCapacity( 1 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 1 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createInUserOrgUnitHierarchyCache( Class<V> valueType )
    {
        return newBuilder( valueType )
            .forRegion( "inUserOuHierarchy" )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 20000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createInUserSearchOrgUnitHierarchyCache( Class<V> valueType )
    {
        return newBuilder( valueType )
            .forRegion( "inUserSearchOuHierarchy" )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 20000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createUserCaptureOrgUnitThresholdCache( Class<V> valueType )
    {
        return newBuilder( valueType )
            .forRegion( "userCaptureOuCountThreshold" )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 20000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createPeriodIdCache( Class<V> valueType )
    {
        return newBuilder( valueType )
            .forRegion( "periodIdCache" )
            .expireAfterWrite( 24, TimeUnit.HOURS )
            .withInitialCapacity( 200 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 10000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createUserAccountRecoverAttemptCache( Class<V> valueType, V defaultValue )
    {
        return newBuilder( valueType )
            .forRegion( "userAccountRecoverAttempt" )
            .expireAfterWrite( 15, TimeUnit.MINUTES )
            .withDefaultValue( defaultValue )
            .build();
    }

    @Override
    public <V> Cache<V> createUserFailedLoginAttemptCache( Class<V> valueType, V defaultValue )
    {
        return newBuilder( valueType )
            .forRegion( "userFailedLoginAttempt" )
            .expireAfterWrite( 15, TimeUnit.MINUTES )
            .withDefaultValue( defaultValue )
            .build();
    }

    @Override
    public <V> Cache<V> createProgramOwnerCache( Class<V> valueType )
    {
        return newBuilder( valueType )
            .forRegion( "programOwner" )
            .expireAfterWrite( 5, TimeUnit.MINUTES )
            .withMaximumSize( orZeroInTestRun( 1000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createProgramTempOwnerCache( Class<V> valueType )
    {
        return newBuilder( valueType )
            .forRegion( "programTempOwner" )
            .expireAfterWrite( 30, TimeUnit.MINUTES )
            .withMaximumSize( orZeroInTestRun( 4000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createUserIdCacheCache( Class<V> valueType )
    {
        return newBuilder( valueType )
            .forRegion( "userIdCache" )
            .expireAfterAccess( 1, TimeUnit.HOURS )
            .withInitialCapacity( 200 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 4000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createCurrentUserGroupInfoCache( Class<V> valueType )
    {
        return newBuilder( valueType )
            .forRegion( "currentUserGroupInfoCache" )
            .expireAfterWrite( 1, TimeUnit.HOURS )
            .forceInMemory()
            .build();
    }

    @Override
    public <V> Cache<V> createUserSettingCache( Class<V> valueType )
    {
        return newBuilder( valueType )
            .forRegion( "userSetting" )
            .expireAfterWrite( 12, TimeUnit.HOURS )
            .withMaximumSize( orZeroInTestRun( 10000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createAttrOptionComboIdCache( Class<V> valueType )
    {
        return newBuilder( valueType )
            .forRegion( "attrOptionComboIdCache" )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 10000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createSystemSettingCache( Class<V> valueType )
    {
        return newBuilder( valueType )
            .forRegion( "systemSetting" )
            .expireAfterWrite( 12, TimeUnit.HOURS )
            .withMaximumSize( orZeroInTestRun( 400 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createGoogleAccessTokenCache( Class<V> valueType )
    {
        return newBuilder( valueType )
            .forRegion( "googleAccessToken" )
            .expireAfterAccess( 10, TimeUnit.MINUTES )
            .withMaximumSize( orZeroInTestRun( 1 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createDataItemsPaginationCache( Class<V> valueType )
    {
        return newBuilder( valueType )
            .forRegion( "dataItemsPagination" )
            .expireAfterWrite( 5, MINUTES )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 20000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createMetadataAttributesCache( Class<V> valueType )
    {
        return newBuilder( valueType )
            .forRegion( "metadataAttributes" )
            .expireAfterWrite( 12, TimeUnit.HOURS )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 10000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createCanDataWriteCocCache( Class<V> valueType )
    {
        return newBuilder( valueType )
            .forRegion( "canDataWriteCocCache" )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 10000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createAnalyticsSqlCache( Class<V> valueType )
    {
        return newBuilder( valueType )
            .forRegion( "analyticsSql" )
            .expireAfterAccess( 10, TimeUnit.HOURS )
            .withInitialCapacity( 10000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 50000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createDataElementCache( Class<V> valueType )
    {
        return newBuilder( valueType )
            .forRegion( "dataElementCache" )
            .expireAfterAccess( 60, TimeUnit.MINUTES )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 50000 ) )
            .build();
    }

    @Override
    public <V> Cache<V> createPropertyTransformerCache( Class<V> valueType )
    {
        return newBuilder( valueType )
            .forRegion( "propertyTransformerCache" )
            .expireAfterAccess( 12, TimeUnit.HOURS )
            .withInitialCapacity( 20 )
            .forceInMemory()
            .withMaximumSize( orZeroInTestRun( 30000 ) )
            .build();
    }
}
