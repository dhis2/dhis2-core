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
package org.hisp.dhis.analytics.table.model;

import static org.hisp.dhis.analytics.table.model.AnalyticsValueType.DIMENSION;
import static org.hisp.dhis.analytics.table.model.AnalyticsValueType.FACT;
import static org.hisp.dhis.db.model.DataType.BIGINT;
import static org.hisp.dhis.db.model.DataType.CHARACTER_11;
import static org.hisp.dhis.db.model.DataType.DOUBLE;
import static org.hisp.dhis.db.model.DataType.TEXT;
import static org.hisp.dhis.db.model.constraint.Nullable.NOT_NULL;
import static org.hisp.dhis.db.model.constraint.Nullable.NULL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.analytics.AnalyticsTableType;
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
  private final List<AnalyticsTableColumn> columnsA =
      List.of(
          AnalyticsTableColumn.builder().name("id").dataType(BIGINT).selectExpression("id").build(),
          AnalyticsTableColumn.builder()
              .name("data")
              .dataType(CHARACTER_11)
              .selectExpression("data")
              .build(),
          AnalyticsTableColumn.builder()
              .name("value")
              .dataType(DOUBLE)
              .selectExpression("value")
              .build());

  private final List<String> sortKeyA = List.of("data");

  @Test
  void testConstructor() {
    AnalyticsTable table =
        new AnalyticsTable(AnalyticsTableType.DATA_VALUE, columnsA, sortKeyA, Logged.UNLOGGED);

    assertEquals(AnalyticsTableType.DATA_VALUE, table.getTableType());
    assertTrue(table.getPrimaryKey().isEmpty());
    assertEquals(sortKeyA, table.getSortKey());
    assertTrue(table.getChecks().isEmpty());
    assertEquals(Logged.UNLOGGED, table.getLogged());
  }

  @Test
  void testGetTableNameDataValue() {
    AnalyticsTable table =
        new AnalyticsTable(AnalyticsTableType.DATA_VALUE, columnsA, sortKeyA, Logged.UNLOGGED);
    assertEquals("analytics", table.getMainName());
    assertEquals("analytics_temp", table.getName());
  }

  @Test
  void testGetTableNameCompleteness() {
    AnalyticsTable table =
        new AnalyticsTable(AnalyticsTableType.COMPLETENESS, columnsA, sortKeyA, Logged.UNLOGGED);
    assertEquals("analytics_completeness", table.getMainName());
    assertEquals("analytics_completeness_temp", table.getName());
  }

  @Test
  void testGetTableNameValidationResult() {
    AnalyticsTable table =
        new AnalyticsTable(
            AnalyticsTableType.VALIDATION_RESULT, columnsA, sortKeyA, Logged.UNLOGGED);
    assertEquals("analytics_validationresult", table.getMainName());
    assertEquals("analytics_validationresult_temp", table.getName());
  }

  @Test
  void testGetTableNameWithProgram() {
    Program program = new Program("ProgramA", "DescriptionA");
    program.setUid("rfT56YbgFeK");
    AnalyticsTable table =
        new AnalyticsTable(AnalyticsTableType.EVENT, columnsA, Logged.UNLOGGED, program);
    assertEquals("analytics_event_rft56ybgfek", table.getMainName());
    assertEquals("analytics_event_rft56ybgfek_temp", table.getName());
  }

  @Test
  void testGetTableNameWithTrackedEntityType() {
    TrackedEntityType trackedEntityType = new TrackedEntityType();
    trackedEntityType.setUid("k7GfrBE3rT5");
    AnalyticsTable table =
        new AnalyticsTable(
            AnalyticsTableType.ENROLLMENT, columnsA, Logged.UNLOGGED, trackedEntityType);
    assertEquals("analytics_enrollment_k7gfrbe3rt5", table.getMainName());
    assertEquals("analytics_enrollment_k7gfrbe3rt5_temp", table.getName());
  }

  @Test
  void testGetDimensionAndFactColumns() {
    List<AnalyticsTableColumn> columns =
        List.of(
            AnalyticsTableColumn.builder()
                .name("dx")
                .dataType(CHARACTER_11)
                .nullable(NOT_NULL)
                .valueType(DIMENSION)
                .selectExpression("de.uid")
                .build(),
            AnalyticsTableColumn.builder()
                .name("co")
                .dataType(CHARACTER_11)
                .nullable(NOT_NULL)
                .selectExpression("co.uid")
                .build(),
            AnalyticsTableColumn.builder()
                .name("ou")
                .dataType(CHARACTER_11)
                .nullable(NOT_NULL)
                .selectExpression("ou.uid")
                .build(),
            AnalyticsTableColumn.builder()
                .name("value")
                .dataType(DOUBLE)
                .nullable(NULL)
                .valueType(FACT)
                .selectExpression("value")
                .build(),
            AnalyticsTableColumn.builder()
                .name("textvalue")
                .dataType(TEXT)
                .nullable(NULL)
                .valueType(FACT)
                .selectExpression("textvalue")
                .build());

    AnalyticsTable table =
        new AnalyticsTable(AnalyticsTableType.DATA_VALUE, columns, List.of(), Logged.UNLOGGED);

    assertEquals(3, table.getDimensionColumns().size());
    assertEquals("dx", table.getDimensionColumns().get(0).getName());
    assertEquals(AnalyticsValueType.DIMENSION, table.getDimensionColumns().get(0).getValueType());
    assertEquals("co", table.getDimensionColumns().get(1).getName());
    assertEquals(AnalyticsValueType.DIMENSION, table.getDimensionColumns().get(1).getValueType());

    assertEquals(2, table.getFactColumns().size());
    assertEquals("value", table.getFactColumns().get(0).getName());
    assertEquals(AnalyticsValueType.FACT, table.getFactColumns().get(0).getValueType());
    assertEquals("textvalue", table.getFactColumns().get(1).getName());
    assertEquals(AnalyticsValueType.FACT, table.getFactColumns().get(1).getValueType());
  }

  @Test
  void testGetTablePartitionName() {
    Program program = new Program("ProgramA", "DescriptionA");
    program.setUid("EvxbPYWkrIa");
    Period periodA = new YearlyPeriodType().createPeriod(new DateTime(2014, 1, 1, 0, 0).toDate());
    Period periodB = new YearlyPeriodType().createPeriod(new DateTime(2015, 1, 1, 0, 0).toDate());
    AnalyticsTable tableA =
        new AnalyticsTable(AnalyticsTableType.EVENT, columnsA, Logged.UNLOGGED, program);
    tableA.addTablePartition(List.of(), 2014, periodA.getStartDate(), periodA.getEndDate());
    tableA.addTablePartition(List.of(), 2015, periodB.getStartDate(), periodB.getEndDate());
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
        new AnalyticsTable(AnalyticsTableType.DATA_VALUE, columnsA, sortKeyA, Logged.UNLOGGED);
    AnalyticsTable tableB =
        new AnalyticsTable(AnalyticsTableType.DATA_VALUE, columnsA, sortKeyA, Logged.UNLOGGED);
    List<AnalyticsTable> uniqueList = new UniqueArrayList<>();
    uniqueList.add(tableA);
    uniqueList.add(tableB);
    assertEquals(1, uniqueList.size());
  }
}
