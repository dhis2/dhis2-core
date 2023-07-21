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
package org.hisp.dhis.webapi.controller.tracker.export.relationship;

import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.TrackerType.EVENT;
import static org.hisp.dhis.tracker.TrackerType.TRACKED_ENTITY;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.validateDeprecatedParameter;

import java.util.Objects;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.export.relationship.RelationshipOperationParams;
import org.hisp.dhis.webapi.common.UID;
import org.springframework.stereotype.Component;

/**
 * Maps operation parameters from {@link RelationshipsExportController} stored in {@link
 * RequestParams} to {@link RelationshipOperationParams} which is used to fetch relationships from
 * the service.
 */
@Component
@RequiredArgsConstructor
class RelationshipRequestParamsMapper {

  public RelationshipOperationParams map(RequestParams requestParams) throws BadRequestException {
    UID trackedEntity =
        validateDeprecatedParameter(
            "tei", requestParams.getTei(), "trackedEntity", requestParams.getTrackedEntity());

    if (ObjectUtils.allNull(
        trackedEntity, requestParams.getEnrollment(), requestParams.getEvent())) {
      throw new BadRequestException(
          "Missing required parameter 'trackedEntity', 'enrollment' or 'event'.");
    }

    if (hasMoreThanOneNotNull(
        trackedEntity, requestParams.getEnrollment(), requestParams.getEvent())) {
      throw new BadRequestException(
          "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.");
    }

    return RelationshipOperationParams.builder()
        .type(
            getTrackerType(trackedEntity, requestParams.getEnrollment(), requestParams.getEvent()))
        .identifier(
            ObjectUtils.firstNonNull(
                    trackedEntity, requestParams.getEnrollment(), requestParams.getEvent())
                .getValue())
        .pagingAndSortingCriteriaAdapter(requestParams)
        .build();
  }

  private TrackerType getTrackerType(UID trackedEntity, UID enrollment, UID event) {
    if (Objects.nonNull(trackedEntity)) {
      return TRACKED_ENTITY;
    } else if (Objects.nonNull(enrollment)) {
      return ENROLLMENT;
    } else if (Objects.nonNull(event)) {
      return EVENT;
    }
    return null;
  }

  private boolean hasMoreThanOneNotNull(Object... values) {
    return Stream.of(values).filter(Objects::nonNull).count() > 1;
  }
}
