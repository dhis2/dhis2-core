/*
 * Copyright (c) 2004-2025, University of Oslo
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

import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;
import org.hisp.dhis.program.ProgramStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnalyticsCustomHeaderTest {

  private ProgramStage programStage;

  @BeforeEach
  void setUp() {
    programStage = new ProgramStage();
    programStage.setUid("StageUid123");
    programStage.setName("Test Stage");
  }

  @Test
  void forEventDate_withCustomLabel() {
    programStage.setExecutionDateLabel("Visit Date");
    programStage.setProgramStageLabel("First Visit");

    AnalyticsCustomHeader header = AnalyticsCustomHeader.forEventDate(programStage);

    assertEquals("StageUid123.EVENT_DATE", header.key());
    assertEquals("Visit Date, First Visit", header.value());
  }

  @Test
  void forEventDate_withDefaultLabel() {
    AnalyticsCustomHeader header = AnalyticsCustomHeader.forEventDate(programStage);

    assertEquals("StageUid123.EVENT_DATE", header.key());
    assertEquals(
        EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME + ", Test Stage", header.value());
  }

  @Test
  void forScheduledDate_withCustomLabel() {
    programStage.setDueDateLabel("Appointment Date");
    programStage.setProgramStageLabel("Follow-up");

    AnalyticsCustomHeader header = AnalyticsCustomHeader.forScheduledDate(programStage);

    assertEquals("StageUid123.SCHEDULED_DATE", header.key());
    assertEquals("Appointment Date, Follow-up", header.value());
  }

  @Test
  void forScheduledDate_withDefaultLabel() {
    AnalyticsCustomHeader header = AnalyticsCustomHeader.forScheduledDate(programStage);

    assertEquals("StageUid123.SCHEDULED_DATE", header.key());
    assertEquals(
        EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME + ", Test Stage", header.value());
  }

  @Test
  void forEventStatus_withCustomStageLabel() {
    programStage.setProgramStageLabel("Custom Stage");

    AnalyticsCustomHeader header = AnalyticsCustomHeader.forEventStatus(programStage);

    assertEquals("StageUid123.EVENT_STATUS", header.key());
    assertEquals("Event status, Custom Stage", header.value());
  }

  @Test
  void forEventStatus_withDefaultStageLabel() {
    AnalyticsCustomHeader header = AnalyticsCustomHeader.forEventStatus(programStage);

    assertEquals("StageUid123.EVENT_STATUS", header.key());
    assertEquals("Event status, Test Stage", header.value());
  }
}
