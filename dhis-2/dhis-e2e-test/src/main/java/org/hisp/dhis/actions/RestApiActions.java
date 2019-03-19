/*
 * Copyright (c) 2004-2018, University of Oslo
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

package org.hisp.dhis.actions;

import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.hisp.dhis.TestRunStorage;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.ImportSummary;

import java.io.File;
import java.util.List;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class RestApiActions
{
    protected String endpoint;

    public RestApiActions( final String endpoint )
    {
        this.endpoint = endpoint;
    }

    protected RequestSpecification given()
    {
        return RestAssured.given()
            .basePath( endpoint )
            .config( RestAssured.config()
                .objectMapperConfig( new ObjectMapperConfig( ObjectMapperType.GSON ) ) );
    }

    /**
     * Sends post request to specified endpoint.
     * If post request successful, saves created entity in TestRunStorage
     *
     * @param object Body of request
     * @return Response
     */
    public ApiResponse post( Object object )
    {
        return post( "", object );
    }

    public ApiResponse post( String resource, Object object )
    {
        return post( resource, object, ContentType.JSON.toString() );
    }

    public ApiResponse post( String resource, Object object, String contentType )
    {
        ApiResponse response = new ApiResponse( this.given()
            .body( object )
            .contentType( contentType )
            .when()
            .post( resource ) );

        saveCreatedObjects( response );

        return response;
    }

    /**
     * Shortcut used in preconditions only.
     * Sends post request to specified endpoint and verifies that request was successful
     *
     * @param object Body of reqeuest
     * @return ID of generated entity.
     */
    public String create( Object object )
    {
        ApiResponse response = post( object );

        response.validate()
            .statusCode( 201 );

        return response.extractUid();
    }

    /**
     * Sends get request with provided path appended to URL.
     *
     * @param path ID of resource
     * @return Response
     */
    public ApiResponse get( String path )
    {
        Response response = this.given()
            .contentType( ContentType.TEXT )
            .when()
            .get( path );

        return new ApiResponse( response );
    }

    /**
     * Sends get request to specified endpoint
     *
     * @return Response
     */
    public ApiResponse get()
    {
        Response response = this.given()
            .contentType( ContentType.TEXT )
            .when()
            .get();

        return new ApiResponse( response );
    }

    /**
     * Sends get request with provided path and queryParams appended to URL.
     *
     * @param path        Id of resource
     * @param queryParams Query params to append to url
     * @return
     */
    public ApiResponse get( String path, String queryParams )
    {
        Response response = this.given()
            .contentType( ContentType.TEXT )
            .when()
            .get( path + "?" + queryParams );

        return new ApiResponse( response );
    }

    /**
     * Sends delete request to specified resource.
     * If delete request successful, removes entity from TestRunStorage.
     *
     * @param path Id of resource
     * @return
     */
    public ApiResponse delete( String path )
    {
        Response response = this.given()
            .when()
            .delete( path );

        if ( response.statusCode() == 200 )
        {
            TestRunStorage.removeEntity( endpoint, path );
        }

        return new ApiResponse( response );
    }

    /**
     * Sends PUT request to specified resource.
     *
     * @param path   Id of resource
     * @param object Body of request
     * @return
     */
    public ApiResponse update( String path, Object object )
    {
        Response response =
            this.given().body( object, ObjectMapperType.GSON )
                .when()
                .put( path );

        return new ApiResponse( response );
    }

    public ApiResponse postFile( File file, String queryParams )
    {
        ApiResponse response = new ApiResponse( this.given()
            .body( file )
            .when()
            .post( queryParams ) );

        saveCreatedObjects( response );

        return response;

    }

    private void saveCreatedObjects( ApiResponse response )
    {
        if ( response.containsImportSummaries() )
        {
            List<ImportSummary> importSummaries = response.getSuccessfulImportSummaries();
            importSummaries.forEach( importSummary -> {
                TestRunStorage.addCreatedEntity( endpoint, importSummary.getReference() );
            } );
            return;
        }

        if ( response.isEntityCreated() )
        {
            TestRunStorage.addCreatedEntity( endpoint, response.extractUid() );
        }

    }
}

