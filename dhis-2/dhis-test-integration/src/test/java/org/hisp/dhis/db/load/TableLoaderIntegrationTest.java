package org.hisp.dhis.db.load;

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

  @Autowired
  private JdbcTemplate jdbcTemplate;
  
  @Autowired
  private TableLoader tableLoader;
  
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
    
    jdbcTemplate.execute(createSql);
    
    String dropSql = sqlBuilder.dropTableIfExists(table);
    
    jdbcTemplate.execute(dropSql);    
  }
}
