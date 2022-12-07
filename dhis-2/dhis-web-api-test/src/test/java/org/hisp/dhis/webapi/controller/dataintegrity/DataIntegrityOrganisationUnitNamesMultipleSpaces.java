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

import java.util.HashSet;

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
 * Tests for orgunits which have multiple spaces in their names or shortnames
 *
 * @author Jason P. Pickering
 */
class DataIntegrityOrganisationUnitNamesMultipleSpacesTest extends AbstractDataIntegrityIntegrationTest
{

    String orgunitA;

    String orgunitB;

    String orgunitC;

    @Test
    void testOrgUnitMultipleSpaces()
    {

        orgunitA = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Space    District', 'shortName': 'Space    District', 'openingDate' : '2022-01-01'}" ) );

        orgunitB = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'TwoSpace District', 'shortName': 'Two  Space  District', 'openingDate' : '2022-01-01', " +
                    "'parent': {'id' : '" + orgunitA + "'}}" ) );

        orgunitC = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'NospaceDistrict', 'shortName': 'NospaceDistrict', 'openingDate' : '2022-01-01'}" ) );

        postSummary( "orgunit_multiple_spaces" );
        JsonDataIntegritySummary summary = GET( "/dataIntegrity/orgunit_multiple_spaces/summary" ).content()
            .as( JsonDataIntegritySummary.class );
        assertTrue( summary.exists() );
        assertTrue( summary.isObject() );
        assertEquals( 2, summary.getCount() );
        assertEquals( 66, summary.getPercentage().intValue() );

        postDetails( "orgunit_multiple_spaces" );

        JsonDataIntegrityDetails details = GET( "/dataIntegrity/orgunit_multiple_spaces/details" ).content()
            .as( JsonDataIntegrityDetails.class );
        assertTrue( details.exists() );
        assertTrue( details.isObject() );
        JsonList<JsonDataIntegrityDetails.JsonDataIntegrityIssue> issues = details.getIssues();
        assertTrue( issues.exists() );

        assertEquals( 2, issues.size() );

        HashSet<String> issueUIDs = new HashSet<String>();
        issueUIDs.add( issues.get( 0 ).getId() );
        issueUIDs.add( issues.get( 1 ).getId() );

        HashSet<String> orgUnitUIDs = new HashSet<String>();
        orgUnitUIDs.add( orgunitA );
        orgUnitUIDs.add( orgunitB );

        assertEquals( issueUIDs, orgUnitUIDs );
        assertEquals( "orgunits", details.getIssuesIdType() );
    }

    @BeforeEach
    public void setUp()
    {
        GET( "/organisationUnits/gist?fields=id&headless=true" ).content().stringValues()
            .forEach( id -> DELETE( "/organisationUnits/" + id ) );
        JsonResponse response = GET( "/organisationUnits/" ).content();
        JsonArray dimensions = response.getArray( "organisationUnits" );
        assertEquals( 0, dimensions.size() );

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
