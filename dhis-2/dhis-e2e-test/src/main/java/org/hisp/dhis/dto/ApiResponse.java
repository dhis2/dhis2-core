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

package org.hisp.dhis.dto;

import com.google.gson.JsonObject;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class ApiResponse
{
    private Response raw;

    public ApiResponse( Response response )
    {
        raw = response;
    }

    /**
     * Extracts uid when only one object was created.
     *
     * @return
     */
    public String extractUid()
    {
        String uid;

        if ( extract( "response" ) == null )
        {
            return extractString( "id" );
        }

        uid = extractString( "response.uid" );

        if ( !StringUtils.isEmpty( uid ) )
        {
            return uid;
        }

        return extractString( "response.importSummaries.reference[0]" );
    }

    /**
     * Extracts uids from import summaries.
     * Use when more than one object was created.
     *
     * @return
     */
    public List<String> extractUids()
    {
        return extractList( "response.importSummaries.reference" );
    }

    public String extractString( String path )
    {
        return raw.jsonPath().getString( path );
    }

    public Object extract( String path )
    {
        return raw.jsonPath().get( path );
    }

    public <T> List<T> extractList( String path )
    {
        return raw.jsonPath().getList( path );
    }

    public <T> List<T> extractList( String path, Class<T> type )
    {
        return raw.jsonPath().getList( path, type );
    }

    public int statusCode()
    {
        return raw.statusCode();
    }

    public ValidatableResponse validate()
    {
        return raw.then();
    }

    public JsonObject getBody()
    {
        return raw.getBody().as( JsonObject.class, ObjectMapperType.GSON );
    }

    public boolean isEntityCreated()
    {
        return this.extractUid() != null;
    }

    public boolean containsImportSummaries()
    {
        return StringUtils.equals( extractString( "response.responseType" ), "ImportSummaries" ) ||
            StringUtils.equals( extractString( "responseType" ), "ImportSummaries" );
    }

    public List<ImportSummary> getImportSummaries()
    {
        if ( this.extract( "responseType" ) != null && this.extract( "responseType" ).equals( "ImportSummaries" ) )
        {
            return this.extractList( "importSummaries", ImportSummary.class );

        }
        return this.extractList( "response.importSummaries", ImportSummary.class );

    }

    public List<ImportSummary> getSuccessfulImportSummaries()
    {
        return getImportSummaries().stream()
            .filter( is -> {
                return is.getStatus() == "SUCCESS";
            } )
            .collect( Collectors.toList() );
    }

}
