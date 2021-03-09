package org.hisp.dhis.actions;



import static io.restassured.RestAssured.oauth2;
import static org.hamcrest.CoreMatchers.equalTo;

import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.config.TestConfiguration;

import io.restassured.RestAssured;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class LoginActions
{
    /**
     * Makes sure user with given name is logged in.
     * Will throw assertion exception if authentication is not successful.
     *
     * @param username
     * @param password
     */
    public void loginAsUser( final String username, final String password )
    {
        ApiResponse loggedInUser = getLoggedInUserInfo();

        if ( loggedInUser.getContentType().contains( "json" ) &&
            loggedInUser.extract( "userCredentials.username" ) != null &&
            loggedInUser.extract( "userCredentials.username" ).equals( username ) )
        {
            return;
        }

        addAuthenticationHeader( username, password );

        getLoggedInUserInfo().validate().statusCode( 200 ).body( "userCredentials.username", equalTo( username ) );
    }

    /**
     * Makes sure user configured in env variables is logged in.
     * Will throw assertion exception if authentication is not successful.
     */
    public void loginAsSuperUser()
    {
        loginAsUser( TestConfiguration.get().superUserUsername(), TestConfiguration.get().superUserPassword() );
    }

    /**
     * Makes sure user admin:district is logged in.
     * Will throw assertion exception if authentication is not successful.
     */
    public void loginAsDefaultUser()
    {
        loginAsUser( TestConfiguration.get().defaultUserUsername(), TestConfiguration.get().defaultUSerPassword() );
    }

    public ApiResponse getLoggedInUserInfo()
    {
        return new RestApiActions( "/me" ).get();
    }

    public String getLoggedInUserId()
    {
        return getLoggedInUserInfo().extractString( "id" );
    }

    /**
     * Adds authentication header that is used in all consecutive requests
     *
     * @param username
     * @param password
     */
    public void addAuthenticationHeader( final String username, final String password )
    {
        RestAssured.authentication = RestAssured.preemptive().basic( username, password );
    }

    /**
     * Removes authentication header
     */
    public void removeAuthenticationHeader()
    {
        RestAssured.authentication = RestAssured.DEFAULT_AUTH;
    }

    /**
     * Logs in with oAuth2 token
     * @param token
     */
    public void loginWithToken( String token )
    {
        RestAssured.authentication = oauth2( token );
    }

    public void loginAsAdmin()
    {
        loginAsUser( TestConfiguration.get().adminUserUsername(), TestConfiguration.get().adminUserPassword() );
    }
}
