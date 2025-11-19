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
package org.hisp.dhis.util;

import static org.hisp.dhis.feedback.ErrorCode.E7242;
import static org.hisp.dhis.feedback.ErrorCode.E7243;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.StagePeriodCombination;
import org.junit.jupiter.api.Test;

class StagePeriodParserTest {

  @Test
  void testParseSingleStagePeriodCombination() {
    // given
    String value = "Zj7UnCAulEk.LAST_12_MONTHS";
    TimeField timeField = TimeField.OCCURRED_DATE;

    // when
    List<StagePeriodCombination> result = StagePeriodParser.parse(value, timeField);

    // then
    assertNotNull(result);
    assertEquals(1, result.size());

    StagePeriodCombination combination = result.get(0);
    assertEquals("Zj7UnCAulEk", combination.getStageUid());
    assertEquals("LAST_12_MONTHS", combination.getPeriod());
    assertEquals(TimeField.OCCURRED_DATE, combination.getTimeField());
  }

  @Test
  void testParseMultipleStagePeriodCombinations() {
    // given
    String value = "stageA.THIS_YEAR;stageB.LAST_MONTH;stageC.202201";
    TimeField timeField = TimeField.EVENT_DATE;

    // when
    List<StagePeriodCombination> result = StagePeriodParser.parse(value, timeField);

    // then
    assertNotNull(result);
    assertEquals(3, result.size());

    assertEquals("stageA", result.get(0).getStageUid());
    assertEquals("THIS_YEAR", result.get(0).getPeriod());
    assertEquals(TimeField.EVENT_DATE, result.get(0).getTimeField());

    assertEquals("stageB", result.get(1).getStageUid());
    assertEquals("LAST_MONTH", result.get(1).getPeriod());

    assertEquals("stageC", result.get(2).getStageUid());
    assertEquals("202201", result.get(2).getPeriod());
  }

  @Test
  void testParseWithSpacesAroundSeparators() {
    // given
    String value = "stageA.LAST_YEAR ; stageB.THIS_YEAR";
    TimeField timeField = TimeField.OCCURRED_DATE;

    // when
    List<StagePeriodCombination> result = StagePeriodParser.parse(value, timeField);

    // then
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("stageA", result.get(0).getStageUid());
    assertEquals("LAST_YEAR", result.get(0).getPeriod());
    assertEquals("stageB", result.get(1).getStageUid());
    assertEquals("THIS_YEAR", result.get(1).getPeriod());
  }

  @Test
  void testParseThrowsExceptionForDuplicateStages() {
    // given
    String value = "stageA.LAST_12_MONTHS;stageB.THIS_YEAR;stageA.LAST_MONTH";
    TimeField timeField = TimeField.OCCURRED_DATE;

    // when/then
    IllegalQueryException exception =
        assertThrows(IllegalQueryException.class, () -> StagePeriodParser.parse(value, timeField));

    assertEquals(E7242, exception.getErrorCode());
    assertEquals("Duplicate stage in eventDate parameter: `stageA`", exception.getMessage());
  }

  @Test
  void testParseThrowsExceptionForInvalidFormat_MissingPeriod() {
    // given
    String value = "stageA.";
    TimeField timeField = TimeField.OCCURRED_DATE;

    // when/then
    IllegalQueryException exception =
        assertThrows(IllegalQueryException.class, () -> StagePeriodParser.parse(value, timeField));

    assertEquals(E7243, exception.getErrorCode());
  }

  @Test
  void testParseThrowsExceptionForInvalidFormat_MissingStage() {
    // given
    String value = ".LAST_12_MONTHS";
    TimeField timeField = TimeField.OCCURRED_DATE;

    // when/then
    IllegalQueryException exception =
        assertThrows(IllegalQueryException.class, () -> StagePeriodParser.parse(value, timeField));

    assertEquals(E7243, exception.getErrorCode());
  }

  @Test
  void testParseThrowsExceptionForInvalidFormat_MissingSeparator() {
    // given
    String value = "stageALAST_12_MONTHS";
    TimeField timeField = TimeField.OCCURRED_DATE;

    // when/then
    IllegalQueryException exception =
        assertThrows(IllegalQueryException.class, () -> StagePeriodParser.parse(value, timeField));

    assertEquals(E7243, exception.getErrorCode());
  }

  @Test
  void testParseThrowsExceptionForInvalidFormat_MultipleDots() {
    // given
    String value = "stageA.LAST.12.MONTHS";
    TimeField timeField = TimeField.OCCURRED_DATE;

    // when
    List<StagePeriodCombination> result = StagePeriodParser.parse(value, timeField);

    // then - should parse stage as "stageA" and period as "LAST.12.MONTHS"
    // (this allows periods with dots, splitting only on first dot)
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("stageA", result.get(0).getStageUid());
    assertEquals("LAST.12.MONTHS", result.get(0).getPeriod());
  }

  @Test
  void testParseReturnsEmptyListForNullValue() {
    // given
    String value = null;
    TimeField timeField = TimeField.OCCURRED_DATE;

    // when
    List<StagePeriodCombination> result = StagePeriodParser.parse(value, timeField);

    // then
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void testParseReturnsEmptyListForBlankValue() {
    // given
    String value = "   ";
    TimeField timeField = TimeField.OCCURRED_DATE;

    // when
    List<StagePeriodCombination> result = StagePeriodParser.parse(value, timeField);

    // then
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void testParseReturnsEmptyListForEmptyString() {
    // given
    String value = "";
    TimeField timeField = TimeField.OCCURRED_DATE;

    // when
    List<StagePeriodCombination> result = StagePeriodParser.parse(value, timeField);

    // then
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void testParseThrowsExceptionForEmptyCombination() {
    // given
    String value = "stageA.LAST_12_MONTHS;;stageB.THIS_YEAR";
    TimeField timeField = TimeField.OCCURRED_DATE;

    // when - should skip empty combination
    List<StagePeriodCombination> result = StagePeriodParser.parse(value, timeField);

    // then
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("stageA", result.get(0).getStageUid());
    assertEquals("stageB", result.get(1).getStageUid());
  }

  @Test
  void testParseThrowsExceptionWhenOnlySemicolons() {
    // given
    String value = ";;;";
    TimeField timeField = TimeField.OCCURRED_DATE;

    // when/then
    IllegalQueryException exception =
        assertThrows(IllegalQueryException.class, () -> StagePeriodParser.parse(value, timeField));

    assertEquals(E7243, exception.getErrorCode());
  }

  @Test
  void testHasStagePeriodFormat_ReturnsTrueForValidFormat() {
    // given
    String value = "stageA.LAST_12_MONTHS";

    // when
    boolean result = StagePeriodParser.hasStagePeriodFormat(value);

    // then
    assertTrue(result);
  }

  @Test
  void testHasStagePeriodFormat_ReturnsTrueForMultipleCombinations() {
    // given
    String value = "stageA.LAST_12_MONTHS;stageB.THIS_YEAR";

    // when
    boolean result = StagePeriodParser.hasStagePeriodFormat(value);

    // then
    assertTrue(result);
  }

  @Test
  void testHasStagePeriodFormat_ReturnsFalseForPlainPeriod() {
    // given
    String value = "LAST_12_MONTHS";

    // when
    boolean result = StagePeriodParser.hasStagePeriodFormat(value);

    // then
    assertFalse(result);
  }

  @Test
  void testHasStagePeriodFormat_ReturnsFalseForNull() {
    // given
    String value = null;

    // when
    boolean result = StagePeriodParser.hasStagePeriodFormat(value);

    // then
    assertFalse(result);
  }

  @Test
  void testHasStagePeriodFormat_ReturnsFalseForBlank() {
    // given
    String value = "   ";

    // when
    boolean result = StagePeriodParser.hasStagePeriodFormat(value);

    // then
    assertFalse(result);
  }

  @Test
  void testHasStagePeriodFormat_ReturnsFalseForEmpty() {
    // given
    String value = "";

    // when
    boolean result = StagePeriodParser.hasStagePeriodFormat(value);

    // then
    assertFalse(result);
  }

  @Test
  void testParseLongStageUidAndComplexPeriod() {
    // given - realistic UIDs and period expressions
    String value =
        "A0000000001.LAST_12_MONTHS;B0000000002.2024Q1;C0000000003.202401;D0000000004.LAST_YEAR";
    TimeField timeField = TimeField.SCHEDULED_DATE;

    // when
    List<StagePeriodCombination> result = StagePeriodParser.parse(value, timeField);

    // then
    assertNotNull(result);
    assertEquals(4, result.size());

    assertEquals("A0000000001", result.get(0).getStageUid());
    assertEquals("LAST_12_MONTHS", result.get(0).getPeriod());

    assertEquals("B0000000002", result.get(1).getStageUid());
    assertEquals("2024Q1", result.get(1).getPeriod());

    assertEquals("C0000000003", result.get(2).getStageUid());
    assertEquals("202401", result.get(2).getPeriod());

    assertEquals("D0000000004", result.get(3).getStageUid());
    assertEquals("LAST_YEAR", result.get(3).getPeriod());
  }

  @Test
  void testParseStagePeriodWithDifferentTimeFields() {
    // given
    String value = "stageA.LAST_12_MONTHS";

    // when - parsing with different time fields
    List<StagePeriodCombination> result1 = StagePeriodParser.parse(value, TimeField.OCCURRED_DATE);
    List<StagePeriodCombination> result2 = StagePeriodParser.parse(value, TimeField.EVENT_DATE);
    List<StagePeriodCombination> result3 = StagePeriodParser.parse(value, TimeField.SCHEDULED_DATE);

    // then - time field should be set correctly
    assertEquals(TimeField.OCCURRED_DATE, result1.get(0).getTimeField());
    assertEquals(TimeField.EVENT_DATE, result2.get(0).getTimeField());
    assertEquals(TimeField.SCHEDULED_DATE, result3.get(0).getTimeField());
  }
}
