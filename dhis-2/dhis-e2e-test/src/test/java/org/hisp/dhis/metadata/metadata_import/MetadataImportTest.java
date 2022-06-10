package org.hisp.dhis.metadata.metadata_import;

/*
 * Copyright (c) 2004-2021, University of Oslo
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
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matchers;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.SchemasActions;
import org.hisp.dhis.actions.SystemActions;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.ObjectReport;
import org.hisp.dhis.dto.TypeReport;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
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
    private MetadataActions metadataActions;

    private SystemActions systemActions;

    @BeforeAll
    public void before()
    {
        metadataActions = new MetadataActions();
        systemActions = new SystemActions();

        new LoginActions().loginAsSuperUser();
    }

    @ParameterizedTest( name = "withImportStrategy[{0}]" )
    @CsvSource( { "CREATE, ignored", "CREATE_AND_UPDATE, updated" } )
    public void shouldUpdateExistingMetadata( String importStrategy, String expected )
    {
        // arrange
        QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
        queryParamsBuilder.addAll( "async=false", "importReportMode=FULL", "importStrategy=" + importStrategy );

        // act
        ApiResponse response = metadataActions.postFile( new File("src/test/resources/setup/metadata.json"), queryParamsBuilder );

        // assert
        response.validate()
            .statusCode( 200 )
            .body( "stats", notNullValue() )
            .rootPath( "stats" )
            .body( "total", greaterThan( 0 ) )
            .body( "created", Matchers.equalTo( 0 ) )
            .body( "deleted", Matchers.equalTo( 0 ) )
            .body( "total", equalTo( response.extract( "stats." + expected ) ) );

        List<HashMap> typeReports = response.extractList( "typeReports.stats" );

        typeReports.forEach( x -> {
            assertEquals( x.get( expected ), x.get( "total" ), expected + " for " + x + " not equals to total" );
        } );
    }

    @Test
    public void shouldImportUniqueMetadataAndReturnObjectReports()
        throws Exception
    {
        // arrange
        JsonObject object = new FileReaderUtils()
            .readJsonAndGenerateData( new File( "src/test/resources/metadata/uniqueMetadata.json" ) );

        // act
        ApiResponse response = metadataActions
            .post( object, new QueryParamsBuilder().addAll( "async=false", "importReportMode=DEBUG", "importStrategy=CREATE" ) );

        // assert
        response.validate()
            .statusCode( 200 )
            .body( "stats", notNullValue() )
            .body( "stats.total", greaterThan( 0 ) )
            .body( "typeReports", notNullValue() )
            .body( "typeReports.stats", notNullValue() )
            .body( "typeReports.objectReports", Matchers.notNullValue() );

        List<HashMap> stats = response.extractList( "typeReports.stats" );

        stats.forEach( x -> {
            assertEquals( x.get( "total" ), x.get( "created" ) );
        } );

        List<ObjectReport> objectReports = getObjectReports( response.getTypeReports() );

        assertNotNull( objectReports );
        validateCreatedEntities( objectReports );
    }

    @Test
    public void shouldReturnObjectReportsWhenSomeMetadataWasIgnoredAndAtomicModeFalse()
        throws Exception
    {
        // arrange
        QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
        queryParamsBuilder.addAll( "async=false", "importReportMode=DEBUG", "importStrategy=CREATE", "atomicMode=NONE" );

        JsonObject object = new FileReaderUtils()
            .readJsonAndGenerateData( new File( "src/test/resources/metadata/uniqueMetadata.json" ) );

        // act
        ApiResponse response = metadataActions.post( object, queryParamsBuilder );
        response.validate().statusCode( 200 );

        JsonObject newObj = new FileReaderUtils()
            .readJsonAndGenerateData( new File( "src/test/resources/metadata/uniqueMetadata.json" ) );

        // add one of the orgunits from already imported metadata to get it ignored
        newObj.get( "organisationUnits" )
            .getAsJsonArray()
            .add( object.get( "organisationUnits" ).getAsJsonArray().get( 0 ) );

        response = metadataActions.post( newObj, queryParamsBuilder );

        // assert
        response.validate()
            .statusCode( 200 )
            .body( "stats", notNullValue() )
            .body( "stats.total", greaterThan( 1 ) )
            .body( "stats.ignored", equalTo( 1 ) )
            .body( "stats.created", equalTo( (Integer) response.extract( "stats.total" ) - 1 ) );

        int total = (int) response.extract( "stats.total" );

        List<ObjectReport> objectReports = getObjectReports( response.getTypeReports() );

        assertNotNull( objectReports );
        validateCreatedEntities( objectReports );

        assertThat( objectReports, hasItems( hasProperty( "errorReports", notNullValue() ) ) );
        assertEquals( total, objectReports.size(), "Not all imported entities had object reports" );
    }

    @Test
    public void shouldReturnImportSummariesWhenImportingInvalidMetadataAsync()
        throws Exception
    {
        // arrange
        QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
        queryParamsBuilder
            .addAll( "async=true", "importReportMode=DEBUG", "importStrategy=CREATE_AND_UPDATE", "atomicMode=NONE" );

        JsonObject metadata = new FileReaderUtils()
            .readJsonAndGenerateData( new File( "src/test/resources/metadata/uniqueMetadata.json" ) );

        metadata.getAsJsonArray( "organisationUnits" ).get( 0 ).getAsJsonObject()
            .addProperty( "shortName", RandomStringUtils.random( 51 ) );

        // act
        ApiResponse response = metadataActions.post( metadata, queryParamsBuilder );
        response.validate()
            .statusCode( 200 )
            .body( not( equalTo( "null" ) ) )
            .body( "response.name", equalTo( "metadataImport" ) )
            .body( "response.jobType", equalTo( "METADATA_IMPORT" ) );

        String taskId = response.extractString( "response.id" );

        // Validate that job was successful

        response = systemActions.waitUntilTaskCompleted( "METADATA_IMPORT", taskId );

        assertThat( response.extractList( "message" ), hasItem( containsString( "Import:Start" ) ) );
        assertThat( response.extractList( "message" ), hasItem( containsString( "Import:Done" ) ) );

        // validate task summaries were created
        response = systemActions.getTaskSummariesResponse( "METADATA_IMPORT", taskId );

        response.validate().statusCode( 200 )
            .body( not( equalTo( "null" ) ) )
            .body( "status", equalTo( "WARNING" ) )
            .body( "typeReports", notNullValue() )
            .rootPath( "typeReports" )
            .body( "stats.total", everyItem( greaterThan( 0 ) ) )
            .body( "stats.ignored", hasSize( greaterThanOrEqualTo( 1 ) ) )
            .body( "objectReports", notNullValue() )
            .body( "objectReports", hasSize( greaterThanOrEqualTo( 1 ) ) )
            .body( "objectReports.errorReports", notNullValue() );

    }

    @Test
    public void shouldImportMetadataAsync()
        throws Exception
    {
        JsonObject object = new FileReaderUtils()
            .readJsonAndGenerateData( new File( "src/test/resources/metadata/uniqueMetadata.json" ) );
        // arrange
        QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
        queryParamsBuilder
            .addAll( "async=false", "importReportMode=DEBUG", "importStrategy=CREATE_AND_UPDATE", "atomicMode=NONE" );

        // import metadata so that we have references and can clean up

        // act
        ApiResponse response = metadataActions.post( object, queryParamsBuilder );

        // send async request
        queryParamsBuilder.add( "async=true" );

        response = metadataActions.post( object, queryParamsBuilder );

        response.validate()
            .statusCode( 200 )
            .body( "response", notNullValue() )
            .body( "response.name", equalTo( "metadataImport" ) )
            .body( "response.jobType", equalTo( "METADATA_IMPORT" ) );

        String taskId = response.extractString( "response.id" );
        assertNotNull( taskId, "Task id was not returned" );
        // Validate that job was successful

        response = systemActions.waitUntilTaskCompleted( "METADATA_IMPORT", taskId );

        response.validate()
            .statusCode( 200 )
            .body( "message", notNullValue() )
            .body( "message", hasItem( containsString( "Import:Start" ) ) )
            .body( "message", hasItem( containsString( "Import:Done" ) ) );

        // validate task summaries were created
        response = systemActions.getTaskSummariesResponse( "METADATA_IMPORT", taskId );

        response.validate().statusCode( 200 )
            .body( not( equalTo( "null" ) ) )
            .body( "status", equalTo( "OK" ) )
            .body( "typeReports", notNullValue() )
            .rootPath( "typeReports" )
            .body( "stats", notNullValue() )
            .body( "stats.total", everyItem( greaterThan( 0 ) ) )
            .body( "objectReports", hasSize( greaterThan( 0 ) ) );
    }

    @Test
    public void shouldNotSkipSharing()
    {
        JsonObject object = generateMetadataObjectWithInvalidSharing();

        ApiResponse response = metadataActions.post( object, new QueryParamsBuilder().add( "skipSharing=false" ) );

        response.validate().statusCode( 200 )
            .body( "status", equalTo( "ERROR" ) )
            .body( "stats.created", equalTo( 0 ) )
            .body( "typeReports[0].objectReports[0].errorReports[0].message",
                stringContainsInOrder( "Invalid reference", "for association `userGroupAccesses`" ) );
    }

    @Test
    public void shouldSkipSharing()
    {
        JsonObject metadata = generateMetadataObjectWithInvalidSharing();

        ApiResponse response = metadataActions.post( metadata, new QueryParamsBuilder().add( "skipSharing=true" ) );

        response.validate().statusCode( 200 )
            .body( "status", isOneOf( "SUCCESS", "OK" ) )
            .body( "stats.created", equalTo( 1 ) );

    }

    private JsonObject generateMetadataObjectWithInvalidSharing()
    {
        JsonObject dataElementGroup = DataGenerator.generateObjectForEndpoint( "/dataElementGroup" );
        dataElementGroup.addProperty( "publicAccess", "rw------" );

        JsonArray userGroupAccesses = new JsonArray();
        JsonObject userGroupAccess = new JsonObject();
        userGroupAccess.addProperty( "access", "rwrw----" );
        userGroupAccess.addProperty( "userGroupUid", "non-existing-id" );
        userGroupAccess.addProperty( "id", "non-existing-id" );

        userGroupAccesses.add( userGroupAccess );

        dataElementGroup.add( "userGroupAccesses", userGroupAccesses );

        JsonArray array = new JsonArray();

        array.add( dataElementGroup );

        JsonObject metadata = new JsonObject();
        metadata.add( "dataElementGroups", array );

        return metadata;
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
