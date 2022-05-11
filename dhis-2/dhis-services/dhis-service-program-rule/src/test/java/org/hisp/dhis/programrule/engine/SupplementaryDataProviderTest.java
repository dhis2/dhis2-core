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
package org.hisp.dhis.programrule.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith( MockitoExtension.class )
class SupplementaryDataProviderTest extends DhisConvenienceTest
{

    private static final String ORG_UNIT_GROUP_UID = "OrgUnitGroupId";

    private static final String NOT_NEEDED_ORG_UNIT_GROUP_UID = "NotNeededOrgUnitGroupId";

    @Mock
    private OrganisationUnitGroupService organisationUnitGroupService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private SupplementaryDataProvider providerToTest;

    private OrganisationUnit orgUnitA;

    private OrganisationUnit orgUnitB;

    @BeforeEach
    void setUp()
    {
        User user = makeUser( "A" );
        user.setUserRoles( getUserRoles() );
        when( currentUserService.getCurrentUser() ).thenReturn( user );
        orgUnitA = createOrganisationUnit( 'A' );
        orgUnitB = createOrganisationUnit( 'B' );
        OrganisationUnitGroup orgUnitGroup = createOrganisationUnitGroup( 'A' );
        orgUnitGroup.setUid( ORG_UNIT_GROUP_UID );
        orgUnitGroup.setMembers( Sets.newHashSet( orgUnitA ) );
        OrganisationUnitGroup notNeededOrgUnitGroup = createOrganisationUnitGroup( 'B' );
        notNeededOrgUnitGroup.setUid( NOT_NEEDED_ORG_UNIT_GROUP_UID );
        notNeededOrgUnitGroup.setMembers( Sets.newHashSet( orgUnitB ) );
        when( organisationUnitGroupService.getOrganisationUnitGroup( ORG_UNIT_GROUP_UID ) ).thenReturn( orgUnitGroup );
    }

    @Test
    void getSupplementaryData()
    {
        Map<String, List<String>> supplementaryData = providerToTest.getSupplementaryData( getProgramRules() );
        assertFalse( supplementaryData.isEmpty() );
        assertEquals( getUserRoleUids(), supplementaryData.get( "USER" ) );
        assertFalse( supplementaryData.get( ORG_UNIT_GROUP_UID ).isEmpty() );
        assertEquals( orgUnitA.getUid(), supplementaryData.get( ORG_UNIT_GROUP_UID ).get( 0 ) );
        assertNull( supplementaryData.get( NOT_NEEDED_ORG_UNIT_GROUP_UID ) );
    }

    private List<ProgramRule> getProgramRules()
    {
        ProgramRule programRule = createProgramRule( 'A', null );
        programRule.setCondition( "d2:inOrgUnitGroup('OrgUnitGroupId')" );
        return Lists.newArrayList( programRule );
    }

    private List<String> getUserRoleUids()
    {
        return getUserRoles().stream().map( UserRole::getUid ).collect( Collectors.toList() );
    }

    private Set<UserRole> getUserRoles()
    {
        UserRole groupA = createUserRole( 'A' );
        UserRole groupB = createUserRole( 'B' );
        return Sets.newHashSet( groupA, groupB );
    }
}
