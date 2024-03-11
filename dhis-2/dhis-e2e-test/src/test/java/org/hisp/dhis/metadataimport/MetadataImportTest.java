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
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.SchemasActions;
import org.hisp.dhis.actions.system.SystemActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.ObjectReport;
import org.hisp.dhis.dto.TypeReport;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class MetadataImportTest
    extends ApiTest
{
    private RestApiActions metadataActions;

    private SchemasActions schemasActions;

    @BeforeEach
    public void before()
    {
        schemasActions = new SchemasActions();
        metadataActions = new RestApiActions( "/metadata" );

        new LoginActions().loginAsSuperUser();
    }

    @ParameterizedTest( name = "withImportStrategy[{0}]" )
    @CsvSource( { "CREATE, ignored", "CREATE_AND_UPDATE, updated" } )
    public void shouldUpdateExistingMetadata( String importStrategy, String expected )
    {
        // arrange
        JsonObject exported = metadataActions.get().getBody();

        String params = "?async=false" +
            "&importReportMode=FULL" +
            "&importStrategy=" + importStrategy;

        // act
        ApiResponse response = metadataActions.post( params, exported );

        // assert
        response.validate()
            .statusCode( 200 )
            .body( "stats.total", greaterThan( 0 ) )
            .body( "stats.created", equalTo( 0 ) )
            .body( "stats.deleted", equalTo( 0 ) )
            .body( "stats.total", equalTo( response.extract( "stats." + expected ) ) );

        List<HashMap> typeReports = response.extractList( "typeReports.stats" );

        typeReports.forEach( x -> {
            assertEquals( x.get( expected ), x.get( "total" ) );
        } );
    }

    @Test
    public void shouldImportUniqueMetadataAndReturnObjectReports()
        throws Exception
    {
        // arrange
        String params = "?async=false" +
            "&importReportMode=DEBUG" +
            "&importStrategy=CREATE";

        JsonObject object = new FileReaderUtils()
            .readJsonAndGenerateData( new File( "src/test/resources/metadata/uniqueMetadata.json" ) );

        // act
        ApiResponse response = metadataActions.post( params, object );

        // assert
        response.validate()
            .statusCode( 200 )
            .body( "stats.total", greaterThan( 0 ) )
            .body( "typeReports", notNullValue() )
            .body( "typeReports.stats", notNullValue() )
            .body( "typeReports.objectReports", notNullValue() );

        List<HashMap> stats = response.extractList( "typeReports.stats" );

        stats.forEach( x -> {
            assertEquals( x.get( "total" ), x.get( "created" ) );
        } );

        List<ObjectReport> objectReports = getObjectReports( response.getTypeReports() );

        assertNotNull( objectReports );
        validateCreatedEntities( objectReports );
    }

    @Disabled
    @Test
    public void shouldReturnObjectReportsWhenSomeMetadataWasIgnoredAndAtomicModeFalse()
        throws Exception
    {
        // arrange
        String params = "?async=false" +
            "&importReportMode=DEBUG" +
            "&importStrategy=CREATE" +
            "&atomicMode=NONE";

        JsonObject object = new FileReaderUtils()
            .readJsonAndGenerateData( new File( "src/test/resources/metadata/uniqueMetadata.json" ) );

        // act
        ApiResponse response = metadataActions.post( params, object );
        response.validate().statusCode( 200 );

        JsonObject newObj = new FileReaderUtils()
            .readJsonAndGenerateData( new File( "src/test/resources/metadata/uniqueMetadata.json" ) );

        // add one of the orgunits from already imported metadata to get it ignored
        newObj.get( "organisationUnits" )
            .getAsJsonArray()
            .add( object.get( "organisationUnits" ).getAsJsonArray().get( 0 ) );

        response = metadataActions.post( params, newObj );

        // assert
        response.validate()
            .statusCode( 200 )
            .body( "stats.total", greaterThan( 1 ) )
            .body( "stats.ignored", equalTo( 1 ) );

        assertEquals( response.extract( "stats.created" ), (Integer) response.extract( "stats.total" ) - 1 );

        int total = (int) response.extract( "stats.total" );

        List<ObjectReport> objectReports = getObjectReports( response.getTypeReports() );

        assertNotNull( objectReports );
        validateCreatedEntities( objectReports );

        assertThat( objectReports, Matchers.hasItems( hasProperty( "errorReports", notNullValue() ) ) );
        assertEquals( total, objectReports.size(), "Not all imported entities had object reports" );
    }

    @Test
    public void shouldImportMetadataAsync()
        throws Exception
    {
        // arrange
        String params = "?async=false" +
            "&importReportMode=DEBUG" +
            "&importStrategy=CREATE" +
            "&atomicMode=NONE";

        // import metadata so that we have references and can clean up
        JsonObject object = new FileReaderUtils()
            .readJsonAndGenerateData( new File( "src/test/resources/metadata/uniqueMetadata.json" ) );

        // act
        ApiResponse response = metadataActions.post( params, object );

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

    private List<ObjectReport> getObjectReports( List<TypeReport> typeReports )
    {
        List<ObjectReport> objectReports = new ArrayList<>();

        typeReports.stream().forEach( typeReport -> {
            objectReports.addAll( typeReport.getObjectReports() );
        } );

        return objectReports;
    }

    private void validateCreatedEntities( List<ObjectReport> objectReports )
    {
        objectReports.forEach(
            report -> {
                assertNotEquals( "", report.getUid() );
                assertNotEquals( "", report.getKlass() );
                assertNotEquals( "", report.getIndex() );
                assertNotEquals( "", report.getDisplayName() );
            }
        );
    }

}
