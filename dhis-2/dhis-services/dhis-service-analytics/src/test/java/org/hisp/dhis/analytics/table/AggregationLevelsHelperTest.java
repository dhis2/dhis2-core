/*
 * Copyright (c) 2004-2025, University of Oslo
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.sql.DorisSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class AggregationLevelsHelperTest {

  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private Table mockTable;

  @Mock private Table parentTable;

  private AggregationLevelsHelper helper;

  @Test
  void applyAggregationLevels_withPostgreSQL_executesCorrectUpdateSql() {
    when(mockTable.getName()).thenReturn("analytics_table");
    helper = new AggregationLevelsHelper(jdbcTemplate, new PostgreSqlBuilder());
    List<String> dataElements = Arrays.asList("de1", "de2", "de3");
    int aggregationLevel = 3;

    // Execute
    helper.applyAggregationLevels(mockTable, dataElements, aggregationLevel, true);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(jdbcTemplate).execute(sqlCaptor.capture());

    String capturedSql = sqlCaptor.getValue();
    System.out.println(capturedSql);

    // Verify SQL format (for PostgreSQL update)
    assertTrue(capturedSql.contains("update analytics_table set"));
    assertTrue(capturedSql.contains("\"uidlevel1\" = null"));
    assertTrue(capturedSql.contains("\"uidlevel2\" = null"));
    assertTrue(capturedSql.contains("\"uidlevel3\" = null"));
    assertTrue(capturedSql.contains("where oulevel > 3"));
    assertTrue(capturedSql.contains("and dx in ( 'de1', 'de2', 'de3' )"));
  }

  @Test
  void applyAggregationLevels_withDoris_executesCorrectInsertDeleteSql() {
    when(mockTable.getName()).thenReturn("analytics_table");
    helper = new AggregationLevelsHelper(jdbcTemplate, new DorisSqlBuilder("", ""));
    List<String> dataElements = Arrays.asList("de1", "de2");
    int aggregationLevel = 2;
    setupTableWithColumns(mockTable);

    // Mock fetching partitions
    List<String> partitions = Arrays.asList("p1", "p2");
    when(jdbcTemplate.query(eq("show partitions from `analytics_table`"), any(RowMapper.class)))
        .thenReturn(partitions);

    // Execute
    helper.applyAggregationLevels(mockTable, dataElements, aggregationLevel, false);

    // Verify the sequence of SQL executions
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(jdbcTemplate, atLeast(5)).execute(sqlCaptor.capture());

    List<String> capturedSqls = sqlCaptor.getAllValues();

    // Verify the stages of Doris path (create staging, insert, delete, insert back, drop)
    assertTrue(
        capturedSqls.stream()
            .anyMatch(sql -> sql.contains("create table") && sql.contains("_staging_")));
    assertTrue(
        capturedSqls.stream()
            .anyMatch(
                sql ->
                    sql.contains("insert into")
                        && sql.contains("select")
                        && sql.contains("where oulevel > 2")
                        && sql.contains("and dx in ('de1', 'de2')")));
    assertTrue(
        capturedSqls.stream()
            .anyMatch(
                sql ->
                    sql.contains("delete from")
                        && sql.contains("where oulevel > 2")
                        && sql.contains("and dx in ('de1', 'de2')")));
    assertTrue(
        capturedSqls.stream()
            .anyMatch(
                sql -> sql.contains("insert into `analytics_table`") && sql.contains("select")));
    assertTrue(
        capturedSqls.stream()
            .anyMatch(sql -> sql.contains("drop table") && sql.contains("_staging_")));
  }

  @Test
  void applyAggregationLevels_withDoris_nonPartitionedTable_handlesCorrectly() {
    when(mockTable.getName()).thenReturn("analytics_table");
    helper = new AggregationLevelsHelper(jdbcTemplate, new DorisSqlBuilder("", ""));
    // Setup
    List<String> dataElements = Arrays.asList("de1", "de2");
    int aggregationLevel = 2;
    setupTableWithColumns(mockTable);

    // Mock empty partitions (non-partitioned table)
    when(jdbcTemplate.query(eq("show partitions from `analytics_table`"), any(RowMapper.class)))
        .thenReturn(Collections.emptyList());

    // Execute
    helper.applyAggregationLevels(mockTable, dataElements, aggregationLevel, false);

    // Verify delete SQL without partition clause
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(jdbcTemplate, atLeast(5)).execute(sqlCaptor.capture());

    List<String> capturedSqls = sqlCaptor.getAllValues();
    assertTrue(
        capturedSqls.stream()
            .anyMatch(
                sql ->
                    sql.contains("delete from `analytics_table` where oulevel > 2")
                        && !sql.contains("partition")));
  }

  @Test
  void applyAggregationLevels_withEmptyDataElements_handlesGracefully() {
    helper = new AggregationLevelsHelper(jdbcTemplate, new DorisSqlBuilder("", ""));
    List<String> emptyDataElements = Collections.emptyList();
    int aggregationLevel = 3;
    helper.applyAggregationLevels(mockTable, emptyDataElements, aggregationLevel, true);
    // Verify that no SQL execution occurs
    verify(jdbcTemplate, atLeast(0)).execute(any(String.class));
  }

  @Test
  void applyAggregationLevels_withNoColumnsAndNoParent_throwsException() {
    helper = new AggregationLevelsHelper(jdbcTemplate, new DorisSqlBuilder("", ""));
    List<String> dataElements = Arrays.asList("de1", "de2");
    int aggregationLevel = 2;

    // Setup table with no columns and no parent
    when(mockTable.getColumns()).thenReturn(Collections.emptyList());
    when(mockTable.getParent()).thenReturn(null);

    // Execute & Verify
    assertThrows(
        IllegalQueryException.class,
        () -> helper.applyAggregationLevels(mockTable, dataElements, aggregationLevel, false));
  }

  @Test
  void applyAggregationLevels_withParentTable_usesParentColumns() {
    // Setup
    helper = new AggregationLevelsHelper(jdbcTemplate, new DorisSqlBuilder("", ""));
    List<String> dataElements = Arrays.asList("de1", "de2");
    int aggregationLevel = 2;

    // Setup parent table with columns
    setupTableWithColumns(parentTable);
    when(mockTable.getName()).thenReturn("analytics_table");

    // Setup main table with no columns but with parent
    when(mockTable.getColumns()).thenReturn(Collections.emptyList());
    when(mockTable.getParent()).thenReturn(parentTable);

    // Mock empty partitions for Doris path
    when(jdbcTemplate.query(eq("show partitions from `analytics_table`"), any(RowMapper.class)))
        .thenReturn(Collections.emptyList());

    // Execute
    helper.applyAggregationLevels(mockTable, dataElements, aggregationLevel, false);

    // Verify parent table columns were used
    verify(parentTable, atLeastOnce()).getColumns();
  }

  private void setupTableWithColumns(Table table) {
    List<Column> columns = new ArrayList<>();
    columns.add(createColumn("dx"));
    columns.add(createColumn("level1"));
    columns.add(createColumn("level2"));
    columns.add(createColumn("level3"));
    columns.add(createColumn("oulevel"));

    when(table.getColumns()).thenReturn(columns);
  }

  /** Helper method to create a mock Column object */
  private Column createColumn(String name) {
    Column column = mock(Column.class);
    when(column.getName()).thenReturn(name);
    return column;
  }
}
