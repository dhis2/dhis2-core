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

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1030;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1032;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1082;

import org.hisp.dhis.common.SoftDeletableObject;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.SingleEvent;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.Validator;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class ExistenceValidator implements Validator<org.hisp.dhis.tracker.imports.domain.Event> {
  @Override
  public void validate(
      Reporter reporter, TrackerBundle bundle, org.hisp.dhis.tracker.imports.domain.Event event) {
    TrackerImportStrategy importStrategy = bundle.getStrategy(event);

    SoftDeletableObject existingEvent = getExistingEvent(event, bundle.getPreheat());

    // If the event is soft-deleted no operation is allowed
    if (existingEvent != null && existingEvent.isDeleted()) {
      reporter.addError(event, E1082, event.getEvent());
      return;
    }

    if (existingEvent != null && importStrategy.isCreate()) {
      reporter.addError(event, E1030, event.getEvent());
    } else if (existingEvent == null && importStrategy.isUpdateOrDelete()) {
      reporter.addError(event, E1032, event.getEvent());
    }
  }

  private SoftDeletableObject getExistingEvent(
      org.hisp.dhis.tracker.imports.domain.Event event, TrackerPreheat preheat) {
    if (event instanceof TrackerEvent) {
      return preheat.getTrackerEvent(event.getEvent());
    } else if (event instanceof SingleEvent) {
      return preheat.getSingleEvent(event.getEvent());
    }
    return null;
  }

  @Override
  public boolean needsToRun(TrackerImportStrategy strategy) {
    return true;
  }
}
