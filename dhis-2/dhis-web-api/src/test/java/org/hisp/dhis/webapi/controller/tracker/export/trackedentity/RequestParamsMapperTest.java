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
package org.hisp.dhis.webapi.controller.tracker.export.trackedentity;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hisp.dhis.DhisConvenienceTest.getDate;
import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
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

/**
 * Tests {@link TrackedEntityParamsMapper}.
 *
 * @author Luciano Fiandesio
 */
@MockitoSettings( strictness = Strictness.LENIENT ) // common setup
@ExtendWith( MockitoExtension.class )
class RequestParamsMapperTest
{
    public static final String TEA_1_UID = "TvjwTPToKHO";

    public static final String TEA_2_UID = "cy2oRh2sNr6";

    private static final String ORG_UNIT_1_UID = "lW0T2U7gZUi";

    private static final String ORG_UNIT_2_UID = "TK4KA0IIWqa";

    private static final String PROGRAM_UID = "XhBYIraw7sv";

    private static final String PROGRAM_STAGE_UID = "RpCr2u2pFqw";

    private static final String TRACKED_ENTITY_TYPE_UID = "Dp8baZYrLtr";

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private ProgramService programService;

    @Mock
    private TrackedEntityAttributeService attributeService;

    @Mock
    private TrackedEntityTypeService trackedEntityTypeService;

    @Mock
    private TrackerAccessManager trackerAccessManager;

    @InjectMocks
    private TrackedEntityParamsMapper mapper;

    private User user;

    private Program program;

    private ProgramStage programStage;

    private OrganisationUnit orgUnit1;

    private OrganisationUnit orgUnit2;

    private TrackedEntityType trackedEntityType;

    private RequestParams criteria;

    @BeforeEach
    public void setUp()
    {
        user = new User();
        when( currentUserService.getCurrentUser() ).thenReturn( user );

        orgUnit1 = new OrganisationUnit( "orgUnit1" );
        orgUnit1.setUid( ORG_UNIT_1_UID );
        when( organisationUnitService.getOrganisationUnit( orgUnit1.getUid() ) ).thenReturn( orgUnit1 );
        when( organisationUnitService.isInUserHierarchy( orgUnit1.getUid(),
            user.getTeiSearchOrganisationUnitsWithFallback() ) ).thenReturn( true );
        orgUnit2 = new OrganisationUnit( "orgUnit2" );
        orgUnit2.setUid( ORG_UNIT_2_UID );
        when( organisationUnitService.getOrganisationUnit( orgUnit2.getUid() ) ).thenReturn( orgUnit2 );
        when( organisationUnitService.isInUserHierarchy( orgUnit2.getUid(),
            user.getTeiSearchOrganisationUnitsWithFallback() ) ).thenReturn( true );

        program = new Program();
        program.setUid( PROGRAM_UID );
        programStage = new ProgramStage();
        programStage.setUid( PROGRAM_STAGE_UID );
        programStage.setProgram( program );
        program.setProgramStages( Set.of( programStage ) );

        when( programService.getProgram( PROGRAM_UID ) ).thenReturn( program );

        TrackedEntityAttribute tea1 = new TrackedEntityAttribute();
        tea1.setUid( TEA_1_UID );
        TrackedEntityAttribute tea2 = new TrackedEntityAttribute();
        tea2.setUid( TEA_2_UID );
        when( attributeService.getAllTrackedEntityAttributes() ).thenReturn( List.of( tea1, tea2 ) );
        when( attributeService.getTrackedEntityAttribute( TEA_1_UID ) ).thenReturn( tea1 );

        trackedEntityType = new TrackedEntityType();
        trackedEntityType.setUid( TRACKED_ENTITY_TYPE_UID );
        when( trackedEntityTypeService.getTrackedEntityType( TRACKED_ENTITY_TYPE_UID ) )
            .thenReturn( trackedEntityType );

        criteria = new RequestParams();
        criteria.setAssignedUserMode( AssignedUserSelectionMode.CURRENT );
    }

    @Test
    void testMapping()
        throws BadRequestException,
        ForbiddenException
    {
        criteria.setQuery( "query-test" );
        criteria.setOuMode( OrganisationUnitSelectionMode.DESCENDANTS );
        criteria.setProgramStatus( ProgramStatus.ACTIVE );
        criteria.setFollowUp( true );
        criteria.setUpdatedAfter( getDate( 2019, 1, 1 ) );
        criteria.setUpdatedBefore( getDate( 2020, 1, 1 ) );
        criteria.setUpdatedWithin( "20" );
        criteria.setEnrollmentOccurredAfter( getDate( 2019, 5, 5 ) );
        criteria.setEnrollmentOccurredBefore( getDate( 2020, 5, 5 ) );
        criteria.setTrackedEntityType( TRACKED_ENTITY_TYPE_UID );
        criteria.setEventStatus( EventStatus.COMPLETED );
        criteria.setEventOccurredAfter( getDate( 2019, 7, 7 ) );
        criteria.setEventOccurredBefore( getDate( 2020, 7, 7 ) );
        criteria.setSkipMeta( true );
        criteria.setPage( 1 );
        criteria.setPageSize( 50 );
        criteria.setTotalPages( false );
        criteria.setSkipPaging( false );
        criteria.setIncludeDeleted( true );
        criteria.setIncludeAllAttributes( true );
        criteria.setOrder( Collections.singletonList( OrderCriteria.of( "created", SortDirection.ASC ) ) );

        final TrackedEntityQueryParams params = mapper.map( criteria );

        assertThat( params.getQuery().getFilter(), is( "query-test" ) );
        assertThat( params.getQuery().getOperator(), is( QueryOperator.EQ ) );
        assertThat( params.getTrackedEntityType(), is( trackedEntityType ) );
        assertThat( params.getPageSizeWithDefault(), is( 50 ) );
        assertThat( params.getPageSize(), is( 50 ) );
        assertThat( params.getPage(), is( 1 ) );
        assertThat( params.isTotalPages(), is( false ) );
        assertThat( params.getProgramStatus(), is( ProgramStatus.ACTIVE ) );
        assertThat( params.getFollowUp(), is( true ) );
        assertThat( params.getLastUpdatedStartDate(), is( criteria.getUpdatedAfter() ) );
        assertThat( params.getLastUpdatedEndDate(), is( criteria.getUpdatedBefore() ) );
        assertThat( params.getProgramIncidentStartDate(), is( criteria.getEnrollmentOccurredAfter() ) );
        assertThat( params.getProgramIncidentEndDate(),
            is( DateUtils.addDays( criteria.getEnrollmentOccurredBefore(), 1 ) ) );
        assertThat( params.getEventStatus(), is( EventStatus.COMPLETED ) );
        assertThat( params.getEventStartDate(), is( criteria.getEventOccurredAfter() ) );
        assertThat( params.getEventEndDate(), is( criteria.getEventOccurredBefore() ) );
        assertThat( params.getAssignedUserQueryParam().getMode(), is( AssignedUserSelectionMode.PROVIDED ) );
        assertThat( params.isIncludeDeleted(), is( true ) );
        assertThat( params.isIncludeAllAttributes(), is( true ) );
        assertTrue( params.getOrders().stream().anyMatch( orderParam -> orderParam
            .equals( new OrderParam( "created", SortDirection.ASC ) ) ) );
    }

    @Test
    void testMappingDoesNotFetchOptionalEmptyQueryParametersFromDB()
        throws BadRequestException,
        ForbiddenException
    {
        mapper.map( criteria );

        verifyNoInteractions( programService );
        verifyNoInteractions( trackedEntityTypeService );
    }

    @Test
    void testMappingProgramEnrollmentStartDate()
        throws BadRequestException,
        ForbiddenException
    {
        Date date = parseDate( "2022-12-13" );
        criteria.setEnrollmentEnrolledAfter( date );

        TrackedEntityQueryParams params = mapper.map( criteria );

        assertEquals( date, params.getProgramEnrollmentStartDate() );
    }

    @Test
    void testMappingProgramEnrollmentEndDate()
        throws BadRequestException,
        ForbiddenException
    {
        Date date = parseDate( "2022-12-13" );
        criteria.setEnrollmentEnrolledBefore( date );

        TrackedEntityQueryParams params = mapper.map( criteria );

        assertEquals( DateUtils.addDays( date, 1 ), params.getProgramEnrollmentEndDate() );
    }

    @Test
    void testFilter()
        throws BadRequestException,
        ForbiddenException
    {
        criteria.setFilter( TEA_1_UID + ":eq:2" + "," + TEA_2_UID + ":like:foo" );

        TrackedEntityQueryParams params = mapper.map( criteria );

        List<QueryItem> items = params.getFilters();
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
    void testFilterWhenTEAHasMultipleFilters()
        throws BadRequestException,
        ForbiddenException
    {
        criteria.setFilter( TEA_1_UID + ":gt:10:lt:20" );

        TrackedEntityQueryParams params = mapper.map( criteria );

        List<QueryItem> items = params.getFilters();
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
    void shouldFailWithBadExceptionWhenBadFormattedQueryProvided()
    {
        String queryWithBadFormat = "wrong-query:";

        criteria.setQuery( "wrong-query:" );

        BadRequestException e = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );

        assertEquals( "Query item or filter is invalid: " + queryWithBadFormat, e.getMessage() );
    }

    @Test
    void testFilterWhenTEAFilterIsRepeated()
    {
        criteria.setFilter( TEA_1_UID + ":gt:10" + "," + TEA_1_UID + ":lt:20" );

        BadRequestException e = assertThrows( BadRequestException.class, () -> mapper.map( criteria ) );

        assertStartsWith( "Filter for attribute TvjwTPToKHO was specified more than once.", e.getMessage() );
        assertThat( e.getMessage(), allOf( containsString( "GT:10" ), containsString( "LT:20" ) ) );
        assertThat( e.getMessage(),
            anyOf( containsString( "TvjwTPToKHO:GT:10" ), containsString( "TvjwTPToKHO:LT:20" ) ) );
    }

    @Test
    void testFilterWhenMultipleTEAFiltersAreRepeated()
    {
        criteria.setFilter( TEA_1_UID + ":gt:10" + "," + TEA_1_UID + ":lt:20" + "," +
            TEA_2_UID + ":gt:30" + "," + TEA_2_UID + ":lt:40" );

        BadRequestException e = assertThrows( BadRequestException.class, () -> mapper.map( criteria ) );

        assertThat( e.getMessage(),
            containsString( "Filter for attribute " + TEA_1_UID + " was specified more than once." ) );
        assertThat( e.getMessage(), allOf( containsString( "GT:10" ), containsString( "LT:20" ) ) );
        assertThat( e.getMessage(),
            anyOf( containsString( TEA_1_UID + ":GT:10" ), containsString( TEA_1_UID + ":LT:20" ) ) );
        assertThat( e.getMessage(),
            containsString( "Filter for attribute " + TEA_2_UID + " was specified more than once." ) );
        assertThat( e.getMessage(), allOf( containsString( "GT:30" ), containsString( "LT:40" ) ) );
        assertThat( e.getMessage(),
            anyOf( containsString( TEA_2_UID + ":GT:30" ), containsString( TEA_2_UID + ":LT:40" ) ) );
    }

    @Test
    void testAttributes()
        throws BadRequestException,
        ForbiddenException
    {
        criteria.setAttribute( TEA_1_UID + "," + TEA_2_UID );

        TrackedEntityQueryParams params = mapper.map( criteria );

        List<QueryItem> items = params.getAttributes();
        assertNotNull( items );
        // mapping to UIDs as the error message by just relying on QueryItem
        // equals() is not helpful
        assertContainsOnly( List.of( TEA_1_UID,
            TEA_2_UID ), items.stream().map( i -> i.getItem().getUid() ).collect( Collectors.toList() ) );
    }

    @Test
    void testMappingAttributeWhenAttributeDoesNotExist()
    {
        criteria.setAttribute( TEA_1_UID + "," + "unknown" );

        BadRequestException e = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Attribute does not exist: unknown", e.getMessage() );
    }

    @Test
    void testMappingFailsOnMissingAttribute()
    {
        criteria.setAttribute( TEA_1_UID + "," + "unknown" );

        BadRequestException e = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Attribute does not exist: unknown", e.getMessage() );
    }

    @Test
    void testMappingProgram()
        throws BadRequestException,
        ForbiddenException
    {
        criteria.setProgram( PROGRAM_UID );

        TrackedEntityQueryParams params = mapper.map( criteria );

        assertEquals( program, params.getProgram() );
    }

    @Test
    void testMappingProgramNotFound()
    {
        criteria.setProgram( "unknown" );

        BadRequestException e = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Program is specified but does not exist: unknown", e.getMessage() );
    }

    @Test
    void testMappingProgramStage()
        throws BadRequestException,
        ForbiddenException
    {
        criteria.setProgram( PROGRAM_UID );
        criteria.setProgramStage( PROGRAM_STAGE_UID );

        TrackedEntityQueryParams params = mapper.map( criteria );

        assertEquals( programStage, params.getProgramStage() );
    }

    @Test
    void testMappingProgramStageGivenWithoutProgram()
    {
        criteria.setProgramStage( PROGRAM_STAGE_UID );

        BadRequestException e = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Program does not contain the specified programStage: " + PROGRAM_STAGE_UID, e.getMessage() );
    }

    @Test
    void testMappingProgramStageNotFound()
    {
        criteria.setProgramStage( "unknown" );

        BadRequestException e = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Program does not contain the specified programStage: unknown", e.getMessage() );
    }

    @Test
    void testMappingTrackedEntityType()
        throws BadRequestException,
        ForbiddenException
    {
        criteria.setTrackedEntityType( TRACKED_ENTITY_TYPE_UID );

        TrackedEntityQueryParams params = mapper.map( criteria );

        assertEquals( trackedEntityType, params.getTrackedEntityType() );
    }

    @Test
    void testMappingTrackedEntityTypeNotFound()
    {
        criteria.setTrackedEntityType( "unknown" );

        BadRequestException e = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Tracked entity type does not exist: unknown", e.getMessage() );
    }

    @Test
    void testMappingOrgUnit()
        throws BadRequestException,
        ForbiddenException
    {
        when( trackerAccessManager.canAccess( user, program, orgUnit1 ) ).thenReturn( true );
        when( trackerAccessManager.canAccess( user, program, orgUnit2 ) ).thenReturn( true );
        criteria.setOrgUnit( ORG_UNIT_1_UID + ";" + ORG_UNIT_2_UID );
        criteria.setProgram( PROGRAM_UID );

        TrackedEntityQueryParams params = mapper.map( criteria );

        assertContainsOnly( Set.of( orgUnit1, orgUnit2 ), params.getOrganisationUnits() );
    }

    @Test
    void testMappingOrgUnitNotFound()
    {
        criteria.setOrgUnit( "unknown" );

        BadRequestException e = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Organisation unit does not exist: unknown", e.getMessage() );
    }

    @Test
    void shouldThrowExceptionWhenOrgUnitNotInScope()
    {
        when( organisationUnitService.isInUserHierarchy( orgUnit1.getUid(),
            user.getTeiSearchOrganisationUnitsWithFallback() ) ).thenReturn( false );

        criteria.setOrgUnit( ORG_UNIT_1_UID );

        ForbiddenException e = assertThrows( ForbiddenException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "User does not have access to organisation unit: " + ORG_UNIT_1_UID, e.getMessage() );
    }

    @Test
    void testMappingAssignedUsers()
        throws BadRequestException,
        ForbiddenException
    {
        criteria.setAssignedUser( "IsdLBTOBzMi;invalid;l5ab8q5skbB" );
        criteria.setAssignedUserMode( AssignedUserSelectionMode.PROVIDED );

        TrackedEntityQueryParams params = mapper.map( criteria );

        assertContainsOnly( Set.of( "IsdLBTOBzMi", "l5ab8q5skbB" ),
            params.getAssignedUserQueryParam().getAssignedUsers() );
        assertEquals( AssignedUserSelectionMode.PROVIDED, params.getAssignedUserQueryParam().getMode() );
    }

    @Test
    void testMappingOrderParams()
        throws BadRequestException,
        ForbiddenException
    {
        OrderCriteria order1 = OrderCriteria.of( "trackedEntity", SortDirection.ASC );
        OrderCriteria order2 = OrderCriteria.of( "createdAt", SortDirection.DESC );
        criteria.setOrder( List.of( order1, order2 ) );

        TrackedEntityQueryParams params = mapper.map( criteria );

        assertEquals( List.of(
            new OrderParam( "trackedEntity", SortDirection.ASC ),
            new OrderParam( "createdAt", SortDirection.DESC ) ), params.getOrders() );
    }

    @Test
    void testMappingOrderParamsNoOrder()
        throws BadRequestException,
        ForbiddenException
    {
        TrackedEntityQueryParams params = mapper.map( criteria );

        assertIsEmpty( params.getOrders() );
    }

    @Test
    void testMappingOrderParamsGivenInvalidField()
    {
        OrderCriteria order1 = OrderCriteria.of( "invalid", SortDirection.DESC );
        criteria.setOrder( List.of( order1 ) );

        BadRequestException e = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Invalid order property: invalid", e.getMessage() );
    }

    @Test
    void shouldCreateCriteriaFiltersWithFirstOperatorWhenMultipleValidOperandAreNotValid()
        throws BadRequestException,
        ForbiddenException
    {
        criteria.setFilter( TEA_2_UID + ":like:project\\:x\\:eq\\:2" );
        TrackedEntityQueryParams params = mapper.map( criteria );

        List<QueryFilter> actualFilters = params.getFilters().stream().flatMap( f -> f.getFilters().stream() )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of(
            new QueryFilter( QueryOperator.LIKE, "project:x:eq:2" ) ), actualFilters );
    }

    @Test
    void shouldThrowBadRequestWhenCriteriaFilterHasOperatorInWrongFormat()
    {
        criteria.setFilter( TEA_1_UID + ":lke:value" );
        BadRequestException exception = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Query item or filter is invalid: " + TEA_1_UID + ":lke:value", exception.getMessage() );
    }

    @Test
    void shouldCreateQueryFilterWhenCriteriaFilterHasDatesFormatWithHours()
        throws ForbiddenException,
        BadRequestException
    {
        criteria.setFilter( TEA_1_UID + ":gt:01-01-2000 00\\:00\\:01:lt:01-01-2001 00\\:00\\:01" );

        List<QueryFilter> actualFilters = mapper.map( criteria ).getFilters().stream()
            .flatMap( f -> f.getFilters().stream() )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of(
            new QueryFilter( QueryOperator.GT, "01-01-2000 00:00:01" ),
            new QueryFilter( QueryOperator.LT, "01-01-2001 00:00:01" ) ), actualFilters );
    }

    @Test
    void shouldCreateQueryFilterWhenCriteriaFilterHasDatesFormatYearMonthDay()
        throws ForbiddenException,
        BadRequestException
    {
        criteria.setFilter( TEA_1_UID + ":gt:01-01-2000:lt:01-01-2001" );

        List<QueryFilter> actualFilters = mapper.map( criteria ).getFilters().stream()
            .flatMap( f -> f.getFilters().stream() )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of(
            new QueryFilter( QueryOperator.GT, "01-01-2000" ),
            new QueryFilter( QueryOperator.LT, "01-01-2001" ) ), actualFilters );
    }

    @Test
    void shouldCreateQueryFilterWhenCriteriaFilterHasDatesFormatDateWithTimeZone()
        throws ForbiddenException,
        BadRequestException
    {
        criteria.setFilter( TEA_1_UID + ":gt:2020-01-01T00\\:00\\:00 +05\\:30:lt:2021-01-01T00\\:00\\:00 +05\\:30" );

        List<QueryFilter> actualFilters = mapper.map( criteria ).getFilters().stream()
            .flatMap( f -> f.getFilters().stream() )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of(
            new QueryFilter( QueryOperator.GT, "2020-01-01T00:00:00 +05:30" ),
            new QueryFilter( QueryOperator.LT, "2021-01-01T00:00:00 +05:30" ) ), actualFilters );
    }

    @Test
    void shouldCreateQueryFilterWhenCriteriaFilterHasDatesFormatDateWithMilliSecondsAndTimeZone()
        throws ForbiddenException,
        BadRequestException
    {
        criteria
            .setFilter(
                TEA_1_UID + ":ge:2020-01-01T00\\:00\\:00.001 +05\\:30:le:2021-01-01T00\\:00\\:00.001 +05\\:30" );

        List<QueryFilter> actualFilters = mapper.map( criteria ).getFilters().stream()
            .flatMap( f -> f.getFilters().stream() )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of(
            new QueryFilter( QueryOperator.GE, "2020-01-01T00:00:00.001 +05:30" ),
            new QueryFilter( QueryOperator.LE, "2021-01-01T00:00:00.001 +05:30" ) ), actualFilters );
    }

    @Test
    void shouldCreateQueryFilterWhenCriteriaFilterHasMultipleOperatorAndTextRange()
        throws ForbiddenException,
        BadRequestException
    {
        criteria
            .setFilter( TEA_1_UID + ":sw:project\\:x:ew:project\\:le\\:" );

        List<QueryFilter> actualFilters = mapper.map( criteria ).getFilters().stream()
            .flatMap( f -> f.getFilters().stream() )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of(
            new QueryFilter( QueryOperator.SW, "project:x" ),
            new QueryFilter( QueryOperator.EW, "project:le:" ) ), actualFilters );
    }

    @Test
    void shouldCreateQueryFilterWhenCriteriaHasMultipleFiltersAndFilterValueWithComma()
        throws ForbiddenException,
        BadRequestException
    {
        criteria
            .setFilter( TEA_1_UID + ":like:value\\,with\\,comma" + "," + TEA_2_UID + ":eq:value" );

        List<QueryFilter> actualFilters = mapper.map( criteria ).getFilters().stream()
            .flatMap( f -> f.getFilters().stream() )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of(
            new QueryFilter( QueryOperator.LIKE, "value\\,with\\,comma" ),
            new QueryFilter( QueryOperator.EQ, "value" ) ), actualFilters );
    }

    @Test
    void shouldCreateQueryFilterWhenCriteriaFilterHasEscapeValue()
        throws ForbiddenException,
        BadRequestException
    {
        criteria
            .setFilter( TEA_1_UID + ":sw:project\\" );

        List<QueryFilter> actualFilters = mapper.map( criteria ).getFilters().stream()
            .flatMap( f -> f.getFilters().stream() )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of(
            new QueryFilter( QueryOperator.SW, "project\\" ) ), actualFilters );
    }
}
