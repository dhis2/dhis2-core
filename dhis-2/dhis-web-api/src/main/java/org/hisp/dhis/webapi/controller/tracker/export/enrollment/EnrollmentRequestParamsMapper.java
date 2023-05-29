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
import org.hisp.dhis.webapi.common.UID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps query parameters from {@link EnrollmentsExportController} stored in
 * {@link RequestParams} to {@link EnrollmentQueryParams} which is used to fetch
 * enrollments from the DB.
 */
@Component
@RequiredArgsConstructor
class EnrollmentRequestParamsMapper
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
        Program program = validateProgram( requestParams.getProgram() );
        TrackedEntityType trackedEntityType = validateTrackedEntityType( requestParams.getTrackedEntityType() );
        TrackedEntity trackedEntity = validateTrackedEntity( requestParams.getTrackedEntity() );

        User user = currentUserService.getCurrentUser();
        Set<UID> orgUnitUids = validateDeprecatedUidsParameter( "orgUnit", requestParams.getOrgUnit(), "orgUnits",
            requestParams.getOrgUnits() );
        Set<OrganisationUnit> orgUnits = validateOrgUnits( user, orgUnitUids, program );

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

    private Program validateProgram( UID uid )
        throws BadRequestException
    {
        if ( uid == null )
        {
            return null;
        }

        Program program = programService.getProgram( uid.getValue() );
        if ( program == null )
        {
            throw new BadRequestException( "Program is specified but does not exist: " + uid );
        }

        return program;
    }

    private TrackedEntityType validateTrackedEntityType( UID uid )
        throws BadRequestException
    {
        if ( uid == null )
        {
            return null;
        }

        TrackedEntityType trackedEntityType = trackedEntityTypeService.getTrackedEntityType( uid.getValue() );
        if ( trackedEntityType == null )
        {
            throw new BadRequestException( "Tracked entity type is specified but does not exist: " + uid );
        }

        return trackedEntityType;
    }

    private TrackedEntity validateTrackedEntity( UID uid )
        throws BadRequestException
    {
        if ( uid == null )
        {
            return null;
        }

        TrackedEntity trackedEntity = trackedEntityService.getTrackedEntity( uid.getValue() );
        if ( trackedEntity == null )
        {
            throw new BadRequestException( "Tracked entity is specified but does not exist: " + uid );
        }

        return trackedEntity;
    }

    private Set<OrganisationUnit> validateOrgUnits( User user, Set<UID> orgUnitUids, Program program )
        throws BadRequestException,
        ForbiddenException
    {
        Set<OrganisationUnit> orgUnits = new HashSet<>();
        if ( orgUnitUids != null )
        {
            for ( UID orgUnitUid : orgUnitUids )
            {
                OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit(
                    orgUnitUid.getValue() );

                if ( orgUnit == null )
                {
                    throw new BadRequestException( "Organisation unit does not exist: " + orgUnitUid.getValue() );
                }

                if ( !trackerAccessManager.canAccess( user, program, orgUnit ) )
                {
                    throw new ForbiddenException(
                        "User does not have access to organisation unit: " + orgUnit.getUid() );
                }
                orgUnits.add( orgUnit );
            }
        }

        return orgUnits;
    }
}
