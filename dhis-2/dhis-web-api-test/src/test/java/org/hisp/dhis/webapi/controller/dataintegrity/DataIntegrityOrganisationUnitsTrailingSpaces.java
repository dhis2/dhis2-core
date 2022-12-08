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

import java.util.Set;

import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
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

    final String check = "orgunit_trailing_spaces";

    @Test
    void DataIntegrityOrganisationUnitsTrailingSpacesTest()
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

            dbmsManager.clearSession();
        } );

        JsonResponse json_unitA = GET( "/organisationUnits/" + unitA.getUid() ).content().as( JsonResponse.class );
        assertEquals( json_unitA.getString( "name" ).string(), unitAName );

        Set orgUnitUIDs = Set.of( unitA.getUid(), unitB.getUid() );

        DataIntegrityPositiveTestTemplate( "orgunits", check, 2, 66, orgUnitUIDs, null, null );
    }

    @Test
    void orgunitsNoTrailingSpaces()
    {
        assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'NospaceDistrict', 'shortName': 'NospaceDistrict', 'openingDate' : '2022-01-01'}" ) );

        DataIntegrityNegativeTestTemplate( "orgunits", check );
    }

    @Test
    void testOrgunitsTrailingSpacesZeroCase()
    {
        DataIntegrityDivideByZeroTestTemplate( "orgunits", check );

    }

    @BeforeEach
    public void setUp()
    {

        deleteAllOrgUnits();
    }

    @AfterEach
    public void tearDown()
    {
        deleteAllOrgUnits();
    }
}
