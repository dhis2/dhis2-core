/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class QueryOperatorTest {
  @Test
  void testIsType() {
    assertTrue(QueryOperator.LIKE.isLike());
    assertTrue(QueryOperator.NLIKE.isLike());
    assertTrue(QueryOperator.EQ.isEqualTo());
    assertTrue(QueryOperator.IEQ.isEqualTo());

    assertFalse(QueryOperator.LIKE.isEqualTo());
    assertFalse(QueryOperator.GT.isEqualTo());
    assertFalse(QueryOperator.LT.isLike());
    assertFalse(QueryOperator.EQ.isLike());
  }

  @Test
  void testNotNull() {
    assertEquals(QueryOperator.NNULL, QueryOperator.fromString("!null"));
  }

  @Test
  void testNull() {
    assertEquals(QueryOperator.NULL, QueryOperator.fromString("null"));
  }

  @ParameterizedTest
  @MethodSource("provideOperatorsForMapping")
  void shouldMapIVariantOperatorsToTrackerOperators(QueryOperator input, QueryOperator expected) {
    assertEquals(expected, input.stripCaseVariant());
  }

  private static Stream<Arguments> provideOperatorsForMapping() {
    return Stream.of(
        Arguments.of(QueryOperator.IEQ, QueryOperator.EQ),
        Arguments.of(QueryOperator.NIEQ, QueryOperator.NEQ),
        Arguments.of(QueryOperator.ILIKE, QueryOperator.LIKE),
        Arguments.of(QueryOperator.NILIKE, QueryOperator.NLIKE),
        Arguments.of(QueryOperator.EQ, QueryOperator.EQ));
  }
}
