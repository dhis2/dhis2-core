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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DorisAnalyticsSqlBuilderTest {

  private DorisAnalyticsSqlBuilder sqlBuilder;

  @BeforeEach
  void setUp() {
    sqlBuilder = new DorisAnalyticsSqlBuilder("pg_dhis", "postgresql.jar");
  }

  @Test
  void renderTimestampValidTimestampShouldFormatCorrectly() {
    String input = "2023-10-20T15:30:45";
    String expected = "2023-10-20 15:30:45.0";
    assertEquals(expected, sqlBuilder.renderTimestamp(input));
  }

  @Test
  void renderTimestampTimestampWithZeroMillisShouldTrimTrailingZeros() {
    String input = "2023-10-20T15:30:45";
    String result = sqlBuilder.renderTimestamp(input);
    assertFalse(result.endsWith("000"));
    assertTrue(result.endsWith(".0"));
  }

  @Test
  void renderTimestampTimestampWithNonZeroMillisShouldKeepMillis() {
    String input = "2023-10-20T15:30:45.123";
    String expected = "2023-10-20 15:30:45.123";
    assertEquals(expected, sqlBuilder.renderTimestamp(input));
  }

  /** Tests that null input returns null output */
  @Test
  void renderTimestampNullInputShouldReturnNull() {
    assertNull(sqlBuilder.renderTimestamp(null));
  }

  /** Tests that empty string input returns null output */
  @Test
  void renderTimestampEmptyStringShouldReturnNull() {
    assertNull(sqlBuilder.renderTimestamp(""));
  }

  /** Tests that blank string input returns null output */
  @Test
  void renderTimestampBlankStringShouldReturnNull() {
    assertNull(sqlBuilder.renderTimestamp("   "));
  }

  /** Tests that invalid timestamp format throws appropriate exception */
  @Test
  void renderTimestampInvalidFormatShouldThrowException() {
    String invalidInput = "2023-13-45 25:65:99"; // Invalid date/time values
    assertThrows(DateTimeParseException.class, () -> sqlBuilder.renderTimestamp(invalidInput));
  }

  @Test
  void renderTimestampAllZeroMillisShouldTrimToSingleZero() {
    String input = "2023-10-20T15:30:45.000";
    String expected = "2023-10-20 15:30:45.0";
    assertEquals(expected, sqlBuilder.renderTimestamp(input));
  }

  @Test
  void renderTimestampTwoTrailingZerosShouldTrimBothZeros() {
    String input = "2023-10-20T15:30:45.400";
    String expected = "2023-10-20 15:30:45.4";
    assertEquals(expected, sqlBuilder.renderTimestamp(input));
  }

  @Test
  void renderTimestampOneTrailingZeroShouldTrimZero() {
    String input = "2023-10-20T15:30:45.420";
    String expected = "2023-10-20 15:30:45.42";
    assertEquals(expected, sqlBuilder.renderTimestamp(input));
  }

  @Test
  void renderTimestampNoTrailingZerosShouldNotTrim() {
    String input = "2023-10-20T15:30:45.123";
    String expected = "2023-10-20 15:30:45.123";
    assertEquals(expected, sqlBuilder.renderTimestamp(input));
  }
}
