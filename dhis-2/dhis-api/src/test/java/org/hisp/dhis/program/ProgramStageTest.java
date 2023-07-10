/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.program;

import static org.apache.commons.lang3.reflect.FieldUtils.getAllFields;
import static org.hisp.dhis.program.ProgramStageDataElementTest.getNewProgramStageDataElement;
import static org.hisp.dhis.program.ProgramStageSectionTest.getNewProgramStageSection;
import static org.hisp.dhis.program.ProgramTest.notEqualsOrBothNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.Test;

/**
 * @author David Mackessy
 */
class ProgramStageTest {
  @Test
  void testCopyOfWithPropertyValuesSet() {
    Program program = new Program("Program 1");
    ProgramStage original = getNewProgramStageWithNoNulls(program);
    ProgramStage copy = ProgramStage.shallowCopy(original, program);

    assertNotSame(original, copy);
    assertNotEquals(original, copy);
    assertNotSame(original.getProgramStageSections(), copy.getProgramStageSections());
    assertNotEquals(original.getProgramStageSections(), copy.getProgramStageSections());
    assertNotSame(original.getProgramStageDataElements(), copy.getProgramStageDataElements());
    assertNotEquals(original.getProgramStageDataElements(), copy.getProgramStageDataElements());
    assertNotEquals(original.getUid(), copy.getUid());

    assertTrue(notEqualsOrBothNull(original.getCode(), copy.getCode()));

    assertEquals(original.getDataEntryForm(), copy.getDataEntryForm());
    assertEquals(original.getDescription(), copy.getDescription());
    assertEquals(original.getFeatureType(), copy.getFeatureType());
    assertEquals(original.getValidationStrategy(), copy.getValidationStrategy());
    assertEquals("Stage Name", copy.getName());
    assertEquals(original.getNotificationTemplates(), copy.getNotificationTemplates());
    assertEquals(original.getPublicAccess(), copy.getPublicAccess());
  }

  @Test
  void testCopyOfWithNulls() {
    Program program = new Program("Program 1");
    ProgramStage original = getNewProgramStageWithNulls();
    ProgramStage copy = ProgramStage.shallowCopy(original, program);

    assertNotSame(original, copy);
    assertNotEquals(original, copy);
    assertEquals(original.getName(), copy.getName());
    assertNotEquals(original.getUid(), copy.getUid());

    assertTrue(notEqualsOrBothNull(original.getCode(), copy.getCode()));

    assertEquals(original.getDataEntryForm(), copy.getDataEntryForm());
    assertEquals(original.getDescription(), copy.getDescription());
    assertEquals(original.getFeatureType(), copy.getFeatureType());
    assertEquals(original.getValidationStrategy(), copy.getValidationStrategy());
    assertTrue(copy.getNotificationTemplates().isEmpty());
    assertTrue(copy.getProgramStageSections().isEmpty());
    assertTrue(copy.getProgramStageDataElements().isEmpty());
    assertEquals(original.getPublicAccess(), copy.getPublicAccess());
  }

  @Test
  void testCopyOfCodeShouldBeNullWhenOriginalHasCode() {
    Program program = new Program("Program 1");
    ProgramStage original = getNewProgramStageWithNoNulls(program);
    original.setCode("stage code");
    ProgramStage copy = ProgramStage.shallowCopy(original, program);

    assertNull(copy.getCode());
  }

  @Test
  void testCopyOfCodeShouldBeNullWhenOriginalHasNullCode() {
    Program program = new Program("Program 1");
    ProgramStage original = getNewProgramStageWithNoNulls(program);
    original.setCode(null);
    ProgramStage copy = ProgramStage.shallowCopy(original, program);

    assertNull(copy.getCode());
  }

  /**
   * This test checks the expected field count for {@link ProgramStage}. This is important due to
   * {@link ProgramStage#deepCopy} functionality. If a new field is added then {@link
   * ProgramStage#deepCopy} should be updated with the appropriate copying approach.
   */
  @Test
  void testExpectedFieldCount() {
    Field[] allClassFieldsIncludingInherited = getAllFields(ProgramStage.class);
    assertEquals(50, allClassFieldsIncludingInherited.length);
  }

  private ProgramStage getNewProgramStageWithNoNulls(Program program) {
    ProgramStage ps = new ProgramStage();
    ps.setDataEntryForm(new DataEntryForm("entry form"));
    ps.setDescription("Program description");
    ps.setDueDateLabel("due label");
    ps.setExecutionDateLabel("label");
    ps.setFeatureType(FeatureType.NONE);
    ps.setFormName("Form name");
    ps.setName("Stage Name");
    ps.setNextScheduleDate(new DataElement("element"));
    ps.setNotificationTemplates(Collections.emptySet());
    ps.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.DAILY));
    ps.setProgram(program);
    ps.setReportDateToUse("report date");
    ps.setSharing(Sharing.builder().publicAccess("yes").owner("admin").build());
    ps.setShortName("short name");
    ps.setProgramStageSections(getProgramStageSections(ps));
    ps.setProgramStageDataElements(getProgramStageDataElements(ps));
    ps.setSortOrder(2);
    ps.setStyle(new ObjectStyle());
    ps.setStandardInterval(11);
    return ps;
  }

  private ProgramStage getNewProgramStageWithNulls() {
    ProgramStage ps = new ProgramStage();
    ps.setCode(null);
    ps.setDataEntryForm(null);
    ps.setDescription(null);
    ps.setDueDateLabel(null);
    ps.setExecutionDateLabel(null);
    ps.setFeatureType(null);
    ps.setFormName(null);
    ps.setName(null);
    ps.setNextScheduleDate(null);
    ps.setNotificationTemplates(null);
    ps.setPeriodType(null);
    ps.setProgram(null);
    ps.setReportDateToUse(null);
    ps.setSharing(null);
    ps.setShortName(null);
    ps.setSortOrder(null);
    ps.setStyle(null);
    ps.setStandardInterval(null);
    ps.setProgramStageSections(null);
    ps.setProgramStageDataElements(null);
    return ps;
  }

  private Set<ProgramStageSection> getProgramStageSections(ProgramStage programStage) {
    ProgramStageSection pss1 = getNewProgramStageSection(programStage, programStage.getProgram());
    ProgramStageSection pss2 = getNewProgramStageSection(programStage, programStage.getProgram());
    return Set.of(pss1, pss2);
  }

  private Set<ProgramStageDataElement> getProgramStageDataElements(ProgramStage programStage) {
    ProgramStageDataElement psde1 = getNewProgramStageDataElement(programStage, "data el1");
    ProgramStageDataElement psde2 = getNewProgramStageDataElement(programStage, "data el2");
    return Set.of(psde1, psde2);
  }
}
