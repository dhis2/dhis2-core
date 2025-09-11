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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.hisp.dhis.db.model.constraint.Unique;
import org.junit.jupiter.api.Test;

class PostgreSqlBuilderTest {
  private final PostgreSqlBuilder sqlBuilder = new PostgreSqlBuilder();

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
    assertEquals("double precision", sqlBuilder.dataTypeDouble());
    assertEquals("geometry", sqlBuilder.dataTypeGeometry());
    assertEquals("jsonb", sqlBuilder.dataTypeJson());
  }

  // Index types

  @Test
  void testIndexTypes() {
    assertEquals("btree", sqlBuilder.indexTypeBtree());
    assertEquals("gist", sqlBuilder.indexTypeGist());
    assertEquals("gin", sqlBuilder.indexTypeGin());
  }

  // Capabilities

  @Test
  void testSupportsAnalyze() {
    assertTrue(sqlBuilder.supportsAnalyze());
  }

  @Test
  void testSupportsVacuum() {
    assertTrue(sqlBuilder.supportsVacuum());
  }

  // Utilities

  @Test
  void testQuote() {
    assertEquals(
        """
        "Treated ""malaria"" at facility\"""",
        sqlBuilder.quote("Treated \"malaria\" at facility"));
    assertEquals("\"quarterly\"", sqlBuilder.quote("quarterly"));
    assertEquals("\"Fully immunized\"", sqlBuilder.quote("Fully immunized"));
  }

  @Test
  void testQuoteAlias() {
    assertEquals(
        """
        ax."Treated ""malaria"" at facility\"""",
        sqlBuilder.quote("ax", "Treated \"malaria\" at facility"));
    assertEquals("analytics.\"quarterly\"", sqlBuilder.quote("analytics", "quarterly"));
    assertEquals("dv.\"Fully immunized\"", sqlBuilder.quote("dv", "Fully immunized"));
  }

  @Test
  void testQuoteAx() {
    assertEquals(
        """
        ax."Treated ""malaria"" at facility\"""",
        sqlBuilder.quoteAx("Treated \"malaria\" at facility"));
    assertEquals("ax.\"quarterly\"", sqlBuilder.quoteAx("quarterly"));
    assertEquals("ax.\"Fully immunized\"", sqlBuilder.quoteAx("Fully immunized"));
  }

  @Test
  void testSingleQuote() {
    assertEquals("'jkhYg65ThbF'", sqlBuilder.singleQuote("jkhYg65ThbF"));
    assertEquals("'Age ''<5'' years'", sqlBuilder.singleQuote("Age '<5' years"));
    assertEquals("'Status \"not checked\"'", sqlBuilder.singleQuote("Status \"not checked\""));
  }

  @Test
  void testUnquote() {
    // Null and empty inputs
    assertEquals("", sqlBuilder.unquote(null));
    assertEquals("", sqlBuilder.unquote(""));

    // Strip surrounding double quotes
    assertEquals("hello", sqlBuilder.unquote("\"hello\""));
    assertEquals("hello world", sqlBuilder.unquote("\"hello world\""));

    // Unescape doubled inner quotes
    assertEquals("he\"llo", sqlBuilder.unquote("\"he\"\"llo\""));

    // Return as-is when not wrapped in double quotes
    assertEquals("not quoted", sqlBuilder.unquote("not quoted"));

    // Empty content between quotes
    assertEquals("", sqlBuilder.unquote("\"\""));

    // Round-trip property: unquote(quote(x)) == x
    assertEquals("simple", sqlBuilder.unquote(sqlBuilder.quote("simple")));
    assertEquals(
        "has \"inner\" quote", sqlBuilder.unquote(sqlBuilder.quote("has \"inner\" quote")));
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
        """
        'dmPbDBKwXyF', 'zMl4kciwJtz', 'q1Nqu1r1GTn'""",
        sqlBuilder.singleQuotedCommaDelimited(
            List.of("dmPbDBKwXyF", "zMl4kciwJtz", "q1Nqu1r1GTn")));
    assertEquals("'1', '3', '5'", sqlBuilder.singleQuotedCommaDelimited(List.of("1", "3", "5")));
    assertEquals("", sqlBuilder.singleQuotedCommaDelimited(List.of()));
    assertEquals("", sqlBuilder.singleQuotedCommaDelimited(null));
  }

  @Test
  void testSafeConcat() {
    assertEquals(
        "concat(de.uid, pe.iso, ou.uid)", sqlBuilder.safeConcat("de.uid", "pe.iso", "ou.uid"));
  }

  @Test
  void testConcat() {
    String result = sqlBuilder.concat(List.of("column1", "column2", "column3"));
    assertEquals("(column1 || column2 || column3)", result);
  }

  @Test
  void testTrim() {
    assertEquals("trim(ax.value)", sqlBuilder.trim("ax.value"));
  }

  @Test
  void testQualifyTable() {
    assertEquals("\"category\"", sqlBuilder.qualifyTable("category"));
    assertEquals("\"categories_options\"", sqlBuilder.qualifyTable("categories_options"));
  }

  @Test
  void testDateTrunc() {
    assertEquals(
        """
        date_trunc('month', pe.startdate)""",
        sqlBuilder.dateTrunc("month", "pe.startdate"));
  }

  @Test
  void testDifferenceInSeconds() {
    assertEquals(
        """
        extract(epoch from (a.startdate - b.enddate))""",
        sqlBuilder.differenceInSeconds("a.startdate", "b.enddate"));
    assertEquals(
        """
        extract(epoch from (a.\"startdate\" - b.\"enddate\"))""",
        sqlBuilder.differenceInSeconds(
            sqlBuilder.quote("a", "startdate"), sqlBuilder.quote("b", "enddate")));
  }

  @Test
  void testIsTrue() {
    assertEquals("dv.\"deleted\" = true", sqlBuilder.isTrue("dv", "deleted"));
    assertEquals("tei.\"followup\" = true", sqlBuilder.isTrue("tei", "followup"));
  }

  @Test
  void testIsFalse() {
    assertEquals("dv.\"deleted\" = false", sqlBuilder.isFalse("dv", "deleted"));
    assertEquals("tei.\"followup\" = false", sqlBuilder.isFalse("tei", "followup"));
  }

  @Test
  void testRegexpMatch() {
    assertEquals("value ~* 'test'", sqlBuilder.regexpMatch("value", "'test'"));
    assertEquals("number ~* '\\d'", sqlBuilder.regexpMatch("number", "'\\d'"));
    assertEquals("color ~* '^Blue$'", sqlBuilder.regexpMatch("color", "'^Blue$'"));
    assertEquals("id ~* '[a-z]\\w+\\d{3}'", sqlBuilder.regexpMatch("id", "'[a-z]\\w+\\d{3}'"));
  }

  @Test
  void testJsonExtract() {
    assertEquals("value ->> 'D7m8vpzxHDJ'", sqlBuilder.jsonExtract("value", "D7m8vpzxHDJ"));
  }

  @Test
  void testJsonExtractObject() {
    assertEquals(
        """
        ev.eventdatavalues #>> '{D7m8vpzxHDJ, value}'""",
        sqlBuilder.jsonExtract("ev.eventdatavalues", "D7m8vpzxHDJ", "value"));
    assertEquals(
        """
        ev.eventdatavalues #>> '{qrur9Dvnyt5, value}'""",
        sqlBuilder.jsonExtract("ev.eventdatavalues", "qrur9Dvnyt5", "value"));
  }

  @Test
  void testCast() {
    assertEquals(
        """
        ax."qrur9Dvnyt5"::numeric""",
        sqlBuilder.cast("ax.\"qrur9Dvnyt5\"", org.hisp.dhis.analytics.DataType.NUMERIC));
    assertEquals(
        """
        ax."qrur9Dvnyt5"::numeric != 0""",
        sqlBuilder.cast("ax.\"qrur9Dvnyt5\"", org.hisp.dhis.analytics.DataType.BOOLEAN));
    assertEquals(
        """
        ax."qrur9Dvnyt5"::text""",
        sqlBuilder.cast("ax.\"qrur9Dvnyt5\"", org.hisp.dhis.analytics.DataType.TEXT));
  }

  @Test
  void testIfThen() {
    assertEquals(
        "case when a.status = 'COMPLETE' then a.eventdate end",
        sqlBuilder.ifThen("a.status = 'COMPLETE'", "a.eventdate"));
  }

  @Test
  void testIfThenElse() {
    assertEquals(
        "case when a.status = 'COMPLETE' then a.eventdate else a.scheduleddate end",
        sqlBuilder.ifThenElse("a.status = 'COMPLETE'", "a.eventdate", "a.scheduleddate"));
  }

  @Test
  void testIfThenElseMulti() {
    String expected =
        """
        case \
        when a.status = 'COMPLETE' then a.eventdate \
        when a.status = 'SCHEDULED' then a.scheduleddate \
        else a.incidentdate \
        end""";

    assertEquals(
        expected,
        sqlBuilder.ifThenElse(
            "a.status = 'COMPLETE'",
            "a.eventdate",
            "a.status = 'SCHEDULED'",
            "a.scheduleddate",
            "a.incidentdate"));
  }

  @Test
  void testLog10() {
    assertEquals("log(value)", sqlBuilder.log10("value"));
  }

  // Statements

  @Test
  void testCreateTableA() {
    Table table = getTableA();

    String expected =
        """
        create table "immunization" ("id" bigint not null, "data" char(11) not null, \
        "period\" varchar(50) not null, "created" timestamp null, "user" jsonb null, \
        "value" double precision null, primary key ("id"));""";

    assertEquals(expected, sqlBuilder.createTable(table));
  }

  @Test
  void testCreateTableB() {
    Table table = getTableB();

    String expected =
        """
        create unlogged table "vaccination" ("id" integer not null, \
        "facility_type" varchar(255) null collate "C", "bcg_doses" double precision null, \
        check("id">0), check("bcg_doses">0));""";

    assertEquals(expected, sqlBuilder.createTable(table));
  }

  @Test
  void testCreateTableC() {
    Table table = getTableC();

    String expected =
        """
        create table "nutrition" ("id" bigint not null, "vitamin_a" bigint null, \
        "vitamin_d" bigint null, primary key ("id")) inherits ("vaccination");""";

    assertEquals(expected, sqlBuilder.createTable(table));
  }

  @Test
  void testAnalyzeTable() {
    Table table = getTableA();

    String expected =
        """
        analyze "immunization";""";

    assertEquals(expected, sqlBuilder.analyzeTable(table));
  }

  @Test
  void testVacuumTable() {
    Table table = getTableA();

    String expected =
        """
        vacuum "immunization";""";

    assertEquals(expected, sqlBuilder.vacuumTable(table));
  }

  @Test
  void testRenameTable() {
    Table table = getTableA();

    String expected =
        """
        alter table "immunization" rename to "vaccination";""";

    assertEquals(expected, sqlBuilder.renameTable(table, "vaccination"));
  }

  @Test
  void testDropTableIfExists() {
    Table table = getTableA();

    String expected =
        """
        drop table if exists "immunization";""";

    assertEquals(expected, sqlBuilder.dropTableIfExists(table));
  }

  @Test
  void testDropTableIfExistsString() {
    String expected =
        """
        drop table if exists \"immunization\";""";

    assertEquals(expected, sqlBuilder.dropTableIfExists("immunization"));
  }

  @Test
  void testDropTableIfExistsCascade() {
    Table table = getTableA();

    String expected =
        """
        drop table if exists "immunization" cascade;""";

    assertEquals(expected, sqlBuilder.dropTableIfExistsCascade(table));
  }

  @Test
  void testDropTableIfExistsCascadeString() {
    String expected =
        """
        drop table if exists "immunization" cascade;""";

    assertEquals(expected, sqlBuilder.dropTableIfExistsCascade("immunization"));
  }

  @Test
  void testSwapTable() {
    String expected =
        """
        drop table if exists "vaccination" cascade; \
        alter table "immunization" rename to "vaccination";""";

    assertEquals(expected, sqlBuilder.swapTable(getTableA(), "vaccination"));
  }

  @Test
  void testSetParent() {
    String expected =
        """
        alter table "immunization" inherit "vaccination";""";

    assertEquals(expected, sqlBuilder.setParentTable(getTableA(), "vaccination"));
  }

  @Test
  void testRemoveParent() {
    String expected =
        """
        alter table "immunization" no inherit "vaccination";""";

    assertEquals(expected, sqlBuilder.removeParentTable(getTableA(), "vaccination"));
  }

  @Test
  void testSwapParentTable() {
    String expected =
        """
        alter table "immunization" no inherit "vaccination"; \
        alter table "immunization" inherit \"nutrition\";""";

    assertEquals(expected, sqlBuilder.swapParentTable(getTableA(), "vaccination", "nutrition"));
  }

  @Test
  void testTableExists() {
    String expected =
        """
        select t.table_name from information_schema.tables t \
        where t.table_schema = 'public' \
        and t.table_name = 'immunization';""";

    assertEquals(expected, sqlBuilder.tableExists("immunization"));
  }

  @Test
  void testCountRows() {
    String expected =
        """
        select count(*) as row_count from "immunization";""";

    assertEquals(expected, sqlBuilder.countRows(getTableA()));
  }

  @Test
  void testCreateIndexA() {
    List<Index> indexes = getIndexesA();

    String expected =
        """
        create index "in_immunization_data" \
        on "immunization" using btree("data");""";

    assertEquals(expected, sqlBuilder.createIndex(indexes.get(0)));
  }

  @Test
  void testCreateIndexB() {
    List<Index> indexes = getIndexesA();

    String expected =
        """
        create index "in_immunization_period_created" \
        on "immunization" using btree("period","created");""";

    assertEquals(expected, sqlBuilder.createIndex(indexes.get(1)));
  }

  @Test
  void testCreateIndexC() {
    List<Index> indexes = getIndexesA();

    String expected =
        """
        create index "in_immunization_user" \
        on "immunization" using gin("user");""";

    assertEquals(expected, sqlBuilder.createIndex(indexes.get(2)));
  }

  @Test
  void testCreateIndexD() {
    List<Index> indexes = getIndexesA();

    String expected =
        """
        create index "in_immunization_data_period" \
        on "immunization" using btree(lower("data"),lower("period"));""";

    assertEquals(expected, sqlBuilder.createIndex(indexes.get(3)));
  }

  @Test
  void testCreateIndexWithDescNullsLast() {
    String expected =
        """
        create unique index "index_a" on "table_a" \
        using btree("column_a" desc nulls last);""";
    Index index =
        Index.builder()
            .name("index_a")
            .tableName("table_a")
            .unique(Unique.UNIQUE)
            .columns(List.of("column_a"))
            .sortOrder("desc nulls last")
            .build();

    String createIndexStmt = sqlBuilder.createIndex(index);

    assertEquals(expected, createIndexStmt);
  }

  @Test
  void testInsertIntoSelectFrom() {
    String expected =
        """
        insert into "vaccination" ("id","facility_type","bcg_doses") \
        select "id","facility_type","bcg_doses" from "immunization";""";

    assertEquals(expected, sqlBuilder.insertIntoSelectFrom(getTableB(), "\"immunization\""));
  }
}
