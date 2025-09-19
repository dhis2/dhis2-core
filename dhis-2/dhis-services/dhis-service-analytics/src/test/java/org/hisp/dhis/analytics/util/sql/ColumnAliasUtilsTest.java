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
package org.hisp.dhis.analytics.util.sql;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ColumnAliasUtilsTest {

  static Stream<Arguments> qualifiedCases() {
    return Stream.of(
        // input, expectedQualifier, expectedColumn (PRESERVE quotes in column name)
        Arguments.of("t.col", "t", "col"),
        Arguments.of("t.\"col\"", "t", "\"col\""),
        Arguments.of("\"T\".\"C\"", "\"T\"", "\"C\""),
        Arguments.of("schema.table.col", "schema.table", "col"),
        Arguments.of("\"sch\".\"tab\".\"c\"", "\"sch\".\"tab\"", "\"c\""),
        Arguments.of("myalias.\"column_name\"", "myalias", "\"column_name\""),
        Arguments.of("myAlias.\"MixedCase\"", "myAlias", "\"MixedCase\""));
  }

  static Stream<Arguments> unqualifiedCases() {
    return Stream.of(
        Arguments.of("col"),
        Arguments.of("\"col\""),
        Arguments.of("   col   "),
        Arguments.of("COLUMN_ONLY"),
        Arguments.of("\"MIXED_case\""),
        // Expressions (not plain columns) should be treated as unqualified/not-a-column
        Arguments.of("COUNT(*)"),
        Arguments.of("LOWER(name)"),
        Arguments.of("price * quantity"),
        Arguments.of("(a + b)"),
        // Wildcards are not Columns in JSqlParser
        Arguments.of("*"),
        Arguments.of("t.*"),
        // Invalid/garbage should just be treated as not-a-column
        Arguments.of(".."),
        Arguments.of("t."),
        Arguments.of(".col"));
  }

  @DisplayName("isQualifiedWithAlias: true for qualified references")
  @ParameterizedTest(name = "[{index}] {0} -> qualifier: {1}")
  @MethodSource("qualifiedCases")
  void isQualified_trueForQualified(String input, String expectedQualifier, String ignored) {
    assertTrue(ColumnAliasUtils.isQualifiedWithAlias(input));
  }

  @DisplayName("isQualifiedWithAlias: false for unqualified or not-a-column expressions")
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("unqualifiedCases")
  void isQualified_falseForUnqualifiedOrExpressions(String input) {
    assertFalse(ColumnAliasUtils.isQualifiedWithAlias(input));
  }

  @Test
  @DisplayName("isQualifiedWithAlias: handles null and blank safely")
  void isQualified_nullAndBlank() {
    assertFalse(ColumnAliasUtils.isQualifiedWithAlias(null));
    assertFalse(ColumnAliasUtils.isQualifiedWithAlias(""));
    assertFalse(ColumnAliasUtils.isQualifiedWithAlias("   "));
  }

  @DisplayName(
      "extractQualifier: returns prefix (alias/schema.table) for qualified columns (preserving quotes)")
  @ParameterizedTest(name = "[{index}] {0} -> {1}")
  @MethodSource("qualifiedCases")
  void extractQualifier_returnsQualifier(String input, String expectedQualifier, String ignored) {
    assertEquals(expectedQualifier, ColumnAliasUtils.extractQualifier(input));
  }

  @DisplayName("extractQualifier: returns null for unqualified or not-a-column expressions")
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("unqualifiedCases")
  void extractQualifier_returnsNull(String input) {
    assertNull(ColumnAliasUtils.extractQualifier(input));
  }

  @Test
  @DisplayName("extractQualifier: handles null and blank safely")
  void extractQualifier_nullAndBlank() {
    assertNull(ColumnAliasUtils.extractQualifier(null));
    assertNull(ColumnAliasUtils.extractQualifier(""));
    assertNull(ColumnAliasUtils.extractQualifier("   "));
  }

  @DisplayName("unqualify: returns only column name, preserving quotes if present")
  @ParameterizedTest(name = "[{index}] {0} -> {2}")
  @MethodSource("qualifiedCases")
  void unqualify_stripsQualifier(String input, String ignored, String expectedColumn) {
    assertEquals(expectedColumn, ColumnAliasUtils.unqualify(input));
  }

  @DisplayName("unqualify: returns original string when not a simple column")
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("unqualifiedCases")
  void unqualify_returnsOriginalWhenNotPlainColumn(String input) {
    assertEquals(input, ColumnAliasUtils.unqualify(input));
  }

  @Test
  @DisplayName("unqualify: handles null and blank safely")
  void unqualify_nullAndBlank() {
    assertNull(ColumnAliasUtils.unqualify(null));
    assertEquals("", ColumnAliasUtils.unqualify(""));
    assertEquals("   ", ColumnAliasUtils.unqualify("   "));
  }

  @Nested
  @DisplayName("Corner cases")
  class BehaviorDetails {

    @Test
    @DisplayName("Quoted names: qualifier preserved with quotes, column name preserved with quotes")
    void quotedBehavior() {
      String ref = "\"TBL\".\"MixedCaseCol\"";
      assertTrue(ColumnAliasUtils.isQualifiedWithAlias(ref));
      assertEquals("\"TBL\"", ColumnAliasUtils.extractQualifier(ref));
      assertEquals("\"MixedCaseCol\"", ColumnAliasUtils.unqualify(ref));
    }

    @Test
    @DisplayName(
        "Multi-part qualifier: returns schema.table as qualifier; column name keeps quotes")
    void multiPartQualifier() {
      String ref = "schema_x.table_y.\"col_z\"";
      assertEquals("schema_x.table_y", ColumnAliasUtils.extractQualifier(ref));
      assertEquals("\"col_z\"", ColumnAliasUtils.unqualify(ref));
    }

    @Test
    @DisplayName("Wildcard is not a simple column (should not be qualified)")
    void wildcard() {
      assertFalse(ColumnAliasUtils.isQualifiedWithAlias("*"));
      assertFalse(ColumnAliasUtils.isQualifiedWithAlias("t.*"));
      assertEquals("t.*", ColumnAliasUtils.unqualify("t.*")); // unchanged
    }
  }
}
