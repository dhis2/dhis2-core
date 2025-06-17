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

import static org.hisp.dhis.tracker.imports.validation.validator.All.all;
import static org.hisp.dhis.tracker.imports.validation.validator.Each.each;
import static org.hisp.dhis.tracker.imports.validation.validator.Field.field;
import static org.hisp.dhis.tracker.imports.validation.validator.Seq.seq;

import org.hisp.dhis.option.OptionService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.Validator;
import org.springframework.stereotype.Component;

/** Validator to validate all {@link Event}s in the {@link TrackerBundle}. */
@Component("org.hisp.dhis.tracker.imports.validation.validator.event.EventValidator")
public class EventValidator implements Validator<TrackerBundle> {
  private final Validator<TrackerBundle> validator;

  public EventValidator(
      SecurityTrackerEventValidator securityTrackerEventValidator,
      SecuritySingleEventValidator securitySingleEventValidator,
      CategoryOptValidator categoryOptValidator,
      OptionService optionService) {
    validator =
        all(
            each(
                TrackerBundle::getEvents,
                seq(
                    new ExistenceValidator(),
                    new MandatoryFieldsValidator(),
                    new MetaValidator(),
                    new UpdatableFieldsValidator(),
                    new DataRelationsValidator(),
                    new CategoryOptionComboValidator(),
                    seq(
                        new StatusValidator(),
                        securityTrackerEventValidator,
                        securitySingleEventValidator),
                    all(
                        categoryOptValidator,
                        new DateValidator(),
                        new GeoValidator(),
                        new NoteValidator(),
                        new DataValuesValidator(optionService),
                        new StatusUpdateValidator(),
                        new AssignedUserValidator()))),
            field(TrackerBundle::getTrackerEvents, new RepeatedEventsValidator()));
  }

  @Override
  public void validate(Reporter reporter, TrackerBundle bundle, TrackerBundle input) {
    validator.validate(reporter, bundle, input);
  }

  @Override
  public boolean needsToRun(TrackerImportStrategy strategy) {
    return true; // this main validator should always run
  }
}
