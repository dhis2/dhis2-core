/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.export.event;

import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
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

@ExtendWith( MockitoExtension.class )
class EventOperationsMapperTest
{
    private static final String TEA_1_UID = "TvjwTPToKHO";

    private static final String TEA_2_UID = "cy2oRh2sNr6";

    private static final String PROGRAM_UID = "PlZSBEN7iZd";

    @Mock
    private ProgramService programService;

    @Mock
    private ProgramStageService programStageService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private TrackedEntityService trackedEntityService;

    @Mock
    private AclService aclService;

    @Mock
    private CategoryOptionComboService categoryOptionComboService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private TrackedEntityAttributeService trackedEntityAttributeService;

    @InjectMocks
    private EventOperationParamsMapper mapper;

    private ProgramStage programStage;

    private User user;

    @BeforeEach
    public void setUp()
    {
        user = new User();
        when( currentUserService.getCurrentUser() ).thenReturn( user );
    }

    @Test
    void shouldFailWithForbiddenExceptionWhenUserHasNoAccessToProgramStage()
    {
        programStage = new ProgramStage();
        programStage.setUid( "PlZSBEN7iZd" );
        EventOperationParams eventOperationParams = EventOperationParams.builder()
            .programStageUid( programStage.getUid() ).build();

        when( aclService.canDataRead( user, programStage ) ).thenReturn( false );
        when( programStageService.getProgramStage( "PlZSBEN7iZd" ) ).thenReturn( programStage );

        Exception exception = assertThrows( ForbiddenException.class,
            () -> mapper.map( eventOperationParams ) );
        assertEquals( "User has no access to program stage: " + programStage.getUid(), exception.getMessage() );
    }

    @Test
    void shouldFailWithBadRequestExceptionWhenTrackedEntityDoesNotExist()
    {
        programStage = new ProgramStage();
        programStage.setUid( "PlZSBEN7iZd" );
        EventOperationParams eventOperationParams = EventOperationParams.builder()
            .programStageUid( programStage.getUid() )
            .trackedEntityUid( "qnR1RK4cTIZ" ).build();

        when( programStageService.getProgramStage( "PlZSBEN7iZd" ) ).thenReturn( programStage );
        when( aclService.canDataRead( user, programStage ) ).thenReturn( true );
        when( trackedEntityService.getTrackedEntity( "qnR1RK4cTIZ" ) ).thenReturn( null );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( eventOperationParams ) );
        assertStartsWith(
            "Tracked entity is specified but does not exist: " + eventOperationParams.getTrackedEntityUid(),
            exception.getMessage() );
    }

    @Test
    void shouldFailWithForbiddenExceptionWhenUserHasNoAccessToProgram()
    {
        Program program = new Program();
        program.setUid( PROGRAM_UID );
        EventOperationParams eventOperationParams = EventOperationParams.builder().programUid( program.getUid() )
            .build();

        when( programService.getProgram( PROGRAM_UID ) ).thenReturn( program );
        when( aclService.canDataRead( user, program ) ).thenReturn( false );

        Exception exception = assertThrows( ForbiddenException.class,
            () -> mapper.map( eventOperationParams ) );
        assertEquals( "User has no access to program: " + program.getUid(), exception.getMessage() );
    }

    @Test
    void shouldFailWithBadRequestExceptionWhenMappingWithUnknownProgramStage()
    {
        EventOperationParams eventOperationParams = EventOperationParams.builder().programStageUid( "NeU85luyD4w" )
            .build();

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( eventOperationParams ) );
        assertEquals( "Program stage is specified but does not exist: NeU85luyD4w", exception.getMessage() );
    }

    @Test
    void shouldFailWithBadRequestExceptionWhenMappingWithUnknownProgram()
    {
        EventOperationParams eventOperationParams = EventOperationParams.builder().programUid( "NeU85luyD4w" ).build();

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( eventOperationParams ) );
        assertEquals( "Program is specified but does not exist: NeU85luyD4w", exception.getMessage() );
    }

    @Test
    void shouldFailWithBadRequestExceptionWhenMappingCriteriaWithUnknownOrgUnit()
    {
        EventOperationParams eventOperationParams = EventOperationParams.builder().orgUnitUid( "NeU85luyD4w" ).build();
        when( organisationUnitService.getOrganisationUnit( any() ) ).thenReturn( null );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( eventOperationParams ) );

        assertEquals( "Org unit is specified but does not exist: NeU85luyD4w", exception.getMessage() );
    }

    @Test
    void shouldFailWithForbiddenExceptionWhenUserHasNoAccessToCategoryComboGivenAttributeCategoryOptions()
    {
        EventOperationParams eventOperationParams = EventOperationParams.builder()
            .attributeCategoryCombo( "NeU85luyD4w" ).attributeCategoryOptions( Set.of( "tqrzUqNMHib", "bT6OSf4qnnk" ) )
            .build();
        CategoryOptionCombo combo = new CategoryOptionCombo();
        combo.setUid( "uid" );
        when( categoryOptionComboService.getAttributeOptionCombo( "NeU85luyD4w", Set.of( "tqrzUqNMHib", "bT6OSf4qnnk" ),
            true ) )
            .thenReturn( combo );
        when( aclService.canDataRead( any( User.class ), any( CategoryOptionCombo.class ) ) ).thenReturn( false );

        Exception exception = assertThrows( ForbiddenException.class,
            () -> mapper.map( eventOperationParams ) );

        assertEquals( "User has no access to attribute category option combo: " + combo.getUid(),
            exception.getMessage() );
    }

    @Test
    void shouldMapGivenAttributeCategoryOptionsWhenUserHasAccessToCategoryCombo()
        throws BadRequestException,
        ForbiddenException
    {
        EventOperationParams operationParams = EventOperationParams.builder().attributeCategoryCombo( "NeU85luyD4w" )
            .attributeCategoryOptions( Set.of( "tqrzUqNMHib", "bT6OSf4qnnk" ) ).build();
        CategoryOptionCombo combo = new CategoryOptionCombo();
        combo.setUid( "uid" );
        when( categoryOptionComboService.getAttributeOptionCombo( "NeU85luyD4w", Set.of( "tqrzUqNMHib", "bT6OSf4qnnk" ),
            true ) )
            .thenReturn( combo );
        when( aclService.canDataRead( any( User.class ), any( CategoryOptionCombo.class ) ) ).thenReturn( true );
        when( trackedEntityAttributeService.getAllTrackedEntityAttributes() ).thenReturn( Collections.emptyList() );

        EventSearchParams searchParams = mapper.map( operationParams );

        assertEquals( combo, searchParams.getCategoryOptionCombo() );
    }

    @Test
    void testMappingAssignedUser()
        throws BadRequestException,
        ForbiddenException
    {
        EventOperationParams requestParams = EventOperationParams.builder()
            .assignedUsers( Set.of( "IsdLBTOBzMi", "l5ab8q5skbB" ) )
            .assignedUserMode( AssignedUserSelectionMode.PROVIDED ).build();

        EventSearchParams params = mapper.map( requestParams );

        assertContainsOnly( Set.of( "IsdLBTOBzMi", "l5ab8q5skbB" ),
            params.getAssignedUserQueryParam().getAssignedUsers() );
        assertEquals( AssignedUserSelectionMode.PROVIDED, params.getAssignedUserQueryParam().getMode() );
    }

    @Test
    void testFilterAttributes()
        throws BadRequestException,
        ForbiddenException
    {
        TrackedEntityAttribute tea1 = new TrackedEntityAttribute();
        tea1.setUid( TEA_1_UID );
        TrackedEntityAttribute tea2 = new TrackedEntityAttribute();
        tea2.setUid( TEA_2_UID );
        when( trackedEntityAttributeService.getAllTrackedEntityAttributes() ).thenReturn( List.of( tea1, tea2 ) );
        EventOperationParams requestParams = EventOperationParams.builder()
            .filterAttributes( Set.of( TEA_1_UID + ":eq:2", TEA_2_UID + ":like:foo" ) ).build();

        EventSearchParams searchParams = mapper.map( requestParams );

        List<QueryItem> items = searchParams.getFilterAttributes();
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
        throws BadRequestException,
        ForbiddenException
    {
        TrackedEntityAttribute tea1 = new TrackedEntityAttribute();
        tea1.setUid( TEA_1_UID );
        when( trackedEntityAttributeService.getAllTrackedEntityAttributes() ).thenReturn( List.of( tea1 ) );
        EventOperationParams operationParams = EventOperationParams.builder()
            .filterAttributes( Set.of( TEA_1_UID + ":gt:10:lt:20" ) ).build();

        EventSearchParams searchParams = mapper.map( operationParams );

        List<QueryItem> items = searchParams.getFilterAttributes();
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
    void testFilterAttributesUsingOnlyUID()
        throws BadRequestException,
        ForbiddenException
    {
        TrackedEntityAttribute tea1 = new TrackedEntityAttribute();
        tea1.setUid( TEA_1_UID );
        when( trackedEntityAttributeService.getAllTrackedEntityAttributes() ).thenReturn( List.of( tea1 ) );
        EventOperationParams operationParams = EventOperationParams.builder().filterAttributes( Set.of( TEA_1_UID ) )
            .build();

        EventSearchParams params = mapper.map( operationParams );

        assertContainsOnly(
            List.of( new QueryItem( tea1, null, tea1.getValueType(), tea1.getAggregationType(), tea1.getOptionSet(),
                tea1.isUnique() ) ),
            params.getFilterAttributes() );
    }

    @Test
    void testFilterAttributesWhenTEAUidIsDuplicated()
    {
        TrackedEntityAttribute tea1 = new TrackedEntityAttribute();
        tea1.setUid( TEA_1_UID );
        TrackedEntityAttribute tea2 = new TrackedEntityAttribute();
        tea2.setUid( TEA_2_UID );
        when( trackedEntityAttributeService.getAllTrackedEntityAttributes() ).thenReturn( List.of( tea1, tea2 ) );
        EventOperationParams operationParams = EventOperationParams.builder()
            .filterAttributes(
                Set.of( "TvjwTPToKHO:lt:20", "cy2oRh2sNr6:lt:20", "TvjwTPToKHO:gt:30", "cy2oRh2sNr6:gt:30" ) )
            .build();

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( operationParams ) );
        assertAll(
            () -> assertStartsWith( "filterAttributes contains duplicate tracked entity attribute",
                exception.getMessage() ),
            // order of TEA UIDs might not always be the same; therefore using
            // contains
            () -> assertContains( TEA_1_UID, exception.getMessage() ),
            () -> assertContains( TEA_2_UID, exception.getMessage() ) );
    }

    @Test
    void testMappingAttributeOrdering()
        throws BadRequestException,
        ForbiddenException
    {
        TrackedEntityAttribute tea1 = new TrackedEntityAttribute();
        tea1.setUid( TEA_1_UID );
        when( trackedEntityAttributeService.getAllTrackedEntityAttributes() ).thenReturn( List.of( tea1 ) );
        when( trackedEntityAttributeService.getTrackedEntityAttribute( "TvjwTPToKHO" ) ).thenReturn( tea1 );
        EventOperationParams operationParams = EventOperationParams.builder()
            .attributeOrders( List.of( OrderCriteria.of( TEA_1_UID, SortDirection.ASC ),
                OrderCriteria.of( "unknownAtt1", SortDirection.ASC ) ) )
            .filterAttributes( Set.of( TEA_1_UID ) )
            .build();

        EventSearchParams params = mapper.map( operationParams );

        assertAll(
            () -> assertContainsOnly( params.getAttributeOrders(),
                List.of( new OrderParam( TEA_1_UID, SortDirection.ASC ) ) ) );
    }

}
