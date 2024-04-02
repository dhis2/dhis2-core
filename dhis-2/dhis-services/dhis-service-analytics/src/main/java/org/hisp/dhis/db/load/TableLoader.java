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
import org.apache.commons.lang3.Validate;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.springframework.jdbc.core.JdbcTemplate;

public class TableLoader {
  private final Table table;

  private final List<Object[]> records;

  private final SqlBuilder sqlBuilder;

  private final JdbcTemplate jdbcTemplate;

  public TableLoader(
      Table table, List<Object[]> records, SqlBuilder sqlBuilder, JdbcTemplate jdbcTemplate) {
    this.table = table;
    this.records = records;
    this.sqlBuilder = sqlBuilder;
    this.jdbcTemplate = jdbcTemplate;
    this.validate();
  }

  /** Validates this object. */
  private void validate() {
    Objects.requireNonNull(table);
    Validate.notEmpty(table.getColumns());
    Objects.requireNonNull(records);
    Objects.requireNonNull(sqlBuilder);
    Objects.requireNonNull(jdbcTemplate);
  }

  private String getInsert() {
    String tableName = sqlBuilder.quote(table.getName());

    List<String> columnNames =
        table.getColumns().stream()
            .map(Column::getName)
            .map(name -> sqlBuilder.quote(name))
            .toList();

    String columns = String.join(",", columnNames);

    String insert = String.format("insert into %s (%s) values ", tableName, columns);

    List<String> rows = new ArrayList<>();

    return null;
  }

  /**
   * Determines whether single quoting of the given value and corresponding column is necessary, and
   * if so, returns the single quoted value as string, if not, the value as string.
   *
   * @param column the {@link Column}.
   * @param value the value.
   * @return the possibly quoted value as string.
   */
  String quoteValue(Column column, Object value) {
    if (value == null) {
      return null;
    }

    DataType dataType = column.getDataType();
    String string = String.valueOf(value);

    if (dataType.isNumeric() || dataType.isBoolean()) {
      return string;
    }

    return sqlBuilder.singleQuote(string);
  }
}
