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
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.utils.DataGenerator;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

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
        this.programStageActions = new ProgramStageActions();
    }

    public ApiResponse createProgram( String programType )
    {
        JsonObject object = getDummy( programType );

        if ( programType.equalsIgnoreCase( "WITH_REGISTRATION" ) )
        {
            JsonObjectBuilder.jsonObject( object )
                .addObject( "trackedEntityType", new JsonObjectBuilder().addProperty( "id", "Q9GufDoplCL" ) );
        }

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

        JsonObjectBuilder.jsonObject( object )
            .addOrAppendToArray( "programStageDataElements", new JsonObjectBuilder()
                .addProperty( "compulsory", String.valueOf( isMandatory ) )
                .addObject( "dataElement", new JsonObjectBuilder().addProperty( "id", dataElementId ) )
                .build() );

        return programStageActions.update( programStageId, object );
    }

    public ApiResponse addAttribute( String programId, String teiAttributeId, boolean isMandatory )
    {
        JsonObject object = this.get( programId, new QueryParamsBuilder().add( "fields=*" ) ).getBody();

        JsonObjectBuilder.jsonObject( object )
            .addOrAppendToArray( "programTrackedEntityAttributes", new JsonObjectBuilder()
                .addProperty( "mandatory", String.valueOf( isMandatory ) )
                .addObject( "trackedEntityAttribute", new JsonObjectBuilder().addProperty( "id", teiAttributeId ) )
                .build() );

        return this.update( programId, object );
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

    public ApiResponse getOrgUnitsAssociations( String... programUids )
    {
        return get( "/orgUnits", new QueryParamsBuilder().add(
            Arrays.stream( programUids )
                .collect( Collectors.joining( ",", "programs=", "" ) ) ) );
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
