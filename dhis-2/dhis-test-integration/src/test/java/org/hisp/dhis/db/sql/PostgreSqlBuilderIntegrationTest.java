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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.db.model.Collation;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.IndexFunction;
import org.hisp.dhis.db.model.IndexType;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class PostgreSqlBuilderIntegrationTest extends PostgresIntegrationTestBase {
  @Autowired private JdbcTemplate jdbcTemplate;

  private final SqlBuilder sqlBuilder = new PostgreSqlBuilder();

  private Table getTableA() {
    List<Column> columns =
        List.of(
            new Column("id", DataType.BIGINT, Nullable.NOT_NULL),
            new Column("data", DataType.CHARACTER_11, Nullable.NOT_NULL),
            new Column("period", DataType.VARCHAR_50, Nullable.NOT_NULL),
            new Column("created", DataType.TIMESTAMP),
            new Column("user", DataType.JSONB),
            new Column("value", DataType.DOUBLE));

    List<String> primaryKey = List.of("id");

    return new Table("immunization", columns, primaryKey);
  }

  private List<Index> getIndexesA() {

    return List.of(
        Index.builder()
            .name("in_immunization_data")
            .tableName("immunization")
            .columns(List.of("data"))
            .build(),
        Index.builder()
            .name("in_immunization_period_created")
            .tableName("immunization")
            .columns(List.of("period", "created"))
            .build(),
        Index.builder()
            .name("in_immunization_user")
            .tableName("immunization")
            .indexType(IndexType.GIN)
            .columns(List.of("user"))
            .build(),
        Index.builder()
            .name("in_immunization_data_period")
            .tableName("immunization")
            .columns(List.of("data", "period"))
            .function(IndexFunction.LOWER)
            .build());
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

  private Table getTableC() {
    List<Column> columns =
        List.of(new Column("vitamin_a", DataType.BIGINT), new Column("vitamin_d", DataType.BIGINT));

    return new Table("nutrition", columns, List.of(), List.of(), Logged.LOGGED, getTableB());
  }

  private Table getTableD() {
    List<Column> columns =
        List.of(
            new Column("a", DataType.SMALLINT),
            new Column("b", DataType.BIGINT),
            new Column("c", DataType.INTEGER),
            new Column("d", DataType.DECIMAL),
            new Column("e", DataType.FLOAT),
            new Column("f", DataType.DOUBLE),
            new Column("g", DataType.BOOLEAN),
            new Column("h", DataType.CHARACTER_11),
            new Column("i", DataType.VARCHAR_50),
            new Column("j", DataType.VARCHAR_255),
            new Column("k", DataType.TEXT),
            new Column("l", DataType.DATE),
            new Column("m", DataType.TIMESTAMP),
            new Column("n", DataType.TIMESTAMPTZ),
            new Column("o", DataType.GEOMETRY),
            new Column("p", DataType.GEOMETRY_POINT),
            new Column("q", DataType.JSONB));

    return new Table("a", columns, List.of());
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
  void testCreateAndDropTableC() {
    Table tableB = getTableB();
    Table tableC = getTableC();
    Table tableD = getTableD();

    execute(sqlBuilder.createTable(tableB));
    execute(sqlBuilder.createTable(tableC));
    execute(sqlBuilder.createTable(tableD));

    assertTrue(tableExists(tableB.getName()));
    assertTrue(tableExists(tableC.getName()));
    assertTrue(tableExists(tableD.getName()));

    jdbcTemplate.execute(sqlBuilder.dropTableIfExists(tableD));
    jdbcTemplate.execute(sqlBuilder.dropTableIfExists(tableC));
    jdbcTemplate.execute(sqlBuilder.dropTableIfExists(tableB));
  }

  @Test
  void testCreateAndDropTableCascade() {
    Table table = getTableA();

    execute(sqlBuilder.createTable(table));

    assertTrue(tableExists(table.getName()));

    jdbcTemplate.execute(sqlBuilder.dropTableIfExistsCascade(table));

    assertFalse(tableExists(table.getName()));
  }

  @Test
  void testRenameTable() {
    Table table = getTableA();

    execute(sqlBuilder.createTable(table));

    assertTrue(tableExists(table.getName()));

    execute(sqlBuilder.renameTable(table, "immunization_temp"));

    assertTrue(tableExists("immunization_temp"));
    assertFalse(tableExists(table.getName()));

    jdbcTemplate.execute(sqlBuilder.dropTableIfExists("immunization_temp"));
  }

  @Test
  void testSwapTable() {
    Table tableA = getTableA();
    Table tableB = getTableB();

    execute(sqlBuilder.createTable(tableA));
    execute(sqlBuilder.createTable(tableB));

    execute(sqlBuilder.swapTable(tableA, "vaccination"));

    assertTrue(tableExists("vaccination"));
    assertFalse(tableExists("immunization"));

    jdbcTemplate.execute(sqlBuilder.dropTableIfExists("vaccination"));
  }

  @Test
  void testCreateIndex() {
    Table table = getTableA();

    List<Index> indexes = getIndexesA();

    execute(sqlBuilder.createTable(table));

    assertDoesNotThrow(() -> execute(sqlBuilder.createIndex(indexes.get(0))));

    assertDoesNotThrow(() -> execute(sqlBuilder.createIndex(indexes.get(1))));

    assertDoesNotThrow(() -> execute(sqlBuilder.createIndex(indexes.get(2))));

    jdbcTemplate.execute(sqlBuilder.dropTableIfExists(table));
  }

  private boolean tableExists(String name) {
    return jdbcTemplate.queryForList(sqlBuilder.tableExists(name), String.class).size() == 1;
  }

  private void execute(String sql) {
    jdbcTemplate.execute(sql);
  }
}
