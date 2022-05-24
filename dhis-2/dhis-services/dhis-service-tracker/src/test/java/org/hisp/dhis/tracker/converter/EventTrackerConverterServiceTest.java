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
package org.hisp.dhis.tracker.converter;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;
import java.util.Set;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.CodeGenerator;
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
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.User;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@ExtendWith( MockitoExtension.class )
class EventTrackerConverterServiceTest extends DhisConvenienceTest
{

    private final static String PROGRAM_INSTANCE_UID = "programInstanceUid";

    private final static String PROGRAM_STAGE_UID = "ProgramStageUid";

    private final static String ORGANISATION_UNIT_UID = "OrganisationUnitUid";

    private final static String PROGRAM_UID = "ProgramUid";

    private final static String USERNAME = "usernameu";

    private final static Date today = new Date();

    private final NotesConverterService notesConverterService = new NotesConverterService();

    private RuleEngineConverterService<Event, ProgramStageInstance> converter;

    @Mock
    public TrackerPreheat preheat;

    private final Program program = createProgram( 'A' );

    private ProgramStage programStage;

    private OrganisationUnit organisationUnit;

    private ProgramStageInstance psi;

    private DataElement dataElement;

    private org.hisp.dhis.user.User user;

    @BeforeEach
    void setUpTest()
    {
        converter = new EventTrackerConverterService( notesConverterService );
        dataElement = createDataElement( 'D' );
        user = makeUser( "U" );
        programStage = createProgramStage( 'A', 1 );
        programStage.setUid( PROGRAM_STAGE_UID );
        programStage.setProgram( program );
        organisationUnit = createOrganisationUnit( 'A' );
        organisationUnit.setUid( ORGANISATION_UNIT_UID );
        program.setUid( PROGRAM_UID );
        program.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        TrackedEntityInstance tei = createTrackedEntityInstance( organisationUnit );
        ProgramInstance programInstance = createProgramInstance( program, tei, organisationUnit );
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
    }

    @Test
    void testToProgramStageInstance()
    {
        setUpMocks();

        DataElement dataElement = new DataElement();
        dataElement.setUid( CodeGenerator.generateUid() );
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElement.getUid() ) ) ).thenReturn( dataElement );

        User user = User.builder().username( USERNAME ).build();

        DataValue dataValue = new DataValue();
        dataValue.setValue( "value" );
        dataValue.setCreatedBy( user );
        dataValue.setUpdatedBy( user );
        dataValue.setCreatedAt( Instant.now() );
        dataValue.setStoredBy( USERNAME );
        dataValue.setUpdatedAt( Instant.now() );
        dataValue.setDataElement( MetadataIdentifier.ofUid( dataElement.getUid() ) );
        Event event = event( dataValue );

        ProgramStageInstance programStageInstance = converter.from( preheat, event );

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
    void fromForRuleEngineGivenNewEvent()
    {
        setUpMocks();

        DataElement dataElement = new DataElement();
        dataElement.setUid( CodeGenerator.generateUid() );
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElement.getUid() ) ) ).thenReturn( dataElement );

        DataValue dataValue = dataValue( dataElement.getUid(), "900" );
        Event event = event( dataValue );

        ProgramStageInstance programStageInstance = converter.fromForRuleEngine( preheat, event );

        assertNotNull( programStageInstance );
        assertNotNull( programStageInstance.getProgramStage() );
        assertNotNull( programStageInstance.getProgramStage().getProgram() );
        assertNotNull( programStageInstance.getOrganisationUnit() );
        assertEquals( PROGRAM_UID, programStageInstance.getProgramStage().getProgram().getUid() );
        assertEquals( PROGRAM_STAGE_UID, programStageInstance.getProgramStage().getUid() );
        assertEquals( ORGANISATION_UNIT_UID, programStageInstance.getOrganisationUnit().getUid() );
        assertEquals( ORGANISATION_UNIT_UID, programStageInstance.getOrganisationUnit().getUid() );
        assertEquals( 1, programStageInstance.getEventDataValues().size() );
        EventDataValue actual = programStageInstance.getEventDataValues().stream().findFirst().get();
        assertEquals( dataValue.getDataElement(), MetadataIdentifier.ofUid( actual.getDataElement() ) );
        assertEquals( dataValue.getValue(), actual.getValue() );
        assertTrue( actual.getProvidedElsewhere() );
        assertEquals( USERNAME, actual.getCreatedByUserInfo().getUsername() );
        assertEquals( USERNAME, actual.getLastUpdatedByUserInfo().getUsername() );
    }

    @Test
    void fromForRuleEngineGivenExistingEventMergesNewDataValuesWithDBOnes()
    {
        setUpMocks();

        ProgramStageInstance existingPsi = programStageInstance();
        EventDataValue existingDataValue = eventDataValue( CodeGenerator.generateUid(), "658" );
        existingPsi.setEventDataValues( Set.of( existingDataValue ) );

        DataElement dataElement = new DataElement();
        dataElement.setUid( CodeGenerator.generateUid() );
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElement.getUid() ) ) ).thenReturn( dataElement );

        // event refers to a different dataElement then currently associated
        // with the event in the DB; thus both
        // dataValues will be merged
        DataValue newDataValue = dataValue( dataElement.getUid(), "900" );
        Event event = event( existingPsi.getUid(), newDataValue );
        when( preheat.getEvent( existingPsi.getUid() ) ).thenReturn( existingPsi );

        ProgramStageInstance programStageInstance = converter.fromForRuleEngine( preheat, event );

        assertEquals( 2, programStageInstance.getEventDataValues().size() );
        EventDataValue expect1 = new EventDataValue();
        expect1.setDataElement( existingDataValue.getDataElement() );
        expect1.setValue( existingDataValue.getValue() );
        EventDataValue expect2 = new EventDataValue();
        expect2.setDataElement( dataElement.getUid() );
        expect2.setValue( newDataValue.getValue() );
        assertContainsOnly( programStageInstance.getEventDataValues(), expect1, expect2 );
    }

    @Test
    void fromForRuleEngineGivenExistingEventUpdatesValueOfExistingDataValueOnIdSchemeUID()
    {
        setUpMocks();

        DataElement dataElement = new DataElement();
        dataElement.setUid( CodeGenerator.generateUid() );
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElement.getUid() ) ) ).thenReturn( dataElement );

        ProgramStageInstance existingPsi = programStageInstance();
        existingPsi.setEventDataValues( Set.of( eventDataValue( dataElement.getUid(), "658" ) ) );

        // dataElement is of idScheme UID if the NTI dataElementIdScheme is set
        // to UID
        DataValue updatedValue = dataValue( dataElement.getUid(), "900" );
        Event event = event( existingPsi.getUid(), updatedValue );
        when( preheat.getEvent( event.getEvent() ) ).thenReturn( existingPsi );

        ProgramStageInstance programStageInstance = converter.fromForRuleEngine( preheat, event );

        assertEquals( 1, programStageInstance.getEventDataValues().size() );
        EventDataValue expect1 = new EventDataValue();
        expect1.setDataElement( dataElement.getUid() );
        expect1.setValue( updatedValue.getValue() );
        assertContainsOnly( programStageInstance.getEventDataValues(), expect1 );
    }

    @Test
    void fromForRuleEngineGivenExistingEventUpdatesValueOfExistingDataValueOnIdSchemeCode()
    {
        // NTI supports multiple idSchemes. Event.dataElement can thus be any of
        // the supported ones
        // UID, CODE, ATTRIBUTE, NAME
        // merging existing & new data values on events needs to respect the
        // user configured idScheme
        setUpMocks();

        DataElement dataElement = new DataElement();
        dataElement.setUid( CodeGenerator.generateUid() );
        dataElement.setCode( "DE_424050" );
        when( preheat.getDataElement( MetadataIdentifier.ofCode( dataElement.getCode() ) ) ).thenReturn( dataElement );

        ProgramStageInstance existingPsi = programStageInstance();
        existingPsi.setEventDataValues( Set.of( eventDataValue( dataElement.getUid(), "658" ) ) );

        // dataElement is of idScheme CODE if the NTI dataElementIdScheme is set
        // to CODE
        DataValue updatedValue = dataValue( dataElement.getCode(), "900" );
        Event event = event( existingPsi.getUid(), updatedValue );
        when( preheat.getEvent( event.getEvent() ) ).thenReturn( existingPsi );

        ProgramStageInstance programStageInstance = converter.fromForRuleEngine( preheat, event );

        assertEquals( 1, programStageInstance.getEventDataValues().size() );
        EventDataValue expect1 = new EventDataValue();
        expect1.setDataElement( dataElement.getUid() );
        expect1.setValue( updatedValue.getValue() );
        assertContainsOnly( programStageInstance.getEventDataValues(), expect1 );
    }

    @Test
    void testToEvent()
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

        Event event = converter.to( psi );

        assertEquals( PROGRAM_INSTANCE_UID, event.getEnrollment() );
        assertEquals( event.getStoredBy(), user.getUsername() );
        event.getDataValues().forEach( e -> {
            assertEquals( DateUtils.fromInstant( e.getCreatedAt() ), psi.getCreated() );
            assertEquals( e.getUpdatedBy().getUsername(), psi.getLastUpdatedByUserInfo().getUsername() );
            assertEquals( e.getUpdatedBy().getUsername(), psi.getCreatedByUserInfo().getUsername() );
        } );
    }

    private void setUpMocks()
    {
        when( preheat.getUser() ).thenReturn( user );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStage ) ) )
            .thenReturn( programStage );
        when( preheat.getProgram( MetadataIdentifier.ofUid( program ) ) ).thenReturn( program );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( organisationUnit ) ) )
            .thenReturn( organisationUnit );
    }

    private Event event( DataValue dataValue )
    {
        return event( null, dataValue );
    }

    private Event event( String uid, DataValue dataValue )
    {
        return Event.builder()
            .event( uid )
            .programStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_UID ) )
            .program( MetadataIdentifier.ofUid( PROGRAM_UID ) )
            .orgUnit( MetadataIdentifier.ofUid( ORGANISATION_UNIT_UID ) )
            .attributeOptionCombo( MetadataIdentifier.EMPTY_UID )
            .dataValues( Sets.newHashSet( dataValue ) )
            .build();
    }

    private ProgramStageInstance programStageInstance()
    {
        ProgramStageInstance existingPsi = new ProgramStageInstance();
        existingPsi.setUid( CodeGenerator.generateUid() );
        return existingPsi;
    }

    private EventDataValue eventDataValue( String dataElement, String value )
    {
        EventDataValue eventDataValue = new EventDataValue();
        eventDataValue.setDataElement( dataElement );
        eventDataValue.setValue( value );
        return eventDataValue;
    }

    private DataValue dataValue( String dataElement, String value )
    {
        User user = User.builder().username( USERNAME ).build();

        return DataValue.builder()
            .dataElement( MetadataIdentifier.ofUid( dataElement ) )
            .value( value )
            .providedElsewhere( true )
            .createdBy( user )
            .updatedBy( user )
            .createdAt( Instant.now() )
            .storedBy( USERNAME )
            .updatedAt( Instant.now() )
            .build();
    }

}
