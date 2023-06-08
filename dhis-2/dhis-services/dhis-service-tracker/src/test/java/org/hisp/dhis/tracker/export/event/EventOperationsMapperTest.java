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

import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith( MockitoExtension.class )
class EventOperationsMapperTest
{

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

        EventSearchParams searchParams = mapper.map( operationParams );

        assertEquals( combo, searchParams.getCategoryOptionCombo() );
    }
}
