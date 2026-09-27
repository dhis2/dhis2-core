/*
 * Copyright (c) 2004-2023, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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

import static org.hisp.dhis.db.model.DataType.DOUBLE;
import static org.hisp.dhis.db.model.DataType.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.analytics.AnalyticsTableManager;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.scheduling.RecordingJobProgress;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Lars Helge Overland
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsTableServiceTest {

  @Mock private SystemSettingManager systemSettingManager;

  @Mock private SqlBuilder sqlBuilder;

  @Mock private AnalyticsTableManager tableManager;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private DataElementService dataElementService;

  @Mock private ResourceTableService resourceTableService;

  @InjectMocks private DefaultAnalyticsTableService tableService;

  @Test
  void testGetTablePartitions() {
    when(sqlBuilder.supportsDeclarativePartitioning()).thenReturn(false);

    List<AnalyticsTableColumn> columns =
        List.of(
            AnalyticsTableColumn.builder().name("dx").dataType(TEXT).selectExpression("dx").build(),
            AnalyticsTableColumn.builder()
                .name("value")
                .dataType(DOUBLE)
                .selectExpression("value")
                .build());

    AnalyticsTable tA = new AnalyticsTable(AnalyticsTableType.DATA_VALUE, columns, Logged.UNLOGGED);
    tA.addTablePartition(
        List.of(),
        2010,
        new DateTime(2010, 1, 1, 0, 0).toDate(),
        new DateTime(2010, 12, 31, 0, 0).toDate());
    tA.addTablePartition(
        List.of(),
        2011,
        new DateTime(2011, 1, 1, 0, 0).toDate(),
        new DateTime(2011, 12, 31, 0, 0).toDate());
    AnalyticsTable tB =
        new AnalyticsTable(AnalyticsTableType.ORG_UNIT_TARGET, columns, Logged.UNLOGGED);
    List<AnalyticsTablePartition> partitions = tableService.getTablePartitions(List.of(tA, tB));

    assertEquals(3, partitions.size());
  }

  @Test
  void testGetParallelJobsA() {
    when(systemSettingManager.getIntegerSetting(SettingKey.PARALLEL_JOBS_IN_ANALYTICS_TABLE_EXPORT))
        .thenReturn(1);
    when(systemSettingManager.getIntegerSetting(SettingKey.DATABASE_SERVER_CPUS)).thenReturn(8);

    assertEquals(1, tableService.getParallelJobs());
  }

  @Test
  void testGetParallelJobsB() {
    when(systemSettingManager.getIntegerSetting(SettingKey.PARALLEL_JOBS_IN_ANALYTICS_TABLE_EXPORT))
        .thenReturn(null);
    when(systemSettingManager.getIntegerSetting(SettingKey.DATABASE_SERVER_CPUS)).thenReturn(8);

    assertEquals(8, tableService.getParallelJobs());
  }

  @Test
  void testRemovesLatestPartitionOverlapForLastYearsUpdate() {
    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder().withLastYears(5).build();

    createTables(params);

    verify(tableManager).removeLatestPartitionOverlap(anyList());
  }

  @Test
  void testRetainsLatestPartitionForFullUpdate() {
    AnalyticsTableUpdateParams params = AnalyticsTableUpdateParams.newBuilder().build();

    createTables(params);

    verify(tableManager, never()).removeLatestPartitionOverlap(anyList());
  }

  @Test
  void testRetainsLatestPartitionForLatestUpdate() {
    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder().withLatestPartition().build();

    createTables(params);

    verify(tableManager, never()).removeLatestPartitionOverlap(anyList());
  }

  @Test
  void testRemovesLatestPartitionOverlapAfterSwappingTables() {
    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder().withLastYears(5).build();

    createTables(params);

    InOrder inOrder = inOrder(tableManager);
    inOrder.verify(tableManager).swapTable(eq(params), any(AnalyticsTable.class));
    inOrder.verify(tableManager).removeLatestPartitionOverlap(anyList());
  }

  @Test
  void testLatestPartitionOverlapFailureAbortsUpdate() {
    RecordingJobProgress progress =
        new RecordingJobProgress(new JobConfiguration(JobType.ANALYTICS_TABLE));

    doThrow(new IllegalStateException("Delete failed"))
        .when(tableManager)
        .removeLatestPartitionOverlap(anyList());

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder().withLastYears(5).build();

    createTables(params, progress);

    assertTrue(
        progress.isCancelled(),
        "A failed latest partition overlap removal must abort the table update, so that the "
            + "last successful update settings are not advanced while duplicate data is live");
  }

  /** Runs a table update with the given parameters against a single partitioned table. */
  private void createTables(AnalyticsTableUpdateParams params) {
    createTables(params, NoopJobProgress.INSTANCE);
  }

  /** Runs a table update with the given parameters against a single partitioned table. */
  private void createTables(AnalyticsTableUpdateParams params, JobProgress progress) {
    when(systemSettingManager.getIntegerSetting(SettingKey.PARALLEL_JOBS_IN_ANALYTICS_TABLE_EXPORT))
        .thenReturn(1);
    when(sqlBuilder.supportsDeclarativePartitioning()).thenReturn(false);
    when(tableManager.getAnalyticsTableType()).thenReturn(AnalyticsTableType.DATA_VALUE);
    when(tableManager.validState()).thenReturn(true);
    when(tableManager.getAnalyticsTables(params)).thenReturn(List.of(dataValueTable()));

    tableService.create(params, progress);
  }

  private AnalyticsTable dataValueTable() {
    List<AnalyticsTableColumn> columns =
        List.of(
            AnalyticsTableColumn.builder()
                .name("dx")
                .dataType(TEXT)
                .selectExpression("dx")
                .build());

    AnalyticsTable table =
        new AnalyticsTable(AnalyticsTableType.DATA_VALUE, columns, Logged.UNLOGGED);
    table.addTablePartition(
        List.of("year = 2022"),
        2022,
        new DateTime(2022, 1, 1, 0, 0).toDate(),
        new DateTime(2023, 1, 1, 0, 0).toDate());

    return table;
  }
}
