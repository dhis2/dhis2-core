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
package org.hisp.dhis.analytics.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.hisp.dhis.analytics.AnalyticsCacheTtlMode;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheBuilder;
import org.hisp.dhis.cache.DefaultCacheProvider;
import org.hisp.dhis.cache.LocalCache;
import org.hisp.dhis.cache.SimpleCacheBuilder;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.BeforeEach;
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

  @Mock private SystemSettingsService settingsService;
  @Mock private DefaultCacheProvider cacheProvider;
  @Mock private SystemSettings systemSettings;
  @Mock private SystemSettingsProvider systemSettingsProvider;

  private AnalyticsCache analyticsCache;

  @BeforeEach
  void setUp() {

    AnalyticsCacheSettings settings = new AnalyticsCacheSettings(settingsService);
    CacheBuilder<Grid> cacheBuilder = new SimpleCacheBuilder<>();
    cacheBuilder.expireAfterWrite(1L, TimeUnit.MINUTES);
    Cache<Grid> cache = new LocalCache<>(cacheBuilder);
    Mockito.<Cache<Grid>>when(cacheProvider.createAnalyticsCache()).thenReturn(cache);
    analyticsCache = new AnalyticsCache(cacheProvider, settings);
  }

  @Test
  void returnSameObjectAfterModifyCachedObject() {
    // arrange
    AnalyticsCacheSettings settings = new AnalyticsCacheSettings(settingsService);

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

  @Test
  void onlyTopLevelCallsAddToCache() {
    when(settingsService.getCurrentSettings()).thenReturn(systemSettings);
    when(systemSettings.getAnalyticsCacheTtlMode()).thenReturn(AnalyticsCacheTtlMode.FIXED);

    // Create string keys for our test
    String outerKey = "outerKey";
    String innerKey = "innerKey";

    // Mock DataQueryParams for outer and inner calls
    DataQueryParams outerParams = mock(DataQueryParams.class);
    DataQueryParams innerParams = mock(DataQueryParams.class);

    // Set up key behavior
    when(outerParams.getKey()).thenReturn(outerKey);
    when(innerParams.getKey()).thenReturn(innerKey);

    // Create a function that simulates nested calls
    Function<DataQueryParams, Grid> outerFunction =
        params -> {
          Grid outerGrid = new ListGrid();
          outerGrid.addHeader(new GridHeader("OuterHeader"));

          // Make a nested call to getOrFetch inside the function
          analyticsCache.getOrFetch(
              innerParams,
              innerParams2 -> {
                Grid grid = new ListGrid();
                grid.addHeader(new GridHeader("InnerHeader"));
                return grid;
              });

          return outerGrid;
        };

    // Execute the outer getOrFetch
    analyticsCache.getOrFetch(outerParams, outerFunction);

    // Verify that outer grid was cached
    Optional<Grid> cachedOuterGrid = analyticsCache.get(outerKey);
    assertTrue(cachedOuterGrid.isPresent(), "Outer grid should be cached");

    // Verify that inner grid was NOT cached
    Optional<Grid> cachedInnerGrid = analyticsCache.get(innerKey);
    assertFalse(cachedInnerGrid.isPresent(), "Inner grid should NOT be cached");
  }

  @Test
  void nestedCallsReadFromCacheCorrectly() {
    // Create parameters
    DataQueryParams outerParams =
        DataQueryParams.newBuilder()
            .withDataElements(List.of(new DataElement("outerElement")))
            .build();

    DataQueryParams innerParams =
        DataQueryParams.newBuilder()
            .withDataElements(List.of(new DataElement("innerElement")))
            .build();

    // Pre-populate cache with inner grid
    Grid preloadedGrid = new ListGrid();
    preloadedGrid.addHeader(new GridHeader("PreloadedHeader"));
    analyticsCache.put(innerParams.getKey(), preloadedGrid, 60);

    // Create a spy to track cache usage
    AnalyticsCache spyCache = spy(analyticsCache);

    // Create a function that simulates nested calls
    Function<DataQueryParams, Grid> outerFunction =
        params -> {
          Grid outerGrid = new ListGrid();
          outerGrid.addHeader(new GridHeader("OuterHeader"));

          // Make a nested call to getOrFetch
          Grid innerGrid =
              spyCache.getOrFetch(
                  innerParams,
                  innerParams2 -> {
                    fail("Should not execute this function because inner grid is already cached");
                    return null;
                  });

          // Verify we got the cached grid
          assertEquals("PreloadedHeader", innerGrid.getHeaders().get(0).getName());

          return outerGrid;
        };

    // Method under test
    spyCache.getOrFetch(outerParams, outerFunction);

    verify(spyCache, times(1)).get(innerParams.getKey());
  }

  @Test
  void multipleLevelsOfNesting() {
    when(settingsService.getCurrentSettings()).thenReturn(systemSettings);
    when(systemSettings.getAnalyticsCacheTtlMode()).thenReturn(AnalyticsCacheTtlMode.FIXED);

    DataQueryParams level1Params = mock(DataQueryParams.class);
    DataQueryParams level2Params = mock(DataQueryParams.class);
    DataQueryParams level3Params = mock(DataQueryParams.class);

    String level1Key = "level1Key";
    String level2Key = "level2Key";
    String level3Key = "level3Key";

    when(level1Params.getKey()).thenReturn(level1Key);
    when(level2Params.getKey()).thenReturn(level2Key);
    when(level3Params.getKey()).thenReturn(level3Key);

    // Level 3 function (innermost)
    Function<DataQueryParams, Grid> level3Function =
        params -> {
          Grid grid = new ListGrid();
          grid.addHeader(new GridHeader("Level3Header"));
          return grid;
        };

    // Level 2 function
    Function<DataQueryParams, Grid> level2Function =
        params -> {
          Grid grid = new ListGrid();
          grid.addHeader(new GridHeader("Level2Header"));

          // Nested call to level 3
          analyticsCache.getOrFetch(level3Params, level3Function);
          return grid;
        };

    // Level 1 function (outermost)
    Function<DataQueryParams, Grid> level1Function =
        params -> {
          Grid grid = new ListGrid();
          grid.addHeader(new GridHeader("Level1Header"));

          // Nested call to level 2
          analyticsCache.getOrFetch(level2Params, level2Function);

          return grid;
        };

    // Execute the outermost getOrFetch
    analyticsCache.getOrFetch(level1Params, level1Function);

    // Verify that only level 1 was cached
    assertTrue(analyticsCache.get(level1Key).isPresent(), "Level 1 grid should be cached");
    assertFalse(analyticsCache.get(level2Key).isPresent(), "Level 2 grid should NOT be cached");
    assertFalse(analyticsCache.get(level3Key).isPresent(), "Level 3 grid should NOT be cached");
  }

  @Test
  void clearingNestedCallFlagAfterException() {

    when(settingsService.getCurrentSettings()).thenReturn(systemSettings);
    when(systemSettings.getAnalyticsCacheTtlMode()).thenReturn(AnalyticsCacheTtlMode.FIXED);

    // Create parameters
    DataQueryParams outerParams =
        DataQueryParams.newBuilder()
            .withDataElements(List.of(new DataElement("outerElement")))
            .build();

    DataQueryParams innerParams =
        DataQueryParams.newBuilder()
            .withDataElements(List.of(new DataElement("innerElement")))
            .build();

    // First call will throw an exception
    Function<DataQueryParams, Grid> outerFunctionWithException =
        params -> {
          Grid outerGrid = new ListGrid();

          // Make a nested call that throws an exception
          try {
            analyticsCache.getOrFetch(
                innerParams,
                innerParams2 -> {
                  throw new RuntimeException("Test exception");
                });
          } catch (RuntimeException e) {
            // ok, continue test
          }

          return outerGrid;
        };

    try {
      // This should throw
      analyticsCache.getOrFetch(outerParams, outerFunctionWithException);
    } catch (RuntimeException e) {
      // Expected
    }

    // Now make another call -> should succeed
    DataQueryParams newParams =
        DataQueryParams.newBuilder()
            .withDataElements(List.of(new DataElement("newElement")))
            .build();

    analyticsCache.getOrFetch(
        newParams,
        params -> {
          Grid grid = new ListGrid();
          grid.addHeader(new GridHeader("NewHeader"));
          return grid;
        });

    // Verify this grid was cached
    assertTrue(
        analyticsCache.get(newParams.getKey()).isPresent(),
        "New grid should be cached because nested call flag was properly reset");
  }

  @Test
  void concurrentRequestsDontInterfere() throws Exception {
    when(settingsService.getCurrentSettings()).thenReturn(systemSettings);
    when(systemSettings.getAnalyticsCacheTtlMode()).thenReturn(AnalyticsCacheTtlMode.FIXED);

    // Create parameter sets for two concurrent threads with mocks
    DataQueryParams thread1Params = mock(DataQueryParams.class);
    DataQueryParams thread1NestedParams = mock(DataQueryParams.class);
    DataQueryParams thread2Params = mock(DataQueryParams.class);
    DataQueryParams thread2NestedParams = mock(DataQueryParams.class);

    // Set up keys
    String thread1Key = "thread1Key";
    String thread1NestedKey = "thread1NestedKey";
    String thread2Key = "thread2Key";
    String thread2NestedKey = "thread2NestedKey";

    when(thread1Params.getKey()).thenReturn(thread1Key);
    when(thread1NestedParams.getKey()).thenReturn(thread1NestedKey);
    when(thread2Params.getKey()).thenReturn(thread2Key);
    when(thread2NestedParams.getKey()).thenReturn(thread2NestedKey);

    // Create a function with nested calls for thread 1
    Function<DataQueryParams, Grid> thread1Function =
        params -> {
          Grid grid = new ListGrid();
          grid.addHeader(new GridHeader("Thread1Header"));

          // Nested call

          analyticsCache.getOrFetch(
              thread1NestedParams,
              nestedParams -> {
                Grid ng = new ListGrid();
                ng.addHeader(new GridHeader("Thread1NestedHeader"));
                return ng;
              });

          return grid;
        };

    // Create a function with nested calls for thread 2
    Function<DataQueryParams, Grid> thread2Function =
        params -> {
          Grid grid = new ListGrid();
          grid.addHeader(new GridHeader("Thread2Header"));

          // Nested call
          analyticsCache.getOrFetch(
              thread2NestedParams,
              nestedParams -> {
                Grid ng = new ListGrid();
                ng.addHeader(new GridHeader("Thread2NestedHeader"));
                return ng;
              });

          return grid;
        };

    // Run the two functions in separate threads
    Thread thread1 =
        new Thread(
            () -> {
              analyticsCache.getOrFetch(thread1Params, thread1Function);
            });

    Thread thread2 =
        new Thread(
            () -> {
              analyticsCache.getOrFetch(thread2Params, thread2Function);
            });

    thread1.start();
    thread2.start();

    // Wait for both threads to complete
    thread1.join();
    thread2.join();

    // Verify that both top-level grids were cached
    assertTrue(
        analyticsCache.get(thread1Key).isPresent(), "Thread 1 top-level grid should be cached");
    assertTrue(
        analyticsCache.get(thread2Key).isPresent(), "Thread 2 top-level grid should be cached");

    // Verify that nested grids were not cached
    assertFalse(
        analyticsCache.get(thread1NestedKey).isPresent(),
        "Thread 1 nested grid should NOT be cached");
    assertFalse(
        analyticsCache.get(thread2NestedKey).isPresent(),
        "Thread 2 nested grid should NOT be cached");
  }
}
