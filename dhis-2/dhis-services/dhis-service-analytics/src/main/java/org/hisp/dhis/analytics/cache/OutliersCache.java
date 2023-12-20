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

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.outlier.data.Outlier;
import org.hisp.dhis.analytics.outlier.data.OutlierRequest;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.springframework.stereotype.Component;

/**
 * This is a wrapper class responsible for keeping and isolating all cache definitions related to
 * the analytics outliers.
 */
@Slf4j
@Component
public class OutliersCache {
  private final AnalyticsCacheSettings analyticsCacheSettings;

  private Cache<List<Outlier>> queryCache;

  /**
   * Default constructor. Note that a default expiration time is set, as the TTL will always be
   * overwritten during cache put operations.
   */
  public OutliersCache(CacheProvider cacheProvider, AnalyticsCacheSettings analyticsCacheSettings) {
    this.analyticsCacheSettings = analyticsCacheSettings;
    this.queryCache = cacheProvider.createOutliersCache();
  }

  public Optional<List<Outlier>> get(String key) {
    return queryCache.get(key);
  }

  /**
   * This method tries to retrieve, from the cache, the list of outliers related to the given {@link
   * OutlierRequest}. If outliers are not found in the cache, they will be fetched by the function
   * provided. In this case, the outliers found will be cached, so the next consumers can hit the
   * cache only.
   *
   * <p>f The TTL of the cached object will be set accordingly to the cache settings available at
   * {@link AnalyticsCacheSettings}.
   *
   * @param params the current {@link OutlierRequest}.
   * @param function that fetches a grid based on the given {@link OutlierRequest}.
   * @return the cached or fetched list of {@link Outlier}.
   * @throws NullPointerException if any argument is null.
   */
  public List<Outlier> getOrFetch(
      OutlierRequest params, Function<OutlierRequest, List<Outlier>> function) {
    Optional<List<Outlier>> cachedOutliers = get(params.getQueryKey());

    if (cachedOutliers.isPresent()) {
      return cachedOutliers.get();
    } else {
      List<Outlier> outliers = function.apply(params);

      put(params, outliers);

      return outliers;
    }
  }

  /**
   * This method will cache the given list of {@link Outlier} associated with the given {@link
   * OutlierRequest}.
   *
   * <p>The TTL of the cached object will be set accordingly to the cache settings available at
   * {@link AnalyticsCacheSettings}.
   *
   * @param outlierRequest the {@link OutlierRequest}.
   * @param outliers the list of {@link Outlier} to cache.
   * @throws NullPointerException if any argument is null.
   */
  public void put(OutlierRequest outlierRequest, List<Outlier> outliers) {
    // Respects the fixed (predefined) caching TTL.
    put(
        outlierRequest.getQueryKey(),
        outliers,
        analyticsCacheSettings.fixedExpirationTimeOrDefault());
  }

  /**
   * Will cache the given key/outliers pair respecting the TTL provided through the parameter
   * "ttlInSeconds".
   *
   * @param key the cache key associate with the list of {@link Outlier}.
   * @param outliers the object to be cached.
   * @param ttlInSeconds the time to live (expiration time) in seconds.
   */
  public void put(String key, List<Outlier> outliers, long ttlInSeconds) {
    queryCache.put(key, outliers, ttlInSeconds);
  }

  /** Clears the current cache by removing all existing entries. */
  public void invalidateAll() {
    queryCache.invalidateAll();

    log.info("Analytics outliers cache cleared");
  }

  public boolean isEnabled() {
    return analyticsCacheSettings.isCachingEnabled();
  }
}
