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
package org.hisp.dhis.webapi.controller.tracker.export.enrollment;

import static org.hisp.dhis.util.ObjectUtils.applyIfNotNull;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validateDeprecatedParameter;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validateDeprecatedUidsParameter;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validateOrderParams;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validateOrgUnitModeForEnrollmentsAndEvents;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams.EnrollmentOperationParamsBuilder;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.hisp.dhis.webapi.webdomain.EndDateTime;
import org.hisp.dhis.webapi.webdomain.StartDateTime;
import org.springframework.stereotype.Component;

/**
 * Maps operation parameters from {@link EnrollmentsExportController} stored in {@link
 * EnrollmentRequestParams} to {@link EnrollmentOperationParams} which is used to fetch enrollments
 * from the service.
 */
@Component
@RequiredArgsConstructor
class EnrollmentRequestParamsMapper {
  private static final Set<String> ORDERABLE_FIELD_NAMES =
      EnrollmentMapper.ORDERABLE_FIELDS.keySet();

  private final EnrollmentFieldsParamMapper fieldsParamMapper;

  public EnrollmentOperationParams map(EnrollmentRequestParams enrollmentRequestParams)
      throws BadRequestException {
    Set<UID> orgUnits =
        validateDeprecatedUidsParameter(
            "orgUnit",
            enrollmentRequestParams.getOrgUnit(),
            "orgUnits",
            enrollmentRequestParams.getOrgUnits());

    OrganisationUnitSelectionMode orgUnitMode =
        validateDeprecatedParameter(
            "ouMode",
            enrollmentRequestParams.getOuMode(),
            "orgUnitMode",
            enrollmentRequestParams.getOrgUnitMode());

    orgUnitMode = validateOrgUnitModeForEnrollmentsAndEvents(orgUnits, orgUnitMode);

    EnrollmentStatus enrollmentStatus =
        validateDeprecatedParameter(
            "programStatus",
            enrollmentRequestParams.getProgramStatus(),
            "status",
            enrollmentRequestParams.getStatus());

    validateOrderParams(enrollmentRequestParams.getOrder(), ORDERABLE_FIELD_NAMES);
    validateRequestParams(enrollmentRequestParams);

    Set<UID> enrollmentUids =
        validateDeprecatedUidsParameter(
            "enrollment",
            enrollmentRequestParams.getEnrollment(),
            "enrollments",
            enrollmentRequestParams.getEnrollments());

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
            .orgUnits(orgUnits)
            .orgUnitMode(orgUnitMode)
            .includeDeleted(enrollmentRequestParams.isIncludeDeleted())
            .enrollments(enrollmentUids)
            .enrollmentParams(fieldsParamMapper.map(enrollmentRequestParams.getFields()));

    mapOrderParam(builder, enrollmentRequestParams.getOrder());

    return builder.build();
  }

  private void mapOrderParam(EnrollmentOperationParamsBuilder builder, List<OrderCriteria> orders) {
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

  private void validateRequestParams(EnrollmentRequestParams params) throws BadRequestException {
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
