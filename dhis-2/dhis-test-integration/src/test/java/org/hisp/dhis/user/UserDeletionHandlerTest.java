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
package org.hisp.dhis.user;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserDeletionHandlerTest extends SingleSetupIntegrationTestBase
{
    @Autowired
    private UserService userService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Override
    public void setUpTest()
    {
        super.userService = userService;
    }

    @Test
    void testDeleteOrganisationUnitCleanUpOUScope()
    {
        OrganisationUnit OUa = createOrganisationUnit( "A" );
        organisationUnitService.addOrganisationUnit( OUa );

        User userA = createAndAddUser( "A" );
        userService.addUser( userA );

        userA.setTeiSearchOrganisationUnits( new HashSet<>( Set.of( OUa ) ) );
        userA.setDataViewOrganisationUnits( new HashSet<>( Set.of( OUa ) ) );
        userA.addOrganisationUnit( OUa );

        userService.updateUser( userA );

        User user = userService.getUser( userA.getUid() );
        assertEquals( 1, user.getOrganisationUnits().size() );
        assertEquals( 1, user.getTeiSearchOrganisationUnits().size() );
        assertEquals( 1, user.getDataViewOrganisationUnits().size() );

        organisationUnitService.deleteOrganisationUnit( OUa );

        user = userService.getUser( userA.getUid() );
        assertEquals( 0, user.getOrganisationUnits().size() );
        assertEquals( 0, user.getTeiSearchOrganisationUnits().size() );
        assertEquals( 0, user.getDataViewOrganisationUnits().size() );
    }
}