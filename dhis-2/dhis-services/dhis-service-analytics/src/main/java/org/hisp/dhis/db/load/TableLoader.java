/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.db.load;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.Validate;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Component responsible for batch loading data into a table.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TableLoader {
  private static final int PARTITION_SIZE = 200;
  private static final String COMMA = ",";

  @Qualifier("analyticsJdbcTemplate")
  private final JdbcTemplate jdbcTemplate;

  private final SqlBuilder sqlBuilder;

  /**
   * Loads the given data rows into the given table.
   *
   * @param table the {@link Table}.
   * @param data the data rows.
   */
  public void load(Table table, List<Object[]> data) {
    Objects.requireNonNull(table);
    Objects.requireNonNull(data);

    if (data.isEmpty()) {
      return;
    }

    List<String> valueStatements = getValueSql(table, data);

    List<List<String>> valuePartitions = ListUtils.partition(valueStatements, PARTITION_SIZE);

    log.info("Loading {} data rows from {} partitions", data.size(), valuePartitions.size());

    for (List<String> partition : valuePartitions) {
      String sql = getInsertValueSql(table, partition);
      jdbcTemplate.execute(sql);
    }
  }

  /**
   * Returns an insert value SQL statement.
   *
   * @param table the {@link Table}.
   * @param valueStatements the list of value SQL statements.
   * @return an insert value SQL statement.
   */
  String getInsertValueSql(Table table, List<String> valueStatements) {
    String insertSql = getInsertSql(table);
    String valueSql = String.join(COMMA, valueStatements);
    return String.format("%s%s;", insertSql, valueSql);
  }

  /**
   * Returns an insert SQL clause for the given table.
   *
   * @param table the {@link Table}.
   * @return an insert SQL clause.
   */
  String getInsertSql(Table table) {
    List<String> columnNames =
        table.getColumns().stream()
            .map(Column::getName)
            .map(name -> sqlBuilder.quote(name))
            .toList();

    String tableName = sqlBuilder.quote(table.getName());
    String columns = String.join(COMMA, columnNames);

    return String.format("insert into %s (%s) values ", tableName, columns);
  }

  /**
   * Returns a list of value SQL clauses.
   *
   * @param table the {@link Table}.
   * @param data the data rows.
   * @return a list of value SQL clauses.
   */
  List<String> getValueSql(Table table, List<Object[]> data) {
    return data.stream().map(row -> getValueSql(table, row)).toList();
  }

  /**
   * Returns a values SQL clause for the given row value objects.
   *
   * @param table the {@link Table}.
   * @param row the row value objects.
   * @return a values SQL clause.
   */
  String getValueSql(Table table, Object[] row) {
    int columnCount = table.getColumns().size();

    Validate.isTrue(
        row.length == columnCount,
        String.format("Column row count mismatch: %d/%d", row.length, columnCount));

    List<String> values = new ArrayList<>();

    for (int i = 0; i < columnCount; i++) {
      Column column = table.getColumns().get(i);
      values.add(singleQuoteValue(column.getDataType(), row[i]));
    }

    return String.format("(%s)", String.join(COMMA, values));
  }

  /**
   * Determines whether single quoting of the given value and corresponding column is necessary, and
   * if so, returns the single quoted value as string, if not, the value as string.
   *
   * @param dataType the {@link DataType}.
   * @param value the value.
   * @return the possibly quoted value as string.
   */
  String singleQuoteValue(DataType dataType, Object value) {
    if (value == null) {
      return null;
    }

    String string = String.valueOf(value);

    if (dataType.isNumeric() || dataType.isBoolean()) {
      return string;
    }

    return sqlBuilder.singleQuote(string);
  }
}
