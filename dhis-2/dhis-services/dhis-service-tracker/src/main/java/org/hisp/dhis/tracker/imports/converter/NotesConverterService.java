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
package org.hisp.dhis.tracker.imports.converter;

import java.util.List;
import java.util.stream.Collectors;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.tracker.imports.domain.User;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;

/**
 * @author Luciano Fiandesio
 */
@Service
public class NotesConverterService
    implements TrackerConverterService<org.hisp.dhis.tracker.imports.domain.Note, Note> {
  @Override
  public org.hisp.dhis.tracker.imports.domain.Note to(Note note) {
    org.hisp.dhis.tracker.imports.domain.Note trackerNote =
        new org.hisp.dhis.tracker.imports.domain.Note();
    trackerNote.setNote(note.getUid());
    trackerNote.setValue(note.getNoteText());
    trackerNote.setStoredAt(DateUtils.instantFromDate(note.getCreated()));
    trackerNote.setCreatedBy(
        User.builder()
            .username(note.getLastUpdatedBy().getUsername())
            .uid(note.getLastUpdatedBy().getUid())
            .firstName(note.getLastUpdatedBy().getFirstName())
            .surname(note.getLastUpdatedBy().getSurname())
            .build());
    trackerNote.setStoredBy(note.getCreator());
    return trackerNote;
  }

  @Override
  public List<org.hisp.dhis.tracker.imports.domain.Note> to(List<Note> notes) {
    return notes.stream().map(this::to).collect(Collectors.toList());
  }

  @Override
  public Note from(TrackerPreheat preheat, org.hisp.dhis.tracker.imports.domain.Note note) {
    org.hisp.dhis.note.Note trackerNote = new org.hisp.dhis.note.Note();
    trackerNote.setUid(note.getNote());
    trackerNote.setAutoFields();
    trackerNote.setNoteText(note.getValue());

    trackerNote.setLastUpdatedBy(preheat.getUser());
    trackerNote.setCreator(note.getStoredBy());
    return trackerNote;
  }

  @Override
  public List<Note> from(
      TrackerPreheat preheat, List<org.hisp.dhis.tracker.imports.domain.Note> notes) {
    return notes.stream().map(n -> from(preheat, n)).collect(Collectors.toList());
  }
}
