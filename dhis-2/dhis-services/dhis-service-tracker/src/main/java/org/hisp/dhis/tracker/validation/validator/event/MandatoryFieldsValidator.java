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
package org.hisp.dhis.tracker.validation.validator.event;

import static org.hisp.dhis.tracker.validation.ValidationCode.E1008;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1123;

import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.validation.Reporter;
import org.hisp.dhis.tracker.validation.Validator;

/**
 * @author Enrico Colasante
 */
class MandatoryFieldsValidator implements Validator<Event> {
  @Override
  public void validate(Reporter reporter, TrackerBundle bundle, Event event) {
    reporter.addErrorIf(() -> event.getOrgUnit().isBlank(), event, E1123, "orgUnit");
    reporter.addErrorIf(() -> event.getProgramStage().isBlank(), event, E1123, "programStage");

    // TODO remove if once metadata import is fixed
    ProgramStage programStage = bundle.getPreheat().getProgramStage(event.getProgramStage());
    if (programStage != null) {
      // Program stages should always have a program! Due to how metadata
      // import is currently implemented
      // it's possible that users run into the edge case that a program
      // stage does not have an associated
      // program. Tell the user it's an issue with the metadata and not
      // the event itself. This should be
      // fixed in the metadata import. For more see
      // https://jira.dhis2.org/browse/DHIS2-12123
      reporter.addErrorIfNull(programStage.getProgram(), event, E1008, event.getProgramStage());
      // return since program is not a required field according to our API
      // and the issue is with the missing reference in
      // the DB entry of the program stage and not the payload itself
      return;
    }

    reporter.addErrorIf(event.getProgram()::isBlank, event, E1123, "program");
  }
}
