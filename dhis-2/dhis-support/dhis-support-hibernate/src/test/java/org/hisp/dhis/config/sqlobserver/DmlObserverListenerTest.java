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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.ttddyy.dsproxy.ConnectionInfo;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import org.hisp.dhis.cache.ETagService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
        .thenReturn(new HibernateTableEntityRegistry.TableInfo("dataelement", DataElement.class));
    when(registry.getTableInfo("organisationunit"))
        .thenReturn(
            new HibernateTableEntityRegistry.TableInfo("organisationunit", OrganisationUnit.class));
    when(registry.getTableInfo("datavalue"))
        .thenReturn(new HibernateTableEntityRegistry.TableInfo("datavalue", DataValue.class));
    listener = new DmlObserverListener(registry, eTagService, null);
    activateListener(listener);
  }

  @ParameterizedTest(name = "[{index}] autoCommit={0}, sql={1}")
  @CsvSource({
    "true, SELECT * FROM dataelement",
    "true, INSERT INTO audit (data) VALUES (?)",
    "false, INSERT INTO dataelement (uid) VALUES (?)"
  })
  void afterQuery_doesNotBumpVersions(boolean autoCommit, String sql) throws Exception {
    ExecutionInfo execInfo = createSuccessExecutionInfo(autoCommit);
    List<QueryInfo> queries = List.of(createQueryInfo(sql));

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
  void afterQuery_ignoresDmlBeforeContextRefresh() throws Exception {
    DmlObserverListener inactiveListener = new DmlObserverListener(registry, eTagService, null);
    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"));

    inactiveListener.afterQuery(execInfo, queries);

    verifyNoInteractions(eTagService);
    verify(registry, never()).getTableInfo(any());
  }

  @Test
  void afterQuery_insertOnAutoCommitBumpsVersionImmediately() throws Exception {
    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"));

    listener.afterQuery(execInfo, queries);

    verify(eTagService, times(1)).incrementEntityTypeVersion(DataElement.class);
  }

  @Test
  void afterQuery_jobConfigurationUpdateBumpsVersion() throws Exception {
    // /api/jobConfigurations is a cached metadata endpoint, so its table must be observed
    when(registry.getTableInfo("jobconfiguration"))
        .thenReturn(
            new HibernateTableEntityRegistry.TableInfo("jobconfiguration", JobConfiguration.class));
    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries =
        List.of(createQueryInfo("UPDATE jobconfiguration SET jobstatus = ? WHERE jobid = ?"));

    listener.afterQuery(execInfo, queries);

    verify(eTagService, times(1)).incrementEntityTypeVersion(JobConfiguration.class);
  }

  @Test
  void commitOfAJoinTableWriteBumpsTheOwnersOfBothSides() throws Exception {
    // adding a user to a group writes only usergroupmembers
    when(registry.getTableInfo("usergroupmembers"))
        .thenReturn(
            new HibernateTableEntityRegistry.TableInfo(
                "usergroupmembers", List.of(UserGroup.class, User.class)));
    ExecutionInfo execInfo = createSuccessExecutionInfo(false);
    listener.afterQuery(
        execInfo,
        List.of(
            createQueryInfo("INSERT INTO usergroupmembers (usergroupid, userid) VALUES (?, ?)")));

    listener.afterMethod(createMethodContext(execInfo.getStatement().getConnection(), "commit"));

    verify(eTagService).incrementEntityTypeVersion(UserGroup.class);
    verify(eTagService).incrementEntityTypeVersion(User.class);
  }

  @Test
  void commitBumpsVersionsForAccumulatedEvents() throws Exception {
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
  void rollbackDoesNotBumpVersions() throws Exception {
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
  void afterQuery_multipleDmlOnAutoCommitBumpsForEachEntityType() throws Exception {
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
  void afterQuery_deleteOnAutoCommitBumpsVersionImmediately() throws Exception {
    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries =
        List.of(createQueryInfo("DELETE FROM dataelement WHERE dataelementid = ?"));

    listener.afterQuery(execInfo, queries);

    verify(eTagService, times(1)).incrementEntityTypeVersion(DataElement.class);
  }

  @Test
  void afterQuery_deduplicatesSameTableAndOperationWithinTransaction() throws Exception {
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
  void afterQuery_differentEntityTypesHaveSeparateVersionBumps() throws Exception {
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
  void afterQuery_sameEntityTypeDifferentOperationsDeduplicatedByEntityType() throws Exception {
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
  void afterQuery_deduplicationResetsAfterCommit() throws Exception {
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
  void afterQuery_nonMetadataEntityDoesNotBumpVersion() throws Exception {
    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO datavalue (value) VALUES (?)"));

    listener.afterQuery(execInfo, queries);

    // DataValue is not a MetadataObject and not in the observed types list
    verify(eTagService, never()).incrementEntityTypeVersion(any());
  }

  @Test
  void afterQuery_unmappedTableDoesNotBumpVersion() throws Exception {
    // Table with no registry mapping (entityClassName will be null)
    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO unknowntable (uid) VALUES (?)"));

    listener.afterQuery(execInfo, queries);

    verify(eTagService, never()).incrementEntityTypeVersion(any());
  }

  @Test
  void afterQuery_exceptionInVersionBumpDoesNotPropagate() throws Exception {
    doThrow(new RuntimeException("Simulated version service failure"))
        .when(eTagService)
        .incrementEntityTypeVersion(DataElement.class);

    ExecutionInfo execInfo = createSuccessExecutionInfo(true);
    List<QueryInfo> queries = List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)"));

    assertDoesNotThrow(
        () -> listener.afterQuery(execInfo, queries),
        "Version bump failure should not propagate to JDBC layer");
  }

  @Test
  void afterQuery_reportsWritesTheParserCannotRead() throws Exception {
    UnmatchedDmlLog unmatched = mock(UnmatchedDmlLog.class);
    DmlObserverListener reporting =
        new DmlObserverListener(registry, eTagService, null, unmatched, System::nanoTime);
    activateListener(reporting);
    String cteDelete = "WITH t AS (SELECT 1) DELETE FROM period WHERE periodid IN (SELECT 1)";
    String indentedUpdate = "  UPDATE dataelement SET name = ?";

    reporting.afterQuery(
        createSuccessExecutionInfo(true),
        List.of(
            createQueryInfo(cteDelete),
            createQueryInfo(indentedUpdate),
            createQueryInfo("SELECT * FROM dataelement"),
            createQueryInfo("UPDATE dataelement SET name = ?")));

    verify(unmatched).report(cteDelete);
    verify(unmatched).report(indentedUpdate);
    verifyNoMoreInteractions(unmatched);
  }

  @Test
  void sweepKeepsTheBatchOfATransactionThatIsStillWriting() throws Exception {
    AtomicLong now = new AtomicLong();
    DmlObserverListener timed =
        new DmlObserverListener(registry, eTagService, null, new UnmatchedDmlLog(), now::get);
    activateListener(timed);
    ExecutionInfo execInfo = createSuccessExecutionInfo(false);

    // a long import: it writes for more than the stale age, never idle for that long
    timed.afterQuery(
        execInfo, List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)")));
    now.addAndGet(TimeUnit.MINUTES.toNanos(4));
    timed.afterQuery(
        execInfo, List.of(createQueryInfo("INSERT INTO organisationunit (uid) VALUES (?)")));
    now.addAndGet(TimeUnit.MINUTES.toNanos(4));
    timed.sweepStaleBatches();
    verifyNoInteractions(eTagService);

    timed.afterMethod(createMethodContext(execInfo.getStatement().getConnection(), "commit"));

    verify(eTagService).incrementEntityTypeVersion(DataElement.class);
    verify(eTagService).incrementEntityTypeVersion(OrganisationUnit.class);
  }

  @Test
  void sweepOfAnAbandonedBatchBumpsItsTypes() throws Exception {
    AtomicLong now = new AtomicLong();
    DmlObserverListener timed =
        new DmlObserverListener(registry, eTagService, null, new UnmatchedDmlLog(), now::get);
    activateListener(timed);

    // no commit, rollback or close is ever seen for this connection
    timed.afterQuery(
        createSuccessExecutionInfo(false),
        List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)")));
    now.addAndGet(TimeUnit.MINUTES.toNanos(6));
    timed.sweepStaleBatches();

    // dropping it silently would leave the ETag unchanged if the write did commit
    verify(eTagService).incrementEntityTypeVersion(DataElement.class);
    assertEquals(0, timed.pendingEventCount());
  }

  @Test
  void lateCommitAfterTheSweepBumpsAgain() throws Exception {
    AtomicLong now = new AtomicLong();
    DmlObserverListener timed =
        new DmlObserverListener(registry, eTagService, null, new UnmatchedDmlLog(), now::get);
    activateListener(timed);
    ExecutionInfo execInfo = createSuccessExecutionInfo(false);

    // a transaction writes, then stays idle past the stale age before it commits
    timed.afterQuery(
        execInfo, List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)")));
    now.addAndGet(TimeUnit.MINUTES.toNanos(6));
    timed.sweepStaleBatches();
    verify(eTagService, times(1)).incrementEntityTypeVersion(DataElement.class);

    // readers between sweep and commit saw the old data under the new ETag: bump again
    timed.afterMethod(createMethodContext(execInfo.getStatement().getConnection(), "commit"));

    verify(eTagService, times(2)).incrementEntityTypeVersion(DataElement.class);
    assertEquals(0, timed.sweptBatchCount());
  }

  @Test
  void rollbackAfterTheSweepDoesNotBumpAgain() throws Exception {
    AtomicLong now = new AtomicLong();
    DmlObserverListener timed =
        new DmlObserverListener(registry, eTagService, null, new UnmatchedDmlLog(), now::get);
    activateListener(timed);
    ExecutionInfo execInfo = createSuccessExecutionInfo(false);

    timed.afterQuery(
        execInfo, List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)")));
    now.addAndGet(TimeUnit.MINUTES.toNanos(6));
    timed.sweepStaleBatches();
    timed.afterMethod(createMethodContext(execInfo.getStatement().getConnection(), "rollback"));

    verify(eTagService, times(1)).incrementEntityTypeVersion(DataElement.class);
    assertEquals(0, timed.sweptBatchCount());
  }

  @Test
  void sweptBatchesAreBoundedByCountAndAge() throws Exception {
    AtomicLong now = new AtomicLong();
    DmlObserverListener timed =
        new DmlObserverListener(registry, eTagService, null, new UnmatchedDmlLog(), now::get);
    activateListener(timed);
    for (int i = 0; i <= DmlObserverListener.MAX_SWEPT_BATCHES; i++) {
      ExecutionInfo execInfo = createSuccessExecutionInfo(false);
      when(execInfo.getConnectionId()).thenReturn("abandoned-" + i);
      timed.afterQuery(
          execInfo, List.of(createQueryInfo("INSERT INTO dataelement (uid) VALUES (?)")));
    }
    now.addAndGet(TimeUnit.MINUTES.toNanos(6));
    timed.sweepStaleBatches();
    assertEquals(DmlObserverListener.MAX_SWEPT_BATCHES, timed.sweptBatchCount());

    // connections that never commit, roll back or close are forgotten after an hour
    now.addAndGet(TimeUnit.MINUTES.toNanos(61));
    timed.sweepStaleBatches();
    assertEquals(0, timed.sweptBatchCount());
  }

  private ExecutionInfo createSuccessExecutionInfo(boolean autoCommit) throws SQLException {
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

  private MethodExecutionContext createMethodContext(Object target, String methodName)
      throws NoSuchMethodException {
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
