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

import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.tracker.model.SingleEvent;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackerEvent;

/** Sets the correct import strategy (CREATE, UPDATE, DELETE) for every tracker object. */
class StrategyPreprocessor {

  private StrategyPreprocessor() {
    throw new UnsupportedOperationException("utility class");
  }

  static void process(TrackerBundle bundle) {
    TrackerImportStrategy importStrategy = bundle.getImportStrategy();

    for (org.hisp.dhis.tracker.imports.domain.TrackedEntity te : bundle.getTrackedEntities()) {
      TrackedEntity existing = bundle.getPreheat().getTrackedEntity(te.getTrackedEntity());
      if (importStrategy.isCreateAndUpdate()) {
        bundle.setStrategy(
            te, existing == null ? TrackerImportStrategy.CREATE : TrackerImportStrategy.UPDATE);
      } else {
        bundle.setStrategy(te, importStrategy);
      }
    }

    for (org.hisp.dhis.tracker.imports.domain.Enrollment enrollment : bundle.getEnrollments()) {
      Enrollment existing = bundle.getPreheat().getEnrollment(enrollment.getEnrollment());
      if (importStrategy.isCreateAndUpdate()) {
        bundle.setStrategy(
            enrollment,
            existing == null ? TrackerImportStrategy.CREATE : TrackerImportStrategy.UPDATE);
      } else {
        bundle.setStrategy(enrollment, importStrategy);
      }
    }

    for (org.hisp.dhis.tracker.imports.domain.TrackerEvent event : bundle.getTrackerEvents()) {
      TrackerEvent existing = bundle.getPreheat().getTrackerEvent(event.getEvent());
      if (importStrategy.isCreateAndUpdate()) {
        bundle.setStrategy(
            event, existing == null ? TrackerImportStrategy.CREATE : TrackerImportStrategy.UPDATE);
      } else {
        bundle.setStrategy(event, importStrategy);
      }
    }

    for (org.hisp.dhis.tracker.imports.domain.SingleEvent event : bundle.getSingleEvents()) {
      SingleEvent existing = bundle.getPreheat().getSingleEvent(event.getEvent());
      if (importStrategy.isCreateAndUpdate()) {
        bundle.setStrategy(
            event, existing == null ? TrackerImportStrategy.CREATE : TrackerImportStrategy.UPDATE);
      } else {
        bundle.setStrategy(event, importStrategy);
      }
    }

    for (Relationship relationship : bundle.getRelationships()) {
      org.hisp.dhis.tracker.model.Relationship existing =
          bundle.getPreheat().getRelationship(relationship.getUID());
      if (importStrategy.isCreateAndUpdate()) {
        bundle.setStrategy(
            relationship,
            existing == null ? TrackerImportStrategy.CREATE : TrackerImportStrategy.UPDATE);
      } else {
        bundle.setStrategy(relationship, importStrategy);
      }
    }
  }
}
