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
package org.hisp.dhis.jdbc.batchhandler;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import org.hisp.quick.JdbcConfiguration;
import org.hisp.quick.batchhandler.AbstractBatchHandler;

/**
 * A batch handler that can write to any database table by using a value map for each row. The table
 * schema need not be known in the source.
 *
 * <p>If only inserting is desired, then only the JDBC configuration, table name, and list of
 * columns need to be specified. If other operations are desired, then other fields must be
 * specified as needed.
 *
 * @author Jim Grace
 */
@Builder
@Getter // Several getters override abstract methods from superclass
public class MappingBatchHandler extends AbstractBatchHandler<Map<String, Object>> {
  // Needed here only to include it in the builder
  private final JdbcConfiguration jdbcConfiguration;

  private final String tableName;

  private final List<String> columns;

  @Builder.Default private String autoIncrementColumn = null;

  @Builder.Default private boolean inclusiveUniqueColumns = true;

  @Builder.Default private List<String> identifierColumns = emptyList();

  @Builder.Default private List<String> uniqueColumns = emptyList();

  // -------------------------------------------------------------------------
  // Constructor
  // -------------------------------------------------------------------------

  // Lombok builder will use this constructor which must call super()
  public MappingBatchHandler(
      JdbcConfiguration jdbcConfiguration,
      String tableName,
      List<String> columns,
      String autoIncrementColumn,
      boolean inclusiveUniqueColumns,
      List<String> identifierColumns,
      List<String> uniqueColumns) {
    super(jdbcConfiguration);
    this.jdbcConfiguration = jdbcConfiguration;
    this.tableName = tableName;
    this.columns = columns;
    this.autoIncrementColumn = autoIncrementColumn;
    this.inclusiveUniqueColumns = inclusiveUniqueColumns;
    this.identifierColumns = identifierColumns;
    this.uniqueColumns = uniqueColumns;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  @Override
  public List<Object> getIdentifierValues(Map<String, Object> row) {
    return getValueList(identifierColumns, row);
  }

  @Override
  public List<Object> getUniqueValues(Map<String, Object> row) {
    return getValueList(uniqueColumns, row);
  }

  @Override
  public List<Object> getValues(Map<String, Object> row) {
    return getValueList(columns, row);
  }

  @Override
  public Map<String, Object> mapRow(ResultSet resultSet) throws SQLException {
    ResultSetMetaData metaData = resultSet.getMetaData();

    Map<String, Object> row = new HashMap<>();

    for (int i = 0; i < metaData.getColumnCount(); i++) {
      row.put(metaData.getColumnName(i), resultSet.getObject(i));
    }

    return row;
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private List<Object> getValueList(List<String> cols, Map<String, Object> row) {
    return cols.stream().map(row::get).collect(toList());
  }
}
