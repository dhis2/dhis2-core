package org.hisp.dhis.cache;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.cache.Region;
import org.hisp.dhis.common.event.CacheInvalidationEvent;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(MockitoExtension.class)
public class DefaultCacheProviderTest {

  @Mock private DhisConfigurationProvider dhisConfigurationProvider;

  @Mock private CacheBuilderProvider cacheBuilderProvider;

  @Mock private CacheBuilder<Object> cacheBuilder;

  @Mock private Cache<Object> mockCache;

  MockEnvironment environment = new MockEnvironment();

  private DefaultCacheProvider defaultCacheProvider;

  @BeforeEach
  public void setUp() {
    environment.setActiveProfiles("nonTestProfile");
    when(dhisConfigurationProvider.getProperty(ConfigurationKey.SYSTEM_CACHE_MAX_SIZE_FACTOR)).thenReturn("0.5");
    defaultCacheProvider =
        new DefaultCacheProvider(cacheBuilderProvider, environment, dhisConfigurationProvider);

  }

  private void registerCache(Region region, Runnable cacheCreator) {
    when(cacheBuilder.getRegion()).thenReturn(region.name());
    when(cacheBuilder.forRegion(any())).thenReturn(cacheBuilder);
    when(cacheBuilder.expireAfterWrite(anyLong(),any())).thenReturn(cacheBuilder);
    when(cacheBuilder.withMaximumSize(anyLong())).thenReturn(cacheBuilder);
    when(cacheBuilderProvider.newCacheBuilder()).thenReturn(cacheBuilder);
    when(cacheBuilder.build()).thenReturn(mockCache);
    cacheCreator.run();
  }

  @Test
  void testInvalidateSpecificKey() {

    registerCache(Region.isDataApproved, ()-> defaultCacheProvider.createIsDataApprovedCache());
    String key = "specificKeyInRegionToInvalidate";
    CacheInvalidationEvent event =
        new CacheInvalidationEvent(this, Region.isDataApproved, key);

    defaultCacheProvider.handleCacheInvalidationEvent(event);

    verify(mockCache).invalidate(key);
    verifyNoMoreInteractions(mockCache);
  }

  @Test
  void testInvalidateAllKeysInRegion() {
    when(cacheBuilder.withInitialCapacity(anyInt())).thenReturn(cacheBuilder);
    registerCache(Region.canDataWriteCocCache, ()-> defaultCacheProvider.createCanDataWriteCocCache());

    CacheInvalidationEvent event = new CacheInvalidationEvent(this, Region.canDataWriteCocCache);

    defaultCacheProvider.handleCacheInvalidationEvent(event);

    verify(mockCache).invalidateAll();
    verifyNoMoreInteractions(mockCache);
  }

  @Test
  void testInvalidateUncachedRegion() {
    CacheInvalidationEvent event =
        new CacheInvalidationEvent(this, Region.canDataWriteCocCache);

    defaultCacheProvider.handleCacheInvalidationEvent(event);

    verifyNoInteractions(mockCache);
  }
}
