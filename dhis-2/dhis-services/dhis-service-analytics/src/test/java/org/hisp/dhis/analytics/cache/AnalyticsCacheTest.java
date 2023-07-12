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
package org.hisp.dhis.analytics.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheBuilder;
import org.hisp.dhis.cache.DefaultCacheProvider;
import org.hisp.dhis.cache.LocalCache;
import org.hisp.dhis.cache.SimpleCacheBuilder;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Dusan Bernat
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsCacheTest {
  @Mock private SystemSettingManager systemSettingManager;

  @Mock private DefaultCacheProvider cacheProvider;

  @Test
  void returnSameObjectAfterModifyCachedObject() {
    // arrange
    AnalyticsCacheSettings settings = new AnalyticsCacheSettings(systemSettingManager);

    CacheBuilder<Grid> cacheBuilder = new SimpleCacheBuilder<>();

    cacheBuilder.expireAfterWrite(1L, TimeUnit.MINUTES);

    Cache<Grid> cache = new LocalCache<>(cacheBuilder);

    Mockito.<Cache<Grid>>when(cacheProvider.createAnalyticsCache()).thenReturn(cache);

    AnalyticsCache analyticsCache = new AnalyticsCache(cacheProvider, settings);

    Grid grid = new ListGrid();
    grid.addHeader(new GridHeader("Header1"))
        .addHeader(new GridHeader("Header2"))
        .addRow()
        .addValue("Value11")
        .addValue("Value12")
        .addRow()
        .addValue("Value21")
        .addValue("Value22");

    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withDataElements(
                List.of(new DataElement("dataElementA"), new DataElement("dataElementB")))
            .build();

    // act, assert
    analyticsCache.put(params.getKey(), grid, 60);

    Optional<Grid> optCachedGrid = analyticsCache.get(params.getKey());

    assertTrue(optCachedGrid.isPresent());

    assertEquals(2, optCachedGrid.get().getHeaderWidth());

    assertEquals(2, optCachedGrid.get().getRows().size());

    // when the cachedGrid is not the clone of grid, next actions will
    // modify it
    grid.addHeader(new GridHeader("Header3")).addRow().addValue("31").addValue("32");

    optCachedGrid = analyticsCache.get(params.getKey());

    assertTrue(optCachedGrid.isPresent());

    assertEquals(2, optCachedGrid.get().getHeaderWidth());

    assertEquals(2, optCachedGrid.get().getRows().size());
  }
}
