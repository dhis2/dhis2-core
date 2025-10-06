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
package org.hisp.dhis.db.util;

import static org.hisp.dhis.db.util.JdbcUtils.POSTGRESQL_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import org.junit.jupiter.api.Test;

class JdbcUtilsTest {
  @Test
  void testExtractHost() {
    // PostgreSQL
    assertHost("localhost", "jdbc:postgresql:d42");
    assertHost("localhost", "jdbc:postgresql://localhost:5432/prod");
    assertHost("192.168.1.100", "jdbc:postgresql://192.168.1.100/dhis2?ssl=true");

    // ClickHouse
    assertHost("localhost", "jdbc:clickhouse://localhost:8123/d42");
    assertHost("127.0.0.1", "jdbc:ch://127.0.0.1/logistics");
    assertHost("134.12.134.14", "jdbc:clickhouse://134.12.134.14:8123/analytics?ssl=true");

    // MySQL/Apache Doris
    assertHost("localhost", "jdbc:mysql://localhost/sales");
    assertHost("12.34.12.34", "jdbc:mysql://12.34.12.34:9030/demo");
  }

  @Test
  void testExtractPort() {
    // PostgreSQL
    assertPort(5432, "jdbc:postgresql:d42");
    assertPort(5432, "jdbc:postgresql:d42");
    assertPort(5432, "jdbc:postgresql://localhost:5432/prod");
    assertPort(5439, "jdbc:postgresql://localhost:5439/prod");
    assertPort(5439, "jdbc:postgresql://34.234.34.234:5439/dev");
    assertPort(5439, "jdbc:postgresql://34.234.34.234:5439/dev");
    assertPort(-1, "https://www.postgresql.org/download/");

    // ClickHouse
    assertPort(8123, "jdbc:clickhouse://localhost:8123/d42");
    assertPort(-1, "jdbc:clickhouse://localhost/d42");
    assertPort(8123, "jdbc:clickhouse://localhost:8123/analytics?ssl=true&user=admin");
    assertPort(-1, "jdbc:ch://play.dhis2.org/emis");

    // MySQL/Apache Doris
    assertPort(9030, "jdbc:mysql://localhost:9030/sales");
    assertPort(-1, "jdbc:mysql://localhost/sales");
    assertPort(9030, "jdbc:mysql://12.34.12.34:9030/demo");
  }

  @Test
  void testExtractPortWithDefault() {
    // PostgreSQL
    assertPortDefault(5432, "jdbc:postgresql:d42", POSTGRESQL_PORT);
    assertPortDefault(5432, "jdbc:postgresql:d42", POSTGRESQL_PORT);
    assertPortDefault(5432, "jdbc:postgresql://localhost:5432/prod", POSTGRESQL_PORT);
    assertPortDefault(5439, "jdbc:postgresql://localhost:5439/prod", POSTGRESQL_PORT);
    assertPortDefault(5439, "jdbc:postgresql://34.234.34.234:5439/dev", POSTGRESQL_PORT);
    assertPortDefault(5432, "https://www.postgresql.org/download/", POSTGRESQL_PORT);

    // ClickHouse
    assertPortDefault(8123, "jdbc:clickhouse://localhost:8123/d42", 8123);
    assertPortDefault(8123, "jdbc:clickhouse://localhost/d42", 8123);
    assertPortDefault(8123, "jdbc:clickhouse://localhost:8123/analytics?ssl=true", 8123);
    assertPortDefault(8123, "jdbc:ch://play.dhis2.org/emis", 8123);

    // MySQL/Apache Doris
    assertPortDefault(9030, "jdbc:mysql://localhost:9030/sales", 9030);
    assertPortDefault(9030, "jdbc:mysql://localhost/sales", 9030);
    assertPortDefault(9030, "jdbc:mysql://12.34.12.34:9030/demo", 9030);
  }

  @Test
  void testExtractDatabaseName() {
    // PostgreSQL
    assertDatabase("d42", "jdbc:postgresql:d42");
    assertDatabase("dhis2", "jdbc:postgresql:dhis2");
    assertDatabase("prod", "jdbc:postgresql://localhost:5432/prod");
    assertDatabase("dhis2", "jdbc:postgresql://192.168.1.100/dhis2?ssl=true");
    assertDatabase("dhis2", "jdbc:postgresql://192.168.1.100:5439/dhis2?ssl=true");
    assertDatabase("hmis", "jdbc:postgresql://db.dhis2.org/hmis?timeout=180");

    // ClickHouse
    assertDatabase("d42", "jdbc:clickhouse://localhost:8123/d42");
    assertDatabase("d41", "jdbc:clickhouse://myserver.org/d41");
    assertDatabase("d42", "jdbc:clickhouse://localhost:8123/d42?characterEncoding=utf8");
    assertDatabase("emis", "jdbc:ch://play.dhis2.org:8123/emis");
    assertDatabase("logistics", "jdbc:ch://127.0.0.1/logistics");
    assertDatabase("analytics", "jdbc:clickhouse://localhost:8123/analytics?ssl=true&user=admin");
    assertDatabase("dhis2", "jdbc:clickhouse://localhost:8123/dhis2?");
    assertDatabase("dev-copy_2025", "jdbc:clickhouse://localhost:8123/dev-copy_2025");

    // MySQL/Apache Doris
    assertDatabase("sales", "jdbc:mysql://localhost/sales");
    assertDatabase("demo", "jdbc:mysql://12.34.12.34:9030/demo");
    assertDatabase("demo", "jdbc:mysql://12.34.12.34:9030/demo?useUnicode=true&my_prop=24");
    assertDatabase("reports", "jdbc:mysql://10.0.0.5:3306/reports?serverTimezone=UTC");
    assertDatabase("dorisdb", "jdbc:mysql://doris-cluster:9030/dorisdb");

    // Null and corner cases
    assertNull(JdbcUtils.getDatabaseFromUrl(null));
    assertNull(JdbcUtils.getDatabaseFromUrl(""));
    assertNull(JdbcUtils.getDatabaseFromUrl("  "));
    assertNull(JdbcUtils.getDatabaseFromUrl("invalid-url"));
    assertNull(JdbcUtils.getDatabaseFromUrl("http://localhost:8123/db"));
    assertNull(JdbcUtils.getDatabaseFromUrl("clickhouse://localhost:8123/db"));
    assertNull(JdbcUtils.getDatabaseFromUrl("jdbc:postgresql:"));
    assertNull(JdbcUtils.getDatabaseFromUrl("jdbc:clickhouse://localhost"));
    assertNull(JdbcUtils.getDatabaseFromUrl("jdbc:clickhouse://localhost:8123/"));
    assertNull(JdbcUtils.getDatabaseFromUrl("jdbc:postgresql://localhost"));
    assertNull(JdbcUtils.getDatabaseFromUrl("jdbc:mysql://localhost:3306/"));
  }

  @Test
  void testIsJdbcUrl() {
    assertTrue(JdbcUtils.isJdbcUrl("jdbc:postgresql:d42"));
    assertTrue(JdbcUtils.isJdbcUrl("jdbc:postgresql://localhost:5432/prod"));
    assertFalse(JdbcUtils.isJdbcUrl("https://www.postgresql.org/download/"));
  }

  @Test
  void testIsPostgreSqlSimpleFormat() {
    assertTrue(JdbcUtils.isPostgreSqlSimpleFormat("jdbc:postgresql:d42"));
    assertFalse(JdbcUtils.isPostgreSqlSimpleFormat("jdbc:postgresql://localhost:5432/prod"));
  }

  @Test
  void testToUri() {
    URI uri = JdbcUtils.toUri("jdbc:postgresql://192.168.1.100:5439/dhis2?ssl=true");

    assertEquals("192.168.1.100", uri.getHost());
    assertEquals(5439, uri.getPort());
    assertEquals("/dhis2", uri.getPath());
    assertEquals("ssl=true", uri.getQuery());
  }

  /**
   * Helper method to assert host extraction.
   *
   * @param host the expected host.
   * @param jdbcUrl the JDBC connection URL.
   */
  private void assertHost(String host, String jdbcUrl) {
    assertEquals(host, JdbcUtils.getHostFromUrl(jdbcUrl));
  }

  /**
   * Helper method to assert port extraction.
   *
   * @param port the expected port.
   * @param jdbcUrl the JDBC connection URL.
   */
  private void assertPort(int port, String jdbcUrl) {
    assertEquals(port, JdbcUtils.getPortFromUrl(jdbcUrl));
  }

  /**
   * Helper method to assert host extraction with default port.
   *
   * @param port the expected port number.
   * @param jdbcUrl the JDBC connection URL.
   * @param defaultPort the default port number.
   */
  private void assertPortDefault(int port, String jdbcUrl, int defaultPort) {
    assertEquals(port, JdbcUtils.getPortFromUrl(jdbcUrl, defaultPort));
  }

  /**
   * Helper method to assert database extraction.
   *
   * @param database the database name.
   * @param jdbcUrl the JDBC connection URL.
   */
  private void assertDatabase(String database, String jdbcUrl) {
    assertEquals(database, JdbcUtils.getDatabaseFromUrl(jdbcUrl));
  }
}
