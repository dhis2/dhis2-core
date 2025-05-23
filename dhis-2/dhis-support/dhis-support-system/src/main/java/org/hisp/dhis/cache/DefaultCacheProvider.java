/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hisp.dhis.commons.util.SystemUtils.isEnableCacheInTest;
import static org.hisp.dhis.commons.util.SystemUtils.isTestRun;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.cache.Region;
import org.hisp.dhis.common.event.ApplicationCacheClearedEvent;
import org.hisp.dhis.common.event.CacheInvalidationEvent;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * The {@link DefaultCacheProvider} has the specific configuration for each of the {@link Cache}
 * factory methods as used within DHIS2.
 *
 * @author Jan Bernitt
 */
@Component("defaultCacheProvider")
public class DefaultCacheProvider implements CacheProvider {
  private static final long SIZE_1 = 1;

  private static final long SIZE_100 = 100;

  private static final long SIZE_1K = 1_000;

  private static final long SIZE_10K = 10_000;

  private final double cacheFactor;

  private final CacheBuilderProvider cacheBuilderProvider;

  private final Environment environment;

  public DefaultCacheProvider(
      CacheBuilderProvider cacheBuilderProvider,
      Environment environment,
      DhisConfigurationProvider dhisConfig) {
    this.cacheBuilderProvider = cacheBuilderProvider;
    this.environment = environment;
    this.cacheFactor =
        Double.parseDouble(dhisConfig.getProperty(ConfigurationKey.SYSTEM_CACHE_MAX_SIZE_FACTOR));
  }

  private final Map<String, Cache<?>> allCaches = new ConcurrentHashMap<>();

  private long orZeroInTestRun(long value) {
    boolean isEnableCacheInTest = isEnableCacheInTest(environment.getActiveProfiles());
    boolean isTestRun = isTestRun(environment.getActiveProfiles());
    return isTestRun && !isEnableCacheInTest ? 0 : value;
  }

  private <V> CacheBuilder<V> newBuilder() {
    return cacheBuilderProvider.newCacheBuilder();
  }

  @SuppressWarnings("unchecked")
  private <V> Cache<V> registerCache(CacheBuilder<V> builder) {
    return (Cache<V>) allCaches.computeIfAbsent(builder.getRegion(), region -> builder.build());
  }

  private long getActualSize(long size) {
    return (long) Math.max(this.cacheFactor * size, 1);
  }

  @EventListener
  @Override
  public void handleApplicationCachesCleared(ApplicationCacheClearedEvent event) {
    allCaches.values().forEach(Cache::invalidateAll);
  }

  @EventListener
  @Override
  public void handleCacheInvalidationEvent(CacheInvalidationEvent event) {
    Cache<?> cache = allCaches.get(event.getRegion().name());
    if (cache == null) {
      return;
    }

    if (StringUtils.isNotBlank(event.getKey())) {
      cache.invalidate(event.getKey());
    } else {
      cache.invalidateAll();
    }
  }

  @Override
  public <V> Cache<V> createAnalyticsCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.analyticsResponse.name())
            .expireAfterWrite(12, TimeUnit.HOURS)
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_10K))));
  }

  @Override
  public <V> Cache<V> createOutliersCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.analyticsResponse.name())
            .expireAfterWrite(12, TimeUnit.HOURS)
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_10K))));
  }

  /**
   * Cache for default objects such as default category combination and default category option
   * combination which are permanent and will never change.
   */
  @Override
  public <V> Cache<V> createDefaultObjectCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.defaultObjectCache.name())
            .expireAfterAccess(12, TimeUnit.HOURS)
            .withInitialCapacity((int) getActualSize(4))
            .forceInMemory()
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_100))));
  }

  @Override
  public <V> Cache<V> createIsDataApprovedCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.isDataApproved.name())
            .expireAfterWrite(12, TimeUnit.HOURS)
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_10K))));
  }

  @Override
  public <V> Cache<V> createAllConstantsCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.allConstantsCache.name())
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .withInitialCapacity((int) getActualSize(1))
            .forceInMemory()
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_1))));
  }

  @Override
  public <V> Cache<V> createInUserOrgUnitHierarchyCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.inUserOrgUnitHierarchy.name())
            .expireAfterWrite(1, TimeUnit.HOURS)
            .withInitialCapacity((int) getActualSize(SIZE_1K))
            .forceInMemory()
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_10K))));
  }

  @Override
  public <V> Cache<V> createInUserSearchOrgUnitHierarchyCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.inUserSearchOrgUnitHierarchy.name())
            .expireAfterWrite(1, TimeUnit.HOURS)
            .withInitialCapacity((int) getActualSize(SIZE_1K))
            .forceInMemory()
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_10K))));
  }

  @Override
  public <V> Cache<V> createPeriodIdCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.periodIdCache.name())
            .expireAfterWrite(24, TimeUnit.HOURS)
            .withInitialCapacity((int) getActualSize(200))
            .forceInMemory()
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_10K))));
  }

  @Override
  public <V> Cache<V> createUserAccountRecoverAttemptCache(V defaultValue) {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.userAccountRecoverAttempt.name())
            .expireAfterWrite(15, MINUTES)
            .withDefaultValue(defaultValue));
  }

  @Override
  public <V> Cache<V> createUserFailedLoginAttemptCache(V defaultValue) {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.userFailedLoginAttempt.name())
            .expireAfterWrite(15, MINUTES)
            .withDefaultValue(defaultValue));
  }

  @Override
  public <V> Cache<V> createDisable2FAFailedAttemptCache(V defaultValue) {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.twoFaDisableFailedAttempt.name())
            .expireAfterWrite(15, MINUTES)
            .withDefaultValue(defaultValue));
  }

  @Override
  public <V> Cache<V> createProgramOwnerCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.programOwner.name())
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_1K))));
  }

  @Override
  public <V> Cache<V> createProgramTempOwnerCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.programTempOwner.name())
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_10K))));
  }

  @Override
  public <V> Cache<V> createCurrentUserGroupInfoCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.currentUserGroupInfoCache.name())
            .expireAfterWrite(1, TimeUnit.HOURS)
            .forceInMemory()
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_10K))));
  }

  @Override
  public <V> Cache<V> createAttrOptionComboIdCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.attrOptionComboIdCache.name())
            .expireAfterWrite(3, TimeUnit.HOURS)
            .withInitialCapacity((int) getActualSize(SIZE_1K))
            .forceInMemory()
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_10K))));
  }

  /**
   * Cache for Google API access tokens. Expiration is set to 10 minutes after access to match the
   * expiration of Google service tokens.
   */
  @Override
  public <V> Cache<V> createGoogleAccessTokenCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.googleAccessToken.name())
            .expireAfterAccess(10, MINUTES)
            .withMaximumSize(orZeroInTestRun(1)));
  }

  @Override
  public <V> Cache<V> createDataItemsPaginationCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.dataItemsPagination.name())
            .expireAfterWrite(5, MINUTES)
            .withInitialCapacity((int) getActualSize(SIZE_1K))
            .forceInMemory()
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_10K))));
  }

  @Override
  public <V> Cache<V> createMetadataAttributesCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.metadataAttributes.name())
            .expireAfterWrite(12, TimeUnit.HOURS)
            .forceInMemory()
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_1K))));
  }

  @Override
  public <V> Cache<V> createCanDataWriteCocCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.canDataWriteCocCache.name())
            .expireAfterWrite(3, TimeUnit.HOURS)
            .withInitialCapacity((int) getActualSize(SIZE_1K))
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_10K))));
  }

  @Override
  public <V> Cache<V> createAnalyticsSqlCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.analyticsSql.name())
            .expireAfterWrite(10, TimeUnit.HOURS)
            .withInitialCapacity((int) getActualSize(SIZE_1K))
            .forceInMemory()
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_10K))));
  }

  @Override
  public <V> Cache<V> createPropertyTransformerCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.propertyTransformerCache.name())
            .expireAfterWrite(12, TimeUnit.HOURS)
            .withInitialCapacity((int) getActualSize(20))
            .forceInMemory()
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_10K))));
  }

  @Override
  public <V> Cache<V> createUserGroupNameCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.userGroupNameCache.name())
            .expireAfterWrite(1, TimeUnit.HOURS)
            .withInitialCapacity((int) getActualSize(20))
            .forceInMemory()
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_1K))));
  }

  @Override
  public <V> Cache<V> createUserDisplayNameCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.userDisplayNameCache.name())
            .expireAfterWrite(1, TimeUnit.HOURS)
            .withInitialCapacity((int) getActualSize(20))
            .forceInMemory()
            .withMaximumSize(orZeroInTestRun(SIZE_10K)));
  }

  @Override
  public <V> Cache<V> createProgramOrgUnitAssociationCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.pgmOrgUnitAssocCache.name())
            .expireAfterWrite(1, TimeUnit.HOURS)
            .withInitialCapacity((int) getActualSize(20))
            .withMaximumSize(orZeroInTestRun(SIZE_1K)));
  }

  @Override
  public <V> Cache<V> createCatOptOrgUnitAssociationCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.catOptOrgUnitAssocCache.name())
            .expireAfterWrite(1, TimeUnit.HOURS)
            .withInitialCapacity((int) getActualSize(20))
            .withMaximumSize(orZeroInTestRun(SIZE_1K)));
  }

  @Override
  public <V> Cache<V> createDataSetOrgUnitAssociationCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.dataSetOrgUnitAssocCache.name())
            .expireAfterWrite(1, TimeUnit.HOURS)
            .withInitialCapacity((int) getActualSize(20))
            .withMaximumSize(orZeroInTestRun(SIZE_1K)));
  }

  @Override
  public <V> Cache<V> createApiKeyCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.apiTokensCache.name())
            .expireAfterWrite(1, TimeUnit.HOURS)
            .withInitialCapacity((int) getActualSize(SIZE_1K))
            .forceInMemory()
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_10K))));
  }

  @Override
  public <V> Cache<V> createTeAttributesCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.teAttributesCache.name())
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .withInitialCapacity((int) getActualSize(SIZE_1))
            .forceInMemory()
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_1))));
  }

  @Override
  public <V> Cache<V> createProgramTeAttributesCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.programTeAttributesCache.name())
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .withInitialCapacity((int) getActualSize(SIZE_1))
            .forceInMemory()
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_1))));
  }

  @Override
  public <V> Cache<V> createUserGroupUIDCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.userGroupUIDCache.name())
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .withInitialCapacity((int) getActualSize(SIZE_100))
            .forceInMemory()
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_1K))));
  }

  @Override
  public <V> Cache<V> createSecurityCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.securityCache.name())
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .withInitialCapacity((int) getActualSize(SIZE_100))
            .forceInMemory()
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_1K))));
  }

  @Override
  public <V> Cache<V> createDataIntegritySummaryCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.dataIntegritySummaryCache.name())
            .expireAfterWrite(1, HOURS));
  }

  @Override
  public <V> Cache<V> createDataIntegrityDetailsCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.dataIntegrityDetailsCache.name())
            .expireAfterWrite(1, HOURS));
  }

  @Override
  public <V> Cache<V> createQueryAliasCache() {
    return registerCache(
        this.<V>newBuilder()
            .forRegion(Region.queryAliasCache.name())
            .expireAfterWrite(3, TimeUnit.HOURS)
            .withInitialCapacity((int) getActualSize(SIZE_100))
            .forceInMemory()
            .withMaximumSize(orZeroInTestRun(getActualSize(SIZE_10K))));
  }
}
