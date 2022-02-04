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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.user.UserLookupController}
 * API.
 *
 * @author Jan Bernitt
 */
class UserLookupControllerTest extends DhisControllerConvenienceTest
{

    private String roleId;

    @BeforeEach
    void setUp()
    {
        User john = switchToNewUser( "John" );
        User paul = switchToNewUser( "Paul" );
        User george = switchToNewUser( "George" );
        User ringo = switchToNewUser( "Ringo" );
        switchToSuperuser();
        roleId = assertStatus( HttpStatus.CREATED, POST( "/userRoles", "{'name':'common'}" ) );
        assertStatus( HttpStatus.NO_CONTENT, POST( "/userRoles/" + roleId + "/users/" + john.getUid() ) );
        assertStatus( HttpStatus.NO_CONTENT, POST( "/userRoles/" + roleId + "/users/" + paul.getUid() ) );
        assertStatus( HttpStatus.NO_CONTENT, POST( "/userRoles/" + roleId + "/users/" + george.getUid() ) );
        assertStatus( HttpStatus.NO_CONTENT, POST( "/userRoles/" + roleId + "/users/" + ringo.getUid() ) );
    }

    /**
     * This test makes sure a user having the same role as users in the system
     * can see those users.
     */
    @Test
    void testLookUpUsers()
    {
        User tester = switchToNewUser( "tester" );
        switchToSuperuser();
        assertStatus( HttpStatus.NO_CONTENT, POST( "/userRoles/" + roleId + "/users/" + tester.getUid() ) );
        switchContextToUser( tester );
        JsonArray matches = GET( "/userLookup?query=John" ).content().getArray( "users" );
        assertEquals( 1, matches.size() );
        JsonUser user = matches.get( 0, JsonUser.class );
        assertEquals( "John", user.getFirstName() );
    }
}
