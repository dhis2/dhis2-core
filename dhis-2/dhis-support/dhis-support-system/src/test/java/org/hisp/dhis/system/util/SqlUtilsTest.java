/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.system.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Lars Helge Overland
 */
class SqlUtilsTest {

  @Test
  void testQuote() {
    assertEquals("\"Some \"\"special\"\" value\"", SqlUtils.quote("Some \"special\" value"));
    assertEquals("\"Data element\"", SqlUtils.quote("Data element"));
    assertEquals(
        "\"Prescribed drug \"\"rx01\"\" to 'John White'\"",
        SqlUtils.quote("Prescribed drug \"rx01\" to 'John White'"));
  }

  @Test
  void testQuoteWithAlias() {
    assertEquals("ougs.\"Short name\"", SqlUtils.quote("ougs", "Short name"));
    assertEquals("ous.\"uid\"", SqlUtils.quote("ous", "uid"));
  }

  @Test
  void testSingleQuote() {
    assertEquals("'jkhYg65ThbF'", SqlUtils.singleQuote("jkhYg65ThbF"));
    assertEquals("'Some ''special'' value'", SqlUtils.singleQuote("Some 'special' value"));
    assertEquals("'Another \"strange\" value'", SqlUtils.singleQuote("Another \"strange\" value"));
    assertEquals("'John White'", SqlUtils.singleQuote("John White"));
    assertEquals(
        "'Main St 1\\\\nSmallwille\\\\n'", SqlUtils.singleQuote("Main St 1\\nSmallwille\\n"));
    assertEquals(
        "'Provided ''Rx01'' to patient'", SqlUtils.singleQuote("Provided 'Rx01' to patient"));
  }

  @Test
  void testEscape() {
    assertEquals("John White", SqlUtils.escape("John White"));
    assertEquals("Main St 1\\\\nSmallwille\\\\n", SqlUtils.escape("Main St 1\\nSmallwille\\n"));
    assertEquals("Provided ''Rx01'' to patient", SqlUtils.escape("Provided 'Rx01' to patient"));
    assertEquals("Some ''special'' value", SqlUtils.escape("Some 'special' value"));
    assertEquals("Prescribed ''rx82''", SqlUtils.escape("Prescribed 'rx82'"));
    assertEquals("Some regular value", SqlUtils.escape("Some regular value"));
    assertEquals("C:\\\\Downloads\\\\Temp", SqlUtils.escape("C:\\Downloads\\Temp"));
  }

  @ParameterizedTest
  @DisplayName("should throw an exception with incorrect args")
  @MethodSource("multiLIkeParams")
  void multiLikeTest(String column, List<String> targets) {
    assertThrows(IllegalArgumentException.class, () -> SqlUtils.likeAny(column, targets));
  }

  @Test
  @DisplayName("should return string with multi like query when 1 param search")
  void multiLikeSingleTest() {
    // given
    List<String> searches = List.of("search1");

    // when
    String multiLike = SqlUtils.likeAny("col", searches);

    // then
    assertEquals("col like '%search1%'", multiLike);
  }

  @Test
  @DisplayName("should return string with multi like query when multiple param searches")
  void multiLikeMultiTest() {
    // given
    List<String> searches = List.of("search1", "search2", "search3");

    // when
    String multiLike = SqlUtils.likeAny("col", searches);

    // then
    assertEquals("col like '%search1%' or col like '%search2%' or col like '%search3%'", multiLike);
  }

  public static Stream<Arguments> multiLIkeParams() {
    return Stream.of(
        Arguments.of(null, null),
        Arguments.of(null, List.of("search1")),
        Arguments.of("column1", null),
        Arguments.of("column1", List.of()));
  }
}
