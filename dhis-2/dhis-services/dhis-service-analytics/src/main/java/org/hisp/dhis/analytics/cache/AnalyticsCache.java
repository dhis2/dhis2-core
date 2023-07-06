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

  private Cache<Grid> queryCache;

  /**
   * Default constructor. Note that a default expiration time is set, as as the TTL will always be
   * overwritten during cache put operations.
   */
  public AnalyticsCache(
      final CacheProvider cacheProvider, final AnalyticsCacheSettings analyticsCacheSettings) {
    checkNotNull(cacheProvider);
    checkNotNull(analyticsCacheSettings);

    this.analyticsCacheSettings = analyticsCacheSettings;
    this.queryCache = cacheProvider.createAnalyticsCache();
  }

  public Optional<Grid> get(final String key) {
    return getGridClone(queryCache.get(key));
  }

  /**
   * This method tries to retrieve, from the cache, the Grid related to the given DataQueryParams.
   * If the Grid is not found in the cache, the Grid will be fetched by the function provided. In
   * this case, the fetched Grid will be cached, so the next consumers can hit the cache only.
   *
   * <p>f The TTL of the cached object will be set accordingly to the cache settings available at
   * {@link org.hisp.dhis.analytics.cache.AnalyticsCacheSettings}.
   *
   * @param params the current DataQueryParams.
   * @param function that fetches a grid based on the given DataQueryParams.
   * @return the cached or fetched Grid.
   */
  public Grid getOrFetch(
      final DataQueryParams params, final Function<DataQueryParams, Grid> function) {
    final Optional<Grid> cachedGrid = get(params.getKey());

    if (cachedGrid.isPresent()) {
      return getGridClone(cachedGrid.get());
    } else {
      final Grid grid = function.apply(params);

      put(params, grid);

      return getGridClone(grid);
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
  public void put(final DataQueryParams params, final Grid grid) {
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
  public void put(final String key, final Grid grid, final long ttlInSeconds) {
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
