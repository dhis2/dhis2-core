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
package org.hisp.dhis.program;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.notification.event.ProgramEnrollmentCompletionNotificationEvent;
import org.hisp.dhis.program.notification.event.ProgramEnrollmentNotificationEvent;
import org.hisp.dhis.programrule.engine.EnrollmentEvaluationEvent;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Abyot Asalefew
 */
@Slf4j
@Service( "org.hisp.dhis.program.ProgramInstanceService" )
public class DefaultProgramInstanceService
    implements ProgramInstanceService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final ProgramInstanceStore programInstanceStore;

    private final ProgramStageInstanceStore programStageInstanceStore;

    private CurrentUserService currentUserService;

    private TrackedEntityInstanceService trackedEntityInstanceService;

    private ApplicationEventPublisher eventPublisher;

    private TrackerOwnershipManager trackerOwnershipAccessManager;

    private AclService aclService;

    public DefaultProgramInstanceService( ProgramInstanceStore programInstanceStore,
        ProgramStageInstanceStore programStageInstanceStore, CurrentUserService currentUserService,
        TrackedEntityInstanceService trackedEntityInstanceService, ApplicationEventPublisher eventPublisher,
        TrackerOwnershipManager trackerOwnershipAccessManager, AclService aclService )
    {
        checkNotNull( programInstanceStore );
        checkNotNull( programStageInstanceStore );
        checkNotNull( currentUserService );
        checkNotNull( trackedEntityInstanceService );
        checkNotNull( eventPublisher );
        checkNotNull( trackerOwnershipAccessManager );
        checkNotNull( aclService );

        this.programInstanceStore = programInstanceStore;
        this.programStageInstanceStore = programStageInstanceStore;
        this.currentUserService = currentUserService;
        this.trackedEntityInstanceService = trackedEntityInstanceService;
        this.eventPublisher = eventPublisher;
        this.trackerOwnershipAccessManager = trackerOwnershipAccessManager;
        this.aclService = aclService;
    }

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addProgramInstance( ProgramInstance programInstance )
    {
        programInstanceStore.save( programInstance );
        return programInstance.getId();
    }

    @Override
    @Transactional
    public long addProgramInstance( ProgramInstance programInstance, User user )
    {
        programInstanceStore.save( programInstance, user );
        return programInstance.getId();
    }

    @Override
    @Transactional
    public void deleteProgramInstance( ProgramInstance programInstance )
    {
        programInstance.setStatus( ProgramStatus.CANCELLED );
        programInstanceStore.update( programInstance );
        programInstanceStore.delete( programInstance );
    }

    @Override
    @Transactional
    public void hardDeleteProgramInstance( ProgramInstance programInstance )
    {
        programInstanceStore.hardDelete( programInstance );
    }

    @Override
    @Transactional( readOnly = true )
    public ProgramInstance getProgramInstance( long id )
    {
        ProgramInstance programInstance = programInstanceStore.get( id );

        return programInstance;
    }

    @Override
    @Transactional( readOnly = true )
    public ProgramInstance getProgramInstance( String uid )
    {
        ProgramInstance programInstance = programInstanceStore.getByUid( uid );

        return programInstance;
    }

    @Override
    @Transactional( readOnly = true )
    public List<ProgramInstance> getProgramInstances( List<String> uids )
    {
        return programInstanceStore.getByUid( uids );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean programInstanceExists( String uid )
    {
        return programInstanceStore.exists( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean programInstanceExistsIncludingDeleted( String uid )
    {
        return programInstanceStore.existsIncludingDeleted( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public List<String> getProgramInstancesUidsIncludingDeleted( List<String> uids )
    {
        return programInstanceStore.getUidsIncludingDeleted( uids );
    }

    @Override
    @Transactional
    public void updateProgramInstance( ProgramInstance programInstance )
    {
        programInstanceStore.update( programInstance );
    }

    @Override
    @Transactional
    public void updateProgramInstance( ProgramInstance programInstance, User user )
    {
        programInstanceStore.update( programInstance, user );
    }

    // TODO consider security
    @Override
    @Transactional( readOnly = true )
    public List<ProgramInstance> getProgramInstances( ProgramInstanceQueryParams params )
    {
        decideAccess( params );
        validate( params );

        User user = currentUserService.getCurrentUser();

        if ( user != null && params.isOrganisationUnitMode( OrganisationUnitSelectionMode.ACCESSIBLE ) )
        {
            params.setOrganisationUnits( user.getTeiSearchOrganisationUnitsWithFallback() );
            params.setOrganisationUnitMode( OrganisationUnitSelectionMode.DESCENDANTS );
        }
        else if ( params.isOrganisationUnitMode( CHILDREN ) )
        {
            Set<OrganisationUnit> organisationUnits = new HashSet<>( params.getOrganisationUnits() );

            for ( OrganisationUnit organisationUnit : params.getOrganisationUnits() )
            {
                organisationUnits.addAll( organisationUnit.getChildren() );
            }

            params.setOrganisationUnits( organisationUnits );
        }

        if ( !params.isPaging() && !params.isSkipPaging() )
        {
            params.setDefaultPaging();
        }

        return programInstanceStore.getProgramInstances( params );
    }

    @Override
    @Transactional( readOnly = true )
    public int countProgramInstances( ProgramInstanceQueryParams params )
    {
        decideAccess( params );
        validate( params );

        User user = currentUserService.getCurrentUser();

        if ( user != null && params.isOrganisationUnitMode( OrganisationUnitSelectionMode.ACCESSIBLE ) )
        {
            params.setOrganisationUnits( user.getTeiSearchOrganisationUnitsWithFallback() );
            params.setOrganisationUnitMode( OrganisationUnitSelectionMode.DESCENDANTS );
        }
        else if ( params.isOrganisationUnitMode( CHILDREN ) )
        {
            Set<OrganisationUnit> organisationUnits = new HashSet<>();
            organisationUnits.addAll( params.getOrganisationUnits() );

            for ( OrganisationUnit organisationUnit : params.getOrganisationUnits() )
            {
                organisationUnits.addAll( organisationUnit.getChildren() );
            }

            params.setOrganisationUnits( organisationUnits );
        }

        params.setSkipPaging( true );

        return programInstanceStore.countProgramInstances( params );
    }

    @Override
    @Transactional( readOnly = true )
    public void decideAccess( ProgramInstanceQueryParams params )
    {
        if ( params.hasProgram() )
        {
            if ( !aclService.canDataRead( params.getUser(), params.getProgram() ) )
            {
                throw new IllegalQueryException( "Current user is not authorized to read data from selected program:  "
                    + params.getProgram().getUid() );
            }

            if ( params.getProgram().getTrackedEntityType() != null
                && !aclService.canDataRead( params.getUser(), params.getProgram().getTrackedEntityType() ) )
            {
                throw new IllegalQueryException(
                    "Current user is not authorized to read data from selected program's tracked entity type:  "
                        + params.getProgram().getTrackedEntityType().getUid() );
            }

        }

        if ( params.hasTrackedEntityType()
            && !aclService.canDataRead( params.getUser(), params.getTrackedEntityType() ) )
        {
            throw new IllegalQueryException(
                "Current user is not authorized to read data from selected tracked entity type:  "
                    + params.getTrackedEntityType().getUid() );
        }
    }

    @Override
    public void validate( ProgramInstanceQueryParams params )
        throws IllegalQueryException
    {
        String violation = null;

        if ( params == null )
        {
            throw new IllegalQueryException( "Params cannot be null" );
        }

        User user = params.getUser();

        if ( !params.hasOrganisationUnits()
            && !(params.isOrganisationUnitMode( ALL ) || params.isOrganisationUnitMode( ACCESSIBLE )) )
        {
            violation = "At least one organisation unit must be specified";
        }

        if ( params.isOrganisationUnitMode( ACCESSIBLE )
            && (user == null || !user.hasDataViewOrganisationUnitWithFallback()) )
        {
            violation = "Current user must be associated with at least one organisation unit when selection mode is ACCESSIBLE";
        }

        if ( params.hasProgram() && params.hasTrackedEntityType() )
        {
            violation = "Program and tracked entity cannot be specified simultaneously";
        }

        if ( params.hasProgramStatus() && !params.hasProgram() )
        {
            violation = "Program must be defined when program status is defined";
        }

        if ( params.hasFollowUp() && !params.hasProgram() )
        {
            violation = "Program must be defined when follow up status is defined";
        }

        if ( params.hasProgramStartDate() && !params.hasProgram() )
        {
            violation = "Program must be defined when program start date is specified";
        }

        if ( params.hasProgramEndDate() && !params.hasProgram() )
        {
            violation = "Program must be defined when program end date is specified";
        }

        if ( params.hasLastUpdated() && params.hasLastUpdatedDuration() )
        {
            violation = "Last updated and last updated duration cannot be specified simultaneously";
        }

        if ( params.hasLastUpdatedDuration() && DateUtils.getDuration( params.getLastUpdatedDuration() ) == null )
        {
            violation = "Duration is not valid: " + params.getLastUpdatedDuration();
        }

        if ( violation != null )
        {
            log.warn( "Validation failed: " + violation );

            throw new IllegalQueryException( violation );
        }
    }

    @Override
    @Transactional( readOnly = true )
    public List<ProgramInstance> getProgramInstances( Program program )
    {
        return programInstanceStore.get( program );
    }

    @Override
    @Transactional( readOnly = true )
    public List<ProgramInstance> getProgramInstances( Program program, ProgramStatus status )
    {
        return programInstanceStore.get( program, status );
    }

    @Override
    @Transactional( readOnly = true )
    public List<ProgramInstance> getProgramInstances( TrackedEntityInstance entityInstance, Program program,
        ProgramStatus status )
    {
        return programInstanceStore.get( entityInstance, program, status );
    }

    @Override
    @Transactional
    public ProgramInstance prepareProgramInstance( TrackedEntityInstance trackedEntityInstance, Program program,
        ProgramStatus programStatus, Date enrollmentDate, Date incidentDate, OrganisationUnit organisationUnit,
        String uid )
    {
        if ( program.getTrackedEntityType() != null
            && !program.getTrackedEntityType().equals( trackedEntityInstance.getTrackedEntityType() ) )
        {
            throw new IllegalQueryException(
                "Tracked entity instance must have same tracked entity as program: " + program.getUid() );
        }

        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( CodeGenerator.isValidUid( uid ) ? uid : CodeGenerator.generateUid() );
        programInstance.setOrganisationUnit( organisationUnit );
        programInstance.enrollTrackedEntityInstance( trackedEntityInstance, program );

        if ( enrollmentDate != null )
        {
            programInstance.setEnrollmentDate( enrollmentDate );
        }
        else
        {
            programInstance.setEnrollmentDate( new Date() );
        }

        if ( incidentDate != null )
        {
            programInstance.setIncidentDate( incidentDate );
        }
        else
        {
            programInstance.setIncidentDate( new Date() );
        }

        programInstance.setStatus( programStatus );

        return programInstance;
    }

    @Override
    @Transactional
    public ProgramInstance enrollTrackedEntityInstance( TrackedEntityInstance trackedEntityInstance, Program program,
        Date enrollmentDate, Date incidentDate, OrganisationUnit organisationUnit )
    {
        return enrollTrackedEntityInstance( trackedEntityInstance, program, enrollmentDate,
            incidentDate, organisationUnit, CodeGenerator.generateUid() );
    }

    @Override
    @Transactional
    public ProgramInstance enrollTrackedEntityInstance( TrackedEntityInstance trackedEntityInstance, Program program,
        Date enrollmentDate, Date incidentDate, OrganisationUnit organisationUnit, String uid )
    {
        // ---------------------------------------------------------------------
        // Add program instance
        // ---------------------------------------------------------------------

        ProgramInstance programInstance = prepareProgramInstance( trackedEntityInstance, program, ProgramStatus.ACTIVE,
            enrollmentDate,
            incidentDate, organisationUnit, uid );
        addProgramInstance( programInstance );

        // ---------------------------------------------------------------------
        // Add program owner and overwrite if already exists.
        // ---------------------------------------------------------------------

        trackerOwnershipAccessManager.assignOwnership( trackedEntityInstance, program, organisationUnit, true, true );

        // -----------------------------------------------------------------
        // Send enrollment notifications (if any)
        // -----------------------------------------------------------------

        eventPublisher.publishEvent( new ProgramEnrollmentNotificationEvent( this, programInstance.getId() ) );

        eventPublisher.publishEvent( new EnrollmentEvaluationEvent( this, programInstance.getId() ) );

        // -----------------------------------------------------------------
        // Update ProgramInstance and TEI
        // -----------------------------------------------------------------

        updateProgramInstance( programInstance );
        trackedEntityInstanceService.updateTrackedEntityInstance( trackedEntityInstance );

        return programInstance;
    }

    @Override
    @Transactional
    public void completeProgramInstanceStatus( ProgramInstance programInstance )
    {
        // -----------------------------------------------------------------
        // Update program-instance
        // -----------------------------------------------------------------

        programInstance.setStatus( ProgramStatus.COMPLETED );
        updateProgramInstance( programInstance );

        // ---------------------------------------------------------------------
        // Send sms-message after program completion
        // ---------------------------------------------------------------------

        eventPublisher
            .publishEvent( new ProgramEnrollmentCompletionNotificationEvent( this, programInstance.getId() ) );

        eventPublisher.publishEvent( new EnrollmentEvaluationEvent( this, programInstance.getId() ) );
    }

    @Override
    @Transactional
    public void cancelProgramInstanceStatus( ProgramInstance programInstance )
    {
        // ---------------------------------------------------------------------
        // Set status of the program-instance
        // ---------------------------------------------------------------------
        programInstance.setStatus( ProgramStatus.CANCELLED );
        updateProgramInstance( programInstance );

        // ---------------------------------------------------------------------
        // Set statuses of the program-stage-instances
        // ---------------------------------------------------------------------

        for ( ProgramStageInstance programStageInstance : programInstance.getProgramStageInstances() )
        {
            if ( programStageInstance.getExecutionDate() == null )
            {
                // -------------------------------------------------------------
                // Set status as skipped for overdue events, or delete
                // -------------------------------------------------------------

                if ( programStageInstance.getDueDate().before( programInstance.getEndDate() ) )
                {
                    programStageInstance.setStatus( EventStatus.SKIPPED );
                    programStageInstanceStore.update( programStageInstance );
                }
                else
                {
                    programStageInstanceStore.delete( programStageInstance );
                }
            }
        }
    }

    @Override
    @Transactional
    public void incompleteProgramInstanceStatus( ProgramInstance programInstance )
    {
        Program program = programInstance.getProgram();

        TrackedEntityInstance tei = programInstance.getEntityInstance();

        if ( getProgramInstances( tei, program, ProgramStatus.ACTIVE ).size() > 0 )
        {
            log.warn( "Program has another active enrollment going on. Not possible to incomplete" );

            throw new IllegalQueryException(
                "Program has another active enrollment going on. Not possible to incomplete" );
        }

        // -----------------------------------------------------------------
        // Update program-instance
        // -----------------------------------------------------------------

        programInstance.setStatus( ProgramStatus.ACTIVE );
        programInstance.setEndDate( null );

        updateProgramInstance( programInstance );
    }
}
