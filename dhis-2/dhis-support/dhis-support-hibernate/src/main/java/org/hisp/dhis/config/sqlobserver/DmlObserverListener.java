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
package org.hisp.dhis.config.sqlobserver;

import static org.hisp.dhis.dml.DmlETagMetrics.DML_OBSERVER_BATCHES_DISCARDED;
import static org.hisp.dhis.dml.DmlETagMetrics.DML_OBSERVER_BATCHES_PUBLISHED;
import static org.hisp.dhis.dml.DmlETagMetrics.DML_OBSERVER_BATCH_SIZE;
import static org.hisp.dhis.dml.DmlETagMetrics.DML_OBSERVER_STALE_SWEEPS;
import static org.hisp.dhis.dml.DmlETagMetrics.DML_OBSERVER_STATEMENTS;
import static org.hisp.dhis.dml.DmlETagMetrics.ETAG_BRIDGE_EVENTS;
import static org.hisp.dhis.dml.DmlETagMetrics.ETAG_VERSION_BUMPS;
import static org.hisp.dhis.dml.DmlETagMetrics.RESULT_OBSERVED;
import static org.hisp.dhis.dml.DmlETagMetrics.RESULT_SKIPPED_EXCLUDED;
import static org.hisp.dhis.dml.DmlETagMetrics.RESULT_SKIPPED_NON_DML;
import static org.hisp.dhis.dml.DmlETagMetrics.STATUS_PROCESSED;
import static org.hisp.dhis.dml.DmlETagMetrics.STATUS_SKIPPED_NULL;
import static org.hisp.dhis.dml.DmlETagMetrics.STATUS_SKIPPED_UNTRACKED;
import static org.hisp.dhis.dml.DmlETagMetrics.TAG_ENTITY_TYPE;
import static org.hisp.dhis.dml.DmlETagMetrics.TAG_RESULT;
import static org.hisp.dhis.dml.DmlETagMetrics.TAG_STATUS;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import net.ttddyy.dsproxy.listener.MethodExecutionListener;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import org.hisp.dhis.cache.ETagObservedEntityTypes;
import org.hisp.dhis.cache.ETagService;
import org.hisp.dhis.config.sqlobserver.HibernateTableEntityRegistry.TableInfo;
import org.hisp.dhis.dml.DmlEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Intercepts all DML statements (INSERT/UPDATE/DELETE) at the JDBC level via datasource-proxy.
 * Accumulates events per connection and bumps ETag versions on transaction commit. Discards on
 * rollback.
 *
 * <p>Implements both {@link QueryExecutionListener} (for SQL interception) and {@link
 * MethodExecutionListener} (for commit/rollback detection).
 *
 * @author Morten Svanæs
 */
@Slf4j
public class DmlObserverListener
    implements QueryExecutionListener,
        MethodExecutionListener,
        ApplicationListener<ContextRefreshedEvent> {

  private static final Class<?> UNRESOLVABLE = Void.class;

  // DML Observer metrics
  private final Counter statementsObserved;
  private final Counter statementsSkippedNonDml;
  private final Counter statementsSkippedExcluded;
  private final Counter batchesPublished;
  private final Counter batchesDiscarded;
  private final Counter staleSweeps;
  private final DistributionSummary batchSize;

  // ETag version bump metrics
  private final Counter eventsProcessed;
  private final Counter eventsSkippedUntracked;
  private final Counter eventsSkippedNull;

  private final HibernateTableEntityRegistry registry;
  private final ETagService eTagService;
  private final MeterRegistry meterRegistry;

  private final ConcurrentHashMap<String, Class<?>> classCache = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Counter> entityTypeBumpCounters =
      new ConcurrentHashMap<>();

  /**
   * Accumulated DML events for one connection. {@code seenTableOps} deduplicates by table+operation
   * so a bulk import produces at most one event per table/operation combo, bounding memory by
   * distinct tables touched rather than row count.
   */
  record PendingBatch(List<DmlEvent> events, Set<String> seenTableOps, long createdAtNanos) {}

  /** Max age for pending batches before they are evicted as stale (5 minutes). */
  private static final long STALE_BATCH_NANOS = 5 * 60 * 1_000_000_000L;

  /** Sweep stale entries every N afterQuery calls to avoid checking on every single statement. */
  private static final long SWEEP_INTERVAL = 1000;

  /**
   * Accumulated DML batches keyed by connection identity hash code (as String). Using identity hash
   * to avoid calling connection.toString() on pooled connections.
   */
  private final ConcurrentHashMap<String, PendingBatch> pendingBatches = new ConcurrentHashMap<>();

  private final AtomicLong queryCounter = new AtomicLong(0);
  private volatile boolean observingEnabled;

  public DmlObserverListener(
      HibernateTableEntityRegistry registry, ETagService eTagService, MeterRegistry meterRegistry) {
    this.registry = registry;
    this.eTagService = eTagService;
    this.meterRegistry = meterRegistry;

    if (meterRegistry != null) {
      statementsObserved =
          Counter.builder(DML_OBSERVER_STATEMENTS)
              .tag(TAG_RESULT, RESULT_OBSERVED)
              .register(meterRegistry);
      statementsSkippedNonDml =
          Counter.builder(DML_OBSERVER_STATEMENTS)
              .tag(TAG_RESULT, RESULT_SKIPPED_NON_DML)
              .register(meterRegistry);
      statementsSkippedExcluded =
          Counter.builder(DML_OBSERVER_STATEMENTS)
              .tag(TAG_RESULT, RESULT_SKIPPED_EXCLUDED)
              .register(meterRegistry);
      batchesPublished = Counter.builder(DML_OBSERVER_BATCHES_PUBLISHED).register(meterRegistry);
      batchesDiscarded = Counter.builder(DML_OBSERVER_BATCHES_DISCARDED).register(meterRegistry);
      staleSweeps = Counter.builder(DML_OBSERVER_STALE_SWEEPS).register(meterRegistry);
      batchSize = DistributionSummary.builder(DML_OBSERVER_BATCH_SIZE).register(meterRegistry);

      eventsProcessed =
          Counter.builder(ETAG_BRIDGE_EVENTS)
              .tag(TAG_STATUS, STATUS_PROCESSED)
              .register(meterRegistry);
      eventsSkippedUntracked =
          Counter.builder(ETAG_BRIDGE_EVENTS)
              .tag(TAG_STATUS, STATUS_SKIPPED_UNTRACKED)
              .register(meterRegistry);
      eventsSkippedNull =
          Counter.builder(ETAG_BRIDGE_EVENTS)
              .tag(TAG_STATUS, STATUS_SKIPPED_NULL)
              .register(meterRegistry);
    } else {
      statementsObserved = null;
      statementsSkippedNonDml = null;
      statementsSkippedExcluded = null;
      batchesPublished = null;
      batchesDiscarded = null;
      staleSweeps = null;
      batchSize = null;
      eventsProcessed = null;
      eventsSkippedUntracked = null;
      eventsSkippedNull = null;
    }
  }

  @Override
  public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
    // No-op
  }

  @Override
  public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
    if (!observingEnabled || !execInfo.isSuccess()) {
      return;
    }

    String connectionId = getConnectionId(execInfo);
    // Cache auto-commit state once per call
    boolean autoCommit = isAutoCommit(execInfo);

    for (QueryInfo queryInfo : queryInfoList) {
      String query = queryInfo.getQuery();

      // Fast-path: skip non-DML
      if (!DmlSqlParser.isPossibleDml(query)) {
        if (statementsSkippedNonDml != null) statementsSkippedNonDml.increment();
        continue;
      }

      Optional<DmlSqlParser.DmlFastResult> parsed = DmlSqlParser.parseFast(query);
      if (parsed.isEmpty()) {
        if (statementsSkippedNonDml != null) statementsSkippedNonDml.increment();
        continue;
      }

      DmlSqlParser.DmlFastResult result = parsed.get();

      // Check exclusions
      if (DmlObserverExclusions.isExcluded(result.tableName())) {
        if (statementsSkippedExcluded != null) statementsSkippedExcluded.increment();
        continue;
      }

      // Map table → entity
      TableInfo tableInfo = registry.getTableInfo(result.tableName());
      String entityClassName = tableInfo != null ? tableInfo.entityClass().getName() : null;

      // Dedup key: "tablename:OPERATION" — one event per table/operation combo per transaction.
      String dedupeKey = result.tableName() + ":" + result.operation();

      DmlEvent event =
          new DmlEvent(
              result.operation(), result.tableName(), entityClassName, Instant.now(), connectionId);

      if (statementsObserved != null) statementsObserved.increment();

      // For auto-commit connections, bump ETag versions immediately
      if (autoCommit) {
        log.debug(
            "DML observed (auto-commit): op={}, table={}, entity={}",
            event.operation(),
            event.tableName(),
            event.entityClassName());
        bumpETagVersions(List.of(event));
      } else {
        // compute() holds the segment lock, preventing a race between
        // publishAndClear() and batch creation/event addition.
        pendingBatches.compute(
            connectionId,
            (k, existing) -> {
              PendingBatch batch =
                  existing != null
                      ? existing
                      : new PendingBatch(
                          Collections.synchronizedList(new ArrayList<>()),
                          Collections.synchronizedSet(new HashSet<>()),
                          System.nanoTime());
              // Only add if we haven't seen this table+operation combo yet in this transaction
              if (batch.seenTableOps().add(dedupeKey)) {
                batch.events().add(event);
                log.debug(
                    "DML observed: op={}, table={}, entity={}",
                    event.operation(),
                    event.tableName(),
                    event.entityClassName());
              }
              return batch;
            });
      }
    }

    // Periodically sweep stale pending batches to prevent memory leaks from abandoned connections
    if (queryCounter.incrementAndGet() % SWEEP_INTERVAL == 0) {
      sweepStaleBatches();
    }
  }

  @Override
  public void beforeMethod(MethodExecutionContext context) {
    // No-op
  }

  @Override
  public void afterMethod(MethodExecutionContext context) {
    if (!observingEnabled) {
      return;
    }

    Method method = context.getMethod();
    String methodName = method.getName();
    Object target = context.getTarget();

    if (!(target instanceof Connection)) {
      return;
    }

    String connectionId = getConnectionIdFromContext(context);
    if (connectionId == null) {
      return;
    }

    switch (methodName) {
      case "commit" -> publishAndClear(connectionId);
      case "rollback" -> discardAndClear(connectionId);
      // close() returns the connection to the pool; process any remaining events rather than
      // silently discarding them.
      case "close" -> publishAndClear(connectionId);
      default -> {
        // No action for other methods
      }
    }
  }

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    if (observingEnabled) {
      return;
    }

    registry.initialize();
    observingEnabled = true;
    log.info("DML observer activated after ContextRefreshedEvent");
  }

  private void publishAndClear(String connectionId) {
    PendingBatch batch = pendingBatches.remove(connectionId);
    if (batch != null && !batch.events().isEmpty()) {
      log.debug("Processing {} DML events for connection {}", batch.events().size(), connectionId);
      if (batchesPublished != null) batchesPublished.increment();
      if (batchSize != null) batchSize.record(batch.events().size());
      bumpETagVersions(batch.events());
    }
  }

  private void discardAndClear(String connectionId) {
    PendingBatch batch = pendingBatches.remove(connectionId);
    if (batch != null && !batch.events().isEmpty()) {
      if (batchesDiscarded != null) batchesDiscarded.increment();
      log.debug(
          "Discarded {} DML events for connection {} (rollback/close)",
          batch.events().size(),
          connectionId);
    }
  }

  /**
   * Resolves entity classes from DML events, filters to observed types, deduplicates by entity
   * type, and bumps ETag versions. Exceptions are caught to prevent failures from propagating to
   * the JDBC layer.
   */
  private void bumpETagVersions(List<DmlEvent> events) {
    try {
      Set<Class<?>> bumpedTypes = new HashSet<>();
      for (DmlEvent event : events) {
        String entityClassName = event.entityClassName();
        if (entityClassName == null) {
          if (eventsSkippedNull != null) eventsSkippedNull.increment();
          continue;
        }

        Class<?> entityClass = resolveEntityClass(entityClassName);
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
          log.debug("Bumped ETag version for entity type: {}", entityClass.getSimpleName());
        }
      }
    } catch (Exception e) {
      log.error("Failed to bump ETag versions for DML events", e);
    }
  }

  private Class<?> resolveEntityClass(String className) {
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

  /**
   * Removes pending batches that have been waiting longer than {@link #STALE_BATCH_NANOS}. This
   * prevents unbounded memory growth from abandoned connections (e.g., network timeouts, pool
   * evictions) where neither commit, rollback, nor close was intercepted.
   */
  private void sweepStaleBatches() {
    long now = System.nanoTime();
    int swept = 0;
    for (var entry : pendingBatches.entrySet()) {
      if (now - entry.getValue().createdAtNanos() > STALE_BATCH_NANOS) {
        pendingBatches.remove(entry.getKey());
        swept++;
      }
    }
    if (swept > 0) {
      if (staleSweeps != null) staleSweeps.increment();
      log.warn("Swept {} stale pending DML batches (abandoned connections)", swept);
    }
  }

  private String getConnectionId(ExecutionInfo execInfo) {
    return String.valueOf(execInfo.getConnectionId());
  }

  private String getConnectionIdFromContext(MethodExecutionContext context) {
    if (context.getConnectionInfo() != null) {
      return String.valueOf(context.getConnectionInfo().getConnectionId());
    }
    // No connection info available — cannot match pending batch.
    log.debug("No ConnectionInfo on MethodExecutionContext; cannot match pending batch");
    return null;
  }

  private boolean isAutoCommit(ExecutionInfo execInfo) {
    try {
      if (execInfo.getStatement() == null) {
        // No statement reference — assume auto-commit to avoid orphaning events.
        return true;
      }
      Connection conn = execInfo.getStatement().getConnection();
      return conn.getAutoCommit();
    } catch (Exception e) {
      // Default to auto-commit to avoid accumulating events that may never be committed.
      return true;
    }
  }
}
