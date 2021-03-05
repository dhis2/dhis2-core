/*
 * Copyright (c) 2004-2020, University of Oslo
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

package org.hisp.dhis.tracker.importer;

import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.tracker.importer.TrackerActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;

import static org.hamcrest.CoreMatchers.*;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class ImportStrategyTests
    extends ApiTest
{
    private TrackerActions trackerActions;

    @BeforeAll
    public void beforeAll()
    {
        trackerActions = new TrackerActions();

        new LoginActions().loginAsSuperUser();
    }

    @ParameterizedTest
    @ValueSource( strings = { "UPDATE", "DELETE" } )
    public void shouldReturnErrorWhenTeiDoesntExist( String importStrategy )
        throws Exception
    {
        JsonObject teiBody = new FileReaderUtils()
            .readJsonAndGenerateData( new File( "src/test/resources/tracker/importer/teis/tei.json" ) );

        ApiResponse response = trackerActions
            .postAndGetJobReport( teiBody, new QueryParamsBuilder().add( String.format( "importStrategy=%s", importStrategy ) ) );

        response.validate().statusCode( 200 )
            .body( "status", equalTo( "ERROR" ) )
            .body( "stats.ignored", equalTo( 1 ) )
            .body( "validationReport.errorReports", notNullValue() )
            .rootPath( "validationReport.errorReports[0]" )
            .body( "errorCode", equalTo( "E1063" ) )
            .body( "message", containsStringIgnoringCase( "does not exist" ) );
    }

    @Test
    public void shouldDeleteWithDeleteStrategy()
        throws Exception
    {
        String teiId = importTei();

        JsonObject teiBody = new FileReaderUtils()
            .readJsonAndGenerateData( new File( "src/test/resources/tracker/importer/teis/tei.json" ) );
        teiBody.getAsJsonArray( "trackedEntities" ).get( 0 ).getAsJsonObject().addProperty( "trackedEntity", teiId );

        ApiResponse response = trackerActions
            .postAndGetJobReport( teiBody, new QueryParamsBuilder().add( "importStrategy=DELETE" ) );

        response.validate().statusCode( 200 )
            .body( "status", equalTo( "OK" ) )
            .body( "stats.deleted", equalTo( 1 ) );
    }

    @Test
    public void shouldUpdateWithUpdateStrategy()
        throws Exception
    {
        String teiId = importTei();

        JsonObject teiBody = new FileReaderUtils()
            .readJsonAndGenerateData( new File( "src/test/resources/tracker/importer/teis/tei.json" ) );
        teiBody.getAsJsonArray( "trackedEntities" ).get( 0 ).getAsJsonObject().addProperty( "trackedEntity", teiId );

        ApiResponse response = trackerActions
            .postAndGetJobReport( teiBody, new QueryParamsBuilder().add( "importStrategy=UPDATE" ) );

        response.validate().statusCode( 200 )
            .body( "status", equalTo( "OK" ) )
            .body( "stats.updated", equalTo( 1 ) );
    }

    private String importTei()
        throws Exception
    {
        JsonObject teiBody = new FileReaderUtils()
            .readJsonAndGenerateData( new File( "src/test/resources/tracker/importer/teis/tei.json" ) );

        return trackerActions.postAndGetJobReport( teiBody ).validateSuccessfulImport().extractImportedTeis().get( 0 );

    }

}
