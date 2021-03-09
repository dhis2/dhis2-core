package org.hisp.dhis;



import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.UaaActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.ResponseValidationHelper;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class LoginTests
    extends ApiTest
{
    private LoginActions loginActions;

    private RestApiActions oauth2Clients;

    private UaaActions uaaActions;

    private UserActions userActions;

    private String oauthClientId = "demo" + DataGenerator.randomString();

    private String secret = "1e6db50c-0fee-11e5-98d0-3c15c2c6caf6";

    private String userName = "LoginTestsUser" + DataGenerator.randomString();

    private String password = "Test1212?";

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
            .body( "userCredentials.username", equalTo( userName ) );
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
            .addAll( "grant_type=refresh_token", "refresh_token=" + passwordGrantTypeResponse.extractString( "refresh_token" ) ) );

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
