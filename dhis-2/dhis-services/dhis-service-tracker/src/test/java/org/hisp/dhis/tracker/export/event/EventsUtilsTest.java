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

import static org.hisp.dhis.common.AccessLevel.OPEN;
import static org.hisp.dhis.common.AccessLevel.PROTECTED;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith( MockitoExtension.class )
class EventsUtilsTest
{

    @Mock
    private OrganisationUnitService organisationUnitService;

    private OrganisationUnit orgUnit;

    private final String orgUnitId = "orgUnitId";

    private final List<OrganisationUnit> orgUnitDescendants = List.of( createOrgUnit( "orgUnit1", "uid1" ),
        createOrgUnit( "orgUnit2", "uid2" ), createOrgUnit( "captureScopeOrgUnit", "uid3" ),
        createOrgUnit( "searchScopeOrgUnit", "uid4" ) );

    @BeforeEach
    void setup()
    {
        orgUnit = createOrgUnit( "orgUnit", orgUnitId );
        orgUnit.setChildren( Set.of( createOrgUnit( "captureScopeChild", "captureScopeChildUid" ),
            createOrgUnit( "searchScopeChild", "searchScopeChildUid" ) ) );
    }

    @Test
    void shouldFindAccessibleOrgUnitWhenProgramProtectedAndOuModeDescendants()
    {
        Program program = new Program();
        program.setAccessLevel( PROTECTED );
        OrganisationUnit captureScopeOrgUnit = createOrgUnit( "captureScopeOrgUnit", "uid3" );
        User user = new User();
        user.setOrganisationUnits( Set.of( captureScopeOrgUnit ) );

        when( organisationUnitService.getOrganisationUnitWithChildren( orgUnitId ) ).thenReturn( orgUnitDescendants );

        assertTrue(
            EventUtils.isOrgUnitAccessible( program, user, orgUnit, DESCENDANTS,
                organisationUnitService::getOrganisationUnitWithChildren ),
            "Should find accessible org unit when program protected and ou mode descendants: "
                + captureScopeOrgUnit.getName() );
    }

    @Test
    void shouldNotFindAccessibleOrgUnitWhenProgramProtectedAndOuModeDescendants()
    {
        Program program = new Program();
        program.setAccessLevel( PROTECTED );
        OrganisationUnit captureScopeOrgUnit = createOrgUnit( "made up org unit", "made up uid" );
        User user = new User();
        user.setOrganisationUnits( Set.of( captureScopeOrgUnit ) );

        when( organisationUnitService.getOrganisationUnitWithChildren( orgUnitId ) ).thenReturn( orgUnitDescendants );

        assertFalse(
            EventUtils.isOrgUnitAccessible( program, user, orgUnit, DESCENDANTS,
                organisationUnitService::getOrganisationUnitWithChildren ),
            "Should have not found any accessible organisation unit when program protected and ou mode descendants" );
    }

    @Test
    void shouldFindAccessibleOrgUnitWhenProgramOpenAndOuModeDescendants()
    {
        Program program = new Program();
        program.setAccessLevel( OPEN );
        OrganisationUnit searchScopeOrgUnit = createOrgUnit( "searchScopeOrgUnit", "uid4" );
        User user = new User();
        user.setTeiSearchOrganisationUnits( Set.of( searchScopeOrgUnit ) );

        when( organisationUnitService.getOrganisationUnitWithChildren( orgUnitId ) ).thenReturn( orgUnitDescendants );

        assertTrue(
            EventUtils.isOrgUnitAccessible( program, user, orgUnit, DESCENDANTS,
                organisationUnitService::getOrganisationUnitWithChildren ),
            "Should find accessible org unit when program open and ou mode descendants: "
                + searchScopeOrgUnit.getName() );
    }

    @Test
    void shouldNotFindAccessibleOrgUnitWhenProgramOpenAndOuModeDescendants()
    {
        Program program = new Program();
        program.setAccessLevel( OPEN );
        OrganisationUnit searchScopeOrgUnit = createOrgUnit( "made up org unit", "made up uid" );
        User user = new User();
        user.setTeiSearchOrganisationUnits( Set.of( searchScopeOrgUnit ) );

        when( organisationUnitService.getOrganisationUnitWithChildren( orgUnitId ) ).thenReturn( orgUnitDescendants );

        assertFalse(
            EventUtils.isOrgUnitAccessible( program, user, orgUnit, DESCENDANTS,
                organisationUnitService::getOrganisationUnitWithChildren ),
            "Should have not found any accessible organisation unit when program open and ou mode descendants" );
    }

    @Test
    void shouldFindAccessibleOrgUnitWhenProgramProtectedAndOuModeChildren()
    {
        Program program = new Program();
        program.setAccessLevel( PROTECTED );
        OrganisationUnit captureScopeOrgUnit = createOrgUnit( "captureScopeChild", "captureScopeChildUid" );
        User user = new User();
        user.setOrganisationUnits( Set.of( captureScopeOrgUnit ) );

        assertTrue(
            EventUtils.isOrgUnitAccessible( program, user, orgUnit, CHILDREN,
                organisationUnitService::getOrganisationUnitWithChildren ),
            "Should find accessible org unit when program protected and ou mode children: "
                + captureScopeOrgUnit.getName() );
    }

    @Test
    void shouldNotFindAccessibleOrgUnitWhenProgramProtectedAndOuModeChildren()
    {
        Program program = new Program();
        program.setAccessLevel( PROTECTED );
        OrganisationUnit captureScopeOrgUnit = createOrgUnit( "made up org unit", "made up uid" );
        User user = new User();
        user.setOrganisationUnits( Set.of( captureScopeOrgUnit ) );

        assertFalse(
            EventUtils.isOrgUnitAccessible( program, user, orgUnit, CHILDREN,
                organisationUnitService::getOrganisationUnitWithChildren ),
            "Should not find accessible org unit when program protected and ou mode children" );
    }

    @Test
    void shouldFindAccessibleOrgUnitWhenProgramOpenAndOuModeChildren()
    {
        Program program = new Program();
        program.setAccessLevel( OPEN );
        OrganisationUnit searchScopeOrgUnit = createOrgUnit( "searchScopeChild", "searchScopeChildUid" );
        User user = new User();
        user.setTeiSearchOrganisationUnits( Set.of( searchScopeOrgUnit ) );

        assertTrue(
            EventUtils.isOrgUnitAccessible( program, user, orgUnit, CHILDREN,
                organisationUnitService::getOrganisationUnitWithChildren ),
            "Should find accessible org unit: " + searchScopeOrgUnit.getName() );
    }

    @Test
    void shouldNotFindAccessibleOrgUnitWhenProgramOpenAndOuModeChildren()
    {
        Program program = new Program();
        program.setAccessLevel( OPEN );
        OrganisationUnit searchScopeOrgUnit = createOrgUnit( "made up org unit", "made up uid" );
        User user = new User();
        user.setTeiSearchOrganisationUnits( Set.of( searchScopeOrgUnit ) );

        assertFalse(
            EventUtils.isOrgUnitAccessible( program, user, orgUnit, CHILDREN,
                organisationUnitService::getOrganisationUnitWithChildren ),
            "Should find accessible org unit: " + searchScopeOrgUnit.getName() );
    }

    private OrganisationUnit createOrgUnit( String name, String uid )
    {
        OrganisationUnit orgUnit = new OrganisationUnit( name );
        orgUnit.setUid( uid );
        return orgUnit;
    }
}