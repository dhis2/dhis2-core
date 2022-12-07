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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegrityDetails;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegritySummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Jason P. Pickering
 */
class DataIntegrityOrphanedOrganisationUnitControllerTest extends AbstractDataIntegrityIntegrationTest
{

    String orgunitA;

    String orgunitB;

    String orgunitC;

    @Test
    void testOrphanedOrganisationUnits()
    {

        orgunitA = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Fish District', 'shortName': 'Fish District', 'openingDate' : '2022-01-01'}" ) );

        orgunitB = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Pizza District', 'shortName': 'Pizza District', 'openingDate' : '2022-01-01', " +
                    "'parent': {'id' : '" + orgunitA + "'}}" ) );

        orgunitC = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Cupcake District', 'shortName': 'Cupcake District', 'openingDate' : '2022-01-01'}" ) );

        //Create an orgunit, but do not add it to the compulsory group
        postSummary( "orgunit_orphaned" );

        JsonDataIntegritySummary summary = GET( "/dataIntegrity/orgunit_orphaned/summary" ).content()
            .as( JsonDataIntegritySummary.class );
        assertTrue( summary.exists() );
        assertTrue( summary.isObject() );
        assertEquals( 1, summary.getCount() );
        assertEquals( 50, summary.getPercentage().intValue() );

        postDetails( "orgunit_orphaned" );

        JsonDataIntegrityDetails details = GET( "/dataIntegrity/orgunit_orphaned/details" ).content()
            .as( JsonDataIntegrityDetails.class );
        assertTrue( details.exists() );
        assertTrue( details.isObject() );
        JsonList<JsonDataIntegrityDetails.JsonDataIntegrityIssue> issues = details.getIssues();
        assertTrue( issues.exists() );
        assertEquals( 1, issues.size() );
        assertEquals( orgunitC, issues.get( 0 ).getId() );
        assertEquals( "Cupcake District", issues.get( 0 ).getName() );
        assertEquals( "orgunits", details.getIssuesIdType() );

    }

    private void deleteAllOrgUnits()
    {
        GET( "/organisationUnits/gist?fields=id&headless=true" ).content().stringValues()
            .forEach( id -> DELETE( "/organisationUnits/" + id ) );
        JsonResponse response = GET( "/organisationUnits/" ).content();
        JsonArray dimensions = response.getArray( "organisationUnits" );
        assertEquals( 0, dimensions.size() );
    }

    @BeforeEach
    public void setUp()
    {
        deleteAllOrgUnits();

    }

    @AfterEach
    public void tearDown()
    {
        assertStatus( HttpStatus.OK,
            DELETE( "/organisationUnits/" + orgunitC ) );
        assertStatus( HttpStatus.NOT_FOUND,
            GET( "/organisationUnits/" + orgunitC ) );

        assertStatus( HttpStatus.OK,
            DELETE( "/organisationUnits/" + orgunitB ) );
        assertStatus( HttpStatus.NOT_FOUND,
            GET( "/organisationUnits/" + orgunitB ) );

        assertStatus( HttpStatus.OK,
            DELETE( "/organisationUnits/" + orgunitA ) );
        assertStatus( HttpStatus.NOT_FOUND,
            GET( "/organisationUnits/" + orgunitA ) );
    }
}
