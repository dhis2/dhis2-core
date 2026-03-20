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
import org.hisp.dhis.config.sqlobserver.HibernateTableEntityRegistry.TableInfo;
import org.hisp.dhis.dml.DmlEvent;
import org.hisp.dhis.dml.DmlObservedEvent;
import org.hisp.dhis.dml.DmlOrigin;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Intercepts all DML statements (INSERT/UPDATE/DELETE) at the JDBC level via datasource-proxy.
 * Accumulates events per connection and publishes them as a batch on transaction commit. Discards
 * on rollback.
 *
 * <p>Implements both {@link QueryExecutionListener} (for SQL interception) and {@link
 * MethodExecutionListener} (for commit/rollback detection).
 */
@Slf4j
public class DmlObserverListener
    implements QueryExecutionListener,
        MethodExecutionListener,
        ApplicationListener<ContextRefreshedEvent> {

  private final HibernateTableEntityRegistry registry;
  private final ApplicationEventPublisher eventPublisher;

  // Pre-built counters (null when meterRegistry is null)
  private final Counter statementsObserved;
  private final Counter statementsSkippedNonDml;
  private final Counter statementsSkippedExcluded;
  private final Counter batchesPublished;
  private final Counter batchesDiscarded;
  private final Counter staleSweeps;
  private final DistributionSummary batchSize;

  /**
   * Accumulated DML events and origin context for one connection. {@code seenTableOps} deduplicates
   * by table+operation so a bulk import produces at most one event per table/operation combo,
   * bounding memory by distinct tables touched rather than row count.
   */
  record PendingBatch(
      List<DmlEvent> events, Set<String> seenTableOps, DmlOrigin origin, long createdAtNanos) {}

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
      HibernateTableEntityRegistry registry,
      ApplicationEventPublisher eventPublisher,
      MeterRegistry meterRegistry) {
    this.registry = registry;
    this.eventPublisher = eventPublisher;

    if (meterRegistry != null) {
      statementsObserved =
          Counter.builder("dhis2_dml_observer_statements_total")
              .tag("result", "observed")
              .register(meterRegistry);
      statementsSkippedNonDml =
          Counter.builder("dhis2_dml_observer_statements_total")
              .tag("result", "skipped_non_dml")
              .register(meterRegistry);
      statementsSkippedExcluded =
          Counter.builder("dhis2_dml_observer_statements_total")
              .tag("result", "skipped_excluded")
              .register(meterRegistry);
      batchesPublished =
          Counter.builder("dhis2_dml_observer_batches_published_total").register(meterRegistry);
      batchesDiscarded =
          Counter.builder("dhis2_dml_observer_batches_discarded_total").register(meterRegistry);
      staleSweeps =
          Counter.builder("dhis2_dml_observer_stale_sweeps_total").register(meterRegistry);
      batchSize =
          DistributionSummary.builder("dhis2_dml_observer_batch_size").register(meterRegistry);
    } else {
      statementsObserved = null;
      statementsSkippedNonDml = null;
      statementsSkippedExcluded = null;
      batchesPublished = null;
      batchesDiscarded = null;
      staleSweeps = null;
      batchSize = null;
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

      // For auto-commit connections, publish immediately
      if (autoCommit) {
        log.debug(
            "DML observed (auto-commit): op={}, table={}, entity={}",
            event.operation(),
            event.tableName(),
            event.entityClassName());
        eventPublisher.publishEvent(
            new DmlObservedEvent(this, List.of(event), DmlOrigin.fromMdc()));
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
                          DmlOrigin.fromMdc(),
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
      // close() returns the connection to the pool; publish any remaining events rather than
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
      log.debug("Publishing {} DML events for connection {}", batch.events().size(), connectionId);
      if (batchesPublished != null) batchesPublished.increment();
      if (batchSize != null) batchSize.record(batch.events().size());
      try {
        eventPublisher.publishEvent(new DmlObservedEvent(this, batch.events(), batch.origin()));
      } catch (Exception e) {
        log.warn("Failed to publish DmlObservedEvent", e);
      }
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
