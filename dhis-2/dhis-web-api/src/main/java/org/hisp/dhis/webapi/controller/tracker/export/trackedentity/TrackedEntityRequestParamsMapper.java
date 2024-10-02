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

import static org.hisp.dhis.util.ObjectUtils.applyIfNotNull;
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
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams.TrackedEntityOperationParamsBuilder;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.hisp.dhis.webapi.webdomain.EndDateTime;
import org.hisp.dhis.webapi.webdomain.StartDateTime;
import org.springframework.stereotype.Component;

/**
 * Maps operation parameters from {@link TrackedEntitiesExportController} stored in {@link
 * TrackedEntityRequestParams} to {@link TrackedEntityOperationParams} which is used to fetch
 * tracked entities.
 */
@Component
@RequiredArgsConstructor
class TrackedEntityRequestParamsMapper {
  private static final Set<String> ORDERABLE_FIELD_NAMES =
      TrackedEntityMapper.ORDERABLE_FIELDS.keySet();

  private final TrackedEntityFieldsParamMapper fieldsParamMapper;

  public TrackedEntityOperationParams map(
      TrackedEntityRequestParams trackedEntityRequestParams, UserDetails user)
      throws BadRequestException {
    return map(trackedEntityRequestParams, trackedEntityRequestParams.getFields(), user);
  }

  public TrackedEntityOperationParams map(
      TrackedEntityRequestParams trackedEntityRequestParams,
      List<FieldPath> fields,
      UserDetails user)
      throws BadRequestException {
    validateRemovedParameters(trackedEntityRequestParams);

    Set<UID> assignedUsers =
        validateDeprecatedUidsParameter(
            "assignedUser",
            trackedEntityRequestParams.getAssignedUser(),
            "assignedUsers",
            trackedEntityRequestParams.getAssignedUsers());

    Set<UID> orgUnitUids =
        validateDeprecatedUidsParameter(
            "orgUnit",
            trackedEntityRequestParams.getOrgUnit(),
            "orgUnits",
            trackedEntityRequestParams.getOrgUnits());

    OrganisationUnitSelectionMode orgUnitMode =
        validateDeprecatedParameter(
            "ouMode",
            trackedEntityRequestParams.getOuMode(),
            "orgUnitMode",
            trackedEntityRequestParams.getOrgUnitMode());

    orgUnitMode =
        validateOrgUnitModeForTrackedEntities(
            orgUnitUids, orgUnitMode, trackedEntityRequestParams.getTrackedEntities());

    EnrollmentStatus enrollmentStatus =
        validateDeprecatedParameter(
            "programStatus",
            trackedEntityRequestParams.getProgramStatus(),
            "enrollmentStatus",
            trackedEntityRequestParams.getEnrollmentStatus());

    Set<UID> trackedEntities =
        validateDeprecatedUidsParameter(
            "trackedEntity",
            trackedEntityRequestParams.getTrackedEntity(),
            "trackedEntities",
            trackedEntityRequestParams.getTrackedEntities());
    validateOrderParams(trackedEntityRequestParams.getOrder(), ORDERABLE_FIELD_NAMES, "attribute");
    validateRequestParams(trackedEntityRequestParams, trackedEntities);

    Map<String, List<QueryFilter>> filters = parseFilters(trackedEntityRequestParams.getFilter());

    TrackedEntityOperationParamsBuilder builder =
        TrackedEntityOperationParams.builder()
            .programUid(applyIfNotNull(trackedEntityRequestParams.getProgram(), UID::getValue))
            .programStageUid(
                applyIfNotNull(trackedEntityRequestParams.getProgramStage(), UID::getValue))
            .enrollmentStatus(enrollmentStatus)
            .followUp(trackedEntityRequestParams.getFollowUp())
            .lastUpdatedStartDate(
                applyIfNotNull(trackedEntityRequestParams.getUpdatedAfter(), StartDateTime::toDate))
            .lastUpdatedEndDate(
                applyIfNotNull(trackedEntityRequestParams.getUpdatedBefore(), EndDateTime::toDate))
            .lastUpdatedDuration(trackedEntityRequestParams.getUpdatedWithin())
            .programEnrollmentStartDate(
                applyIfNotNull(
                    trackedEntityRequestParams.getEnrollmentEnrolledAfter(), StartDateTime::toDate))
            .programEnrollmentEndDate(
                applyIfNotNull(
                    trackedEntityRequestParams.getEnrollmentEnrolledBefore(), EndDateTime::toDate))
            .programIncidentStartDate(
                applyIfNotNull(
                    trackedEntityRequestParams.getEnrollmentOccurredAfter(), StartDateTime::toDate))
            .programIncidentEndDate(
                applyIfNotNull(
                    trackedEntityRequestParams.getEnrollmentOccurredBefore(), EndDateTime::toDate))
            .trackedEntityTypeUid(
                applyIfNotNull(trackedEntityRequestParams.getTrackedEntityType(), UID::getValue))
            .organisationUnits(UID.toValueSet(orgUnitUids))
            .orgUnitMode(orgUnitMode)
            .eventStatus(trackedEntityRequestParams.getEventStatus())
            .eventStartDate(
                applyIfNotNull(
                    trackedEntityRequestParams.getEventOccurredAfter(), StartDateTime::toDate))
            .eventEndDate(
                applyIfNotNull(
                    trackedEntityRequestParams.getEventOccurredBefore(), EndDateTime::toDate))
            .assignedUserQueryParam(
                new AssignedUserQueryParam(
                    trackedEntityRequestParams.getAssignedUserMode(),
                    UID.toValueSet(assignedUsers),
                    user.getUid()))
            .trackedEntityUids(UID.toValueSet(trackedEntities))
            .filters(filters)
            .includeDeleted(trackedEntityRequestParams.isIncludeDeleted())
            .potentialDuplicate(trackedEntityRequestParams.getPotentialDuplicate())
            .trackedEntityParams(fieldsParamMapper.map(fields));

    mapOrderParam(builder, trackedEntityRequestParams.getOrder());

    return builder.build();
  }

  private void validateRequestParams(TrackedEntityRequestParams params, Set<UID> trackedEntities)
      throws BadRequestException {

    if (params.getProgram() != null && params.getTrackedEntityType() != null) {
      throw new BadRequestException(
          "`program` and `trackedEntityType` cannot be specified simultaneously");
    }

    if (params.getProgram() == null) {
      if (trackedEntities.isEmpty() && params.getTrackedEntityType() == null) {
        throw new BadRequestException(
            "Either `program`, `trackedEntityType` or `trackedEntities` should be specified");
      }

      if (params.getProgramStatus() != null) {
        throw new BadRequestException("`program` must be defined when `programStatus` is defined");
      }
      if (params.getEnrollmentStatus() != null) {
        throw new BadRequestException(
            "`program` must be defined when `enrollmentStatus` is defined");
      }

      if (params.getFollowUp() != null) {
        throw new BadRequestException("`program` must be defined when `followUp` is defined");
      }

      if (params.getEnrollmentEnrolledAfter() != null) {
        throw new BadRequestException(
            "`program` must be defined when `enrollmentEnrolledAfter` is specified");
      }

      if (params.getEnrollmentEnrolledBefore() != null) {
        throw new BadRequestException(
            "`program` must be defined when `enrollmentEnrolledBefore` is specified");
      }

      if (params.getEnrollmentOccurredAfter() != null) {
        throw new BadRequestException(
            "`program` must be defined when `enrollmentOccurredAfter` is specified");
      }

      if (params.getEnrollmentOccurredBefore() != null) {
        throw new BadRequestException(
            "`program` must be defined when `enrollmentOccurredBefore` is specified");
      }
    }

    if (ObjectUtils.firstNonNull(
                params.getEventStatus(),
                params.getEventOccurredAfter(),
                params.getEventOccurredBefore())
            != null
        && !ObjectUtils.allNonNull(
            params.getEventStatus(),
            params.getEventOccurredAfter(),
            params.getEventOccurredBefore())) {
      throw new BadRequestException(
          "`eventOccurredAfter`, `eventOccurredBefore` and `eventStatus` must be specified together");
    }

    if (params.getUpdatedWithin() != null
        && (params.getUpdatedAfter() != null || params.getUpdatedBefore() != null)) {
      throw new BadRequestException(
          "`updatedAfter` or `updatedBefore` and `updatedWithin` cannot be specified simultaneously");
    }

    if (params.getUpdatedWithin() != null
        && DateUtils.getDuration(params.getUpdatedWithin()) == null) {
      throw new BadRequestException("`updatedWithin` is not valid: " + params.getUpdatedWithin());
    }
  }

  private void validateRemovedParameters(TrackedEntityRequestParams trackedEntityRequestParams)
      throws BadRequestException {
    if (StringUtils.isNotBlank(trackedEntityRequestParams.getQuery())) {
      throw new BadRequestException("`query` parameter was removed in v41. Use `filter` instead.");
    }
    if (StringUtils.isNotBlank(trackedEntityRequestParams.getAttribute())) {
      throw new BadRequestException(
          "`attribute` parameter was removed in v41. Use `filter` instead.");
    }
    if (StringUtils.isNotBlank(trackedEntityRequestParams.getIncludeAllAttributes())) {
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
