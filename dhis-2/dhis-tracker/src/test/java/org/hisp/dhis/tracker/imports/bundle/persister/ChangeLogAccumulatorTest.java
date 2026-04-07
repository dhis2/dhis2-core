/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.imports.bundle.persister;

import static org.hisp.dhis.changelog.ChangeLogType.CREATE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.model.SingleEvent;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackerEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChangeLogAccumulatorTest {

  private ChangeLogAccumulator accumulator;

  private TrackedEntity trackedEntity;
  private TrackedEntityAttribute attribute;
  private TrackerEvent trackerEvent;
  private SingleEvent singleEvent;
  private DataElement dataElement;
  private Program programWithChangeLog;
  private Program programWithoutChangeLog;

  @BeforeEach
  void setUp() {
    accumulator = new ChangeLogAccumulator();

    TrackedEntityType teTypeWithChangeLog = new TrackedEntityType();
    teTypeWithChangeLog.setEnableChangeLog(true);

    TrackedEntityType teTypeWithoutChangeLog = new TrackedEntityType();
    teTypeWithoutChangeLog.setEnableChangeLog(false);

    trackedEntity = new TrackedEntity();
    trackedEntity.setTrackedEntityType(teTypeWithChangeLog);

    attribute = new TrackedEntityAttribute();

    programWithChangeLog = new Program();
    programWithChangeLog.setEnableChangeLog(true);

    programWithoutChangeLog = new Program();
    programWithoutChangeLog.setEnableChangeLog(false);

    ProgramStage stage = new ProgramStage();
    stage.setProgram(programWithChangeLog);

    trackerEvent = new TrackerEvent();
    trackerEvent.setProgramStage(stage);

    singleEvent = new SingleEvent();
    singleEvent.setProgramStage(stage);

    dataElement = new DataElement();
  }

  @Test
  void addTrackedEntityChangeLogWhenChangeLogEnabled() {
    accumulator.addTrackedEntityChangeLog(trackedEntity, attribute, null, "value", CREATE, "admin");

    ChangeLogAccumulator.Mark mark = accumulator.mark();
    assertEquals(1, mark.teSize());
  }

  @Test
  void addTrackedEntityChangeLogSkipsWhenChangeLogDisabled() {
    TrackedEntityType teType = new TrackedEntityType();
    teType.setEnableChangeLog(false);
    TrackedEntity te = new TrackedEntity();
    te.setTrackedEntityType(teType);

    accumulator.addTrackedEntityChangeLog(te, attribute, null, "value", CREATE, "admin");

    ChangeLogAccumulator.Mark mark = accumulator.mark();
    assertEquals(0, mark.teSize());
  }

  @Test
  void addTrackerEventChangeLogWhenChangeLogEnabled() {
    accumulator.addTrackerEventChangeLog(
        trackerEvent, dataElement, programWithChangeLog, null, "value", CREATE, "admin");

    ChangeLogAccumulator.Mark mark = accumulator.mark();
    assertEquals(1, mark.trackerEventSize());
  }

  @Test
  void addTrackerEventChangeLogSkipsWhenChangeLogDisabled() {
    accumulator.addTrackerEventChangeLog(
        trackerEvent, dataElement, programWithoutChangeLog, null, "value", CREATE, "admin");

    ChangeLogAccumulator.Mark mark = accumulator.mark();
    assertEquals(0, mark.trackerEventSize());
  }

  @Test
  void addSingleEventChangeLogWhenChangeLogEnabled() {
    accumulator.addSingleEventChangeLog(
        singleEvent, dataElement, programWithChangeLog, null, "value", CREATE, "admin");

    ChangeLogAccumulator.Mark mark = accumulator.mark();
    assertEquals(1, mark.singleEventSize());
  }

  @Test
  void addSingleEventChangeLogSkipsWhenChangeLogDisabled() {
    accumulator.addSingleEventChangeLog(
        singleEvent, dataElement, programWithoutChangeLog, null, "value", CREATE, "admin");

    ChangeLogAccumulator.Mark mark = accumulator.mark();
    assertEquals(0, mark.singleEventSize());
  }

  @Test
  void addTrackerEventFieldChangeLogWhenChangeLogEnabled() {
    accumulator.addTrackerEventFieldChangeLog(
        trackerEvent, "occurredAt", programWithChangeLog, null, "2024-01-01", CREATE, "admin");

    ChangeLogAccumulator.Mark mark = accumulator.mark();
    assertEquals(1, mark.trackerEventSize());
  }

  @Test
  void addTrackerEventFieldChangeLogSkipsWhenChangeLogDisabled() {
    accumulator.addTrackerEventFieldChangeLog(
        trackerEvent, "occurredAt", programWithoutChangeLog, null, "2024-01-01", CREATE, "admin");

    ChangeLogAccumulator.Mark mark = accumulator.mark();
    assertEquals(0, mark.trackerEventSize());
  }

  @Test
  void rollbackToDiscardEntriesAfterMark() {
    accumulator.addTrackedEntityChangeLog(trackedEntity, attribute, null, "v1", CREATE, "admin");
    accumulator.addTrackerEventChangeLog(
        trackerEvent, dataElement, programWithChangeLog, null, "v1", CREATE, "admin");

    ChangeLogAccumulator.Mark mark = accumulator.mark();

    accumulator.addTrackedEntityChangeLog(trackedEntity, attribute, null, "v2", CREATE, "admin");
    accumulator.addTrackerEventChangeLog(
        trackerEvent, dataElement, programWithChangeLog, null, "v2", CREATE, "admin");
    accumulator.addSingleEventChangeLog(
        singleEvent, dataElement, programWithChangeLog, null, "v2", CREATE, "admin");

    ChangeLogAccumulator.Mark afterAdd = accumulator.mark();
    assertEquals(2, afterAdd.teSize());
    assertEquals(2, afterAdd.trackerEventSize());
    assertEquals(1, afterAdd.singleEventSize());

    accumulator.rollbackTo(mark);

    ChangeLogAccumulator.Mark afterRollback = accumulator.mark();
    assertEquals(1, afterRollback.teSize());
    assertEquals(1, afterRollback.trackerEventSize());
    assertEquals(0, afterRollback.singleEventSize());
  }

  @Test
  void rollbackToIsNoOpWhenNoEntriesAdded() {
    ChangeLogAccumulator.Mark mark = accumulator.mark();

    accumulator.rollbackTo(mark);

    ChangeLogAccumulator.Mark afterRollback = accumulator.mark();
    assertEquals(0, afterRollback.teSize());
    assertEquals(0, afterRollback.trackerEventSize());
    assertEquals(0, afterRollback.singleEventSize());
  }
}
