/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static java.lang.String.format;
import static org.hisp.dhis.web.WebClient.Body;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Tests handling of data values of {@link ValueType#FILE_RESOURCE}. These need
 * to deal with the file resource associated with the value.
 *
 * @author Jan Bernitt
 */
class DataValueFileResourceControllerTest extends DhisControllerIntegrationTest
{
    private String pe;

    private String de;

    private String ou;

    private String coc;

    @BeforeEach
    void setUp()
    {
        pe = "2021-01";
        JsonObject ccDefault = GET(
            "/categoryCombos/gist?fields=id,categoryOptionCombos::ids&pageSize=1&headless=true&filter=name:eq:default" )
                .content().getObject( 0 );
        String cc = ccDefault.getString( "id" ).string();
        coc = ccDefault.getArray( "categoryOptionCombos" ).getString( 0 ).string();
        de = addDataElement( "file data", "FDE1", ValueType.FILE_RESOURCE, null, cc );
        ou = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits/", "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}" ) );
        // add OU to users hierarchy
        assertStatus( HttpStatus.OK, POST( "/users/{id}/organisationUnits", getCurrentUser().getUid(),
            Body( "{'additions':[{'id':'" + ou + "'}]}" ) ) );
    }

    @Test
    void testClearFileResourceDataValue_EmptyFile()
    {
        assertClearsFileResourceDataValue( () ->
        // making a request to the /dataValues/file with "file" being undefined
        assertStatus( HttpStatus.ACCEPTED, POST( format( "/dataValues/file?de=%s&pe=%s&ou=%s&co=%s&file=",
            de, pe, ou, coc ) ) ) );
    }

    @Test
    void testClearFileResourceDataValue_EmptyValue()
    {
        assertClearsFileResourceDataValue( () ->
        // making a request to normal /dataValues endpoint with an undefined "value"
        assertStatus( HttpStatus.CREATED, POST( format( "/dataValues?de=%s&pe=%s&ou=%s&co=%s",
            de, pe, ou, coc ) ) ) );
    }

    private void assertClearsFileResourceDataValue( Runnable clearRequest )
    {
        String url = format( "/dataValues/file?de=%s&pe=%s&ou=%s&co=%s", de, pe, ou, coc );
        MockMultipartFile image = new MockMultipartFile( "file", "OU_profile_image.png", "image/png",
            "<<png data>>".getBytes() );
        // create the data value with a file resource that is cleared
        assertStatus( HttpStatus.ACCEPTED, POST_MULTIPART( url, image ) );

        // check the file resource does exist
        JsonArray values = GET( "/dataValues?de={de}&pe={pe}&ou={ou}", de, pe, ou ).content();
        assertEquals( 1, values.size() );
        String fileUid = values.getString( 0 ).string();
        assertTrue( CodeGenerator.isValidUid( fileUid ) );
        assertStatus( HttpStatus.OK, GET( "/fileResources/{id}", fileUid ) );

        // clear the data value
        clearRequest.run();

        // check the file resource no longer exists
        assertStatus( HttpStatus.NOT_FOUND, GET( "/fileResources/{id}", fileUid ) );
    }
}
