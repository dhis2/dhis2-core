



package org.hisp.dhis.actions.metadata;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hamcrest.Matchers;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.utils.DataGenerator;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class ProgramActions
    extends RestApiActions
{
    public RestApiActions programStageActions;

    public ProgramActions()
    {
        super( "/programs" );
        this.programStageActions = new RestApiActions( "/programStages" );
    }

    public ApiResponse createProgram( String programType )
    {
        JsonObject object = getDummy( programType );

        return post( object );
    }

    public ApiResponse createTrackerProgram( String... orgUnitIds )
    {
        return createProgram( "WITH_REGISTRATION", orgUnitIds );

    }

    public ApiResponse createEventProgram( String... orgUnitsIds )
    {
        return createProgram( "WITHOUT_REGISTRATION", orgUnitsIds );
    }

    public ApiResponse createProgram( String programType, String... orgUnitIds )
    {
        JsonObject object = getDummy( programType );
        JsonArray orgUnits = new JsonArray();

        for ( String ouid : orgUnitIds
        )
        {
            JsonObject orgUnit = new JsonObject();
            orgUnit.addProperty( "id", ouid );

            orgUnits.add( orgUnit );
        }

        object.add( "organisationUnits", orgUnits );

        return post( object );
    }

    public ApiResponse addProgramStage( String programId, String programStageId )
    {
        JsonObject body = get( programId ).getBody();
        JsonArray programStages = new JsonArray();
        JsonObject programStage = new JsonObject();

        programStage.addProperty( "id", programStageId );

        programStages.add( programStage );

        body.add( "programStages", programStages );

        return update( programId, body );
    }

    public String createProgramStage( String name )
    {
        JsonObject body = new JsonObject();

        body.addProperty( "name", name );

        ApiResponse response = programStageActions.post( body );
        response.validate().statusCode( Matchers.isOneOf( 201, 200 ) );

        return response.extractUid();
    }

    public JsonObject getDummy()
    {
        String random = DataGenerator.randomString();

        JsonObject object = new JsonObject();
        object.addProperty( "name", "AutoTest program " + random );
        object.addProperty( "shortName", "AutoTest program " + random );

        return object;
    }

    public JsonObject getDummy( String programType )
    {

        JsonObject program = getDummy();
        program.addProperty( "programType", programType );

        return program;
    }

}
