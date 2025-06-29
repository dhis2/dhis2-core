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
package org.hisp.dhis.tracker.imports.preprocess;

import static java.util.Objects.nonNull;

import java.util.List;
import java.util.Optional;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.SingleEvent;
import org.springframework.stereotype.Component;

/**
 * This preprocessor is responsible for setting the Program UID on an Event from the ProgramStage if
 * the Program is not present in the payload.
 *
 * @author Enrico Colasante
 */
@Component
public class EventProgramPreProcessor implements BundlePreProcessor {
  @Override
  public void process(TrackerBundle bundle) {
    List<Event> eventsToPreprocess =
        bundle.getEvents().stream()
            .filter(
                e ->
                    e.getProgram() == null
                        || e.getProgram().isBlank()
                        || e.getProgramStage() == null
                        || e.getProgramStage().isBlank())
            .toList();

    for (Event event : eventsToPreprocess) {
      // Extract program from program stage
      if (nonNull(event.getProgramStage()) && event.getProgramStage().isNotBlank()) {
        ProgramStage programStage = bundle.getPreheat().getProgramStage(event.getProgramStage());
        if (nonNull(programStage)) {
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
          TrackerIdSchemeParams idSchemes = bundle.getPreheat().getIdSchemes();
          event.setProgram(idSchemes.toMetadataIdentifier(programStage.getProgram()));
          bundle.getPreheat().put(programStage.getProgram());
        }
      }
      // If it is a single event, extract program stage from program
      else if (nonNull(event.getProgram()) && event.getProgram().isNotBlank()) {
        Program program = bundle.getPreheat().getProgram(event.getProgram());
        if (nonNull(program) && event instanceof SingleEvent) {
          Optional<ProgramStage> programStage = program.getProgramStages().stream().findFirst();
          if (programStage.isPresent()) {
            TrackerIdSchemeParams idSchemes = bundle.getPreheat().getIdSchemes();
            event.setProgramStage(idSchemes.toMetadataIdentifier(programStage.get()));
            bundle.getPreheat().put(programStage.get());
          }
        }
      }
    }
  }

  @Override
  public int getPriority() {
    return -1;
  }
}
