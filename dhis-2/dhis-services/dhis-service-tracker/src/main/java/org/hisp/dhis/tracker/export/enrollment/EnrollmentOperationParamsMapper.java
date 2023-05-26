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
package org.hisp.dhis.tracker.export.enrollment;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;

import java.util.HashSet;
import java.util.Set;

import lombok.RequiredArgsConstructor;

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
 * Maps operation parameters stored in {@link EnrollmentOperationParams} to
 * {@link EnrollmentQueryParams} which is used to fetch enrollments from the DB.
 */
@Component
@RequiredArgsConstructor
class EnrollmentOperationParamsMapper
{
    private final CurrentUserService currentUserService;

    private final OrganisationUnitService organisationUnitService;

    private final ProgramService programService;

    private final TrackedEntityTypeService trackedEntityTypeService;

    private final TrackedEntityService trackedEntityService;

    private final TrackerAccessManager trackerAccessManager;

    @Transactional( readOnly = true )
    public EnrollmentQueryParams map( EnrollmentOperationParams operationParams )
        throws BadRequestException,
        ForbiddenException
    {
        Program program = validateProgram( operationParams.getProgramUid() );
        TrackedEntityType trackedEntityType = validateTrackedEntityType( operationParams.getTrackedEntityTypeUid() );
        TrackedEntity trackedEntity = validateTrackedEntity( operationParams.getTrackedEntityUid() );

        User user = currentUserService.getCurrentUser();
        Set<OrganisationUnit> orgUnits = validateOrgUnits( user, operationParams.getOrganisationUnitUids(), program );

        EnrollmentQueryParams params = new EnrollmentQueryParams();
        params.setProgram( program );
        params.setProgramStatus( operationParams.getProgramStatus() );
        params.setFollowUp( operationParams.getFollowUp() );
        params.setLastUpdated( operationParams.getLastUpdated() );
        params.setLastUpdatedDuration( operationParams.getLastUpdatedDuration() );
        params.setProgramStartDate( operationParams.getProgramStartDate() );
        params.setProgramEndDate( operationParams.getProgramEndDate() );
        params.setTrackedEntityType( trackedEntityType );
        params.setTrackedEntity( trackedEntity );
        params.addOrganisationUnits( orgUnits );
        params.setOrganisationUnitMode( operationParams.getOrganisationUnitMode() );
        params.setPage( operationParams.getPage() );
        params.setPageSize( operationParams.getPageSize() );
        params.setTotalPages( operationParams.isTotalPages() );
        params.setSkipPaging( toBooleanDefaultIfNull( operationParams.isSkipPaging(), false ) );
        params.setIncludeDeleted( operationParams.isIncludeDeleted() );
        params.setUser( user );
        params.setOrder( operationParams.getOrder() );

        return params;
    }

    private Program validateProgram( String uid )
        throws BadRequestException
    {
        if ( uid == null )
        {
            return null;
        }

        Program program = programService.getProgram( uid );
        if ( program == null )
        {
            throw new BadRequestException( "Program is specified but does not exist: " + uid );
        }

        return program;
    }

    private TrackedEntityType validateTrackedEntityType( String uid )
        throws BadRequestException
    {
        if ( uid == null )
        {
            return null;
        }

        TrackedEntityType trackedEntityType = trackedEntityTypeService.getTrackedEntityType( uid );
        if ( trackedEntityType == null )
        {
            throw new BadRequestException( "Tracked entity type is specified but does not exist: " + uid );
        }

        return trackedEntityType;
    }

    private TrackedEntity validateTrackedEntity( String uid )
        throws BadRequestException
    {
        if ( uid == null )
        {
            return null;
        }

        TrackedEntity trackedEntity = trackedEntityService.getTrackedEntity( uid );
        if ( trackedEntity == null )
        {
            throw new BadRequestException( "Tracked entity is specified but does not exist: " + uid );
        }

        return trackedEntity;
    }

    private Set<OrganisationUnit> validateOrgUnits( User user, Set<String> orgUnitUids, Program program )
        throws BadRequestException,
        ForbiddenException
    {
        Set<OrganisationUnit> orgUnits = new HashSet<>();
        if ( orgUnitUids != null )
        {
            for ( String orgUnitUid : orgUnitUids )
            {
                OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit(
                    orgUnitUid );

                if ( orgUnit == null )
                {
                    throw new BadRequestException( "Organisation unit does not exist: " + orgUnitUid );
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
