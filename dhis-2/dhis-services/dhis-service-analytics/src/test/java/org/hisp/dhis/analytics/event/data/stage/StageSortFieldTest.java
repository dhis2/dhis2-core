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
package org.hisp.dhis.analytics.event.data.stage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.hisp.dhis.analytics.common.CteUtils;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.RepeatableStageParams;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class StageSortFieldTest {
  private static final String STAGE_UID = "s1234567890";

  private final ProgramStage stage = stage();
  private final Program program = new Program();

  @ParameterizedTest
  @CsvSource({
    "ou, OU",
    "OU, OU",
    "ouname, OU_NAME",
    "OuName, OU_NAME",
    "oucode, OU_CODE",
    "OUCODE, OU_CODE",
    "eventdate, EVENT_DATE",
    "EVENT_DATE, EVENT_DATE",
    "event_date, EVENT_DATE",
    "scheduleddate, SCHEDULED_DATE",
    "SCHEDULED_DATE, SCHEDULED_DATE"
  })
  void forRequestedSuffixMatchesOutputNameAndCanonicalTokenIgnoringCase(
      String suffix, StageSortField expected) {
    assertEquals(Optional.of(expected), StageSortField.forRequestedSuffix(suffix));
  }

  @ParameterizedTest
  @ValueSource(strings = {"bogus", "occurreddate", "eventstatus", "ounamehierarchy", "ou.name", ""})
  void forRequestedSuffixRejectsAnythingOutsideTheVocabulary(String suffix) {
    assertTrue(StageSortField.forRequestedSuffix(suffix).isEmpty());
  }

  @Test
  void forRequestedSuffixRejectsNull() {
    assertTrue(StageSortField.forRequestedSuffix(null).isEmpty());
  }

  @ParameterizedTest
  @CsvSource({
    "ou, OU",
    "ouname, OU_NAME",
    "oucode, OU_CODE",
    "occurreddate, EVENT_DATE",
    "scheduleddate, SCHEDULED_DATE"
  })
  void forItemIdMatchesTheSortItemIdExactly(String itemId, StageSortField expected) {
    assertEquals(Optional.of(expected), StageSortField.forItemId(itemId));
  }

  @ParameterizedTest
  @ValueSource(strings = {"eventdate", "EVENT_DATE", "Ouname", "bogus"})
  void forItemIdDoesNotMatchOutputNamesOrDimensionTokens(String itemId) {
    assertTrue(StageSortField.forItemId(itemId).isEmpty());
  }

  @ParameterizedTest
  @CsvSource({
    "OU, ou",
    "OU_NAME, ou",
    "OU_CODE, ou",
    "EVENT_DATE, EVENT_DATE",
    "SCHEDULED_DATE, SCHEDULED_DATE"
  })
  void canonicalDimensionIsTheTokenTheLocatorValidates(
      StageSortField field, String expectedDimension) {
    assertEquals(expectedDimension, field.getCanonicalDimension());
  }

  @ParameterizedTest
  @CsvSource({
    "OU, value",
    "OU_NAME, ev_ouname",
    "OU_CODE, ev_oucode",
    "EVENT_DATE, value",
    "SCHEDULED_DATE, value"
  })
  void enrollmentCteColumnIsTheColumnTheStageCteProjects(
      StageSortField field, String expectedColumn) {
    assertEquals(expectedColumn, field.getEnrollmentCteColumn());
  }

  @Test
  void orgUnitSortFieldsAreDistinctTermsReadFromOneCanonicalCte() {
    QueryItem ou = stageItem(StageSortField.OU.getItemId());
    QueryItem ouName = stageItem(StageSortField.OU_NAME.getItemId());
    QueryItem ouCode = stageItem(StageSortField.OU_CODE.getItemId());

    assertNotEquals(ou, ouName);
    assertNotEquals(ou, ouCode);
    assertNotEquals(ouName, ouCode);

    String canonicalKey = CteUtils.computeKey(StageSortField.OU.toCanonicalItem(ou));
    assertEquals(STAGE_UID + "_ou_0", canonicalKey);
    assertEquals(canonicalKey, CteUtils.computeKey(StageSortField.OU_NAME.toCanonicalItem(ouName)));
    assertEquals(canonicalKey, CteUtils.computeKey(StageSortField.OU_CODE.toCanonicalItem(ouCode)));
  }

  @ParameterizedTest
  @EnumSource(
      value = StageSortField.class,
      names = {"OU", "EVENT_DATE", "SCHEDULED_DATE"})
  void toCanonicalItemReturnsTheSortItemItselfWhenAlreadyCanonical(StageSortField field) {
    QueryItem sortItem = stageItem(field.getItemId());

    assertSame(sortItem, field.toCanonicalItem(sortItem));
  }

  @Test
  void toCanonicalItemKeepsStageProgramAndOffset() {
    QueryItem ouName = stageItem(StageSortField.OU_NAME.getItemId());
    ouName.setRepeatableStageParams(RepeatableStageParams.of(-1, STAGE_UID + "[-1].ouname"));

    QueryItem canonical = StageSortField.OU_NAME.toCanonicalItem(ouName);

    assertEquals(StageSortField.OU.getItemId(), canonical.getItemId());
    assertSame(stage, canonical.getProgramStage());
    assertSame(program, canonical.getProgram());
    assertEquals(-1, canonical.getProgramStageOffset());
    assertEquals(canonical.getValueType(), ouName.getValueType());
  }

  private QueryItem stageItem(String itemId) {
    QueryItem item = new QueryItem(new BaseDimensionalItemObject(itemId));
    item.setProgram(program);
    item.setProgramStage(stage);
    return item;
  }

  private static ProgramStage stage() {
    ProgramStage programStage = new ProgramStage();
    programStage.setUid(STAGE_UID);
    return programStage;
  }
}
