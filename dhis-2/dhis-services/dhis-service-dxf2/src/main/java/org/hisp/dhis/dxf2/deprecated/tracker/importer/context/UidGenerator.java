/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dxf2.deprecated.tracker.importer.context;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hisp.dhis.common.CodeGenerator.generateUid;
import static org.hisp.dhis.common.CodeGenerator.isValidUid;

import java.util.List;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Event;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Note;

/**
 * UID generator for Tracker entities.
 *
 * @author Luciano Fiandesio
 */
public class UidGenerator {
  /**
   * Generates a valid uid and assign it to the uid field of each event.
   *
   * <p>If the event has the 'event' field populated, it will be used for the 'uid' value
   *
   * <p>Generates a valid uid and assign it to all the notes of each event (if the UID is missing)
   *
   * @param events a List of {@see Events}
   * @return a List of {@see Events} with the uid field populated
   */
  public List<Event> assignUidToEvents(List<Event> events) {
    for (Event event : events) {
      doAssignUid(event);
    }
    return events;
  }

  public Event assignUidToEvent(Event event) {
    doAssignUid(event);
    return event;
  }

  private void doAssignUid(Event event) {
    if (isNotEmpty(event.getEvent()) && isValidUid(event.getEvent())) {
      event.setUid(event.getEvent());
    }

    if (isEmpty(event.getUid())) {
      final String uid = CodeGenerator.generateUid();
      event.setUid(uid);
      event.setEvent(uid);
    }

    List<Note> notes = event.getNotes();
    for (Note note : notes) {
      note.setNote(isValidUid(note.getNote()) ? note.getNote() : generateUid());
    }
  }
}
