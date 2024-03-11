package org.hisp.dhis.program;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.notification.ProgramNotificationEventType;
import org.hisp.dhis.program.notification.ProgramNotificationPublisher;
import org.hisp.dhis.programrule.engine.TrackedEntityInstanceEnrolledEvent;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.*;

/**
 * @author Abyot Asalefew
 */
public class DefaultProgramInstanceService
    implements ProgramInstanceService
{
    private static final Log log = LogFactory.getLog( DefaultProgramInstanceService.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private ProgramInstanceStore programInstanceStore;

    @Autowired
    private ProgramStageInstanceStore programStageInstanceStore;

    @Autowired
    private ProgramService programService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private ProgramNotificationPublisher programNotificationPublisher;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TrackerOwnershipManager trackerOwnershipAccessManager;

    @Autowired
    private ProgramInstanceAuditService programInstanceAuditService;

    @Autowired
    private AclService aclService;

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public int addProgramInstance( ProgramInstance programInstance )
    {
        programInstanceStore.save( programInstance );
        return programInstance.getId();
    }

    @Override
    @Transactional
    public void deleteProgramInstance( ProgramInstance programInstance )
    {
        deleteProgramInstance( programInstance, false );
    }

    @Override
    @Transactional
    public void deleteProgramInstance( ProgramInstance programInstance, boolean forceDelete )
    {
        if ( forceDelete )
        {
            programInstanceStore.delete( programInstance );
        }
        else
        {
            programInstance.setDeleted( true );
            programInstance.setStatus( ProgramStatus.CANCELLED );
            programInstanceStore.update( programInstance );
        }
    }

    @Override
    @Transactional( readOnly = true )
    public ProgramInstance getProgramInstance( int id )
    {
        ProgramInstance programInstance = programInstanceStore.get( id );

        User user = currentUserService.getCurrentUser();

        if ( user != null )
        {
            addProgramInstanceAudit( programInstance, user.getUsername() );
        }

        return programInstance;
    }

    @Override
    @Transactional( readOnly = true )
    public ProgramInstance getProgramInstance( String uid )
    {
        ProgramInstance programInstance = programInstanceStore.getByUid( uid );

        User user = currentUserService.getCurrentUser();

        if ( user != null )
        {
            addProgramInstanceAudit( programInstance, user.getUsername() );
        }

        return programInstance;
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
    @Transactional( readOnly = true )
    public ProgramInstanceQueryParams getFromUrl( Set<String> ou, OrganisationUnitSelectionMode ouMode,
        Date lastUpdated, String lastUpdatedDuration, String program, ProgramStatus programStatus,
        Date programStartDate, Date programEndDate, String trackedEntityType, String trackedEntityInstance,
        Boolean followUp, Integer page, Integer pageSize, boolean totalPages, boolean skipPaging, boolean includeDeleted )
    {
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

        TrackedEntityType te = trackedEntityType != null ? trackedEntityTypeService.getTrackedEntityType( trackedEntityType ) : null;

        if ( trackedEntityType != null && te == null )
        {
            throw new IllegalQueryException( "Tracked entity does not exist: " + program );
        }

        TrackedEntityInstance tei = trackedEntityInstance != null ? trackedEntityInstanceService.getTrackedEntityInstance( trackedEntityInstance ) : null;

        if ( trackedEntityInstance != null && tei == null )
        {
            throw new IllegalQueryException( "Tracked entity instance does not exist: " + program );
        }

        params.setProgram( pr );
        params.setProgramStatus( programStatus );
        params.setFollowUp( followUp );
        params.setLastUpdated( lastUpdated );
        params.setLastUpdatedDuration( lastUpdatedDuration );
        params.setProgramStartDate( programStartDate );
        params.setProgramEndDate( programEndDate );
        params.setTrackedEntityType( te );
        params.setTrackedEntityInstance( tei );
        params.setOrganisationUnitMode( ouMode );
        params.setPage( page );
        params.setPageSize( pageSize );
        params.setTotalPages( totalPages );
        params.setSkipPaging( skipPaging );
        params.setIncludeDeleted( includeDeleted );

        return params;
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
            Set<OrganisationUnit> organisationUnits = new HashSet<>();
            organisationUnits.addAll( params.getOrganisationUnits() );

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

        List<ProgramInstance> programInstances = programInstanceStore.getProgramInstances( params );

        if ( user != null )
        {
            addProrgamInstanceAudits( programInstances, user.getUsername() );
        }

        return programInstances;
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
        // TODO Check access for current user
    }

    @Override
    @Transactional( readOnly = true )
    public void validate( ProgramInstanceQueryParams params ) throws IllegalQueryException
    {
        String violation = null;

        if ( params == null )
        {
            throw new IllegalQueryException( "Params cannot be null" );
        }

        User user = currentUserService.getCurrentUser();

        if ( !params.hasOrganisationUnits() && !(params.isOrganisationUnitMode( ALL ) || params.isOrganisationUnitMode( ACCESSIBLE )) )
        {
            violation = "At least one organisation unit must be specified";
        }

        if ( params.isOrganisationUnitMode( ACCESSIBLE ) && (user == null || !user.hasDataViewOrganisationUnitWithFallback()) )
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
    public List<ProgramInstance> getProgramInstances( TrackedEntityInstance entityInstance, Program program, ProgramStatus status )
    {
        return programInstanceStore.get( entityInstance, program, status );
    }

    @Override
    @Transactional
    public ProgramInstance prepareProgramInstance( TrackedEntityInstance trackedEntityInstance, Program program,
        ProgramStatus programStatus, Date enrollmentDate, Date incidentDate, OrganisationUnit organisationUnit, String uid )
    {
        if ( program.getTrackedEntityType() != null && !program.getTrackedEntityType().equals( trackedEntityInstance.getTrackedEntityType() ) )
        {
            throw new IllegalQueryException( "Tracked entity instance must have same tracked entity as program: " + program.getUid() );
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

        ProgramInstance programInstance = prepareProgramInstance( trackedEntityInstance, program, ProgramStatus.ACTIVE, enrollmentDate,
            incidentDate, organisationUnit, uid );
        addProgramInstance( programInstance );

        // ---------------------------------------------------------------------
        // Add program owner and overwrite if already exists.
        // ---------------------------------------------------------------------

        trackerOwnershipAccessManager.assignOwnership( trackedEntityInstance, program, organisationUnit, true, true );

        // -----------------------------------------------------------------
        // Send enrollment notifications (if any)
        // -----------------------------------------------------------------

        programNotificationPublisher.publishEnrollment( programInstance, ProgramNotificationEventType.PROGRAM_ENROLLMENT );

        eventPublisher.publishEvent( new TrackedEntityInstanceEnrolledEvent( this, programInstance ) );

        // -----------------------------------------------------------------
        // Update ProgramInstance and TEI
        // -----------------------------------------------------------------

        updateProgramInstance( programInstance );
        trackedEntityInstanceService.updateTrackedEntityInstance( trackedEntityInstance );

        return programInstance;
    }

    @Override
    @Transactional( readOnly = true )
    public boolean canAutoCompleteProgramInstanceStatus( ProgramInstance programInstance )
    {
        Set<ProgramStageInstance> programStageInstances = new HashSet<>( programInstance.getProgramStageInstances() );
        Set<ProgramStage> programStages = new HashSet<>();

        for ( ProgramStageInstance programStageInstance : programStageInstances )
        {
            if ( (!programStageInstance.isCompleted() && programStageInstance.getStatus() != EventStatus.SKIPPED)
                || programStageInstance.getProgramStage().getRepeatable() )
            {
                return false;
            }

            programStages.add( programStageInstance.getProgramStage() );
        }

        return programStages.size() == programInstance.getProgram().getProgramStages().size();
    }

    @Override
    @Transactional
    public void completeProgramInstanceStatus( ProgramInstance programInstance )
    {
        // ---------------------------------------------------------------------
        // Send sms-message when to completed the program
        // ---------------------------------------------------------------------

        programNotificationPublisher.publishEnrollment( programInstance, ProgramNotificationEventType.PROGRAM_COMPLETION );

        eventPublisher.publishEvent( new TrackedEntityInstanceEnrolledEvent( this, programInstance ) );

        // -----------------------------------------------------------------
        // Update program-instance
        // -----------------------------------------------------------------

        programInstance.setStatus( ProgramStatus.COMPLETED );
        programInstance.setEndDate( new Date() );
        programInstance.setCompletedBy( currentUserService.getCurrentUsername() );

        updateProgramInstance( programInstance );
    }

    @Override
    @Transactional
    public void cancelProgramInstanceStatus( ProgramInstance programInstance )
    {
        // ---------------------------------------------------------------------
        // Set status of the program-instance
        // ---------------------------------------------------------------------

        Calendar today = Calendar.getInstance();
        PeriodType.clearTimeOfDay( today );
        Date currentDate = today.getTime();

        programInstance.setEndDate( currentDate );
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

                if ( programStageInstance.getDueDate().before( currentDate ) )
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

            throw new IllegalQueryException( "Program has another active enrollment going on. Not possible to incomplete" );
        }

        // -----------------------------------------------------------------
        // Update program-instance
        // -----------------------------------------------------------------

        programInstance.setStatus( ProgramStatus.ACTIVE );

        updateProgramInstance( programInstance );
    }

    private void addProgramInstanceAudit( ProgramInstance programInstance, String accessedBy )
    {
        if ( programInstance != null && programInstance.getProgram().getAccessLevel() != null && programInstance.getProgram().getAccessLevel() != AccessLevel.OPEN && accessedBy != null )
        {
            ProgramInstanceAudit programInstanceAudit = new ProgramInstanceAudit( programInstance, accessedBy, AuditType.READ );

            programInstanceAuditService.addProgramInstanceAudit( programInstanceAudit );
        }
    }

    private void addProrgamInstanceAudits( List<ProgramInstance> programInstances, String accessedBy )
    {
        for ( ProgramInstance programInstance : programInstances )
        {
            addProgramInstanceAudit( programInstance, accessedBy );
        }
    }
}
