/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.util;

import static org.hisp.dhis.util.SqlExceptionUtils.columnDoesNotExist;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link SqlExceptionUtils#columnDoesNotExist(SQLException)}. */
class SqlExceptionUtilsTest {

  @Test
  void testColumnDoesNotExistWithNullException() {
    assertFalse(columnDoesNotExist(null));
  }

  @Test
  void testColumnDoesNotExistWithNullMessageAndNullSqlState() {
    assertFalse(columnDoesNotExist(new SQLException()));
  }

  @Test
  void testColumnDoesNotExistPostgres() {
    SQLException ex = new SQLException("ERROR: column \"abc\" does not exist", "42703");

    assertTrue(columnDoesNotExist(ex));
  }

  @Test
  void testColumnDoesNotExistPostgresSqlStateWithoutMessage() {
    assertTrue(columnDoesNotExist(new SQLException(null, "42703")));
  }

  @Test
  void testColumnDoesNotExistPostgresMissingTableSqlState() {
    SQLException ex = new SQLException("ERROR: relation \"abc\" does not exist", "42P01");

    assertFalse(columnDoesNotExist(ex));
  }

  @Test
  void testColumnDoesNotExistDoris() {
    SQLException ex =
        new SQLException("errCode = 2, detailMessage = column 'abc' does not exist in the table");

    assertTrue(columnDoesNotExist(ex));
  }

  @Test
  void testColumnDoesNotExistDorisPartialMessage() {
    assertFalse(columnDoesNotExist(new SQLException("column 'abc' is ambiguous")));
    assertFalse(columnDoesNotExist(new SQLException("value does not exist in the table")));
  }

  @Test
  void testColumnDoesNotExistDorisIsCaseSensitive() {
    assertFalse(columnDoesNotExist(new SQLException("Column 'abc' does not exist in the table")));
  }

  @Test
  void testColumnDoesNotExistClickHouse() {
    SQLException ex =
        new SQLException(
            "Code: 47. DB::Exception: Unknown column 'abc' in table 'analytics'. (UNKNOWN_IDENTIFIER)");

    assertTrue(columnDoesNotExist(ex));
  }

  @Test
  void testColumnDoesNotExistClickHouseIsCaseSensitive() {
    assertFalse(columnDoesNotExist(new SQLException("unknown column 'abc'")));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "syntax error at or near \"select\"",
        "Table 'analytics' does not exist in database 'dhis2'",
        "Unknown table expression identifier 'analytics'"
      })
  void testColumnDoesNotExistWithUnrelatedMessages(String message) {
    assertFalse(columnDoesNotExist(new SQLException(message, "42601")));
  }
}
