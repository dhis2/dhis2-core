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
package org.hisp.dhis.webapi.controller.tracker.export.relationship;

import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.TrackerType.EVENT;
import static org.hisp.dhis.tracker.TrackerType.TRACKED_ENTITY;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateOrderParams;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.common.OrderCriteria;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.export.relationship.RelationshipFields;
import org.hisp.dhis.tracker.export.relationship.RelationshipOperationParams;
import org.hisp.dhis.tracker.export.relationship.RelationshipOperationParams.RelationshipOperationParamsBuilder;

/**
 * Maps operation parameters from {@link RelationshipsExportController} stored in {@link
 * RelationshipRequestParams} to {@link RelationshipOperationParams} which is used to fetch
 * relationships from the service.
 */
class RelationshipRequestParamsMapper {
  private static final Set<String> ORDERABLE_FIELD_NAMES =
      RelationshipMapper.ORDERABLE_FIELDS.keySet();

  private RelationshipRequestParamsMapper() {
    throw new IllegalStateException("Utility class");
  }

  public static RelationshipOperationParams map(RelationshipRequestParams relationshipRequestParams)
      throws BadRequestException {
    UID trackedEntity = relationshipRequestParams.getTrackedEntity();

    if (hasMoreThanOneNotNull(
        trackedEntity,
        relationshipRequestParams.getEnrollment(),
        relationshipRequestParams.getEvent())) {
      throw new BadRequestException(
          "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.");
    }

    RelationshipOperationParamsBuilder builder =
        RelationshipOperationParams.builder(
            getTrackerType(
                trackedEntity,
                relationshipRequestParams.getEnrollment(),
                relationshipRequestParams.getEvent()),
            ObjectUtils.firstNonNull(
                trackedEntity,
                relationshipRequestParams.getEnrollment(),
                relationshipRequestParams.getEvent()));

    validateOrderParams(relationshipRequestParams.getOrder(), ORDERABLE_FIELD_NAMES);

    mapOrderParam(builder, relationshipRequestParams.getOrder());

    return builder
        .fields(
            RelationshipFields.of(
                relationshipRequestParams.getFields()::includes, FieldPath.FIELD_PATH_SEPARATOR))
        .includeDeleted(relationshipRequestParams.isIncludeDeleted())
        .build();
  }

  private static TrackerType getTrackerType(UID trackedEntity, UID enrollment, UID event)
      throws BadRequestException {
    if (Objects.nonNull(trackedEntity)) {
      return TRACKED_ENTITY;
    } else if (Objects.nonNull(enrollment)) {
      return ENROLLMENT;
    } else if (Objects.nonNull(event)) {
      return EVENT;
    }
    throw new BadRequestException(
        "Missing required parameter 'trackedEntity', 'enrollment' or 'event'.");
  }

  private static boolean hasMoreThanOneNotNull(Object... values) {
    return Stream.of(values).filter(Objects::nonNull).count() > 1;
  }

  private static void mapOrderParam(
      RelationshipOperationParamsBuilder builder, List<OrderCriteria> orders) {
    if (orders == null || orders.isEmpty()) {
      return;
    }

    for (OrderCriteria order : orders) {
      if (RelationshipMapper.ORDERABLE_FIELDS.containsKey(order.getField())) {
        builder.orderBy(
            RelationshipMapper.ORDERABLE_FIELDS.get(order.getField()), order.getDirection());
      }
    }
  }
}
