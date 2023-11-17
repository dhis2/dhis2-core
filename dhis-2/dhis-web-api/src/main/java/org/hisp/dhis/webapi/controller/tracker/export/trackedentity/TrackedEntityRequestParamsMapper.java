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
package org.hisp.dhis.webapi.controller.tracker.export.trackedentity;

import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.parseFilters;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validateDeprecatedParameter;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validateDeprecatedUidsParameter;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validateOrderParams;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validateOrgUnitModeForTrackedEntities;

import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.AssignedUserQueryParam;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams.TrackedEntityOperationParamsBuilder;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.springframework.stereotype.Component;

/**
 * Maps operation parameters from {@link TrackedEntitiesExportController} stored in {@link
 * RequestParams} to {@link TrackedEntityOperationParams} which is used to fetch tracked entities.
 */
@Component
@RequiredArgsConstructor
class TrackedEntityRequestParamsMapper {
  private static final Set<String> ORDERABLE_FIELD_NAMES =
      TrackedEntityMapper.ORDERABLE_FIELDS.keySet();

  private final TrackedEntityFieldsParamMapper fieldsParamMapper;

  public TrackedEntityOperationParams map(RequestParams requestParams, User user)
      throws BadRequestException {
    return map(requestParams, user, requestParams.getFields());
  }

  public TrackedEntityOperationParams map(
      RequestParams requestParams, User user, List<FieldPath> fields) throws BadRequestException {
    validateRemovedParameters(requestParams);

    Set<UID> assignedUsers =
        validateDeprecatedUidsParameter(
            "assignedUser",
            requestParams.getAssignedUser(),
            "assignedUsers",
            requestParams.getAssignedUsers());

    Set<UID> orgUnitUids =
        validateDeprecatedUidsParameter(
            "orgUnit", requestParams.getOrgUnit(), "orgUnits", requestParams.getOrgUnits());

    OrganisationUnitSelectionMode orgUnitMode =
        validateDeprecatedParameter(
            "ouMode", requestParams.getOuMode(), "orgUnitMode", requestParams.getOrgUnitMode());

    orgUnitMode =
        validateOrgUnitModeForTrackedEntities(
            orgUnitUids, orgUnitMode, requestParams.getTrackedEntities());

    Set<UID> trackedEntities =
        validateDeprecatedUidsParameter(
            "trackedEntity",
            requestParams.getTrackedEntity(),
            "trackedEntities",
            requestParams.getTrackedEntities());
    validateOrderParams(requestParams.getOrder(), ORDERABLE_FIELD_NAMES, "attribute");
    validateRequestParams(requestParams);

    Map<String, List<QueryFilter>> filters = parseFilters(requestParams.getFilter());

    TrackedEntityOperationParamsBuilder builder =
        TrackedEntityOperationParams.builder()
            .programUid(
                requestParams.getProgram() == null ? null : requestParams.getProgram().getValue())
            .programStageUid(
                requestParams.getProgramStage() == null
                    ? null
                    : requestParams.getProgramStage().getValue())
            .programStatus(requestParams.getProgramStatus())
            .followUp(requestParams.getFollowUp())
            .lastUpdatedStartDate(requestParams.getUpdatedAfter())
            .lastUpdatedEndDate(requestParams.getUpdatedBefore())
            .lastUpdatedDuration(requestParams.getUpdatedWithin())
            .programEnrollmentStartDate(requestParams.getEnrollmentEnrolledAfter())
            .programEnrollmentEndDate(requestParams.getEnrollmentEnrolledBefore())
            .programIncidentStartDate(requestParams.getEnrollmentOccurredAfter())
            .programIncidentEndDate(requestParams.getEnrollmentOccurredBefore())
            .trackedEntityTypeUid(
                requestParams.getTrackedEntityType() == null
                    ? null
                    : requestParams.getTrackedEntityType().getValue())
            .organisationUnits(UID.toValueSet(orgUnitUids))
            .orgUnitMode(orgUnitMode)
            .eventStatus(requestParams.getEventStatus())
            .eventStartDate(requestParams.getEventOccurredAfter())
            .eventEndDate(requestParams.getEventOccurredBefore())
            .assignedUserQueryParam(
                new AssignedUserQueryParam(
                    requestParams.getAssignedUserMode(), user, UID.toValueSet(assignedUsers)))
            .user(user)
            .trackedEntityUids(UID.toValueSet(trackedEntities))
            .filters(filters)
            .includeDeleted(requestParams.isIncludeDeleted())
            .potentialDuplicate(requestParams.getPotentialDuplicate())
            .trackedEntityParams(fieldsParamMapper.map(fields));

    mapOrderParam(builder, requestParams.getOrder());

    return builder.build();
  }

  private void validateRequestParams(RequestParams params) throws BadRequestException {

    String violation = null;
    if (params.getProgram() != null && params.getTrackedEntityType() != null) {
      violation = "Program and tracked entity cannot be specified simultaneously";
    }

    if (params.getProgram() == null) {
      if (params.getProgramStatus() != null) {
        violation = "Program must be defined when program status is defined";
      }

      if (params.getFollowUp() != null) {
        violation = "Program must be defined when follow up status is defined";
      }

      if (params.getEnrollmentEnrolledAfter() != null) {
        violation = "Program must be defined when program enrollment start date is specified";
      }

      if (params.getEnrollmentEnrolledBefore() != null) {
        violation = "Program must be defined when program enrollment end date is specified";
      }

      if (params.getEnrollmentOccurredAfter() != null) {
        violation = "Program must be defined when program incident start date is specified";
      }

      if (params.getEnrollmentOccurredBefore() != null) {
        violation = "Program must be defined when program incident end date is specified";
      }
    }

    if (params.getEventStatus() != null
        && (params.getEventOccurredAfter() == null || params.getEventOccurredBefore() == null)) {
      violation = "Event start and end date must be specified when event status is specified";
    }

    if (params.getUpdatedWithin() != null
        && (params.getUpdatedAfter() != null || params.getUpdatedBefore() != null)) {
      violation =
          "Last updated from and/or to and last updated duration cannot be specified simultaneously";
    }

    if (params.getUpdatedWithin() != null
        && DateUtils.getDuration(params.getUpdatedWithin()) == null) {
      violation = "Duration is not valid: " + params.getUpdatedWithin();
    }

    if (violation != null) {
      throw new BadRequestException(violation);
    }
  }

  private void validateRemovedParameters(RequestParams requestParams) throws BadRequestException {
    if (StringUtils.isNotBlank(requestParams.getQuery())) {
      throw new BadRequestException("`query` parameter was removed in v41. Use `filter` instead.");
    }
    if (StringUtils.isNotBlank(requestParams.getAttribute())) {
      throw new BadRequestException(
          "`attribute` parameter was removed in v41. Use `filter` instead.");
    }
    if (StringUtils.isNotBlank(requestParams.getIncludeAllAttributes())) {
      throw new BadRequestException("`includeAllAttributes` parameter was removed in v41.");
    }
  }

  private void mapOrderParam(
      TrackedEntityOperationParamsBuilder builder, List<OrderCriteria> orders) {
    if (orders == null || orders.isEmpty()) {
      return;
    }

    for (OrderCriteria order : orders) {
      if (TrackedEntityMapper.ORDERABLE_FIELDS.containsKey(order.getField())) {
        builder.orderBy(
            TrackedEntityMapper.ORDERABLE_FIELDS.get(order.getField()), order.getDirection());
      } else {
        builder.orderBy(UID.of(order.getField()), order.getDirection());
      }
    }
  }
}
