/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static org.hisp.dhis.analytics.table.model.AnalyticsValueType.DIMENSION;
import static org.hisp.dhis.analytics.table.model.AnalyticsValueType.FACT;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.db.model.DataType.CHARACTER_11;
import static org.hisp.dhis.db.model.DataType.DOUBLE;
import static org.hisp.dhis.db.model.DataType.TEXT;
import static org.hisp.dhis.db.model.constraint.Nullable.NOT_NULL;
import static org.hisp.dhis.db.model.constraint.Nullable.NULL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.List;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.model.AnalyticsValueType;
import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class AnalyticsTableTest {
  @Test
  void testGetTableName() {
    AnalyticsTable table =
        new AnalyticsTable(AnalyticsTableType.COMPLETENESS, List.of(), Logged.UNLOGGED);
    assertEquals("analytics_completeness", table.getMainName());
    assertEquals("analytics_completeness_temp", table.getName());
  }

  @Test
  void testGetTableNameWithProgram() {
    Program program = new Program("ProgramA", "DescriptionA");
    program.setUid("rfT56YbgFeK");
    AnalyticsTable table =
        new AnalyticsTable(AnalyticsTableType.EVENT, List.of(), Logged.UNLOGGED, program);
    assertEquals("analytics_event_rft56ybgfek", table.getMainName());
    assertEquals("analytics_event_rft56ybgfek_temp", table.getName());
  }

  @Test
  void testGetTableNameWithTrackedEntityType() {
    TrackedEntityType trackedEntityType = new TrackedEntityType();
    trackedEntityType.setUid("k7GfrBE3rT5");
    AnalyticsTable table =
        new AnalyticsTable(
            AnalyticsTableType.ENROLLMENT, List.of(), Logged.UNLOGGED, trackedEntityType);
    assertEquals("analytics_enrollment_k7gfrbe3rt5", table.getMainName());
    assertEquals("analytics_enrollment_k7gfrbe3rt5_temp", table.getName());
  }

  @Test
  void testGetDimensionAndFactColumns() {
    List<AnalyticsTableColumn> columns =
        List.of(
            new AnalyticsTableColumn(quote("dx"), CHARACTER_11, NOT_NULL, DIMENSION, "de.uid"),
            new AnalyticsTableColumn(quote("co"), CHARACTER_11, NOT_NULL, "co.uid"),
            new AnalyticsTableColumn(quote("ou"), CHARACTER_11, NOT_NULL, "ou.uid"),
            new AnalyticsTableColumn(quote("value"), DOUBLE, NULL, FACT, "value"),
            new AnalyticsTableColumn(quote("textvalue"), TEXT, NULL, FACT, "textvalue"));

    AnalyticsTable table =
        new AnalyticsTable(AnalyticsTableType.DATA_VALUE, columns, Logged.UNLOGGED);

    assertEquals(3, table.getDimensionColumns().size());
    assertEquals(quote("dx"), table.getDimensionColumns().get(0).getName());
    assertEquals(AnalyticsValueType.DIMENSION, table.getDimensionColumns().get(0).getValueType());
    assertEquals(quote("co"), table.getDimensionColumns().get(1).getName());
    assertEquals(AnalyticsValueType.DIMENSION, table.getDimensionColumns().get(1).getValueType());

    assertEquals(2, table.getFactColumns().size());
    assertEquals(quote("value"), table.getFactColumns().get(0).getName());
    assertEquals(AnalyticsValueType.FACT, table.getFactColumns().get(0).getValueType());
    assertEquals(quote("textvalue"), table.getFactColumns().get(1).getName());
    assertEquals(AnalyticsValueType.FACT, table.getFactColumns().get(1).getValueType());
  }

  @Test
  void testGetTablePartitionName() {
    Program program = new Program("ProgramA", "DescriptionA");
    program.setUid("EvxbPYWkrIa");
    Period periodA = new YearlyPeriodType().createPeriod(new DateTime(2014, 1, 1, 0, 0).toDate());
    Period periodB = new YearlyPeriodType().createPeriod(new DateTime(2015, 1, 1, 0, 0).toDate());
    AnalyticsTable tableA =
        new AnalyticsTable(AnalyticsTableType.EVENT, List.of(), Logged.UNLOGGED, program);
    tableA.addPartitionTable(2014, periodA.getStartDate(), periodA.getEndDate());
    tableA.addPartitionTable(2015, periodB.getStartDate(), periodB.getEndDate());
    AnalyticsTablePartition partitionA = tableA.getTablePartitions().get(0);
    AnalyticsTablePartition partitionB = tableA.getTablePartitions().get(1);
    assertNotNull(partitionA);
    assertNotNull(partitionB);
    assertEquals("analytics_event_evxbpywkria_2014", partitionA.getMainName());
    assertEquals("analytics_event_evxbpywkria_2015", partitionB.getMainName());
    assertEquals("analytics_event_evxbpywkria_2014_temp", partitionA.getName());
    assertEquals("analytics_event_evxbpywkria_2015_temp", partitionB.getName());
  }

  @Test
  void testEquals() {
    AnalyticsTable tableA =
        new AnalyticsTable(AnalyticsTableType.DATA_VALUE, List.of(), Logged.UNLOGGED);
    AnalyticsTable tableB =
        new AnalyticsTable(AnalyticsTableType.DATA_VALUE, List.of(), Logged.UNLOGGED);
    List<AnalyticsTable> uniqueList = new UniqueArrayList<>();
    uniqueList.add(tableA);
    uniqueList.add(tableB);
    assertEquals(1, uniqueList.size());
  }
}
