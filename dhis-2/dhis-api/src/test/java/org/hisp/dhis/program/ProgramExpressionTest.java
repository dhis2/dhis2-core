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
package org.hisp.dhis.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class ProgramExpressionTest {

  @Test
  void testEqualsAndHashCodeIgnoreId() {
    ProgramExpression first = new ProgramExpression("[DE:stage.de]", "same");
    first.setId(1);
    ProgramExpression second = new ProgramExpression("[DE:stage.de]", "same");
    second.setId(2);

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
  }

  @Test
  void testNotEqualWhenDescriptionDiffers() {
    assertNotEquals(
        new ProgramExpression("[DE:stage.de]", "one"),
        new ProgramExpression("[DE:stage.de]", "other"));
  }

  @Test
  void testNotEqualWhenExpressionDiffers() {
    assertNotEquals(
        new ProgramExpression("[DE:stage.deA]", "same"),
        new ProgramExpression("[DE:stage.deB]", "same"));
  }

  @Test
  void testEqualWhenBothFieldsNull() {
    ProgramExpression first = new ProgramExpression(null, null);
    ProgramExpression second = new ProgramExpression(null, null);

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
  }

  @Test
  void testNotEqualWhenOnlyOneSideHasNullField() {
    assertNotEquals(
        new ProgramExpression(null, "same"), new ProgramExpression("[DE:stage.de]", "same"));
    assertNotEquals(
        new ProgramExpression("[DE:stage.de]", "same"), new ProgramExpression(null, "same"));
    assertNotEquals(
        new ProgramExpression("[DE:stage.de]", null), new ProgramExpression("[DE:stage.de]", "x"));
  }

  @Test
  void testNotEqualToNullOrOtherType() {
    ProgramExpression expression = new ProgramExpression("[DE:stage.de]", "same");

    assertNotEquals(null, expression);
    assertNotEquals("[DE:stage.de]", expression);
  }
}
