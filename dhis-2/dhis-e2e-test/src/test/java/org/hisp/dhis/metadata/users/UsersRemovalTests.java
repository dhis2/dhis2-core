package org.hisp.dhis.metadata.users;



import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.metadata.OptionActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.ResponseValidationHelper;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class UsersRemovalTests
    extends ApiTest
{
    private UserActions userActions;

    private OptionActions optionActions;

    private LoginActions loginActions;

    private String userId;

    private String userName;

    private String password = "!XPTOqwerty1";

    @BeforeEach
    public void beforeEach()
    {
        userActions = new UserActions();
        optionActions = new OptionActions();
        loginActions = new LoginActions();

        userName = DataGenerator.randomString();

        loginActions.loginAsSuperUser();

        userId = userActions.addUser( "johnny", "bravo", userName, password );
    }

    @Test
    public void shouldRemoveWhenUserWasLoggedIn()
    {
        loginActions.loginAsUser( userName, password );
        loginActions.loginAsSuperUser();

        ApiResponse response = userActions.delete( userId );

        ResponseValidationHelper.validateObjectRemoval( response, "User was not removed" );
    }

    @Test
    @Disabled
    //jira issue 5573
    public void shouldRemoveWhenUserWasGrantedAccessToMetadata()
    {
        // arrange
        String id = optionActions.createOptionSet();

        optionActions.grantUserAccessToOptionSet( id, userId );

        // act
        ApiResponse response = userActions.delete( userId );

        // assert
        ResponseValidationHelper.validateObjectRemoval( response, "User was not removed" );
    }
}
