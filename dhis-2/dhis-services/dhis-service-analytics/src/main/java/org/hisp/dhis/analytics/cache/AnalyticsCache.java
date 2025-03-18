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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.Grid;
import org.springframework.stereotype.Component;

/**
 * This is a wrapper class responsible for keeping and isolating all cache definitions related to
 * the analytics.
 */
@Slf4j
@Component
public class AnalyticsCache {
  private final AnalyticsCacheSettings analyticsCacheSettings;
  private final Cache<Grid> queryCache;
  // Track nested call chain
  private static final ThreadLocal<Integer> nestingLevel = ThreadLocal.withInitial(() -> 0);

  /**
   * Default constructor. Note that a default expiration time is set, as the TTL will always be
   * overwritten during cache put operations.
   */
  public AnalyticsCache(
      CacheProvider cacheProvider, AnalyticsCacheSettings analyticsCacheSettings) {
    checkNotNull(cacheProvider);
    checkNotNull(analyticsCacheSettings);

    this.analyticsCacheSettings = analyticsCacheSettings;
    this.queryCache = cacheProvider.createAnalyticsCache();
  }

  public Optional<Grid> get(String key) {
    return getGridClone(queryCache.get(key));
  }

  /**
   * Retrieves a Grid from the cache or computes it if not available, with special handling for
   * nested calls.
   *
   * <p>This method first checks if the Grid for the given DataQueryParams is already in the cache.
   * If found, it returns a clone of the cached Grid. If not found, it computes the Grid using the
   * provided function and caches the result, but only if this is a top-level call (not a nested
   * call within another getOrFetch operation).
   *
   * <p>The nested call detection prevents duplicate cache entries when one Grid computation
   * triggers another Grid computation with different parameters. Only the top-level call's result
   * is cached, while nested calls compute their results without caching them.
   *
   * <p>The TTL of the cached object is determined according to the configuration in {@link
   * org.hisp.dhis.analytics.cache.AnalyticsCacheSettings}, which supports both fixed and
   * progressive expiration strategies.
   *
   * <p>This method is thread-safe.
   *
   * @param params The DataQueryParams used as the cache key and computation input
   * @param function A function that computes a Grid based on the provided DataQueryParams
   * @return A clone of the Grid, either from cache or newly computed
   */
  public Grid getOrFetch(DataQueryParams params, Function<DataQueryParams, Grid> function) {
    String key = params.getKey();

    // First check if it's already cached
    Optional<Grid> cachedGrid = get(key);
    if (cachedGrid.isPresent()) {
      return getGridClone(cachedGrid.get());
    }

    // Get current nesting level and increment
    int currentLevel = nestingLevel.get();
    nestingLevel.set(currentLevel + 1);

    try {
      // Compute the grid
      Grid grid = function.apply(params);

      // Only add to cache if this is the top level call (level was 0)
      if (currentLevel == 0) {
        if (analyticsCacheSettings.isProgressiveCachingEnabled()) {
          put(
              params.getKey(),
              grid,
              analyticsCacheSettings.progressiveExpirationTimeOrDefault(params.getLatestEndDate()));
        } else {
          put(params.getKey(), grid, analyticsCacheSettings.fixedExpirationTimeOrDefault());
        }
      }

      return getGridClone(grid);
    } finally {
      // Restore previous nesting level
      nestingLevel.set(currentLevel);

      // Clean up ThreadLocal
      if (currentLevel == 0) {
        nestingLevel.remove();
      }
    }
  }

  /**
   * This method will cache the given Grid associated with the given DataQueryParams.
   *
   * <p>The TTL of the cached object will be set accordingly to the cache settings available at
   * {@link AnalyticsCacheSettings}.
   *
   * @param params the DataQueryParams.
   * @param grid the associated Grid.
   */
  public void put(DataQueryParams params, Grid grid) {
    if (analyticsCacheSettings.isProgressiveCachingEnabled()) {
      // Uses the progressive TTL
      put(
          params.getKey(),
          grid,
          analyticsCacheSettings.progressiveExpirationTimeOrDefault(params.getLatestEndDate()));
    } else {
      // Respects the fixed (predefined) caching TTL
      put(params.getKey(), grid, analyticsCacheSettings.fixedExpirationTimeOrDefault());
    }
  }

  /**
   * Will cache the given key/Grid pair respecting the TTL provided through the parameter
   * "ttlInSeconds".
   *
   * @param key the cache key associate with the Grid.
   * @param grid the Grid object to be cached.
   * @param ttlInSeconds the time to live (expiration time) in seconds.
   */
  public void put(String key, Grid grid, long ttlInSeconds) {
    queryCache.put(key, getGridClone(grid), ttlInSeconds);
  }

  /** Clears the current cache by removing all existing entries. */
  public void invalidateAll() {
    queryCache.invalidateAll();

    log.info("Analytics cache cleared");
  }

  public boolean isEnabled() {
    return analyticsCacheSettings.isCachingEnabled();
  }

  private Grid getGridClone(Grid grid) {
    if (grid != null) {
      return SerializationUtils.clone(grid);
    }

    return null;
  }

  private Optional<Grid> getGridClone(Optional<Grid> grid) {
    return grid.map(SerializationUtils::clone);
  }
}
