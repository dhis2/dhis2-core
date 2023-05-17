/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.metadata.metadata_import;

import static org.hamcrest.Matchers.equalTo;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.file.JsonFileReader;
import org.hisp.dhis.utils.DataGenerator;
import org.hisp.dhis.utils.SharingUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.gson.JsonObject;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class MetadataImportImportStrategyTests
    extends ApiTest
{
    private MetadataActions metadataActions;

    @BeforeAll
    public void before()
    {
        metadataActions = new MetadataActions();

        new LoginActions().loginAsAdmin();
    }

    @ValueSource( strings = {
        "CODE",
        "UID",
        "NAME"
    } )
    @ParameterizedTest
    public void shouldUpdateMetadataByIdentifier( String identifier )
        throws IOException
    {
        JsonObject ob = new JsonFileReader( new File( "src/test/resources/setup/metadata.json" ) )
            .get( JsonObject.class );

        ApiResponse response = metadataActions.importMetadata( ob, "identifier=" + identifier );

        response
            .validate().statusCode( 200 )
            .body( "stats.updated", equalTo( response.extract( "stats.total" ) ) );
    }

    @Test
    public void shouldCreateMetadataWithCodeIdentifier()
    {
        JsonObject object = JsonObjectBuilder
            .jsonObject( DataGenerator.generateObjectForEndpoint( "/dataElementGroup" ) )
            .addProperty( "code", "TA_CODE_DATAELEMENT_GROUP" )
            .addObject( "sharing",
                SharingUtils.createSharingObject( null, null, null, Map.of( "OPVIvvXzNTw", "rw------" ) ) )
            .wrapIntoArray( "dataElementGroups" );

        ApiResponse response = metadataActions.importMetadata( object, "identifier=CODE" );

        response
            .validate().statusCode( 200 )
            .body( "response.stats.created", equalTo( 1 ) );

        response = metadataActions.importMetadata( object, "identifier=CODE" );

        response
            .validate().statusCode( 200 )
            .body( "response.stats.updated", equalTo( 1 ) );
    }
}
