/*
 * Copyright (c) 2004-2025, University of Oslo
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

import java.util.List;
import org.hisp.dhis.db.model.Collation;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class DorisSqlBuilderTest {
  private final DorisSqlBuilder sqlBuilder = new DorisSqlBuilder("pg_dhis", "postgresql.jar");

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
    assertEquals("double", sqlBuilder.dataTypeDouble());
    assertEquals("datetime(3)", sqlBuilder.dataTypeTimestamp());
    assertEquals("json", sqlBuilder.dataTypeJson());
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

  // Statements

  @Test
  void testCreateTableA() {
    Table table = getTableA();
    String expected =
        """
        create table `immunization` (`id` bigint not null,`data` char(11) not null,\
        `period` varchar(50) not null,`created` datetime(3) null,`user` json null,\
        `value` double null) \
        engine = olap \
        unique key (`id`) \
        distributed by hash(`id`) \
        buckets 10 \
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
        distributed by hash(`id`) \
        buckets 10 properties ("replication_num" = "1");""";

    assertEquals(expected, sqlBuilder.createTable(table));
  }

  @Test
  void testCreateTableC() {
    Table table = getTableC();

    String expected =
        """
        create table `nutrition` (`id` bigint not null,`vitamin_a` bigint null,\
        `vitamin_d` bigint null) \
        engine = olap \
        unique key (`id`) \
        distributed by hash(`id`) \
        buckets 10 \
        properties ("replication_num" = "1");""";

    assertEquals(expected, sqlBuilder.createTable(table));
  }

  @Test
  void testCreateTableD() {
    Table table = getTableD();

    String expected =
        """
        create table `immunization` (`id` bigint not null,`data` char(11) not null,\
        `period` varchar(50) not null,`value` double null) \
        engine = olap \
        duplicate key (`id`) \
        distributed by hash(`id`) \
        buckets 10 \
        properties ("replication_num" = "1");""";

    assertEquals(expected, sqlBuilder.createTable(table));
  }

  @Test
  void testRenameTable() {
    Table table = getTableA();

    String expected = "alter table `immunization` rename `vaccination`;";

    assertEquals(expected, sqlBuilder.renameTable(table, "vaccination"));
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
        select t.table_name \
        from information_schema.tables t \
        where t.table_schema = 'public' \
        and t.table_name = 'immunization';""";

    assertEquals(expected, sqlBuilder.tableExists("immunization"));
  }

  @Test
  void testCountRows() {
    String expected =
        """
        select count(*) as row_count from `immunization`;""";

    assertEquals(expected, sqlBuilder.countRows(getTableA()));
  }

  @Test
  void testInsertIntoSelectFrom() {
    String expected =
        """
        insert into `vaccination` (`id`,`facility_type`,`bcg_doses`) \
        select `id`,`facility_type`,`bcg_doses` from `immunization`;""";

    assertEquals(expected, sqlBuilder.insertIntoSelectFrom(getTableB(), "`immunization`"));
  }

  // Normalization

  @Test
  void testToValidColumnName() {
    assertNull(sqlBuilder.toValidColumnName(null));
    assertEquals("", sqlBuilder.toValidColumnName(""));

    assertEquals("FacilityType", sqlBuilder.toValidColumnName("FacilityType"));
    assertEquals("Facility type", sqlBuilder.toValidColumnName("Facility type"));
    assertEquals("Facility_Type", sqlBuilder.toValidColumnName("Facility_Type"));
    assertEquals("Facility-Type", sqlBuilder.toValidColumnName("Facility-Type"));

    assertEquals("Age in years", sqlBuilder.toValidColumnName("Age in years"));
    assertEquals("Age&in*years", sqlBuilder.toValidColumnName("Age&in*years"));
    assertEquals("Age#in@years", sqlBuilder.toValidColumnName("Age#in@years"));
    assertEquals("Age^in$years", sqlBuilder.toValidColumnName("Age^in$years"));

    assertEquals("Facility_Type", sqlBuilder.toValidColumnName("Facility!Type"));
    assertEquals("Facility_Type", sqlBuilder.toValidColumnName("Facility!!!Type"));
    assertEquals("Facility_Type_", sqlBuilder.toValidColumnName("Facility(Type)"));
    assertEquals("Facility_Type_", sqlBuilder.toValidColumnName("(Facility)(Type)"));

    assertEquals("Age_in_years", sqlBuilder.toValidColumnName("Age=in=years"));
    assertEquals("Age _in_ _years_", sqlBuilder.toValidColumnName("Age [in] [years]"));
    assertEquals("Age_ _in_ _years_", sqlBuilder.toValidColumnName("[[[Age]]] [in] [years]"));
    assertEquals("Age_ _in_ _years_", sqlBuilder.toValidColumnName("[[[Age]]] [[in]] [[years]]"));
  }

  // Catalog

  @Test
  void testCreateCatalog() {
    String expected =
        """
        create catalog `pg_dhis` \
        properties (\
        "type" = "jdbc", "user" = "dhis", "password" = "district", \
        "jdbc_url" = "127.0.0.1", "driver_url" = "postgresql.jar", \
        "driver_class" = "org.postgresql.Driver");""";

    assertEquals(expected, sqlBuilder.createCatalog("127.0.0.1", "dhis", "district"));
  }
}
