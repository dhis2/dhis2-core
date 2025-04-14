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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ColumnNameUtilsTest {

  @Test
  @DisplayName("Valid simple name should remain unchanged")
  void testValidSimpleName() {
    assertEquals("ValidName", toValidColumnName("ValidName"));
  }

  @Test
  @DisplayName("Valid name starting with underscore should remain unchanged")
  void testValidNameStartingWithUnderscore() {
    assertEquals("_ValidName", toValidColumnName("_ValidName"));
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

  @Test
  @DisplayName("Single valid character (underscore)")
  void testSingleValidUnderscore() {
    assertEquals("_", toValidColumnName("_"));
  }

  // --- Null, Empty, Blank Inputs ---

  @Test
  @DisplayName("Null input should return empty string")
  void testNullInput() {
    assertEquals("", toValidColumnName(null));
  }

  @Test
  @DisplayName("Empty input should return empty string")
  void testEmptyInput() {
    assertEquals("", toValidColumnName(""));
  }

  @ParameterizedTest
  @ValueSource(strings = {" ", "   ", "\t", "\n"})
  @DisplayName("Blank input should return default underscore")
  void testBlankInput(String blankInput) {
    assertEquals("", toValidColumnName(blankInput));
  }

  @ParameterizedTest
  @CsvSource({
    "'Valid Name', 'Valid_Name'", // Space
    "'Valid\"Name', 'Valid_Name'", // Double Quote
    "'Valid`Name', 'Valid_Name'", // Backtick
    "'Valid$Name', 'Valid_Name'", // Symbol
    "'Valid-Name', 'Valid_Name'", // Hyphen
    "'Valid+Name', 'Valid_Name'", // Plus
    "'Valid/Name', 'Valid_Name'", // Slash
    "'Valid\\Name', 'Valid_Name'", // Backslash
    "'Valid:Name', 'Valid_Name'", // Colon
    "'Valid.Name', 'Valid_Name'", // Period
    "'Valid,Name', 'Valid_Name'", // Comma
    "'Valid(Name)', 'Valid_Name_'", // Parentheses
    "'Naïve', 'Na_ve'", // Non-ASCII character
    "'你好世界', '____'", // Multi-byte characters -> Replaced
    "' leadingSpace', '_leadingSpace'", // Leading space replaced
    "'trailingSpace ', 'trailingSpace_'" // Trailing space replaced
  })
  @DisplayName("Invalid characters should be replaced with underscore")
  void testInvalidCharactersReplacement(String input, String expected) {
    assertEquals(expected, toValidColumnName(input));
  }

  @Test
  @DisplayName("Single quote should be replaced with underscore")
  void testSingleQuoteReplacement() {
    assertEquals("_Valid_Name_", toValidColumnName("'Valid'Name'"));
  }

  @Test
  @DisplayName("Multiple consecutive invalid characters")
  void testMultipleConsecutiveInvalid() {
    assertEquals("Valid___Name", toValidColumnName("Valid $ Name"));
    assertEquals("___", toValidColumnName("!@#"));
  }

  @ParameterizedTest
  @CsvSource({
    "'1Name', '_1Name'", // Digit start
    "'$Name', '_Name'", // Symbol start -> Symbol replaced, result starts with 'N' (valid)
    "' Name', '_Name'", // Space start -> Space replaced, result starts with 'N' (valid)
    "'\"Name', '_Name'", // Quote start -> Quote replaced, result starts with 'N' (valid)
    "'1$Name', '_1_Name'", // Digit start, followed by symbol replacement
    "'$1Name', '_1Name'", // Symbol start replaced, result starts with '1' -> prepend _
    "'!@#Name', '___Name'", // Multiple invalid start -> Replaced, starts with 'N'
    "'123OnlyDigits', '_123OnlyDigits'", // Only digits
    "' F C ', '_F_C_'" // Spaces and valid chars
  })
  @DisplayName("Invalid starting character handling")
  void testInvalidStartingCharacter(String input, String expected) {
    assertEquals(expected, toValidColumnName(input));
  }

  @Test
  @DisplayName("Maximum valid length (127)")
  void testMaxLengthValid() {
    String maxLengthName = "A".repeat(127);
    assertEquals(maxLengthName, toValidColumnName(maxLengthName));
  }

  @Test
  @DisplayName("Maximum valid length (127) starting with underscore")
  void testMaxLengthValidUnderscoreStart() {
    String maxLengthName = "_" + "A".repeat(126);
    assertEquals(maxLengthName, toValidColumnName(maxLengthName));
  }

  @Test
  @DisplayName("Exceeding max length (128) should be truncated")
  void testTooLongTruncated() {
    String tooLongName = "A".repeat(128);
    String expected = "A".repeat(127);
    assertEquals(expected, toValidColumnName(tooLongName));
  }

  @Test
  @DisplayName("Exceeding max length significantly should be truncated")
  void testVeryLongTruncated() {
    String veryLongName = "Long".repeat(50); // 200 chars
    String expected = "Long".repeat(31) + "Lon"; // 31*4 + 3 = 124 + 3 = 127
    assertEquals(expected, toValidColumnName(veryLongName));
  }

  @Test
  @DisplayName("Too long name with invalid chars at the end")
  void testTooLongWithInvalidCharsEnd() {
    String name = "A".repeat(126) + "$$"; // 128 chars total
    String expected = "A".repeat(126) + "_"; // $ replaced, then truncated
    // Trace: Build A(126) + "_". Builder length 127. Loop stops. Result: A(126) + "_".
    assertEquals(expected, toValidColumnName(name));
  }

  @Test
  @DisplayName("Too long name needing start prepend and truncation")
  void testTooLongWithInvalidStartAndTruncation() {
    String name = "1" + "A".repeat(127); // 128 chars total
    // Trace: Build "1" + A(126). Builder length 127. Loop stops.
    // Result: "1" + A(126). First char '1' invalid. Prepend _: "_" + "1" + A(126). Length 128.
    // Truncate to 127: "_" + "1" + A(125).
    String expected = "_1" + "A".repeat(125);
    assertEquals(expected, toValidColumnName(name));
  }

  @Test
  @DisplayName("Too long name with invalid chars needing replacement and truncation")
  void testTooLongWithReplacementAndTruncation() {
    String name = "A B".repeat(50); // 150 chars total -> A_B...
    String expected = "A_B".repeat(42) + "A"; // 42 * 3 = 126. Add 'A'. Total 127.
    assertEquals(expected, toValidColumnName(name));
  }
}
