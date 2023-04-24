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
package org.hisp.dhis.webapi.controller.event;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.utils.Assertions.assertNotEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ForbiddenException;

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstanceQueryParams;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.EnrollmentCriteriaMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith( MockitoJUnitRunner.class )
public class EnrollmentCriteriaMapperTest
{

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

    @Mock
    private TrackerAccessManager trackerAccessManager;

    @InjectMocks
    private EnrollmentCriteriaMapper mapper;

    private static final String ORG_UNIT1 = "orgUnit1";

    private static final String PROGRAM_UID = "programUid";

    private static final String ENTITY_TYPE = "entityType";

    private static final String ENTITY_INSTANCE = "entityInstance";

    private Program program;

    private OrganisationUnit organisationUnit;

    private User user;

    private TrackedEntityType trackedEntityType;

    private TrackedEntityInstance trackedEntityInstance;

    @Before
    public void setUp()
    {
        program = new Program();
        program.setUid( PROGRAM_UID );

        organisationUnit = new OrganisationUnit();
        organisationUnit.setUid( ORG_UNIT1 );

        user = new User();
        when( currentUserService.getCurrentUser() ).thenReturn( user );

        trackedEntityType = new TrackedEntityType();
        trackedEntityType.setUid( ENTITY_TYPE );

        trackedEntityInstance = new TrackedEntityInstance();
        trackedEntityInstance.setUid( ENTITY_INSTANCE );
    }

    @Test
    public void shouldMapCorrectlyWhenOrgUnitExistsAndUserInScope()
        throws IllegalQueryException,
        ForbiddenException
    {
        Set<String> orgUnits = new HashSet<>( Collections.singletonList( ORG_UNIT1 ) );
        when( programService.getProgram( PROGRAM_UID ) ).thenReturn( program );
        when( organisationUnitService.getOrganisationUnit( ORG_UNIT1 ) ).thenReturn( organisationUnit );
        when( trackerAccessManager.canAccess( user, program, organisationUnit ) ).thenReturn( true );
        when( trackedEntityTypeService.getTrackedEntityType( ENTITY_TYPE ) ).thenReturn( trackedEntityType );
        when( trackedEntityInstanceService.getTrackedEntityInstance( ENTITY_INSTANCE ) )
            .thenReturn( trackedEntityInstance );

        ProgramInstanceQueryParams params = mapper.getFromUrl( orgUnits, DESCENDANTS, null, "lastUpdated", PROGRAM_UID,
            ProgramStatus.ACTIVE, null, null, ENTITY_TYPE, ENTITY_INSTANCE, false, 1, 1, false, false, false, null );

        assertNotEmpty( params.getOrganisationUnits() );
        assertEquals( ORG_UNIT1, params.getOrganisationUnits().iterator().next().getUid() );
        assertEquals( PROGRAM_UID, params.getProgram().getUid() );
    }

    @Test
    public void shouldThrowExceptionWhenOrgUnitDoesNotExist()
    {
        Set<String> orgUnits = new HashSet<>( Collections.singletonList( ORG_UNIT1 ) );
        when( programService.getProgram( PROGRAM_UID ) ).thenReturn( program );

        Exception exception = assertThrows( IllegalQueryException.class,
            () -> mapper.getFromUrl( orgUnits, DESCENDANTS, null, "lastUpdated", PROGRAM_UID, ProgramStatus.ACTIVE,
                null, null, "trackedEntityType", "trackedEntityInstance", false, 1, 1, false, false, false, null ) );
        assertEquals( "Organisation unit does not exist: " + ORG_UNIT1, exception.getMessage() );
    }

    @Test
    public void shouldThrowExceptionWhenOrgUnitNotInScope()
    {
        Set<String> orgUnits = new HashSet<>( Collections.singletonList( ORG_UNIT1 ) );
        when( programService.getProgram( PROGRAM_UID ) ).thenReturn( program );
        when( organisationUnitService.getOrganisationUnit( ORG_UNIT1 ) ).thenReturn( organisationUnit );
        when( trackerAccessManager.canAccess( user, program, organisationUnit ) ).thenReturn( false );

        Exception exception = assertThrows( ForbiddenException.class,
            () -> mapper.getFromUrl( orgUnits, DESCENDANTS, null, "lastUpdated", PROGRAM_UID, ProgramStatus.ACTIVE,
                null, null, "trackedEntityType", "trackedEntityInstance", false, 1, 1, false, false, false, null ) );
        assertEquals( "User does not have access to organisation unit: " + ORG_UNIT1, exception.getMessage() );
    }
}
