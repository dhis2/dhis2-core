/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static org.hisp.dhis.db.model.DataType.DOUBLE;
import static org.hisp.dhis.db.model.DataType.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Lars Helge Overland
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsTableServiceTest {
  @Mock private SystemSettingsProvider settingsProvider;

  @Mock private SystemSettings settings;

  @Mock private SqlBuilder sqlBuilder;

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

    List<String> sortKey = List.of("dx");

    AnalyticsTable tA =
        new AnalyticsTable(AnalyticsTableType.DATA_VALUE, columns, sortKey, Logged.UNLOGGED);
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
        new AnalyticsTable(AnalyticsTableType.ORG_UNIT_TARGET, columns, sortKey, Logged.UNLOGGED);
    List<AnalyticsTablePartition> partitions = tableService.getTablePartitions(List.of(tA, tB));

    assertEquals(3, partitions.size());
  }

  @Test
  void testGetParallelJobsA() {
    when(settingsProvider.getCurrentSettings()).thenReturn(settings);
    when(settings.getParallelJobsInAnalyticsTableExport()).thenReturn(1);

    assertEquals(1, tableService.getParallelJobs());
  }

  @Test
  void testGetParallelJobsB() {
    when(settingsProvider.getCurrentSettings()).thenReturn(settings);
    when(settings.getParallelJobsInAnalyticsTableExport()).thenReturn(-1);
    when(settings.getDatabaseServerCpus()).thenReturn(8);

    assertEquals(8, tableService.getParallelJobs());
  }
}
