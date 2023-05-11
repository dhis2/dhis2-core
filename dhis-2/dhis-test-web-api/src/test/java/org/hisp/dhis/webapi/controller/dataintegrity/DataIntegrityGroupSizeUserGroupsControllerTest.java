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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;

import java.util.Set;

import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.web.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the metadata check for category option groups which have fewer than two
 * members.
 * {@see dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/groups/group_size_category_option_groups.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityGroupSizeUserGroupsControllerTest extends AbstractDataIntegrityIntegrationTest
{

    @Autowired
    UserService userService;

    private static final String check = "user_groups_scarce";

    private static final String detailsIdType = "userGroups";

    private User bill;

    @Test
    void testUserGroupsScarce()
    {

        setUpTest();

        String userGroupB = assertStatus( HttpStatus.CREATED,
            POST( "/userGroups", "{'name' : 'TestB', 'code' : 'TestB', 'users' : [{'id' : '" +
                bill.getUid() + "'}], 'managedGroups': [], 'attributeValues' : []}" ) );

        String userGroupC = assertStatus( HttpStatus.CREATED,
            POST( "/userGroups",
                "{'name' : 'TestC', 'code' : 'TestC', 'users' : [], 'managedGroups': [], 'attributeValues' : []}" ) );

        assertHasDataIntegrityIssues( detailsIdType, check, 66,
            Set.of( userGroupB, userGroupC ), Set.of( "TestB", "TestC" ), Set.of( "0", "1" ),
            true );

    }

    @Test
    void testUserGroupsOK()
    {
        setUpTest();

        assertHasNoDataIntegrityIssues( detailsIdType, check, true );

    }

    @Test
    void testUserGroupScarceRuns()
    {

        assertHasNoDataIntegrityIssues( detailsIdType, check, false );

    }

    private static User createUser( String uniquePart )
    {
        User user = new User();
        user.setCode( "Code" + uniquePart );
        user.setFirstName( FIRST_NAME + uniquePart );
        user.setSurname( SURNAME + uniquePart );
        user.setUsername( "username" + uniquePart );
        return user;
    }

    void setUpTest()
    {

        bill = createUser( "Bill" );
        User mary = createUser( "Mary" );

        userService.addUser( bill );
        userService.addUser( mary );
        dbmsManager.clearSession();

        assertStatus( HttpStatus.CREATED,
            POST( "/userGroups", "{'name' : 'TestA', 'code' : 'TestA', 'users' : [{'id' : '" +
                bill.getUid() + "'}, {'id' : '" + mary.getUid()
                + "'}], 'managedGroups': [], 'attributeValues' : []}" ) );
    }
}