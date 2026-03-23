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

import static org.hisp.dhis.dml.DmlETagMetrics.ETAG_BRIDGE_EVENTS;
import static org.hisp.dhis.dml.DmlETagMetrics.ETAG_VERSION_BUMPS;
import static org.hisp.dhis.dml.DmlETagMetrics.STATUS_PROCESSED;
import static org.hisp.dhis.dml.DmlETagMetrics.STATUS_SKIPPED_NULL;
import static org.hisp.dhis.dml.DmlETagMetrics.STATUS_SKIPPED_UNTRACKED;
import static org.hisp.dhis.dml.DmlETagMetrics.TAG_ENTITY_TYPE;
import static org.hisp.dhis.dml.DmlETagMetrics.TAG_STATUS;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.cache.ETagObservedEntityTypes;
import org.hisp.dhis.cache.ETagService;
import org.hisp.dhis.dml.DmlEvent;
import org.hisp.dhis.dml.DmlObservedEvent;
import org.hisp.dhis.external.conf.ApiCacheEnabledCondition;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Bridges DML observer events to the ETag version service. Increments entity-type ETag versions for
 * observed types on {@link DmlObservedEvent}s, enabling conditional HTTP caching.
 *
 * <p>Only observed entity types (metadata plus a small non-metadata allowlist) trigger version
 * bumps. High-volume data writes (DataValue, Event, etc.) are excluded. Within a single event
 * batch, each entity type is bumped at most once.
 *
 * @author Morten Svanæs
 */
@Slf4j
@Component
@Conditional(ApiCacheEnabledCondition.class)
public class DmlCacheInvalidationBridge {

  private static final Class<?> UNRESOLVABLE = Void.class;

  // Metrics counters
  private final Counter eventsProcessed;
  private final Counter eventsSkippedUntracked;
  private final Counter eventsSkippedNull;

  private final ETagService eTagService;
  private final MeterRegistry meterRegistry;

  private final ConcurrentHashMap<String, Class<?>> classCache = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Counter> entityTypeBumpCounters =
      new ConcurrentHashMap<>();

  @Autowired
  public DmlCacheInvalidationBridge(
      ETagService eTagService,
      @Autowired(required = false) MeterRegistry meterRegistry,
      @Autowired(required = false) DhisConfigurationProvider config) {
    this.eTagService = eTagService;
    MeterRegistry effectiveRegistry =
        config != null && config.isEnabled(ConfigurationKey.MONITORING_CACHE_ETAG_ENABLED)
            ? meterRegistry
            : null;
    this.meterRegistry = effectiveRegistry;

    if (effectiveRegistry != null) {
      eventsProcessed =
          Counter.builder(ETAG_BRIDGE_EVENTS)
              .tag(TAG_STATUS, STATUS_PROCESSED)
              .register(effectiveRegistry);
      eventsSkippedUntracked =
          Counter.builder(ETAG_BRIDGE_EVENTS)
              .tag(TAG_STATUS, STATUS_SKIPPED_UNTRACKED)
              .register(effectiveRegistry);
      eventsSkippedNull =
          Counter.builder(ETAG_BRIDGE_EVENTS)
              .tag(TAG_STATUS, STATUS_SKIPPED_NULL)
              .register(effectiveRegistry);
    } else {
      eventsProcessed = null;
      eventsSkippedUntracked = null;
      eventsSkippedNull = null;
    }
  }

  @Async
  @EventListener
  public void onDmlObserved(DmlObservedEvent event) {
    try {
      Set<Class<?>> bumpedTypes = new HashSet<>();

      for (DmlEvent dmlEvent : event.getEvents()) {
        String entityClassName = dmlEvent.entityClassName();
        if (entityClassName == null) {
          if (eventsSkippedNull != null) eventsSkippedNull.increment();
          continue;
        }

        Class<?> entityClass = resolveClass(entityClassName);
        if (entityClass == null) {
          if (eventsSkippedNull != null) eventsSkippedNull.increment();
          continue;
        }

        if (!ETagObservedEntityTypes.isObservedType(entityClass)) {
          if (eventsSkippedUntracked != null) eventsSkippedUntracked.increment();
          continue;
        }

        // Deduplicate within batch: one version bump per entity type per transaction
        if (bumpedTypes.add(entityClass)) {
          eTagService.incrementEntityTypeVersion(entityClass);
          if (eventsProcessed != null) eventsProcessed.increment();
          if (meterRegistry != null) {
            entityTypeBumpCounters
                .computeIfAbsent(
                    entityClass.getSimpleName(),
                    name ->
                        Counter.builder(ETAG_VERSION_BUMPS)
                            .tag(TAG_ENTITY_TYPE, name)
                            .register(meterRegistry))
                .increment();
          }
          log.debug(
              "Bumped ETag version for metadata entity type: {}", entityClass.getSimpleName());
        }
      }
    } catch (Exception e) {
      log.error("Failed to process DML observed event for cache invalidation", e);
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
