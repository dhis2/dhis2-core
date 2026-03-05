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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import net.ttddyy.dsproxy.listener.MethodExecutionListener;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.proxy.ParameterSetOperation;
import org.hisp.dhis.audit.DmlEvent;
import org.hisp.dhis.audit.DmlObservedEvent;
import org.hisp.dhis.audit.DmlOrigin;
import org.hisp.dhis.config.sqlobserver.DmlSqlParser.DmlParseResult;
import org.hisp.dhis.config.sqlobserver.HibernateTableEntityRegistry.TableInfo;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Intercepts all DML statements (INSERT/UPDATE/DELETE) at the JDBC level via datasource-proxy.
 * Accumulates events per connection and publishes them as a batch on transaction commit. Discards
 * on rollback.
 *
 * <p>Implements both {@link QueryExecutionListener} (for SQL interception) and {@link
 * MethodExecutionListener} (for commit/rollback detection).
 */
@Slf4j
public class DmlObserverListener implements QueryExecutionListener, MethodExecutionListener {

  private final HibernateTableEntityRegistry registry;
  private final ApplicationEventPublisher eventPublisher;

  /** Holder for accumulated DML events and the origin context captured on first DML. */
  record PendingBatch(List<DmlEvent> events, DmlOrigin origin) {}

  /**
   * Accumulated DML batches keyed by connection identity hash code (as String). Using identity hash
   * to avoid calling connection.toString() on pooled connections.
   */
  private final ConcurrentHashMap<String, PendingBatch> pendingBatches =
      new ConcurrentHashMap<>();

  public DmlObserverListener(
      HibernateTableEntityRegistry registry, ApplicationEventPublisher eventPublisher) {
    this.registry = registry;
    this.eventPublisher = eventPublisher;
  }

  // ── QueryExecutionListener ──

  @Override
  public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
    // No-op
  }

  @Override
  public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
    if (!execInfo.isSuccess()) {
      return;
    }

    String connectionId = getConnectionId(execInfo);

    for (QueryInfo queryInfo : queryInfoList) {
      String query = queryInfo.getQuery();

      // Fast-path: skip non-DML
      if (!DmlSqlParser.isPossibleDml(query)) {
        continue;
      }

      Optional<DmlParseResult> parsed = DmlSqlParser.parse(query);
      if (parsed.isEmpty()) {
        continue;
      }

      DmlParseResult result = parsed.get();

      // Check exclusions
      if (DmlObserverExclusions.isExcluded(result.getTableName())) {
        continue;
      }

      // Map table → entity
      TableInfo tableInfo = registry.getTableInfo(result.getTableName());
      String entityClassName = tableInfo != null ? tableInfo.getEntityClass().getName() : null;

      // Extract PK from parameters (best-effort)
      List<List<ParameterSetOperation>> parametersList = queryInfo.getParametersList();
      int batchSize = parametersList != null ? parametersList.size() : 1;

      for (int batchIdx = 0; batchIdx < Math.max(1, batchSize); batchIdx++) {
        Serializable entityId = extractPrimaryKey(result, tableInfo, parametersList, batchIdx);

        DmlEvent event =
            DmlEvent.builder()
                .operation(result.getOperation())
                .tableName(result.getTableName())
                .entityClassName(entityClassName)
                .entityId(entityId)
                .timestamp(Instant.now())
                .connectionId(connectionId)
                .build();

        log.debug(
            "DML observed: op={}, table={}, entity={}, pk={}",
            event.getOperation(),
            event.getTableName(),
            event.getEntityClassName(),
            event.getEntityId());

        // For auto-commit connections, publish immediately
        if (isAutoCommit(execInfo)) {
          eventPublisher.publishEvent(
              new DmlObservedEvent(this, List.of(event), DmlOrigin.fromMdc()));
        } else {
          pendingBatches
              .computeIfAbsent(
                  connectionId, k -> new PendingBatch(new ArrayList<>(), DmlOrigin.fromMdc()))
              .events()
              .add(event);
        }
      }
    }
  }

  // ── MethodExecutionListener ──

  @Override
  public void beforeMethod(MethodExecutionContext context) {
    // No-op
  }

  @Override
  public void afterMethod(MethodExecutionContext context) {
    Method method = context.getMethod();
    String methodName = method.getName();
    Object target = context.getTarget();

    if (!(target instanceof Connection)) {
      return;
    }

    String connectionId = getConnectionIdFromContext(context);

    switch (methodName) {
      case "commit" -> publishAndClear(connectionId);
      case "rollback" -> discardAndClear(connectionId);
      case "close" -> discardAndClear(connectionId); // Safety net for auto-commit leftovers
      default -> {
        // No action for other methods
      }
    }
  }

  // ── Internal helpers ──

  private void publishAndClear(String connectionId) {
    PendingBatch batch = pendingBatches.remove(connectionId);
    if (batch != null && !batch.events().isEmpty()) {
      log.debug(
          "Publishing {} DML events for connection {}", batch.events().size(), connectionId);
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
      log.debug(
          "Discarded {} DML events for connection {} (rollback/close)",
          batch.events().size(),
          connectionId);
    }
  }

  private String getConnectionId(ExecutionInfo execInfo) {
    return String.valueOf(execInfo.getConnectionId());
  }

  private String getConnectionIdFromContext(MethodExecutionContext context) {
    if (context.getConnectionInfo() != null) {
      return String.valueOf(context.getConnectionInfo().getConnectionId());
    }
    // Fallback to identity hash
    return String.valueOf(System.identityHashCode(context.getTarget()));
  }

  private boolean isAutoCommit(ExecutionInfo execInfo) {
    try {
      Connection conn = execInfo.getStatement().getConnection();
      return conn.getAutoCommit();
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Extracts the primary key value from JDBC parameters using the parse result's column-to-param
   * mapping and the registry's PK column info.
   */
  private Serializable extractPrimaryKey(
      DmlParseResult parseResult,
      TableInfo tableInfo,
      List<List<ParameterSetOperation>> parametersList,
      int batchIndex) {
    if (tableInfo == null || parametersList == null || parametersList.isEmpty()) {
      return null;
    }

    String[] pkColumns = tableInfo.getPkColumnNames();
    if (pkColumns == null || pkColumns.length == 0) {
      return null;
    }

    // Only handle single-column PKs for now
    if (pkColumns.length != 1) {
      return null;
    }

    String pkColumn = pkColumns[0];
    Map<String, Integer> columnToParam = parseResult.getColumnToParamIndex();

    Integer paramPosition = columnToParam.get(pkColumn);
    if (paramPosition == null) {
      return null;
    }

    return extractParamValueAsSerializable(parametersList, paramPosition, batchIndex);
  }

  private Serializable extractParamValueAsSerializable(
      List<List<ParameterSetOperation>> parametersList, int paramPosition, int batchIndex) {
    if (batchIndex >= parametersList.size()) {
      return null;
    }

    List<ParameterSetOperation> params = parametersList.get(batchIndex);
    if (params == null) {
      return null;
    }

    for (ParameterSetOperation op : params) {
      Object[] args = op.getArgs();
      if (args != null && args.length >= 2 && args[0] instanceof Number num) {
        if (num.intValue() == paramPosition && args[1] instanceof Serializable ser) {
          return ser;
        }
      }
    }

    return null;
  }
}
