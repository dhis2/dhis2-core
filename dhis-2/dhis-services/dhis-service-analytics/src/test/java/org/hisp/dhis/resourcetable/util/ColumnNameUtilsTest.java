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
package org.hisp.dhis.resourcetable.util;

import static org.hisp.dhis.resourcetable.util.ColumnNameUtils.toValidColumnName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ColumnNameUtilsTest {

  @Test
  void testToValidColumnName() {
    assertNull(toValidColumnName(null));
    assertEquals("", toValidColumnName(""));

    assertEquals("FacilityType", toValidColumnName("FacilityType"));
    assertEquals("Facility type", toValidColumnName("Facility type"));
    assertEquals("Facility_Type", toValidColumnName("Facility_Type"));
    assertEquals("Facility-Type", toValidColumnName("Facility-Type"));

    assertEquals("Age in years", toValidColumnName("Age in years"));
    assertEquals("Age&in*years", toValidColumnName("Age&in*years"));
    assertEquals("Age#in@years", toValidColumnName("Age#in@years"));
    assertEquals("Age^in$years", toValidColumnName("Age^in$years"));

    assertEquals("Facility_Type", toValidColumnName("Facility!Type"));
    assertEquals("Facility_Type", toValidColumnName("Facility!!!Type"));
    assertEquals("Facility_Type_", toValidColumnName("Facility(Type)"));
    assertEquals("Facility_Type_", toValidColumnName("(Facility)(Type)"));

    assertEquals("Age_in_years", toValidColumnName("Age=in=years"));
    assertEquals("Age _in_ _years_", toValidColumnName("Age [in] [years]"));
    assertEquals("Age_ _in_ _years_", toValidColumnName("[[[Age]]] [in] [years]"));
    assertEquals("Age_ _in_ _years_", toValidColumnName("[[[Age]]] [[in]] [[years]]"));
  }

  @Test
  @DisplayName("Valid simple name should remain unchanged")
  void testValidSimpleName() {
    assertEquals("ValidName", toValidColumnName("ValidName"));
  }

  @Test
  @DisplayName("Valid name with numbers should remain unchanged")
  void testValidNameWithNumbers() {
    assertEquals("ValidName123", toValidColumnName("ValidName123"));
  }

  @Test
  @DisplayName("Valid name with underscores should remain unchanged")
  void testValidNameWithUnderscores() {
    assertEquals("Valid_Name_123", toValidColumnName("Valid_Name_123"));
  }

  @Test
  @DisplayName("Single valid character (letter)")
  void testSingleValidLetter() {
    assertEquals("a", toValidColumnName("a"));
  }

  // --- Null, Empty, Blank Inputs ---

  @Test
  @DisplayName("Empty input should return empty string")
  void testEmptyInput() {
    assertEquals("", toValidColumnName(""));
  }

  @Test
  @DisplayName("Single quote should be replaced with underscore")
  void testSingleQuoteReplacement() {
    assertEquals("Valid_Name_", toValidColumnName("'Valid'Name'"));
  }

  @Test
  @DisplayName("Maximum valid length (128)")
  void testMaxLengthValid() {
    String maxLengthName = "A".repeat(127);
    assertEquals(maxLengthName, toValidColumnName(maxLengthName));
  }

  @Test
  @DisplayName("Exceeding max length (128) should be truncated")
  void testTooLongTruncated() {
    String tooLongName = "A".repeat(129);
    String expected = "A".repeat(128);
    assertEquals(expected, toValidColumnName(tooLongName));
  }

  @Test
  @DisplayName("Exceeding max length significantly should be truncated")
  void testVeryLongTruncated() {
    String veryLongName = "Long".repeat(50); // 200 chars
    String expected = "Long".repeat(32);
    assertEquals(expected, toValidColumnName(veryLongName));
  }
}
