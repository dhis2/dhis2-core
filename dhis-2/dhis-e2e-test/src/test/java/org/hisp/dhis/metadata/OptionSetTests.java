package org.hisp.dhis.metadata;



import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.metadata.OptionActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.ResponseValidationHelper;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.*;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class OptionSetTests
    extends ApiTest
{
    private OptionActions optionActions;

    private LoginActions loginActions;

    private String createdOptionSet;

    @BeforeAll
    public void beforeAll()
    {

        optionActions = new OptionActions();

        loginActions = new LoginActions();

        loginActions.loginAsSuperUser();
    }

    @BeforeEach
    public void beforeEach()
    {
        createdOptionSet = createOptionSet();
    }

    @Test
    public void shouldNotBeRemovedWithAssociatedData()
    {
        // arrange
        createOption( createdOptionSet );

        // act
        ApiResponse response = optionActions.optionSetActions.delete( createdOptionSet );

        // assert
        ResponseValidationHelper.validateObjectRemoval( response, "Option set was not deleted" );

        optionActions.optionSetActions.get( createdOptionSet )
            .validate()
            .statusCode( 404 );
    }

    @Test
    public void shouldBeAbleToReferenceWithOption()
    {
        // arrange
        String optionId = createOption( createdOptionSet );

        // act
        ApiResponse response = optionActions.optionSetActions.get( createdOptionSet );

        // assert
        response.validate()
            .statusCode( 200 )
            .body( "options.id[0]", equalTo( optionId ) );
    }

    @Test
    public void shouldAddOptions()
    {
        String option1 = createOption( createdOptionSet );
        String option2 = createOption( createdOptionSet );

        ApiResponse response = optionActions.optionSetActions.get( createdOptionSet );

        response.validate().statusCode( 200 )
            .body( "options", not( emptyArray() ) )
            .body( "options.id", not( emptyArray() ) )
            .rootPath( "options" )
            .body( "id[0]", equalTo( option1 ) )
            .body( "id[1]", equalTo( option2 ) );
    }

    @Test
    public void shouldRemoveOptions()
    {
        // arrange
        createOption( createdOptionSet );
        ApiResponse response = optionActions.optionSetActions.get( createdOptionSet );

        JsonObject object = response.getBody();
        object.remove( "options" );

        // act
        response = optionActions.optionSetActions.update( createdOptionSet, object );
        response.validate()
            .statusCode( 200 );

        // assert
        response = optionActions.optionSetActions.get( createdOptionSet );
        response.validate()
            .statusCode( 200 )
            .body( "options", hasSize( 0 ) );
    }

    @Test
    public void shouldRemoveOptionFromCollection()
    {
        // arrange
        String optionId = createOption( createdOptionSet );

        optionActions.optionSetActions.get( createdOptionSet + "/options/" + optionId )
            .validate()
            .statusCode( 200 );

        // act
        optionActions.optionSetActions.delete( createdOptionSet + "/options/" + optionId )
            .validate()
            .statusCode( 204 );

        // assert
        optionActions.optionSetActions.get( createdOptionSet )
            .validate()
            .statusCode( 200 );

        optionActions.optionSetActions.get( createdOptionSet + "/options/" + optionId )
            .validate()
            .statusCode( 404 );
    }

    private String createOptionSet( String... optionIds )
    {
        String random = DataGenerator.randomString();
        return optionActions.createOptionSet( "AutoTest option set " + random, "TEXT", optionIds );
    }

    /**
     * Creates an option associated with option set
     *
     * @param optionSetId UID of option set
     * @return
     */
    private String createOption( String optionSetId )
    {
        return optionActions
            .createOption( "Option name auto" + DataGenerator.randomString(), "Option code auto" + DataGenerator.randomString(),
                optionSetId );
    }
}
