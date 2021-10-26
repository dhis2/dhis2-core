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
package org.hisp.dhis.tracker.converter;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Sets;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@RunWith( MockitoJUnitRunner.class )
public class EventTrackerConverterServiceTest
    extends DhisConvenienceTest
{
    private final static String PROGRAM_INSTANCE_UID = "programInstanceUid";

    private final static String PROGRAM_STAGE_UID = "ProgramStageUid";

    private final static String ORGANISATION_UNIT_UID = "OrganisationUnitUid";

    private final static String PROGRAM_UID = "ProgramUid";

    private final static String USERNAME = "usernameU";

    private final static Date today = new Date();

    private NotesConverterService notesConverterService = new NotesConverterService();

    private TrackerConverterService<Event, ProgramStageInstance> trackerConverterService;

    @Mock
    public TrackerPreheat preheat;

    private Program program = createProgram( 'A' );

    private ProgramInstance programInstance;

    private ProgramStageInstance psi;

    private TrackedEntityInstance tei;

    private DataElement dataElement;

    private User user;

    @Before
    public void setUpTest()
    {
        trackerConverterService = new EventTrackerConverterService( notesConverterService );

        dataElement = createDataElement( 'D' );

        user = createUser( 'U' );

        ProgramStage programStage = createProgramStage( 'A', 1 );
        programStage.setUid( PROGRAM_STAGE_UID );

        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        organisationUnit.setUid( ORGANISATION_UNIT_UID );

        program.setUid( PROGRAM_UID );
        program.setProgramType( ProgramType.WITHOUT_REGISTRATION );

        programStage.setProgram( program );

        tei = createTrackedEntityInstance( organisationUnit );
        programInstance = createProgramInstance( program, tei, organisationUnit );
        programInstance.setUid( PROGRAM_INSTANCE_UID );

        psi = new ProgramStageInstance();
        psi.setAutoFields();
        psi.setAttributeOptionCombo( createCategoryOptionCombo( 'C' ) );
        psi.setCreated( today );
        psi.setExecutionDate( today );
        psi.setProgramInstance( programInstance );
        psi.setOrganisationUnit( organisationUnit );
        psi.setProgramStage( programStage );
        psi.setEventDataValues( Sets.newHashSet() );
        psi.setDueDate( null );
        psi.setCompletedDate( null );
        psi.setStoredBy( user.getUsername() );
        psi.setLastUpdatedByUserInfo( UserInfoSnapshot.from( user ) );
        psi.setCreatedByUserInfo( UserInfoSnapshot.from( user ) );

        when( preheat.getUsers() ).thenReturn( Collections.singletonMap( USERNAME, user ) );
        when( preheat.get( ProgramStage.class, programStage.getUid() ) ).thenReturn( programStage );
        when( preheat.get( Program.class, program.getUid() ) ).thenReturn( program );
        when( preheat.get( OrganisationUnit.class, organisationUnit.getUid() ) ).thenReturn( organisationUnit );
        when( preheat.getUser() ).thenReturn( user );
    }

    @Test
    public void testToProgramStageInstance()
    {
        Event event = new Event();
        event.setProgramStage( PROGRAM_STAGE_UID );
        event.setProgram( PROGRAM_UID );
        event.setOrgUnit( ORGANISATION_UNIT_UID );

        DataValue dataValue = new DataValue();
        dataValue.setValue( "value" );
        dataValue.setCreatedBy( USERNAME );
        dataValue.setLastUpdatedBy( USERNAME );
        dataValue.setCreatedAt( Instant.now() );
        dataValue.setStoredBy( USERNAME );
        dataValue.setUpdatedAt( Instant.now() );
        dataValue.setDataElement( dataElement.getUid() );

        event.setDataValues( Sets.newHashSet( dataValue ) );

        ProgramStageInstance programStageInstance = trackerConverterService.from( preheat, event );

        assertNotNull( programStageInstance );
        assertNotNull( programStageInstance.getProgramStage() );
        assertNotNull( programStageInstance.getProgramStage().getProgram() );
        assertNotNull( programStageInstance.getOrganisationUnit() );

        assertEquals( PROGRAM_UID, programStageInstance.getProgramStage().getProgram().getUid() );
        assertEquals( PROGRAM_STAGE_UID, programStageInstance.getProgramStage().getUid() );
        assertEquals( ORGANISATION_UNIT_UID, programStageInstance.getOrganisationUnit().getUid() );
        assertEquals( ORGANISATION_UNIT_UID, programStageInstance.getOrganisationUnit().getUid() );

        Set<EventDataValue> eventDataValues = programStageInstance.getEventDataValues();

        eventDataValues.forEach( e -> {
            assertEquals( USERNAME, e.getCreatedByUserInfo().getUsername() );
            assertEquals( USERNAME, e.getLastUpdatedByUserInfo().getUsername() );
        } );
    }

    @Test
    public void testToEvent()
    {
        EventDataValue eventDataValue = new EventDataValue();
        eventDataValue.setAutoFields();
        eventDataValue.setCreated( today );
        eventDataValue.setValue( "sample-value" );
        eventDataValue.setDataElement( dataElement.getUid() );
        eventDataValue.setStoredBy( user.getUsername() );
        eventDataValue.setCreatedByUserInfo( UserInfoSnapshot.from( user ) );
        eventDataValue.setLastUpdatedByUserInfo( UserInfoSnapshot.from( user ) );
        psi.getEventDataValues().add( eventDataValue );

        Event event = trackerConverterService.to( psi );

        assertEquals( event.getEnrollment(), PROGRAM_INSTANCE_UID );
        assertEquals( event.getStoredBy(), user.getUsername() );

        event.getDataValues().forEach( e -> {

            assertEquals( DateUtils.fromInstant( e.getCreatedAt() ), psi.getCreated() );
            assertEquals( e.getLastUpdatedBy(), psi.getLastUpdatedByUserInfo().getUsername() );
            assertEquals( e.getLastUpdatedBy(), psi.getCreatedByUserInfo().getUsername() );
        } );
    }
}
