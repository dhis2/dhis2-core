/*
 * Copyright (c) 2004-2018, University of Oslo
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

package org.hisp.dhis;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.response.Response;
import org.hisp.dhis.actions.ApiActions;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class Users_Remove_Tests
    extends ApiTest
{
    private UserActions userActions = new UserActions();
    private ApiActions optionSetActions = new ApiActions( "/optionSets" );
    private LoginActions loginActions = new LoginActions();
    private String userId;
    private String userName;
    private String password = "!XPTOqwerty1";

    @BeforeEach
    public void beforeEach()
    {
        userName = DataGenerator.randomString();
        loginActions.loginAsDefaultUser();
        userId = userActions.addUser( "johnny", "bravo", userName, password );
    }

    @Test
    public void users_remove_userWhoLoggedIn()
    {
        loginActions.loginAsUser( userName, password );

        loginActions.loginAsDefaultUser();

        Response response = userActions.delete( userId );

        response.then()
            .statusCode( 200 );

        assertEquals( response.jsonPath().getString( "response.uid" ), userId );
    }

    @Test
    //jira issue 5573
    public void users_remove_userWhoWasGrantedAccessToMetadata()
    {
        String id = createOptionSet();

        JsonObject jsonObject = optionSetActions.get( id )
            .getBody()
            .as( JsonObject.class, ObjectMapperType.GSON );

        JsonArray userAccesses = new JsonArray();
        JsonObject userAccess = new JsonObject();
        userAccesses.add( userAccess );
        userAccess.addProperty( "access", "rw------" );
        userAccess.addProperty( "id", userId );
        userAccess.addProperty( "userUid", userId );

        jsonObject.add( "userAccesses", userAccesses );

        optionSetActions.update( id, jsonObject );

        Response response = userActions.delete( userId );
        response.prettyPrint();
        assertEquals( response.statusCode(), 200 );
    }

    private String createOptionSet( String... optionIds )
    {
        JsonObject optionSet = new JsonObject();

        String random = DataGenerator.randomString();
        optionSet.addProperty( "name", "Option Set auto " + random );
        optionSet.addProperty( "valueType", "TEXT" );

        if ( optionIds != null )
        {
            JsonArray options = new JsonArray();
            for ( String optionID : optionIds
            )
            {
                JsonObject option = new JsonObject();
                option.addProperty( "id", optionID );

                options.add( option );
            }
            optionSet.add( "options", options );

        }
        return optionSetActions.create( optionSet );
    }
}
