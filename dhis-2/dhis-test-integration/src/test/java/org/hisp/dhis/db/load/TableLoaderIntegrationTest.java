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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class TableLoaderIntegrationTest extends IntegrationTestBase {

  private Table table;

  private List<Object[]> data;

  private final SqlBuilder sqlBuilder = new PostgreSqlBuilder();

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private TableLoader tableLoader;

  private Table getTable() {
    List<Column> columns =
        List.of(
            new Column("id", DataType.BIGINT, Nullable.NOT_NULL),
            new Column("data", DataType.CHARACTER_11, Nullable.NOT_NULL),
            new Column("created", DataType.DATE),
            new Column("value", DataType.DOUBLE));

    List<String> primaryKey = List.of("id");

    return new Table("immunization", columns, primaryKey, Logged.LOGGED);
  }

  private List<Object[]> getData() {
    return List.of(
        new Object[] {24, "BCG", "2024-03-01", 12.0},
        new Object[] {45, "OPV", "2024-05-02", 20.5},
        new Object[] {79, "IPT", "2024-06-08", 34.0});
  }

  @BeforeEach
  void beforeEach() {
    table = getTable();
    data = getData();
  }

  @Test
  void testLoad() {
    String createSql = sqlBuilder.createTable(table);

    execute(createSql);
    
    assertTrue(tableExists(table));
    
    tableLoader.load(table, data);
    
    String dropSql = sqlBuilder.dropTableIfExists(table);

    execute(dropSql);

    assertFalse(tableExists(table));    
  }

  private boolean tableExists(Table table) {
    return jdbcTemplate.queryForList(sqlBuilder.tableExists(table), String.class).size() == 1;
  }

  private void execute(String sql) {
    jdbcTemplate.execute(sql);
  }
}
