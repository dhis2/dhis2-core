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
package org.hisp.dhis.tracker.imports.validation.validator;

import static org.hisp.dhis.tracker.imports.validation.validator.All.all;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.Validator;
import org.hisp.dhis.tracker.imports.validation.validator.enrollment.EnrollmentValidator;
import org.hisp.dhis.tracker.imports.validation.validator.event.EventValidator;
import org.hisp.dhis.tracker.imports.validation.validator.relationship.RelationshipValidator;
import org.hisp.dhis.tracker.imports.validation.validator.trackedentity.TrackedEntityValidator;
import org.springframework.stereotype.Component;

/** Validator to validate the {@link TrackerBundle}. */
@RequiredArgsConstructor
@Component("org.hisp.dhis.tracker.imports.validation.validator.DefaultValidator")
public class DefaultValidator implements Validator<TrackerBundle> {

  private final TrackedEntityValidator trackedEntityValidator;

  private final EnrollmentValidator enrollmentValidator;

  private final EventValidator eventValidator;

  private final RelationshipValidator relationshipValidator;

  private Validator<TrackerBundle> bundleValidator() {
    return all(trackedEntityValidator, enrollmentValidator, eventValidator, relationshipValidator);
  }

  @Override
  public void validate(Reporter reporter, TrackerBundle bundle, TrackerBundle input) {
    bundleValidator().validate(reporter, bundle, input);
  }

  @Override
  public boolean needsToRun(TrackerImportStrategy strategy) {
    return true; // this main validator should always run
  }
}
