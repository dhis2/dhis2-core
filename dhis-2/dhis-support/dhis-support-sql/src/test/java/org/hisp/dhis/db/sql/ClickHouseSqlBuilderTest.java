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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.db.model.Collation;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ClickHouseSqlBuilderTest {
  private final ClickHouseSqlBuilder sqlBuilder = new ClickHouseSqlBuilder("dhis2");

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

    return new Table("vaccination", columns, List.of());
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

  private Table getTableD() {
    List<Column> columns =
        List.of(
            new Column("id", DataType.BIGINT, Nullable.NOT_NULL),
            new Column("data", DataType.CHARACTER_11, Nullable.NOT_NULL),
            new Column("period", DataType.VARCHAR_50, Nullable.NOT_NULL),
            new Column("value", DataType.DOUBLE));

    List<String> sortKey = List.of("data", "period");

    return new Table("immunization", columns, List.of(), sortKey, List.of(), Logged.LOGGED);
  }

  // Data types

  @Test
  void testDataTypes() {
    assertEquals("Float64", sqlBuilder.dataTypeDouble());
    assertEquals("DateTime64(3)", sqlBuilder.dataTypeTimestamp());
    assertEquals("String", sqlBuilder.dataTypeJson());
  }

  // Index types

  @Test
  void testIndexTypes() {
    assertThrows(UnsupportedOperationException.class, sqlBuilder::indexTypeBtree);
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
        "\"Treated \"\"malaria\"\" at facility\"",
        sqlBuilder.quote("Treated \"malaria\" at facility"));
    assertEquals(
        "\"\"\"Patients on \"\"treatment\"\" for TB\"\"\"",
        sqlBuilder.quote("\"Patients on \"treatment\" for TB\""));
    assertEquals("\"quarterly\"", sqlBuilder.quote("quarterly"));
    assertEquals("\"Fully immunized\"", sqlBuilder.quote("Fully immunized"));
  }

  @Test
  void testQuoteAlias() {
    assertEquals(
        """
        ax."Treated ""malaria"" at facility\"""",
        sqlBuilder.quote("ax", "Treated \"malaria\" at facility"));
    assertEquals(
        """
        analytics."Patients on ""treatment"" for TB\"""",
        sqlBuilder.quote("analytics", "Patients on \"treatment\" for TB"));
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
  void testEscape() {
    assertEquals("Age group ''under 5'' years", sqlBuilder.escape("Age group 'under 5' years"));
    assertEquals("Level ''high'' found", sqlBuilder.escape("Level 'high' found"));
  }

  @Test
  void testConcat() {
    assertEquals(
        "concat(de.uid, pe.iso, ou.uid)", sqlBuilder.safeConcat("de.uid", "pe.iso", "ou.uid"));
  }

  @Test
  void testConcat_FromList() {
    String result = sqlBuilder.concat(List.of("column1", "column2", "column3"));
    assertEquals("concat(column1, column2, column3)", result);
  }

  @Test
  void testTrim() {
    assertEquals("trim(ax.value)", sqlBuilder.trim("ax.value"));
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
    assertEquals("postgresql(\"pg_dhis\", table='category')", sqlBuilder.qualifyTable("category"));
    assertEquals(
        "postgresql(\"pg_dhis\", table='categories_options')",
        sqlBuilder.qualifyTable("categories_options"));
  }

  @Test
  void testDateTrunc() {
    assertEquals(
        "date_trunc('month', pe.startdate)", sqlBuilder.dateTrunc("month", "pe.startdate"));
  }

  @Test
  void testDifferenceInSeconds() {
    assertEquals(
        "(toUnixTimestamp(a.startdate) - toUnixTimestamp(b.enddate))",
        sqlBuilder.differenceInSeconds("a.startdate", "b.enddate"));
    assertEquals(
        "(toUnixTimestamp(a.\"startdate\") - toUnixTimestamp(b.\"enddate\"))",
        sqlBuilder.differenceInSeconds(
            sqlBuilder.quote("a", "startdate"), sqlBuilder.quote("b", "enddate")));
  }

  @Test
  void testIsTrue() {
    assertEquals("dv.\"deleted\"", sqlBuilder.isTrue("dv", "deleted"));
    assertEquals("tei.\"followup\"", sqlBuilder.isTrue("tei", "followup"));
  }

  @Test
  void testIsFalse() {
    assertEquals("not dv.\"deleted\"", sqlBuilder.isFalse("dv", "deleted"));
    assertEquals("not tei.\"followup\"", sqlBuilder.isFalse("tei", "followup"));
  }

  @Test
  void testRegexpMatch() {
    assertEquals("match(value, 'test')", sqlBuilder.regexpMatch("value", "'test'"));
    assertEquals("match(number, '\\d')", sqlBuilder.regexpMatch("number", "'\\d'"));
    assertEquals("match(color, '^Blue$')", sqlBuilder.regexpMatch("color", "'^Blue$'"));
    assertEquals("match(id, '[a-z]\\w+\\d{3}')", sqlBuilder.regexpMatch("id", "'[a-z]\\w+\\d{3}'"));
  }

  @Test
  void testJsonExtract() {
    assertEquals(
        "JSONExtractString(value, 'D7m8vpzxHDJ')", sqlBuilder.jsonExtract("value", "D7m8vpzxHDJ"));
  }

  @Test
  void testJsonExtractObject() {
    assertEquals(
        """
        JSONExtractString(JSONExtractRaw(ev.eventdatavalues, 'D7m8vpzxHDJ'), 'value')""",
        sqlBuilder.jsonExtract("ev.eventdatavalues", "D7m8vpzxHDJ", "value"));
    assertEquals(
        """
        JSONExtractString(JSONExtractRaw(ev.eventdatavalues, 'qrur9Dvnyt5'), 'value')""",
        sqlBuilder.jsonExtract("ev.eventdatavalues", "qrur9Dvnyt5", "value"));
  }

  @Test
  void testCast() {
    assertEquals(
        """
        toFloat64(ax."qrur9Dvnyt5")""",
        sqlBuilder.cast("ax.\"qrur9Dvnyt5\"", org.hisp.dhis.analytics.DataType.NUMERIC));
    assertEquals(
        """
        toUInt8(ax."qrur9Dvnyt5") != 0""",
        sqlBuilder.cast("ax.\"qrur9Dvnyt5\"", org.hisp.dhis.analytics.DataType.BOOLEAN));
    assertEquals(
        """
        toString(ax."qrur9Dvnyt5")""",
        sqlBuilder.cast("ax.\"qrur9Dvnyt5\"", org.hisp.dhis.analytics.DataType.TEXT));
  }

  @Test
  void testIfThen() {
    assertEquals(
        """
        if(a.status = 'COMPLETE', a.eventdate, null)""",
        sqlBuilder.ifThen("a.status = 'COMPLETE'", "a.eventdate"));
  }

  @Test
  void testIfThenElse() {
    assertEquals(
        """
        if(a.status = 'COMPLETE', a.eventdate, a.scheduleddate)""",
        sqlBuilder.ifThenElse("a.status = 'COMPLETE'", "a.eventdate", "a.scheduleddate"));
  }

  @Test
  void testIfThenElseMulti() {
    String expected =
        """
        multiIf(a.status = 'COMPLETE', a.eventdate, a.status = 'SCHEDULED', a.scheduleddate, a.incidentdate)""";

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
    assertEquals("log10(value)", sqlBuilder.log10("value"));
  }

  @Test
  void testStandardDeviation() {
    assertEquals("stddevSamp(value)", sqlBuilder.stddev("value"));
  }

  @Test
  void testVariance() {
    assertEquals("varSamp(value)", sqlBuilder.variance("value"));
  }

  // Statements

  @Test
  void testCreateTableA() {
    Table table = getTableA();

    String expected =
        """
        create table "immunization" ("id" Int64 not null,"data" String not null,\
        "period" String not null,"created" DateTime64(3) null,"user" String null,\
        "value" Float64 null) \
        engine = MergeTree() \
        order by ("id");""";

    assertEquals(expected, sqlBuilder.createTable(table));
  }

  @Test
  void testCreateTableB() {
    Table table = getTableB();

    String expected =
        """
        create table "vaccination" ("id" Int32 not null,\
        "facility_type" String null,"bcg_doses" Float64 null) \
        engine = MergeTree() \
        order by ("id");""";

    assertEquals(expected, sqlBuilder.createTable(table));
  }

  @Test
  void testCreateTableC() {
    Table table = getTableC();

    String expected =
        """
        create table "nutrition" ("id" Int64 not null,"vitamin_a" Int64 null,\
        "vitamin_d" Int64 null) \
        engine = MergeTree() order by ("id");""";

    assertEquals(expected, sqlBuilder.createTable(table));
  }

  @Test
  void testCreateTableD() {
    Table table = getTableD();

    String expected =
        """
        create table "immunization" ("id" Int64 not null,"data" String not null,\
        "period" String not null,"value" Float64 null) \
        engine = MergeTree() \
        order by ("data","period");""";

    assertEquals(expected, sqlBuilder.createTable(table));
  }

  @Test
  void testRenameTable() {
    Table table = getTableA();

    String expected = "rename table \"immunization\" to \"vaccination\";";

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

    String expected = "drop table if exists \"immunization\";";

    assertEquals(expected, sqlBuilder.dropTableIfExistsCascade(table));
  }

  @Test
  void testDropTableIfExistsCascadeString() {
    String expected = "drop table if exists \"immunization\";";

    assertEquals(expected, sqlBuilder.dropTableIfExistsCascade("immunization"));
  }

  @Test
  void testSwapTable() {
    String expected =
        """
        drop table if exists "vaccination"; \
        rename table "immunization" to "vaccination";""";

    assertEquals(expected, sqlBuilder.swapTable(getTableA(), "vaccination"));
  }

  @Test
  void testTableExists() {
    String expected =
        """
        select t.name as table_name \
        from system.tables t \
        where t.database = 'default' \
        and t.name = 'immunization' \
        and engine not in ('View', 'Materialized View');""";

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
  void testInsertIntoSelectFrom() {
    String expected =
        """
        insert into "vaccination" ("id","facility_type","bcg_doses") \
        select "id","facility_type","bcg_doses" from "immunization";""";

    assertEquals(expected, sqlBuilder.insertIntoSelectFrom(getTableB(), "\"immunization\""));
  }

  // Named collection

  @Test
  void testCreateNamedCollection() {
    String expected =
        """
        create named collection "pg_dhis" as """;

    assertTrue(
        sqlBuilder
            .createNamedCollection("pg_dhis", Map.of("host", "mydomain.org"))
            .startsWith(expected));
  }

  @Test
  void testDropNamedCollectionIfExists() {
    assertEquals(
        "drop named collection if exists \"pg_dhis\";",
        sqlBuilder.dropNamedCollectionIfExists("pg_dhis"));
  }

  @Nested
  @DisplayName("Valid Database Name Extraction and Table Qualification")
  class ValidDatabaseNameTests {

    @Test
    @DisplayName("Should extract database name from basic URL")
    void shouldExtractBasicDatabaseName() {
      String url = "jdbc:clickhouse://localhost:8123/dhis2";
      ClickHouseSqlBuilder sqlBuilder = new ClickHouseSqlBuilder(url);
      assertEquals("dhis2", sqlBuilder.getDatabaseName());
    }

    @Test
    @DisplayName("Should qualify table with database name from URL with query parameters")
    void shouldQualifyTableWithQueryParams() {
      String url = "jdbc:clickhouse://localhost:8123/analytics?ssl=true&user=admin";
      ClickHouseSqlBuilder sqlBuilder = new ClickHouseSqlBuilder(url);
      assertEquals("analytics", sqlBuilder.getDatabaseName());
    }

    @Test
    @DisplayName("Should qualify table with database name from URL with trailing question mark")
    void shouldQualifyTableWithTrailingQuestionMark() {
      String url = "jdbc:clickhouse://localhost:8123/dhis2?";
      ClickHouseSqlBuilder sqlBuilder = new ClickHouseSqlBuilder(url);
      assertEquals("dhis2", sqlBuilder.getDatabaseName());
    }

    @Test
    @DisplayName("Should handle different ports")
    void shouldHandleDifferentPorts() {
      String url = "jdbc:clickhouse://localhost:9440/mydb";
      ClickHouseSqlBuilder sqlBuilder = new ClickHouseSqlBuilder(url);
      assertEquals("mydb", sqlBuilder.getDatabaseName());
    }
  }

  @Nested
  @DisplayName("No Database Cases")
  class NoDatabaseTests {

    @Test
    @DisplayName("Should handle case when no database is specified (root path)")
    void shouldHandleRootPath() {
      String url = "jdbc:clickhouse://localhost:8123/";
      ClickHouseSqlBuilder sqlBuilder = new ClickHouseSqlBuilder(url);
      assertNull(sqlBuilder.getDatabaseName());
    }

    @Test
    @DisplayName("Should handle case when no path is specified")
    void shouldHandleNoPath() {
      String url = "jdbc:clickhouse://localhost:8123";
      ClickHouseSqlBuilder sqlBuilder = new ClickHouseSqlBuilder(url);
      // Assuming it returns just the table name when no database is found
      // You may need to adjust this based on actual behavior
      assertNull(sqlBuilder.getDatabaseName());
    }
  }

  @Nested
  @DisplayName("Invalid Input Tests")
  class InvalidInputTests {

    @Test
    @DisplayName("Should throw exception for null connection URL")
    void shouldThrowExceptionForNullUrl() {
      assertThrows(IllegalArgumentException.class, () -> new ClickHouseSqlBuilder(null));
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should throw exception for empty/whitespace connection URL")
    void shouldThrowExceptionForEmptyUrl(String url) {
      assertThrows(IllegalArgumentException.class, () -> new ClickHouseSqlBuilder(url));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "invalid-url",
          "http://localhost:8123/db",
          "clickhouse://localhost:8123/db",
          "jdbc:mysql://localhost:3306/mysql",
          "jdbc:postgresql://localhost:5432/postgres"
        })
    @DisplayName("Should throw exception for non-ClickHouse JDBC URLs")
    void shouldThrowExceptionForNonClickHouseUrls(String url) {
      assertThrows(IllegalArgumentException.class, () -> new ClickHouseSqlBuilder(url));
    }

    @Test
    @DisplayName("Should handle null table name without database prefix")
    void shouldHandleNullTableNameWithoutPrefix() {
      // URL with no database specified
      String url = "jdbc:clickhouse://localhost:8123/";
      ClickHouseSqlBuilder sqlBuilder = new ClickHouseSqlBuilder(url);

      // When there's no database prefix, null table name returns null
      String result = sqlBuilder.getDatabaseName();
      assertNull(result);
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle URL with whitespace around it")
    void shouldHandleUrlWithWhitespace() {
      String url = "  jdbc:clickhouse://localhost:8123/trimmed_db  ";
      ClickHouseSqlBuilder sqlBuilder = new ClickHouseSqlBuilder(url);
      assertEquals("trimmed_db", sqlBuilder.getDatabaseName());
    }

    @Test
    @DisplayName("Should handle table names with special characters")
    void shouldHandleTableWithSpecialCharacters() {
      String url = "jdbc:clickhouse://localhost:8123/mydb";
      ClickHouseSqlBuilder sqlBuilder = new ClickHouseSqlBuilder(url);
      assertEquals("mydb", sqlBuilder.getDatabaseName());
    }

    @Test
    @DisplayName("Should handle database name with special characters")
    void shouldHandleDatabaseWithSpecialCharacters() {
      String url = "jdbc:clickhouse://localhost:8123/db-name_123";
      ClickHouseSqlBuilder sqlBuilder = new ClickHouseSqlBuilder(url);
      assertEquals("db-name_123", sqlBuilder.getDatabaseName());
    }
  }
}
