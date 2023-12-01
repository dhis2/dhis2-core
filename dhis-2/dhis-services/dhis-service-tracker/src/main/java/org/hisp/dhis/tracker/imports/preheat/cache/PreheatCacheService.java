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
package org.hisp.dhis.tracker.imports.preheat.cache;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.event.ApplicationCacheClearedEvent;

/**
 * A DHIS2 metadata cache implementation to reduce db lookups during pre-heat
 *
 * @author Luciano Fiandesio
 */
public interface PreheatCacheService {
  /**
   * Fetches an object from the pre-heat cache.
   *
   * @param cacheKey the full class name of the object being cached
   * @param id the identifier of the object to retrieve
   */
  Optional<IdentifiableObject> get(String cacheKey, String id);

  /**
   * Returns the metadata value cached in {@code cacheKey} mapped to {@code id}, obtaining that
   * value from the {@code mappingFunction} if necessary. This method provides a simple substitute
   * for the conventional "if cached, return; otherwise create, cache and return" pattern. If
   * metadata value is null, the given mapping function is evaluated and inserted into this cache
   * unless {@code null}.
   *
   * @param cacheKey the cacheKey for retrieving the cache
   * @param id the identifier for retrieving the metadata value from the cache
   * @param mappingFunction the function to compute a value.
   * @return an optional containing current (existing or computed) value, or Optional.empty() if the
   *     computed value is null
   * @throws IllegalArgumentException if the specified mappingFunction is null
   */
  Optional<IdentifiableObject> get(
      String cacheKey,
      String id,
      BiFunction<String, String, Optional<IdentifiableObject>> mappingFunction,
      int cacheTTL,
      long capacity);

  /**
   * Check whether a class type is part of the cache
   *
   * @param cacheKey the full class name of a metadata object
   */
  boolean hasKey(String cacheKey);

  /**
   * Fetch all the cached entries for the given class type key
   *
   * @param cacheKey the full class name of a metadata object
   */
  List<IdentifiableObject> getAll(String cacheKey);

  /**
   * Adds an object to the pre-heat cache.
   *
   * @param cacheKey the full class name of the object being cached
   * @param id the identifier of the object being cached, used as cache key
   * @param object The object being cached
   * @param cacheTTL The amount of **minutes**
   * @param capacity The maximum number of entries hold by the cache.
   */
  void put(String cacheKey, String id, IdentifiableObject object, int cacheTTL, long capacity);

  /** Invalidates all caches. */
  void invalidateCache();

  /**
   * Event handler for {@link ApplicationCacheClearedEvent}.
   *
   * @param event the {@link ApplicationCacheClearedEvent}.
   */
  void handleApplicationCachesCleared(ApplicationCacheClearedEvent event);
}
