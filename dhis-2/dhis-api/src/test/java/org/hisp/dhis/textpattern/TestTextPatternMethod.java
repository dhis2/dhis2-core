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
package org.hisp.dhis.textpattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TestTextPatternMethod {

  @Test
  void testValidateText() {
    String[] valid = {"\"Hello world!\"", "\"Hello \\\"world\\\"\""};
    String[] invalid = {"Hello world", "Hello \" world", "\"Hello world", "Hello world\""};
    testSyntax(TextPatternMethod.TEXT, valid, true);
    testSyntax(TextPatternMethod.TEXT, invalid, false);
  }

  @Test
  void testValidateTextWithSpecialCharacters() {
    String[] valid = {
      "\"My digit is \\d\"",
      "\"My lowercase letter is \\x\"",
      "\"My uppercase letter is \\X\"",
      "\"My any character is \\w\""
    };
    testSyntax(TextPatternMethod.TEXT, valid, true);
  }

  @Test
  void testValidateRandom() {
    String[] valid = {
      "RANDOM(#)",
      "RANDOM(X)",
      "RANDOM(x)",
      "RANDOM(Xx#)",
      "RANDOM(xX#)",
      "RANDOM(##XXxx)", // 12
      // characters
      "RANDOM(#X#xXx#xX#XX)"
    };
    String[] invalid = {
      "RAND(#)",
      "RANDOM()",
      "RANDOM(1)", // 13
      // characters
      "RANDOM(#############)"
    };
    testSyntax(TextPatternMethod.RANDOM, valid, true);
    testSyntax(TextPatternMethod.RANDOM, invalid, false);
  }

  @Test
  void testValidateSequential() {
    String[] valid = {
      "SEQUENTIAL(#)", // 12 characters
      "SEQUENTIAL(############)"
    };
    String[] invalid = {
      "SEQ(#)",
      "SEQUENTIAL()",
      "SEQUENTIAL(1)",
      "SEQUENTIAL(x)",
      "SEQUENTIAL(X)", // 13
      // characters
      "SEQUENTIAL(#############)"
    };
    testSyntax(TextPatternMethod.SEQUENTIAL, valid, true);
    testSyntax(TextPatternMethod.SEQUENTIAL, invalid, false);
  }

  @Test
  void testValidateOrgUnitCode() {
    String[] valid = {
      "ORG_UNIT_CODE()",
      "ORG_UNIT_CODE(.)",
      "ORG_UNIT_CODE(...)",
      "ORG_UNIT_CODE(^.)",
      "ORG_UNIT_CODE(.$)"
    };
    String[] invalid = {
      "ORG_UNIT_CODE(1)",
      "ORG_UNIT_CODE(.^)",
      "ORG_UNIT_CODE($.)",
      "ORG_UNIT_CODE(.^.)",
      "ORG_UNIT_CODE(.$.)",
      "ORG_UNIT_CODE(^$)",
      "ORG_UNIT_CODE(^^^)",
      "ORG_UNIT_CODE(.$$)"
    };
    testSyntax(TextPatternMethod.ORG_UNIT_CODE, valid, true);
    testSyntax(TextPatternMethod.ORG_UNIT_CODE, invalid, false);
  }

  @Test
  void testValidateCurrentDate() {
    String[] valid = {
      "CURRENT_DATE(.)",
      "CURRENT_DATE(yy/mm/dd)",
      "CURRENT_DATE(DDMMYYYY)",
      "CURRENT_DATE(DD-MM-HH)",
      "CURRENT_DATE(Hello world)"
    };
    String[] invalid = {"CURRENT()", "CURRENT_DATE()"};
    testSyntax(TextPatternMethod.CURRENT_DATE, valid, true);
    testSyntax(TextPatternMethod.CURRENT_DATE, invalid, false);
  }

  @Test
  void testGetParamText() {
    assertEquals("Hello world", TextPatternMethod.TEXT.getType().getParam("\"Hello world\""));
  }

  @Test
  void testGetParamRandom() {
    assertEquals("##XXxx", TextPatternMethod.RANDOM.getType().getParam("RANDOM(##XXxx)"));
  }

  @Test
  void testGetParamSequential() {
    assertEquals("###", TextPatternMethod.SEQUENTIAL.getType().getParam("SEQUENTIAL(###)"));
  }

  @Test
  void testGetParamOrgUnitCode() {
    assertEquals("...", TextPatternMethod.ORG_UNIT_CODE.getType().getParam("ORG_UNIT_CODE(...)"));
  }

  @Test
  void testGetParamCurrentDate() {
    assertEquals(
        "DDMMYYYY", TextPatternMethod.CURRENT_DATE.getType().getParam("CURRENT_DATE(DDMMYYYY)"));
  }

  @Test
  void testGetParamTextFails() {
    assertNull(TextPatternMethod.TEXT.getType().getParam("Hello world"));
  }

  @Test
  void testGetParamRandomFails() {
    assertNull(TextPatternMethod.RANDOM.getType().getParam("INVALID(##XXxx)"));
  }

  @Test
  void testGetParamSequentialFails() {
    assertNull(TextPatternMethod.SEQUENTIAL.getType().getParam("INVALID(###)"));
  }

  @Test
  void testGetParamOrgUnitCodeFails() {
    assertNull(TextPatternMethod.ORG_UNIT_CODE.getType().getParam("INVALID(...)"));
  }

  @Test
  void testGetParamCurrentDateFails() {
    assertNull(TextPatternMethod.CURRENT_DATE.getType().getParam("INVALID(DDMMYYYY)"));
  }

  private void testSyntax(TextPatternMethod method, String[] input, boolean expected) {
    for (String s : input) {
      assertEquals(expected, method.getType().validatePattern(s));
    }
  }
}
