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
package org.hisp.dhis.tracker.imports.note;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.singleevent.SingleEventService;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventService;
import org.hisp.dhis.tracker.imports.domain.Note;
import org.hisp.dhis.user.CurrentUserUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultNoteService implements NoteService {
  private final EnrollmentService enrollmentService;

  private final TrackerEventService trackerEventService;

  private final SingleEventService singleEventService;

  private final IdentifiableObjectManager manager;

  private final JdbcNoteStore noteStore;

  @Transactional
  @Override
  public void addNoteForEnrollment(Note note, UID enrollment)
      throws NotFoundException, BadRequestException {
    // Check enrollment existence and access
    enrollmentService.getEnrollment(enrollment);
    validateNote(note);

    noteStore.saveEnrollmentNote(enrollment, note, CurrentUserUtil.getCurrentUserDetails());
  }

  @Transactional
  @Override
  public void addNoteForEvent(Note note, UID event) throws NotFoundException, BadRequestException {
    // Check event existence and access
    if (trackerEventService.exists(event)) {
      trackerEventService.getEvent(event);
      validateNote(note);
      noteStore.saveTrackerEventNote(event, note, CurrentUserUtil.getCurrentUserDetails());
    } else {
      singleEventService.getEvent(event);
      validateNote(note);
      noteStore.saveSingleEventNote(event, note, CurrentUserUtil.getCurrentUserDetails());
    }
  }

  private void validateNote(Note note) throws BadRequestException {
    if (isEmpty(note.getValue())) {
      throw new BadRequestException("Value cannot be empty");
    }

    if (noteStore.exists(note.getNote())) {
      throw new BadRequestException(String.format("Note `%s` already exists.", note.getNote()));
    }
  }
}
