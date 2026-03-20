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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.hisp.dhis.dml.DmlObservedEvent;
import org.hisp.dhis.dml.DmlOperation;
import org.hisp.dhis.dml.DmlOrigin;
import org.hisp.dhis.log.MdcKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

class DmlObserverListenerTest {

  private HibernateTableEntityRegistry registry;
  private ApplicationEventPublisher eventPublisher;
  private DmlObserverListener listener;

  @BeforeEach
  void setUp() {
    registry = mock(HibernateTableEntityRegistry.class);
    eventPublisher = mock(ApplicationEventPublisher.class);
    listener = new DmlObserverListener(registry, eventPublisher, null);
    activateListener(listener);
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  void afterQuery_selectDoesNotProduceEvents() {
    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries = List.of(createQueryInfo("SELECT * FROM dataelement"));

    listener.afterQuery(execInfo, queries);

    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void afterQuery_failedExecutionDoesNotProduceEvents() {
    ExecutionInfo execInfo = mock(ExecutionInfo.class);
    when(execInfo.isSuccess()).thenReturn(false);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"));

    listener.afterQuery(execInfo, queries);

    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void afterQuery_ignoresDmlBeforeContextRefresh() {
    DmlObserverListener inactiveListener = new DmlObserverListener(registry, eventPublisher, null);
    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"));

    inactiveListener.afterQuery(execInfo, queries);

    verify(eventPublisher, never()).publishEvent(any());
    verify(registry, never()).getTableInfo(any());
  }

  @Test
  void afterQuery_excludedTableDoesNotProduceEvents() {
    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO audit (data) VALUES (?)"));

    listener.afterQuery(execInfo, queries);

    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void afterQuery_insertOnAutoCommitPublishesImmediately() {
    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"));

    listener.afterQuery(execInfo, queries);

    ArgumentCaptor<DmlObservedEvent> captor = ArgumentCaptor.forClass(DmlObservedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    DmlObservedEvent event = captor.getValue();
    assertEquals(1, event.getEvents().size());
    assertEquals(DmlOperation.INSERT, event.getEvents().get(0).operation());
    assertEquals("dataelement", event.getEvents().get(0).tableName());
  }

  @Test
  void afterQuery_insertOnNonAutoCommitAccumulatesEvents() {
    ExecutionInfo execInfo = createSuccessExecutionInfo(false);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"));

    listener.afterQuery(execInfo, queries);

    // No event published yet (waiting for commit)
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void commitPublishesAccumulatedEvents() throws java.sql.SQLException {
    // Accumulate an event
    ExecutionInfo execInfo = createSuccessExecutionInfo(false);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"));
    listener.afterQuery(execInfo, queries);

    // Simulate commit
    Connection conn = execInfo.getStatement().getConnection();
    MethodExecutionContext commitCtx = createMethodContext(conn, "commit");
    listener.afterMethod(commitCtx);

    ArgumentCaptor<DmlObservedEvent> captor = ArgumentCaptor.forClass(DmlObservedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    DmlObservedEvent event = captor.getValue();
    assertEquals(1, event.getEvents().size());
  }

  @Test
  void rollbackDiscardsAccumulatedEvents() throws java.sql.SQLException {
    // Accumulate an event
    ExecutionInfo execInfo = createSuccessExecutionInfo(false);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"));
    listener.afterQuery(execInfo, queries);

    // Simulate rollback
    Connection conn = execInfo.getStatement().getConnection();
    MethodExecutionContext rollbackCtx = createMethodContext(conn, "rollback");
    listener.afterMethod(rollbackCtx);

    // No event should be published
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void afterQuery_multipleDmlInBatch() {
    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries =
        List.of(
            createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"),
            createQueryInfo("UPDATE organisationunit SET name = ? WHERE organisationunitid = ?"));

    listener.afterQuery(execInfo, queries);

    // Auto-commit publishes one event per DML statement
    ArgumentCaptor<DmlObservedEvent> captor = ArgumentCaptor.forClass(DmlObservedEvent.class);
    verify(eventPublisher, org.mockito.Mockito.times(2)).publishEvent(captor.capture());

    List<DmlObservedEvent> published = captor.getAllValues();
    assertEquals(2, published.size());
    assertEquals(DmlOperation.INSERT, published.get(0).getEvents().get(0).operation());
    assertEquals("dataelement", published.get(0).getEvents().get(0).tableName());
    assertEquals(DmlOperation.UPDATE, published.get(1).getEvents().get(0).operation());
    assertEquals("organisationunit", published.get(1).getEvents().get(0).tableName());
  }

  @Test
  void afterQuery_deleteOnAutoCommitPublishesImmediately() {
    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries =
        List.of(createQueryInfo("DELETE FROM dataelement WHERE dataelementid = ?"));

    listener.afterQuery(execInfo, queries);

    ArgumentCaptor<DmlObservedEvent> captor = ArgumentCaptor.forClass(DmlObservedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    DmlObservedEvent event = captor.getValue();
    assertEquals(1, event.getEvents().size());
    assertEquals(DmlOperation.DELETE, event.getEvents().get(0).operation());
    assertEquals("dataelement", event.getEvents().get(0).tableName());
  }

  @Test
  void afterQuery_autoCommitCapturesOriginFromMdc() {
    MDC.put(MdcKeys.MDC_CONTROLLER, "DataElementController");
    MDC.put(MdcKeys.MDC_METHOD, "postJsonObject");
    MDC.put(MdcKeys.MDC_REQUEST_ID, "req-123");
    MDC.put(MdcKeys.MDC_SESSION_ID, "sess-abc");

    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"));

    listener.afterQuery(execInfo, queries);

    ArgumentCaptor<DmlObservedEvent> captor = ArgumentCaptor.forClass(DmlObservedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    DmlOrigin origin = captor.getValue().getOrigin();
    assertNotNull(origin);
    assertEquals("DataElementController", origin.controller());
    assertEquals("postJsonObject", origin.method());
    assertEquals("req-123", origin.requestId());
    assertEquals("sess-abc", origin.sessionId());
  }

  @Test
  void commitCapturesOriginFromMdcAtAccumulationTime() throws java.sql.SQLException {
    MDC.put(MdcKeys.MDC_CONTROLLER, "OrgUnitController");
    MDC.put(MdcKeys.MDC_REQUEST_ID, "req-456");

    ExecutionInfo execInfo = createSuccessExecutionInfo(false);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"));
    listener.afterQuery(execInfo, queries);

    // Clear MDC before commit to prove origin was captured at accumulation time
    MDC.clear();

    Connection conn = execInfo.getStatement().getConnection();
    MethodExecutionContext commitCtx = createMethodContext(conn, "commit");
    listener.afterMethod(commitCtx);

    ArgumentCaptor<DmlObservedEvent> captor = ArgumentCaptor.forClass(DmlObservedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    DmlOrigin origin = captor.getValue().getOrigin();
    assertNotNull(origin);
    assertEquals("OrgUnitController", origin.controller());
    assertEquals("req-456", origin.requestId());
  }

  @Test
  void afterQuery_capturesAuthenticatedUsername() {
    User principal = new User("admin", "password", List.of());
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));

    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"));

    listener.afterQuery(execInfo, queries);

    ArgumentCaptor<DmlObservedEvent> captor = ArgumentCaptor.forClass(DmlObservedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    DmlOrigin origin = captor.getValue().getOrigin();
    assertNotNull(origin);
    assertEquals("admin", origin.username());
  }

  @Test
  void afterQuery_emptyMdcProducesOriginWithNulls() {
    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"));

    listener.afterQuery(execInfo, queries);

    ArgumentCaptor<DmlObservedEvent> captor = ArgumentCaptor.forClass(DmlObservedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    DmlOrigin origin = captor.getValue().getOrigin();
    assertNotNull(origin);
    assertNull(origin.username());
    assertNull(origin.controller());
    assertNull(origin.method());
    assertNull(origin.requestId());
    assertNull(origin.sessionId());
  }

  @Test
  void afterQuery_deduplicatesSameTableAndOperationWithinTransaction()
      throws java.sql.SQLException {
    // Simulate a bulk import: 100 INSERTs into the same table on the same connection
    for (int i = 0; i < 100; i++) {
      ExecutionInfo execInfo = createSuccessExecutionInfo(false);
      List<QueryInfo> queries =
          List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"));
      listener.afterQuery(execInfo, queries);
    }

    // Commit
    Connection conn = createSuccessExecutionInfo(false).getStatement().getConnection();
    MethodExecutionContext commitCtx = createMethodContext(conn, "commit");
    listener.afterMethod(commitCtx);

    // Only 1 event should be published (deduped by table:operation)
    ArgumentCaptor<DmlObservedEvent> captor = ArgumentCaptor.forClass(DmlObservedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    DmlObservedEvent event = captor.getValue();
    assertEquals(1, event.getEvents().size(), "100 INSERTs to same table should dedup to 1 event");
    assertEquals(DmlOperation.INSERT, event.getEvents().get(0).operation());
    assertEquals("dataelement", event.getEvents().get(0).tableName());
  }

  @Test
  void afterQuery_differentTablesNotDeduplicated() throws java.sql.SQLException {
    ExecutionInfo execInfo = createSuccessExecutionInfo(false);
    listener.afterQuery(
        execInfo, List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)")));
    listener.afterQuery(
        execInfo, List.of(createQueryInfo("INSERT INTO organisationunit (uid) VALUES (?)")));
    listener.afterQuery(
        execInfo, List.of(createQueryInfo("UPDATE dataelement SET name = ? WHERE uid = ?")));

    // Commit
    Connection conn = execInfo.getStatement().getConnection();
    MethodExecutionContext commitCtx = createMethodContext(conn, "commit");
    listener.afterMethod(commitCtx);

    ArgumentCaptor<DmlObservedEvent> captor = ArgumentCaptor.forClass(DmlObservedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    DmlObservedEvent event = captor.getValue();
    // 3 distinct table:operation combos: dataelement:INSERT, organisationunit:INSERT,
    // dataelement:UPDATE
    assertEquals(
        3,
        event.getEvents().size(),
        "Different table:operation combos should produce separate events");
  }

  @Test
  void afterQuery_sameTableDifferentOperationsNotDeduplicated() throws java.sql.SQLException {
    ExecutionInfo execInfo = createSuccessExecutionInfo(false);
    listener.afterQuery(
        execInfo, List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)")));
    listener.afterQuery(
        execInfo, List.of(createQueryInfo("UPDATE dataelement SET name = ? WHERE uid = ?")));
    listener.afterQuery(
        execInfo, List.of(createQueryInfo("DELETE FROM dataelement WHERE uid = ?")));

    // Commit
    Connection conn = execInfo.getStatement().getConnection();
    MethodExecutionContext commitCtx = createMethodContext(conn, "commit");
    listener.afterMethod(commitCtx);

    ArgumentCaptor<DmlObservedEvent> captor = ArgumentCaptor.forClass(DmlObservedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    DmlObservedEvent event = captor.getValue();
    assertEquals(
        3,
        event.getEvents().size(),
        "INSERT, UPDATE, DELETE on same table should be 3 distinct events");
  }

  @Test
  void afterQuery_deduplicationResetsAfterCommit() throws java.sql.SQLException {
    ExecutionInfo execInfo = createSuccessExecutionInfo(false);

    // First transaction: INSERT into dataelement
    listener.afterQuery(
        execInfo, List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)")));

    Connection conn = execInfo.getStatement().getConnection();
    MethodExecutionContext commitCtx = createMethodContext(conn, "commit");
    listener.afterMethod(commitCtx);

    // Second transaction: same INSERT into dataelement (should NOT be deduped against first tx)
    listener.afterQuery(
        execInfo, List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)")));
    listener.afterMethod(commitCtx);

    ArgumentCaptor<DmlObservedEvent> captor = ArgumentCaptor.forClass(DmlObservedEvent.class);
    verify(eventPublisher, org.mockito.Mockito.times(2)).publishEvent(captor.capture());
    // Both transactions should have published 1 event each
    assertEquals(1, captor.getAllValues().get(0).getEvents().size());
    assertEquals(1, captor.getAllValues().get(1).getEvents().size());
  }

  @lombok.SneakyThrows
  private ExecutionInfo createSuccessExecutionInfo(boolean autoCommit) {
    ExecutionInfo execInfo = mock(ExecutionInfo.class);
    when(execInfo.isSuccess()).thenReturn(true);
    when(execInfo.getConnectionId()).thenReturn("test-conn-1");

    Connection conn = mock(Connection.class);
    when(conn.getAutoCommit()).thenReturn(autoCommit);

    Statement stmt = mock(Statement.class);
    when(stmt.getConnection()).thenReturn(conn);
    when(execInfo.getStatement()).thenReturn(stmt);

    return execInfo;
  }

  private QueryInfo createQueryInfo(String sql) {
    QueryInfo queryInfo = new QueryInfo();
    queryInfo.setQuery(sql);
    queryInfo.getParametersList().add(new ArrayList<>());
    return queryInfo;
  }

  @lombok.SneakyThrows
  private MethodExecutionContext createMethodContext(Object target, String methodName) {
    MethodExecutionContext ctx = mock(MethodExecutionContext.class);
    when(ctx.getTarget()).thenReturn(target);
    Method method = Connection.class.getMethod(methodName);
    when(ctx.getMethod()).thenReturn(method);

    // Match the connection ID used in ExecutionInfo
    ConnectionInfo connInfo = new ConnectionInfo();
    connInfo.setConnectionId("test-conn-1");
    when(ctx.getConnectionInfo()).thenReturn(connInfo);

    return ctx;
  }

  private void activateListener(DmlObserverListener dmlObserverListener) {
    dmlObserverListener.onApplicationEvent(
        new ContextRefreshedEvent(new StaticApplicationContext()));
  }
}
