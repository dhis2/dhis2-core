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
package org.hisp.dhis.tracker.imports.preprocess;

import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.springframework.stereotype.Component;

/**
 * This preprocessor is responsible for setting the Enrollment UID on an Event if the Program that
 * the event belongs is of type 'WITHOUT_REGISTRATION'
 *
 * @author Enrico Colasante
 */
@Component
public class EventWithoutRegistrationPreProcessor implements BundlePreProcessor {
  @Override
  public void process(TrackerBundle bundle) {
    for (Event event : bundle.getEvents()) {
      if (event.getProgramStage().isNotBlank()) {
        ProgramStage programStage = bundle.getPreheat().getProgramStage(event.getProgramStage());

        if (programStage != null) {
          // TODO remove if once metadata import is fixed
          if (programStage.getProgram() == null) {
            // Program stages should always have a program! Due to
            // how metadata
            // import is currently implemented
            // it's possible that users run into the edge case that
            // a program
            // stage does not have an associated
            // program. Tell the user it's an issue with the
            // metadata and not
            // the event itself. This should be
            // fixed in the metadata import. For more see
            // https://jira.dhis2.org/browse/DHIS2-12123
            //
            // PreCheckMandatoryFieldsValidationHook.validateEvent
            // will create
            // a validation error for this edge case
            return;
          }
          setEnrollment(bundle, programStage.getProgram().getUid(), event);
        }
      } else if (event.getProgram().isNotBlank()) {
        Program program = bundle.getPreheat().getProgram(event.getProgram());

        if (program != null) {
          setEnrollment(bundle, program.getUid(), event);
        }
      }
    }
  }

  private void setEnrollment(TrackerBundle bundle, String uid, Event event) {
    Enrollment enrollment = bundle.getPreheat().getEnrollmentsWithoutRegistration(uid);

    if (enrollment != null) {
      event.setEnrollment(enrollment.getUid());
    }
  }
}
