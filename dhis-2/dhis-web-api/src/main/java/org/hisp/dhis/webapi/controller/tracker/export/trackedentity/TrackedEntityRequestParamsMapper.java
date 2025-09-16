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
package org.hisp.dhis.webapi.controller.tracker.export.trackedentity;

import static org.hisp.dhis.util.ObjectUtils.applyIfNotNull;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateDeprecatedParameter;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateOrderParams;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateOrgUnitModeForTrackedEntities;
import static org.hisp.dhis.webapi.controller.tracker.export.FilterParser.parseFilters;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.hisp.dhis.common.AssignedUserQueryParam;
import org.hisp.dhis.common.OrderCriteria;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.tracker.export.fieldfiltering.Fields;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityFields;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams.TrackedEntityOperationParamsBuilder;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.hisp.dhis.webapi.webdomain.EndDateTime;
import org.hisp.dhis.webapi.webdomain.StartDateTime;

/**
 * Maps operation parameters from {@link TrackedEntitiesExportController} stored in {@link
 * TrackedEntityRequestParams} to {@link TrackedEntityOperationParams} which is used to fetch
 * tracked entities.
 */
class TrackedEntityRequestParamsMapper {
  private static final Set<String> ORDERABLE_FIELD_NAMES =
      TrackedEntityMapper.ORDERABLE_FIELDS.keySet();

  private TrackedEntityRequestParamsMapper() {
    throw new IllegalStateException("Utility class");
  }

  public static TrackedEntityOperationParams map(
      TrackedEntityRequestParams trackedEntityRequestParams, UserDetails user)
      throws BadRequestException {
    return mapCommon(
        trackedEntityRequestParams, trackedEntityRequestParams.getFields()::includes, user);
  }

  public static TrackedEntityOperationParams map(
      TrackedEntityRequestParams trackedEntityRequestParams, Fields fields, UserDetails user)
      throws BadRequestException {
    return mapCommon(trackedEntityRequestParams, fields::includes, user);
  }

  private static TrackedEntityOperationParams mapCommon(
      TrackedEntityRequestParams trackedEntityRequestParams,
      java.util.function.Predicate<String> fieldFilter,
      UserDetails user)
      throws BadRequestException {
    OrganisationUnitSelectionMode orgUnitMode =
        validateOrgUnitModeForTrackedEntities(
            trackedEntityRequestParams.getOrgUnits(),
            trackedEntityRequestParams.getOrgUnitMode(),
            trackedEntityRequestParams.getTrackedEntities());

    EnrollmentStatus enrollmentStatus =
        validateDeprecatedParameter(
            "programStatus",
            trackedEntityRequestParams.getProgramStatus(),
            "enrollmentStatus",
            trackedEntityRequestParams.getEnrollmentStatus());

    validateOrderParams(trackedEntityRequestParams.getOrder(), ORDERABLE_FIELD_NAMES, "attribute");
    validateRequestParams(
        trackedEntityRequestParams, trackedEntityRequestParams.getTrackedEntities());

    Map<UID, List<QueryFilter>> filters =
        parseFilters("filter", trackedEntityRequestParams.getFilter());

    TrackedEntityOperationParamsBuilder builder =
        TrackedEntityOperationParams.builder()
            .program(trackedEntityRequestParams.getProgram())
            .programStage(trackedEntityRequestParams.getProgramStage())
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
            .trackedEntityType(trackedEntityRequestParams.getTrackedEntityType())
            .organisationUnits(trackedEntityRequestParams.getOrgUnits())
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
                    trackedEntityRequestParams.getAssignedUsers(),
                    UID.of(user)))
            .trackedEntities(trackedEntityRequestParams.getTrackedEntities())
            .includeDeleted(trackedEntityRequestParams.isIncludeDeleted())
            .potentialDuplicate(trackedEntityRequestParams.getPotentialDuplicate())
            .fields(TrackedEntityFields.of(fieldFilter, FieldPath.FIELD_PATH_SEPARATOR));

    mapOrderParam(builder, trackedEntityRequestParams.getOrder());
    mapFilterParam(builder, filters);

    return builder.build();
  }

  private static void validateRequestParams(
      TrackedEntityRequestParams params, Set<UID> trackedEntities) throws BadRequestException {

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

  private static void mapOrderParam(
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

  private static void mapFilterParam(
      TrackedEntityOperationParamsBuilder builder, Map<UID, List<QueryFilter>> filters) {
    if (filters == null || filters.isEmpty()) {
      return;
    }

    filters.replaceAll(
        (uid, f) ->
            f.stream()
                .map(qf -> new QueryFilter(qf.getOperator().stripCaseVariant(), qf.getFilter()))
                .toList());

    for (Entry<UID, List<QueryFilter>> entry : filters.entrySet()) {
      if (entry.getValue().isEmpty()) {
        builder.filterBy(entry.getKey());
      } else {
        builder.filterBy(entry.getKey(), entry.getValue());
      }
    }
  }
}
