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

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link EventHookController} using (mocked) REST requests.
 *
 * @author Morten Olav Hansen
 */
class EventHookControllerTest extends DhisControllerIntegrationTest
{
    @Test
    void testGetEventHooks()
    {
        JsonObject eventHooks = GET( "/eventHooks" ).content( HttpStatus.OK );
        assertTrue( eventHooks.isObject() );
        assertTrue( eventHooks.has( "eventHooks" ) );
        assertTrue( eventHooks.get( "eventHooks" ).isArray() );
        assertTrue( eventHooks.get( "eventHooks" ).asList( JsonObject.class ).isEmpty() );
    }

    @Test
    void testCreateEventHookWebhook()
    {
        String id = assertStatus( HttpStatus.CREATED,
            POST( "/eventHooks/",
                "{'id':'bRNvL6NMQXb','name':'hook1','source':{'path':'metadata','fields':'id,name'},'targets':[{'type':'webhook','url':'http://localhost:8081/api/gateway','auth':{'type':'api-token','token':'EB3F6799-AA5A-47E8-B6B7-97EA54EB3873'}}]}" ) );

        assertEquals( "bRNvL6NMQXb", id );
    }
}
