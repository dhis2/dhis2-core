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
package org.hisp.dhis.tracker.imports.converter;

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
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.User;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
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

    private final static String ENROLLMENT_UID = "enrollmentUid";

    private final static String PROGRAM_STAGE_UID = "ProgramStageUid";

    private final static String ORGANISATION_UNIT_UID = "OrganisationUnitUid";

    private final static String PROGRAM_UID = "ProgramUid";

    private final static String USERNAME = "usernameu";

    private final static Date today = new Date();

    private final NotesConverterService notesConverterService = new NotesConverterService();

    private RuleEngineConverterService<org.hisp.dhis.tracker.imports.domain.Event, Event> converter;

    @Mock
    public TrackerPreheat preheat;

    private final Program program = createProgram( 'A' );

    private ProgramStage programStage;

    private OrganisationUnit organisationUnit;

    private Event event;

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
        TrackedEntity tei = createTrackedEntityInstance( organisationUnit );
        Enrollment enrollment = createEnrollment( program, tei, organisationUnit );
        enrollment.setUid( ENROLLMENT_UID );
        event = new Event();
        event.setAutoFields();
        event.setAttributeOptionCombo( createCategoryOptionCombo( 'C' ) );
        event.setCreated( today );
        event.setExecutionDate( today );
        event.setEnrollment( enrollment );
        event.setOrganisationUnit( organisationUnit );
        event.setProgramStage( programStage );
        event.setEventDataValues( Sets.newHashSet() );
        event.setDueDate( null );
        event.setCompletedDate( null );
        event.setStoredBy( user.getUsername() );
        event.setLastUpdatedByUserInfo( UserInfoSnapshot.from( user ) );
        event.setCreatedByUserInfo( UserInfoSnapshot.from( user ) );
    }

    @Test
    void testFromEvent()
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
        org.hisp.dhis.tracker.imports.domain.Event event = event( dataValue );

        Event result = converter.from( preheat, event );

        assertNotNull( result );
        assertNotNull( result.getProgramStage() );
        assertNotNull( result.getProgramStage().getProgram() );
        assertNotNull( result.getOrganisationUnit() );
        assertEquals( PROGRAM_UID, result.getProgramStage().getProgram().getUid() );
        assertEquals( PROGRAM_STAGE_UID, result.getProgramStage().getUid() );
        assertEquals( ORGANISATION_UNIT_UID, result.getOrganisationUnit().getUid() );
        assertEquals( ORGANISATION_UNIT_UID, result.getOrganisationUnit().getUid() );
        Set<EventDataValue> eventDataValues = result.getEventDataValues();
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
        MetadataIdentifier metadataIdentifier = MetadataIdentifier.ofUid( dataElement.getUid() );
        when( preheat.getDataElement( metadataIdentifier ) ).thenReturn( dataElement );

        DataValue dataValue = dataValue( metadataIdentifier, "900" );
        org.hisp.dhis.tracker.imports.domain.Event event = event( dataValue );

        Event result = converter.fromForRuleEngine( preheat, event );

        assertNotNull( result );
        assertNotNull( result.getProgramStage() );
        assertNotNull( result.getProgramStage().getProgram() );
        assertNotNull( result.getOrganisationUnit() );
        assertEquals( PROGRAM_UID, result.getProgramStage().getProgram().getUid() );
        assertEquals( PROGRAM_STAGE_UID, result.getProgramStage().getUid() );
        assertEquals( ORGANISATION_UNIT_UID, result.getOrganisationUnit().getUid() );
        assertEquals( ORGANISATION_UNIT_UID, result.getOrganisationUnit().getUid() );
        assertEquals( 1, result.getEventDataValues().size() );
        EventDataValue actual = result.getEventDataValues().stream().findFirst().get();
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

        Event existingEvent = event();
        EventDataValue existingDataValue = eventDataValue( CodeGenerator.generateUid(), "658" );
        existingEvent.setEventDataValues( Set.of( existingDataValue ) );

        DataElement dataElement = new DataElement();
        dataElement.setUid( CodeGenerator.generateUid() );
        MetadataIdentifier metadataIdentifier = MetadataIdentifier.ofUid( dataElement.getUid() );
        when( preheat.getDataElement( metadataIdentifier ) ).thenReturn( dataElement );

        // event refers to a different dataElement then currently associated
        // with the event in the DB; thus both
        // dataValues will be merged
        DataValue newDataValue = dataValue( metadataIdentifier, "900" );
        org.hisp.dhis.tracker.imports.domain.Event event = event( existingEvent.getUid(), newDataValue );
        when( preheat.getEvent( existingEvent.getUid() ) ).thenReturn( existingEvent );

        Event result = converter.fromForRuleEngine( preheat, event );

        assertEquals( 2, result.getEventDataValues().size() );
        EventDataValue expect1 = new EventDataValue();
        expect1.setDataElement( existingDataValue.getDataElement() );
        expect1.setValue( existingDataValue.getValue() );
        EventDataValue expect2 = new EventDataValue();
        expect2.setDataElement( dataElement.getUid() );
        expect2.setValue( newDataValue.getValue() );
        assertContainsOnly( Set.of( expect1, expect2 ), result.getEventDataValues() );
    }

    @Test
    void fromForRuleEngineGivenExistingEventUpdatesValueOfExistingDataValueOnIdSchemeUID()
    {
        setUpMocks();

        DataElement dataElement = new DataElement();
        dataElement.setUid( CodeGenerator.generateUid() );
        MetadataIdentifier metadataIdentifier = MetadataIdentifier.ofUid( dataElement.getUid() );
        when( preheat.getDataElement( metadataIdentifier ) ).thenReturn( dataElement );

        Event existingEvent = event();
        existingEvent.setEventDataValues( Set.of( eventDataValue( dataElement.getUid(), "658" ) ) );

        // dataElement is of idScheme UID if the NTI dataElementIdScheme is set
        // to UID
        DataValue updatedValue = dataValue( metadataIdentifier, "900" );
        org.hisp.dhis.tracker.imports.domain.Event event = event( existingEvent.getUid(), updatedValue );
        when( preheat.getEvent( event.getEvent() ) ).thenReturn( existingEvent );

        Event result = converter.fromForRuleEngine( preheat, event );

        assertEquals( 1, result.getEventDataValues().size() );
        EventDataValue expect1 = new EventDataValue();
        expect1.setDataElement( dataElement.getUid() );
        expect1.setValue( updatedValue.getValue() );
        assertContainsOnly( Set.of( expect1 ), result.getEventDataValues() );
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

        Event existingEvent = event();
        existingEvent.setEventDataValues( Set.of( eventDataValue( dataElement.getUid(), "658" ) ) );

        // dataElement is of idScheme CODE if the NTI dataElementIdScheme is set
        // to CODE
        DataValue updatedValue = dataValue( MetadataIdentifier.ofCode( dataElement.getCode() ), "900" );
        org.hisp.dhis.tracker.imports.domain.Event event = event( existingEvent.getUid(), updatedValue );
        when( preheat.getEvent( event.getEvent() ) ).thenReturn( existingEvent );

        Event actual = converter.fromForRuleEngine( preheat, event );

        assertEquals( 1, actual.getEventDataValues().size() );
        EventDataValue expect1 = new EventDataValue();
        expect1.setDataElement( dataElement.getUid() );
        expect1.setValue( updatedValue.getValue() );
        assertContainsOnly( Set.of( expect1 ), actual.getEventDataValues() );
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
        event.getEventDataValues().add( eventDataValue );

        org.hisp.dhis.tracker.imports.domain.Event event = converter.to( this.event );

        assertEquals( ENROLLMENT_UID, event.getEnrollment() );
        assertEquals( event.getStoredBy(), user.getUsername() );
        event.getDataValues().forEach( e -> {
            assertEquals( DateUtils.fromInstant( e.getCreatedAt() ), this.event.getCreated() );
            assertEquals( e.getUpdatedBy().getUsername(), this.event.getLastUpdatedByUserInfo().getUsername() );
            assertEquals( e.getUpdatedBy().getUsername(), this.event.getCreatedByUserInfo().getUsername() );
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

    private org.hisp.dhis.tracker.imports.domain.Event event( DataValue dataValue )
    {
        return event( null, dataValue );
    }

    private org.hisp.dhis.tracker.imports.domain.Event event( String uid, DataValue dataValue )
    {
        return org.hisp.dhis.tracker.imports.domain.Event.builder()
            .event( uid )
            .programStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_UID ) )
            .program( MetadataIdentifier.ofUid( PROGRAM_UID ) )
            .orgUnit( MetadataIdentifier.ofUid( ORGANISATION_UNIT_UID ) )
            .attributeOptionCombo( MetadataIdentifier.EMPTY_UID )
            .dataValues( Sets.newHashSet( dataValue ) )
            .build();
    }

    private Event event()
    {
        Event event = new Event();
        event.setUid( CodeGenerator.generateUid() );
        return event;
    }

    private EventDataValue eventDataValue( String dataElement, String value )
    {
        EventDataValue eventDataValue = new EventDataValue();
        eventDataValue.setDataElement( dataElement );
        eventDataValue.setValue( value );
        return eventDataValue;
    }

    private DataValue dataValue( MetadataIdentifier dataElement, String value )
    {
        User user = User.builder().username( USERNAME ).build();

        return DataValue.builder()
            .dataElement( dataElement )
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
