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
package org.hisp.dhis.tracker.validation.hooks;

import java.util.List;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Note;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class PreCheckUidValidationHook extends AbstractTrackerDtoValidationHook {
  @Override
  public void validateTrackedEntity(ValidationErrorReporter reporter, TrackedEntity trackedEntity) {
    checkUidFormat(
        trackedEntity.getTrackedEntity(),
        reporter,
        trackedEntity,
        trackedEntity,
        trackedEntity.getTrackedEntity());
  }

  @Override
  public void validateEnrollment(ValidationErrorReporter reporter, Enrollment enrollment) {
    checkUidFormat(
        enrollment.getEnrollment(), reporter, enrollment, enrollment, enrollment.getEnrollment());

    validateNotesUid(enrollment.getNotes(), reporter, enrollment);
  }

  @Override
  public void validateEvent(ValidationErrorReporter reporter, Event event) {
    checkUidFormat(event.getEvent(), reporter, event, event, event.getEvent());

    validateNotesUid(event.getNotes(), reporter, event);
  }

  @Override
  public void validateRelationship(ValidationErrorReporter reporter, Relationship relationship) {
    checkUidFormat(
        relationship.getRelationship(),
        reporter,
        relationship,
        relationship,
        relationship.getRelationship());
  }

  private void validateNotesUid(
      List<Note> notes, ValidationErrorReporter reporter, TrackerDto dto) {
    for (Note note : notes) {
      checkUidFormat(note.getNote(), reporter, dto, note, note.getNote());
    }
  }

  /**
   * Check if the given UID has a valid format.
   *
   * @param checkUid a UID to be checked
   * @param reporter a {@see ValidationErrorReporter} to which the error is added
   * @param dto the dto to which the report will be linked to
   * @param args list of arguments for the Error report
   */
  private void checkUidFormat(
      String checkUid, ValidationErrorReporter reporter, TrackerDto dto, Object... args) {
    if (!CodeGenerator.isValidUid(checkUid)) {
      reporter.addError(dto, TrackerErrorCode.E1048, checkUid, args[0], args[1]);
    }
  }

  @Override
  public boolean removeOnError() {
    return true;
  }
}
