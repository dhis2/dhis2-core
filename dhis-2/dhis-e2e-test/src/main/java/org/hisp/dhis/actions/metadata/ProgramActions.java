package org.hisp.dhis.actions.metadata;

/*
 * Copyright (c) 2004-2021 University of Oslo
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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
    public ProgramStageActions programStageActions;

    public ProgramActions()
    {
        super( "/programs" );
        this.programStageActions = new ProgramStageActions( );
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
        String programStageId = createProgramStage( "DEFAULT STAGE" );

        JsonObject body = getDummy( "WITHOUT_REGISTRATION", orgUnitsIds );

        JsonArray programStages = new JsonArray();

        JsonObject programStage = new JsonObject();
        programStage.addProperty( "id", programStageId );

        programStages.add( programStage );

        body.add( "programStages", programStages );

        return post( body );
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

    public String createProgramStage( String name )
    {
        JsonObject body = new JsonObject();

        body.addProperty( "name", name );

        ApiResponse response = programStageActions.post( body );
        response.validate().statusCode( Matchers.isOneOf( 201, 200 ) );

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
            .addUserGroupAccess()
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

}
