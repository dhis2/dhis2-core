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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.webapi.WebClient.Accept;
import static org.hisp.dhis.webapi.WebClient.Body;
import static org.hisp.dhis.webapi.WebClient.ContentType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonObject;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * Tests the
 * {@link org.hisp.dhis.webapi.controller.metadata.MetadataImportExportController}
 * using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
public class MetadataImportExportControllerTest extends DhisControllerConvenienceTest
{

    @Test
    public void testPostJsonMetadata()
    {
        assertWebMessage( "OK", 200, "OK", null,
            POST( "/38/metadata",
                "{'organisationUnits':[{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}]}" )
                    .content( HttpStatus.OK ) );
    }

    @Test
    public void testPostJsonMetadata_Empty()
    {
        assertWebMessage( "OK", 200, "OK", null,
            POST( "/38/metadata", "{}" ).content( HttpStatus.OK ) );
    }

    @Test
    public void testPostJsonMetadata_Async()
    {
        assertWebMessage( "OK", 200, "OK", "Initiated metadataImport",
            POST( "/metadata?async=true",
                "{'organisationUnits':[{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}]}" )
                    .content( HttpStatus.OK ) );
    }

    @Test
    public void testPostJsonMetadata_Pre38()
    {
        JsonObject report = POST( "/37/metadata",
            "{'organisationUnits':[{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}]}" )
                .content( HttpStatus.OK );
        assertEquals( "OK", report.getString( "status" ).string() );
    }

    @Test
    public void testPostCsvMetadata()
    {
        assertWebMessage( "OK", 200, "OK", null,
            POST( "/38/metadata?classKey=ORGANISATION_UNIT", Body( "," ), ContentType( "application/csv" ) )
                .content( HttpStatus.OK ) );
    }

    @Test
    public void testPostCsvMetadata_Async()
    {
        assertWebMessage( "OK", 200, "OK", "Initiated metadataImport",
            POST( "/metadata?async=true&classKey=ORGANISATION_UNIT", Body( "," ), ContentType( "application/csv" ) )
                .content( HttpStatus.OK ) );
    }

    @Test
    public void testPostCsvMetadata_Pre38()
    {
        JsonObject report = POST( "/37/metadata?classKey=ORGANISATION_UNIT", Body( "," ),
            ContentType( "application/csv" ) )
                .content( HttpStatus.OK );
        assertEquals( "OK", report.getString( "status" ).string() );
    }

    @Test
    public void testPostGmlMetadata()
    {
        assertWebMessage( "OK", 200, "OK", null,
            POST( "/38/metadata/gml", Body( "<metadata></metadata>" ),
                ContentType( "application/xml" ) ).content( HttpStatus.OK ) );
    }

    @Test
    public void testPostGmlMetadata_Async()
    {
        assertWebMessage( "OK", 200, "OK", "Initiated metadataImport",
            POST( "/metadata/gml?async=true", Body( "<metadata></metadata>" ),
                ContentType( "application/xml" ) ).content( HttpStatus.OK ) );
    }

    @Test
    public void testPostGmlMetadata_Pre38()
    {
        JsonObject report = POST( "/37/metadata/gml", Body( "<metadata></metadata>" ),
            ContentType( "application/xml" ) ).content( HttpStatus.OK );
        assertEquals( "OK", report.getString( "status" ).string() );
        assertEquals( "ImportReport", report.getString( "responseType" ).string() );
    }

    @Test
    public void testPostXmlMetadata()
    {
        HttpResponse response = POST( "/38/metadata", Body( "<metadata></metadata>" ),
            ContentType( "application/xml" ), Accept( "application/xml" ) );
        assertEquals( HttpStatus.OK, response.status() );
        assertTrue( response.content( MediaType.APPLICATION_XML ).startsWith( "<webMessage" ) );
    }

    @Test
    public void testPostXmlMetadata_Async()
    {
        HttpResponse response = POST( "/metadata?async=true", Body( "<metadata></metadata>" ),
            ContentType( "application/xml" ), Accept( "application/xml" ) );
        assertEquals( HttpStatus.OK, response.status() );
        assertTrue( response.content( MediaType.APPLICATION_XML ).startsWith( "<webMessage" ) );
    }

    @Test
    public void testPostXmlMetadata_Pre38()
    {
        HttpResponse response = POST( "/37/metadata", Body( "<metadata></metadata>" ),
            ContentType( "application/xml" ), Accept( "application/xml" ) );
        assertEquals( HttpStatus.OK, response.status() );
        assertTrue( response.content( MediaType.APPLICATION_XML ).startsWith( "<importReport" ) );
    }

    @Test
    public void testPostXmlMetadata_JsonResponse()
    {
        assertWebMessage( "OK", 200, "OK", null,
            POST( "/38/metadata", Body( "<metadata></metadata>" ),
                ContentType( "application/xml" ), Accept( "application/json" ) ).content( HttpStatus.OK ) );
    }
}
