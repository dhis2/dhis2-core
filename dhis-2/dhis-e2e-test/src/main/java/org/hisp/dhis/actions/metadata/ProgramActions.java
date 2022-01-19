package org.hisp.dhis.actions.metadata;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.utils.DataGenerator;

import java.util.Optional;

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
        JsonObject body = new JsonObjectBuilder( buildProgram( "WITHOUT_REGISTRATION", null, orgUnitsIds ) ).build();
        ApiResponse response = post( body );

        createProgramStage( response.extractUid(), "DEFAULT STAGE" );

        return response;
    }

    public ApiResponse createProgram( String programType, String... orgUnitIds )
    {
        JsonObject object = getDummy( programType, orgUnitIds );

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

    /**
     * Creates a program stage and links it to the program.
     *
     * @param programId
     * @param programStageName
     * @return program stage id
     */
    public String createProgramStage( String programId, String programStageName )
    {
        JsonObjectBuilder builder = new JsonObjectBuilder();
        ApiResponse response = programStageActions
            .post( new JsonObjectBuilder().addProperty( "name", programStageName ).
                addObject( "program" , new JsonObjectBuilder().addProperty( "id", programId ))
                .addProperty( "publicAccess", "rwrw----" ).build() );
        response.validate().statusCode( Matchers.is( Matchers.oneOf( 201, 200 ) ) );

        return response.extractUid();
    }

    public ApiResponse addOrganisationUnits( String programId, String... orgUnitIds )
    {
        JsonObject object = this.get( programId ).getBody();

        JsonArray orgUnits = Optional.ofNullable( object.getAsJsonArray( "organisationUnits" ) ).orElse( new JsonArray() );

        for ( String ouid : orgUnitIds
        )
        {
            JsonObject orgUnit = new JsonObject();
            orgUnit.addProperty( "id", ouid );

            orgUnits.add( orgUnit );
        }

        object.add( "organisationUnits", orgUnits );

        return this.update( programId, object );
    }

    public ApiResponse addDataElement( String programStageId, String dataElementId, boolean isMandatory )
    {
        JsonObject object = programStageActions.get( programStageId, new QueryParamsBuilder().add( "fields=*" ) ).getBody();

        JsonArray programStageDataElements = object.getAsJsonArray( "programStageDataElements" );

        JsonObject programStageDataElement = new JsonObject();
        programStageDataElement.addProperty( "compulsory", String.valueOf( isMandatory ) );

        JsonObject dataElement = new JsonObject();
        dataElement.addProperty( "id", dataElementId );

        programStageDataElement.add( "dataElement", dataElement );
        programStageDataElements.add( programStageDataElement );

        object.add( "programStageDataElements", programStageDataElements );

        return programStageActions.update( programStageId, object );
    }

    public JsonObject getDummy()
    {
        String random = DataGenerator.randomString();

        JsonObject object = JsonObjectBuilder.jsonObject()
            .addProperty( "name", "AutoTest program " + random )
            .addProperty( "shortName", "AutoTest program " + random )
            .build();

        return object;
    }

    public JsonObject getDummy( String programType )
    {
        JsonObject program = getDummy();
        program.addProperty( "programType", programType );

        return program;
    }

    JsonObject getDummy( String programType, String... orgUnitIds )
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

        return object;
    }

    public JsonObject buildProgram()
    {
        String random = DataGenerator.randomString();
        JsonObject object = JsonObjectBuilder.jsonObject()
            .addProperty( "name", "AutoTest program " + random )
            .addProperty( "shortName", "AutoTest program " + random )
            .addUserGroupAccess()
            .addProperty( "publicAccess", "rwrw----" )
            .build();
        return object;
    }
    public JsonObject buildProgram( String programType )
    {
        return new JsonObjectBuilder( buildProgram() )
            .addProperty( "programType", programType )
            .addProperty( "publicAccess", "rwrw----" )
            .build();
    }
    public JsonObject buildProgram( String programType, String trackedEntityTypeId, String... orgUnitIds )
    {
        JsonObjectBuilder builder = new JsonObjectBuilder( buildProgram( programType ) );
        for ( String ouid : orgUnitIds )
        {
            builder.addOrAppendToArray( "organisationUnits", new JsonObjectBuilder()
                .addProperty( "id", ouid ).build() );
        }
        if ( !StringUtils.isEmpty( trackedEntityTypeId ) )
        {
            builder.addObject( "trackedEntityType", new JsonObjectBuilder().addProperty( "id", trackedEntityTypeId ) ).build();
        }
        return builder.build();
    }

}
