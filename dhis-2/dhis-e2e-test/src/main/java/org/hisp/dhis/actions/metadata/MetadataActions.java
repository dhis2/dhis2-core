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
package org.hisp.dhis.actions.metadata;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

import java.io.File;

import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.MetadataApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;

import com.google.gson.JsonObject;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class MetadataActions
    extends RestApiActions
{
    public MetadataActions()
    {
        super( "/metadata" );
    }

    public MetadataApiResponse importMetadata( File file, String... queryParams )
    {
        QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
        queryParamsBuilder.addAll( queryParams );
        queryParamsBuilder.addAll( "importReportMode=FULL" );

        ApiResponse response = postFile( file, queryParamsBuilder );
        response.validate().statusCode( 200 );

        return new MetadataApiResponse( response );
    }

    public MetadataApiResponse importMetadata( JsonObject object, String... queryParams )
    {
        QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
        queryParamsBuilder.addAll( queryParams );
        queryParamsBuilder.addAll( "atomicMode=OBJECT", "importReportMode=FULL" );

        ApiResponse response = post( object, queryParamsBuilder );
        response.validate().statusCode( 200 );

        return new MetadataApiResponse( response );
    }

    public MetadataApiResponse importAndValidateMetadata( JsonObject object, String... queryParams )
    {
        ApiResponse response = importMetadata( object, queryParams );

        response.validate().body( "response.stats.ignored", not(
            equalTo( response.extract( "response.stats.total" ) ) ) );

        return new MetadataApiResponse( response );
    }

    public MetadataApiResponse importAndValidateMetadata( File file, String... queryParams )
    {
        ApiResponse response = importMetadata( file, queryParams );

        response.validate().body( "response.stats.ignored", not(
            equalTo( response.extract( "response.stats.total" ) ) ) );

        return new MetadataApiResponse( response );
    }
}
