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
 * Service for managing ETag versions used for conditional HTTP caching. This service provides fast
 * version lookups for generating ETags, enabling efficient cache invalidation without expensive
 * database queries.
 *
 * <p>The version is incremented when data changes are detected via Redis pub/sub cache invalidation
 * messages, ensuring that cached responses are invalidated across all DHIS2 instances in a cluster.
 *
 * @author Morten Svanæs
 */
public interface ETagVersionService {

  /**
   * Gets the current ETag version for a user. This is a fast O(1) lookup designed to be called on
   * every request.
   *
   * @param userUid the UID of the user
   * @return the current version number, or 0 if no version exists
   */
  long getVersion(@Nonnull String userUid);

  /**
   * Increments the ETag version for a user. This should be called when data relevant to the user
   * has changed.
   *
   * @param userUid the UID of the user
   * @return the new version number after increment
   */
  long incrementVersion(@Nonnull String userUid);

  /**
   * Increments the global ETag version that applies to all users. This should be called when data
   * that affects all users has changed (e.g., organisation unit structure changes).
   *
   * @return the new global version number after increment
   */
  long incrementGlobalVersion();

  /**
   * Gets the current global ETag version that applies to all users.
   *
   * @return the current global version number, or 0 if no version exists
   */
  long getGlobalVersion();

  /**
   * Gets the current ETag version for a specific entity type. This provides more granular cache
   * invalidation - changes to one entity type don't invalidate caches for other entity types.
   *
   * @param entityType the entity class (e.g., OrganisationUnit.class, DataSet.class)
   * @return the current version number for that entity type, or 0 if no version exists
   */
  long getEntityTypeVersion(@Nonnull Class<?> entityType);

  /**
   * Increments the ETag version for a specific entity type. This should be called when data of that
   * entity type has changed. Only caches for that specific entity type will be invalidated.
   *
   * @param entityType the entity class (e.g., OrganisationUnit.class, DataSet.class)
   * @return the new version number after increment
   */
  long incrementEntityTypeVersion(@Nonnull Class<?> entityType);

  /**
   * Checks if the ETag caching feature is enabled.
   *
   * @return true if ETag caching is enabled, false otherwise
   */
  boolean isEnabled();

  /**
   * Gets the TTL window in minutes for ETag caching. This is the maximum time a cached response can
   * be considered valid without checking for data changes.
   *
   * @return the TTL window in minutes
   */
  int getTtlMinutes();
}
