/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.util;

import static org.hisp.dhis.util.SqlExceptionUtils.columnDoesNotExist;
import static org.hisp.dhis.util.SqlExceptionUtils.unionColumnsDontMatch;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link SqlExceptionUtils}. */
class SqlExceptionUtilsTest {

  /** Tests for {@link SqlExceptionUtils#columnDoesNotExist(SQLException)}. */
  @Nested
  class ColumnDoesNotExist {

    @Test
    void returnsFalseForNullException() {
      assertFalse(columnDoesNotExist(null));
    }

    @Test
    void returnsFalseForNullMessageAndNullSqlState() {
      assertFalse(columnDoesNotExist(new SQLException()));
    }

    @Test
    void returnsTrueForPostgresUndefinedColumn() {
      assertTrue(
          columnDoesNotExist(new SQLException("ERROR: column \"abc\" does not exist", "42703")));
    }

    @Test
    void returnsTrueForPostgresSqlStateWithoutMessage() {
      assertTrue(columnDoesNotExist(new SQLException(null, "42703")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"42P01", "42601", "42704", "4270", "XX000"})
    void returnsFalseForOtherPostgresSqlStates(String sqlState) {
      assertFalse(columnDoesNotExist(new SQLException("ERROR: something went wrong", sqlState)));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "unknown column 'abc'",
          "COLUMN 'abc' DOES NOT EXIST IN THE TABLE",
          "unknown_identifier"
        })
    void matchingIsCaseSensitive(String message) {
      assertFalse(columnDoesNotExist(new SQLException(message)));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "",
          "column 'abc' is ambiguous",
          "column 'abc' does not exist",
          "syntax error at or near \"select\"",
          "Table 'analytics' does not exist in database 'dhis2'",
          "Unknown table expression identifier 'analytics'"
        })
    void returnsFalseForUnrelatedMessages(String message) {
      assertFalse(columnDoesNotExist(new SQLException(message, "42601")));
    }
  }

  /** Tests for {@link SqlExceptionUtils#unionColumnsDontMatch(SQLException)}. */
  @Nested
  class UnionColumnsDontMatch {

    @Test
    void returnsFalseForNullException() {
      assertFalse(unionColumnsDontMatch(null));
    }

    @Test
    void returnsFalseForNullMessageAndNullSqlState() {
      assertFalse(unionColumnsDontMatch(new SQLException()));
    }

    @Test
    void returnsTrueForPostgresUnionColumnMismatch() {
      assertTrue(
          unionColumnsDontMatch(
              new SQLException(
                  "ERROR: each UNION query must have the same number of columns", "42601")));
    }

    @Test
    void returnsFalseForPostgresSqlStateWithoutMessage() {
      assertFalse(unionColumnsDontMatch(new SQLException(null, "42601")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"42P01", "42703", "42602", "4260", "XX000"})
    void returnsFalseForOtherPostgresSqlStates(String sqlState) {
      assertFalse(unionColumnsDontMatch(new SQLException("ERROR: something went wrong", sqlState)));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {"operands have unequal number of columns", "different number of columns"})
    void matchingIsCaseSensitive(String message) {
      assertFalse(unionColumnsDontMatch(new SQLException(message)));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "",
          "Operands have unequal number of rows",
          "Different number of rows",
          "Table 'analytics' does not exist in database 'dhis2'",
          "Unknown column 'abc' in table 'analytics'"
        })
    void returnsFalseForUnrelatedMessages(String message) {
      assertFalse(unionColumnsDontMatch(new SQLException(message, "42703")));
    }
  }
}
