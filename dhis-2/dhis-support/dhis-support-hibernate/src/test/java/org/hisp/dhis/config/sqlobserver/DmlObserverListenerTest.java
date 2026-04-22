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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.hisp.dhis.cache.ETagService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.StaticApplicationContext;

class DmlObserverListenerTest {

  private HibernateTableEntityRegistry registry;
  private ETagService eTagService;
  private DmlObserverListener listener;

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
    when(registry.getTableInfo("datavalue"))
        .thenReturn(
            new HibernateTableEntityRegistry.TableInfo("datavalue", DataValue.class, List.of()));
    listener = new DmlObserverListener(registry, eTagService, null);
    activateListener(listener);
  }

  @Test
  void afterQuery_selectDoesNotBumpVersions() {
    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries = List.of(createQueryInfo("SELECT * FROM dataelement"));

    listener.afterQuery(execInfo, queries);

    verifyNoInteractions(eTagService);
  }

  @Test
  void afterQuery_failedExecutionDoesNotBumpVersions() {
    ExecutionInfo execInfo = mock(ExecutionInfo.class);
    when(execInfo.isSuccess()).thenReturn(false);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"));

    listener.afterQuery(execInfo, queries);

    verifyNoInteractions(eTagService);
  }

  @Test
  void afterQuery_ignoresDmlBeforeContextRefresh() {
    DmlObserverListener inactiveListener = new DmlObserverListener(registry, eTagService, null);
    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"));

    inactiveListener.afterQuery(execInfo, queries);

    verifyNoInteractions(eTagService);
    verify(registry, never()).getTableInfo(any());
  }

  @Test
  void afterQuery_excludedTableDoesNotBumpVersions() {
    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO audit (data) VALUES (?)"));

    listener.afterQuery(execInfo, queries);

    verifyNoInteractions(eTagService);
  }

  @Test
  void afterQuery_insertOnAutoCommitBumpsVersionImmediately() {
    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"));

    listener.afterQuery(execInfo, queries);

    verify(eTagService, times(1)).incrementEntityTypeVersion(DataElement.class);
  }

  @Test
  void afterQuery_insertOnNonAutoCommitAccumulatesEvents() {
    ExecutionInfo execInfo = createSuccessExecutionInfo(false);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"));

    listener.afterQuery(execInfo, queries);

    // No version bump yet (waiting for commit)
    verifyNoInteractions(eTagService);
  }

  @Test
  void commitBumpsVersionsForAccumulatedEvents() throws java.sql.SQLException {
    // Accumulate an event
    ExecutionInfo execInfo = createSuccessExecutionInfo(false);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"));
    listener.afterQuery(execInfo, queries);

    // Simulate commit
    Connection conn = execInfo.getStatement().getConnection();
    MethodExecutionContext commitCtx = createMethodContext(conn, "commit");
    listener.afterMethod(commitCtx);

    verify(eTagService, times(1)).incrementEntityTypeVersion(DataElement.class);
  }

  @Test
  void rollbackDoesNotBumpVersions() throws java.sql.SQLException {
    // Accumulate an event
    ExecutionInfo execInfo = createSuccessExecutionInfo(false);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"));
    listener.afterQuery(execInfo, queries);

    // Simulate rollback
    Connection conn = execInfo.getStatement().getConnection();
    MethodExecutionContext rollbackCtx = createMethodContext(conn, "rollback");
    listener.afterMethod(rollbackCtx);

    // No version bump should happen
    verifyNoInteractions(eTagService);
  }

  @Test
  void afterQuery_multipleDmlOnAutoCommitBumpsForEachEntityType() {
    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries =
        List.of(
            createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"),
            createQueryInfo("UPDATE organisationunit SET name = ? WHERE organisationunitid = ?"));

    listener.afterQuery(execInfo, queries);

    verify(eTagService, times(1)).incrementEntityTypeVersion(DataElement.class);
    verify(eTagService, times(1)).incrementEntityTypeVersion(OrganisationUnit.class);
  }

  @Test
  void afterQuery_deleteOnAutoCommitBumpsVersionImmediately() {
    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries =
        List.of(createQueryInfo("DELETE FROM dataelement WHERE dataelementid = ?"));

    listener.afterQuery(execInfo, queries);

    verify(eTagService, times(1)).incrementEntityTypeVersion(DataElement.class);
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

    // Only 1 version bump (deduped by table:operation AND by entity type)
    verify(eTagService, times(1)).incrementEntityTypeVersion(DataElement.class);
  }

  @Test
  void afterQuery_differentEntityTypesHaveSeparateVersionBumps() throws java.sql.SQLException {
    ExecutionInfo execInfo = createSuccessExecutionInfo(false);
    listener.afterQuery(
        execInfo, List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)")));
    listener.afterQuery(
        execInfo, List.of(createQueryInfo("INSERT INTO organisationunit (uid) VALUES (?)")));

    // Commit
    Connection conn = execInfo.getStatement().getConnection();
    MethodExecutionContext commitCtx = createMethodContext(conn, "commit");
    listener.afterMethod(commitCtx);

    verify(eTagService, times(1)).incrementEntityTypeVersion(DataElement.class);
    verify(eTagService, times(1)).incrementEntityTypeVersion(OrganisationUnit.class);
  }

  @Test
  void afterQuery_sameEntityTypeDifferentOperationsDeduplicatedByEntityType()
      throws java.sql.SQLException {
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

    // 3 DmlEvents in batch (different operations), but only 1 entity-type version bump
    verify(eTagService, times(1)).incrementEntityTypeVersion(DataElement.class);
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

    // Both transactions should have bumped the version
    verify(eTagService, times(2)).incrementEntityTypeVersion(DataElement.class);
  }

  @Test
  void afterQuery_nonMetadataEntityDoesNotBumpVersion() {
    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO datavalue (value) VALUES (?)"));

    listener.afterQuery(execInfo, queries);

    // DataValue is not a MetadataObject and not in the observed types list
    verify(eTagService, never()).incrementEntityTypeVersion(any());
  }

  @Test
  void afterQuery_unmappedTableDoesNotBumpVersion() {
    // Table with no registry mapping (entityClassName will be null)
    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO unknowntable (uid) VALUES (?)"));

    listener.afterQuery(execInfo, queries);

    verify(eTagService, never()).incrementEntityTypeVersion(any());
  }

  @Test
  void afterQuery_exceptionInVersionBumpDoesNotPropagate() {
    doThrow(new RuntimeException("Simulated version service failure"))
        .when(eTagService)
        .incrementEntityTypeVersion(DataElement.class);

    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"));

    assertDoesNotThrow(
        () -> listener.afterQuery(execInfo, queries),
        "Version bump failure should not propagate to JDBC layer");
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
