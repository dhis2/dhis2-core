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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import net.ttddyy.dsproxy.ConnectionInfo;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import org.hisp.dhis.audit.DmlObservedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.StaticApplicationContext;

/**
 * Tests that connection IDs from afterQuery (ExecutionInfo) and afterMethod
 * (MethodExecutionContext) match for the same connection lifecycle, ensuring the pending batch is
 * correctly published on commit and discarded on rollback.
 */
class DmlObserverListenerConnectionIdTest {

  private HibernateTableEntityRegistry registry;
  private ApplicationEventPublisher eventPublisher;
  private DmlObserverListener listener;

  @BeforeEach
  void setUp() {
    registry = mock(HibernateTableEntityRegistry.class);
    eventPublisher = mock(ApplicationEventPublisher.class);
    listener = new DmlObserverListener(registry, eventPublisher, null);
    listener.onApplicationEvent(new ContextRefreshedEvent(new StaticApplicationContext()));
  }

  @Test
  void connectionIdFromAfterQueryMatchesAfterMethod_commit() throws Exception {
    String connId = "conn-42";

    // afterQuery with connection ID from ExecutionInfo
    ExecutionInfo execInfo = createExecInfo(connId, false);
    listener.afterQuery(execInfo, List.of(createDml("INSERT INTO dataelement (uid) VALUES (?)")));
    verify(eventPublisher, never()).publishEvent(any());

    // afterMethod with same connection ID from MethodExecutionContext
    MethodExecutionContext commitCtx = createMethodCtx(connId, "commit");
    listener.afterMethod(commitCtx);

    ArgumentCaptor<DmlObservedEvent> captor = ArgumentCaptor.forClass(DmlObservedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertEquals(1, captor.getValue().getEvents().size());
  }

  @Test
  void connectionIdFromAfterQueryMatchesAfterMethod_rollback() throws Exception {
    String connId = "conn-43";

    ExecutionInfo execInfo = createExecInfo(connId, false);
    listener.afterQuery(execInfo, List.of(createDml("INSERT INTO dataelement (uid) VALUES (?)")));

    MethodExecutionContext rollbackCtx = createMethodCtx(connId, "rollback");
    listener.afterMethod(rollbackCtx);

    // Rollback should discard, not publish
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void mismatchedConnectionId_doesNotPublishBatch() throws Exception {
    // afterQuery uses one connection ID
    ExecutionInfo execInfo = createExecInfo("conn-A", false);
    listener.afterQuery(execInfo, List.of(createDml("INSERT INTO dataelement (uid) VALUES (?)")));

    // afterMethod uses a DIFFERENT connection ID
    MethodExecutionContext commitCtx = createMethodCtx("conn-B", "commit");
    listener.afterMethod(commitCtx);

    // The batch for conn-A is still pending, conn-B has no batch to publish
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void nullConnectionInfo_doesNotCrash() throws Exception {
    ExecutionInfo execInfo = createExecInfo("conn-C", false);
    listener.afterQuery(execInfo, List.of(createDml("INSERT INTO dataelement (uid) VALUES (?)")));

    // MethodExecutionContext with null ConnectionInfo
    MethodExecutionContext ctx = mock(MethodExecutionContext.class);
    Connection conn = mock(Connection.class);
    when(ctx.getTarget()).thenReturn(conn);
    when(ctx.getMethod()).thenReturn(Connection.class.getMethod("commit"));
    when(ctx.getConnectionInfo()).thenReturn(null);

    listener.afterMethod(ctx);

    // Should not publish because connection ID couldn't be resolved
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void stringConnectionId_matchesAcrossCallbacks() throws Exception {
    // Verify string connection IDs match between afterQuery and afterMethod
    String connId = "12345";

    ExecutionInfo execInfo = createExecInfo(connId, false);
    listener.afterQuery(execInfo, List.of(createDml("INSERT INTO dataelement (uid) VALUES (?)")));

    MethodExecutionContext commitCtx = createMethodCtx(connId, "commit");
    listener.afterMethod(commitCtx);

    ArgumentCaptor<DmlObservedEvent> captor = ArgumentCaptor.forClass(DmlObservedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertEquals(1, captor.getValue().getEvents().size());
  }

  @Test
  void multipleConnectionsDoNotInterfere() throws Exception {
    // Two connections accumulating events simultaneously
    ExecutionInfo execA = createExecInfo("conn-X", false);
    ExecutionInfo execB = createExecInfo("conn-Y", false);

    listener.afterQuery(execA, List.of(createDml("INSERT INTO dataelement (uid) VALUES (?)")));
    listener.afterQuery(execB, List.of(createDml("INSERT INTO organisationunit (uid) VALUES (?)")));

    // Commit conn-X only
    listener.afterMethod(createMethodCtx("conn-X", "commit"));

    ArgumentCaptor<DmlObservedEvent> captor = ArgumentCaptor.forClass(DmlObservedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertEquals(1, captor.getValue().getEvents().size());
    assertEquals("dataelement", captor.getValue().getEvents().get(0).getTableName());

    // conn-Y still pending — rollback it
    listener.afterMethod(createMethodCtx("conn-Y", "rollback"));
    // Should still be only 1 event published total
    verify(eventPublisher, org.mockito.Mockito.times(1)).publishEvent(any());
  }

  // ── Helpers ──

  private ExecutionInfo createExecInfo(String connectionId, boolean autoCommit) throws Exception {
    ExecutionInfo execInfo = mock(ExecutionInfo.class);
    when(execInfo.isSuccess()).thenReturn(true);
    when(execInfo.getConnectionId()).thenReturn(connectionId);
    Connection conn = mock(Connection.class);
    when(conn.getAutoCommit()).thenReturn(autoCommit);
    Statement stmt = mock(Statement.class);
    when(stmt.getConnection()).thenReturn(conn);
    when(execInfo.getStatement()).thenReturn(stmt);
    return execInfo;
  }

  private MethodExecutionContext createMethodCtx(String connectionId, String methodName)
      throws Exception {
    MethodExecutionContext ctx = mock(MethodExecutionContext.class);
    Connection conn = mock(Connection.class);
    when(ctx.getTarget()).thenReturn(conn);
    Method method = Connection.class.getMethod(methodName);
    when(ctx.getMethod()).thenReturn(method);
    ConnectionInfo connInfo = new ConnectionInfo();
    connInfo.setConnectionId(connectionId);
    when(ctx.getConnectionInfo()).thenReturn(connInfo);
    return ctx;
  }

  private QueryInfo createDml(String sql) {
    QueryInfo queryInfo = new QueryInfo();
    queryInfo.setQuery(sql);
    queryInfo.getParametersList().add(new ArrayList<>());
    return queryInfo;
  }
}
