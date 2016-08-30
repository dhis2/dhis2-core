package org.hisp.dhis.dxf2.common;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResponseExtractor;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * @author aamerm
 */
public class ImportSummariesResponseExtractor
    implements ResponseExtractor<ImportSummaries>
{
    @Override
    public ImportSummaries extractData( ClientHttpResponse response ) throws IOException
    {
        HttpStatus status = response.getStatusCode();

        if ( !(HttpStatus.CREATED.equals( status ) || HttpStatus.OK.equals( status )) )
        {
            throw new HttpServerErrorException( status, "Event sync failed on remote server" );
        }

        ImportSummaries summary = null;
        InputStream stream = response.getBody();

        if ( stream != null )
        {
            StringWriter writer = new StringWriter();
            IOUtils.copy( stream, writer, "UTF-8" );

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode objectNode = objectMapper.readTree( writer.toString() );
            JsonNode responseNode = objectNode.get( "response" );

            if ( responseNode != null && responseNode.get( "responseType" ) != null )
            {
                summary = objectMapper.readValue( responseNode.toString(), ImportSummaries.class );
            }
        }

        return summary;
    }
}