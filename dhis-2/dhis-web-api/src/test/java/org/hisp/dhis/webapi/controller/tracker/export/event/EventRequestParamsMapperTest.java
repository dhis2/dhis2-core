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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.BadRequestException;
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
import org.hisp.dhis.tracker.export.event.EventOperationParams;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.common.UID;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings( strictness = Strictness.LENIENT ) // common setup
@ExtendWith( MockitoExtension.class )
class EventRequestParamsMapperTest
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

    @InjectMocks
    private EventRequestParamsMapper mapper;

    private Program program;

    private OrganisationUnit ou;

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

        ProgramStage programStage = new ProgramStage();
        programStage.setUid( "PlZSBEN7iZd" );
        when( programStageService.getProgramStage( "PlZSBEN7iZd" ) ).thenReturn( programStage );
        when( aclService.canDataRead( user, programStage ) ).thenReturn( true );

        ou = new OrganisationUnit();
        when( organisationUnitService.getOrganisationUnit( any() ) ).thenReturn( ou );
        when( organisationUnitService.isInUserHierarchy( ou ) ).thenReturn( true );

        TrackedEntity trackedEntity = new TrackedEntity();
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
        throws BadRequestException
    {
        RequestParams requestParams = new RequestParams();

        mapper.map( requestParams );

        verifyNoInteractions( programService );
        verifyNoInteractions( programStageService );
        verifyNoInteractions( organisationUnitService );
        verifyNoInteractions( entityInstanceService );
    }

    @Test
    void testMappingProgram()
        throws BadRequestException
    {
        RequestParams requestParams = new RequestParams();
        requestParams.setProgram( UID.of( PROGRAM_UID ) );

        EventOperationParams params = mapper.map( requestParams );

        assertEquals( program.getUid(), params.getProgramUid() );
    }

    @Test
    void shouldReturnOrgUnitWhenCorrectOrgUnitMapped()
        throws BadRequestException
    {
        RequestParams requestParams = new RequestParams();
        requestParams.setOrgUnit( UID.of( ou.getUid() ) );

        EventOperationParams params = mapper.map( requestParams );

        assertEquals( ou.getUid(), params.getOrgUnitUid() );
    }

    @Test
    void testMappingTrackedEntity()
        throws BadRequestException
    {
        RequestParams requestParams = new RequestParams();
        requestParams.setTrackedEntity( UID.of( "qnR1RK4cTIZ" ) );

        EventOperationParams params = mapper.map( requestParams );

        assertEquals( "qnR1RK4cTIZ", params.getTrackedEntityUid() );
    }

    @Test
    void testMappingOccurredAfterBefore()
        throws BadRequestException
    {
        RequestParams requestParams = new RequestParams();

        Date occurredAfter = parseDate( "2020-01-01" );
        requestParams.setOccurredAfter( occurredAfter );
        Date occurredBefore = parseDate( "2020-09-12" );
        requestParams.setOccurredBefore( occurredBefore );

        EventOperationParams params = mapper.map( requestParams );

        assertEquals( occurredAfter, params.getStartDate() );
        assertEquals( occurredBefore, params.getEndDate() );
    }

    @Test
    void testMappingScheduledAfterBefore()
        throws BadRequestException
    {
        RequestParams requestParams = new RequestParams();

        Date scheduledAfter = parseDate( "2021-01-01" );
        requestParams.setScheduledAfter( scheduledAfter );
        Date scheduledBefore = parseDate( "2021-09-12" );
        requestParams.setScheduledBefore( scheduledBefore );

        EventOperationParams params = mapper.map( requestParams );

        assertEquals( scheduledAfter, params.getScheduledAfter() );
        assertEquals( scheduledBefore, params.getScheduledBefore() );
    }

    @Test
    void shouldMapAfterAndBeforeDatesWhenSupplied()
        throws BadRequestException
    {
        RequestParams requestParams = new RequestParams();

        Date updatedAfter = parseDate( "2022-01-01" );
        requestParams.setUpdatedAfter( updatedAfter );
        Date updatedBefore = parseDate( "2022-09-12" );
        requestParams.setUpdatedBefore( updatedBefore );

        EventOperationParams params = mapper.map( requestParams );

        assertEquals( updatedAfter, params.getUpdatedAfter() );
        assertEquals( updatedBefore, params.getUpdatedBefore() );
    }

    @Test
    void shouldMapUpdatedWithinDateWhenSupplied()
        throws BadRequestException
    {
        RequestParams requestParams = new RequestParams();
        String updatedWithin = "6m";
        requestParams.setUpdatedWithin( updatedWithin );

        EventOperationParams params = mapper.map( requestParams );

        assertEquals( updatedWithin, params.getUpdatedWithin() );
    }

    @Test
    void shouldFailWithBadRequestExceptionWhenTryingToMapAllUpdateDatesTogether()
    {
        RequestParams requestParams = new RequestParams();

        Date updatedAfter = parseDate( "2022-01-01" );
        requestParams.setUpdatedAfter( updatedAfter );
        Date updatedBefore = parseDate( "2022-09-12" );
        requestParams.setUpdatedBefore( updatedBefore );
        String updatedWithin = "P6M";
        requestParams.setUpdatedWithin( updatedWithin );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( requestParams ) );

        assertEquals( "Last updated from and/or to and last updated duration cannot be specified simultaneously",
            exception.getMessage() );
    }

    @Test
    void testMappingEnrollmentEnrolledAtDates()
        throws BadRequestException
    {
        RequestParams requestParams = new RequestParams();

        Date enrolledBefore = parseDate( "2022-01-01" );
        requestParams.setEnrollmentEnrolledBefore( enrolledBefore );
        Date enrolledAfter = parseDate( "2022-02-01" );
        requestParams.setEnrollmentEnrolledAfter( enrolledAfter );

        EventOperationParams params = mapper.map( requestParams );

        assertEquals( enrolledBefore, params.getEnrollmentEnrolledBefore() );
        assertEquals( enrolledAfter, params.getEnrollmentEnrolledAfter() );
    }

    @Test
    void testMappingEnrollmentOccurredAtDates()
        throws BadRequestException
    {
        RequestParams requestParams = new RequestParams();

        Date enrolledBefore = parseDate( "2022-01-01" );
        requestParams.setEnrollmentOccurredBefore( enrolledBefore );
        Date enrolledAfter = parseDate( "2022-02-01" );
        requestParams.setEnrollmentOccurredAfter( enrolledAfter );

        EventOperationParams params = mapper.map( requestParams );

        assertEquals( enrolledBefore, params.getEnrollmentOccurredBefore() );
        assertEquals( enrolledAfter, params.getEnrollmentOccurredAfter() );
    }

    @Test
    void testMappingEnrollments()
        throws BadRequestException
    {
        RequestParams requestParams = new RequestParams();

        requestParams.setEnrollments( Set.of( UID.of( "NQnuK2kLm6e" ) ) );

        EventOperationParams params = mapper.map( requestParams );

        assertEquals( Set.of( "NQnuK2kLm6e" ), params.getEnrollments() );
    }

    @Test
    void testMappingEvent()
        throws BadRequestException
    {
        RequestParams requestParams = new RequestParams();
        requestParams.setEvent( "XKrcfuM4Hcw;M4pNmLabtXl" );

        EventOperationParams params = mapper.map( requestParams );

        assertEquals( Set.of( "XKrcfuM4Hcw", "M4pNmLabtXl" ), params.getEvents() );
    }

    @Test
    void testMappingEvents()
        throws BadRequestException
    {
        RequestParams requestParams = new RequestParams();
        requestParams.setEvents( Set.of( UID.of( "XKrcfuM4Hcw" ), UID.of( "M4pNmLabtXl" ) ) );

        EventOperationParams params = mapper.map( requestParams );

        assertEquals( Set.of( "XKrcfuM4Hcw", "M4pNmLabtXl" ), params.getEvents() );
    }

    @Test
    void testMappingEventIsNull()
        throws BadRequestException
    {
        RequestParams requestParams = new RequestParams();

        EventOperationParams params = mapper.map( requestParams );

        assertIsEmpty( params.getEvents() );
    }

    @Test
    void testMappingAssignedUser()
        throws BadRequestException
    {
        RequestParams requestParams = new RequestParams();
        requestParams.setAssignedUser( "IsdLBTOBzMi;l5ab8q5skbB" );
        requestParams.setAssignedUserMode( AssignedUserSelectionMode.PROVIDED );

        EventOperationParams params = mapper.map( requestParams );

        assertContainsOnly( Set.of( "IsdLBTOBzMi", "l5ab8q5skbB" ),
            params.getAssignedUsers() );
        assertEquals( AssignedUserSelectionMode.PROVIDED, params.getAssignedUserMode() );
    }

    @Test
    void testMappingAssignedUsers()
        throws BadRequestException
    {
        RequestParams requestParams = new RequestParams();
        requestParams.setAssignedUsers( Set.of( UID.of( "IsdLBTOBzMi" ), UID.of( "l5ab8q5skbB" ) ) );
        requestParams.setAssignedUserMode( AssignedUserSelectionMode.PROVIDED );

        EventOperationParams params = mapper.map( requestParams );

        assertContainsOnly( Set.of( "IsdLBTOBzMi", "l5ab8q5skbB" ),
            params.getAssignedUsers() );
        assertEquals( AssignedUserSelectionMode.PROVIDED, params.getAssignedUserMode() );
    }

    @Test
    void testMutualExclusionOfEventsAndFilter()
    {
        RequestParams requestParams = new RequestParams();
        requestParams.setFilter( Set.of( "qrur9Dvnyt5:ge:1:le:2" ) );
        requestParams.setEvent( "XKrcfuM4Hcw;M4pNmLabtXl" );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( requestParams ) );
        assertEquals( "Event UIDs and filters can not be specified at the same time", exception.getMessage() );
    }

    @Test
    void shouldMapOrderParameterToOrderCriteriaWhenFieldsAreSortable()
        throws BadRequestException
    {
        RequestParams requestParams = new RequestParams();
        requestParams.setOrder( OrderCriteria.fromOrderString( "createdAt:asc,programStage:desc,scheduledAt:asc" ) );

        EventOperationParams params = mapper.map( requestParams );

        assertContainsOnly( List.of(
            new OrderParam( "createdAt", SortDirection.ASC ),
            new OrderParam( "programStage", SortDirection.DESC ),
            new OrderParam( "scheduledAt", SortDirection.ASC ) ), params.getOrders() );
    }

    @Test
    void shouldThrowWhenOrderParameterContainsUnsupportedField()
    {
        RequestParams requestParams = new RequestParams();
        requestParams.setOrder(
            OrderCriteria.fromOrderString( "unsupportedProperty1:asc,enrolledAt:asc,unsupportedProperty2:desc" ) );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( requestParams ) );
        assertAll(
            () -> assertStartsWith( "Order by property `", exception.getMessage() ),
            // order of properties might not always be the same; therefore using
            // contains
            () -> assertContains( "unsupportedProperty1", exception.getMessage() ),
            () -> assertContains( "unsupportedProperty2", exception.getMessage() ) );
    }
}
