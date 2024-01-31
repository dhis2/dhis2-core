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

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.hisp.dhis.db.model.constraint.Unique;
import org.junit.jupiter.api.Test;

class PostgreSqlBuilderTest {
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

    List<Index> indexes =
        List.of(
            new Index("in_immunization_data", "immunization", List.of("data")),
            new Index("in_immunization_period_created", "immunization", List.of("period", "created")),
            new Index("in_immunization_user", "immunization", IndexType.GIN, List.of("user")),
            new Index(
                "in_immunization_data_period",
                "immunization",
                Unique.NON_UNIQUE,
                List.of("data", "period"),
                IndexFunction.LOWER));

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
  void testDataType() {
    assertEquals("double precision", sqlBuilder.dataTypeDouble());
    assertEquals("geometry", sqlBuilder.dataTypeGeometry());
  }

  @Test
  void testIndexType() {
    assertEquals("btree", sqlBuilder.indexTypeBtree());
    assertEquals("gist", sqlBuilder.indexTypeGist());
  }

  @Test
  void testCreateTableA() {
    Table table = getTableA();

    String expected =
        "create table \"immunization\" (\"id\" bigint not null, \"data\" char(11) not null, "
            + "\"period\" varchar(50) not null, \"created\" timestamp null, \"user\" jsonb null, "
            + "\"value\" double precision null, primary key (\"id\"));";

    assertEquals(expected, sqlBuilder.createTable(table));
  }

  @Test
  void testCreateTableB() {
    Table table = getTableB();

    String expected =
        "create unlogged table \"vaccination\" (\"id\" integer not null, "
            + "\"facility_type\" varchar(255) null collate \"C\", \"bcg_doses\" double precision null, "
            + "check(\"id\">0), check(\"bcg_doses\">0));";

    assertEquals(expected, sqlBuilder.createTable(table));
  }

  @Test
  void testAnalyzeTable() {
    Table table = getTableA();

    String expected = "analyze \"immunization\";";

    assertEquals(expected, sqlBuilder.analyzeTable(table));
  }

  @Test
  void testVacuumTable() {
    Table table = getTableA();

    String expected = "vacuum \"immunization\";";

    assertEquals(expected, sqlBuilder.vacuumTable(table));
  }

  @Test
  void testRenameTable() {
    Table table = getTableA();

    String expected = "alter table \"immunization\" rename to \"vaccination\";";

    assertEquals(expected, sqlBuilder.renameTable(table, "vaccination"));
  }

  @Test
  void testDropTableIfExists() {
    Table table = getTableA();

    String expected = "drop table if exists \"immunization\";";

    assertEquals(expected, sqlBuilder.dropTableIfExists(table));
  }

  @Test
  void testDropTableIfExistsString() {
    String expected = "drop table if exists \"immunization\";";

    assertEquals(expected, sqlBuilder.dropTableIfExists("immunization"));
  }

  @Test
  void testDropTableIfExistsCascade() {
    Table table = getTableA();

    String expected = "drop table if exists \"immunization\" cascade;";

    assertEquals(expected, sqlBuilder.dropTableIfExistsCascade(table));
  }

  @Test
  void testDropTableIfExistsCascadeString() {
    String expected = "drop table if exists \"immunization\" cascade;";

    assertEquals(expected, sqlBuilder.dropTableIfExistsCascade("immunization"));
  }

  @Test
  void testTableExists() {
    String expected =
        "select t.table_name from information_schema.tables t "
            + "where t.table_schema = 'public' and t.table_name = 'immunization';";

    assertEquals(expected, sqlBuilder.tableExists("immunization"));
  }

  @Test
  void testCreateIndexA() {
    Table table = getTableA();

    String expected =
        "create index \"in_immunization_data\" on \"immunization\" using btree(\"data\");";

    assertEquals(expected, sqlBuilder.createIndex(table, table.getIndexes().get(0)));
  }

  @Test
  void testCreateIndexB() {
    Table table = getTableA();

    String expected =
        "create index \"in_immunization_period_created\" on \"immunization\" using btree(\"period\", \"created\");";

    assertEquals(expected, sqlBuilder.createIndex(table, table.getIndexes().get(1)));
  }

  @Test
  void testCreateIndexC() {
    Table table = getTableA();

    String expected =
        "create index \"in_immunization_user\" on \"immunization\" using gin(\"user\");";

    assertEquals(expected, sqlBuilder.createIndex(table, table.getIndexes().get(2)));
  }

  @Test
  void testCreateIndexD() {
    Table table = getTableA();

    String expected =
        "create index \"in_immunization_data_period\" on \"immunization\" using btree(lower(\"data\"), lower(\"period\"));";

    assertEquals(expected, sqlBuilder.createIndex(table, table.getIndexes().get(3)));
  }
}
