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
package org.hisp.dhis.webapi.controller.tracker.export.enrollment;

import static org.hisp.dhis.util.ObjectUtils.applyIfNotNull;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateDeprecatedParameter;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateOrderParams;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateOrgUnitModeForEnrollmentsAndEvents;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentFields;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams.EnrollmentOperationParamsBuilder;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.hisp.dhis.webapi.webdomain.EndDateTime;
import org.hisp.dhis.webapi.webdomain.StartDateTime;

/**
 * Maps operation parameters from {@link EnrollmentsExportController} stored in {@link
 * EnrollmentRequestParams} to {@link EnrollmentOperationParams} which is used to fetch enrollments
 * from the service.
 */
class EnrollmentRequestParamsMapper {
  private static final Set<String> ORDERABLE_FIELD_NAMES =
      EnrollmentMapper.ORDERABLE_FIELDS.keySet();

  private EnrollmentRequestParamsMapper() {
    throw new IllegalStateException("Utility class");
  }

  public static EnrollmentOperationParams map(EnrollmentRequestParams enrollmentRequestParams)
      throws BadRequestException {
    OrganisationUnitSelectionMode orgUnitMode =
        validateOrgUnitModeForEnrollmentsAndEvents(
            enrollmentRequestParams.getOrgUnits(), enrollmentRequestParams.getOrgUnitMode());

    EnrollmentStatus enrollmentStatus =
        validateDeprecatedParameter(
            "programStatus",
            enrollmentRequestParams.getProgramStatus(),
            "status",
            enrollmentRequestParams.getStatus());

    validateOrderParams(enrollmentRequestParams.getOrder(), ORDERABLE_FIELD_NAMES);
    validateRequestParams(enrollmentRequestParams);

    EnrollmentOperationParamsBuilder builder =
        EnrollmentOperationParams.builder()
            .program(enrollmentRequestParams.getProgram())
            .enrollmentStatus(enrollmentStatus)
            .followUp(enrollmentRequestParams.getFollowUp())
            .lastUpdated(
                applyIfNotNull(enrollmentRequestParams.getUpdatedAfter(), StartDateTime::toDate))
            .lastUpdatedDuration(enrollmentRequestParams.getUpdatedWithin())
            .programStartDate(
                applyIfNotNull(enrollmentRequestParams.getEnrolledAfter(), StartDateTime::toDate))
            .programEndDate(
                applyIfNotNull(enrollmentRequestParams.getEnrolledBefore(), EndDateTime::toDate))
            .trackedEntityType(enrollmentRequestParams.getTrackedEntityType())
            .trackedEntity(enrollmentRequestParams.getTrackedEntity())
            .orgUnits(enrollmentRequestParams.getOrgUnits())
            .orgUnitMode(orgUnitMode)
            .includeDeleted(enrollmentRequestParams.isIncludeDeleted())
            .enrollments(enrollmentRequestParams.getEnrollments())
            .fields(
                EnrollmentFields.of(
                    enrollmentRequestParams.getFields()::includes, FieldPath.FIELD_PATH_SEPARATOR));

    mapOrderParam(builder, enrollmentRequestParams.getOrder());

    return builder.build();
  }

  private static void mapOrderParam(
      EnrollmentOperationParamsBuilder builder, List<OrderCriteria> orders) {
    if (orders == null || orders.isEmpty()) {
      return;
    }

    for (OrderCriteria order : orders) {
      if (EnrollmentMapper.ORDERABLE_FIELDS.containsKey(order.getField())) {
        builder.orderBy(
            EnrollmentMapper.ORDERABLE_FIELDS.get(order.getField()), order.getDirection());
      }
    }
  }

  private static void validateRequestParams(EnrollmentRequestParams params)
      throws BadRequestException {
    if (params.getProgram() != null && params.getTrackedEntityType() != null) {
      throw new BadRequestException(
          "`program` and `trackedEntityType` cannot be specified simultaneously");
    }

    if (params.getProgram() == null) {
      if (params.getProgramStatus() != null) {
        throw new BadRequestException("`program` must be defined when `programStatus` is defined");
      }
      if (params.getStatus() != null) {
        throw new BadRequestException("`program` must be defined when `status` is defined");
      }

      if (params.getFollowUp() != null) {
        throw new BadRequestException(
            "`program` must be defined when `followUp` status is defined");
      }

      if (params.getEnrolledAfter() != null) {
        throw new BadRequestException(
            "`program` must be defined when `enrolledAfter` is specified");
      }

      if (params.getEnrolledBefore() != null) {
        throw new BadRequestException(
            "`program` must be defined when `enrolledBefore` is specified");
      }
    }

    if (params.getUpdatedWithin() != null && params.getUpdatedAfter() != null) {
      throw new BadRequestException(
          "`updatedAfter` and `updatedWithin` cannot be specified simultaneously");
    }

    if (params.getUpdatedWithin() != null
        && DateUtils.getDuration(params.getUpdatedWithin()) == null) {
      throw new BadRequestException("`updatedWithin` is not valid: " + params.getUpdatedWithin());
    }
  }
}
