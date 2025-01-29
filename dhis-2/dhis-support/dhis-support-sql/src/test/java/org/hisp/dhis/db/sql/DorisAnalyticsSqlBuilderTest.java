/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.db.sql;

import static org.junit.jupiter.api.Assertions.*;

import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DorisAnalyticsSqlBuilderTest {

  private DorisAnalyticsSqlBuilder sqlBuilder;

  @BeforeEach
  void setUp() {
    sqlBuilder = new DorisAnalyticsSqlBuilder();
  }

  @Test
  void renderTimestamp_validTimestamp_shouldFormatCorrectly() {
    String input = "2023-10-20T15:30:45";
    String expected = "2023-10-20 15:30:45.0";
    assertEquals(expected, sqlBuilder.renderTimestamp(input));
  }

  @Test
  void renderTimestamp_timestampWithZeroMillis_shouldTrimTrailingZeros() {
    String input = "2023-10-20T15:30:45";
    String result = sqlBuilder.renderTimestamp(input);
    assertFalse(result.endsWith("000"));
    assertTrue(result.endsWith(".0"));
  }

  @Test
  void renderTimestamp_timestampWithNonZeroMillis_shouldKeepMillis() {
    String input = "2023-10-20T15:30:45.123";
    String expected = "2023-10-20 15:30:45.123";
    assertEquals(expected, sqlBuilder.renderTimestamp(input));
  }

  /** Tests that null input returns null output */
  @Test
  void renderTimestamp_nullInput_shouldReturnNull() {
    assertNull(sqlBuilder.renderTimestamp(null));
  }

  /** Tests that empty string input returns null output */
  @Test
  void renderTimestamp_emptyString_shouldReturnNull() {
    assertNull(sqlBuilder.renderTimestamp(""));
  }

  /** Tests that blank string input returns null output */
  @Test
  void renderTimestamp_blankString_shouldReturnNull() {
    assertNull(sqlBuilder.renderTimestamp("   "));
  }

  /** Tests that invalid timestamp format throws appropriate exception */
  @Test
  void renderTimestamp_invalidFormat_shouldThrowException() {
    String invalidInput = "2023-13-45 25:65:99"; // Invalid date/time values
    assertThrows(DateTimeParseException.class, () -> sqlBuilder.renderTimestamp(invalidInput));
  }

  @Test
  void renderTimestamp_allZeroMillis_shouldTrimToSingleZero() {
    String input = "2023-10-20T15:30:45.000";
    String expected = "2023-10-20 15:30:45.0";
    assertEquals(expected, sqlBuilder.renderTimestamp(input));
  }

  @Test
  void renderTimestamp_twoTrailingZeros_shouldTrimBothZeros() {
    String input = "2023-10-20T15:30:45.400";
    String expected = "2023-10-20 15:30:45.4";
    assertEquals(expected, sqlBuilder.renderTimestamp(input));
  }

  @Test
  void renderTimestamp_oneTrailingZero_shouldTrimZero() {
    String input = "2023-10-20T15:30:45.420";
    String expected = "2023-10-20 15:30:45.42";
    assertEquals(expected, sqlBuilder.renderTimestamp(input));
  }

  @Test
  void renderTimestamp_noTrailingZeros_shouldNotTrim() {
    String input = "2023-10-20T15:30:45.123";
    String expected = "2023-10-20 15:30:45.123";
    assertEquals(expected, sqlBuilder.renderTimestamp(input));
  }
}
