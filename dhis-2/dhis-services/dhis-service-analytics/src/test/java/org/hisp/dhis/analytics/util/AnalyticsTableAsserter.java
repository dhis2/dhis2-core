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
package org.hisp.dhis.analytics.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.Skip;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.IndexType;

/**
 * @author Luciano Fiandesio
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AnalyticsTableAsserter {
  /** The analytics table to verify. */
  private AnalyticsTable table;

  private int columnsSize;

  private List<AnalyticsTableColumn> defaultColumns;

  private List<AnalyticsTableColumn> columns;

  private Map<String, Consumer<AnalyticsTableColumn>> matchers;

  private AnalyticsTableType tableType;

  private String name;

  private String mainName;

  public void verify() {
    // verify column size
    assertThat(table.getDimensionColumns(), hasSize(columnsSize));
    assertThat(table.getTableType(), is(tableType));
    assertThat(table.getName(), is(name));
    assertThat(table.getMainName(), is(mainName));
    // verify default columns
    Map<String, AnalyticsTableColumn> tableColumnMap =
        table.getAnalyticsTableColumns().stream()
            .collect(Collectors.toMap(AnalyticsTableColumn::getName, c -> c));

    for (AnalyticsTableColumn col : defaultColumns) {
      if (!tableColumnMap.containsKey(col.getName())) {
        fail(
            "Default column '" + col.getName() + "' is missing, found: " + tableColumnMap.keySet());
      } else {
        new AnalyticsColumnAsserter.Builder(col).build().verify(tableColumnMap.get(col.getName()));
      }
    }

    for (AnalyticsTableColumn col : columns) {
      if (!tableColumnMap.containsKey(col.getName())) {
        fail("Column '" + col.getName() + "' is missing, found: " + tableColumnMap.keySet());
      } else {
        new AnalyticsColumnAsserter.Builder(col).build().verify(tableColumnMap.get(col.getName()));
      }
    }

    for (String name : matchers.keySet()) {
      if (!tableColumnMap.containsKey(name)) {
        fail("Column '" + name + "' is missing, found: " + tableColumnMap.keySet());
      } else {
        matchers.get(name).accept(tableColumnMap.get(name));
      }
    }
  }

  public static class Builder {

    private AnalyticsTable _table;

    private int _columnSize;

    private List<AnalyticsTableColumn> _defaultColumns;

    private List<AnalyticsTableColumn> _columns = new ArrayList<>();

    private Map<String, Consumer<AnalyticsTableColumn>> _matchers = new HashMap<>();

    private AnalyticsTableType _tableType;

    private String _name;

    private String _mainName;

    public Builder(AnalyticsTable analyticsTable) {
      _table = analyticsTable;
    }

    public Builder withColumnSize(int columnSize) {
      _columnSize = columnSize;
      return this;
    }

    public Builder withDefaultColumns(List<AnalyticsTableColumn> defaultColumns) {
      _defaultColumns = defaultColumns;
      return this;
    }

    public Builder withTableType(AnalyticsTableType analyticsTableType) {
      _tableType = analyticsTableType;
      return this;
    }

    public Builder withName(String name) {
      _name = name;
      return this;
    }

    public Builder withMainName(String mainName) {
      _mainName = mainName;
      return this;
    }

    public Builder addColumn(
        String name, DataType dataType, String selectExpression, Date created) {
      AnalyticsTableColumn col =
          AnalyticsTableColumn.builder()
              .name(name)
              .dataType(dataType)
              .selectExpression(selectExpression)
              .created(created)
              .build();
      this._columns.add(col);
      return this;
    }

    public Builder addColumn(String name, DataType dataType, String selectExpression) {
      return addColumn(name, dataType, selectExpression, Skip.INCLUDE, IndexType.BTREE);
    }

    public Builder addColumn(
        String name, DataType dataType, String selectExpression, IndexType indexType) {
      return addColumn(name, dataType, selectExpression, Skip.INCLUDE, indexType);
    }

    public Builder addColumn(
        String name, DataType dataType, String selectExpression, Skip skipIndex) {
      return addColumn(name, dataType, selectExpression, skipIndex, IndexType.BTREE);
    }

    public Builder addColumn(
        String name,
        DataType dataType,
        String selectExpression,
        Skip skipIndex,
        IndexType indexType) {
      AnalyticsTableColumn col =
          Skip.SKIP == skipIndex
              ? AnalyticsTableColumn.builder()
                  .name(name)
                  .dataType(dataType)
                  .selectExpression(selectExpression)
                  .skipIndex(skipIndex)
                  .build()
              : AnalyticsTableColumn.builder()
                  .name(name)
                  .dataType(dataType)
                  .selectExpression(selectExpression)
                  .indexType(indexType)
                  .build();
      this._columns.add(col);
      return this;
    }

    public Builder addColumns(List<AnalyticsTableColumn> columns) {
      this._columns.addAll(columns);
      return this;
    }

    public Builder addColumn(String name, Consumer<AnalyticsTableColumn> matcher) {
      this._matchers.put(name, matcher);
      return this;
    }

    public AnalyticsTableAsserter build() {
      if (_tableType == null) {
        fail("Missing table type");
      }
      AnalyticsTableAsserter asserter = new AnalyticsTableAsserter();
      asserter.table = _table;
      asserter.columnsSize = _columnSize;
      asserter.defaultColumns = _defaultColumns;
      asserter.tableType = _tableType;
      asserter.columns = _columns;
      asserter.matchers = _matchers;
      asserter.name = _name;
      asserter.mainName = _mainName;
      return asserter;
    }
  }
}
