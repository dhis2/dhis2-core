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
import static org.junit.jupiter.api.Assertions.*;

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

class DataIntegrityMultipleRootsControllerTest extends AbstractDataIntegrityIntegrationTest
{
    @Test
    void testOrgunitMultipleRoots()
    {

        String nullIsland = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Null Island', 'shortName': 'Null Island', " +
                    "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Point', 'coordinates' : [ 0.001, 0.004]} }" ) );

        String notNullIsland = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Not Null Island', 'shortName': 'Null Island', " +
                    "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Point', 'coordinates' : [ 10.2, 13.2]} }" ) );

        this.postSummary( "orgunit_multiple_roots" );
        JsonDataIntegritySummary summary = GET( "/dataIntegrity/orgunit_multiple_roots/summary" ).content()
            .as( JsonDataIntegritySummary.class );
        assertTrue( summary.exists() );
        assertTrue( summary.isObject() );
        assertEquals( 2, summary.getCount() );
        assertEquals( 100, summary.getPercentage().intValue() );

        postDetails( "orgunit_multiple_roots" );

        JsonDataIntegrityDetails details = GET( "/dataIntegrity/orgunit_multiple_roots/details" ).content()
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
        orgUnitUIDs.add( nullIsland );
        orgUnitUIDs.add( notNullIsland );

        assertEquals( issueUIDs, orgUnitUIDs );

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
        GET( "/organisationUnits/gist?fields=id&headless=true" ).content().stringValues()
            .forEach( id -> DELETE( "/organisationUnits/" + id ) );
        JsonResponse response = GET( "/organisationUnits/" ).content();
        JsonArray dimensions = response.getArray( "organisationUnits" );
        assertEquals( 0, dimensions.size() );

    }
}