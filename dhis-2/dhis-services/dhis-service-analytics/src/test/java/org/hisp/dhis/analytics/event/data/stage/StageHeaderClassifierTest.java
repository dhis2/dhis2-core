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
package org.hisp.dhis.analytics.event.data.stage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.analytics.event.data.stage.StageHeaderClassifier.StageHeaderType;
import org.junit.jupiter.api.Test;

class StageHeaderClassifierTest {
  private final StageHeaderClassifier subject = new StageHeaderClassifier();

  @Test
  void shouldClassifyEventDate() {
    assertEquals(StageHeaderType.EVENT_DATE, subject.classify("StageA.eventdate"));
  }

  @Test
  void shouldClassifyScheduledDate() {
    assertEquals(StageHeaderType.SCHEDULED_DATE, subject.classify("stageB.scheduleddate"));
  }

  @Test
  void shouldClassifyOuVariants() {
    assertEquals(StageHeaderType.OU, subject.classify("stageC.ou"));
    assertEquals(StageHeaderType.OU_NAME, subject.classify("stageC.ouname"));
    assertEquals(StageHeaderType.OU_CODE, subject.classify("stageC.oucode"));
  }

  @Test
  void shouldClassifyEventStatus() {
    assertEquals(StageHeaderType.EVENT_STATUS, subject.classify("stageD.eventstatus"));
  }

  @Test
  void shouldClassifyGenericStageItem() {
    assertEquals(StageHeaderType.GENERIC_STAGE_ITEM, subject.classify("stageE.de1"));
  }

  @Test
  void shouldHandleQuotedHeaders() {
    assertEquals(StageHeaderType.EVENT_DATE, subject.classify("\"stageF.eventdate\""));
  }

  @Test
  void shouldDetectNonStageSpecificHeaders() {
    assertEquals(StageHeaderType.NOT_STAGE_SPECIFIC, subject.classify("eventdate"));
    assertFalse(subject.isStageSpecific("eventdate"));
    assertTrue(subject.isStageSpecific("stageG.eventdate"));
  }

  @Test
  void shouldHandleNullAndBlankHeaders() {
    assertEquals(StageHeaderType.NOT_STAGE_SPECIFIC, subject.classify(null));
    assertEquals(StageHeaderType.NOT_STAGE_SPECIFIC, subject.classify(""));
    assertEquals(StageHeaderType.NOT_STAGE_SPECIFIC, subject.classify("   "));
    assertFalse(subject.isStageSpecific(null));
  }

  @Test
  void shouldNormalizeCaseForStageHeaders() {
    assertEquals(StageHeaderType.EVENT_DATE, subject.classify("StageH.EventDate"));
    assertEquals(StageHeaderType.OU, subject.classify("\"StageI.OU\""));
  }
}
