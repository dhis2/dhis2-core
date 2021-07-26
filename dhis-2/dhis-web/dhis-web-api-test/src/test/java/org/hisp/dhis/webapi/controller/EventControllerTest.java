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
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_XML;

import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.event.EventController} using
 * (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
public class EventControllerTest extends DhisControllerConvenienceTest
{
    @Test
    public void testPostXmlEvent()
    {
        HttpResponse response = POST( "/events/", Body( "<events></events>" ), ContentType( APPLICATION_XML ),
            Accept( APPLICATION_XML ) );
        assertEquals( HttpStatus.OK, response.status() );
        assertTrue( response.content( APPLICATION_XML ).startsWith( "<webMessage " ) );
    }

    @Test
    public void testPostXmlEvent_Async()
    {
        HttpResponse response = POST( "/events?async=true", Body( "<events></events>" ), ContentType( APPLICATION_XML ),
            Accept( APPLICATION_XML ) );
        assertEquals( HttpStatus.OK, response.status() );
        assertTrue( response.content( APPLICATION_XML ).startsWith( "<webMessage " ) );
    }

    @Test
    public void testPostJsonEvent()
    {
        assertWebMessage( "OK", 200, "OK", "Import was successful.",
            POST( "/events/", "{'events':[]}" ).content( HttpStatus.OK ) );
    }

    @Test
    public void testPostJsonEvent_Async()
    {
        assertWebMessage( "OK", 200, "OK", "Initiated inMemoryEventImport",
            POST( "/events?async=true", "{'events':[]}" ).content( HttpStatus.OK ) );
    }

    @Test
    public void testPostJsonEventForNote_NoSuchObject()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "Event not found for ID xyz",
            POST( "/events/xyz/note", "{}" ).content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    public void testPostCsvEvents()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "An error occurred, please check import summary.",
            POST( "/events", Body( ",," ), ContentType( "text/csv" ) )
                .content( HttpStatus.CONFLICT ) );
    }

    @Test
    public void testPostCsvEvents_Async()
    {
        assertWebMessage( "OK", 200, "OK", "Initiated inMemoryEventImport",
            POST( "/events?async=true", Body( ",," ), ContentType( "text/csv" ) )
                .content( HttpStatus.OK ) );
    }

    @Test
    public void testPutXmlEvent()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "An error occurred, please check import summary.",
            PUT( "/events/xyz", Body( "<event></event>" ), ContentType( APPLICATION_XML ) )
                .content( HttpStatus.CONFLICT ) );
    }

    @Test
    public void testPutJsonEvent()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "An error occurred, please check import summary.",
            PUT( "/events/xyz", Body( "{}" ) )
                .content( HttpStatus.CONFLICT ) );
    }

    @Test
    public void testPutJsonEventSingleValue_NoSuchObject()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "DataElement not found for ID abc",
            PUT( "/events/xyz/abc", "{}" ).content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    public void testPutJsonEventForEventDate_NoSuchObject()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "Event not found for ID xyz",
            PUT( "/events/xyz/eventDate", "{}" ).content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    public void testDeleteEvent_NoSuchObject()
    {
        assertWebMessage( "OK", 200, "OK", "Import was successful.",
            DELETE( "/events/xyz" ).content( HttpStatus.OK ) );
    }

    private String postEvent()
    {
        String ouId = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits/", "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}" ) );

        String progId = assertStatus( HttpStatus.CREATED,
            POST( "/programs/",
                "{'name':'P1', 'shortName':'P1', 'programType':'WITHOUT_REGISTRATION','organisationUnits':[{'id':'"
                    + ouId + "'}]}" ) );

        String psId = assertStatus( HttpStatus.CREATED,
            POST( "/programStages/", "{'name':'PS1', 'shortName':'PS1', 'program':{'id':'" + progId + "'}}" ) );

        return assertStatus( HttpStatus.OK,
            POST( "/events",
                "{" +
                    "'status':'ACTIVE'," +
                    "'eventDate':'2030-01-01'," +
                    "'orgUnit':'" + ouId + "'," +
                    "'program':'" + progId + "'," +
                    "'programStage':'" + psId + "'" +
                    "}" ) );
    }
}
