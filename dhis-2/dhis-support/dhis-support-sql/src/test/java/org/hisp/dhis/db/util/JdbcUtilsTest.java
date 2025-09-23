package org.hisp.dhis.db.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class JdbcUtilsTest {
  @Test
  void testExtractDatabaseName() {
      // PostgreSQL
      assertEquals("d42", JdbcUtils.getDatabaseFromConnectionUrl("jdbc:postgresql:d42"));
      assertEquals("testdb", JdbcUtils.getDatabaseFromConnectionUrl("jdbc:postgresql://localhost:5432/testdb"));
      assertEquals("mydb", JdbcUtils.getDatabaseFromConnectionUrl("jdbc:postgresql://192.168.1.100/mydb?ssl=true"));

      // ClickHouse
      assertEquals("d42", JdbcUtils.getDatabaseFromConnectionUrl("jdbc:clickhouse://localhost:8123/d42"));
      assertEquals("d42", JdbcUtils.getDatabaseFromConnectionUrl("jdbc:clickhouse://myserver.org/d42"));
      assertEquals("d42", JdbcUtils.getDatabaseFromConnectionUrl("jdbc:clickhouse://localhost:8123/d42?time_to_live=12&my_prop=14"));
      assertEquals("system", JdbcUtils.getDatabaseFromConnectionUrl("jdbc:ch://my-server:8123/system"));
      assertEquals("anotherdb", JdbcUtils.getDatabaseFromConnectionUrl("jdbc:ch://127.0.0.1/anotherdb"));

      // Test cases for MySQL and Apache Doris (using MySQL protocol)
      assertEquals("demo", JdbcUtils.getDatabaseFromConnectionUrl("jdbc:mysql://12.34.12.34:9030/demo"));
      assertEquals("demo", JdbcUtils.getDatabaseFromConnectionUrl("jdbc:mysql://12.34.12.34:9030/demo?useUnicode=true&characterEncoding=utf8"));
      assertEquals("sales", JdbcUtils.getDatabaseFromConnectionUrl("jdbc:mysql://localhost/sales"));
      assertEquals("reports", JdbcUtils.getDatabaseFromConnectionUrl("jdbc:mysql://10.0.0.5:3306/reports?serverTimezone=UTC"));
      assertEquals("dorisdb", JdbcUtils.getDatabaseFromConnectionUrl("jdbc:mysql://doris-cluster:9030/dorisdb"));
      assertNull(JdbcUtils.getDatabaseFromConnectionUrl("jdbc:mysql://localhost:3306/"));

      // General and corner cases
      assertNull(JdbcUtils.getDatabaseFromConnectionUrl(null));
      assertNull(JdbcUtils.getDatabaseFromConnectionUrl(""));
      assertNull(JdbcUtils.getDatabaseFromConnectionUrl("   "));
      assertNull(JdbcUtils.getDatabaseFromConnectionUrl("jdbc:clickhouse://localhost"));
      assertNull(JdbcUtils.getDatabaseFromConnectionUrl("jdbc:postgresql://localhost"));
  }
  
  @Test
  void testA() {
    String[] testUrls = {
        "jdbc:postgresql:d42",
        "jdbc:clickhouse://localhost:8123/d42",
        "jdbc:clickhouse://myserver.org/d42",
        "jdbc:clickhouse://localhost:8123/d42?time_to_live=12&my_prop=14",
        "jdbc:ch://my-server:8123/system",
        "jdbc:mysql://FE_IP:FE_PORT/demo?useUnicode=true&characterEncoding=utf8",
        "jdbc:postgresql://localhost:5432/mydb",
        "jdbc:mysql://127.0.0.1:3306/mydatabase"
    };

    for (String url : testUrls) {
        try {
            System.out.printf("URL: %-80s => DB Name: %s%n", url, JdbcUtils.getDatabaseFromConnectionUrl(url));
        } catch (Exception e) {
            System.out.printf("URL: %-80s => ERROR: %s%n", url, e.getMessage());
        }
    }
  }
}
