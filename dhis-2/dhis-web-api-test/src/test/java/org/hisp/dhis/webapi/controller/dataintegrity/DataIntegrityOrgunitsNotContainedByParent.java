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

import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegrityDetails;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegritySummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Checks for organisation units which have point coordinates which are not
 * contained by their parent organisation unit. This only applies to situations
 * where the parent has geometry of type Polygon or Multipolygon.
 *
 * @author Jason P. Pickering
 */
class DataIntegrityOrganisationUnitsNotContainedByParentControllerTest extends AbstractDataIntegrityIntegrationTest
{

    String clinicA;

    String clinicB;

    String districtA;

    String check = "organisation_units_not_contained_by_parent";

    @Test
    void testOrgunitsNotContainedByParent()
    {

        districtA = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'District A', 'shortName': 'District A', " +
                    "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Polygon', 'coordinates' : [[[0,0],[3,0],[3,3],[0,3],[0,0]]]} }" ) );

        clinicA = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Clinic A', 'shortName': 'Clinic A', " +
                    "'parent': {'id' : '" + districtA + "'}, " +
                    "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Point', 'coordinates' : [1, 1]} }" ) );

        clinicB = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Clinic B', 'shortName': 'Clinic B', " +
                    "'parent': {'id' : '" + districtA + "'}, " +
                    "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Point', 'coordinates' : [5, 5]} }" ) );

        organisationUnitPositiveTestTemplate( check, 1,
            50, clinicB, "Clinic B", null );

    }

    @Test
    void testOrgunitsContainedByParent()
    {

        districtA = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'District A', 'shortName': 'District A', " +
                    "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Polygon', 'coordinates' : [[[0,0],[3,0],[3,3],[0,3],[0,0]]]} }" ) );

        clinicA = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Clinic A', 'shortName': 'Clinic A', " +
                    "'parent': {'id' : '" + districtA + "'}, " +
                    "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Point', 'coordinates' : [1, 1]} }" ) );

        clinicB = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Clinic B', 'shortName': 'Clinic B', " +
                    "'parent': {'id' : '" + districtA + "'}, " +
                    "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Point', 'coordinates' : [2, 2]} }" ) );

        //Create an orgunit, but do not add it to the compulsory group
        postSummary( "organisation_units_not_contained_by_parent" );

        JsonDataIntegritySummary summary = GET( "/dataIntegrity/organisation_units_not_contained_by_parent/summary" )
            .content()
            .as( JsonDataIntegritySummary.class );
        assertTrue( summary.exists() );
        assertTrue( summary.isObject() );
        assertEquals( 0, summary.getCount() );
        assertEquals( 0, summary.getPercentage().intValue() );

        postDetails( "organisation_units_not_contained_by_parent" );

        JsonDataIntegrityDetails details = GET( "/dataIntegrity/organisation_units_not_contained_by_parent/details" )
            .content()
            .as( JsonDataIntegrityDetails.class );
        assertTrue( details.exists() );
        assertTrue( details.isObject() );
        JsonList<JsonDataIntegrityDetails.JsonDataIntegrityIssue> issues = details.getIssues();
        assertTrue( issues.exists() );
        assertEquals( 0, issues.size() );

    }

    @Test
    void testOrgunitsContainedByParentDivideByZero()
    {

        postSummary( "organisation_units_not_contained_by_parent" );

        JsonDataIntegritySummary summary = GET( "/dataIntegrity/organisation_units_not_contained_by_parent/summary" )
            .content()
            .as( JsonDataIntegritySummary.class );
        assertTrue( summary.exists() );
        assertTrue( summary.isObject() );
        assertEquals( 0, summary.getCount() );
        assertNull( summary.getPercentage() );

        postDetails( "organisation_units_not_contained_by_parent" );

        JsonDataIntegrityDetails details = GET( "/dataIntegrity/organisation_units_not_contained_by_parent/details" )
            .content()
            .as( JsonDataIntegrityDetails.class );
        assertTrue( details.exists() );
        assertTrue( details.isObject() );
        JsonList<JsonDataIntegrityDetails.JsonDataIntegrityIssue> issues = details.getIssues();
        assertTrue( issues.exists() );
        assertEquals( 0, issues.size() );

    }

    @BeforeEach
    public void setUp()
    {
        deleteAllOrgUnits();

    }

    @AfterEach
    public void tearDown()
        throws Exception
    {
        DeleteMetadataObject( "organisationUnits", clinicA );
        DeleteMetadataObject( "organisationUnits", clinicB );
        DeleteMetadataObject( "organisationUnits", districtA );
    }
}
