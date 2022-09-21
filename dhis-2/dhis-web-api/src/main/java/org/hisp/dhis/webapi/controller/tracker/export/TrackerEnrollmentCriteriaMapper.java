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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.webapi.controller.event.mapper.OrderParamsHelper.toOrderParams;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstanceQueryParams;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps query parameters from {@link TrackerEnrollmentsExportController} stored
 * in {@link TrackerEnrollmentCriteria} to {@link ProgramInstanceQueryParams}
 * which is used to fetch enrollments from the DB.
 */
@Service( "org.hisp.dhis.webapi.controller.tracker.export.TrackerEnrollmentCriteriaMapper" )
@RequiredArgsConstructor
public class TrackerEnrollmentCriteriaMapper
{

    private final CurrentUserService currentUserService;

    private final OrganisationUnitService organisationUnitService;

    private final ProgramService programService;

    private final TrackedEntityTypeService trackedEntityTypeService;

    private final TrackedEntityInstanceService trackedEntityInstanceService;

    @Transactional( readOnly = true )
    public ProgramInstanceQueryParams getFromUrl( TrackerEnrollmentCriteria trackerEnrollmentCriteria )
    {
        Set<String> ou = TextUtils.splitToSet( trackerEnrollmentCriteria.getOrgUnit(), TextUtils.SEMICOLON );
        String program = trackerEnrollmentCriteria.getProgram();
        String trackedEntityType = trackerEnrollmentCriteria.getTrackedEntityType();
        String trackedEntityInstance = trackerEnrollmentCriteria.getTrackedEntity();
        ProgramInstanceQueryParams params = new ProgramInstanceQueryParams();

        Set<OrganisationUnit> possibleSearchOrgUnits = new HashSet<>();

        User user = currentUserService.getCurrentUser();

        if ( user != null )
        {
            possibleSearchOrgUnits = user.getTeiSearchOrganisationUnitsWithFallback();
        }

        if ( ou != null )
        {
            for ( String orgUnit : ou )
            {
                OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( orgUnit );

                if ( organisationUnit == null )
                {
                    throw new IllegalQueryException( "Organisation unit does not exist: " + orgUnit );
                }

                if ( !organisationUnitService.isInUserHierarchy( organisationUnit.getUid(), possibleSearchOrgUnits ) )
                {
                    throw new IllegalQueryException( "Organisation unit is not part of the search scope: " + orgUnit );
                }

                params.getOrganisationUnits().add( organisationUnit );
            }
        }

        Program pr = program != null ? programService.getProgram( program ) : null;

        if ( program != null && pr == null )
        {
            throw new IllegalQueryException( "Program does not exist: " + program );
        }

        TrackedEntityType te = trackedEntityType != null
            ? trackedEntityTypeService.getTrackedEntityType( trackedEntityType )
            : null;

        if ( trackedEntityType != null && te == null )
        {
            throw new IllegalQueryException( "Tracked entity does not exist: " + trackedEntityType );
        }

        TrackedEntityInstance tei = trackedEntityInstance != null
            ? trackedEntityInstanceService.getTrackedEntityInstance( trackedEntityInstance )
            : null;

        if ( trackedEntityInstance != null && tei == null )
        {
            throw new IllegalQueryException( "Tracked entity instance does not exist: " + trackedEntityInstance );
        }

        params.setProgram( pr );
        params.setProgramStatus( trackerEnrollmentCriteria.getProgramStatus() );
        params.setFollowUp( trackerEnrollmentCriteria.getFollowUp() );
        params.setLastUpdated( trackerEnrollmentCriteria.getUpdatedAfter() );
        params.setLastUpdatedDuration( trackerEnrollmentCriteria.getUpdatedWithin() );
        params.setProgramStartDate( trackerEnrollmentCriteria.getEnrolledAfter() );
        params.setProgramEndDate( trackerEnrollmentCriteria.getEnrolledBefore() );
        params.setTrackedEntityType( te );
        params.setTrackedEntityInstanceUid(
            Optional.ofNullable( tei ).map( BaseIdentifiableObject::getUid ).orElse( null ) );
        params.setOrganisationUnitMode( trackerEnrollmentCriteria.getOuMode() );
        params.setPage( trackerEnrollmentCriteria.getPage() );
        params.setPageSize( trackerEnrollmentCriteria.getPageSize() );
        params.setTotalPages( trackerEnrollmentCriteria.isTotalPages() );
        params.setSkipPaging( trackerEnrollmentCriteria.isSkipPaging() );
        params.setIncludeDeleted( trackerEnrollmentCriteria.isIncludeDeleted() );
        params.setUser( user );
        params.setOrder( toOrderParams( trackerEnrollmentCriteria.getOrder() ) );

        return params;
    }
}
