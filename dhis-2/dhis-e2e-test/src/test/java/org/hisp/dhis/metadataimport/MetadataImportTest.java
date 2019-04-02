/*
 * Copyright (c) 2004-2019, University of Oslo
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

/*
 * Copyright (c) 2004-2019, University of Oslo
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

package org.hisp.dhis.metadataimport;

import com.google.gson.JsonObject;
import org.hamcrest.Matchers;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.TestRunStorage;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.SchemasActions;
import org.hisp.dhis.actions.system.SystemActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class MetadataImportTest
    extends ApiTest
{
    List<HashMap> createdObjectReport = new ArrayList<>();

    private RestApiActions metadataActions;

    private SchemasActions schemasActions;

    @BeforeEach
    public void before()
    {
        schemasActions = new SchemasActions();
        metadataActions = new RestApiActions( "/metadata" );

        new LoginActions().loginAsDefaultUser();
    }

    @ParameterizedTest( name = "withImportStrategy[{0}]" )
    @CsvSource( { "CREATE, ignored", "CREATE_AND_UPDATE, updated" } )
    public void shouldUpdateExistingMetadata( String importStrategy, String expected )
    {
        JsonObject exported = metadataActions.get().getBody();

        String params = "?async=false" +
            "&importReportMode=FULL" +
            "&importStrategy=" + importStrategy;

        ApiResponse response = metadataActions.post( params, exported );

        System.out.print( response.getBody() );

        response.validate()
            .statusCode( 200 )
            .body( "stats.total", greaterThan( 0 ) )
            .body( "stats.created", Matchers.equalTo( 0 ) )
            .body( "stats.deleted", Matchers.equalTo( 0 ) );

        assertEquals( response.extractString( "stats.total" ), response.extractString( "stats." + expected ),
            "Ignored objects count should have matched total." );

        List<HashMap> typeReports = response.extractList( "typeReports.stats" );

        typeReports.forEach( x -> {
            assertEquals( x.get( expected ), x.get( "total" ) );
        } );
    }

    @Test
    public void shouldImportUniqueMetadataAndReturnObjectReports()
        throws Exception
    {
        String params = "?async=false" +
            "&importReportMode=DEBUG" +
            "&importStrategy=CREATE";

        JsonObject object = new FileReaderUtils()
            .readJsonAndGenerateData( new File( "src/test/resources/metadata/uniqueMetadata.json" ) );

        ApiResponse response = metadataActions.post( params, object );

        response.validate()
            .statusCode( 200 )
            .body( "stats.total", greaterThan( 0 ) )
            .body( "typeReports", Matchers.notNullValue() )
            .body( "typeReports.stats", Matchers.notNullValue() )
            .body( "typeReports.objectReports", Matchers.notNullValue() );

        List<HashMap> stats = response.extractList( "typeReports.stats" );

        stats.forEach( x -> {
            assertEquals( x.get( "total" ), x.get( "created" ) );
        } );

        // assert all object reports contains references and store them for clean up
        ArrayList<HashMap> objectReports = flatten( response.extractList( "typeReports.objectReports" ) );

        validateAndStoreCreatedEntities( objectReports );
    }

    @Test
    public void shouldReturnObjectReportsWhenSomeMetadataWasIgnoredAndAtomicModeFalse()
        throws Exception
    {
        String params = "?async=false" +
            "&importReportMode=DEBUG" +
            "&importStrategy=CREATE" +
            "&atomicMode=NONE";

        JsonObject object = new FileReaderUtils()
            .readJsonAndGenerateData( new File( "src/test/resources/metadata/uniqueMetadata.json" ) );

        ApiResponse response = metadataActions.post( params, object );
        response.validate().statusCode( 200 );

        JsonObject newObj = new FileReaderUtils()
            .readJsonAndGenerateData( new File( "src/test/resources/metadata/uniqueMetadata.json" ) );

        // add one of the orgunits from already imported metadata to get it ignored
        newObj.get( "organisationUnits" )
            .getAsJsonArray()
            .add( object.get( "organisationUnits" ).getAsJsonArray().get( 0 ) );

        response = metadataActions.post( params, newObj );

        response.validate()
            .statusCode( 200 )
            .body( "stats.total", greaterThan( 1 ) )
            .body( "stats.ignored", equalTo( 1 ) );

        assertEquals( response.extract( "stats.created" ), (Integer) response.extract( "stats.total" ) - 1 );

        int total = (int) response.extract( "stats.total" );

        ArrayList<HashMap> flatenned = flatten( response.extractList( "typeReports.objectReports" ) );

        validateAndStoreCreatedEntities( flatenned );

        assertEquals( total, flatenned.size(), "Not all imported entities had object reports" );

    }

    @Test
    public void shouldImportMetadataAsync()
        throws Exception
    {
        String params = "?async=false" +
            "&importReportMode=DEBUG" +
            "&importStrategy=CREATE" +
            "&atomicMode=NONE";

        // import metadata so that we have references and can clean up
        JsonObject object = new FileReaderUtils()
            .readJsonAndGenerateData( new File( "src/test/resources/metadata/uniqueMetadata.json" ) );

        ApiResponse response = metadataActions.post( params, object );

        ArrayList<HashMap> flatennedObjectReports = flatten( response.extractList( "typeReports.objectReports" ) );

        storeCreatedEntities( flatennedObjectReports );

        // send async request
        params = params.replace( "async=false", "async=true" );

        response = metadataActions.post( params, object );

        response.validate()
            .statusCode( 200 )
            .body( "response.name", equalTo( "metadataImport" ) )
            .body( "response.jobType", equalTo( "METADATA_IMPORT" ) );

        String taskId = response.extractString( "response.id" );

        // Validate that job was successful

        response = new SystemActions().waitUntilTaskCompleted( "METADATA_IMPORT", taskId );

        assertThat( response.extractList( "message" ), hasItem( containsString( "Import:Start" ) ) );
        assertThat( response.extractList( "message" ), hasItem( containsString( "Import:Done" ) ) );
    }

    private void validateAndStoreCreatedEntities( ArrayList<HashMap> array )
    {
        array.forEach(
            report -> {
                assertNotEquals( "", report.get( "uid" ) );
                assertNotEquals( "", report.get( "klass" ) );
                assertNotEquals( "", report.get( "index" ) );
                assertNotEquals( "", report.get( "displayName" ) );

                createdObjectReport.add( report );
            }
        );
    }

    private void storeCreatedEntities( ArrayList<HashMap> hashMaps )
    {
        createdObjectReport.addAll( hashMaps );
    }

    private ArrayList<HashMap> flatten( List<List> array )
    {
        ArrayList list = new ArrayList();
        array.forEach( p -> {
            p.forEach( map -> {
                list.add( map );
            } );
        } );

        return list;
    }

    @AfterEach
    public void cleanUp()
    {
        createdObjectReport.forEach( p -> {
            String resource = schemasActions.findSchemaPropertyByKlassName( String.valueOf( p.get( "klass" ) ), "plural" );
            TestRunStorage.addCreatedEntity( resource, String.valueOf( p.get( "uid" ) ) );
        } );
    }
}