package org.hisp.dhis.metadata.users;



import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class UserTest extends ApiTest
{
    private String username;
    private String password = "Test1212?";

    private UserActions userActions;
    private LoginActions loginActions;
    private RestApiActions meActions;

    @BeforeEach
    public void beforeEach() {
        userActions = new UserActions();
        loginActions = new LoginActions();
        meActions = new RestApiActions( "/me" );

        username = "user-tests-" + DataGenerator.randomString();
        loginActions.loginAsSuperUser();
        userActions.addUser( username, password );
        loginActions.loginAsUser( username, password );
    }

    private Stream<Arguments> provideParams() {
        return Stream.of( new Arguments[] {
            Arguments.of( password, password, "Password must not be one of the previous 24 passwords", "newPassword is the same as old" ),
            Arguments.of( password, "Test1?", "Password must have at least 8, and at most 40 characters", "newPassword is too short" ),
            Arguments.of( password, DataGenerator.randomString(41) + "1?", "Password must have at least 8, and at most 40 characters", "newPassword is too-long" ),
            Arguments.of( password, "", "OldPassword and newPassword must be provided", "newPassword is empty" ),
            Arguments.of( "not-an-old-password", "Test1212???", "OldPassword is incorrect", "oldPassword is incorrect" ),
            Arguments.of( password, "test1212?", "Password must have at least one upper case", "newPassword doesn't contain uppercase" ),
            Arguments.of( password, "Testtest1212", "Password must have at least one special character", "newPassword doesn't contain a special character" ),
            Arguments.of( password, "Testtest?", "Password must have at least one digit", "newPassword doesn't contain a digit" )

        } );
    }


    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideParams")
    public void shouldNotBeAbleToChangePasswordWhenValidationErrors(String oldPassword, String newPassword, String message, String description) {
        JsonObject payload = getPayload( oldPassword, newPassword );

        ApiResponse response = meActions.update( "/changePassword", payload  );

        response.validate().statusCode( 409 )
            .body( "status", equalTo( "ERROR" ) )
            .body( "message", equalTo( message ) );
    }

    @Test
    public void shouldBeAbleToChangePassword() {
        String newPassword = "Test1212??";
        JsonObject payload = getPayload( password, newPassword );

        ApiResponse response = meActions.update( "/changePassword", payload  );

        response.validate().statusCode( 202 );

        // should login with new credentials
        loginActions.addAuthenticationHeader( username, newPassword );
        loginActions.getLoggedInUserInfo().validate()
            .statusCode( 200 )
            .body( "userCredentials.username", equalTo( username ) );

        // should not login in with old credentials
        loginActions.addAuthenticationHeader( username, password );
        loginActions.getLoggedInUserInfo().validate().statusCode( 401);
    }

    private JsonObject getPayload(String oldPsw, String newPsw) {
        JsonObject payload = new JsonObject();

        payload.addProperty( "oldPassword", oldPsw );
        payload.addProperty( "newPassword", newPsw );

        return payload;
    }
}
