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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.ProgramStage;
import org.junit.jupiter.api.Test;

class StageQueryItemClassifierTest {
  private final StageQueryItemClassifier subject = new DefaultStageQueryItemClassifier();

  @Test
  void shouldClassifyStageEventDate() {
    QueryItem item = createItem(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME, true);

    assertTrue(subject.isStageEventDate(item));
    assertTrue(subject.isStageDate(item));
    assertTrue(subject.isStageScoped(item));
  }

  @Test
  void shouldClassifyStageScheduledDate() {
    QueryItem item = createItem(EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME, true);

    assertTrue(subject.isStageScheduledDate(item));
    assertTrue(subject.isStageDate(item));
    assertTrue(subject.isStageScoped(item));
  }

  @Test
  void shouldClassifyStageEventStatus() {
    QueryItem item = createItem(EventAnalyticsColumnName.EVENT_STATUS_COLUMN_NAME, true);

    assertTrue(subject.isStageEventStatus(item));
    assertTrue(subject.isStageScoped(item));
  }

  @Test
  void shouldClassifyStageOrgUnit() {
    QueryItem item = createItem(EventAnalyticsColumnName.OU_COLUMN_NAME, true);

    assertTrue(subject.isStageOrgUnit(item));
    assertTrue(subject.isStageScoped(item));
  }

  @Test
  void shouldNotClassifyNonStageItem() {
    QueryItem item = createItem(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME, false);

    assertFalse(subject.isStageDate(item));
    assertFalse(subject.isStageOrgUnit(item));
    assertFalse(subject.isStageEventStatus(item));
    assertFalse(subject.isStageScoped(item));
  }

  @Test
  void shouldNotClassifyNullItem() {
    assertFalse(subject.isStageDate(null));
    assertFalse(subject.isStageEventDate(null));
    assertFalse(subject.isStageScheduledDate(null));
    assertFalse(subject.isStageEventStatus(null));
    assertFalse(subject.isStageOrgUnit(null));
    assertFalse(subject.isStageScoped(null));
  }

  private QueryItem createItem(String itemId, boolean withProgramStage) {
    DataElement dataElement = new DataElement();
    dataElement.setUid(itemId);
    QueryItem item = new QueryItem(dataElement);
    if (withProgramStage) {
      ProgramStage stage = new ProgramStage();
      stage.setUid("s1234567890");
      item.setProgramStage(stage);
    }
    return item;
  }
}
