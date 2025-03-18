/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.analytics.event.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.hisp.dhis.analytics.event.LabelMapper;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for LabelMapper.
 *
 * @author maikel arabori
 */
class LabelMapperTest {
  public static final String EVENT_DATE = "Event date";

  public static final String INCIDENT_DATE = "Incident date";

  public static final String ENROLLMENT_DATE = "Enrollment date";

  private static final String SCHEDULED_DATE = "Scheduled date";

  private static final String ORG_UNIT = "Organisation unit";

  private static final String PROGRAM_STAGE = "Program stage";

  private static final String ENROLLMENT = "Enrollment";

  private static final String EVENT = "Event";

  @Test
  void testGetHeaderNameFor_EVENT() {
    ProgramStage aMockedProgramStageWithLabels = mockProgramStageWithLabels();

    String actualName = LabelMapper.getEventLabel(aMockedProgramStageWithLabels, EVENT);

    assertThat(actualName, is(aMockedProgramStageWithLabels.getEventLabel()));
  }

  @Test
  void testGetHeaderNameFor_ENROLLMENT() {
    Program aMockedProgramWithLabels = mockProgramWithLabels();

    String actualName = LabelMapper.getEnrollmentLabel(aMockedProgramWithLabels, ENROLLMENT);

    assertThat(actualName, is(aMockedProgramWithLabels.getDisplayEnrollmentLabel()));
  }

  @Test
  void testGetHeaderNameFor_PROGRAM_STAGE() {
    ProgramStage aMockedProgramStageWithLabels = mockProgramStageWithLabels();

    String actualName =
        LabelMapper.getProgramStageLabel(aMockedProgramStageWithLabels, PROGRAM_STAGE);

    assertThat(actualName, is(aMockedProgramStageWithLabels.getProgramStageLabel()));
  }

  @Test
  void testGetHeaderNameFor_ORG_UNIT() {
    Program aMockedProgramWithLabels = mockProgramWithLabels();

    String actualName = LabelMapper.getOrgUnitLabel(aMockedProgramWithLabels, ORG_UNIT);

    assertThat(actualName, is(aMockedProgramWithLabels.getDisplayOrgUnitLabel()));
  }

  @Test
  void testGetHeaderNameFor_ORG_UNIT_WhenLabelIsNull() {
    Program aMockedProgramWithLabels = mockProgramWithLabels();
    aMockedProgramWithLabels.setOrgUnitLabel(null);

    String actualName = LabelMapper.getOrgUnitLabel(aMockedProgramWithLabels, ORG_UNIT);

    assertThat(actualName, is(ORG_UNIT));
  }

  @Test
  void testGetHeaderNameFor_EVENT_DATE() {
    ProgramStage aMockedProgramStageWithLabels = mockProgramStageWithLabels();

    String actualName = LabelMapper.getEventDateLabel(aMockedProgramStageWithLabels, EVENT_DATE);

    assertThat(actualName, is(aMockedProgramStageWithLabels.getDisplayExecutionDateLabel()));
  }

  @Test
  void testGetHeaderNameFor_SCHEDULED_DATE() {
    ProgramStage aMockedProgramStageWithLabels = mockProgramStageWithLabels();

    String actualName =
        LabelMapper.getScheduledDateLabel(aMockedProgramStageWithLabels, SCHEDULED_DATE);

    assertThat(actualName, is(aMockedProgramStageWithLabels.getDisplayDueDateLabel()));
  }

  @Test
  void testGetHeaderNameFor_ENROLLMENT_DATE() {
    Program aMockedProgramWithLabels = mockProgramWithLabels();

    String actualName =
        LabelMapper.getEnrollmentDateLabel(aMockedProgramWithLabels, ENROLLMENT_DATE);

    assertThat(actualName, is(aMockedProgramWithLabels.getDisplayEnrollmentDateLabel()));
  }

  @Test
  void testGetHeaderNameFor_INCIDENT_DATE() {
    Program aMockedProgramWithLabels = mockProgramWithLabels();

    String actualName = LabelMapper.getIncidentDateLabel(aMockedProgramWithLabels, INCIDENT_DATE);

    assertThat(actualName, is(aMockedProgramWithLabels.getDisplayIncidentDateLabel()));
  }

  @Test
  void testGetHeaderNameWhenNoLabelIsSetFor_EVENT_DATE() {
    ProgramStage aMockedProgramStageWithNoLabels = mockProgramStageWithoutLabels();

    String actualName = LabelMapper.getEventDateLabel(aMockedProgramStageWithNoLabels, EVENT_DATE);

    assertThat(actualName, is(EVENT_DATE));
  }

  @Test
  void testGetHeaderNameWhenNoLabelIsSetFor_SCHEDULED_DATE() {
    ProgramStage aMockedProgramStageWithNoLabels = mockProgramStageWithoutLabels();

    String actualName =
        LabelMapper.getEventDateLabel(aMockedProgramStageWithNoLabels, SCHEDULED_DATE);

    assertThat(actualName, is(SCHEDULED_DATE));
  }

  @Test
  void testGetHeaderNameWhenNoLabelIsSetFor_ENROLLMENT_DATE() {
    Program aMockedProgramWithLabels = mockProgramWithoutLabels();

    String actualName =
        LabelMapper.getEnrollmentDateLabel(aMockedProgramWithLabels, ENROLLMENT_DATE);

    assertThat(actualName, is(ENROLLMENT_DATE));
  }

  @Test
  void testGetHeaderNameWhenNoLabelIsSetFor_INCIDENT_DATE() {
    Program aMockedProgramWithLabels = mockProgramWithoutLabels();

    String actualName = LabelMapper.getIncidentDateLabel(aMockedProgramWithLabels, INCIDENT_DATE);

    assertThat(actualName, is(INCIDENT_DATE));
  }

  @Test
  void testGetHeaderNameWhenProgramIsNull() {
    Program nullProgram = null;

    String actualName = LabelMapper.getEnrollmentDateLabel(nullProgram, ENROLLMENT_DATE);

    assertThat(actualName, is(ENROLLMENT_DATE));
  }

  @Test
  void testGetHeaderNameWhenProgramStageIsNull() {
    ProgramStage nullProgramStage = null;

    String actualName = LabelMapper.getProgramStageLabel(nullProgramStage, PROGRAM_STAGE);

    assertThat(actualName, is(PROGRAM_STAGE));
  }

  private Program mockProgramWithLabels() {
    Program program = new Program();
    program.setEnrollmentDateLabel("enrollment date label");
    program.setIncidentDateLabel("incident date label");
    program.setEnrollmentLabel("enrollment label");
    program.setOrgUnitLabel("org. unit label");

    return program;
  }

  private Program mockProgramWithoutLabels() {
    Program program = new Program();
    return program;
  }

  private ProgramStage mockProgramStageWithLabels() {
    ProgramStage programStage = new ProgramStage();
    programStage.setExecutionDateLabel("execution date label");
    programStage.setDueDateLabel("scheduled date label");
    programStage.setProgramStageLabel("program stage label");
    programStage.setEventLabel("event label");

    Program program = new Program();
    program.setEnrollmentDateLabel("enrollment date label");
    program.setIncidentDateLabel("incident date label");
    program.setEnrollmentLabel("enrollment label");
    program.setOrgUnitLabel("org. unit label");
    programStage.setProgram(program);
    return programStage;
  }

  private ProgramStage mockProgramStageWithoutLabels() {
    final ProgramStage programStage = new ProgramStage();
    final Program program = new Program();
    programStage.setProgram(program);
    return programStage;
  }
}
