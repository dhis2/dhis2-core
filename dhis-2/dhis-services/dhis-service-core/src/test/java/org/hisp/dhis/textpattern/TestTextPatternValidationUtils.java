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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import org.hisp.dhis.common.ValueType;
import org.junit.jupiter.api.Test;

class TestTextPatternValidationUtils {

  private TextPatternSegment textSegment =
      new TextPatternSegment(TextPatternMethod.TEXT, "\"FOOBAR\"");

  private TextPatternSegment textSegmentWithSpecialCharacters =
      new TextPatternSegment(TextPatternMethod.TEXT, "\"\\d\\x\\X\\w\"");

  private TextPatternSegment randomSegment =
      new TextPatternSegment(TextPatternMethod.RANDOM, "RANDOM(XXxx##)");

  private TextPatternSegment sequentialSegment =
      new TextPatternSegment(TextPatternMethod.SEQUENTIAL, "SEQUENTIAL(###)");

  private TextPatternSegment orgUnitCodeSegment =
      new TextPatternSegment(TextPatternMethod.ORG_UNIT_CODE, "ORG_UNIT_CODE(...)");

  private TextPatternSegment currentDateSegment =
      new TextPatternSegment(TextPatternMethod.CURRENT_DATE, "CURRENT_DATE(dd/mm/yyyy)");

  @Test
  void testValidationUtilsValidateSegmentValue() {
    assertTrue(TextPatternValidationUtils.validateSegmentValue(textSegment, "FOOBAR"));
    assertFalse(TextPatternValidationUtils.validateSegmentValue(textSegment, "FOBAR"));
    assertFalse(TextPatternValidationUtils.validateSegmentValue(textSegment, ""));
    assertTrue(
        TextPatternValidationUtils.validateSegmentValue(textSegmentWithSpecialCharacters, "0aA0"));
    assertTrue(
        TextPatternValidationUtils.validateSegmentValue(textSegmentWithSpecialCharacters, "9zZa"));
    assertTrue(
        TextPatternValidationUtils.validateSegmentValue(textSegmentWithSpecialCharacters, "0aAA"));
    assertFalse(
        TextPatternValidationUtils.validateSegmentValue(textSegmentWithSpecialCharacters, "aaA0"));
    assertFalse(
        TextPatternValidationUtils.validateSegmentValue(textSegmentWithSpecialCharacters, "01A0"));
    assertFalse(
        TextPatternValidationUtils.validateSegmentValue(textSegmentWithSpecialCharacters, "0a10"));
    assertFalse(
        TextPatternValidationUtils.validateSegmentValue(textSegmentWithSpecialCharacters, "12aA0"));
    assertFalse(
        TextPatternValidationUtils.validateSegmentValue(textSegmentWithSpecialCharacters, "0aA01"));
    assertTrue(TextPatternValidationUtils.validateSegmentValue(randomSegment, "AAaa11"));
    assertFalse(TextPatternValidationUtils.validateSegmentValue(randomSegment, "11AAaa"));
    assertFalse(TextPatternValidationUtils.validateSegmentValue(randomSegment, "AAaa111"));
    assertFalse(TextPatternValidationUtils.validateSegmentValue(randomSegment, "Aa1"));
    assertFalse(TextPatternValidationUtils.validateSegmentValue(randomSegment, ""));
    assertTrue(TextPatternValidationUtils.validateSegmentValue(sequentialSegment, "001"));
    assertFalse(TextPatternValidationUtils.validateSegmentValue(sequentialSegment, "1234"));
    assertFalse(TextPatternValidationUtils.validateSegmentValue(sequentialSegment, "01"));
    assertFalse(TextPatternValidationUtils.validateSegmentValue(sequentialSegment, "asd"));
    assertFalse(TextPatternValidationUtils.validateSegmentValue(sequentialSegment, ""));
    assertTrue(TextPatternValidationUtils.validateSegmentValue(orgUnitCodeSegment, "ABC"));
    assertFalse(TextPatternValidationUtils.validateSegmentValue(orgUnitCodeSegment, "ABCD"));
    assertFalse(TextPatternValidationUtils.validateSegmentValue(orgUnitCodeSegment, "AB"));
    assertFalse(TextPatternValidationUtils.validateSegmentValue(orgUnitCodeSegment, ""));
    // TODO: We only validate that there is <something> , not that it
    // follows the format.
    assertTrue(TextPatternValidationUtils.validateSegmentValue(currentDateSegment, "22/10/1990"));
  }

  @Test
  void testValidationUtilsValidateTextPatternValue()
      throws TextPatternParser.TextPatternParsingException {
    TextPattern tp =
        TextPatternParser.parse(
            "\"FOOBAR\"+RANDOM(xxx)+\"-\"+SEQUENTIAL(##)+ORG_UNIT_CODE(...)+CURRENT_DATE(yyyy)");
    assertTrue(TextPatternValidationUtils.validateTextPatternValue(tp, "FOOBARabc-01OSL1990"));
    assertFalse(
        TextPatternValidationUtils.validateTextPatternValue(tp, "FOOBAR abc - 01 OSL 1990"));
    assertFalse(TextPatternValidationUtils.validateTextPatternValue(tp, "FOOBARabc-01 OSL 1990"));
    assertFalse(TextPatternValidationUtils.validateTextPatternValue(tp, "FOOBARabc-01OSL 1990"));
    assertFalse(TextPatternValidationUtils.validateTextPatternValue(tp, ""));
  }

  @Test
  void testValidateValueType() {
    TextPattern textTP = new TextPattern(Lists.newArrayList(textSegment));
    TextPattern numberTP = new TextPattern(Lists.newArrayList(sequentialSegment));
    for (ValueType valueType : ValueType.values()) {
      if (valueType.equals(ValueType.TEXT)) {
        assertTrue(TextPatternValidationUtils.validateValueType(textTP, valueType));
      } else if (valueType.equals(ValueType.NUMBER)) {
        assertTrue(TextPatternValidationUtils.validateValueType(numberTP, valueType));
      } else {
        assertFalse(TextPatternValidationUtils.validateValueType(textTP, valueType));
        assertFalse(TextPatternValidationUtils.validateValueType(numberTP, valueType));
      }
    }
  }

  @Test
  void testValidateValueTypeWithDifferentTextPattern() {
    TextPattern just_text = new TextPattern(Lists.newArrayList(textSegment));
    TextPattern just_random = new TextPattern(Lists.newArrayList(randomSegment));
    TextPattern just_sequential = new TextPattern(Lists.newArrayList(sequentialSegment));
    TextPattern just_orgunitcode = new TextPattern(Lists.newArrayList(orgUnitCodeSegment));
    TextPattern just_currentdate = new TextPattern(Lists.newArrayList(currentDateSegment));
    TextPattern text_and_numbers =
        new TextPattern(Lists.newArrayList(textSegment, sequentialSegment));
    TextPattern just_numbers = new TextPattern(Lists.newArrayList(sequentialSegment));
    assertTrue(TextPatternValidationUtils.validateValueType(just_text, ValueType.TEXT));
    assertFalse(TextPatternValidationUtils.validateValueType(just_text, ValueType.NUMBER));
    assertTrue(TextPatternValidationUtils.validateValueType(just_random, ValueType.TEXT));
    assertFalse(TextPatternValidationUtils.validateValueType(just_random, ValueType.NUMBER));
    assertTrue(TextPatternValidationUtils.validateValueType(just_sequential, ValueType.TEXT));
    assertTrue(TextPatternValidationUtils.validateValueType(just_sequential, ValueType.NUMBER));
    assertTrue(TextPatternValidationUtils.validateValueType(just_orgunitcode, ValueType.TEXT));
    assertFalse(TextPatternValidationUtils.validateValueType(just_orgunitcode, ValueType.NUMBER));
    assertTrue(TextPatternValidationUtils.validateValueType(just_currentdate, ValueType.TEXT));
    assertFalse(TextPatternValidationUtils.validateValueType(just_currentdate, ValueType.NUMBER));
    assertTrue(TextPatternValidationUtils.validateValueType(text_and_numbers, ValueType.TEXT));
    assertFalse(TextPatternValidationUtils.validateValueType(text_and_numbers, ValueType.NUMBER));
    assertTrue(TextPatternValidationUtils.validateValueType(just_numbers, ValueType.TEXT));
    assertTrue(TextPatternValidationUtils.validateValueType(just_numbers, ValueType.NUMBER));
  }
}
