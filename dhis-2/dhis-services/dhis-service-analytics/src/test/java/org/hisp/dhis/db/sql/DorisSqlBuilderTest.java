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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.hisp.dhis.db.model.Collation;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;
import org.junit.jupiter.api.Test;

class DorisSqlBuilderTest {
  private final SqlBuilder sqlBuilder = new DorisSqlBuilder("pg_dhis", "postgresql.jar");

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

    return new Table("immunization", columns, primaryKey, Logged.LOGGED);
  }

  private Table getTableB() {
    List<Column> columns =
        List.of(
            new Column("id", DataType.INTEGER, Nullable.NOT_NULL),
            new Column("facility_type", DataType.VARCHAR_255, Nullable.NULL, Collation.C),
            new Column("bcg_doses", DataType.DOUBLE));

    List<String> checks = List.of("\"id\">0", "\"bcg_doses\">0");

    return new Table("vaccination", columns, List.of(), checks, Logged.UNLOGGED);
  }

  private Table getTableC() {
    List<Column> columns =
        List.of(
            new Column("id", DataType.BIGINT, Nullable.NOT_NULL),
            new Column("vitamin_a", DataType.BIGINT),
            new Column("vitamin_d", DataType.BIGINT));

    List<String> primaryKey = List.of("id");

    return new Table("nutrition", columns, primaryKey, List.of(), Logged.LOGGED, getTableB());
  }

  // Data types

  @Test
  void testDataTypes() {
    assertEquals("double", sqlBuilder.dataTypeDouble());
    assertEquals("datetime", sqlBuilder.dataTypeTimestamp());
  }

  // Index types

  @Test
  void testIndexTypes() {
    assertThrows(UnsupportedOperationException.class, () -> sqlBuilder.indexTypeBtree());
  }

  // Capabilities

  @Test
  void testSupportsAnalyze() {
    assertFalse(sqlBuilder.supportsAnalyze());
  }

  @Test
  void testSupportsVacuum() {
    assertFalse(sqlBuilder.supportsVacuum());
  }

  // Utilities

  @Test
  void testQuote() {
    assertEquals(
        "`Treated \"malaria\" at facility`", sqlBuilder.quote("Treated \"malaria\" at facility"));
    assertEquals(
        "`Patients on ``treatment`` for TB`", sqlBuilder.quote("Patients on `treatment` for TB"));
    assertEquals("`quarterly`", sqlBuilder.quote("quarterly"));
    assertEquals("`Fully immunized`", sqlBuilder.quote("Fully immunized"));
  }

  @Test
  void testQuoteAlias() {
    assertEquals(
        "ax.`Treated \"malaria\" at facility`",
        sqlBuilder.quote("ax", "Treated \"malaria\" at facility"));
    assertEquals(
        "analytics.`Patients on ``treatment`` for TB`",
        sqlBuilder.quote("analytics", "Patients on `treatment` for TB"));
    assertEquals("analytics.`quarterly`", sqlBuilder.quote("analytics", "quarterly"));
    assertEquals("dv.`Fully immunized`", sqlBuilder.quote("dv", "Fully immunized"));
  }

  @Test
  void testQuoteAx() {
    assertEquals(
        "ax.`Treated ``malaria`` at facility`",
        sqlBuilder.quoteAx("Treated `malaria` at facility"));
    assertEquals("ax.`quarterly`", sqlBuilder.quoteAx("quarterly"));
    assertEquals("ax.`Fully immunized`", sqlBuilder.quoteAx("Fully immunized"));
  }

  @Test
  void testSingleQuote() {
    assertEquals("'jkhYg65ThbF'", sqlBuilder.singleQuote("jkhYg65ThbF"));
    assertEquals("'Age ''<5'' years'", sqlBuilder.singleQuote("Age '<5' years"));
    assertEquals("'Status \"not checked\"'", sqlBuilder.singleQuote("Status \"not checked\""));
  }

  @Test
  void testEscape() {
    assertEquals("Age group ''under 5'' years", sqlBuilder.escape("Age group 'under 5' years"));
    assertEquals("Level ''high'' found", sqlBuilder.escape("Level 'high' found"));
    assertEquals("C:\\\\Downloads\\\\File.doc", sqlBuilder.escape("C:\\Downloads\\File.doc"));
  }

  @Test
  void testSinqleQuotedCommaDelimited() {
    assertEquals(
        "'dmPbDBKwXyF', 'zMl4kciwJtz', 'q1Nqu1r1GTn'",
        sqlBuilder.singleQuotedCommaDelimited(
            List.of("dmPbDBKwXyF", "zMl4kciwJtz", "q1Nqu1r1GTn")));
    assertEquals("'1', '3', '5'", sqlBuilder.singleQuotedCommaDelimited(List.of("1", "3", "5")));
    assertEquals("", sqlBuilder.singleQuotedCommaDelimited(List.of()));
    assertEquals("", sqlBuilder.singleQuotedCommaDelimited(null));
  }

  @Test
  void testQualifyTable() {
    assertEquals("pg_dhis.public.`category`", sqlBuilder.qualifyTable("category"));
    assertEquals(
        "pg_dhis.public.`categories_options`", sqlBuilder.qualifyTable("categories_options"));
  }

  @Test
  void testDateTrunc() {
    assertEquals(
        "date_trunc(pe.startdate, 'month')", sqlBuilder.dateTrunc("month", "pe.startdate"));
  }

  // Statements

  @Test
  void testCreateTableA() {
    Table table = getTableA();

    String expected =
        """
        create table `immunization` (`id` bigint not null,\
        `data` char(11) not null,`period` varchar(50) not null,\
        `created` datetime null,`user` json null,`value` double null) \
        engine = olap \
        unique key (`id`) \
        distributed by hash(`id`) buckets 10 \
        properties ("replication_num" = "1");""";

    assertEquals(expected, sqlBuilder.createTable(table));
  }

  @Test
  void testCreateTableB() {
    Table table = getTableB();

    String expected =
        """
        create table `vaccination` (`id` int not null,\
        `facility_type` varchar(255) null,`bcg_doses` double null) \
        engine = olap \
        duplicate key (`id`) \
        distributed by hash(`id`) buckets 10 \
        properties ("replication_num" = "1");""";

    assertEquals(expected, sqlBuilder.createTable(table));
  }

  // void testCreateTableB()

  @Test
  void testCreateTableC() {
    Table table = getTableC();

    String expected =
        """
        create table `nutrition` (`id` bigint not null,\
        `vitamin_a` bigint null,`vitamin_d` bigint null) \
        engine = olap \
        unique key (`id`) \
        distributed by hash(`id`) buckets 10 \
        properties ("replication_num" = "1");""";

    assertEquals(expected, sqlBuilder.createTable(table));
  }

  @Test
  void testCreateCatalog() {
    String expected =
        """
        create catalog `pg_dhis` \
        properties (
        "type" = "jdbc", \
        "user" = "dhis", \
        "password" = "kH7g", \
        "jdbc_url" = "jdbc:mysql://127.18.0.1:9030/dev?useUnicode=true&characterEncoding=UTF-8", \
        "driver_url" = "postgresql.jar", \
        "driver_class" = "org.postgresql.Driver"
        );""";

    assertEquals(
        expected,
        sqlBuilder.createCatalog(
            "jdbc:mysql://127.18.0.1:9030/dev?useUnicode=true&characterEncoding=UTF-8",
            "dhis",
            "kH7g"));
  }

  @Test
  void testDropCatalogIfExists() {
    String expected = "drop catalog if exists `pg_dhis`;";

    assertEquals(expected, sqlBuilder.dropCatalogIfExists());
  }

  @Test
  void testRenameTable() {
    Table table = getTableA();

    String expected =
        """
        alter table `immunization` rename `immunization_main`;""";

    assertEquals(expected, sqlBuilder.renameTable(table, "immunization_main"));
  }

  @Test
  void testDropTableIfExists() {
    Table table = getTableA();

    String expected = "drop table if exists `immunization`;";

    assertEquals(expected, sqlBuilder.dropTableIfExists(table));
  }

  @Test
  void testDropTableIfExistsString() {
    String expected = "drop table if exists `immunization`;";

    assertEquals(expected, sqlBuilder.dropTableIfExists("immunization"));
  }

  @Test
  void testDropTableIfExistsCascade() {
    Table table = getTableA();

    String expected = "drop table if exists `immunization`;";

    assertEquals(expected, sqlBuilder.dropTableIfExistsCascade(table));
  }

  @Test
  void testDropTableIfExistsCascadeString() {
    String expected = "drop table if exists `immunization`;";

    assertEquals(expected, sqlBuilder.dropTableIfExistsCascade("immunization"));
  }

  @Test
  void testSwapTable() {
    String expected =
        """
        drop table if exists `vaccination`; \
        alter table `immunization` rename `vaccination`;""";

    assertEquals(expected, sqlBuilder.swapTable(getTableA(), "vaccination"));
  }

  @Test
  void testTableExists() {
    String expected =
        """
        select t.table_name from information_schema.tables t \
        where t.table_schema = 'public' and t.table_name = 'immunization';""";

    assertEquals(expected, sqlBuilder.tableExists("immunization"));
  }

  @Test
  void testCountRows() {
    String expected =
        """
        select count(*) as row_count from `immunization`;""";

    assertEquals(expected, sqlBuilder.countRows(getTableA()));
  }
}
