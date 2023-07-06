/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker.programrule.executor;

import static org.hisp.dhis.tracker.programrule.executor.RuleActionExecutor.isEqual;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.hisp.dhis.common.ValueType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RuleActionExecutorTest {
  private static Stream<Arguments> equalValues() {
    return Stream.of(
        Arguments.of("first_dose", ValueType.TEXT),
        Arguments.of("2020-01-01", ValueType.DATE),
        Arguments.of("true", ValueType.BOOLEAN),
        Arguments.of("26.4", ValueType.TEXT),
        Arguments.of("24.8", ValueType.NUMBER),
        Arguments.of("32", ValueType.INTEGER));
  }

  private static Stream<Arguments> differentValues() {
    return Stream.of(
        Arguments.of("first_dose", "second_dose", ValueType.TEXT),
        Arguments.of("2020-01-01", "2020-01-02", ValueType.DATE),
        Arguments.of("true", "false", ValueType.BOOLEAN),
        Arguments.of("26.4", "26.5", ValueType.TEXT),
        Arguments.of("24.8", "24.9", ValueType.NUMBER),
        Arguments.of("32", "33", ValueType.INTEGER));
  }

  private static Stream<Arguments> valuesOfDifferentTypes() {
    return Stream.of(
        Arguments.of("first_dose", "46.2", ValueType.NUMBER),
        Arguments.of("24", "second_dose", ValueType.NUMBER),
        Arguments.of(null, "46.2", ValueType.NUMBER),
        Arguments.of("26.4", null, ValueType.NUMBER),
        Arguments.of("first_dose", null, ValueType.TEXT),
        Arguments.of(null, "second_dose", ValueType.TEXT));
  }

  @ParameterizedTest
  @MethodSource("equalValues")
  void shouldCheckValuesAreEqual(String value, ValueType valueType) {
    assertTrue(isEqual(value, value, valueType));
  }

  @ParameterizedTest
  @MethodSource("differentValues")
  void shouldCheckDifferentValuesOfSameTypeAreDifferent(
      String value1, String value2, ValueType valueType) {
    assertFalse(isEqual(value1, value2, valueType));
  }

  @ParameterizedTest
  @MethodSource("valuesOfDifferentTypes")
  void shouldTestIsEqualIsComparingCorrectlyDifferentTypeValues(
      String value1, String value2, ValueType valueType) {
    assertFalse(isEqual(value1, value2, valueType));
  }
}
