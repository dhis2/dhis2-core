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
package org.hisp.dhis.db.sql;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.db.model.Collation;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class PostgreSqlBuilderIntegrationTest extends IntegrationTestBase {
  @Autowired private JdbcTemplate jdbcTemplate;

  private SqlBuilder sqlBuilder = new PostgreSqlBuilder();

  private Table getTableA() {
    List<Column> columns =
        List.of(
            new Column("id", DataType.BIGINT, Nullable.NOT_NULL),
            new Column("data", DataType.CHARACTER_11, Nullable.NOT_NULL),
            new Column("period", DataType.VARCHAR_50, Nullable.NOT_NULL),
            new Column("created", DataType.TIMESTAMP),
            new Column("value", DataType.DOUBLE));

    List<String> primaryKey = List.of("id");

    List<Index> indexes =
        List.of(
            new Index("in_immunization_data", List.of("data")),
            new Index("in_immunization_period", List.of("period", "created")));

    return new Table("immunization", columns, primaryKey, indexes);
  }

  private Table getTableB() {
    List<Column> columns =
        List.of(
            new Column("id", DataType.INTEGER, Nullable.NOT_NULL),
            new Column("facility_type", DataType.VARCHAR_255, Nullable.NULL, Collation.C),
            new Column("bcg_doses", DataType.DOUBLE));

    List<String> checks = List.of("\"id\">0", "\"bcg_doses\">0");

    return new Table("vaccination", columns, List.of(), List.of(), checks, Logged.UNLOGGED);
  }

  @Test
  void testCreateAndDropTableA() {
    Table table = getTableA();

    execute(sqlBuilder.createTable(table));

    assertTrue(tableExists(table.getName()));

    assertDoesNotThrow(() -> execute(sqlBuilder.vacuumTable(table)));

    assertDoesNotThrow(() -> execute(sqlBuilder.analyzeTable(table)));

    jdbcTemplate.execute(sqlBuilder.dropTableIfExists(table));

    assertFalse(tableExists(table.getName()));
  }

  @Test
  void testCreateAndDropTableB() {
    Table table = getTableB();

    execute(sqlBuilder.createTable(table));

    assertTrue(tableExists(table.getName()));

    jdbcTemplate.execute(sqlBuilder.dropTableIfExists(table));
  }

  @Test
  void testCreateIndex() {
    Table table = getTableA();

    execute(sqlBuilder.createTable(table));

    assertDoesNotThrow(() -> execute(sqlBuilder.createIndex(table, table.getIndexes().get(0))));

    assertDoesNotThrow(() -> execute(sqlBuilder.createIndex(table, table.getIndexes().get(1))));

    jdbcTemplate.execute(sqlBuilder.dropTableIfExists(table));
  }

  private boolean tableExists(String name) {
    return jdbcTemplate.queryForList(sqlBuilder.tableExists(name), String.class).size() == 1;
  }

  private void execute(String sql) {
    jdbcTemplate.execute(sql);
  }
}
