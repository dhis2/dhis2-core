package org.hisp.dhis.actions.metadata;



import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.utils.DataGenerator;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class OptionActions
{
    public RestApiActions optionSetActions;

    public RestApiActions optionActions;

    public OptionActions()
    {
        optionActions = new RestApiActions( "/options" );
        optionSetActions = new RestApiActions( "/optionSets" );
    }

    /**
     * Creates an option. If optionSetId provided, associates option with an option set;
     *
     * @param optionSetId UID of option set. If null - option set won´t be associated with an option.
     * @return
     */
    public String createOption( String optionName, String optionCode, String optionSetId )
    {
        JsonObject option = new JsonObject();

        option.addProperty( "name", optionName );
        option.addProperty( "code", optionCode );

        if ( optionSetId != null )
        {
            JsonObject optionSet = new JsonObject();
            optionSet.addProperty( "id", optionSetId );

            option.add( "optionSet", optionSet );
        }

        return optionActions.create( option );
    }

    /**
     * Creates an option set. If optionIds are provided, links options with option set.
     *
     * @param optionSetName
     * @param valueType
     * @param optionIds     UIDS of options
     * @return
     */
    public String createOptionSet( String optionSetName, String valueType, String... optionIds )
    {
        JsonObject optionSet = new JsonObject();

        optionSet.addProperty( "name", optionSetName );
        optionSet.addProperty( "valueType", valueType );

        if ( optionIds != null )
        {
            JsonArray options = new JsonArray();
            for ( String optionID : optionIds )
            {
                JsonObject option = new JsonObject();
                option.addProperty( "id", optionID );

                options.add( option );
            }
            optionSet.add( "options", options );

        }

        return optionSetActions.create( optionSet );
    }

    public String createOptionSet( String... optionIDs )
    {
        String random = DataGenerator.randomString();

        return createOptionSet( "Option Set auto " + random, "TEXT", optionIDs );
    }

    /**
     * Grants user read and write access to option set.
     * Validates that request was successful.
     *
     * @param optionSetId
     * @param userId
     */
    public void grantUserAccessToOptionSet( String optionSetId, String userId )
    {
        JsonObject jsonBody = optionSetActions.get( optionSetId ).getBody();

        JsonArray userAccesses = new JsonArray();
        JsonObject userAccess = new JsonObject();
        userAccesses.add( userAccess );
        userAccess.addProperty( "access", "rw------" );
        userAccess.addProperty( "id", userId );
        userAccess.addProperty( "userUid", userId );

        jsonBody.add( "userAccesses", userAccesses );

        optionSetActions.update( optionSetId, jsonBody )
            .validate()
            .statusCode( 200 );
    }
}
