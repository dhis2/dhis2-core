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
import io.restassured.mapper.ObjectMapperType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.hisp.dhis.TestRunStorage;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class ApiActions
{
    protected String endpoint;

    public ApiActions( final String endpoint )
    {
        this.endpoint = endpoint;
    }

    protected RequestSpecification given()
    {
        return RestAssured.given().basePath( endpoint );
    }

    /**
     * Sends post request to specified endpoint.
     * If post request successful, saves created entity in TestRunStorage
     *
     * @param object Body of request
     * @return Response
     */
    public Response post( Object object )
    {
        Response response = this.given()
            .body( object, ObjectMapperType.GSON )
            .when()
            .post();

        if ( response.statusCode() == 201 )
        {
            TestRunStorage.addCreatedEntity( endpoint, response.jsonPath().getString( "response.uid" ) );
        }

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
        Response response = post( object )
            .thenReturn();

        response.then().statusCode( 201 );

        return response.jsonPath().getString( "response.uid" );
    }

    /**
     * Sends get request with provided path appended to URL.
     *
     * @param path ID of resource
     * @return Response
     */
    public Response get( String path )
    {
        return
            this.given()
                .when()
                .get( path );
    }

    /**
     * Sends get request to specified endpoint
     *
     * @return Response
     */
    public Response get()
    {
        return this.given()
            .when()
            .get();
    }

    /**
     * Sends get request with provided path and queryParams appended to URL.
     *
     * @param path        Id of resource
     * @param queryParams Query params to append to url
     * @return
     */
    public Response get( String path, String queryParams )
    {
        return this.given()
            .when()
            .get( path + "?" + queryParams );

    }

    /**
     * Sends delete request to specified resource.
     * If delete request successful, removes entity from TestRunStorage.
     *
     * @param path Id of resource
     * @return
     */
    public Response delete( String path )
    {
        Response response = this.given()
            .when()
            .delete( path );

        if ( response.statusCode() == 200 )
        {
            TestRunStorage.removeEntity( endpoint, path );
        }

        return response;
    }

    /**
     * Sends PUT request to specified resource.
     *
     * @param path   Id of resource
     * @param object Body of request
     * @return
     */
    public Response update( String path, Object object )
    {
        Response response =
            this.given().body( object, ObjectMapperType.GSON )
                .when()
                .put( path );

        return response;
    }
}
