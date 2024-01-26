/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.fieldfiltering;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author david mackessy
 */
class FieldPathHelperTest {

  @ParameterizedTest
  @MethodSource("noMatchingFields")
  @DisplayName("Field does not equal root field")
  void fieldDoesNotEqualRootFieldTest(String fullFieldPath, String field) {
    boolean result = FieldPathHelper.fieldEqualsRootField(fullFieldPath, field);
    assertFalse(result);
  }

  @ParameterizedTest
  @MethodSource("matchingFields")
  @DisplayName("Field equals root field")
  void fieldEqualsRootFieldTest(String fullFieldPath, String field) {
    boolean result = FieldPathHelper.fieldEqualsRootField(fullFieldPath, field);
    assertTrue(result);
  }

  private static Stream<Arguments> noMatchingFields() {
    return Stream.of(
        arguments("username", "user"),
        arguments("userRoles", "user"),
        arguments("", "user"),
        arguments(null, "user"),
        arguments("", null),
        arguments("test", "user"));
  }

  private static Stream<Arguments> matchingFields() {
    return Stream.of(
        arguments("user", "user"),
        arguments("user.name", "user"),
        arguments("user.address", "user"),
        arguments("user.name.last", "user"));
  }
}
