/*
 * Copyright (c) 2004-2024, University of Oslo
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
class DefaultCacheProviderTest {

  @Mock private DhisConfigurationProvider dhisConfigurationProvider;

  @Mock private CacheBuilderProvider cacheBuilderProvider;

  @Mock private CacheBuilder<Object> cacheBuilder;

  @Mock private Cache<Object> mockCache;

  MockEnvironment environment = new MockEnvironment();

  private DefaultCacheProvider defaultCacheProvider;

  @BeforeEach
  public void setUp() {
    environment.setActiveProfiles("nonTestProfile");
    when(dhisConfigurationProvider.getProperty(ConfigurationKey.SYSTEM_CACHE_MAX_SIZE_FACTOR))
        .thenReturn("0.5");
    defaultCacheProvider =
        new DefaultCacheProvider(cacheBuilderProvider, environment, dhisConfigurationProvider);
  }

  private void registerCache(Region region, Runnable cacheCreator) {
    when(cacheBuilder.getRegion()).thenReturn(region.name());
    when(cacheBuilder.forRegion(any())).thenReturn(cacheBuilder);
    when(cacheBuilder.expireAfterWrite(anyLong(), any())).thenReturn(cacheBuilder);
    when(cacheBuilder.withMaximumSize(anyLong())).thenReturn(cacheBuilder);
    when(cacheBuilderProvider.newCacheBuilder()).thenReturn(cacheBuilder);
    when(cacheBuilder.build()).thenReturn(mockCache);
    cacheCreator.run();
  }

  @Test
  void testInvalidateSpecificKey() {

    registerCache(Region.analyticsResponse, () -> defaultCacheProvider.createOutliersCache());
    String key = "specificKeyInRegionToInvalidate";
    CacheInvalidationEvent event = new CacheInvalidationEvent(this, Region.analyticsResponse, key);

    defaultCacheProvider.handleCacheInvalidationEvent(event);

    verify(mockCache).invalidate(key);
    verifyNoMoreInteractions(mockCache);
  }

  @Test
  void testInvalidateAllKeysInRegion() {
    when(cacheBuilder.withInitialCapacity(anyInt())).thenReturn(cacheBuilder);
    registerCache(
        Region.canDataWriteCocCache, () -> defaultCacheProvider.createCanDataWriteCocCache());

    CacheInvalidationEvent event = new CacheInvalidationEvent(this, Region.canDataWriteCocCache);

    defaultCacheProvider.handleCacheInvalidationEvent(event);

    verify(mockCache).invalidateAll();
    verifyNoMoreInteractions(mockCache);
  }

  @Test
  void testInvalidateUncachedRegion() {
    CacheInvalidationEvent event = new CacheInvalidationEvent(this, Region.canDataWriteCocCache);

    defaultCacheProvider.handleCacheInvalidationEvent(event);

    verifyNoInteractions(mockCache);
  }
}
