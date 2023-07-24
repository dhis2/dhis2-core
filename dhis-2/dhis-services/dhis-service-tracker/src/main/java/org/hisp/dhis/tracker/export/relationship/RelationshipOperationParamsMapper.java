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
package org.hisp.dhis.tracker.export.relationship;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps {@link RelationshipOperationParams} to {@link RelationshipQueryParams} which is used to
 * fetch enrollments from the DB.
 */
@Component
@RequiredArgsConstructor
class RelationshipOperationParamsMapper {

  private final TrackedEntityService trackedEntityService;

  private final EnrollmentService enrollmentService;

  private final EventService eventService;

  @Transactional(readOnly = true)
  public RelationshipQueryParams map(RelationshipOperationParams params) throws NotFoundException {

    IdentifiableObject entity =
        switch (params.getType()) {
          case TRACKED_ENTITY -> validateTrackedEntity(params.getIdentifier());
          case ENROLLMENT -> validateEnrollment(params.getIdentifier());
          case EVENT -> validateEvent(params.getIdentifier());
          case RELATIONSHIP -> throw new IllegalArgumentException("Unsupported type");
        };

    return RelationshipQueryParams.builder()
        .entity(entity)
        .page(params.getPage())
        .pageSize(params.getPageSize())
        .totalPages(params.isTotalPages())
        .skipPaging(params.isSkipPaging())
        .build();
  }

  private TrackedEntity validateTrackedEntity(String uid) throws NotFoundException {
    TrackedEntity trackedEntity = trackedEntityService.getTrackedEntity(uid);
    if (trackedEntity == null) {
      throw new NotFoundException("Tracked entity is specified but does not exist: " + uid);
    }

    return trackedEntity;
  }

  private Enrollment validateEnrollment(String uid) throws NotFoundException {
    Enrollment enrollment = enrollmentService.getEnrollment(uid);
    if (enrollment == null) {
      throw new NotFoundException("Enrollment is specified but does not exist: " + uid);
    }

    return enrollment;
  }

  private Event validateEvent(String uid) throws NotFoundException {
    Event event = eventService.getEvent(uid);
    if (event == null) {
      throw new NotFoundException("Event is specified but does not exist: " + uid);
    }

    return event;
  }
}
