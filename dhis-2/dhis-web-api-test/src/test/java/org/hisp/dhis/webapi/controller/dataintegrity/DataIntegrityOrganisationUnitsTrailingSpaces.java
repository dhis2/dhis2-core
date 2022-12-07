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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;

import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegrityDetails;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegritySummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests for orgunits whose closed dates are after their opening dates
 *
 * @author Jason P. Pickering
 */
class DataIntegrityOrganisationUnitsTrailingSpacesTest extends AbstractDataIntegrityIntegrationTest
{
    @Autowired
    private OrganisationUnitService orgUnitService;

    private OrganisationUnit unitA;

    private OrganisationUnit unitB;

    private OrganisationUnit unitC;

    private User superUser;

    final String unitAName = "Space District   ";

    final String unitBName = "Spaced Out District";

    @Test
    void testOrgUnitOpeningDateAfterClosedDate()
    {
        doInTransaction( () -> {

            unitA = createOrganisationUnit( 'A' );
            unitA.setName( unitAName );
            unitA.setShortName( unitAName );
            unitA.setOpeningDate( getDate( "2022-01-01" ) );
            orgUnitService.addOrganisationUnit( unitA );

            unitB = createOrganisationUnit( 'B' );
            unitB.setName( unitBName );
            unitB.setShortName( unitBName + "    " );
            unitB.setOpeningDate( getDate( "2022-01-01" ) );
            orgUnitService.addOrganisationUnit( unitB );

            unitC = createOrganisationUnit( 'C' );
            unitC.setName( "NoSpaceDistrict" );
            unitC.setShortName( "NoSpaceDistrict" );
            unitC.setOpeningDate( getDate( "2022-01-01" ) );
            orgUnitService.addOrganisationUnit( unitC );

            dbmsManager.flushSession();
        } );

        JsonResponse json_unitA = GET( "/organisationUnits/" + unitA.getUid() ).content().as( JsonResponse.class );
        assertEquals( json_unitA.getString( "name" ).string(), unitAName );

        postSummary( "orgunit_trailing_spaces" );
        JsonDataIntegritySummary summary = GET( "/dataIntegrity/orgunit_trailing_spaces/summary" ).content()
            .as( JsonDataIntegritySummary.class );
        assertTrue( summary.exists() );
        assertTrue( summary.isObject() );
        assertEquals( 2, summary.getCount() );
        assertEquals( 66, summary.getPercentage().intValue() );

        postDetails( "orgunit_trailing_spaces" );

        JsonDataIntegrityDetails details = GET( "/dataIntegrity/orgunit_trailing_spaces/details" ).content()
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
        orgUnitUIDs.add( unitA.getUid() );
        orgUnitUIDs.add( unitB.getUid() );

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
        GET( "/organisationUnits/gist?fields=id&headless=true" ).content().stringValues()
            .forEach( id -> DELETE( "/organisationUnits/" + id ) );
        JsonResponse response = GET( "/organisationUnits/" ).content();
        JsonArray dimensions = response.getArray( "organisationUnits" );
        assertEquals( 0, dimensions.size() );

    }
}
