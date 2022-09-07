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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventSearchParams;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class EventRequestToSearchParamsMapperTest
{

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ProgramService programService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private ProgramStageService programStageService;

    @Mock
    private AclService aclService;

    @Mock
    private TrackedEntityInstanceService entityInstanceService;

    @Mock
    private DataElementService dataElementService;

    @Mock
    private InputUtils inputUtils;

    @Mock
    private SchemaService schemaService;

    private EventRequestToSearchParamsMapper requestToSearchParamsMapper;

    private Program program;

    private ProgramStage programStage;

    private TrackedEntityInstance trackedEntityInstance;

    private SimpleDateFormat dateFormatter;

    @BeforeEach
    public void setUp()
    {
        requestToSearchParamsMapper = new EventRequestToSearchParamsMapper( currentUserService, programService,
            organisationUnitService, programStageService, aclService, entityInstanceService, dataElementService,
            inputUtils, schemaService );

        dateFormatter = new SimpleDateFormat( "yyyy-MM-dd", Locale.ENGLISH );

        User user = new User();
        when( currentUserService.getCurrentUser() ).thenReturn( user );

        program = new Program();
        program.setUid( "programuid" );
        when( programService.getProgram( "programuid" ) ).thenReturn( program );
        when( aclService.canDataRead( user, program ) ).thenReturn( true );

        programStage = new ProgramStage();
        programStage.setUid( "programstageuid" );
        when( programStageService.getProgramStage( "programstageuid" ) ).thenReturn( programStage );
        when( aclService.canDataRead( user, programStage ) ).thenReturn( true );

        OrganisationUnit ou = new OrganisationUnit();
        DataElement de = new DataElement();

        when( organisationUnitService.getOrganisationUnit( any() ) ).thenReturn( ou );
        when( organisationUnitService.isInUserHierarchy( ou ) ).thenReturn( true );

        trackedEntityInstance = new TrackedEntityInstance();
        when( entityInstanceService.getTrackedEntityInstance( "teiuid" ) ).thenReturn( trackedEntityInstance );

        when( dataElementService.getDataElement( any() ) ).thenReturn( de );

        Schema eventSchema = new Schema( Event.class, "event", "events" );
        Property prop1 = new Property();
        prop1.setName( "programStage" );
        prop1.setSimple( true );
        eventSchema.addProperty( prop1 );
        Property prop2 = new Property();
        prop2.setName( "dueDate" );
        prop2.setSimple( true );
        eventSchema.addProperty( prop2 );
        Property prop3 = new Property();
        prop3.setName( "nonSimple" );
        prop3.setSimple( false );
        eventSchema.addProperty( prop3 );
        when( schemaService.getDynamicSchema( Event.class ) ).thenReturn( eventSchema );
        requestToSearchParamsMapper.setSchema();
    }

    @Test
    void testMappingProgram()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setProgram( "programuid" );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertEquals( program, params.getProgram() );
    }

    @Test
    void testMappingProgramStage()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setProgramStage( "programstageuid" );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertEquals( programStage, params.getProgramStage() );
    }

    @Test
    void testMappingTrackedEntity()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setTrackedEntity( "teiuid" );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertEquals( trackedEntityInstance, params.getTrackedEntityInstance() );
    }

    @Test
    void testMappingOccurredAfterBefore()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();

        Date occurredAfter = date( "2020-01-01" );
        eventCriteria.setOccurredAfter( occurredAfter );
        Date occurredBefore = date( "2020-09-12" );
        eventCriteria.setOccurredBefore( occurredBefore );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertEquals( occurredAfter, params.getStartDate() );
        assertEquals( occurredBefore, params.getEndDate() );
    }

    @Test
    void testMappingScheduledAfterBefore()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();

        Date scheduledAfter = date( "2021-01-01" );
        eventCriteria.setScheduledAfter( scheduledAfter );
        Date scheduledBefore = date( "2021-09-12" );
        eventCriteria.setScheduledBefore( scheduledBefore );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertEquals( scheduledAfter, params.getDueDateStart() );
        assertEquals( scheduledBefore, params.getDueDateEnd() );
    }

    @Test
    void testMappingUpdatedDates()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();

        Date updatedAfter = date( "2022-01-01" );
        eventCriteria.setUpdatedAfter( updatedAfter );
        Date updatedBefore = date( "2022-09-12" );
        eventCriteria.setUpdatedBefore( updatedBefore );
        String updatedWithin = "P6M";
        eventCriteria.setUpdatedWithin( updatedWithin );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertEquals( updatedAfter, params.getLastUpdatedStartDate() );
        assertEquals( updatedBefore, params.getLastUpdatedEndDate() );
        assertEquals( updatedWithin, params.getLastUpdatedDuration() );
    }

    @Test
    void testMappingEnrollments()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();

        Set<String> enrollments = Set.of( "NQnuK2kLm6e" );
        eventCriteria.setEnrollments( enrollments );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertEquals( enrollments, params.getProgramInstances() );
    }

    @Test
    void testMappingEvents()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setEvent( "XKrcfuM4Hcw;M4pNmLabtXl" );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertEquals( Set.of( "XKrcfuM4Hcw", "M4pNmLabtXl" ), params.getEvents() );
    }

    @Test
    void testMappingEventsStripsInvalidUid()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setEvent( "invalidUid;M4pNmLabtXl" );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertEquals( Set.of( "M4pNmLabtXl" ), params.getEvents() );
    }

    @Test
    void testMappingNoEvents()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertEquals( Collections.emptySet(), params.getEvents() );
    }

    @Test
    void testMappingAssignedUsers()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setAssignedUser( "XKrcfuM4Hcw;M4pNmLabtXl" );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertEquals( Set.of( "XKrcfuM4Hcw", "M4pNmLabtXl" ), params.getAssignedUsers() );
    }

    @Test
    void testMappingAssignedUsersStripsInvalidUid()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setAssignedUser( "invalidUid;M4pNmLabtXl" );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertEquals( Set.of( "M4pNmLabtXl" ), params.getAssignedUsers() );
    }

    @Test
    void testMappingNoAssignedUsers()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertEquals( Collections.emptySet(), params.getAssignedUsers() );
    }

    @Test
    void testMutualExclusionOfEventsAndFilter()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setFilter( Set.of( "qrur9Dvnyt5:ge:1:le:2" ) );
        eventCriteria.setEvent( "XKrcfuM4Hcw;M4pNmLabtXl" );

        Exception exception = assertThrows( IllegalQueryException.class,
            () -> requestToSearchParamsMapper.map( eventCriteria ) );
        assertEquals( "Event UIDs and filters can not be specified at the same time", exception.getMessage() );
    }

    @Test
    void testOrderByEventSchemaProperties()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setOrder( OrderCriteria.fromOrderString( "programStage:desc,dueDate:asc" ) );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertContainsOnly( params.getOrders(),
            new OrderParam( "programStage", OrderParam.SortDirection.DESC ),
            new OrderParam( "dueDate", OrderParam.SortDirection.ASC ) );
    }

    @Test
    void testOrderBySupportedPropertyNotInEventSchema()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setOrder( OrderCriteria.fromOrderString( "enrolledAt:asc" ) );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertContainsOnly( params.getOrders(),
            new OrderParam( "enrolledAt", OrderParam.SortDirection.ASC ) );
    }

    @Test
    void testOrderFailsForNonSimpleEventProperty()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setOrder( OrderCriteria.fromOrderString( "nonSimple:desc" ) );

        Exception exception = assertThrows( IllegalQueryException.class,
            () -> requestToSearchParamsMapper.map( eventCriteria ) );
        assertNotNull( exception.getMessage() );
        String prefix = "Order by property `nonSimple` is not supported";
        assertTrue( exception.getMessage().startsWith( prefix ), () -> String
            .format( "expected message to start with '%s', got '%s' instead", prefix, exception.getMessage() ) );
    }

    @Test
    void testOrderFailsForUnsupportedProperty()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setOrder(
            OrderCriteria.fromOrderString( "unsupportedProperty1:asc,enrolledAt:asc,unsupportedProperty2:desc" ) );

        Exception exception = assertThrows( IllegalQueryException.class,
            () -> requestToSearchParamsMapper.map( eventCriteria ) );
        assertNotNull( exception.getMessage() );
        String prefix = "Order by property `";
        assertTrue( exception.getMessage().startsWith( prefix ), () -> String
            .format( "expected message to start with '%s', got '%s' instead", prefix, exception.getMessage() ) );
        // order of properties in exception message might not always be the same
        String property1 = "unsupportedProperty1";
        assertTrue( exception.getMessage().contains( property1 ), () -> String
            .format( "expected message to contain '%s', got '%s' instead", property1, exception.getMessage() ) );
        String property2 = "unsupportedProperty2";
        assertTrue( exception.getMessage().contains( property2 ), () -> String
            .format( "expected message to contain '%s', got '%s' instead", property2, exception.getMessage() ) );
    }

    private Date date( String date )
    {
        try
        {
            return dateFormatter.parse( date );
        }
        catch ( ParseException e )
        {
            throw new RuntimeException( e );
        }
    }
}
