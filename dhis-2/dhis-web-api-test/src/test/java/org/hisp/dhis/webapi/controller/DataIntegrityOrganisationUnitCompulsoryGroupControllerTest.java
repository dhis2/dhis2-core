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

import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegritySummary;
import org.hisp.dhis.webapi.json.domain.JsonIdentifiableObject;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.Test;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataIntegrityOrganisationUnitCompulsoryGroupControllerTest extends DhisControllerIntegrationTest {
    @Test
    void testOrgUnitInCompulsoryGroup()
    {

        //Create an orgunit group
        String test_orgunitgroup = assertStatus( HttpStatus.CREATED,
                POST( "/organisationUnitGroups", "{'name': 'Type A', 'shortName': 'Type A'}" ) );
        //Add it to a group set
        String myougs = assertStatus( HttpStatus.CREATED,
                POST( "/organisationUnitGroupSets", "{'name': 'Type', 'shortName': 'Type', 'compulsory' : 'true' , 'organisationUnitGroups' :[{'id' : '" + test_orgunitgroup + "'}]}" ) );

        JsonIdentifiableObject test_ougs = GET( "/organisationUnitGroupSets/" + myougs ).content().as(JsonIdentifiableObject.class);
        //Create an orgunit, but do not add it to the compulsory group
        assertStatus( HttpStatus.CREATED,
                POST( "/organisationUnits", "{ 'name': 'Fish District', 'shortName': 'Fish District', 'openingDate' : '2022-01-01'}" ) );

        postSummary( "orgunit_compulsory_group_count" );
        JsonDataIntegritySummary summary = GET( "/dataIntegrity/orgunit_compulsory_group_count/summary" ).content()
                .as( JsonDataIntegritySummary.class );
        assertTrue( summary.exists() );
        assertTrue( summary.isObject() );
        assertEquals( 1, summary.getCount() );
        assertEquals( 100, summary.getPercentage().intValue() );
    }

    protected final void postSummary( String check )
    {
        HttpResponse trigger = POST( "/dataIntegrity/summary?checks=" + check );
        assertEquals( "http://localhost/dataIntegrity/summary?checks=" + check, trigger.location() );
        assertTrue( trigger.content().isA( JsonWebMessage.class ) );
    }
}
