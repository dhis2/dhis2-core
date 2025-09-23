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

import static org.hisp.dhis.db.util.JdbcUtils.getDatabaseFromUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class JdbcUtilsTest {
  @Test
  void testExtractDatabaseName() {
    // PostgreSQL
    assertEquals("d42", getDatabaseFromUrl("jdbc:postgresql:d42"));
    assertEquals("testdb", getDatabaseFromUrl("jdbc:postgresql://localhost:5432/testdb"));
    assertEquals("mydb", getDatabaseFromUrl("jdbc:postgresql://192.168.1.100/mydb?ssl=true"));

    // ClickHouse
    assertEquals("d42", getDatabaseFromUrl("jdbc:clickhouse://localhost:8123/d42"));
    assertEquals("d42", getDatabaseFromUrl("jdbc:clickhouse://myserver.org/d42"));
    assertEquals(
        "d42",
        getDatabaseFromUrl("jdbc:clickhouse://localhost:8123/d42?time_to_live=12&my_prop=14"));
    assertEquals("system", getDatabaseFromUrl("jdbc:ch://my-server:8123/system"));
    assertEquals("anotherdb", getDatabaseFromUrl("jdbc:ch://127.0.0.1/anotherdb"));

    // Test cases for MySQL and Apache Doris (using MySQL protocol)
    assertEquals("demo", getDatabaseFromUrl("jdbc:mysql://12.34.12.34:9030/demo"));
    assertEquals(
        "demo",
        getDatabaseFromUrl(
            "jdbc:mysql://12.34.12.34:9030/demo?useUnicode=true&characterEncoding=utf8"));
    assertEquals("sales", getDatabaseFromUrl("jdbc:mysql://localhost/sales"));
    assertEquals(
        "reports", getDatabaseFromUrl("jdbc:mysql://10.0.0.5:3306/reports?serverTimezone=UTC"));
    assertEquals("dorisdb", getDatabaseFromUrl("jdbc:mysql://doris-cluster:9030/dorisdb"));
    assertNull(getDatabaseFromUrl("jdbc:mysql://localhost:3306/"));

    // General and corner cases
    assertNull(getDatabaseFromUrl(null));
    assertNull(getDatabaseFromUrl(""));
    assertNull(getDatabaseFromUrl("   "));
    assertNull(getDatabaseFromUrl("jdbc:clickhouse://localhost"));
    assertNull(getDatabaseFromUrl("jdbc:postgresql://localhost"));
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
        System.out.printf("URL: %-80s => DB Name: %s%n", url, getDatabaseFromUrl(url));
      } catch (Exception e) {
        System.out.printf("URL: %-80s => ERROR: %s%n", url, e.getMessage());
      }
    }
  }
}
