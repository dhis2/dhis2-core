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
package org.hisp.dhis.dxf2.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.jsontree.JsonTypedAccess;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseExtractor;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author aamerm
 */
public class ImportSummariesResponseExtractor implements ResponseExtractor<ImportSummaries>
{
    private static final ObjectMapper JSON_MAPPER = JacksonObjectMapperConfig.staticJsonMapper();

    @Override
    public ImportSummaries extractData( ClientHttpResponse response )
        throws IOException
    {
        JsonObject body = new JsonResponse( new String( response.getBody().readAllBytes(), StandardCharsets.UTF_8 ),
            JsonTypedAccess.GLOBAL );
        // auto-detect if it is wrapped in a WebMessage envelope
        if ( body.has( "httpStatus", "response" ) ) // is a WebMessage wrapper
        {
            return JSON_MAPPER.readValue( body.getObject( "response" ).node().getDeclaration(), ImportSummaries.class );
        }
        return JSON_MAPPER.readValue( body.node().getDeclaration(), ImportSummaries.class );
    }
}