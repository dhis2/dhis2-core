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

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.hisp.dhis.webapi.controller.event.mapper.OrderParamsHelper.toOrderParams;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.validateDeprecatedUidsParameter;

import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams;
import org.hisp.dhis.webapi.common.UID;
import org.springframework.stereotype.Component;

/**
 * Maps operation parameters from {@link EnrollmentsExportController} stored in
 * {@link RequestParams} to {@link EnrollmentOperationParams} which is used to
 * fetch enrollments from the service.
 */
@Component
@RequiredArgsConstructor
class EnrollmentRequestParamsMapper
{
    private final EnrollmentFieldsParamMapper fieldsParamMapper;

    public EnrollmentOperationParams map( RequestParams requestParams )
        throws BadRequestException
    {
        Set<UID> orgUnits = validateDeprecatedUidsParameter( "orgUnit", requestParams.getOrgUnit(), "orgUnits",
            requestParams.getOrgUnits() );

        return EnrollmentOperationParams.builder()
            .programUid( requestParams.getProgram() != null ? requestParams.getProgram().getValue() : null )
            .programStatus( requestParams.getProgramStatus() )
            .followUp( requestParams.getFollowUp() )
            .lastUpdated( requestParams.getUpdatedAfter() )
            .lastUpdatedDuration( requestParams.getUpdatedWithin() )
            .programStartDate( requestParams.getEnrolledAfter() )
            .programEndDate( requestParams.getEnrolledBefore() )
            .trackedEntityTypeUid(
                requestParams.getTrackedEntityType() != null ? requestParams.getTrackedEntityType().getValue() : null )
            .trackedEntityUid(
                requestParams.getTrackedEntity() != null ? requestParams.getTrackedEntity().getValue() : null )
            .orgUnitUids( UID.toValueSet( orgUnits ) )
            .orgUnitMode( requestParams.getOuMode() )
            .page( requestParams.getPage() )
            .pageSize( requestParams.getPageSize() )
            .totalPages( requestParams.isTotalPages() )
            .skipPaging( toBooleanDefaultIfNull( requestParams.isSkipPaging(), false ) )
            .includeDeleted( requestParams.isIncludeDeleted() )
            .order( toOrderParams( requestParams.getOrder() ) )
            .enrollmentParams( fieldsParamMapper.map( requestParams.getFields() ) )
            .build();
    }
}
