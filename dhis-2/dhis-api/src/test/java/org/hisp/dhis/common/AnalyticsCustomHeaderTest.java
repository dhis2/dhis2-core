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

import static org.junit.jupiter.api.Assertions.*;

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
  void constructor_createsHeaderWithKeyValueAndLabel() {
    AnalyticsCustomHeader header = new AnalyticsCustomHeader("testKey", "testValue", "testLabel");

    assertEquals("testKey", header.key());
    assertEquals("testValue", header.value());
    assertEquals("testLabel", header.label());
  }

  @Test
  void forEventDate_withCustomLabel() {
    programStage.setExecutionDateLabel("Visit Date");
    programStage.setProgramStageLabel("First Visit");

    AnalyticsCustomHeader header = AnalyticsCustomHeader.forEventDate(programStage);

    assertEquals("StageUid123.EVENT_DATE", header.key());
    assertEquals("Visit Date, First Visit", header.value());
    assertEquals("Visit Date", header.label());
  }

  @Test
  void forEventDate_withDefaultLabel() {
    AnalyticsCustomHeader header = AnalyticsCustomHeader.forEventDate(programStage);

    assertEquals("StageUid123.EVENT_DATE", header.key());
    assertEquals("Event date, Test Stage", header.value());
    assertEquals("Event date", header.label());
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
    assertEquals("Scheduled date, Test Stage", header.value());
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

  @Test
  void forOrgUnit_withCustomStageLabel() {
    programStage.setProgramStageLabel("Custom Stage");

    AnalyticsCustomHeader header = AnalyticsCustomHeader.forOrgUnit(programStage);

    assertEquals("StageUid123.ou", header.key());
    assertEquals("Organisation unit, Custom Stage", header.value());
  }

  @Test
  void forOrgUnit_withDefaultStageLabel() {
    AnalyticsCustomHeader header = AnalyticsCustomHeader.forOrgUnit(programStage);

    assertEquals("StageUid123.ou", header.key());
    assertEquals("Organisation unit, Test Stage", header.value());
  }

  @Test
  void headerKey_withPrefixAndMappedKey() {
    AnalyticsCustomHeader header = AnalyticsCustomHeader.forEventDate(programStage);

    String result = header.headerKey("StageUid123.EVENT_DATE");

    assertEquals("StageUid123.eventdate", result);
  }

  @Test
  void headerKey_withPrefixAndUnmappedKey() {
    AnalyticsCustomHeader header = AnalyticsCustomHeader.forEventDate(programStage);

    String result = header.headerKey("StageUid123.UNKNOWN_KEY");

    assertEquals("StageUid123.UNKNOWN_KEY", result);
  }

  @Test
  void headerKey_withoutPrefixAndMappedKey() {
    AnalyticsCustomHeader header = AnalyticsCustomHeader.forEventDate(programStage);

    String result = header.headerKey("EVENT_DATE");

    assertEquals("eventdate", result);
  }

  @Test
  void headerKey_withoutPrefixAndUnmappedKey() {
    AnalyticsCustomHeader header = AnalyticsCustomHeader.forEventDate(programStage);

    String result = header.headerKey("UNKNOWN_KEY");

    assertEquals("UNKNOWN_KEY", result);
  }

  @Test
  void headerKey_withScheduledDate() {
    AnalyticsCustomHeader header = AnalyticsCustomHeader.forScheduledDate(programStage);

    String result = header.headerKey("StageUid123.SCHEDULED_DATE");

    assertEquals("StageUid123.scheduleddate", result);
  }

  @Test
  void headerKey_withEventStatus() {
    AnalyticsCustomHeader header = AnalyticsCustomHeader.forEventStatus(programStage);

    String result = header.headerKey("StageUid123.EVENT_STATUS");

    assertEquals("StageUid123.eventstatus", result);
  }

  @Test
  void headerKey_withOrgUnit() {
    AnalyticsCustomHeader header = AnalyticsCustomHeader.forOrgUnit(programStage);

    String result = header.headerKey("StageUid123.ou");

    assertEquals("StageUid123.ou", result);
  }

  // Edge cases for headerKey
  @Test
  void headerKey_withEmptyString() {
    AnalyticsCustomHeader header = AnalyticsCustomHeader.forEventDate(programStage);

    String result = header.headerKey("");

    assertEquals("", result);
  }

  @Test
  void headerKey_withMultipleDots() {
    AnalyticsCustomHeader header = AnalyticsCustomHeader.forEventDate(programStage);

    String result = header.headerKey("prefix.middle.EVENT_DATE");

    assertEquals("prefix.middle.EVENT_DATE", result);
  }

  @Test
  void headerKey_withDotAtBeginning() {
    AnalyticsCustomHeader header = AnalyticsCustomHeader.forEventDate(programStage);

    String result = header.headerKey(".EVENT_DATE");

    assertEquals(".eventdate", result);
  }

  @Test
  void headerKey_withDotAtEnd() {
    AnalyticsCustomHeader header = AnalyticsCustomHeader.forEventDate(programStage);

    String result = header.headerKey("prefix.");

    assertEquals("prefix.", result);
  }

  @Test
  void headerKey_withNull() {
    AnalyticsCustomHeader header = AnalyticsCustomHeader.forEventDate(programStage);

    assertThrows(NullPointerException.class, () -> header.headerKey(null));
  }

  // Record equality and hashCode
  @Test
  void equals_sameValues_returnsTrue() {
    AnalyticsCustomHeader header1 = new AnalyticsCustomHeader("key", "value", "label");
    AnalyticsCustomHeader header2 = new AnalyticsCustomHeader("key", "value", "label");

    assertEquals(header1, header2);
    assertEquals(header1.hashCode(), header2.hashCode());
  }

  @Test
  void equals_differentKeys_returnsFalse() {
    AnalyticsCustomHeader header1 = new AnalyticsCustomHeader("key1", "value", "label");
    AnalyticsCustomHeader header2 = new AnalyticsCustomHeader("key2", "value", "label");

    assertNotEquals(header1, header2);
  }

  @Test
  void equals_differentValues_returnsFalse() {
    AnalyticsCustomHeader header1 = new AnalyticsCustomHeader("key", "value1", "label");
    AnalyticsCustomHeader header2 = new AnalyticsCustomHeader("key", "value2", "label");

    assertNotEquals(header1, header2);
  }

  @Test
  void equals_differentLabels_returnsFalse() {
    AnalyticsCustomHeader header1 = new AnalyticsCustomHeader("key", "value", "label1");
    AnalyticsCustomHeader header2 = new AnalyticsCustomHeader("key", "value", "label2");

    assertNotEquals(header1, header2);
  }

  // Null handling in factory methods
  @Test
  void forEventDate_withNullName_usesProgramStageName() {
    programStage.setProgramStageLabel(null);

    AnalyticsCustomHeader header = AnalyticsCustomHeader.forEventDate(programStage);

    assertEquals("Event date, Test Stage", header.value());
  }

  @Test
  void forScheduledDate_withNullLabels_usesDefaults() {
    programStage.setDueDateLabel(null);
    programStage.setProgramStageLabel(null);

    AnalyticsCustomHeader header = AnalyticsCustomHeader.forScheduledDate(programStage);

    assertEquals("Scheduled date, Test Stage", header.value());
  }
}
