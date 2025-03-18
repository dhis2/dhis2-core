/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.db.sql;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class PostgreSqlBuilderInheritanceIntegrationTest extends PostgresIntegrationTestBase {
  @Autowired private JdbcTemplate jdbcTemplate;

  private final SqlBuilder sqlBuilder = new PostgreSqlBuilder();

  private final List<Column> columns =
      List.of(
          new Column("id", DataType.BIGINT, Nullable.NOT_NULL),
          new Column("data", DataType.CHARACTER_11, Nullable.NOT_NULL),
          new Column("period", DataType.VARCHAR_50, Nullable.NOT_NULL),
          new Column("value", DataType.DOUBLE));

  private final List<String> primaryKey = List.of("id");

  private Table getTableA() {
    return new Table("data", columns, primaryKey);
  }

  private Table getTableB() {
    return new Table("data_staging", columns, primaryKey);
  }

  private Table getTableC() {
    return new Table("data_2022", columns, primaryKey);
  }

  private Table getTableD() {
    return new Table(
        "data_2023_staging", List.of(), List.of(), List.of(), Logged.UNLOGGED, getTableB());
  }

  @Test
  void testSetParentTable() {
    Table tableA = getTableA();
    Table tableC = getTableC();

    execute(sqlBuilder.createTable(tableA));
    execute(sqlBuilder.createTable(tableC));

    assertDoesNotThrow(() -> execute(sqlBuilder.setParentTable(tableC, "data")));

    jdbcTemplate.execute(sqlBuilder.dropTableIfExists(tableC));
    jdbcTemplate.execute(sqlBuilder.dropTableIfExists(tableA));
  }

  @Test
  void testRemoveParentTable() {
    Table tableB = getTableB();
    Table tableD = getTableD();

    execute(sqlBuilder.createTable(tableB));
    execute(sqlBuilder.createTable(tableD));

    assertDoesNotThrow(() -> execute(sqlBuilder.removeParentTable(tableD, "data_staging")));

    jdbcTemplate.execute(sqlBuilder.dropTableIfExists(tableD));
    jdbcTemplate.execute(sqlBuilder.dropTableIfExists(tableB));
  }

  @Test
  void testSwapParentTable() {
    Table tableA = getTableA();
    Table tableB = getTableB();
    Table tableD = getTableD();

    execute(sqlBuilder.createTable(tableA));
    execute(sqlBuilder.createTable(tableB));
    execute(sqlBuilder.createTable(tableD));

    assertDoesNotThrow(
        () -> execute(sqlBuilder.swapParentTable(getTableD(), "data_staging", "data")));

    jdbcTemplate.execute(sqlBuilder.dropTableIfExists(tableD));
    jdbcTemplate.execute(sqlBuilder.dropTableIfExists(tableB));
    jdbcTemplate.execute(sqlBuilder.dropTableIfExists(tableA));
  }

  private void execute(String sql) {
    jdbcTemplate.execute(sql);
  }
}
