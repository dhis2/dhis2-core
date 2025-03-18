/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.analytics.table;

import static java.util.Calendar.FEBRUARY;
import static java.util.Calendar.JANUARY;
import static java.util.Calendar.MARCH;
import static java.util.Collections.emptyList;
import static org.hisp.dhis.analytics.table.writer.JdbcOwnershipWriter.ENDDATE;
import static org.hisp.dhis.analytics.table.writer.JdbcOwnershipWriter.OU;
import static org.hisp.dhis.analytics.table.writer.JdbcOwnershipWriter.STARTDATE;
import static org.hisp.dhis.analytics.table.writer.JdbcOwnershipWriter.TRACKEDENTITY;
import static org.hisp.dhis.db.model.DataType.CHARACTER_11;
import static org.hisp.dhis.db.model.DataType.DATE;
import static org.hisp.dhis.db.model.constraint.Nullable.NOT_NULL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.analytics.table.writer.JdbcOwnershipWriter;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.system.database.DatabaseInfoProvider;
import org.hisp.dhis.test.TestBase;
import org.hisp.quick.JdbcConfiguration;
import org.hisp.quick.StatementDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.invocation.Invocation;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

/**
 * {@see JdbcOwnershipAnalyticsTableManager} Tester.
 *
 * @author Jim Grace
 */
@ExtendWith(MockitoExtension.class)
class JdbcOwnershipAnalyticsTableManagerTest extends TestBase {
  @Mock private IdentifiableObjectManager idObjectManager;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private CategoryService categoryService;

  @Mock private SystemSettingsProvider settingsProvider;

  @Mock private DataApprovalLevelService dataApprovalLevelService;

  @Mock private ResourceTableService resourceTableService;

  @Mock private AnalyticsTableHookService tableHookService;

  @Mock private PartitionManager partitionManager;

  @Mock private DatabaseInfoProvider databaseInfoProvider;

  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private JdbcConfiguration jdbcConfiguration;

  @Mock private DataSource dataSource;

  @Mock private Connection connection;

  @Mock private Statement statement;

  @Mock private JdbcOwnershipWriter writer;

  @Mock private AnalyticsTableSettings analyticsTableSettings;

  @Mock private PeriodDataProvider periodDataProvider;

  @Spy private final SqlBuilder sqlBuilder = new PostgreSqlBuilder();

  @InjectMocks private JdbcOwnershipAnalyticsTableManager manager;

  private static final Program programA = createProgram('A');

  private static final Program programB = createProgramWithoutRegistration('B');

  private static AnalyticsTable tableA;

  private static AnalyticsTable tableB;

  private static AnalyticsTablePartition partitionA;

  @BeforeEach
  public void setUp() {
    lenient().when(settingsProvider.getCurrentSettings()).thenReturn(SystemSettings.of(Map.of()));

    tableA =
        new AnalyticsTable(
            AnalyticsTableType.OWNERSHIP,
            JdbcOwnershipAnalyticsTableManager.FIXED_COLS,
            Logged.UNLOGGED,
            programA);

    tableB =
        new AnalyticsTable(
            AnalyticsTableType.OWNERSHIP,
            JdbcOwnershipAnalyticsTableManager.FIXED_COLS,
            Logged.UNLOGGED,
            programB);

    partitionA = new AnalyticsTablePartition(tableA, List.of(), 1, new Date(), new Date());
  }

  @Test
  void testGetAnalyticsTableType() {
    assertEquals(AnalyticsTableType.OWNERSHIP, manager.getAnalyticsTableType());
  }

  @Test
  void testGetAnalyticsTables() {
    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(programA, programB));

    AnalyticsTableUpdateParams params = AnalyticsTableUpdateParams.newBuilder().build();

    assertEquals(List.of(tableA, tableB), manager.getAnalyticsTables(params));

    params =
        AnalyticsTableUpdateParams.newBuilder()
            .lastYears(AnalyticsTablePartition.LATEST_PARTITION)
            .build();

    assertEquals(emptyList(), manager.getAnalyticsTables(params));
  }

  @Test
  void testGetPartitionChecks() {
    assertTrue(manager.getPartitionChecks(1, new Date()).isEmpty());
  }

  @Test
  void testPopulateTable() throws SQLException {
    String te1 = "teUid00001";
    String te2 = "teUid00002";

    String ou1 = "orgUnit0001";
    String ou2 = "orgUnit0002";

    Date start1 = new GregorianCalendar(2022, JANUARY, 1).getTime();
    Date end1 = new GregorianCalendar(2022, JANUARY, 31).getTime();
    Date start2 = new GregorianCalendar(2022, FEBRUARY, 1).getTime();
    Date end2 = new GregorianCalendar(2022, FEBRUARY, 28).getTime();
    Date start3 = new GregorianCalendar(2022, MARCH, 1).getTime();
    Date end3 = new GregorianCalendar(2022, MARCH, 31).getTime();

    when(jdbcConfiguration.getDialect()).thenReturn(StatementDialect.POSTGRESQL);
    when(jdbcConfiguration.getDataSource()).thenReturn(dataSource);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);

    // Mock the jdbcTemplate callback handler to return the mocked ResultSet
    // object:

    ResultSet resultSet1 = mock(ResultSet.class);
    ResultSet resultSet2 = mock(ResultSet.class);
    ResultSet resultSet3 = mock(ResultSet.class);

    doAnswer(
            invocation -> {
              RowCallbackHandler callbackHandler = invocation.getArgument(1);
              callbackHandler.processRow(resultSet1);
              callbackHandler.processRow(resultSet2);
              callbackHandler.processRow(resultSet3);
              return null;
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowCallbackHandler.class));

    // TE uid:
    when(resultSet1.getObject(1)).thenReturn(te1);
    when(resultSet2.getObject(1)).thenReturn(te2);
    when(resultSet3.getObject(1)).thenReturn(te2);

    // Start date:
    when(resultSet1.getObject(2)).thenReturn(start1);
    when(resultSet2.getObject(2)).thenReturn(start2);
    when(resultSet3.getObject(2)).thenReturn(start3);

    // End date (always null):
    when(resultSet1.getObject(3)).thenReturn(end1);
    when(resultSet2.getObject(3)).thenReturn(end2);
    when(resultSet3.getObject(3)).thenReturn(end3);

    // OrgUnit:
    when(resultSet1.getObject(4)).thenReturn(ou1);
    when(resultSet2.getObject(4)).thenReturn(ou2);
    when(resultSet3.getObject(4)).thenReturn(ou2);

    AnalyticsTableUpdateParams params = AnalyticsTableUpdateParams.newBuilder().build();

    try (MockedStatic<JdbcOwnershipWriter> mocked = mockStatic(JdbcOwnershipWriter.class)) {
      mocked.when(() -> JdbcOwnershipWriter.getInstance(any())).thenReturn(writer);

      manager.populateTable(params, partitionA);
    }

    List<Invocation> jdbcInvocations = getInvocations(jdbcTemplate);
    assertEquals(1, jdbcInvocations.size());
    assertEquals("query", jdbcInvocations.get(0).getMethod().getName());

    String sql = jdbcInvocations.get(0).getArgument(0);
    String sqlMasked =
        sql.replaceAll(
            "lastupdated <= '\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}'",
            "lastupdated <= 'yyyy-mm-ddThh:mm:ss'");
    assertEquals(
        """
        select te.uid,a.startdate,a.enddate,ou.uid from (\
        select h.trackedentityid, '1001-01-01' as startdate, h.enddate as enddate, h.organisationunitid \
        from "programownershiphistory" h \
        where h.programid = 0 \
        and h.organisationunitid is not null \
        union distinct \
        select o.trackedentityid, '2002-02-02' as startdate, null as enddate, o.organisationunitid \
        from "trackedentityprogramowner" o \
        where o.programid = 0 \
        and o.trackedentityid in (\
        select distinct p.trackedentityid \
        from "programownershiphistory" p \
        where p.programid = 0 \
        and p.organisationunitid is not null)) a \
        inner join "trackedentity" te on a.trackedentityid = te.trackedentityid \
        inner join "organisationunit" ou on a.organisationunitid = ou.organisationunitid \
        left join analytics_rs_orgunitstructure ous on a.organisationunitid = ous.organisationunitid \
        left join analytics_rs_organisationunitgroupsetstructure ougs on a.organisationunitid = ougs.organisationunitid \
        order by te.uid, a.startdate, a.enddate""",
        sqlMasked);

    List<Invocation> writerInvocations = getInvocations(writer);
    assertEquals(3, writerInvocations.size());

    assertEquals("write", writerInvocations.get(0).getMethod().getName());
    assertEquals("write", writerInvocations.get(1).getMethod().getName());
    assertEquals("write", writerInvocations.get(2).getMethod().getName());

    Map<String, Object> map0 = writerInvocations.get(0).getArgument(0);
    Map<String, Object> map1 = writerInvocations.get(1).getArgument(0);
    Map<String, Object> map2 = writerInvocations.get(2).getArgument(0);

    assertEquals(Map.of(TRACKEDENTITY, te1, STARTDATE, start1, ENDDATE, end1, OU, ou1), map0);
    assertEquals(Map.of(TRACKEDENTITY, te2, STARTDATE, start2, ENDDATE, end2, OU, ou2), map1);
    assertEquals(Map.of(TRACKEDENTITY, te2, STARTDATE, start3, ENDDATE, end3, OU, ou2), map2);
  }

  @Test
  void testGetFixedColumns() {
    List<AnalyticsTableColumn> expected =
        List.of(
            AnalyticsTableColumn.builder()
                .name("teuid")
                .dataType(CHARACTER_11)
                .selectExpression("te.uid")
                .build(),
            AnalyticsTableColumn.builder()
                .name("startdate")
                .dataType(DATE)
                .selectExpression("a.startdate")
                .build(),
            AnalyticsTableColumn.builder()
                .name("enddate")
                .dataType(DATE)
                .selectExpression("a.enddate")
                .build(),
            AnalyticsTableColumn.builder()
                .name("ou")
                .dataType(CHARACTER_11)
                .nullable(NOT_NULL)
                .selectExpression("ou.uid")
                .build());

    assertEquals(expected, JdbcOwnershipAnalyticsTableManager.FIXED_COLS);
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /** Gets a list of invocations of a mocked object */
  private List<Invocation> getInvocations(Object mock) {
    return new ArrayList<>(mockingDetails(mock).getInvocations());
  }
}
