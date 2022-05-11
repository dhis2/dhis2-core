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
package org.hisp.dhis;

import static org.hamcrest.Matchers.*;

import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.UaaActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.ResponseValidationHelper;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
@Disabled( "Disabled temporary due to issues with DHIS2-12098" )
public class LoginTests
    extends ApiTest
{
    private LoginActions loginActions;

    private RestApiActions oauth2Clients;

    private UaaActions uaaActions;

    private UserActions userActions;

    private String oauthClientId = "demo" + DataGenerator.randomString();

    private String secret = "1e6db50c-0fee-11e5-98d0-3c15c2c6caf6";

    private String userName = ("LoginTestsUser" + DataGenerator.randomString()).toLowerCase();

    private String password = Constants.USER_PASSWORD;

    @BeforeAll
    public void preconditions()
    {
        oauth2Clients = new RestApiActions( "/oAuth2Clients" );
        uaaActions = new UaaActions();
        loginActions = new LoginActions();
        userActions = new UserActions();

        loginActions.loginAsSuperUser();

        addOAuthClient();

        userActions.addUser( userName, password );
    }

    @Test
    public void shouldBeAbleToLoginWithOAuth2()
    {

        loginActions.addAuthenticationHeader( oauthClientId, secret );

        ApiResponse response = uaaActions.post( "oauth/token", new JsonObject(),
            new QueryParamsBuilder().addAll( "username=" + userName, "password=" + password, "grant_type=password" ) );

        response.validate().statusCode( 200 )
            .body( "access_token", notNullValue() )
            .body( "token_type", notNullValue() )
            .body( "refresh_token", notNullValue() )
            .body( "expires_in", notNullValue() )
            .body( "scope", notNullValue() );

        loginActions.loginWithToken( response.extractString( "access_token" ) );

        loginActions.getLoggedInUserInfo().validate()
            .statusCode( 200 )
            .body( "username", equalTo( userName ) );
    }

    @Test
    public void shouldBeAbleToGetRefreshToken()
    {
        loginActions.addAuthenticationHeader( oauthClientId, secret );

        JsonObject object = new JsonObject();
        object.addProperty( "password", password );
        object.addProperty( "grant_type", "password" );
        object.addProperty( "username", userName );

        ApiResponse passwordGrantTypeResponse = uaaActions.post( "oauth/token", new JsonObject(),
            new QueryParamsBuilder().addAll( "password=" + password, "username=" + userName, "grant_type=password" ) );
        passwordGrantTypeResponse.validate().statusCode( 200 );

        loginActions.addAuthenticationHeader( oauthClientId, secret );
        ApiResponse refreshTokenResponse = uaaActions.post( "oauth/token", new JsonObject(), new QueryParamsBuilder()
            .addAll( "grant_type=refresh_token",
                "refresh_token=" + passwordGrantTypeResponse.extractString( "refresh_token" ) ) );

        refreshTokenResponse.validate().statusCode( 200 )
            .body( "access_token", notNullValue() )
            .body( "token_type", notNullValue() )
            .body( "refresh_token", notNullValue() )
            .body( "expires_in", notNullValue() )
            .body( "scope", notNullValue() )
            .body( "access_token", not( equalTo( passwordGrantTypeResponse.extractString( "access_token" ) ) ) );
    }

    private void addOAuthClient()
    {
        JsonObject client = new JsonObject();
        client.addProperty( "name", "OAuth2 client" + DataGenerator.randomString() );
        client.addProperty( "cid", oauthClientId );
        client.addProperty( "secret", secret );

        JsonArray grantTypes = new JsonArray();

        grantTypes.add( "password" );
        grantTypes.add( "refresh_token" );
        grantTypes.add( "authorization_code" );

        client.add( "grantTypes", grantTypes );

        ApiResponse response = oauth2Clients.post( client );

        ResponseValidationHelper.validateObjectCreation( response );
    }
}
