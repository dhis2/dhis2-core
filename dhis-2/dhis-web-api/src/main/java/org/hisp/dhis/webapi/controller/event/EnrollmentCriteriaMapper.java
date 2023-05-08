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
package org.hisp.dhis.webapi.controller.event;

import static org.hisp.dhis.webapi.controller.event.mapper.OrderParamsHelper.toOrderParams;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.EnrollmentQueryParams;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service( "org.hisp.dhis.webapi.controller.event.EnrollmentCriteriaMapper" )
@RequiredArgsConstructor
public class EnrollmentCriteriaMapper
{

    private final CurrentUserService currentUserService;

    private final OrganisationUnitService organisationUnitService;

    private final ProgramService programService;

    private final TrackedEntityTypeService trackedEntityTypeService;

    private final TrackedEntityInstanceService trackedEntityInstanceService;

    private final TrackerAccessManager trackerAccessManager;

    /**
     * Returns a EnrollmentQueryParams based on the given input.
     *
     * @param ou the set of organisation unit identifiers.
     * @param ouMode the OrganisationUnitSelectionMode.
     * @param lastUpdated the last updated for enrollment.
     * @param lastUpdatedDuration the last updated duration filter.
     * @param programUid the Program identifier.
     * @param programStatus the ProgramStatus in the given program.
     * @param programStartDate the start date for enrollment in the given
     *        Program.
     * @param programEndDate the end date for enrollment in the given Program.
     * @param trackedEntityType the TrackedEntityType uid.
     * @param trackedEntityInstance the TrackedEntity uid.
     * @param followUp indicates follow up status in the given Program.
     * @param page the page number.
     * @param pageSize the page size.
     * @param totalPages indicates whether to include the total number of pages.
     * @param skipPaging whether to skip paging.
     * @param includeDeleted whether to include soft deleted ones
     * @return a EnrollmentQueryParams.
     */
    @Transactional( readOnly = true )
    public EnrollmentQueryParams getFromUrl( Set<String> ou, OrganisationUnitSelectionMode ouMode,
        Date lastUpdated, String lastUpdatedDuration, String programUid, ProgramStatus programStatus,
        Date programStartDate, Date programEndDate, String trackedEntityType, String trackedEntityInstance,
        Boolean followUp, Integer page, Integer pageSize, boolean totalPages, boolean skipPaging,
        boolean includeDeleted, List<OrderCriteria> orderCriteria )
        throws ForbiddenException
    {
        EnrollmentQueryParams params = new EnrollmentQueryParams();

        User user = currentUserService.getCurrentUser();

        Program program = programUid != null ? programService.getProgram( programUid ) : null;

        if ( programUid != null && program == null )
        {
            throw new IllegalQueryException( "Program does not exist: " + programUid );
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

                if ( !trackerAccessManager.canAccess( user, program, organisationUnit ) )
                {
                    throw new ForbiddenException(
                        "User does not have access to organisation unit: " + organisationUnit.getUid() );
                }

                params.getOrganisationUnits().add( organisationUnit );
            }
        }

        TrackedEntityType te = trackedEntityType != null
            ? trackedEntityTypeService.getTrackedEntityType( trackedEntityType )
            : null;

        if ( trackedEntityType != null && te == null )
        {
            throw new IllegalQueryException( "Tracked entity does not exist: " + trackedEntityType );
        }

        TrackedEntity tei = trackedEntityInstance != null
            ? trackedEntityInstanceService.getTrackedEntityInstance( trackedEntityInstance )
            : null;

        if ( trackedEntityInstance != null && tei == null )
        {
            throw new IllegalQueryException( "Tracked entity instance does not exist: " + trackedEntityInstance );
        }

        params.setProgram( program );
        params.setProgramStatus( programStatus );
        params.setFollowUp( followUp );
        params.setLastUpdated( lastUpdated );
        params.setLastUpdatedDuration( lastUpdatedDuration );
        params.setProgramStartDate( programStartDate );
        params.setProgramEndDate( programEndDate );
        params.setTrackedEntityType( te );
        params.setTrackedEntityInstanceUid(
            Optional.ofNullable( tei ).map( IdentifiableObject::getUid ).orElse( null ) );
        params.setOrganisationUnitMode( ouMode );
        params.setPage( page );
        params.setPageSize( pageSize );
        params.setTotalPages( totalPages );
        params.setSkipPaging( skipPaging );
        params.setIncludeDeleted( includeDeleted );
        params.setUser( user );
        params.setOrder( toOrderParams( orderCriteria ) );

        return params;
    }
}
