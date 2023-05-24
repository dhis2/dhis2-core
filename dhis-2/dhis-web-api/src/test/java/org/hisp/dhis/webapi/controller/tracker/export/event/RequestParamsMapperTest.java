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
package org.hisp.dhis.webapi.controller.tracker.export.event;

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
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.tracker.export.event.EventSearchParams;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.common.UID;
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
class RequestParamsMapperTest
{

    private static final String DE_1_UID = "OBzmpRP6YUh";

    private static final String DE_2_UID = "KSd4PejqBf9";

    private static final String TEA_1_UID = "TvjwTPToKHO";

    private static final String TEA_2_UID = "cy2oRh2sNr6";

    private static final String PROGRAM_UID = "PlZSBEN7iZd";

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
    private TrackedEntityService entityInstanceService;

    @Mock
    private TrackedEntityAttributeService attributeService;

    @Mock
    private DataElementService dataElementService;

    @Mock
    private CategoryOptionComboService categoryOptionComboService;

    @InjectMocks
    private EventParamsMapper mapper;

    private Program program;

    private ProgramStage programStage;

    private OrganisationUnit ou;

    private TrackedEntity trackedEntity;

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
        programStage.setUid( "PlZSBEN7iZd" );
        when( programStageService.getProgramStage( "PlZSBEN7iZd" ) ).thenReturn( programStage );
        when( aclService.canDataRead( user, programStage ) ).thenReturn( true );

        ou = new OrganisationUnit();
        when( organisationUnitService.getOrganisationUnit( any() ) ).thenReturn( ou );
        when( organisationUnitService.isInUserHierarchy( ou ) ).thenReturn( true );

        trackedEntity = new TrackedEntity();
        when( entityInstanceService.getTrackedEntity( "qnR1RK4cTIZ" ) ).thenReturn( trackedEntity );
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
    }

    @Test
    void testMappingDoesNotFetchOptionalEmptyQueryParametersFromDB()
        throws BadRequestException,
        ForbiddenException
    {
        RequestParams criteria = new RequestParams();

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
        RequestParams criteria = new RequestParams();
        criteria.setProgram( UID.of( PROGRAM_UID ) );

        EventSearchParams params = mapper.map( criteria );

        assertEquals( program, params.getProgram() );
    }

    @Test
    void testMappingProgramNotFound()
    {
        RequestParams criteria = new RequestParams();
        criteria.setProgram( UID.of( "NeU85luyD4w" ) );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Program is specified but does not exist: NeU85luyD4w", exception.getMessage() );
    }

    @Test
    void testMappingProgramStage()
        throws BadRequestException,
        ForbiddenException
    {
        RequestParams criteria = new RequestParams();
        criteria.setProgramStage( UID.of( "PlZSBEN7iZd" ) );

        EventSearchParams params = mapper.map( criteria );

        assertEquals( programStage, params.getProgramStage() );
    }

    @Test
    void shouldFailWithBadRequestExceptionWhenMappingCriteriaWithUnknownProgramStage()
    {
        RequestParams criteria = new RequestParams();
        criteria.setProgramStage( UID.of( "NeU85luyD4w" ) );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Program stage is specified but does not exist: NeU85luyD4w", exception.getMessage() );
    }

    @Test
    void shouldReturnOrgUnitWhenCorrectOrgUnitMapped()
        throws BadRequestException,
        ForbiddenException
    {
        RequestParams criteria = new RequestParams();
        criteria.setOrgUnit( UID.of( ou.getUid() ) );

        EventSearchParams params = mapper.map( criteria );

        assertEquals( ou, params.getOrgUnit() );
    }

    @Test
    void shouldFailWithBadRequestExceptionWhenMappingCriteriaWithUnknownOrgUnit()
    {
        RequestParams criteria = new RequestParams();
        criteria.setOrgUnit( UID.of( "NeU85luyD4w" ) );
        when( organisationUnitService.getOrganisationUnit( any() ) ).thenReturn( null );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );

        assertEquals( "Org unit is specified but does not exist: NeU85luyD4w", exception.getMessage() );
    }

    @Test
    void testMappingTrackedEntity()
        throws BadRequestException,
        ForbiddenException
    {
        RequestParams criteria = new RequestParams();
        criteria.setTrackedEntity( UID.of( "qnR1RK4cTIZ" ) );

        EventSearchParams params = mapper.map( criteria );

        assertEquals( trackedEntity, params.getTrackedEntity() );
    }

    @Test
    void shouldFailWithBadRequestExceptionWhenTrackedEntityDoesNotExist()
    {
        RequestParams criteria = new RequestParams();
        criteria.setTrackedEntity( UID.of( "qnR1RK4cTIZ" ) );
        when( entityInstanceService.getTrackedEntity( "qnR1RK4cTIZ" ) ).thenReturn( null );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );

        assertStartsWith( "Tracked entity is specified but does not exist: " + criteria.getTrackedEntity(),
            exception.getMessage() );
    }

    @Test
    void testMappingOccurredAfterBefore()
        throws BadRequestException,
        ForbiddenException
    {
        RequestParams criteria = new RequestParams();

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
        RequestParams criteria = new RequestParams();

        Date scheduledAfter = parseDate( "2021-01-01" );
        criteria.setScheduledAfter( scheduledAfter );
        Date scheduledBefore = parseDate( "2021-09-12" );
        criteria.setScheduledBefore( scheduledBefore );

        EventSearchParams params = mapper.map( criteria );

        assertEquals( scheduledAfter, params.getScheduleAtStartDate() );
        assertEquals( scheduledBefore, params.getScheduleAtEndDate() );
    }

    @Test
    void testMappingUpdatedDates()
        throws BadRequestException,
        ForbiddenException
    {
        RequestParams criteria = new RequestParams();

        Date updatedAfter = parseDate( "2022-01-01" );
        criteria.setUpdatedAfter( updatedAfter );
        Date updatedBefore = parseDate( "2022-09-12" );
        criteria.setUpdatedBefore( updatedBefore );
        String updatedWithin = "P6M";
        criteria.setUpdatedWithin( updatedWithin );

        EventSearchParams params = mapper.map( criteria );

        assertEquals( updatedAfter, params.getUpdatedAtStartDate() );
        assertEquals( updatedBefore, params.getUpdatedAtEndDate() );
        assertEquals( updatedWithin, params.getUpdatedAtDuration() );
    }

    @Test
    void testMappingEnrollmentEnrolledAtDates()
        throws BadRequestException,
        ForbiddenException
    {
        RequestParams criteria = new RequestParams();

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
        RequestParams criteria = new RequestParams();

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
        RequestParams criteria = new RequestParams();

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
        RequestParams criteria = new RequestParams();

        criteria.setEnrollments( Set.of( UID.of( "NQnuK2kLm6e" ) ) );

        EventSearchParams params = mapper.map( criteria );

        assertEquals( Set.of( "NQnuK2kLm6e" ), params.getEnrollments() );
    }

    @Test
    void testMappingEvent()
        throws BadRequestException,
        ForbiddenException
    {
        RequestParams criteria = new RequestParams();
        criteria.setEvent( "XKrcfuM4Hcw;M4pNmLabtXl" );

        EventSearchParams params = mapper.map( criteria );

        assertEquals( Set.of( "XKrcfuM4Hcw", "M4pNmLabtXl" ), params.getEvents() );
    }

    @Test
    void testMappingEvents()
        throws BadRequestException,
        ForbiddenException
    {
        RequestParams criteria = new RequestParams();
        criteria.setEvents( Set.of( UID.of( "XKrcfuM4Hcw" ), UID.of( "M4pNmLabtXl" ) ) );

        EventSearchParams params = mapper.map( criteria );

        assertEquals( Set.of( "XKrcfuM4Hcw", "M4pNmLabtXl" ), params.getEvents() );
    }

    @Test
    void testMappingEventIsNull()
        throws BadRequestException,
        ForbiddenException
    {
        RequestParams criteria = new RequestParams();

        EventSearchParams params = mapper.map( criteria );

        assertIsEmpty( params.getEvents() );
    }

    @Test
    void testMappingAssignedUser()
        throws BadRequestException,
        ForbiddenException
    {
        RequestParams criteria = new RequestParams();
        criteria.setAssignedUser( "IsdLBTOBzMi;l5ab8q5skbB" );
        criteria.setAssignedUserMode( AssignedUserSelectionMode.PROVIDED );

        EventSearchParams params = mapper.map( criteria );

        assertContainsOnly( Set.of( "IsdLBTOBzMi", "l5ab8q5skbB" ),
            params.getAssignedUserQueryParam().getAssignedUsers() );
        assertEquals( AssignedUserSelectionMode.PROVIDED, params.getAssignedUserQueryParam().getMode() );
    }

    @Test
    void testMappingAssignedUsers()
        throws BadRequestException,
        ForbiddenException
    {
        RequestParams criteria = new RequestParams();
        criteria.setAssignedUsers( Set.of( UID.of( "IsdLBTOBzMi" ), UID.of( "l5ab8q5skbB" ) ) );
        criteria.setAssignedUserMode( AssignedUserSelectionMode.PROVIDED );

        EventSearchParams params = mapper.map( criteria );

        assertContainsOnly( Set.of( "IsdLBTOBzMi", "l5ab8q5skbB" ),
            params.getAssignedUserQueryParam().getAssignedUsers() );
        assertEquals( AssignedUserSelectionMode.PROVIDED, params.getAssignedUserQueryParam().getMode() );
    }

    @Test
    void testMutualExclusionOfEventsAndFilter()
    {
        RequestParams criteria = new RequestParams();
        criteria.setFilter( DE_1_UID + ":ge:1:le:2" );
        criteria.setEvent( DE_1_UID + ";" + DE_2_UID );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Event UIDs and filters can not be specified at the same time", exception.getMessage() );
    }

    @Test
    void shouldMapOrderParameterToOrderCriteriaWhenFieldsAreSortable()
        throws BadRequestException,
        ForbiddenException
    {
        RequestParams criteria = new RequestParams();
        criteria.setOrder( OrderCriteria.fromOrderString( "createdAt:asc,programStage:desc,scheduledAt:asc" ) );

        EventSearchParams params = mapper.map( criteria );

        assertContainsOnly( List.of(
            new OrderParam( "createdAt", SortDirection.ASC ),
            new OrderParam( "programStage", SortDirection.DESC ),
            new OrderParam( "scheduledAt", SortDirection.ASC ) ), params.getOrders() );
    }

    @Test
    void shouldThrowWhenOrderParameterContainsUnsupportedField()
    {
        RequestParams criteria = new RequestParams();
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
        RequestParams criteria = new RequestParams();
        criteria.setFilter( DE_1_UID + ":eq:2" + "," + DE_2_UID + ":like:foo" );

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

        RequestParams criteria = new RequestParams();
        criteria.setFilterAttributes( TEA_1_UID + ":eq:2" + "," + TEA_2_UID + ":like:foo" );

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
        RequestParams criteria = new RequestParams();
        criteria.setFilter( DE_1_UID + ":gt:10:lt:20" );

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
        RequestParams criteria = new RequestParams();
        criteria.setFilterAttributes( TEA_1_UID + ":gt:10:lt:20" );

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
        RequestParams criteria = new RequestParams();
        String filterName = "filter";
        criteria.setFilter( filterName );
        when( dataElementService.getDataElement( filterName ) ).thenReturn( null );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );

        assertEquals( "Data element does not exist: " + filterName, exception.getMessage() );
    }

    @Test
    void testFilterAttributesWhenTEAUidIsDuplicated()
    {
        RequestParams criteria = new RequestParams();
        criteria.setFilterAttributes(
            "TvjwTPToKHO:lt:20" + "," + "cy2oRh2sNr6:lt:20" + "," + "TvjwTPToKHO:gt:30" + "," + "cy2oRh2sNr6:gt:30" );

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
        RequestParams criteria = new RequestParams();
        criteria.setFilterAttributes( TEA_1_UID );

        EventSearchParams params = mapper.map( criteria );

        assertContainsOnly(
            List.of( new QueryItem( tea1, null, tea1.getValueType(), tea1.getAggregationType(), tea1.getOptionSet(),
                tea1.isUnique() ) ),
            params.getFilterAttributes() );
    }

    @Test
    void shouldFailWithForbiddenExceptionWhenUserHasNoAccessToProgram()
    {
        RequestParams criteria = new RequestParams();
        criteria.setProgram( UID.of( program ) );
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
        RequestParams criteria = new RequestParams();
        criteria.setProgramStage( UID.of( programStage ) );
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
        RequestParams criteria = new RequestParams();
        criteria.setAttributeCc( UID.of( "NeU85luyD4w" ) );
        criteria.setAttributeCos( "Cos" );
        CategoryOptionCombo combo = new CategoryOptionCombo();
        combo.setUid( "uid" );
        when( categoryOptionComboService.getAttributeOptionCombo( "NeU85luyD4w", criteria.getAttributeCos(),
            true ) )
            .thenReturn( combo );
        when( aclService.canDataRead( any( User.class ), any( CategoryOptionCombo.class ) ) ).thenReturn( false );

        Exception exception = assertThrows( ForbiddenException.class,
            () -> mapper.map( criteria ) );

        assertEquals( "User has no access to attribute category option combo: " + combo.getUid(),
            exception.getMessage() );
    }

    @Test
    void shouldCreateQueryFilterWhenCriteriaHasMultipleFiltersAndFilterValueWithSplitChars()
        throws ForbiddenException,
        BadRequestException
    {
        RequestParams criteria = new RequestParams();
        criteria.setFilterAttributes( TEA_1_UID + ":like:value\\,with\\,comma" + "," + TEA_2_UID + ":eq:value\\:x" );

        List<QueryFilter> actualFilters = mapper.map( criteria ).getFilterAttributes().stream()
            .flatMap( f -> f.getFilters().stream() )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of(
            new QueryFilter( QueryOperator.LIKE, "value,with,comma" ),
            new QueryFilter( QueryOperator.EQ, "value:x" ) ), actualFilters );
    }
}
