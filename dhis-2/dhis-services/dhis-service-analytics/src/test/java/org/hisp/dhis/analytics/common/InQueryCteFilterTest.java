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
package org.hisp.dhis.analytics.common;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class InQueryCteFilterTest {

  @Test
  void testMultipleNumericValues() {

    InQueryCteFilter inQueryCteFilter = createFilter("age", "10;11;12", false);
    String actual = inQueryCteFilter.getSqlFilter(0);
    String expectedEnding = "_0.age in (10,11,12)";
    assertTrue(
        actual.endsWith(expectedEnding), () -> "Expected string to end with: " + expectedEnding);
  }

  @Test
  void testMultipleTextValues() {
    InQueryCteFilter inQueryCteFilter = createFilter("name", "john;jack", true);
    String actual = inQueryCteFilter.getSqlFilter(1);
    String expectedEnding = "_1.name in ('john','jack')";
    assertTrue(
        actual.endsWith(expectedEnding), () -> "Expected string to end with: " + expectedEnding);
  }

  @Test
  void testNullValueInFilter() {
    InQueryCteFilter inQueryCteFilter =
        new InQueryCteFilter("name", "NV", true, new CteDefinition("select", "cteDefinition"));
    String actual = inQueryCteFilter.getSqlFilter(0);
    String exp1 = "_0.enrollment is not null and";
    String exp2 = "_0.name is null";

    assertTrue(actual.contains(exp1), () -> "Expected string to contain: " + exp1);
    assertTrue(actual.contains(exp2), () -> "Expected string to contain: " + exp2);
  }

  @Test
  void testSingleValue() {
    InQueryCteFilter inQueryCteFilter = createFilter("age", "10", false);
    String actual = inQueryCteFilter.getSqlFilter(0);
    String expectedEnding = "_0.age in (10)";
    assertTrue(
        actual.endsWith(expectedEnding), () -> "Expected string to end with: " + expectedEnding);
  }

  @Test
  void testSpecialCharactersInTextFilter() {
    InQueryCteFilter inQueryCteFilter = createFilter("name", "O'Connor;Smith-Jones", true);
    String actual = inQueryCteFilter.getSqlFilter(0);
    String expectedEnding = "_0.name in ('O''Connor','Smith-Jones')";
    System.out.println(actual);
    assertTrue(
        actual.endsWith(expectedEnding), () -> "Expected string to end with: " + expectedEnding);
  }

  @Test
  void testWhitespaceInFilter() {
    InQueryCteFilter inQueryCteFilter = createFilter("name", "John Doe; Jane Smith ", true);
    String actual = inQueryCteFilter.getSqlFilter(0);
    String expectedEnding = "_0.name in ('John Doe','Jane Smith')";
    assertTrue(
        actual.endsWith(expectedEnding), () -> "Expected string to end with: " + expectedEnding);
  }

  @Test
  void testLargeNumberOfValues() {
    // Test with a large number of values to ensure no performance issues
    String manyValues =
        IntStream.range(1, 1001).mapToObj(String::valueOf).collect(Collectors.joining(";"));
    InQueryCteFilter inQueryCteFilter = createFilter("id", manyValues, false);
    String actual = inQueryCteFilter.getSqlFilter(0);
    assertTrue(actual.contains("_0.id in (1,2,3"));
    assertTrue(actual.endsWith("1000)"));
  }

  @Test
  void testDuplicateValues() {
    InQueryCteFilter inQueryCteFilter = createFilter("age", "10;10;10", false);
    String actual = inQueryCteFilter.getSqlFilter(0);
    String expectedEnding = "_0.age in (10,10,10)";
    assertTrue(
        actual.endsWith(expectedEnding), () -> "Expected string to end with: " + expectedEnding);
  }

  @Test
  void testQuoteEscaping() {
    InQueryCteFilter inQueryCteFilter =
        createFilter("name", "O'Connor;McDonald's;Smith's-Jones", true);
    String actual = inQueryCteFilter.getSqlFilter(0);
    String expectedEnding = "_0.name in ('O''Connor','McDonald''s','Smith''s-Jones')";
    assertTrue(
        actual.endsWith(expectedEnding), () -> "Expected string to end with: " + expectedEnding);
  }

  private InQueryCteFilter createFilter(String field, String filter, boolean isText) {
    return new InQueryCteFilter(
        field, filter, isText, new CteDefinition("programStageUid", "cteDefinition"));
  }
}
