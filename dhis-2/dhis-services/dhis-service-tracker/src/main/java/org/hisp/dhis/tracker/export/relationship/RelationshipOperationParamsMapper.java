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
package org.hisp.dhis.tracker.export.relationship;

import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.SoftDeletableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.collection.CollectionUtils;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.acl.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserUtil;
import org.springframework.stereotype.Component;

/**
 * Maps {@link RelationshipOperationParams} to {@link RelationshipQueryParams} which is used to
 * fetch enrollments from the DB.
 */
@Component
@RequiredArgsConstructor
class RelationshipOperationParamsMapper {

  private final HibernateRelationshipStore relationshipStore;
  private final TrackerAccessManager trackerAccessManager;

  public RelationshipQueryParams map(@Nonnull RelationshipOperationParams params)
      throws NotFoundException, ForbiddenException {

    if (params.getType() == null) {
      if (CollectionUtils.isEmpty(params.getRelationships())) {
        throw new IllegalArgumentException("relationships cannot be empty when type is null");
      }
      return new RelationshipQueryParams(
          null, params.getOrder(), params.isIncludeDeleted(), params.getRelationships());
    }

    SoftDeletableObject entity =
        switch (params.getType()) {
          case TRACKED_ENTITY ->
              getTrackedEntity(params.getIdentifier(), params.isIncludeDeleted());
          case ENROLLMENT -> getEnrollment(params.getIdentifier(), params.isIncludeDeleted());
          case EVENT -> getEvent(params.getIdentifier(), params.isIncludeDeleted());
          case RELATIONSHIP -> throw new IllegalArgumentException("Unsupported type");
        };

    return new RelationshipQueryParams(entity, params.getOrder(), params.isIncludeDeleted(), null);
  }

  private TrackedEntity getTrackedEntity(UID trackedEntityUid, boolean includeDeleted)
      throws NotFoundException, ForbiddenException {
    TrackedEntity trackedEntity =
        relationshipStore
            .findTrackedEntity(trackedEntityUid, includeDeleted)
            .orElseThrow(() -> new NotFoundException(TrackedEntity.class, trackedEntityUid));
    if (!trackerAccessManager
        .canRead(CurrentUserUtil.getCurrentUserDetails(), trackedEntity)
        .isEmpty()) {
      throw new ForbiddenException(TrackedEntity.class, trackedEntityUid);
    }
    return trackedEntity;
  }

  private Enrollment getEnrollment(UID enrollmentUid, boolean includeDeleted)
      throws NotFoundException, ForbiddenException {
    Enrollment enrollment =
        relationshipStore
            .findEnrollment(enrollmentUid, includeDeleted)
            .orElseThrow(() -> new NotFoundException(Enrollment.class, enrollmentUid));
    if (!trackerAccessManager
        .canRead(CurrentUserUtil.getCurrentUserDetails(), enrollment, false)
        .isEmpty()) {
      throw new ForbiddenException(Enrollment.class, enrollmentUid);
    }
    return enrollment;
  }

  private SoftDeletableObject getEvent(UID eventUid, boolean includeDeleted)
      throws NotFoundException, ForbiddenException {
    Optional<Event> event = relationshipStore.findEvent(eventUid, includeDeleted);
    if (event.isPresent()) {
      if (!trackerAccessManager
          .canRead(CurrentUserUtil.getCurrentUserDetails(), event.get(), false)
          .isEmpty()) {
        throw new ForbiddenException(Event.class, eventUid);
      }
      return event.get();
    }

    Optional<SingleEvent> singleEvent = relationshipStore.findSingleEvent(eventUid, includeDeleted);
    if (singleEvent.isPresent()) {
      if (!trackerAccessManager
          .canRead(CurrentUserUtil.getCurrentUserDetails(), singleEvent.get())
          .isEmpty()) {
        throw new ForbiddenException(Event.class, eventUid);
      }
      return singleEvent.get();
    }
    throw new NotFoundException(Event.class, eventUid);
  }
}
