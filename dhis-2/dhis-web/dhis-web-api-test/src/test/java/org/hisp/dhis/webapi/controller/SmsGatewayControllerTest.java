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

import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.wildfly.common.Assert.assertTrue;

import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonArray;
import org.hisp.dhis.webapi.json.JsonObject;
import org.junit.After;
import org.junit.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.sms.SmsGatewayController}
 * using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
public class SmsGatewayControllerTest extends DhisControllerConvenienceTest
{
    private String uid;

    @After
    public void tearDown()
    {
        JsonArray gateways = GET( "/gateways" ).content().getArray( "gateways" );
        for ( JsonObject gateway : gateways.asList( JsonObject.class ) )
        {
            assertStatus( HttpStatus.OK, DELETE( "/gateways/" + gateway.getString( "uid" ).string() ) );
        }
        assertTrue( GET( "/gateways" ).content().getArray( "gateways" ).isEmpty() );
    }

    @Test
    public void testSetDefault()
    {
        uid = assertStatus( HttpStatus.OK,
            POST( "/gateways", "{'name':'test', 'username':'user', 'password':'pwd', 'type':'http'}" ) );

        assertWebMessage( "OK", 200, "OK", "test is set to default",
            PUT( "/gateways/default/" + uid ).content( HttpStatus.OK ) );
    }

    @Test
    public void testSetDefault_NoSuchObject()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "No gateway found",
            PUT( "/gateways/default/xyz" ).content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    public void testUpdateGateway()
    {
        uid = assertStatus( HttpStatus.OK,
            POST( "/gateways", "{'name':'test', 'username':'user', 'password':'pwd', 'type':'http'}" ) );

        JsonObject gateway = GET( "/gateways/{uid}", uid ).content();
        assertWebMessage( "OK", 200, "OK", "Gateway with uid: " + uid + " has been updated",
            PUT( "/gateways/" + uid, gateway.toString() ).content( HttpStatus.OK ) );
    }

    @Test
    public void testUpdateGateway_NoSuchObject()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "No gateway found",
            PUT( "/gateways/xyz" ).content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    public void testAddGateway()
    {
        assertWebMessage( "OK", 200, "OK", "Gateway configuration added",
            POST( "/gateways", "{'name':'test', 'username':'user', 'password':'pwd', 'type':'http'}" )
                .content( HttpStatus.OK ) );
    }

    @Test
    public void testRemoveGateway()
    {
        uid = assertStatus( HttpStatus.OK,
            POST( "/gateways", "{'name':'test', 'username':'user', 'password':'pwd', 'type':'http'}" ) );

        assertWebMessage( "OK", 200, "OK", "Gateway removed successfully",
            DELETE( "/gateways/" + uid ).content( HttpStatus.OK ) );
    }

    @Test
    public void testRemoveGateway_NoSuchObject()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "No gateway found with id: xyz",
            DELETE( "/gateways/xyz" ).content( HttpStatus.NOT_FOUND ) );
    }
}
