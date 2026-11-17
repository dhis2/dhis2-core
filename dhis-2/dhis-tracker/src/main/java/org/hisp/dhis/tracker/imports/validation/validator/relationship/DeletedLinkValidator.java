/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.tracker.imports.validation.validator.relationship;

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E4021;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E4022;
import static org.hisp.dhis.tracker.imports.validation.validator.ValidationUtils.findDeletedEnrollmentOrTrackedEntity;

import org.hisp.dhis.common.UID;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.domain.RelationshipItem;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.Validator;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.tracker.model.SingleEvent;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackerEvent;

/**
 * Rejects creating a relationship that links to a soft deleted tracked entity, enrollment or event,
 * or to an enrollment or event under a soft deleted enrollment or tracked entity.
 *
 * <p>It runs after the security validators, so that a user without access to the linked entities
 * does not learn about them from the error. Entities that only exist in the payload are not checked
 * here: their own validation rejects them if they are deleted or belong to a deleted parent.
 */
class DeletedLinkValidator implements Validator<Relationship> {
  @Override
  public void validate(Reporter reporter, TrackerBundle bundle, Relationship relationship) {
    validateItem(reporter, bundle.getPreheat(), relationship, relationship.getFrom());
    validateItem(reporter, bundle.getPreheat(), relationship, relationship.getTo());
  }

  private static void validateItem(
      Reporter reporter, TrackerPreheat preheat, Relationship relationship, RelationshipItem item) {
    if (item.getTrackedEntity() != null) {
      validateTrackedEntity(
          reporter, relationship, preheat.getTrackedEntity(item.getTrackedEntity()));
    } else if (item.getEnrollment() != null) {
      validateEnrollment(reporter, relationship, preheat.getEnrollment(item.getEnrollment()));
    } else if (item.getEvent() != null) {
      validateEvent(reporter, preheat, relationship, item.getEvent());
    }
  }

  private static void validateTrackedEntity(
      Reporter reporter, Relationship relationship, TrackedEntity trackedEntity) {
    if (trackedEntity != null && trackedEntity.isDeleted()) {
      reporter.addError(
          relationship, E4021, TrackerType.TRACKED_ENTITY.getName(), trackedEntity.getUid());
    }
  }

  private static void validateEnrollment(
      Reporter reporter, Relationship relationship, Enrollment enrollment) {
    if (enrollment == null) {
      return;
    }
    findDeletedEnrollmentOrTrackedEntity(enrollment)
        .ifPresent(
            deleted -> {
              if (deleted.type() == TrackerType.ENROLLMENT) {
                reporter.addError(
                    relationship, E4021, TrackerType.ENROLLMENT.getName(), enrollment.getUid());
              } else {
                reporter.addError(
                    relationship,
                    E4022,
                    TrackerType.ENROLLMENT.getName(),
                    enrollment.getUid(),
                    deleted.type().getName(),
                    deleted.uid());
              }
            });
  }

  private static void validateEvent(
      Reporter reporter, TrackerPreheat preheat, Relationship relationship, UID uid) {
    SingleEvent singleEvent = preheat.getSingleEvent(uid);
    if (singleEvent != null && singleEvent.isDeleted()) {
      reporter.addError(relationship, E4021, TrackerType.EVENT.getName(), uid);
      return;
    }

    TrackerEvent trackerEvent = preheat.getTrackerEvent(uid);
    if (trackerEvent == null) {
      return;
    }
    if (trackerEvent.isDeleted()) {
      reporter.addError(relationship, E4021, TrackerType.EVENT.getName(), uid);
      return;
    }
    if (trackerEvent.getEnrollment() == null) {
      return;
    }
    findDeletedEnrollmentOrTrackedEntity(trackerEvent.getEnrollment())
        .ifPresent(
            deleted ->
                reporter.addError(
                    relationship,
                    E4022,
                    TrackerType.EVENT.getName(),
                    uid,
                    deleted.type().getName(),
                    deleted.uid()));
  }

  @Override
  public boolean needsToRun(TrackerImportStrategy strategy) {
    return strategy.isCreate();
  }
}
