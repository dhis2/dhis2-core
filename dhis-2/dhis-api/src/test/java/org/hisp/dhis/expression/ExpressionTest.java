/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Set;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.translation.Translation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExpressionTest {

  @Test
  void testEqualsAndHashCodeUseOnlyExpressionAndDescription() {
    Expression first = new Expression("1 + 1", "same", MissingValueStrategy.NEVER_SKIP);
    first.setId(1);
    first.setSlidingWindow(true);
    first.setTranslations(Set.of(new Translation(Locale.of("fr"), "DESCRIPTION", "pareil")));
    Expression second =
        new Expression("1 + 1", "same", MissingValueStrategy.SKIP_IF_ANY_VALUE_MISSING);
    second.setId(2);

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
  }

  @Test
  void testEqualWhenBothFieldsNull() {
    assertEquals(new Expression(null, null), new Expression(null, null));
    assertEquals(new Expression(null, null).hashCode(), new Expression(null, null).hashCode());
  }

  @ParameterizedTest
  @CsvSource(
      nullValues = "null",
      value = {
        "1 + 1, one, 1 + 1, other",
        "1 + 1, same, 2 + 2, same",
        "null, same, 1 + 1, same",
        "1 + 1, same, null, same",
        "1 + 1, null, 1 + 1, same"
      })
  void testNotEqualWhenExpressionOrDescriptionDiffers(
      String expressionA, String descriptionA, String expressionB, String descriptionB) {
    assertNotEquals(
        new Expression(expressionA, descriptionA), new Expression(expressionB, descriptionB));
  }

  @Test
  void testNotEqualToNullOrOtherType() {
    Expression expression = new Expression("1 + 1", "same");

    assertNotEquals(null, expression);
    assertNotEquals("1 + 1", expression);
  }
}
