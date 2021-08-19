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

import static org.junit.Assert.assertEquals;
import static org.wildfly.common.Assert.assertTrue;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonArray;
import org.hisp.dhis.webapi.json.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import com.google.common.collect.Sets;

/**
 * Tests the
 * {@link org.hisp.dhis.webapi.controller.event.ProgramMessageController} using
 * (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
public class ProgramMessageControllerTest extends DhisControllerConvenienceTest
{
    @Autowired
    private TrackedEntityInstanceService teiService;

    @Autowired
    private ProgramInstanceService piService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    private ProgramInstance piA;

    @Before
    public void setUp()
    {
        OrganisationUnit ouA = createOrganisationUnit( 'A' );
        idObjectManager.save( ouA );

        Program prA = createProgram( 'A', Sets.newHashSet(), ouA );
        idObjectManager.save( prA );

        TrackedEntityInstance teiA = createTrackedEntityInstance( 'A', ouA );
        teiService.addTrackedEntityInstance( teiA );

        piA = createProgramInstance( prA, teiA, ouA );
        piService.addProgramInstance( piA );
    }

    @Test
    public void testGetProgramMessages()
    {
        assertTrue( GET( "/messages?programInstance={id}", piA.getUid() ).content( HttpStatus.OK ).isArray() );
    }

    @Test
    public void testGetScheduledSentMessage()
    {
        assertTrue(
            GET( "/messages/scheduled/sent?programInstance={id}", piA.getUid() ).content( HttpStatus.OK ).isArray() );
    }

    @Test
    public void testSaveMessages()
    {
        JsonObject status = POST( "/messages", "{'programMessages': []}" ).content( HttpStatus.OK );
        assertTrue( status.isObject() );
        assertEquals( 1, status.size() );
        JsonArray summaries = status.getArray( "summaries" );
        assertTrue( summaries.isArray() );
        assertTrue( summaries.isEmpty() );
    }
}
