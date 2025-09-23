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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class JdbcUtilsTest {
  @Test
  void testExtractDatabaseName() {
    // PostgreSQL
    assertDatabase("d42", "jdbc:postgresql:d42");
    assertDatabase("testdb", "jdbc:postgresql://localhost:5432/testdb");
    assertDatabase("dhis2", "jdbc:postgresql://192.168.1.100/dhis2?ssl=true");

    // ClickHouse
    assertDatabase("d42", "jdbc:clickhouse://localhost:8123/d42");
    assertDatabase("d42", "jdbc:clickhouse://myserver.org/d42");
    assertDatabase("d42", "jdbc:clickhouse://localhost:8123/d42?characterEncoding=utf8&my_prop=14");
    assertDatabase("system", "jdbc:ch://play.dhis2.org:8123/system");
    assertDatabase("anotherdb", "jdbc:ch://127.0.0.1/anotherdb");

    // MySQL/Apache Doris
    assertDatabase("demo", "jdbc:mysql://12.34.12.34:9030/demo");
    assertDatabase("demo", "jdbc:mysql://12.34.12.34:9030/demo?useUnicode=true&my_prop=24");
    assertDatabase("sales", "jdbc:mysql://localhost/sales");
    assertDatabase("reports", "jdbc:mysql://10.0.0.5:3306/reports?serverTimezone=UTC");
    assertDatabase("dorisdb", "jdbc:mysql://doris-cluster:9030/dorisdb");

    // Null and corner cases
    assertNull(JdbcUtils.getDatabaseFromUrl(null));
    assertNull(JdbcUtils.getDatabaseFromUrl(""));
    assertNull(JdbcUtils.getDatabaseFromUrl("  "));
    assertNull(JdbcUtils.getDatabaseFromUrl("jdbc:clickhouse://localhost"));
    assertNull(JdbcUtils.getDatabaseFromUrl("jdbc:postgresql://localhost"));
    assertNull(JdbcUtils.getDatabaseFromUrl("jdbc:mysql://localhost:3306/"));
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
