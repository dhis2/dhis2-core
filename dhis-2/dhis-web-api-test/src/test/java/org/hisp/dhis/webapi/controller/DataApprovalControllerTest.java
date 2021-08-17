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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonArray;
import org.hisp.dhis.webapi.json.JsonObject;
import org.hisp.dhis.webapi.json.domain.JsonDataApprovalPermissions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * Tests the {@link DataApprovalController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
public class DataApprovalControllerTest extends DhisControllerConvenienceTest
{
    private String ouId;

    private String wfId;

    @Autowired
    private PeriodService periodService;

    @Before
    public void setUp()
    {
        periodService.addPeriod( createPeriod( "202101" ) );

        ouId = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits/",
                "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}" ) );

        assertStatus( HttpStatus.NO_CONTENT,
            POST( "/users/" + getCurrentUser().getUid() + "/organisationUnits/" + ouId ) );

        String levelId = assertStatus( HttpStatus.CREATED,
            POST( "/dataApprovalLevels/", "{'name':'L1', 'orgUnitLevel': 1}" ) );

        wfId = assertStatus( HttpStatus.CREATED,
            POST( "/dataApprovalWorkflows/",
                "{'name':'W1', 'periodType':'Monthly', " +
                    "'dataApprovalLevels':[{'id':'" + levelId + "'}]}" ) );

        String dsId = assertStatus( HttpStatus.CREATED,
            POST( "/dataSets/", "{'name':'My data set', 'periodType':'Monthly', 'workflow': {'id':'" + wfId + "'}}" ) );
    }

    @Test
    public void testGetApprovalPermissions()
    {
        JsonDataApprovalPermissions permissions = GET( "/dataApprovals?ou={ou}&pe=202101&wf={wf}", ouId, wfId )
            .content( HttpStatus.OK ).as( JsonDataApprovalPermissions.class );
        assertEquals( "UNAPPROVABLE", permissions.getState() );
        assertTrue( permissions.isMayReadData() );
        assertFalse( permissions.isMayAccept() );
        assertFalse( permissions.isMayUnaccept() );
        assertFalse( permissions.isMayApprove() );
        assertFalse( permissions.isMayUnapprove() );
    }

    @Test
    public void testGetMultipleApprovalPermissions_Multiple()
    {
        JsonArray statuses = GET( "/dataApprovals/multiple?ou={ou}&pe=202101&wf={wf}", ouId, wfId )
            .content( HttpStatus.OK );
        assertEquals( 1, statuses.size() );
        JsonObject status = statuses.getObject( 0 );
        assertTrue( status.has( "wf", "pe", "ou", "aoc" ) );
        assertEquals( ouId, status.getString( "ou" ).string() );
        assertEquals( wfId, status.getString( "wf" ).string() );
        assertEquals( "202101", status.getString( "pe" ).string() );
    }

    @Test
    public void testGetMultipleApprovalPermissions_Approvals()
    {
        JsonArray statuses = GET( "/dataApprovals/approvals?ou={ou}&pe=202101&wf={wf}", ouId, wfId )
            .content( HttpStatus.OK );
        assertEquals( 1, statuses.size() );
        JsonObject status = statuses.getObject( 0 );
        assertTrue( status.has( "wf", "pe", "ou", "aoc" ) );
        assertEquals( ouId, status.getString( "ou" ).string() );
        assertEquals( wfId, status.getString( "wf" ).string() );
        assertEquals( "202101", status.getString( "pe" ).string() );
    }

    @Test
    public void testGetApprovalByCategoryOptionCombos()
    {
        JsonArray statuses = GET( "/dataApprovals/categoryOptionCombos?ou={ou}&pe=202101&wf={wf}", ouId, wfId )
            .content( HttpStatus.OK );
        assertTrue( statuses.isArray() );
        assertEquals( 0, statuses.size() );
    }
}
