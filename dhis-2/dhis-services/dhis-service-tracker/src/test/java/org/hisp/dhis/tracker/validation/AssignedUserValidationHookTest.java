package org.hisp.dhis.tracker.validation;

/*
 * Copyright (c) 2004-2021, University of Oslo
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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.AtomicMode;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.util.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AssignedUserValidationHookTest
    extends AbstractImportValidationTest
{
    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private TrackerImportService trackerImportService;

    private TrackedEntityType teta;

    private OrganisationUnit organisationUnitA;

    private Program programA;

    private ProgramStage programStageA;

    private User user;

    private ProgramInstance pi;

    @Before
    public void setup()
    {
        organisationUnitA = createOrganisationUnit( 'A' );
        manager.save( organisationUnitA );

        user = new User();
        UserCredentials userCredentials = new UserCredentials();

        user.setAutoFields();
        user.setUserCredentials( userCredentials );
        user.setSurname( "Nordmann" );
        user.setFirstName( "Ola" );
        user.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );

        userCredentials.setAutoFields();
        userCredentials.setUserInfo( user );
        user.setUserCredentials( userCredentials );

        _userService.addUser( user );

        teta = createTrackedEntityType( 'A' );
        manager.save( teta );

        programA = createProgram( 'A' );
        programA.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        programA.setTrackedEntityType( teta );
        programA.addOrganisationUnit( organisationUnitA );
        manager.save( programA );
        programA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        manager.update( programA );

        programStageA = createProgramStage( 'A', programA );
        programStageA.setEnableUserAssignment( true );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        manager.save( programStageA );

        pi = new ProgramInstance();
        pi.setProgram( programA );
        pi.setOrganisationUnit( organisationUnitA );
        pi.setEnrollmentDate( new Date() );
        pi.setIncidentDate( new Date() );

        manager.save( pi );
    }

    @Test
    public void testAssignedUserInvalidUid()
    {
        Event event = new Event();

        String testUserUid = "123";

        event.setEvent( "EVENTUID001" );
        event.setAssignedUser( testUserUid );
        event.setProgram( programA.getUid() );
        event.setProgramStage( programStageA.getUid() );
        event.setOrgUnit( organisationUnitA.getUid() );
        event.setEnrollment( pi.getUid() );
        event.setOccurredAt( DateUtils.instantFromDateAsString( "1990-10-22" ) );
        event.setCreatedAt( DateUtils.instantFromDateAsString( "2010-10-22" ) );
        event.setScheduledAt( DateUtils.instantFromDateAsString( "2010-10-22" ) );

        TrackerImportParams params = TrackerImportParams.builder()
            .atomicMode( AtomicMode.ALL )
            .events( Lists.newArrayList( event ) )
            .importStrategy( TrackerImportStrategy.CREATE_AND_UPDATE )
            .userId( user.getUid() )
            .user( user )
            .build();

        TrackerImportReport report = trackerImportService.importTracker( params );

        assertEquals( 1, report.getValidationReport().getErrorReports().size() );
        assertEquals( "Assigned user `123` is not a valid uid.", report.getValidationReport().getErrorReports().get( 0 ).getMessage() );
        assertEquals( TrackerErrorCode.E1118, report.getValidationReport().getErrorReports().get( 0 ).getErrorCode() );
    }

    @Test
    public void testAssignedUserDoesNotExist()
    {
        Event event = new Event();

        String testUserUid = "A01234567890";

        event.setEvent( "EVENTUID001" );
        event.setAssignedUser( testUserUid );
        event.setProgram( programA.getUid() );
        event.setProgramStage( programStageA.getUid() );
        event.setOrgUnit( organisationUnitA.getUid() );
        event.setEnrollment( pi.getUid() );
        event.setOccurredAt( DateUtils.instantFromDateAsString( "1990-10-22" ) );
        event.setScheduledAt( DateUtils.instantFromDateAsString( "2010-10-22" ) );
        event.setCreatedAt( DateUtils.instantFromDateAsString( "2010-10-22" ) );

        TrackerImportParams params = TrackerImportParams.builder()
            .atomicMode( AtomicMode.ALL )
            .events( Lists.newArrayList( event ) )
            .importStrategy( TrackerImportStrategy.CREATE_AND_UPDATE )
            .userId( user.getUid() )
            .user( user )
            .build();

        TrackerImportReport report = trackerImportService.importTracker( params );

        assertEquals( 1, report.getValidationReport().getErrorReports().size() );
        assertEquals( "Assigned user `A01234567890` is not a valid uid.", report.getValidationReport().getErrorReports().get( 0 ).getMessage() );
        assertEquals( TrackerErrorCode.E1118, report.getValidationReport().getErrorReports().get( 0 ).getErrorCode() );
    }

    @Test
    public void testAssignedUserExists()
    {
        Event event = new Event();

        event.setEvent( CodeGenerator.generateUid() );
        event.setAssignedUser( user.getUid() );
        event.setProgram( programA.getUid() );
        event.setProgramStage( programStageA.getUid() );
        event.setOrgUnit( organisationUnitA.getUid() );
        event.setEnrollment( pi.getUid() );
        event.setOccurredAt( DateUtils.instantFromDateAsString( "1990-10-22" ) );

        TrackerImportParams params = TrackerImportParams.builder()
            .atomicMode( AtomicMode.ALL )
            .events( Lists.newArrayList( event ) )
            .importStrategy( TrackerImportStrategy.CREATE_AND_UPDATE )
            .userId( user.getUid() )
            .user( user )
            .build();

        TrackerImportReport report = trackerImportService.importTracker( params );

        assertTrue( CollectionUtils.isEmpty( report.getValidationReport().getErrorReports() ) );
    }

    @Test
    public void testAssignedUserIsNull()
    {
        Event event = new Event();

        event.setAssignedUser( null );
        event.setProgram( programA.getUid() );
        event.setProgramStage( programStageA.getUid() );
        event.setOrgUnit( organisationUnitA.getUid() );
        event.setEnrollment( pi.getUid() );
        event.setOccurredAt( DateUtils.instantFromDateAsString( "1990-10-22" ) );

        TrackerImportParams params = TrackerImportParams.builder()
            .atomicMode( AtomicMode.ALL )
            .events( Lists.newArrayList( event ) )
            .importStrategy( TrackerImportStrategy.CREATE_AND_UPDATE )
            .userId( user.getUid() )
            .user( user )
            .build();

        TrackerImportReport report = trackerImportService.importTracker( params );

        assertTrue( report.getValidationReport().getErrorReports().isEmpty() );
    }

}
