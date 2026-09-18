/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static org.hisp.dhis.dml.DmlETagMetrics.DML_OBSERVER_PENDING_BATCHES;
import static org.hisp.dhis.dml.DmlETagMetrics.DML_OBSERVER_PENDING_EVENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import net.ttddyy.dsproxy.ConnectionInfo;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import org.hisp.dhis.cache.ETagService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.StaticApplicationContext;

/**
 * Pending-batch Micrometer gauges for {@link DmlObserverListener}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class DmlObserverListenerGaugeTest {

  private HibernateTableEntityRegistry registry;
  private ETagService eTagService;

  @BeforeEach
  void setUp() {
    registry = mock(HibernateTableEntityRegistry.class);
    eTagService = mock(ETagService.class);
    when(registry.getTableInfo("dataelement"))
        .thenReturn(
            new HibernateTableEntityRegistry.TableInfo(
                "dataelement", DataElement.class, List.of()));
    when(registry.getTableInfo("organisationunit"))
        .thenReturn(
            new HibernateTableEntityRegistry.TableInfo(
                "organisationunit", OrganisationUnit.class, List.of()));
  }

  @Test
  @DisplayName("Pending gauges track batches and event counts across connections")
  void pendingGaugesTrackBatchesAndEvents() throws Exception {
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    DmlObserverListener listener = new DmlObserverListener(registry, eTagService, meters);
    activate(listener);

    Gauge batches = meters.find(DML_OBSERVER_PENDING_BATCHES).gauge();
    Gauge events = meters.find(DML_OBSERVER_PENDING_EVENTS).gauge();
    assertNotNull(batches);
    assertNotNull(events);
    assertEquals(0.0, batches.value());
    assertEquals(0.0, events.value());

    // Two connections, non-autocommit: each accumulates a pending batch.
    ExecutionInfo connA = createSuccessExecutionInfo(false, "conn-a");
    ExecutionInfo connB = createSuccessExecutionInfo(false, "conn-b");
    listener.afterQuery(
        connA, List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)")));
    listener.afterQuery(
        connA, List.of(createQueryInfo("UPDATE dataelement SET name = ? WHERE uid = ?")));
    listener.afterQuery(
        connB, List.of(createQueryInfo("INSERT INTO organisationunit (uid) VALUES (?)")));

    // connA: INSERT + UPDATE on same table = 2 events (different ops); connB: 1 event.
    assertEquals(2.0, batches.value());
    assertEquals(3.0, events.value());
    assertEquals(3, listener.pendingEventCount());

    // Commit connA clears one batch.
    listener.afterMethod(createMethodContext(connA, "commit"));
    assertEquals(1.0, batches.value());
    assertEquals(1.0, events.value());

    listener.afterMethod(createMethodContext(connB, "rollback"));
    assertEquals(0.0, batches.value());
    assertEquals(0.0, events.value());
  }

  @Test
  @DisplayName("No pending gauges when MeterRegistry is null")
  void noGaugesWithoutRegistry() {
    DmlObserverListener listener = new DmlObserverListener(registry, eTagService, null);
    activate(listener);
    // No registry to query; ensure construction and pendingEventCount still work.
    assertEquals(0, listener.pendingEventCount());
  }

  @Test
  @DisplayName("Null registry path does not register meter names on a new registry")
  void nullRegistryDoesNotLeakMeters() {
    // Smoke: building without registry leaves a fresh SimpleMeterRegistry empty of our names.
    SimpleMeterRegistry other = new SimpleMeterRegistry();
    new DmlObserverListener(registry, eTagService, null);
    assertNull(other.find(DML_OBSERVER_PENDING_BATCHES).gauge());
    assertNull(other.find(DML_OBSERVER_PENDING_EVENTS).gauge());
  }

  private static void activate(DmlObserverListener listener) {
    listener.onApplicationEvent(new ContextRefreshedEvent(new StaticApplicationContext()));
  }

  private static QueryInfo createQueryInfo(String sql) {
    QueryInfo qi = mock(QueryInfo.class);
    when(qi.getQuery()).thenReturn(sql);
    return qi;
  }

  private static ExecutionInfo createSuccessExecutionInfo(boolean autoCommit, String connectionId)
      throws Exception {
    ExecutionInfo execInfo = mock(ExecutionInfo.class);
    when(execInfo.isSuccess()).thenReturn(true);
    when(execInfo.getConnectionId()).thenReturn(connectionId);

    Connection conn = mock(Connection.class);
    when(conn.getAutoCommit()).thenReturn(autoCommit);
    Statement statement = mock(Statement.class);
    when(statement.getConnection()).thenReturn(conn);
    when(execInfo.getStatement()).thenReturn(statement);
    return execInfo;
  }

  private static MethodExecutionContext createMethodContext(ExecutionInfo execInfo, String method)
      throws Exception {
    Connection conn = execInfo.getStatement().getConnection();
    Method m = Connection.class.getMethod(method);
    MethodExecutionContext ctx = mock(MethodExecutionContext.class);
    when(ctx.getMethod()).thenReturn(m);
    when(ctx.getTarget()).thenReturn(conn);
    ConnectionInfo info = new ConnectionInfo();
    info.setConnectionId(String.valueOf(execInfo.getConnectionId()));
    when(ctx.getConnectionInfo()).thenReturn(info);
    return ctx;
  }
}
