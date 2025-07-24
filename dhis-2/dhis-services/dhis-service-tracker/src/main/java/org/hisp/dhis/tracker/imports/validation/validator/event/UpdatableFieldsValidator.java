/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker.imports.validation.validator.event;

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1128;

import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.Validator;

/**
 * @author Enrico Colasante
 */
class UpdatableFieldsValidator implements Validator<org.hisp.dhis.tracker.imports.domain.Event> {
  @Override
  public void validate(
      Reporter reporter, TrackerBundle bundle, org.hisp.dhis.tracker.imports.domain.Event event) {
    ProgramStage programStage;
    Enrollment enrollment;
    if (event instanceof org.hisp.dhis.tracker.imports.domain.TrackerEvent) {

      TrackerEvent preheatEvent = bundle.getPreheat().getTrackerEvent(event.getEvent());
      programStage = preheatEvent.getProgramStage();
      enrollment = preheatEvent.getEnrollment();
    } else {
      SingleEvent preheatEvent = bundle.getPreheat().getSingleEvent(event.getEvent());
      programStage = preheatEvent.getProgramStage();
      enrollment = preheatEvent.getEnrollment();
    }
    reporter.addErrorIf(
        () -> !event.getProgramStage().isEqualTo(programStage), event, E1128, "programStage");
    reporter.addErrorIf(
        () ->
            event.getEnrollment() != null
                && !event.getEnrollment().getValue().equals(enrollment.getUid()),
        event,
        E1128,
        "enrollment");
  }

  @Override
  public boolean needsToRun(TrackerImportStrategy strategy) {
    return strategy == TrackerImportStrategy.UPDATE;
  }
}
