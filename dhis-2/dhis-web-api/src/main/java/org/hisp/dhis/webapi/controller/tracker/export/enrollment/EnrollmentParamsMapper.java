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
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hisp.dhis.webapi.controller.event.mapper.OrderParamsHelper.toOrderParams;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.applyIfNonEmpty;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.parseUids;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.EnrollmentQueryParams;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps query parameters from {@link EnrollmentsExportController} stored in
 * {@link RequestParams} to {@link EnrollmentQueryParams} which is used to fetch
 * enrollments from the DB.
 */
@Component
@RequiredArgsConstructor
public class EnrollmentParamsMapper
{

    @Nonnull
    private final CurrentUserService currentUserService;

    @Nonnull
    private final OrganisationUnitService organisationUnitService;

    @Nonnull
    private final ProgramService programService;

    @Nonnull
    private final TrackedEntityTypeService trackedEntityTypeService;

    @Nonnull
    private final TrackedEntityService trackedEntityService;

    @Nonnull
    private final TrackerAccessManager trackerAccessManager;

    @Transactional( readOnly = true )
    public EnrollmentQueryParams map( RequestParams requestParams )
        throws BadRequestException,
        ForbiddenException
    {
        Program program = applyIfNonEmpty( programService::getProgram, requestParams.getProgram() );
        validateProgram( requestParams.getProgram(), program );

        TrackedEntityType trackedEntityType = applyIfNonEmpty( trackedEntityTypeService::getTrackedEntityType,
            requestParams.getTrackedEntityType() );
        validateTrackedEntityType( requestParams.getTrackedEntityType(), trackedEntityType );

        TrackedEntity trackedEntity = applyIfNonEmpty( trackedEntityService::getTrackedEntity,
            requestParams.getTrackedEntity() );
        validateTrackedEntity( requestParams.getTrackedEntity(), trackedEntity );

        User user = currentUserService.getCurrentUser();
        Set<String> orgUnitIds = parseUids( requestParams.getOrgUnit() );
        Set<OrganisationUnit> orgUnits = validateOrgUnits( user, orgUnitIds, program );

        EnrollmentQueryParams params = new EnrollmentQueryParams();
        params.setProgram( program );
        params.setProgramStatus( requestParams.getProgramStatus() );
        params.setFollowUp( requestParams.getFollowUp() );
        params.setLastUpdated( requestParams.getUpdatedAfter() );
        params.setLastUpdatedDuration( requestParams.getUpdatedWithin() );
        params.setProgramStartDate( requestParams.getEnrolledAfter() );
        params.setProgramEndDate( requestParams.getEnrolledBefore() );
        params.setTrackedEntityType( trackedEntityType );
        params.setTrackedEntityUid(
            Optional.ofNullable( trackedEntity ).map( IdentifiableObject::getUid ).orElse( null ) );
        params.addOrganisationUnits( orgUnits );
        params.setOrganisationUnitMode( requestParams.getOuMode() );
        params.setPage( requestParams.getPage() );
        params.setPageSize( requestParams.getPageSize() );
        params.setTotalPages( requestParams.isTotalPages() );
        params.setSkipPaging( toBooleanDefaultIfNull( requestParams.isSkipPaging(), false ) );
        params.setIncludeDeleted( requestParams.isIncludeDeleted() );
        params.setUser( user );
        params.setOrder( toOrderParams( requestParams.getOrder() ) );

        return params;
    }

    private static void validateProgram( String id, Program program )
        throws BadRequestException
    {
        if ( isNotEmpty( id ) && program == null )
        {
            throw new BadRequestException( "Program is specified but does not exist: " + id );
        }
    }

    private void validateTrackedEntityType( String id, TrackedEntityType trackedEntityType )
        throws BadRequestException
    {
        if ( isNotEmpty( id ) && trackedEntityType == null )
        {
            throw new BadRequestException( "Tracked entity type is specified but does not exist: " + id );
        }
    }

    private void validateTrackedEntity( String id, TrackedEntity trackedEntity )
        throws BadRequestException
    {
        if ( isNotEmpty( id ) && trackedEntity == null )
        {
            throw new BadRequestException( "Tracked entity instance is specified but does not exist: " + id );
        }
    }

    private Set<OrganisationUnit> validateOrgUnits( User user, Set<String> orgUnitIds, Program program )
        throws BadRequestException,
        ForbiddenException
    {
        Set<OrganisationUnit> orgUnits = new HashSet<>();
        if ( orgUnitIds != null )
        {
            for ( String orgUnitId : orgUnitIds )
            {
                OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( orgUnitId );

                if ( organisationUnit == null )
                {
                    throw new BadRequestException( "Organisation unit does not exist: " + orgUnitId );
                }

                if ( !trackerAccessManager.canAccess( user, program, organisationUnit ) )
                {
                    throw new ForbiddenException(
                        "User does not have access to organisation unit: " + organisationUnit.getUid() );
                }
                orgUnits.add( organisationUnit );
            }
        }

        return orgUnits;
    }
}
