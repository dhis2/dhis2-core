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

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.hisp.dhis.tracker.export.OperationParamUtils.parseQueryFilter;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.validateDeprecatedUidsParameter;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.AssignedUserQueryParam;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.common.UID;
import org.springframework.stereotype.Component;

/**
 * Maps operation parameters from {@link TrackedEntitiesExportController} stored in {@link
 * RequestParams} to {@link TrackedEntityOperationParams} which is used to fetch tracked entities.
 */
@Component
@RequiredArgsConstructor
class TrackedEntityRequestParamsMapper {

  private final TrackedEntityFieldsParamMapper fieldsParamMapper;

  public TrackedEntityOperationParams map(
      RequestParams requestParams, User user, List<FieldPath> fields) throws BadRequestException {
    Set<UID> assignedUsers =
        validateDeprecatedUidsParameter(
            "assignedUser",
            requestParams.getAssignedUser(),
            "assignedUsers",
            requestParams.getAssignedUsers());

    Set<UID> orgUnitUids =
        validateDeprecatedUidsParameter(
            "orgUnit", requestParams.getOrgUnit(), "orgUnits", requestParams.getOrgUnits());

    QueryFilter queryFilter = parseQueryFilter(requestParams.getQuery());

    Set<UID> trackedEntities =
        validateDeprecatedUidsParameter(
            "trackedEntity",
            requestParams.getTrackedEntity(),
            "trackedEntities",
            requestParams.getTrackedEntities());

    return TrackedEntityOperationParams.builder()
        .query(queryFilter)
        .programUid(UID.toValue(requestParams.getProgram()))
        .programStageUid(UID.toValue(requestParams.getProgramStage()))
        .programStatus(requestParams.getProgramStatus())
        .followUp(requestParams.getFollowUp())
        .lastUpdatedStartDate(requestParams.getUpdatedAfter())
        .lastUpdatedEndDate(requestParams.getUpdatedBefore())
        .lastUpdatedDuration(requestParams.getUpdatedWithin())
        .programEnrollmentStartDate(requestParams.getEnrollmentEnrolledAfter())
        .programEnrollmentEndDate(requestParams.getEnrollmentEnrolledBefore())
        .programIncidentStartDate(requestParams.getEnrollmentOccurredAfter())
        .programIncidentEndDate(requestParams.getEnrollmentOccurredBefore())
        .trackedEntityTypeUid(UID.toValue(requestParams.getTrackedEntityType()))
        .organisationUnits(UID.toValueSet(orgUnitUids))
        .organisationUnitMode(
            requestParams.getOuMode() == null
                ? OrganisationUnitSelectionMode.DESCENDANTS
                : requestParams.getOuMode())
        .eventStatus(requestParams.getEventStatus())
        .eventStartDate(requestParams.getEventOccurredAfter())
        .eventEndDate(requestParams.getEventOccurredBefore())
        .assignedUserQueryParam(
            new AssignedUserQueryParam(
                requestParams.getAssignedUserMode(), user, UID.toValueSet(assignedUsers)))
        .user(user)
        .trackedEntityUids(UID.toValueSet(trackedEntities))
        .attributes(requestParams.getAttribute())
        .filters(requestParams.getFilter())
        .skipMeta(requestParams.isSkipMeta())
        .page(requestParams.getPage())
        .pageSize(requestParams.getPageSize())
        .totalPages(requestParams.isTotalPages())
        .skipPaging(toBooleanDefaultIfNull(requestParams.isSkipPaging(), false))
        .includeDeleted(requestParams.isIncludeDeleted())
        .includeAllAttributes(requestParams.isIncludeAllAttributes())
        .potentialDuplicate(requestParams.getPotentialDuplicate())
        .orders(requestParams.getOrder())
        .trackedEntityParams(fieldsParamMapper.map(fields))
        .build();
  }
}
