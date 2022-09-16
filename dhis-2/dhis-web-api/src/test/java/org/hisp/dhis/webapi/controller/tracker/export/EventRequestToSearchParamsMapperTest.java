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

import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
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
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class EventRequestToSearchParamsMapperTest
{

    public static final String TEA_1_UID = "TvjwTPToKHO";

    public static final String TEA_2_UID = "cy2oRh2sNr6";

    public static final String PROGRAM_UID = "programuid";

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
    private TrackedEntityAttributeService attributeService;

    @Mock
    private DataElementService dataElementService;

    @Mock
    private InputUtils inputUtils;

    @Mock
    private SchemaService schemaService;

    @InjectMocks
    private EventRequestToSearchParamsMapper requestToSearchParamsMapper;

    private Program program;

    private ProgramStage programStage;

    private TrackedEntityInstance trackedEntityInstance;

    private TrackedEntityAttribute tea1;

    @BeforeEach
    public void setUp()
    {
        User user = new User();
        when( currentUserService.getCurrentUser() ).thenReturn( user );

        program = new Program();
        program.setUid( PROGRAM_UID );
        when( programService.getProgram( PROGRAM_UID ) ).thenReturn( program );
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
        tea1 = new TrackedEntityAttribute();
        tea1.setUid( TEA_1_UID );
        TrackedEntityAttribute tea2 = new TrackedEntityAttribute();
        tea2.setUid( TEA_2_UID );
        when( attributeService.getAllTrackedEntityAttributes() ).thenReturn( List.of( tea1, tea2 ) );
        when( attributeService.getTrackedEntityAttribute( TEA_1_UID ) ).thenReturn( tea1 );

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
    void testMappingDoesNotFetchOptionalEmptyQueryParametersFromDB()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();

        requestToSearchParamsMapper.map( eventCriteria );

        verifyNoInteractions( programService );
        verifyNoInteractions( programStageService );
        verifyNoInteractions( organisationUnitService );
        verifyNoInteractions( entityInstanceService );
    }

    @Test
    void testMappingProgram()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setProgram( PROGRAM_UID );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertEquals( program, params.getProgram() );
    }

    @Test
    void testMappingProgramNotFound()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setProgram( "unknown" );

        Exception exception = assertThrows( IllegalQueryException.class,
            () -> requestToSearchParamsMapper.map( eventCriteria ) );
        assertEquals( "Program is specified but does not exist: unknown", exception.getMessage() );
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

        Date occurredAfter = parseDate( "2020-01-01" );
        eventCriteria.setOccurredAfter( occurredAfter );
        Date occurredBefore = parseDate( "2020-09-12" );
        eventCriteria.setOccurredBefore( occurredBefore );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertEquals( occurredAfter, params.getStartDate() );
        assertEquals( occurredBefore, params.getEndDate() );
    }

    @Test
    void testMappingScheduledAfterBefore()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();

        Date scheduledAfter = parseDate( "2021-01-01" );
        eventCriteria.setScheduledAfter( scheduledAfter );
        Date scheduledBefore = parseDate( "2021-09-12" );
        eventCriteria.setScheduledBefore( scheduledBefore );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertEquals( scheduledAfter, params.getDueDateStart() );
        assertEquals( scheduledBefore, params.getDueDateEnd() );
    }

    @Test
    void testMappingUpdatedDates()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();

        Date updatedAfter = parseDate( "2022-01-01" );
        eventCriteria.setUpdatedAfter( updatedAfter );
        Date updatedBefore = parseDate( "2022-09-12" );
        eventCriteria.setUpdatedBefore( updatedBefore );
        String updatedWithin = "P6M";
        eventCriteria.setUpdatedWithin( updatedWithin );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertEquals( updatedAfter, params.getLastUpdatedStartDate() );
        assertEquals( updatedBefore, params.getLastUpdatedEndDate() );
        assertEquals( updatedWithin, params.getLastUpdatedDuration() );
    }

    @Test
    void testMappingEnrollmentEnrolledAtDates()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();

        Date enrolledBefore = parseDate( "2022-01-01" );
        eventCriteria.setEnrollmentEnrolledBefore( enrolledBefore );
        Date enrolledAfter = parseDate( "2022-02-01" );
        eventCriteria.setEnrollmentEnrolledAfter( enrolledAfter );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertEquals( enrolledBefore, params.getEnrollmentEnrolledBefore() );
        assertEquals( enrolledAfter, params.getEnrollmentEnrolledAfter() );
    }

    @Test
    void testMappingEnrollmentOcurredAtDates()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();

        Date enrolledBefore = parseDate( "2022-01-01" );
        eventCriteria.setEnrollmentOccurredBefore( enrolledBefore );
        Date enrolledAfter = parseDate( "2022-02-01" );
        eventCriteria.setEnrollmentOccurredAfter( enrolledAfter );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertEquals( enrolledBefore, params.getEnrollmentOccurredBefore() );
        assertEquals( enrolledAfter, params.getEnrollmentOccurredAfter() );
    }

    @Test
    void testMappingAttributeOrdering()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();

        OrderCriteria attributeOrder = OrderCriteria.of( TEA_1_UID, OrderParam.SortDirection.ASC );
        OrderCriteria unknownAttributeOrder = OrderCriteria.of( "unknownAtt1", OrderParam.SortDirection.ASC );
        eventCriteria.setOrder( List.of( attributeOrder, unknownAttributeOrder ) );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertAll(
            () -> assertContainsOnly( params.getAttributeOrders(),
                List.of( new OrderParam( TEA_1_UID, OrderParam.SortDirection.ASC ) ) ),
            () -> assertContainsOnly( params.getFilterAttributes(), List.of( new QueryItem( tea1 ) ) ) );
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
    void testMappingEventIsNull()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertIsEmpty( params.getEvents() );
    }

    @Test
    void testMappingEventIsEmpty()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setEvent( " " );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertIsEmpty( params.getEvents() );
    }

    @Test
    void testMappingAssignedUser()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setAssignedUser( "XKrcfuM4Hcw;M4pNmLabtXl" );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertContainsOnly( Set.of( "XKrcfuM4Hcw", "M4pNmLabtXl" ), params.getAssignedUsers() );
    }

    @Test
    void testMappingAssignedUserStripsInvalidUid()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setAssignedUser( "invalidUid;M4pNmLabtXl" );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertEquals( Set.of( "M4pNmLabtXl" ), params.getAssignedUsers() );
    }

    @Test
    void testMappingAssignedUserIsNull()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertIsEmpty( params.getAssignedUsers() );
    }

    @Test
    void testMappingAssignedUserIsEmpty()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setAssignedUser( " " );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertIsEmpty( params.getAssignedUsers() );
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

        assertContainsOnly( List.of( new OrderParam( "programStage", OrderParam.SortDirection.DESC ),
            new OrderParam( "dueDate", OrderParam.SortDirection.ASC ) ), params.getOrders() );
    }

    @Test
    void testOrderBySupportedPropertyNotInEventSchema()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setOrder( OrderCriteria.fromOrderString( "enrolledAt:asc" ) );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertContainsOnly( List.of( new OrderParam( "enrolledAt", OrderParam.SortDirection.ASC ) ),
            params.getOrders() );
    }

    @Test
    void testOrderFailsForNonSimpleEventProperty()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setOrder( OrderCriteria.fromOrderString( "nonSimple:desc" ) );

        Exception exception = assertThrows( IllegalQueryException.class,
            () -> requestToSearchParamsMapper.map( eventCriteria ) );
        assertStartsWith( "Order by property `nonSimple` is not supported", exception.getMessage() );
    }

    @Test
    void testOrderFailsForUnsupportedProperty()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setOrder(
            OrderCriteria.fromOrderString( "unsupportedProperty1:asc,enrolledAt:asc,unsupportedProperty2:desc" ) );

        Exception exception = assertThrows( IllegalQueryException.class,
            () -> requestToSearchParamsMapper.map( eventCriteria ) );
        assertAll(
            () -> assertStartsWith( "Order by property `", exception.getMessage() ),
            // order of properties might not always be the same; therefore using
            // contains
            () -> assertContains( "unsupportedProperty1", exception.getMessage() ),
            () -> assertContains( "unsupportedProperty2", exception.getMessage() ) );
    }

    @Test
    void testFilterAttributes()
    {

        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setFilterAttributes( Set.of( TEA_1_UID + ":eq:2", TEA_2_UID + ":like:foo" ) );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        List<QueryItem> items = params.getFilterAttributes();
        assertNotNull( items );
        // mapping to UIDs as the error message by just relying on QueryItem
        // equals() is not helpful
        assertContainsOnly( List.of( TEA_1_UID,
            TEA_2_UID ), items.stream().map( i -> i.getItem().getUid() ).collect( Collectors.toList() ) );

        // QueryItem equals() does not take the QueryFilter into account so
        // assertContainsOnly alone does not ensure operators and filter value
        // are correct
        // the following block is needed because of that
        // assertion is order independent as the order of QueryItems is not
        // guaranteed
        Map<String, QueryFilter> expectedFilters = Map.of(
            TEA_1_UID, new QueryFilter( QueryOperator.EQ, "2" ),
            TEA_2_UID, new QueryFilter( QueryOperator.LIKE, "foo" ) );
        assertAll( items.stream().map( i -> (Executable) () -> {
            String uid = i.getItem().getUid();
            QueryFilter expected = expectedFilters.get( uid );
            assertEquals( expected.getOperator().getValue() + " " + expected.getFilter(), i.getFiltersAsString(),
                () -> String.format( "QueryFilter mismatch for TEA with UID %s", uid ) );
        } ).collect( Collectors.toList() ) );
    }

    @Test
    void testFilterAttributesWhenTEAHasMultipleFilters()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setFilterAttributes( Set.of( TEA_1_UID + ":gt:10:lt:20" ) );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        List<QueryItem> items = params.getFilterAttributes();
        assertNotNull( items );
        // mapping to UIDs as the error message by just relying on QueryItem
        // equals() is not helpful
        assertContainsOnly( List.of( TEA_1_UID ),
            items.stream().map( i -> i.getItem().getUid() ).collect( Collectors.toList() ) );

        // QueryItem equals() does not take the QueryFilter into account so
        // assertContainsOnly alone does not ensure operators and filter value
        // are correct
        assertContainsOnly( Set.of(
            new QueryFilter( QueryOperator.GT, "10" ),
            new QueryFilter( QueryOperator.LT, "20" ) ), items.get( 0 ).getFilters() );
    }

    @Test
    void testFilterAttributesWhenNumberOfFilterSegmentsIsEven()
    {
        when( attributeService.getAllTrackedEntityAttributes() ).thenReturn( Collections.emptyList() );

        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setFilterAttributes( Set.of( "eq:2" ) );

        Exception exception = assertThrows( IllegalQueryException.class,
            () -> requestToSearchParamsMapper.map( eventCriteria ) );
        assertEquals( "Query item or filter is invalid: eq:2", exception.getMessage() );
    }

    @Test
    void testFilterAttributesWhenNoTEAExist()
    {
        when( attributeService.getAllTrackedEntityAttributes() ).thenReturn( Collections.emptyList() );

        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setFilterAttributes( Set.of( TEA_1_UID + ":eq:2" ) );

        Exception exception = assertThrows( IllegalQueryException.class,
            () -> requestToSearchParamsMapper.map( eventCriteria ) );
        assertEquals( "Attribute does not exist: " + TEA_1_UID, exception.getMessage() );
    }

    @Test
    void testFilterAttributesWhenTEAInFilterDoesNotExist()
    {
        when( attributeService.getAllTrackedEntityAttributes() ).thenReturn( Collections.emptyList() );

        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setFilterAttributes( Set.of( "JM5zWuf1mkb:eq:2" ) );

        Exception exception = assertThrows( IllegalQueryException.class,
            () -> requestToSearchParamsMapper.map( eventCriteria ) );
        assertEquals( "Attribute does not exist: JM5zWuf1mkb", exception.getMessage() );
    }

    @Test
    void testFilterAttributesWhenTEAUidIsDuplicated()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setFilterAttributes(
            Set.of( "TvjwTPToKHO:lt:20", "cy2oRh2sNr6:lt:20", "TvjwTPToKHO:gt:30", "cy2oRh2sNr6:gt:30" ) );

        Exception exception = assertThrows( IllegalQueryException.class,
            () -> requestToSearchParamsMapper.map( eventCriteria ) );
        assertAll(
            () -> assertStartsWith( "filterAttributes can only have one filter per tracked entity attribute (TEA).",
                exception.getMessage() ),
            // order of TEA UIDs might not always be the same; therefore using
            // contains
            () -> assertContains( TEA_1_UID, exception.getMessage() ),
            () -> assertContains( TEA_2_UID, exception.getMessage() ) );
    }

    @Test
    void testFilterAttributesUsingOnlyUID()
    {

        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setFilterAttributes( Set.of( TEA_1_UID ) );

        EventSearchParams params = requestToSearchParamsMapper.map( eventCriteria );

        assertContainsOnly(
            List.of( new QueryItem( tea1, null, tea1.getValueType(), tea1.getAggregationType(), tea1.getOptionSet(),
                tea1.isUnique() ) ),
            params.getFilterAttributes() );
    }
}
