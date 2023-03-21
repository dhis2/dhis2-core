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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventSearchParams;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
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
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
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

@MockitoSettings( strictness = Strictness.LENIENT ) // common setup
@ExtendWith( MockitoExtension.class )
class TrackerEventCriteriaMapperTest
{

    private static final String DE_1_UID = "OBzmpRP6YUh";

    private static final String DE_2_UID = "KSd4PejqBf9";

    private static final String TEA_1_UID = "TvjwTPToKHO";

    private static final String TEA_2_UID = "cy2oRh2sNr6";

    private static final String PROGRAM_UID = "programuid";

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
    private TrackerEventCriteriaMapper mapper;

    private Program program;

    private ProgramStage programStage;

    private OrganisationUnit ou;

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

        ou = new OrganisationUnit();
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

        DataElement de1 = new DataElement();
        de1.setUid( DE_1_UID );
        when( dataElementService.getDataElement( DE_1_UID ) ).thenReturn( de1 );
        DataElement de2 = new DataElement();
        de2.setUid( DE_2_UID );
        when( dataElementService.getDataElement( DE_2_UID ) ).thenReturn( de2 );

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
        mapper.setSchema();
    }

    @Test
    void testMappingDoesNotFetchOptionalEmptyQueryParametersFromDB()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();

        mapper.map( criteria );

        verifyNoInteractions( programService );
        verifyNoInteractions( programStageService );
        verifyNoInteractions( organisationUnitService );
        verifyNoInteractions( entityInstanceService );
    }

    @Test
    void testMappingProgram()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setProgram( PROGRAM_UID );

        EventSearchParams params = mapper.map( criteria );

        assertEquals( program, params.getProgram() );
    }

    @Test
    void testMappingProgramNotFound()
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setProgram( "unknown" );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Program is specified but does not exist: unknown", exception.getMessage() );
    }

    @Test
    void testMappingProgramStage()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setProgramStage( "programstageuid" );

        EventSearchParams params = mapper.map( criteria );

        assertEquals( programStage, params.getProgramStage() );
    }

    @Test
    void shouldFailWithBadRequestExceptionWhenMappingCriteriaWithUnknownProgramStage()
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setProgramStage( "unknown" );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Program stage is specified but does not exist: unknown", exception.getMessage() );
    }

    @Test
    void shouldReturnOrgUnitWhenCorrectOrgUnitMapped()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setOrgUnit( ou.getUid() );

        EventSearchParams params = mapper.map( criteria );

        assertEquals( ou, params.getOrgUnit() );
    }

    @Test
    void shouldFailWithBadRequestExceptionWhenMappingCriteriaWithUnknownOrgUnit()
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setOrgUnit( "unknown" );
        when( organisationUnitService.getOrganisationUnit( any() ) ).thenReturn( null );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );

        assertEquals( "Org unit is specified but does not exist: unknown", exception.getMessage() );
    }

    @Test
    void testMappingTrackedEntity()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setTrackedEntity( "teiuid" );

        EventSearchParams params = mapper.map( criteria );

        assertEquals( trackedEntityInstance, params.getTrackedEntityInstance() );
    }

    @Test
    void shouldFailWithBadRequestExceptionWhenTrackedEntityDoesNotExist()
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setTrackedEntity( "teiuid" );
        when( entityInstanceService.getTrackedEntityInstance( "teiuid" ) ).thenReturn( null );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );

        assertStartsWith( "Tracked entity instance is specified but does not exist: " + criteria.getTrackedEntity(),
            exception.getMessage() );
    }

    @Test
    void testMappingOccurredAfterBefore()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();

        Date occurredAfter = parseDate( "2020-01-01" );
        criteria.setOccurredAfter( occurredAfter );
        Date occurredBefore = parseDate( "2020-09-12" );
        criteria.setOccurredBefore( occurredBefore );

        EventSearchParams params = mapper.map( criteria );

        assertEquals( occurredAfter, params.getStartDate() );
        assertEquals( occurredBefore, params.getEndDate() );
    }

    @Test
    void testMappingScheduledAfterBefore()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();

        Date scheduledAfter = parseDate( "2021-01-01" );
        criteria.setScheduledAfter( scheduledAfter );
        Date scheduledBefore = parseDate( "2021-09-12" );
        criteria.setScheduledBefore( scheduledBefore );

        EventSearchParams params = mapper.map( criteria );

        assertEquals( scheduledAfter, params.getDueDateStart() );
        assertEquals( scheduledBefore, params.getDueDateEnd() );
    }

    @Test
    void testMappingUpdatedDates()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();

        Date updatedAfter = parseDate( "2022-01-01" );
        criteria.setUpdatedAfter( updatedAfter );
        Date updatedBefore = parseDate( "2022-09-12" );
        criteria.setUpdatedBefore( updatedBefore );
        String updatedWithin = "P6M";
        criteria.setUpdatedWithin( updatedWithin );

        EventSearchParams params = mapper.map( criteria );

        assertEquals( updatedAfter, params.getLastUpdatedStartDate() );
        assertEquals( updatedBefore, params.getLastUpdatedEndDate() );
        assertEquals( updatedWithin, params.getLastUpdatedDuration() );
    }

    @Test
    void testMappingEnrollmentEnrolledAtDates()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();

        Date enrolledBefore = parseDate( "2022-01-01" );
        criteria.setEnrollmentEnrolledBefore( enrolledBefore );
        Date enrolledAfter = parseDate( "2022-02-01" );
        criteria.setEnrollmentEnrolledAfter( enrolledAfter );

        EventSearchParams params = mapper.map( criteria );

        assertEquals( enrolledBefore, params.getEnrollmentEnrolledBefore() );
        assertEquals( enrolledAfter, params.getEnrollmentEnrolledAfter() );
    }

    @Test
    void testMappingEnrollmentOccurredAtDates()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();

        Date enrolledBefore = parseDate( "2022-01-01" );
        criteria.setEnrollmentOccurredBefore( enrolledBefore );
        Date enrolledAfter = parseDate( "2022-02-01" );
        criteria.setEnrollmentOccurredAfter( enrolledAfter );

        EventSearchParams params = mapper.map( criteria );

        assertEquals( enrolledBefore, params.getEnrollmentOccurredBefore() );
        assertEquals( enrolledAfter, params.getEnrollmentOccurredAfter() );
    }

    @Test
    void testMappingAttributeOrdering()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();

        OrderCriteria attributeOrder = OrderCriteria.of( TEA_1_UID, SortDirection.ASC );
        OrderCriteria unknownAttributeOrder = OrderCriteria.of( "unknownAtt1", SortDirection.ASC );
        criteria.setOrder( List.of( attributeOrder, unknownAttributeOrder ) );

        EventSearchParams params = mapper.map( criteria );

        assertAll(
            () -> assertContainsOnly( params.getAttributeOrders(),
                List.of( new OrderParam( TEA_1_UID, SortDirection.ASC ) ) ),
            () -> assertContainsOnly( params.getFilterAttributes(), List.of( new QueryItem( tea1 ) ) ) );
    }

    @Test
    void testMappingEnrollments()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();

        Set<String> enrollments = Set.of( "NQnuK2kLm6e" );
        criteria.setEnrollments( enrollments );

        EventSearchParams params = mapper.map( criteria );

        assertEquals( enrollments, params.getProgramInstances() );
    }

    @Test
    void testMappingEvents()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setEvent( "XKrcfuM4Hcw;M4pNmLabtXl" );

        EventSearchParams params = mapper.map( criteria );

        assertEquals( Set.of( "XKrcfuM4Hcw", "M4pNmLabtXl" ), params.getEvents() );
    }

    @Test
    void testMappingEventsStripsInvalidUid()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setEvent( "invalidUid;M4pNmLabtXl" );

        EventSearchParams params = mapper.map( criteria );

        assertEquals( Set.of( "M4pNmLabtXl" ), params.getEvents() );
    }

    @Test
    void testMappingEventIsNull()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();

        EventSearchParams params = mapper.map( criteria );

        assertIsEmpty( params.getEvents() );
    }

    @Test
    void testMappingEventIsEmpty()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setEvent( " " );

        EventSearchParams params = mapper.map( criteria );

        assertIsEmpty( params.getEvents() );
    }

    @Test
    void testMappingAssignedUserStripsInvalidUid()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setAssignedUser( "invalidUid;M4pNmLabtXl" );

        EventSearchParams params = mapper.map( criteria );

        assertEquals( Set.of( "M4pNmLabtXl" ), params.getAssignedUserQueryParam().getAssignedUsers() );
    }

    @Test
    void testMappingAssignedUserIsEmpty()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setAssignedUser( " " );

        EventSearchParams params = mapper.map( criteria );

        assertIsEmpty( params.getAssignedUserQueryParam().getAssignedUsers() );
    }

    @Test
    void testMutualExclusionOfEventsAndFilter()
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setFilter( Set.of( "qrur9Dvnyt5:ge:1:le:2" ) );
        criteria.setEvent( "XKrcfuM4Hcw;M4pNmLabtXl" );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Event UIDs and filters can not be specified at the same time", exception.getMessage() );
    }

    @Test
    void testOrderByEventSchemaProperties()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setOrder( OrderCriteria.fromOrderString( "programStage:desc,dueDate:asc" ) );

        EventSearchParams params = mapper.map( criteria );

        assertContainsOnly( List.of( new OrderParam( "programStage", SortDirection.DESC ),
            new OrderParam( "dueDate", SortDirection.ASC ) ), params.getOrders() );
    }

    @Test
    void testOrderBySupportedPropertyNotInEventSchema()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setOrder( OrderCriteria.fromOrderString( "enrolledAt:asc" ) );

        EventSearchParams params = mapper.map( criteria );

        assertContainsOnly( List.of( new OrderParam( "enrolledAt", SortDirection.ASC ) ),
            params.getOrders() );
    }

    @Test
    void testOrderFailsForNonSimpleEventProperty()
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setOrder( OrderCriteria.fromOrderString( "nonSimple:desc" ) );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );
        assertStartsWith( "Order by property `nonSimple` is not supported", exception.getMessage() );
    }

    @Test
    void testOrderFailsForUnsupportedProperty()
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setOrder(
            OrderCriteria.fromOrderString( "unsupportedProperty1:asc,enrolledAt:asc,unsupportedProperty2:desc" ) );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );
        assertAll(
            () -> assertStartsWith( "Order by property `", exception.getMessage() ),
            // order of properties might not always be the same; therefore using
            // contains
            () -> assertContains( "unsupportedProperty1", exception.getMessage() ),
            () -> assertContains( "unsupportedProperty2", exception.getMessage() ) );
    }

    @Test
    void testFilter()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setFilter( Set.of( DE_1_UID + ":eq:2", DE_2_UID + ":like:foo" ) );

        EventSearchParams params = mapper.map( criteria );

        List<QueryItem> items = params.getFilters();
        assertNotNull( items );
        // mapping to UIDs as the error message by just relying on QueryItem
        // equals() is not helpful
        assertContainsOnly( List.of( DE_1_UID,
            DE_2_UID ), items.stream().map( i -> i.getItem().getUid() ).collect( Collectors.toList() ) );

        // QueryItem equals() does not take the QueryFilter into account so
        // assertContainsOnly alone does not ensure operators and filter value
        // are correct
        // the following block is needed because of that
        // assertion is order independent as the order of QueryItems is not
        // guaranteed
        Map<String, QueryFilter> expectedFilters = Map.of(
            DE_1_UID, new QueryFilter( QueryOperator.EQ, "2" ),
            DE_2_UID, new QueryFilter( QueryOperator.LIKE, "foo" ) );
        assertAll( items.stream().map( i -> (Executable) () -> {
            String uid = i.getItem().getUid();
            QueryFilter expected = expectedFilters.get( uid );
            assertEquals( expected.getOperator().getValue() + " " + expected.getFilter(), i.getFiltersAsString(),
                () -> String.format( "QueryFilter mismatch for DE with UID %s", uid ) );
        } ).collect( Collectors.toList() ) );
    }

    @Test
    void testFilterAttributes()
        throws BadRequestException,
        ForbiddenException
    {

        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setFilterAttributes( Set.of( TEA_1_UID + ":eq:2", TEA_2_UID + ":like:foo" ) );

        EventSearchParams params = mapper.map( criteria );

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
    void testFilterWhenDEHasMultipleFilters()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setFilter( Set.of( DE_1_UID + ":gt:10:lt:20" ) );

        EventSearchParams params = mapper.map( criteria );

        List<QueryItem> items = params.getFilters();
        assertNotNull( items );
        // mapping to UIDs as the error message by just relying on QueryItem
        // equals() is not helpful
        assertContainsOnly( List.of( DE_1_UID ),
            items.stream().map( i -> i.getItem().getUid() ).collect( Collectors.toList() ) );

        // QueryItem equals() does not take the QueryFilter into account so
        // assertContainsOnly alone does not ensure operators and filter value
        // are correct
        assertContainsOnly( Set.of(
            new QueryFilter( QueryOperator.GT, "10" ),
            new QueryFilter( QueryOperator.LT, "20" ) ), items.get( 0 ).getFilters() );
    }

    @Test
    void testFilterAttributesWhenTEAHasMultipleFilters()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setFilterAttributes( Set.of( TEA_1_UID + ":gt:10:lt:20" ) );

        EventSearchParams params = mapper.map( criteria );

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
    void shouldFailWithBadRequestExceptionWhenCriteriaDataElementDoesNotExist()
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        String filterName = "filter";
        criteria.setFilter( Set.of( filterName ) );
        when( dataElementService.getDataElement( filterName ) ).thenReturn( null );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );

        assertEquals( "Dataelement does not exist: " + filterName, exception.getMessage() );
    }

    @Test
    void testFilterAttributesWhenTEAUidIsDuplicated()
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setFilterAttributes(
            Set.of( "TvjwTPToKHO:lt:20", "cy2oRh2sNr6:lt:20", "TvjwTPToKHO:gt:30", "cy2oRh2sNr6:gt:30" ) );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );
        assertAll(
            () -> assertStartsWith( "filterAttributes contains duplicate tracked entity attribute",
                exception.getMessage() ),
            // order of TEA UIDs might not always be the same; therefore using
            // contains
            () -> assertContains( TEA_1_UID, exception.getMessage() ),
            () -> assertContains( TEA_2_UID, exception.getMessage() ) );
    }

    @Test
    void testFilterAttributesUsingOnlyUID()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setFilterAttributes( Set.of( TEA_1_UID ) );

        EventSearchParams params = mapper.map( criteria );

        assertContainsOnly(
            List.of( new QueryItem( tea1, null, tea1.getValueType(), tea1.getAggregationType(), tea1.getOptionSet(),
                tea1.isUnique() ) ),
            params.getFilterAttributes() );
    }

    @Test
    void shouldFailWithForbiddenExceptionWhenUserHasNoAccessToProgram()
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setProgram( program.getUid() );
        User user = new User();
        when( currentUserService.getCurrentUser() ).thenReturn( user );
        when( aclService.canDataRead( user, program ) ).thenReturn( false );

        Exception exception = assertThrows( ForbiddenException.class,
            () -> mapper.map( criteria ) );

        assertEquals( "User has no access to program: " + program.getUid(), exception.getMessage() );
    }

    @Test
    void shouldFailWithForbiddenExceptionWhenUserHasNoAccessToProgramStage()
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setProgramStage( programStage.getUid() );
        User user = new User();
        when( currentUserService.getCurrentUser() ).thenReturn( user );
        when( aclService.canDataRead( user, programStage ) ).thenReturn( false );

        Exception exception = assertThrows( ForbiddenException.class,
            () -> mapper.map( criteria ) );

        assertEquals( "User has no access to program stage: " + programStage.getUid(), exception.getMessage() );
    }

    @Test
    void shouldFailWithForbiddenExceptionWhenUserHasNoAccessToCategoryCombo()
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setAttributeCc( "Cc" );
        criteria.setAttributeCos( "Cos" );
        CategoryOptionCombo combo = new CategoryOptionCombo();
        combo.setUid( "uid" );
        when( inputUtils.getAttributeOptionCombo( criteria.getAttributeCc(), criteria.getAttributeCos(), true ) )
            .thenReturn( combo );
        when( aclService.canDataRead( any( User.class ), any( CategoryOptionCombo.class ) ) ).thenReturn( false );

        Exception exception = assertThrows( ForbiddenException.class,
            () -> mapper.map( criteria ) );

        assertEquals( "User has no access to attribute category option combo: " + combo.getUid(),
            exception.getMessage() );
    }

    @Test
    void shouldCreateQueryFilterattributesWhenQueryHasOperatorAndValueWithDelimiter()
        throws BadRequestException,
        ForbiddenException
    {
        TrackerEventCriteria criteria = new TrackerEventCriteria();
        criteria.setFilterAttributes( Set.of( TEA_1_UID + ":eq:2", TEA_2_UID + ":like:project:x" ) );

        EventSearchParams params = mapper.map( criteria );

        List<QueryFilter> actualFilters = params.getFilterAttributes().stream().flatMap( f -> f.getFilters().stream() )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of(
            new QueryFilter( QueryOperator.LIKE, "project:x" ),
            new QueryFilter( QueryOperator.EQ, "2" ) ), actualFilters );
    }
}
