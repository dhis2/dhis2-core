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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.DhisConvenienceTest.getDate;
import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
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

/**
 * Tests {@link TrackerTrackedEntityCriteriaMapper}.
 *
 * @author Luciano Fiandesio
 */
@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class TrackerTrackedEntityCriteriaMapperTest
{
    public static final String TEA_1_UID = "TvjwTPToKHO";

    public static final String TEA_2_UID = "cy2oRh2sNr6";

    private static final String ORG_UNIT_1_UID = "lW0T2U7gZUi";

    private static final String ORG_UNIT_2_UID = "TK4KA0IIWqa";

    private static final String PROGRAM_UID = "XhBYIraw7sv";

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

    @InjectMocks
    private TrackerTrackedEntityCriteriaMapper mapper;

    private User user;

    private Program program;

    private OrganisationUnit orgUnit1;

    private OrganisationUnit orgUnit2;

    private TrackedEntityType trackedEntityType;

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
    }

    @Test
    void verifyCriteriaMapping()
    {
        TrackerTrackedEntityCriteria criteria = new TrackerTrackedEntityCriteria();
        criteria.setQuery( "query-test" );
        criteria.setOuMode( OrganisationUnitSelectionMode.DESCENDANTS );
        criteria.setProgramStatus( ProgramStatus.ACTIVE );
        criteria.setFollowUp( true );
        criteria.setUpdatedAfter( getDate( 2019, 1, 1 ) );
        criteria.setUpdatedBefore( getDate( 2020, 1, 1 ) );
        criteria.setUpdatedWithin( "20" );
        criteria.setEnrollmentEnrolledAfter( getDate( 2019, 8, 5 ) );
        criteria.setEnrollmentEnrolledBefore( getDate( 2020, 8, 5 ) );
        criteria.setEnrollmentOccurredAfter( getDate( 2019, 5, 5 ) );
        criteria.setEnrollmentOccurredBefore( getDate( 2020, 5, 5 ) );
        criteria.setTrackedEntityType( TRACKED_ENTITY_TYPE_UID );
        criteria.setEventStatus( EventStatus.COMPLETED );
        criteria.setEventOccurredAfter( getDate( 2019, 7, 7 ) );
        criteria.setEventOccurredBefore( getDate( 2020, 7, 7 ) );
        criteria.setAssignedUserMode( AssignedUserSelectionMode.PROVIDED );
        criteria.setSkipMeta( true );
        criteria.setPage( 1 );
        criteria.setPageSize( 50 );
        criteria.setTotalPages( false );
        criteria.setSkipPaging( false );
        criteria.setIncludeDeleted( true );
        criteria.setIncludeAllAttributes( true );
        criteria.setOrder( Collections.singletonList( OrderCriteria.of( "created", OrderParam.SortDirection.ASC ) ) );

        final TrackedEntityInstanceQueryParams params = mapper.map( criteria );

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
        assertThat( params.getProgramEnrollmentStartDate(), is( criteria.getEnrollmentEnrolledAfter() ) );
        assertThat( params.getProgramEnrollmentEndDate(),
            is( DateUtils.addDays( criteria.getEnrollmentEnrolledBefore(), 1 ) ) );
        assertThat( params.getProgramIncidentStartDate(), is( criteria.getEnrollmentOccurredAfter() ) );
        assertThat( params.getProgramIncidentEndDate(),
            is( DateUtils.addDays( criteria.getEnrollmentOccurredBefore(), 1 ) ) );
        assertThat( params.getEventStatus(), is( EventStatus.COMPLETED ) );
        assertThat( params.getEventStartDate(), is( criteria.getEventOccurredAfter() ) );
        assertThat( params.getEventEndDate(), is( criteria.getEventOccurredBefore() ) );
        assertThat( params.getAssignedUserSelectionMode(), is( AssignedUserSelectionMode.PROVIDED ) );
        assertThat( params.isIncludeDeleted(), is( true ) );
        assertThat( params.isIncludeAllAttributes(), is( true ) );
        assertTrue( params.getOrders().stream().anyMatch( orderParam -> orderParam
            .equals( new OrderParam( "created", OrderParam.SortDirection.ASC ) ) ) );
    }

    @Test
    void testMappingProgramEnrollmentStartDate()
    {
        TrackerTrackedEntityCriteria criteria = new TrackerTrackedEntityCriteria();
        Date date = parseDate( "2022-12-13" );
        criteria.setEnrollmentEnrolledAfter( date );

        TrackedEntityInstanceQueryParams params = mapper.map( criteria );

        assertEquals( date, params.getProgramEnrollmentStartDate() );
    }

    @Test
    void testMappingProgramEnrollmentEndDate()
    {
        TrackerTrackedEntityCriteria criteria = new TrackerTrackedEntityCriteria();
        Date date = parseDate( "2022-12-13" );
        criteria.setEnrollmentEnrolledBefore( date );

        TrackedEntityInstanceQueryParams params = mapper.map( criteria );

        assertEquals( DateUtils.addDays( date, 1 ), params.getProgramEnrollmentEndDate() );
    }

    @Test
    void testFilter()
    {
        TrackerTrackedEntityCriteria criteria = new TrackerTrackedEntityCriteria();
        criteria.setFilter( Set.of( TEA_1_UID + ":eq:2", TEA_2_UID + ":like:foo" ) );

        TrackedEntityInstanceQueryParams params = mapper.map( criteria );

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
    {
        TrackerTrackedEntityCriteria criteria = new TrackerTrackedEntityCriteria();
        criteria.setFilter( Set.of( TEA_1_UID + ":gt:10:lt:20" ) );

        TrackedEntityInstanceQueryParams params = mapper.map( criteria );

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
    void testFilterWhenNumberOfFilterSegmentsIsEven()
    {
        when( attributeService.getAllTrackedEntityAttributes() ).thenReturn( Collections.emptyList() );

        TrackerTrackedEntityCriteria criteria = new TrackerTrackedEntityCriteria();
        criteria.setFilter( Set.of( "eq:2" ) );

        Exception exception = assertThrows( IllegalQueryException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Query item or filter is invalid: eq:2", exception.getMessage() );
    }

    @Test
    void testFilterWhenNoTEAExist()
    {
        when( attributeService.getAllTrackedEntityAttributes() ).thenReturn( Collections.emptyList() );

        TrackerTrackedEntityCriteria criteria = new TrackerTrackedEntityCriteria();
        criteria.setFilter( Set.of( TEA_1_UID + ":eq:2" ) );

        Exception exception = assertThrows( IllegalQueryException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Attribute does not exist: " + TEA_1_UID, exception.getMessage() );
    }

    @Test
    void testFilterWhenTEAInFilterDoesNotExist()
    {
        TrackerTrackedEntityCriteria criteria = new TrackerTrackedEntityCriteria();
        criteria.setFilter( Set.of( "JM5zWuf1mkb:eq:2" ) );

        Exception exception = assertThrows( IllegalQueryException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Attribute does not exist: JM5zWuf1mkb", exception.getMessage() );
    }

    @Test
    void testAttributes()
    {
        TrackerTrackedEntityCriteria criteria = new TrackerTrackedEntityCriteria();
        criteria.setAttribute( Set.of( TEA_1_UID, TEA_2_UID ) );

        TrackedEntityInstanceQueryParams params = mapper.map( criteria );

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
        TrackerTrackedEntityCriteria criteria = new TrackerTrackedEntityCriteria();
        criteria.setAttribute( Set.of( TEA_1_UID, "unknown" ) );

        IllegalQueryException e = assertThrows( IllegalQueryException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Attribute does not exist: unknown", e.getMessage() );
    }

    @Test
    void testMappingFailsOnMissingAttribute()
    {
        TrackerTrackedEntityCriteria criteria = new TrackerTrackedEntityCriteria();
        criteria.setAttribute( Set.of( TEA_1_UID, "unknown" ) );

        IllegalQueryException e = assertThrows( IllegalQueryException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Attribute does not exist: unknown", e.getMessage() );
    }

    @Test
    void testMappingProgram()
    {
        TrackerTrackedEntityCriteria criteria = new TrackerTrackedEntityCriteria();
        criteria.setProgram( PROGRAM_UID );

        TrackedEntityInstanceQueryParams params = mapper.map( criteria );

        assertEquals( program, params.getProgram() );
    }

    @Test
    void testMappingProgramNotFound()
    {
        TrackerTrackedEntityCriteria criteria = new TrackerTrackedEntityCriteria();
        criteria.setProgram( "unknown" );

        IllegalQueryException e = assertThrows( IllegalQueryException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Program does not exist: unknown", e.getMessage() );
    }

    @Test
    void testMappingTrackedEntityTypeNotFound()
    {
        TrackerTrackedEntityCriteria criteria = new TrackerTrackedEntityCriteria();
        criteria.setTrackedEntityType( "unknown" );

        IllegalQueryException e = assertThrows( IllegalQueryException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Tracked entity type does not exist: unknown", e.getMessage() );
    }

    @Test
    void testMappingOrgUnit()
    {
        TrackerTrackedEntityCriteria criteria = new TrackerTrackedEntityCriteria();
        criteria.setOrgUnit( ORG_UNIT_1_UID + ";" + ORG_UNIT_2_UID );

        TrackedEntityInstanceQueryParams params = mapper.map( criteria );

        assertContainsOnly( Set.of( orgUnit1, orgUnit2 ), params.getOrganisationUnits() );
    }

    @Test
    void testMappingOrgUnitNotFound()
    {
        TrackerTrackedEntityCriteria criteria = new TrackerTrackedEntityCriteria();
        criteria.setOrgUnit( "unknown" );

        IllegalQueryException e = assertThrows( IllegalQueryException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Organisation unit does not exist: unknown", e.getMessage() );
    }

    @Test
    void testMappingOrgUnitNotInSearchScope()
    {
        when( organisationUnitService.isInUserHierarchy( orgUnit1.getUid(),
            user.getTeiSearchOrganisationUnitsWithFallback() ) ).thenReturn( false );

        TrackerTrackedEntityCriteria criteria = new TrackerTrackedEntityCriteria();
        criteria.setOrgUnit( ORG_UNIT_1_UID );

        IllegalQueryException e = assertThrows( IllegalQueryException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Organisation unit is not part of the search scope: " + ORG_UNIT_1_UID, e.getMessage() );
    }

    @Test
    void testMappingAssignedUsers()
    {
        TrackerTrackedEntityCriteria criteria = new TrackerTrackedEntityCriteria();
        criteria.setAssignedUser( "IsdLBTOBzMi;invalid;l5ab8q5skbB" );

        TrackedEntityInstanceQueryParams params = mapper.map( criteria );

        assertContainsOnly( Set.of( "IsdLBTOBzMi", "l5ab8q5skbB" ), params.getAssignedUsers() );
    }

    @Test
    void testMappingFailsOnNonProvidedAndAssignedUsers()
    {
        TrackerTrackedEntityCriteria criteria = new TrackerTrackedEntityCriteria();
        criteria.setAssignedUser( "IsdLBTOBzMi;l5ab8q5skbB" );
        criteria.setAssignedUserMode( AssignedUserSelectionMode.CURRENT );

        IllegalQueryException e = assertThrows( IllegalQueryException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Assigned User uid(s) cannot be specified if selectionMode is not PROVIDED", e.getMessage() );
    }

    @Test
    void testMappingOrderParams()
    {
        TrackerTrackedEntityCriteria criteria = new TrackerTrackedEntityCriteria();
        OrderCriteria order1 = OrderCriteria.of( "trackedEntity", OrderParam.SortDirection.ASC );
        OrderCriteria order2 = OrderCriteria.of( "createdAt", OrderParam.SortDirection.DESC );
        criteria.setOrder( List.of( order1, order2 ) );

        TrackedEntityInstanceQueryParams params = mapper.map( criteria );

        assertEquals( List.of(
            new OrderParam( "trackedEntity", OrderParam.SortDirection.ASC ),
            new OrderParam( "createdAt", OrderParam.SortDirection.DESC ) ), params.getOrders() );
    }

    @Test
    void testMappingOrderParamsNoOrder()
    {
        TrackerTrackedEntityCriteria criteria = new TrackerTrackedEntityCriteria();

        TrackedEntityInstanceQueryParams params = mapper.map( criteria );

        assertIsEmpty( params.getOrders() );
    }

    @Test
    void testMappingOrderParamsGivenInvalidField()
    {
        TrackerTrackedEntityCriteria criteria = new TrackerTrackedEntityCriteria();
        OrderCriteria order1 = OrderCriteria.of( "invalid", OrderParam.SortDirection.DESC );
        criteria.setOrder( List.of( order1 ) );

        IllegalQueryException e = assertThrows( IllegalQueryException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Invalid order property: invalid", e.getMessage() );
    }
}
