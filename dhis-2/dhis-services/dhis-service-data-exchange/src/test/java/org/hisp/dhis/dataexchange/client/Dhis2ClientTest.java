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
package org.hisp.dhis.dataexchange.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;

import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dataexchange.client.response.Dhis2Response;
import org.hisp.dhis.dataexchange.client.response.Status;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;

class Dhis2ClientTest
{
    @Test
    void testGetDataValueSetUri()
    {
        String baseUrl = "https://play.dhis2.org/2.38.0";

        Dhis2Client client = Dhis2Client.withBasicAuth(
            baseUrl, "admin", "district" );

        ImportOptions optionsA = new ImportOptions()
            .setDataElementIdScheme( "code" )
            .setOrgUnitIdScheme( "code" )
            .setCategoryOptionComboIdScheme( "uid" );
        ImportOptions optionsB = new ImportOptions()
            .setDataElementIdScheme( "uid" )
            .setOrgUnitIdScheme( "code" )
            .setCategoryOptionComboIdScheme( "uid" )
            .setIdScheme( "code" );

        String uriA = client.getDataValueSetUri( optionsA ).toString();
        String uriB = client.getDataValueSetUri( optionsB ).toString();

        assertEquals( baseUrl + "/api/dataValueSets?dataElementIdScheme=code&orgUnitIdScheme=code", uriA );
        assertEquals( baseUrl + "/api/dataValueSets?orgUnitIdScheme=code&idScheme=code", uriB );
    }

    @Test
    void testNullConstructorArgument()
    {
        assertThrows( NullPointerException.class, () -> Dhis2Client.withBasicAuth(
            null, "admin", "district" ) );
        assertThrows( NullPointerException.class, () -> Dhis2Client.withBasicAuth(
            "https://play.dhis2.org/2.38.0", null, "district" ) );
    }

    @Test
    void testGetUrl()
    {
        Dhis2Client client = Dhis2Client.withBasicAuth(
            "https://play.dhis2.org/2.38.0", "admin", "district" );

        assertEquals( "https://play.dhis2.org/2.38.0", client.getUrl() );
    }

    @Test
    void testGetResolvedUriBuilder()
    {
        Dhis2Client client = Dhis2Client.withBasicAuth(
            "https://play.dhis2.org/2.38.0", "admin", "district" );

        assertEquals( "https://play.dhis2.org/2.38.0/api/dataValueSets",
            client.getResolvedUriBuilder( "dataValueSets" ).build().toUriString() );
        assertEquals( "https://play.dhis2.org/2.38.0/api/system/info",
            client.getResolvedUriBuilder( "system/info" ).build().toUriString() );
    }

    @Test
    void testGetJsonAuthHeaders()
    {
        Dhis2Client client = Dhis2Client.withBasicAuth(
            "https://play.dhis2.org/2.38.0", "admin", "district" );

        HttpHeaders headers = client.getJsonAuthHeaders();

        assertNotNull( headers );
        assertEquals( MediaType.APPLICATION_JSON, headers.getContentType() );
    }

    @Test
    void testAddIfNotDefault()
        throws Exception
    {
        Dhis2Client client = Dhis2Client.withBasicAuth(
            "https://play.dhis2.org/2.38.0", "admin", "district" );

        UriComponentsBuilder builder = UriComponentsBuilder.fromUri( new URI( "https://myserver.org" ) );

        client.addIfNotDefault( builder, "dataElementIdScheme", IdScheme.CODE );
        client.addIfNotDefault( builder, "orgUnitIdScheme", null );
        client.addIfNotDefault( builder, "idScheme", IdScheme.UID );

        assertEquals( "https://myserver.org?dataElementIdScheme=code", builder.build().toString() );
    }

    @Test
    void testDeserialize()
    {
        String json = "{\"httpStatusCode\": 409, \"status\": \"ERROR\", \"message\": \"There was a problem\"}";

        Dhis2Client client = Dhis2Client.withBasicAuth(
            "https://play.dhis2.org/2.38.0", "admin", "district" );

        Dhis2Response response = client.deserialize( json, Dhis2Response.class );

        assertEquals( HttpStatus.CONFLICT, response.getHttpStatus() );
        assertEquals( Status.ERROR, response.getStatus() );
        assertEquals( "There was a problem", response.getMessage() );
    }
}
