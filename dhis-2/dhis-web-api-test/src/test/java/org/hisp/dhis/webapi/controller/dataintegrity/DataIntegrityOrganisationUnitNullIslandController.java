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

import org.hisp.dhis.web.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DataIntegrityOrganisationUnitNullIslandControllerTest extends AbstractDataIntegrityIntegrationTest
{

    private String nullIsland;

    private String notNullIsland;

    final String check = "orgunit_null_island";

    @Test
    void testOrgUnitNullIsland()
    {

        nullIsland = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Null Island', 'shortName': 'Null Island', " +
                    "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Point', 'coordinates' : [ 0.001, 0.004]} }" ) );

        notNullIsland = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Not Null Island', 'shortName': 'Null Island', " +
                    "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Point', 'coordinates' : [ 10.2, 13.2]} }" ) );

        assertHasDataIntegrityIssues( "orgunits", check, 50, nullIsland,
            "Null Island", null, true );

    }

    @Test
    void testOrgUnitNotNullIsland()
    {

        nullIsland = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Null Island', 'shortName': 'Null Island', " +
                    "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Point', 'coordinates' : [ 5,6]} }" ) );

        notNullIsland = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Not Null Island', 'shortName': 'Null Island', " +
                    "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Point', 'coordinates' : [ 10.2, 13.2]} }" ) );

        assertHasNoDataIntegrityIssues( "orgunits", check, true );

    }

    @Test
    void testOrgUnitNotNullIslandZeroCase()
    {
        assertHasNoDataIntegrityIssues( "orgunits", check, false );

    }

    @BeforeEach
    public void setUp()
    {
        deleteAllOrgUnits();

    }

    @AfterEach
    public void tearDown()
    {
        deleteMetadataObject( "organisationUnits", nullIsland );
        deleteMetadataObject( "organisationUnits", notNullIsland );

    }
}