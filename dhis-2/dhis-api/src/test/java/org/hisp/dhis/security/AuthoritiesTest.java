/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.security;

import static org.hisp.dhis.security.Authorities.ALL;
import static org.hisp.dhis.security.Authorities.F_SYSTEM_SETTING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AuthoritiesTest {

  @ParameterizedTest
  @MethodSource("authsInputOutput")
  @DisplayName("Converting from Authorities[] to String[] is successful")
  void convertAuthoritiesToStringsTest(
      Authorities[] input, int expectedArraySize, List<String> output) {
    String[] outputStrings = Authorities.toStringArray(input);
    assertNotNull(outputStrings);
    assertEquals(expectedArraySize, outputStrings.length);
    assertTrue(List.of(outputStrings).containsAll(output));
  }

  @ParameterizedTest
  @MethodSource("authsInputOutputList")
  @DisplayName("Converting from Collection<Authorities> to List<String> is successful")
  void convertAuthoritiesToStringListTest(
      Collection<Authorities> input, int expectedListSize, List<String> output) {
    List<String> outputStrings = Authorities.toStringList(input);
    assertNotNull(outputStrings);
    assertEquals(expectedListSize, outputStrings.size());
    assertTrue(outputStrings.containsAll(output));
  }

  private static Stream<Arguments> authsInputOutput() {
    return Stream.of(
        arguments(null, 0, List.of()),
        arguments(new Authorities[0], 0, List.of()),
        arguments(
            new Authorities[] {ALL, F_SYSTEM_SETTING},
            2,
            List.of(ALL.toString(), F_SYSTEM_SETTING.toString())));
  }

  private static Stream<Arguments> authsInputOutputList() {
    return Stream.of(
        arguments(null, 0, List.of()),
        arguments(List.of(), 0, List.of()),
        arguments(
            List.of(ALL, F_SYSTEM_SETTING),
            2,
            List.of(ALL.toString(), F_SYSTEM_SETTING.toString())));
  }
}
