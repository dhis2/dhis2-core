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
package org.hisp.dhis.webapi.controller.tracker.export.enrollment;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstanceQueryParams;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
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
class EnrollmentCriteriaMapperTest
{

    private static final String ORG_UNIT_1_UID = "lW0T2U7gZUi";

    private static final String ORG_UNIT_2_UID = "TK4KA0IIWqa";

    private static final String PROGRAM_UID = "XhBYIraw7sv";

    private static final String TRACKED_ENTITY_TYPE_UID = "Dp8baZYrLtr";

    private static final String TRACKED_ENTITY_UID = "DGbr8GHG4li";

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private ProgramService programService;

    @Mock
    private TrackedEntityTypeService trackedEntityTypeService;

    @Mock
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @InjectMocks
    private EnrollmentCriteriaMapper mapper;

    private OrganisationUnit orgUnit1;

    private OrganisationUnit orgUnit2;

    private User user;

    private Program program;

    private TrackedEntityType trackedEntityType;

    @BeforeEach
    void setUp()
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

        trackedEntityType = new TrackedEntityType();
        trackedEntityType.setUid( TRACKED_ENTITY_TYPE_UID );
        when( trackedEntityTypeService.getTrackedEntityType( TRACKED_ENTITY_TYPE_UID ) )
            .thenReturn( trackedEntityType );

        TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
        trackedEntityInstance.setUid( TRACKED_ENTITY_UID );
        when( trackedEntityInstanceService.getTrackedEntityInstance( TRACKED_ENTITY_UID ) )
            .thenReturn( trackedEntityInstance );
    }

    @Test
    void testMappingDoesNotFetchOptionalEmptyQueryParametersFromDB()
        throws BadRequestException,
        ForbiddenException
    {
        EnrollmentCriteria criteria = new EnrollmentCriteria();

        mapper.map( criteria );

        verifyNoInteractions( programService );
        verifyNoInteractions( organisationUnitService );
        verifyNoInteractions( trackedEntityTypeService );
        verifyNoInteractions( trackedEntityInstanceService );
    }

    @Test
    void testMappingOrgUnit()
        throws BadRequestException,
        ForbiddenException
    {
        EnrollmentCriteria criteria = new EnrollmentCriteria();
        criteria.setOrgUnit( ORG_UNIT_1_UID + ";" + ORG_UNIT_2_UID );

        ProgramInstanceQueryParams params = mapper.map( criteria );

        assertContainsOnly( Set.of( orgUnit1, orgUnit2 ), params.getOrganisationUnits() );
    }

    @Test
    void testMappingOrgUnitNotFound()
    {
        EnrollmentCriteria criteria = new EnrollmentCriteria();
        criteria.setOrgUnit( "unknown;" + ORG_UNIT_2_UID );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Organisation unit does not exist: unknown", exception.getMessage() );
    }

    @Test
    void testMappingOrgUnitNotPartOfSearchScope()
    {
        EnrollmentCriteria criteria = new EnrollmentCriteria();
        criteria.setOrgUnit( ORG_UNIT_1_UID );
        when( organisationUnitService.isInUserHierarchy( ORG_UNIT_1_UID,
            user.getTeiSearchOrganisationUnitsWithFallback() ) ).thenReturn( false );

        Exception exception = assertThrows( ForbiddenException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Organisation unit is not part of the search scope: " + ORG_UNIT_1_UID, exception.getMessage() );
    }

    @Test
    void testMappingProgram()
        throws BadRequestException,
        ForbiddenException
    {
        EnrollmentCriteria criteria = new EnrollmentCriteria();
        criteria.setProgram( PROGRAM_UID );

        ProgramInstanceQueryParams params = mapper.map( criteria );

        assertEquals( program, params.getProgram() );
    }

    @Test
    void testMappingProgramNotFound()
    {
        EnrollmentCriteria criteria = new EnrollmentCriteria();
        criteria.setProgram( "unknown" );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Program is specified but does not exist: unknown", exception.getMessage() );
    }

    @Test
    void testMappingTrackedEntityType()
        throws BadRequestException,
        ForbiddenException
    {
        EnrollmentCriteria criteria = new EnrollmentCriteria();
        criteria.setTrackedEntityType( TRACKED_ENTITY_TYPE_UID );

        ProgramInstanceQueryParams params = mapper.map( criteria );

        assertEquals( trackedEntityType, params.getTrackedEntityType() );
    }

    @Test
    void testMappingTrackedEntityTypeNotFound()
    {
        EnrollmentCriteria criteria = new EnrollmentCriteria();
        criteria.setTrackedEntityType( "unknown" );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Tracked entity type is specified but does not exist: unknown", exception.getMessage() );
    }

    @Test
    void testMappingTrackedEntity()
        throws BadRequestException,
        ForbiddenException
    {
        EnrollmentCriteria criteria = new EnrollmentCriteria();
        criteria.setTrackedEntity( TRACKED_ENTITY_UID );

        ProgramInstanceQueryParams params = mapper.map( criteria );

        assertEquals( TRACKED_ENTITY_UID, params.getTrackedEntityInstanceUid() );
    }

    @Test
    void testMappingTrackedEntityNotFound()
    {
        EnrollmentCriteria criteria = new EnrollmentCriteria();
        criteria.setTrackedEntity( "unknown" );

        Exception exception = assertThrows( BadRequestException.class,
            () -> mapper.map( criteria ) );
        assertEquals( "Tracked entity instance is specified but does not exist: unknown", exception.getMessage() );
    }

    @Test
    void testMappingOrderParams()
        throws BadRequestException,
        ForbiddenException
    {
        EnrollmentCriteria criteria = new EnrollmentCriteria();
        OrderCriteria order1 = OrderCriteria.of( "field1", SortDirection.ASC );
        OrderCriteria order2 = OrderCriteria.of( "field2", SortDirection.DESC );
        criteria.setOrder( List.of( order1, order2 ) );

        ProgramInstanceQueryParams params = mapper.map( criteria );

        assertEquals( List.of(
            new OrderParam( "field1", SortDirection.ASC ),
            new OrderParam( "field2", SortDirection.DESC ) ), params.getOrder() );
    }

    @Test
    void testMappingOrderParamsNoOrder()
        throws BadRequestException,
        ForbiddenException
    {
        EnrollmentCriteria criteria = new EnrollmentCriteria();

        ProgramInstanceQueryParams params = mapper.map( criteria );

        assertIsEmpty( params.getOrder() );
    }
}
