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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonObject;
import org.junit.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests the
 * {@link org.hisp.dhis.webapi.controller.metadata.MetadataProposalController}.
 *
 * @author Jan Bernitt
 */
public class MetadataProposalControllerTest extends DhisControllerConvenienceTest
{
    @Test
    public void testGetProposals()
    {
        String uid = postAddOrganisationUnitProposal( "My Unit", "OU1" );
        assertNotNull( uid );

        JsonObject page = GET( "/metadata/proposals/" ).content();
        assertTrue( page.has( "pager", "proposals" ) );
        assertEquals( 1, page.getArray( "proposals" ).size() );
        assertEquals( uid, page.getArray( "proposals" ).getObject( 0 ).getString( "id" ).string() );
    }

    @Test
    public void testGetProposal()
    {
        String uid = postAddOrganisationUnitProposal( "My Unit", "OU1" );
        assertNotNull( uid );

        JsonObject page = GET( "/metadata/proposals/" ).content();
        assertTrue( page.has( "pager", "proposals" ) );
        assertEquals( uid, page.getArray( "proposals" ).getObject( 0 ).getString( "id" ).string() );
    }

    private String postAddOrganisationUnitProposal( String name, String shortName )
    {
        return assertStatus( HttpStatus.CREATED,
            POST( "/metadata/proposals/", "{" +
                "'type':'ADD'," +
                "'target':'ORGANISATION_UNIT'," +
                "'change':{'name':'" + name + "', " +
                "'shortName':'" + shortName + "', " +
                "'openingDate': '2020-01-01'" +
                "}" +
                "}" ) );
    }
}
