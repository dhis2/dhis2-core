package org.hisp.dhis.metadata.users;



import static org.hamcrest.CoreMatchers.is;

import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class UserDisableTest
    extends ApiTest
{
    private UserActions userActions;

    private LoginActions loginActions;

    private String userId;

    private String userName;

    private String password = "!XPTOqwerty1";

    @BeforeEach
    public void beforeEach()
    {
        userActions = new UserActions();
        loginActions = new LoginActions();

        userName = DataGenerator.randomString();

        loginActions.loginAsSuperUser();

        userId = userActions.addUser( "johnny", "bravo", userName, password );
    }

    @Test
    public void shouldDisableUser()
    {
        loginActions.loginAsUser( userName, password );
        loginActions.loginAsSuperUser();

        ApiResponse preChangeResponse = userActions.get( userId );
        preChangeResponse.validate().statusCode( 200 ).body( "userCredentials.disabled", is( false ) );

        ApiResponse response = userActions.post( userId + "/disabled", new Object(), null );
        response.validate().statusCode( 204 );

        ApiResponse getResponse = userActions.get( userId );
        getResponse.validate().statusCode( 200 ).body( "userCredentials.disabled", is( true ) );

        loginActions.addAuthenticationHeader( userName, password );
        loginActions.getLoggedInUserInfo().validate().statusCode( 401 );
    }

    @Test
    public void shouldEnableUser()
    {
        loginActions.loginAsUser( userName, password );
        loginActions.loginAsSuperUser();

        ApiResponse preChangeResponse = userActions.get( userId );
        preChangeResponse.validate().statusCode( 200 ).body( "userCredentials.disabled", is( false ) );

        ApiResponse response = userActions.post( userId + "/disabled", new Object(), null );
        response.validate().statusCode( 204 );

        ApiResponse getResponse = userActions.get( userId );
        getResponse.validate().statusCode( 200 ).body( "userCredentials.disabled", is( true ) );

        loginActions.addAuthenticationHeader( userName, password );
        loginActions.getLoggedInUserInfo().validate().statusCode( 401 );

        loginActions.loginAsSuperUser();

        ApiResponse enableResponse = userActions.post( userId + "/enabled", new Object(), null );
        enableResponse.validate().statusCode( 204 );

        ApiResponse getAfterEnabled = userActions.get( userId );
        getAfterEnabled.validate().statusCode( 200 ).body( "userCredentials.disabled", is( false ) );

        loginActions.addAuthenticationHeader( userName, password );
        loginActions.getLoggedInUserInfo().validate().statusCode( 200 );
    }
}
