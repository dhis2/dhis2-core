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

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
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

  private final TrackerAccessManager accessManager;

  private final CurrentUserService currentUserService;

  @Transactional(readOnly = true)
  public RelationshipQueryParams map(RelationshipOperationParams params)
      throws NotFoundException, ForbiddenException {

    User user = currentUserService.getCurrentUser();

    IdentifiableObject entity =
        switch (params.getType()) {
          case TRACKED_ENTITY -> validateTrackedEntity(user, params.getIdentifier());
          case ENROLLMENT -> validateEnrollment(user, params.getIdentifier());
          case EVENT -> validateEvent(user, params.getIdentifier());
          case RELATIONSHIP -> throw new IllegalArgumentException("Unsupported type");
        };

    return RelationshipQueryParams.builder()
        .entity(entity)
        .page(params.getPage())
        .pageSize(params.getPageSize())
        .totalPages(params.isTotalPages())
        .skipPaging(params.isSkipPaging())
        .order(params.getOrder())
        .build();
  }

  private TrackedEntity validateTrackedEntity(User user, String uid)
      throws NotFoundException, ForbiddenException {
    TrackedEntity trackedEntity = trackedEntityService.getTrackedEntity(uid);
    if (trackedEntity == null) {
      throw new NotFoundException(TrackedEntity.class, uid);
    }

    List<String> errors = accessManager.canRead(user, trackedEntity);
    if (!errors.isEmpty()) {
      throw new ForbiddenException(TrackedEntity.class, uid);
    }

    return trackedEntity;
  }

  private Enrollment validateEnrollment(User user, String uid)
      throws NotFoundException, ForbiddenException {
    Enrollment enrollment = enrollmentService.getEnrollment(uid);
    if (enrollment == null) {
      throw new NotFoundException(Enrollment.class, uid);
    }

    List<String> errors = accessManager.canRead(user, enrollment, false);
    if (!errors.isEmpty()) {
      throw new ForbiddenException(Enrollment.class, uid);
    }

    return enrollment;
  }

  private Event validateEvent(User user, String uid) throws NotFoundException, ForbiddenException {
    Event event = eventService.getEvent(uid);
    if (event == null) {
      throw new NotFoundException(Event.class, uid);
    }

    List<String> errors = accessManager.canRead(user, event, false);
    if (!errors.isEmpty()) {
      throw new ForbiddenException(Event.class, uid);
    }

    return event;
  }
}
