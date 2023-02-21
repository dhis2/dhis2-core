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

import java.util.Set;

import org.hisp.dhis.web.HttpStatus;
import org.junit.jupiter.api.Test;

/**
 * Test for multiple roots in the organisation unit hierarchy.
 * {@see dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/orgunits/orgunits_multiple_roots.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityOrganisationUnitsMultipleRootsControllerTest extends AbstractDataIntegrityIntegrationTest
{
    private static final String check = "orgunits_multiple_roots";

    private static final String detailsIdType = "organisationUnits";

    private String nullIsland;

    private String notNullIsland;

    @Test
    void testOrgunitMultipleRoots()
    {

        nullIsland = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Null Island', 'shortName': 'Null Island', " +
                    "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Point', 'coordinates' : [ 0.001, 0.004]} }" ) );

        notNullIsland = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Not Null Island', 'shortName': 'Null Island', " +
                    "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Point', 'coordinates' : [ 10.2, 13.2]} }" ) );

        Set<String> orgUnitUIDs = Set.of();
        if ( nullIsland != null && notNullIsland != null )
        {
            orgUnitUIDs = Set.of( nullIsland, notNullIsland );
        }
        assertHasDataIntegrityIssues( detailsIdType, check, 100, orgUnitUIDs, Set.of(), Set.of(),
            true );

    }

    @Test
    void testOrgunitNoMultipleRoots()
    {

        nullIsland = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Null Island', 'shortName': 'Null Island', " +
                    "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Point', 'coordinates' : [ 0.001, 0.004]} }" ) );

        notNullIsland = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Not Null Island', 'shortName': 'Null Island', " +
                    "'parent' : {'id': '" + nullIsland + "'}, " +
                    "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Point', 'coordinates' : [ 10.2, 13.2]} }" ) );

        assertHasNoDataIntegrityIssues( detailsIdType, check, true );

    }

    @Test
    void testOrgUnitsMultipleRootsRuns()
    {
        assertHasNoDataIntegrityIssues( detailsIdType, check, false );
    }

}