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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.webapi.WebClient.Accept;
import static org.hisp.dhis.webapi.WebClient.Body;
import static org.hisp.dhis.webapi.WebClient.ContentType;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_XML;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_XML_ADX;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_XML;

import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonImportSummary;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests the {@link DataValueSetController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class DataValueSetControllerTest extends DhisControllerConvenienceTest
{

    @Test
    void testPostJsonDataValueSet()
    {
        assertWebMessage( "OK", 200, "OK", "Import was successful.",
            POST( "/38/dataValueSets/", "{}" ).content( HttpStatus.OK ) );
    }

    @Test
    void testPostJsonDataValueSet_Async()
    {
        assertWebMessage( "OK", 200, "OK", "Initiated dataValueImport",
            POST( "/dataValueSets?async=true", "{}" ).content( HttpStatus.OK ) );
    }

    @Test
    void testPostJsonDataValueSet_Pre38()
    {
        JsonImportSummary summary = POST( "/37/dataValueSets/", "{}" ).content( HttpStatus.OK )
            .as( JsonImportSummary.class );
        assertEquals( "ImportSummary", summary.getResponseType() );
        assertEquals( "SUCCESS", summary.getStatus() );
    }

    @Test
    void testPostAdxDataValueSet()
    {
        String content = POST( "/38/dataValueSets/", Body( "<adx xmlns=\"urn:ihe:qrph:adx:2015\"></adx>" ),
            ContentType( CONTENT_TYPE_XML_ADX ), Accept( CONTENT_TYPE_XML ) ).content( APPLICATION_XML );
        assertTrue( content.contains( "httpStatusCode=\"200\"" ) );
    }

    @Test
    void testPostAdxDataValueSet_Async()
    {
        String content = POST( "/dataValueSets?async=true", Body( "<adx xmlns=\"urn:ihe:qrph:adx:2015\"></adx>" ),
            ContentType( CONTENT_TYPE_XML_ADX ), Accept( CONTENT_TYPE_XML ) ).content( APPLICATION_XML );
        assertTrue( content.contains( "httpStatusCode=\"200\"" ) );
        assertTrue( content.contains( "Initiated dataValueImport" ) );
    }

    @Test
    void testPostAdxDataValueSet_Pre38()
    {
        HttpResponse response = POST( "/37/dataValueSets/", Body( "<adx xmlns=\"urn:ihe:qrph:adx:2015\"></adx>" ),
            ContentType( CONTENT_TYPE_XML_ADX ), Accept( CONTENT_TYPE_XML ) );
        assertEquals( HttpStatus.OK, response.status() );
        assertTrue( response.content( APPLICATION_XML ).startsWith( "<importSummary " ) );
    }

    @Test
    void testPostDxf2DataValueSet()
    {
        String content = POST( "/38/dataValueSets/",
            Body( "<dataValueSet xmlns=\"http://dhis2.org/schema/dxf/2.0\"></dataValueSet>" ),
            ContentType( APPLICATION_XML ), Accept( CONTENT_TYPE_XML ) ).content( APPLICATION_XML );
        assertTrue( content.contains( "httpStatusCode=\"200\"" ) );
    }

    @Test
    void testPostDxf2DataValueSet_Async()
    {
        String content = POST( "/dataValueSets?async=true",
            Body( "<dataValueSet xmlns=\"http://dhis2.org/schema/dxf/2.0\"></dataValueSet>" ),
            ContentType( APPLICATION_XML ), Accept( CONTENT_TYPE_XML ) ).content( APPLICATION_XML );
        assertTrue( content.contains( "httpStatusCode=\"200\"" ) );
        assertTrue( content.contains( "Initiated dataValueImport" ) );
    }

    @Test
    void testPostDxf2DataValueSet_Pre38()
    {
        HttpResponse response = POST( "/37/dataValueSets/",
            Body( "<dataValueSet xmlns=\"http://dhis2.org/schema/dxf/2.0\"></dataValueSet>" ),
            ContentType( APPLICATION_XML ), Accept( CONTENT_TYPE_XML ) );
        assertEquals( HttpStatus.OK, response.status() );
        assertTrue( response.content( APPLICATION_XML ).startsWith( "<importSummary " ) );
    }

    @Test
    void testPostCsvDataValueSet()
    {
        assertWebMessage( "OK", 200, "OK", "Import was successful.",
            POST( "/38/dataValueSets/", Body( "abc" ), ContentType( "application/csv" ) ).content( HttpStatus.OK ) );
    }

    @Test
    void testPostCsvDataValueSet_Async()
    {
        assertWebMessage( "OK", 200, "OK", "Initiated dataValueImport",
            POST( "/dataValueSets?async=true", Body( "abc" ), ContentType( "application/csv" ) )
                .content( HttpStatus.OK ) );
    }

    @Test
    void testPostCsvDataValueSet_Pre38()
    {
        JsonImportSummary summary = POST( "/37/dataValueSets/", Body( "abc" ), ContentType( "application/csv" ) )
            .content( HttpStatus.OK ).as( JsonImportSummary.class );
        assertEquals( "ImportSummary", summary.getResponseType() );
        assertEquals( "SUCCESS", summary.getStatus() );
    }

    @Test
    void testGetDataValueSetJson()
    {
        String ouId = assertStatus( HttpStatus.CREATED, POST( "/organisationUnits/",
            "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01', 'code':'OU1'}" ) );
        String dsId = assertStatus( HttpStatus.CREATED,
            POST( "/dataSets/", "{'name':'My data set', 'periodType':'Monthly'}" ) );
        JsonWebMessage response = GET(
            "/dataValueSets/?inputOrgUnitIdScheme=code&idScheme=name&orgUnit={ou}&period=2022-01&dataSet={ds}", "OU1",
            dsId )
                .content( HttpStatus.CONFLICT ).as( JsonWebMessage.class );
        assertEquals( String.format( "User is not allowed to view org unit: `%s`", ouId ), response.getMessage() );
    }
}
