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

import javax.annotation.Nonnull;

/**
 * Manages ETag version counters used for conditional HTTP caching. Versions are incremented when
 * data changes are detected (e.g., via the DML observer layer), invalidating stale cached
 * responses.
 *
 * @author Morten Svanæs
 */
public interface ETagService {

  /** Returns the all-cache version. Incrementing this invalidates every API ETag family at once. */
  long getAllCacheVersion();

  /**
   * Increments the all-cache version. Use for deliberate broad cache invalidation, not for ordinary
   * entity-type changes.
   *
   * @return the new version number after increment
   */
  long incrementAllCacheVersion();

  /**
   * Returns the ETag version for the given entity type.
   *
   * @param entityType the entity class (e.g., OrganisationUnit.class)
   * @return the current version number, or 0 if none exists
   */
  long getEntityTypeVersion(@Nonnull Class<?> entityType);

  /**
   * Increments the ETag version for the given entity type.
   *
   * @param entityType the entity class (e.g., OrganisationUnit.class)
   * @return the new version number after increment
   */
  long incrementEntityTypeVersion(@Nonnull Class<?> entityType);

  /**
   * Returns the version for a named (non-entity) cache key. Named keys are used for endpoints whose
   * data is not tied to a single JPA entity type, such as the app menu or schema list.
   *
   * @param key the version key name (e.g. {@code "installedApps"})
   * @return the current version number, or 0 if none exists
   */
  long getNamedVersion(@Nonnull String key);

  /**
   * Increments the version for a named (non-entity) cache key. Call this when the underlying data
   * changes outside the DML observer pipeline (e.g. app install/uninstall).
   *
   * @param key the version key name
   * @return the new version number after increment
   */
  long incrementNamedVersion(@Nonnull String key);

  /** Returns {@code true} if ETag caching is enabled. */
  boolean isEnabled();

  /**
   * Returns the TTL window in minutes. Cached responses are revalidated at most this often.
   *
   * @return the TTL window in minutes
   */
  int getTtlMinutes();

  /**
   * Returns the stale-while-revalidate duration in seconds. When positive, the browser can serve
   * stale cached responses instantly while revalidating in the background. When 0, synchronous
   * revalidation is enforced.
   *
   * @return the stale-while-revalidate window in seconds
   */
  int getStaleWhileRevalidateSeconds();
}
