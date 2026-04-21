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
import org.hisp.dhis.program.Event;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChangeLogAccumulatorTest {

  private ChangeLogAccumulator accumulator;

  private TrackedEntity trackedEntity;
  private TrackedEntityAttribute attribute;
  private Event event;
  private DataElement dataElement;

  @BeforeEach
  void setUp() {
    accumulator = new ChangeLogAccumulator(true);

    trackedEntity = new TrackedEntity();
    attribute = new TrackedEntityAttribute();
    event = new Event();
    dataElement = new DataElement();
  }

  @Test
  void addTrackedEntityChangeLog() {
    accumulator.addTrackedEntityChangeLog(trackedEntity, attribute, null, "value", CREATE, "admin");

    ChangeLogAccumulator.Mark mark = accumulator.mark();
    assertEquals(1, mark.teSize());
  }

  @Test
  void addEventChangeLog() {
    accumulator.addEventChangeLog(event, dataElement, null, "value", CREATE, "admin");

    ChangeLogAccumulator.Mark mark = accumulator.mark();
    assertEquals(1, mark.eventSize());
  }

  @Test
  void addEventFieldChangeLog() {
    accumulator.addEventFieldChangeLog(event, "occurredAt", null, "2024-01-01", CREATE, "admin");

    ChangeLogAccumulator.Mark mark = accumulator.mark();
    assertEquals(1, mark.eventSize());
  }

  @Test
  void rollbackToDiscardEntriesAfterMark() {
    accumulator.addTrackedEntityChangeLog(trackedEntity, attribute, null, "v1", CREATE, "admin");
    accumulator.addEventChangeLog(event, dataElement, null, "v1", CREATE, "admin");

    ChangeLogAccumulator.Mark mark = accumulator.mark();

    accumulator.addTrackedEntityChangeLog(trackedEntity, attribute, null, "v2", CREATE, "admin");
    accumulator.addEventChangeLog(event, dataElement, null, "v2", CREATE, "admin");

    ChangeLogAccumulator.Mark afterAdd = accumulator.mark();
    assertEquals(2, afterAdd.teSize());
    assertEquals(2, afterAdd.eventSize());

    accumulator.rollbackTo(mark);

    ChangeLogAccumulator.Mark afterRollback = accumulator.mark();
    assertEquals(1, afterRollback.teSize());
    assertEquals(1, afterRollback.eventSize());
  }

  @Test
  void addIsNoOpWhenDisabled() {
    ChangeLogAccumulator disabled = new ChangeLogAccumulator(false);

    disabled.addTrackedEntityChangeLog(trackedEntity, attribute, null, "value", CREATE, "admin");
    disabled.addEventChangeLog(event, dataElement, null, "value", CREATE, "admin");
    disabled.addEventFieldChangeLog(event, "occurredAt", null, "2024-01-01", CREATE, "admin");

    ChangeLogAccumulator.Mark mark = disabled.mark();
    assertEquals(0, mark.teSize());
    assertEquals(0, mark.eventSize());
  }

  @Test
  void rollbackToIsNoOpWhenNoEntriesAdded() {
    ChangeLogAccumulator.Mark mark = accumulator.mark();

    accumulator.rollbackTo(mark);

    ChangeLogAccumulator.Mark afterRollback = accumulator.mark();
    assertEquals(0, afterRollback.teSize());
    assertEquals(0, afterRollback.eventSize());
  }
}
