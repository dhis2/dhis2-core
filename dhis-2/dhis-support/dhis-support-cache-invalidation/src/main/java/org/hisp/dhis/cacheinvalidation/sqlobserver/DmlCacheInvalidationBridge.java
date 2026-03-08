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
package org.hisp.dhis.cacheinvalidation.sqlobserver;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.audit.DmlEvent;
import org.hisp.dhis.audit.DmlObservedEvent;
import org.hisp.dhis.cache.ETagVersionService;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.configuration.Configuration;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Bridges DML observer events to the ETag version service. Listens for {@link DmlObservedEvent}s
 * and increments entity-type ETag versions for metadata changes, enabling HTTP conditional caching
 * (ETag/If-None-Match) to detect stale responses.
 *
 * <p>Only metadata entity types (those implementing {@link MetadataObject}) trigger version bumps.
 * High-volume data writes (DataValue, Event, etc.) are intentionally excluded to avoid unnecessary
 * cache churn. Within a single batch of events, each entity type is bumped at most once.
 *
 * @author Morten Svanæs
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Conditional(DmlCacheInvalidationCondition.class)
public class DmlCacheInvalidationBridge {

  private static final Class<?> UNRESOLVABLE = Void.class;

  /**
   * Non-MetadataObject entity types that should still trigger version bumps because they back
   * composite API endpoints. For example, {@link Configuration} is not a MetadataObject but the
   * {@code /api/configuration} endpoint needs ETag invalidation when it changes.
   */
  private static final Set<Class<?>> ADDITIONAL_TRACKED_TYPES = Set.of(Configuration.class);

  private final ETagVersionService eTagVersionService;

  private final ConcurrentHashMap<String, Class<?>> classCache = new ConcurrentHashMap<>();

  @EventListener
  public void onDmlObserved(DmlObservedEvent event) {
    Set<Class<?>> bumpedTypes = new HashSet<>();

    for (DmlEvent dmlEvent : event.getEvents()) {
      String entityClassName = dmlEvent.getEntityClassName();
      if (entityClassName == null) {
        continue;
      }

      Class<?> entityClass = resolveClass(entityClassName);
      if (entityClass == null) {
        continue;
      }

      // Only metadata changes and explicitly tracked non-metadata types
      // should invalidate API caches. Data writes (DataValue, Event, etc.)
      // are high-volume and should not churn cache versions.
      if (!MetadataObject.class.isAssignableFrom(entityClass)
          && !ADDITIONAL_TRACKED_TYPES.contains(entityClass)) {
        continue;
      }

      // Deduplicate within batch: one version bump per entity type per transaction
      if (bumpedTypes.add(entityClass)) {
        eTagVersionService.incrementEntityTypeVersion(entityClass);
        log.debug("Bumped ETag version for metadata entity type: {}", entityClass.getSimpleName());
      }
    }
  }

  private Class<?> resolveClass(String className) {
    Class<?> cached =
        classCache.computeIfAbsent(
            className,
            name -> {
              try {
                return Class.forName(name);
              } catch (ClassNotFoundException e) {
                log.warn("Could not resolve entity class: {}", name);
                return UNRESOLVABLE;
              }
            });
    return cached == UNRESOLVABLE ? null : cached;
  }
}
