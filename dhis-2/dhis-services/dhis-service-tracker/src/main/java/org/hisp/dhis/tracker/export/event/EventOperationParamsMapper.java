/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.export.event;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps {@link EventOperationParams} to {@link EventSearchParams} which is used
 * to fetch enrollments from the DB.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventOperationParamsMapper
{

    private final ProgramService programService;

    private final ProgramStageService programStageService;

    private final OrganisationUnitService organisationUnitService;

    private final TrackedEntityService trackedEntityService;

    private final AclService aclService;

    private final CategoryOptionComboService categoryOptionComboService;

    private final TrackerAccessManager trackerAccessManager;

    private final CurrentUserService currentUserService;

    //For now this maps to EventSearchParams. We should create a new EventQueryParams class that should be used in the persistence layer
    @Transactional( readOnly = true )
    public EventSearchParams map( EventOperationParams operationParams )
        throws BadRequestException,
        ForbiddenException
    {
        User user = currentUserService.getCurrentUser();

        Program program = validateProgram( operationParams.getProgramUid() );
        ProgramStage programStage = validateProgramStage( operationParams.getProgramStageUid() );
        OrganisationUnit orgUnit = validateOrgUnit( operationParams.getOrgUnitUid() );
        validateUser( user, program, programStage, operationParams.getOrgUnitUid(), orgUnit );
        TrackedEntity trackedEntity = validateTrackedEntity( operationParams.getTrackedEntityUid() );

        CategoryOptionCombo attributeOptionCombo = categoryOptionComboService.getAttributeOptionCombo(
            operationParams.getAttributeCategoryCombo() != null ? operationParams.getAttributeCategoryCombo() : null,
            operationParams.getAttributeCategoryOptions(), true );

        validateAttributeOptionCombo( attributeOptionCombo, user );

        validateOperationParams( operationParams, user, program, orgUnit );

        EventSearchParams searchParams = new EventSearchParams();

        return searchParams.setProgram( program ).setProgramStage( programStage ).setOrgUnit( orgUnit )
            .setTrackedEntity( trackedEntity )
            .setProgramStatus( operationParams.getProgramStatus() )
            .setFollowUp( operationParams.getFollowUp() )
            .setOrgUnitSelectionMode( operationParams.getOrgUnitSelectionMode() )
            .setAssignedUserQueryParam( operationParams.getAssignedUserQueryParam() )
            .setStartDate( operationParams.getStartDate() )
            .setEndDate( operationParams.getEndDate() )
            .setScheduleAtStartDate( operationParams.getScheduleAtStartDate() )
            .setScheduleAtEndDate( operationParams.getScheduleAtEndDate() )
            .setUpdatedAtStartDate( operationParams.getUpdatedAtStartDate() )
            .setUpdatedAtEndDate( operationParams.getUpdatedAtEndDate() )
            .setUpdatedAtDuration( operationParams.getUpdatedAtDuration() )
            .setEnrollmentEnrolledBefore( operationParams.getEnrollmentEnrolledBefore() )
            .setEnrollmentEnrolledAfter( operationParams.getEnrollmentEnrolledAfter() )
            .setEnrollmentOccurredBefore( operationParams.getEnrollmentOccurredBefore() )
            .setEnrollmentOccurredAfter( operationParams.getEnrollmentOccurredAfter() )
            .setEventStatus( operationParams.getEventStatus() )
            .setCategoryOptionCombo( attributeOptionCombo )
            .setIdSchemes( operationParams.getIdSchemes() )
            .setPage( operationParams.getPage() )
            .setPageSize( operationParams.getPageSize() )
            .setTotalPages( operationParams.isTotalPages() )
            .setSkipPaging( toBooleanDefaultIfNull( operationParams.isSkipPaging(), false ) )
            .setSkipEventId( operationParams.getSkipEventId() )
            .setIncludeAttributes( false )
            .setIncludeAllDataElements( false )
            .addDataElements( operationParams.getDataElements() )
            .addFilters( operationParams.getFilters() )
            .addFilterAttributes( operationParams.getFilterAttributes() )
            .addOrders( operationParams.getOrders() )
            .addGridOrders( operationParams.getGridOrders() )
            .addAttributeOrders( operationParams.getAttributeOrders() )
            .setEvents( operationParams.getEvents() )
            .setEnrollments( operationParams.getEnrollments() )
            .setIncludeDeleted( operationParams.isIncludeDeleted() );
    }

    private Program validateProgram( String programUid )
        throws BadRequestException
    {
        if ( programUid == null )
        {
            return null;
        }

        Program program = programService.getProgram( programUid );
        if ( program == null )
        {
            throw new BadRequestException( "Program is specified but does not exist: " + programUid );
        }

        return program;
    }

    private ProgramStage validateProgramStage( String programStageUid )
        throws BadRequestException
    {
        if ( programStageUid == null )
        {
            return null;
        }

        ProgramStage programStage = programStageService.getProgramStage( programStageUid );
        if ( programStage == null )
        {
            throw new BadRequestException( "Program stage is specified but does not exist: " + programStageUid );
        }

        return programStage;
    }

    private OrganisationUnit validateOrgUnit( String orgUnitUid )
        throws BadRequestException
    {
        if ( orgUnitUid == null )
        {
            return null;
        }

        OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit( orgUnitUid );
        if ( orgUnit == null )
        {
            throw new BadRequestException( "Org unit is specified but does not exist: " + orgUnitUid );
        }

        return orgUnit;
    }

    private void validateUser( User user, Program pr, ProgramStage ps, String orgUnitUid, OrganisationUnit orgUnit )
        throws ForbiddenException
    {
        if ( pr != null && !user.isSuper() && !aclService.canDataRead( user, pr ) )
        {
            throw new ForbiddenException( "User has no access to program: " + pr.getUid() );
        }

        if ( ps != null && !user.isSuper() && !aclService.canDataRead( user, ps ) )
        {
            throw new ForbiddenException( "User has no access to program stage: " + ps.getUid() );
        }

        if ( orgUnitUid != null && !trackerAccessManager.canAccess( user, pr, orgUnit ) )
        {
            throw new ForbiddenException( "User does not have access to orgUnit: " + orgUnit );
        }
    }

    private TrackedEntity validateTrackedEntity( String trackedEntityUid )
        throws BadRequestException
    {
        if ( trackedEntityUid == null )
        {
            return null;
        }

        TrackedEntity trackedEntity = trackedEntityService.getTrackedEntity( trackedEntityUid );
        if ( trackedEntity == null )
        {
            throw new BadRequestException( "Tracked entity is specified but does not exist: " + trackedEntityUid );
        }

        return trackedEntity;
    }

    private void validateAttributeOptionCombo( CategoryOptionCombo attributeOptionCombo, User user )
        throws ForbiddenException
    {
        if ( attributeOptionCombo != null && !user.isSuper()
            && !aclService.canDataRead( user, attributeOptionCombo ) )
        {
            throw new ForbiddenException(
                "User has no access to attribute category option combo: " + attributeOptionCombo.getUid() );
        }
    }

    public void validateOperationParams( EventOperationParams params, User user, Program program,
        OrganisationUnit orgUnit )
        throws IllegalQueryException
    {
        //TODO Should all these validations be moved to the web layer?
        String violation = null;

        if ( params.hasUpdatedAtDuration() && (params.hasUpdatedAtStartDate() || params.hasUpdatedAtEndDate()) )
        {
            violation = "Last updated from and/or to and last updated duration cannot be specified simultaneously";
        }

        if ( violation == null && params.hasUpdatedAtDuration()
            && DateUtils.getDuration( params.getUpdatedAtDuration() ) == null )
        {
            violation = "Duration is not valid: " + params.getUpdatedAtDuration();
        }

        if ( violation == null && params.getOrgUnitUid() != null
            && !trackerAccessManager.canAccess( user, program, orgUnit ) )
        {
            violation = "User does not have access to orgUnit: " + orgUnit;
        }

        if ( violation == null && params.getOrgUnitSelectionMode() != null )
        {
            violation = getOuModeViolation( params, user, program );
        }

        if ( violation != null )
        {
            log.warn( "Validation failed: " + violation );

            throw new IllegalQueryException( violation );
        }
    }

    private String getOuModeViolation( EventOperationParams params, User user, Program program )
    {
        OrganisationUnitSelectionMode selectedOuMode = params.getOrgUnitSelectionMode();

        String violation;

        switch ( selectedOuMode )
        {
        case ALL:
            violation = userCanSearchOuModeALL( user ) ? null
                : "Current user is not authorized to query across all organisation units";
            break;
        case ACCESSIBLE:
            violation = getAccessibleScopeValidation( user, program );
            break;
        case CAPTURE:
            violation = getCaptureScopeValidation( user );
            break;
        case CHILDREN:
        case SELECTED:
        case DESCENDANTS:
            violation = params.getOrgUnitUid() == null
                ? "Organisation unit is required for ouMode: " + params.getOrgUnitSelectionMode()
                : null;
            break;
        default:
            violation = "Invalid ouMode:  " + params.getOrgUnitSelectionMode();
            break;
        }

        return violation;
    }

    private String getCaptureScopeValidation( User user )
    {
        String violation = null;

        if ( user == null )
        {
            violation = "User is required for ouMode: " + OrganisationUnitSelectionMode.CAPTURE;
        }
        else if ( user.getOrganisationUnits().isEmpty() )
        {
            violation = "User needs to be assigned data capture orgunits";
        }

        return violation;
    }

    private String getAccessibleScopeValidation( User user, Program program )
    {
        String violation;

        if ( user == null )
        {
            return "User is required for ouMode: " + OrganisationUnitSelectionMode.ACCESSIBLE;
        }

        if ( program == null || program.isClosed() || program.isProtected() )
        {
            violation = user.getOrganisationUnits().isEmpty() ? "User needs to be assigned data capture orgunits"
                : null;
        }
        else
        {
            violation = user.getTeiSearchOrganisationUnitsWithFallback().isEmpty()
                ? "User needs to be assigned either TEI search, data view or data capture org units"
                : null;
        }

        return violation;
    }

    private boolean userCanSearchOuModeALL( User user )
    {
        if ( user == null )
        {
            return false;
        }

        return user.isSuper()
            || user.isAuthorized( Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS.name() );
    }
}