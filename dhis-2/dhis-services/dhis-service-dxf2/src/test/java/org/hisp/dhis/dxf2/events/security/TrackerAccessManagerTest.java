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
package org.hisp.dhis.dxf2.events.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.hibernate.SessionFactory;
import org.hisp.dhis.TransactionalIntegrationTest;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
class TrackerAccessManagerTest extends TransactionalIntegrationTest
{

    @Autowired
    private TrackerAccessManager trackerAccessManager;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private TrackerOwnershipManager trackerOwnershipManager;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private ProgramStageDataElementService programStageDataElementService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private UserService _userService;

    @Autowired
    private SessionFactory sessionFactory;

    private org.hisp.dhis.trackedentity.TrackedEntityInstance maleA;

    private org.hisp.dhis.trackedentity.TrackedEntityInstance maleB;

    private org.hisp.dhis.trackedentity.TrackedEntityInstance femaleA;

    private org.hisp.dhis.trackedentity.TrackedEntityInstance femaleB;

    private OrganisationUnit organisationUnitA;

    private OrganisationUnit organisationUnitB;

    private Program programA;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private ProgramStage programStageA;

    private ProgramStage programStageB;

    private TrackedEntityType trackedEntityType;

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Override
    protected void setUpTest()
    {
        userService = _userService;
        organisationUnitA = createOrganisationUnit( 'A' );
        organisationUnitB = createOrganisationUnit( 'B' );
        manager.save( organisationUnitA );
        manager.save( organisationUnitB );
        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );
        dataElementA.setValueType( ValueType.INTEGER );
        dataElementB.setValueType( ValueType.INTEGER );
        manager.save( dataElementA );
        manager.save( dataElementB );
        programStageA = createProgramStage( 'A', 0 );
        programStageB = createProgramStage( 'B', 0 );
        programStageB.setRepeatable( true );
        manager.save( programStageA );
        manager.save( programStageB );
        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programA.setProgramType( ProgramType.WITH_REGISTRATION );
        programA.setAccessLevel( AccessLevel.PROTECTED );
        programA.setPublicAccess( AccessStringHelper.FULL );
        programA.addOrganisationUnit( organisationUnitB );
        manager.save( programA );
        ProgramStageDataElement programStageDataElement = new ProgramStageDataElement();
        programStageDataElement.setDataElement( dataElementA );
        programStageDataElement.setProgramStage( programStageA );
        programStageDataElementService.addProgramStageDataElement( programStageDataElement );
        programStageA.getProgramStageDataElements().add( programStageDataElement );
        programStageA.setProgram( programA );
        programStageDataElement = new ProgramStageDataElement();
        programStageDataElement.setDataElement( dataElementB );
        programStageDataElement.setProgramStage( programStageB );
        programStageDataElementService.addProgramStageDataElement( programStageDataElement );
        programStageB.getProgramStageDataElements().add( programStageDataElement );
        programStageB.setProgram( programA );
        programStageB.setMinDaysFromStart( 2 );
        programA.getProgramStages().add( programStageA );
        programA.getProgramStages().add( programStageB );
        manager.update( programStageA );
        manager.update( programStageB );
        manager.update( programA );
        trackedEntityType = createTrackedEntityType( 'A' );
        trackedEntityType.setPublicAccess( AccessStringHelper.FULL );
        trackedEntityTypeService.addTrackedEntityType( trackedEntityType );
        maleA = createTrackedEntityInstance( organisationUnitA );
        maleB = createTrackedEntityInstance( organisationUnitB );
        femaleA = createTrackedEntityInstance( organisationUnitA );
        femaleB = createTrackedEntityInstance( organisationUnitB );
        maleA.setTrackedEntityType( trackedEntityType );
        maleB.setTrackedEntityType( trackedEntityType );
        femaleA.setTrackedEntityType( trackedEntityType );
        femaleB.setTrackedEntityType( trackedEntityType );
        manager.save( maleA );
        manager.save( maleB );
        manager.save( femaleA );
        manager.save( femaleB );
        enrollmentService.addEnrollment( createEnrollment( programA.getUid(), maleA.getUid() ),
            ImportOptions.getDefaultImportOptions() );
        // this is required because the event import takes place through JDBC
        // and
        // hibernate does not see
        // the values inserted by the JDBC session. Clearing the session, forces
        // hibernate to reload from db rather than
        // using the session
        sessionFactory.getCurrentSession().clear();
    }

    @Test
    void checkAccessPermissionForTeiWhenTeiOuInCaptureScope()
    {
        programA.setPublicAccess( AccessStringHelper.FULL );
        manager.update( programA );
        User user = createUserWithAuth( "user1" ).setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        trackedEntityType.setPublicAccess( AccessStringHelper.FULL );
        manager.update( trackedEntityType );
        TrackedEntityInstance tei = trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() );
        // Can read tei
        assertNoErrors( trackerAccessManager.canRead( user, tei ) );
        // can write tei
        assertNoErrors( trackerAccessManager.canWrite( user, tei ) );
    }

    @Test
    void checkAccessPermissionForTeiWhenTeiOuInSearchScope()
    {
        programA.setPublicAccess( AccessStringHelper.FULL );
        manager.update( programA );
        User user = createUserWithAuth( "user1" ).setOrganisationUnits( Sets.newHashSet( organisationUnitB ) );
        user.setTeiSearchOrganisationUnits( Sets.newHashSet( organisationUnitA, organisationUnitB ) );
        trackedEntityType.setPublicAccess( AccessStringHelper.FULL );
        manager.update( trackedEntityType );
        TrackedEntityInstance tei = trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() );
        // Can Read
        assertNoErrors( trackerAccessManager.canRead( user, tei ) );
        // Can write
        assertNoErrors( trackerAccessManager.canWrite( user, tei ) );
    }

    @Test
    void checkAccessPermissionForTeiWhenTeiOuOutsideSearchScope()
    {
        programA.setPublicAccess( AccessStringHelper.FULL );
        manager.update( programA );
        User user = createUserWithAuth( "user1" ).setOrganisationUnits( Sets.newHashSet( organisationUnitB ) );
        trackedEntityType.setPublicAccess( AccessStringHelper.FULL );
        manager.update( trackedEntityType );
        TrackedEntityInstance tei = trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() );
        // Cannot Read
        assertHasError( trackerAccessManager.canRead( user, tei ), "User has no read access to organisation unit:" );
        // Cannot write
        assertHasError( trackerAccessManager.canWrite( user, tei ), "User has no write access to organisation unit:" );
    }

    @Test
    void checkAccessPermissionForEnrollmentInClosedProgram()
    {
        programA.setPublicAccess( AccessStringHelper.FULL );
        manager.update( programA );
        trackedEntityType.setPublicAccess( AccessStringHelper.FULL );
        manager.update( trackedEntityType );
        User user = createUserWithAuth( "user1" ).setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        user.setTeiSearchOrganisationUnits( Sets.newHashSet( organisationUnitA, organisationUnitB ) );
        TrackedEntityInstance tei = trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() );
        ProgramInstance pi = tei.getProgramInstances().iterator().next();
        // Can create enrollment
        assertNoErrors( trackerAccessManager.canCreate( user, pi, false ) );
        // Can update enrollment
        assertNoErrors( trackerAccessManager.canUpdate( user, pi, false ) );
        // Cannot delete enrollment
        assertNoErrors( trackerAccessManager.canDelete( user, pi, false ) );
        // Can read enrollment
        assertNoErrors( trackerAccessManager.canRead( user, pi, false ) );
        // Cannot create enrollment if enrollmentOU is outside capture scope
        // even if user is owner.
        pi.setOrganisationUnit( organisationUnitB );
        assertHasError( trackerAccessManager.canCreate( user, pi, false ),
            "User has no create access to organisation unit:" );
        pi.setOrganisationUnit( organisationUnitA );
        // Transferring ownership to orgUnitB. user is no longer owner
        trackerOwnershipManager.transferOwnership( tei, programA, organisationUnitB, true, true );
        // Cannot create enrollment if not owner
        assertHasError( trackerAccessManager.canCreate( user, pi, false ), "OWNERSHIP_ACCESS_DENIED" );
        // Cannot update enrollment if not owner
        assertHasError( trackerAccessManager.canUpdate( user, pi, false ), "OWNERSHIP_ACCESS_DENIED" );
        // Cannot delete enrollment if not owner
        assertHasError( trackerAccessManager.canDelete( user, pi, false ), "OWNERSHIP_ACCESS_DENIED" );
        // Cannot read enrollment if not owner
        assertHasError( trackerAccessManager.canRead( user, pi, false ), "OWNERSHIP_ACCESS_DENIED" );
    }

    @Test
    void checkAccessPermissionForEnrollmentWhenOrgUnitIsNull()
    {
        programA.setPublicAccess( AccessStringHelper.FULL );
        programA.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        manager.update( programA );
        trackedEntityType.setPublicAccess( AccessStringHelper.FULL );
        manager.update( trackedEntityType );
        User user = createUserWithAuth( "user1" ).setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        user.setTeiSearchOrganisationUnits( Sets.newHashSet( organisationUnitA, organisationUnitB ) );
        TrackedEntityInstance tei = trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() );
        ProgramInstance pi = tei.getProgramInstances().iterator().next();
        pi.setOrganisationUnit( null );
        // Can create enrollment
        assertNoErrors( trackerAccessManager.canCreate( user, pi, false ) );
        // Can update enrollment
        assertNoErrors( trackerAccessManager.canUpdate( user, pi, false ) );
        // Cannot delete enrollment
        assertNoErrors( trackerAccessManager.canDelete( user, pi, false ) );
        // Can read enrollment
        assertNoErrors( trackerAccessManager.canRead( user, pi, false ) );
    }

    @Test
    void checkAccessPermissionForEnrollmentInOpenProgram()
    {
        programA.setPublicAccess( AccessStringHelper.FULL );
        programA.setAccessLevel( AccessLevel.OPEN );
        manager.update( programA );
        trackedEntityType.setPublicAccess( AccessStringHelper.FULL );
        manager.update( trackedEntityType );
        User user = createUserWithAuth( "user1" ).setOrganisationUnits( Sets.newHashSet( organisationUnitB ) );
        user.setTeiSearchOrganisationUnits( Sets.newHashSet( organisationUnitA, organisationUnitB ) );
        TrackedEntityInstance tei = trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() );
        ProgramInstance pi = tei.getProgramInstances().iterator().next();
        // Cannot create enrollment if enrollmentOU falls outside capture scope
        assertHasError( trackerAccessManager.canCreate( user, pi, false ) );
        // Can update enrollment if ownerOU falls inside search scope
        assertNoErrors( trackerAccessManager.canUpdate( user, pi, false ) );
        // Can delete enrollment if ownerOU falls inside search scope
        assertNoErrors( trackerAccessManager.canDelete( user, pi, false ) );
        // Can read enrollment if ownerOU falls inside search scope
        assertNoErrors( trackerAccessManager.canRead( user, pi, false ) );
        // Transferring ownership to orgUnitB. user is now owner
        trackerOwnershipManager.transferOwnership( tei, programA, organisationUnitB, true, true );
        // Cannot create enrollment if enrollmentOU falls outside capture scope,
        // even if user is owner
        assertHasError( trackerAccessManager.canCreate( user, pi, false ),
            "User has no create access to organisation unit:" );
        // Can update enrollment
        assertNoErrors( trackerAccessManager.canUpdate( user, pi, false ) );
        // Can delete enrollment
        assertNoErrors( trackerAccessManager.canDelete( user, pi, false ) );
        // Can read enrollment
        assertNoErrors( trackerAccessManager.canRead( user, pi, false ) );
        // Transferring ownership to orgUnitB. user is now owner
        trackerOwnershipManager.transferOwnership( tei, programA, organisationUnitA, true, true );
        user.setTeiSearchOrganisationUnits( Sets.newHashSet( organisationUnitA, organisationUnitB ) );
        // Cannot create enrollment if enrollment OU is outside capture scope
        assertHasError( trackerAccessManager.canCreate( user, pi, false ),
            "User has no create access to organisation unit:" );
        // Can update enrollment if ownerOU is in search scope
        assertNoErrors( trackerAccessManager.canUpdate( user, pi, false ) );
        // Can delete enrollment if ownerOU is in search scope
        assertNoErrors( trackerAccessManager.canDelete( user, pi, false ) );
        // Can read enrollment if ownerOU is in search scope
        assertNoErrors( trackerAccessManager.canRead( user, pi, false ) );
    }

    @Test
    void checkAccessPermissionsForEventInClosedProgram()
    {
        programA.setPublicAccess( AccessStringHelper.FULL );
        programStageA.setPublicAccess( AccessStringHelper.FULL );
        programStageB.setPublicAccess( AccessStringHelper.FULL );
        manager.update( programStageA );
        manager.update( programStageB );
        manager.update( programA );
        User user = createUserWithAuth( "user1" ).setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        user.setTeiSearchOrganisationUnits( Sets.newHashSet( organisationUnitA, organisationUnitB ) );
        trackedEntityType.setPublicAccess( AccessStringHelper.FULL );
        manager.update( trackedEntityType );
        TrackedEntityInstance tei = trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() );
        ProgramInstance pi = tei.getProgramInstances().iterator().next();
        // Scheduled event on orgUnitB
        ProgramStageInstance psi = pi.getProgramStageInstanceByStage( 2 );
        if ( psi.getStatus() != EventStatus.SCHEDULE )
        {
            psi = pi.getProgramStageInstanceByStage( 1 );
        }
        // Can create scheduled events outside capture scope if user is owner
        assertNoErrors( trackerAccessManager.canCreate( user, psi, false ) );
        // Cannot create regular events outside capture scope even if user is
        // owner
        psi.setStatus( EventStatus.ACTIVE );
        assertHasError( trackerAccessManager.canCreate( user, psi, false ),
            "User has no create access to organisation unit:" );
        // Can read events if user is owner irrespective of eventOU
        assertNoErrors( trackerAccessManager.canRead( user, psi, false ) );
        // Can update events if user is owner irrespective of eventOU
        assertNoErrors( trackerAccessManager.canUpdate( user, psi, false ) );
        // Can delete events if user is owner irrespective of eventOU
        assertNoErrors( trackerAccessManager.canDelete( user, psi, false ) );
        trackerOwnershipManager.transferOwnership( tei, programA, organisationUnitB, true, true );
        // Cannot create events anywhere if user is not owner
        assertHasErrors( 2, trackerAccessManager.canCreate( user, psi, false ) );
        // Cannot read events if user is not owner (OwnerOU falls into capture
        // scope)
        assertHasError( trackerAccessManager.canRead( user, psi, false ), "OWNERSHIP_ACCESS_DENIED" );
        // Cannot update events if user is not owner (OwnerOU falls into capture
        // scope)
        assertHasError( trackerAccessManager.canUpdate( user, psi, false ), "OWNERSHIP_ACCESS_DENIED" );
        // Cannot delete events if user is not owner (OwnerOU falls into capture
        // scope)
        assertHasError( trackerAccessManager.canDelete( user, psi, false ), "OWNERSHIP_ACCESS_DENIED" );
    }

    @Test
    void checkAccessPermissionsForEventInOpenProgram()
    {
        programA.setPublicAccess( AccessStringHelper.FULL );
        programA.setAccessLevel( AccessLevel.OPEN );
        programStageA.setPublicAccess( AccessStringHelper.FULL );
        programStageB.setPublicAccess( AccessStringHelper.FULL );
        manager.update( programStageA );
        manager.update( programStageB );
        manager.update( programA );
        User user = createUserWithAuth( "user1" ).setOrganisationUnits( Sets.newHashSet( organisationUnitB ) );
        user.setTeiSearchOrganisationUnits( Sets.newHashSet( organisationUnitA, organisationUnitB ) );
        trackedEntityType.setPublicAccess( AccessStringHelper.FULL );
        manager.update( trackedEntityType );
        TrackedEntityInstance tei = trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() );
        ProgramInstance pi = tei.getProgramInstances().iterator().next();
        // Active event on orgUnitA
        ProgramStageInstance psi = pi.getProgramStageInstanceByStage( 2 );
        if ( psi.getStatus() == EventStatus.SCHEDULE )
        {
            psi = pi.getProgramStageInstanceByStage( 1 );
        }
        // Cannot create events with evemntOu outside capture scope
        assertHasError( trackerAccessManager.canCreate( user, psi, false ),
            "User has no create access to organisation unit:" );
        // Can read events if ownerOu falls into users search scope
        assertNoErrors( trackerAccessManager.canRead( user, psi, false ) );
        // Can update events if ownerOu falls into users search scope
        assertNoErrors( trackerAccessManager.canUpdate( user, psi, false ) );
        // Can delete events if ownerOu falls into users search scope
        assertNoErrors( trackerAccessManager.canDelete( user, psi, false ) );
        trackerOwnershipManager.transferOwnership( tei, programA, organisationUnitB, true, true );
        // Cannot create events with eventOu outside capture scope, even if
        // ownerOu is
        // also in capture scope
        assertHasError( trackerAccessManager.canCreate( user, psi, false ),
            "User has no create access to organisation unit:" );
        // Can read events if ownerOu falls into users capture scope
        assertNoErrors( trackerAccessManager.canRead( user, psi, false ) );
        // Can update events if ownerOu falls into users capture scope
        assertNoErrors( trackerAccessManager.canUpdate( user, psi, false ) );
        // Can delete events if ownerOu falls into users capture scope
        assertNoErrors( trackerAccessManager.canDelete( user, psi, false ) );
    }

    private Enrollment createEnrollment( String program, String person )
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( CodeGenerator.generateUid() );
        enrollment.setOrgUnit( organisationUnitA.getUid() );
        enrollment.setProgram( program );
        enrollment.setTrackedEntityInstance( person );
        enrollment.setEnrollmentDate( new Date() );
        enrollment.setIncidentDate( new Date() );
        Event event1 = new Event();
        event1.setEnrollment( enrollment.getEnrollment() );
        event1
            .setEventDate( DateTimeFormatter.ofPattern( "yyyy-MM-dd", Locale.ENGLISH ).format( LocalDateTime.now() ) );
        event1.setProgram( programA.getUid() );
        event1.setProgramStage( programStageA.getUid() );
        event1.setStatus( EventStatus.COMPLETED );
        event1.setTrackedEntityInstance( maleA.getUid() );
        event1.setOrgUnit( organisationUnitA.getUid() );
        Event event2 = new Event();
        event2.setEnrollment( enrollment.getEnrollment() );
        event2.setDueDate(
            DateTimeFormatter.ofPattern( "yyyy-MM-dd", Locale.ENGLISH ).format( LocalDateTime.now().plusDays( 10 ) ) );
        event2.setProgram( programA.getUid() );
        event2.setProgramStage( programStageB.getUid() );
        event2.setStatus( EventStatus.SCHEDULE );
        event2.setTrackedEntityInstance( maleA.getUid() );
        event2.setOrgUnit( organisationUnitB.getUid() );
        enrollment.setEvents( Arrays.asList( event1, event2 ) );
        return enrollment;
    }

    private void assertNoErrors( List<String> errors )
    {
        assertEquals( 0, errors.size() );
    }

    private void assertHasError( List<String> errors, String error )
    {
        assertEquals( 1, errors.size() );
        assertTrue( errors.get( 0 ).contains( error ) );
    }

    private void assertHasError( List<String> errors )
    {
        assertEquals( 1, errors.size() );
    }

    private void assertHasErrors( int errorNumber, List<String> errors )
    {
        assertEquals( errorNumber, errors.size() );
    }
}
