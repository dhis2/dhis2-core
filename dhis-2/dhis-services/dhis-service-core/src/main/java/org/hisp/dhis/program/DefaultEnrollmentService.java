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

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@Service( "org.hisp.dhis.program.EnrollmentService" )
public class DefaultEnrollmentService
    implements EnrollmentService
{
    private final EnrollmentStore enrollmentStore;

    private final EventStore eventStore;

    private final CurrentUserService currentUserService;

    private final TrackedEntityInstanceService trackedEntityInstanceService;

    private final ApplicationEventPublisher eventPublisher;

    private final TrackerOwnershipManager trackerOwnershipAccessManager;

    private final AclService aclService;

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addEnrollment( Enrollment enrollment )
    {
        enrollmentStore.save( enrollment );
        return enrollment.getId();
    }

    @Override
    @Transactional
    public long addEnrollment( Enrollment enrollment, User user )
    {
        enrollmentStore.save( enrollment, user );
        return enrollment.getId();
    }

    @Override
    @Transactional
    public void deleteEnrollment( Enrollment enrollment )
    {
        enrollment.setStatus( ProgramStatus.CANCELLED );
        enrollmentStore.update( enrollment );
        enrollmentStore.delete( enrollment );
    }

    @Override
    @Transactional
    public void hardDeleteEnrollment( Enrollment enrollment )
    {
        enrollmentStore.hardDelete( enrollment );
    }

    @Override
    @Transactional( readOnly = true )
    public Enrollment getEnrollment( long id )
    {
        Enrollment enrollment = enrollmentStore.get( id );

        return enrollment;
    }

    @Override
    @Transactional( readOnly = true )
    public Enrollment getEnrollment( String uid )
    {
        Enrollment enrollment = enrollmentStore.getByUid( uid );

        return enrollment;
    }

    @Override
    @Transactional( readOnly = true )
    public List<Enrollment> getEnrollments( @Nonnull List<String> uids )
    {
        return enrollmentStore.getByUid( uids );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean enrollmentExists( String uid )
    {
        return enrollmentStore.exists( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean enrollmentExistsIncludingDeleted( String uid )
    {
        return enrollmentStore.existsIncludingDeleted( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public List<String> getEnrollmentsUidsIncludingDeleted( List<String> uids )
    {
        return enrollmentStore.getUidsIncludingDeleted( uids );
    }

    @Override
    @Transactional
    public void updateEnrollment( Enrollment enrollment )
    {
        enrollmentStore.update( enrollment );
    }

    @Override
    @Transactional
    public void updateEnrollment( Enrollment enrollment, User user )
    {
        enrollmentStore.update( enrollment, user );
    }

    // TODO consider security
    @Override
    @Transactional( readOnly = true )
    public List<Enrollment> getEnrollments( EnrollmentQueryParams params )
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

        return enrollmentStore.getEnrollments( params );
    }

    @Override
    @Transactional( readOnly = true )
    public int countEnrollments( EnrollmentQueryParams params )
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

        return enrollmentStore.countEnrollments( params );
    }

    @Override
    @Transactional( readOnly = true )
    public void decideAccess( EnrollmentQueryParams params )
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
    public void validate( EnrollmentQueryParams params )
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
    public List<Enrollment> getEnrollments( Program program )
    {
        return enrollmentStore.get( program );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Enrollment> getEnrollments( Program program, ProgramStatus status )
    {
        return enrollmentStore.get( program, status );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Enrollment> getEnrollments( TrackedEntityInstance entityInstance, Program program,
        ProgramStatus status )
    {
        return enrollmentStore.get( entityInstance, program, status );
    }

    @Nonnull
    @Override
    @Transactional
    public Enrollment prepareEnrollment( TrackedEntityInstance trackedEntityInstance, Program program,
        ProgramStatus programStatus, Date enrollmentDate, Date incidentDate, OrganisationUnit organisationUnit,
        String uid )
    {
        if ( program.getTrackedEntityType() != null
            && !program.getTrackedEntityType().equals( trackedEntityInstance.getTrackedEntityType() ) )
        {
            throw new IllegalQueryException(
                "Tracked entity instance must have same tracked entity as program: " + program.getUid() );
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setUid( CodeGenerator.isValidUid( uid ) ? uid : CodeGenerator.generateUid() );
        enrollment.setOrganisationUnit( organisationUnit );
        enrollment.enrollTrackedEntityInstance( trackedEntityInstance, program );

        if ( enrollmentDate != null )
        {
            enrollment.setEnrollmentDate( enrollmentDate );
        }
        else
        {
            enrollment.setEnrollmentDate( new Date() );
        }

        if ( incidentDate != null )
        {
            enrollment.setIncidentDate( incidentDate );
        }
        else
        {
            enrollment.setIncidentDate( new Date() );
        }

        enrollment.setStatus( programStatus );

        return enrollment;
    }

    @Override
    @Transactional
    public Enrollment enrollTrackedEntityInstance( TrackedEntityInstance trackedEntityInstance, Program program,
        Date enrollmentDate, Date incidentDate, OrganisationUnit organisationUnit )
    {
        return enrollTrackedEntityInstance( trackedEntityInstance, program, enrollmentDate,
            incidentDate, organisationUnit, CodeGenerator.generateUid() );
    }

    @Override
    @Transactional
    public Enrollment enrollTrackedEntityInstance( TrackedEntityInstance trackedEntityInstance, Program program,
        Date enrollmentDate, Date incidentDate, OrganisationUnit organisationUnit, String uid )
    {
        // ---------------------------------------------------------------------
        // Add program instance
        // ---------------------------------------------------------------------

        Enrollment enrollment = prepareEnrollment( trackedEntityInstance, program, ProgramStatus.ACTIVE,
            enrollmentDate,
            incidentDate, organisationUnit, uid );
        addEnrollment( enrollment );

        // ---------------------------------------------------------------------
        // Add program owner and overwrite if already exists.
        // ---------------------------------------------------------------------

        trackerOwnershipAccessManager.assignOwnership( trackedEntityInstance, program, organisationUnit, true, true );

        // -----------------------------------------------------------------
        // Send enrollment notifications (if any)
        // -----------------------------------------------------------------

        eventPublisher.publishEvent( new ProgramEnrollmentNotificationEvent( this, enrollment.getId() ) );

        eventPublisher.publishEvent( new EnrollmentEvaluationEvent( this, enrollment.getId() ) );

        // -----------------------------------------------------------------
        // Update Enrollment and TEI
        // -----------------------------------------------------------------

        updateEnrollment( enrollment );
        trackedEntityInstanceService.updateTrackedEntityInstance( trackedEntityInstance );

        return enrollment;
    }

    @Override
    @Transactional
    public void completeEnrollmentStatus( Enrollment enrollment )
    {
        // -----------------------------------------------------------------
        // Update program-instance
        // -----------------------------------------------------------------

        enrollment.setStatus( ProgramStatus.COMPLETED );
        updateEnrollment( enrollment );

        // ---------------------------------------------------------------------
        // Send sms-message after program completion
        // ---------------------------------------------------------------------

        eventPublisher
            .publishEvent( new ProgramEnrollmentCompletionNotificationEvent( this, enrollment.getId() ) );

        eventPublisher.publishEvent( new EnrollmentEvaluationEvent( this, enrollment.getId() ) );
    }

    @Override
    @Transactional
    public void cancelEnrollmentStatus( Enrollment enrollment )
    {
        // ---------------------------------------------------------------------
        // Set status of the program-instance
        // ---------------------------------------------------------------------
        enrollment.setStatus( ProgramStatus.CANCELLED );
        updateEnrollment( enrollment );

        // ---------------------------------------------------------------------
        // Set statuses of the program-stage-instances
        // ---------------------------------------------------------------------

        for ( Event event : enrollment.getEvents() )
        {
            if ( event.getExecutionDate() == null )
            {
                // -------------------------------------------------------------
                // Set status as skipped for overdue events, or delete
                // -------------------------------------------------------------

                if ( event.getDueDate().before( enrollment.getEndDate() ) )
                {
                    event.setStatus( EventStatus.SKIPPED );
                    eventStore.update( event );
                }
                else
                {
                    eventStore.delete( event );
                }
            }
        }
    }

    @Override
    @Transactional
    public void incompleteEnrollmentStatus( Enrollment enrollment )
    {
        Program program = enrollment.getProgram();

        TrackedEntityInstance tei = enrollment.getEntityInstance();

        if ( getEnrollments( tei, program, ProgramStatus.ACTIVE ).size() > 0 )
        {
            log.warn( "Program has another active enrollment going on. Not possible to incomplete" );

            throw new IllegalQueryException(
                "Program has another active enrollment going on. Not possible to incomplete" );
        }

        // -----------------------------------------------------------------
        // Update program-instance
        // -----------------------------------------------------------------

        enrollment.setStatus( ProgramStatus.ACTIVE );
        enrollment.setEndDate( null );

        updateEnrollment( enrollment );
    }
}
