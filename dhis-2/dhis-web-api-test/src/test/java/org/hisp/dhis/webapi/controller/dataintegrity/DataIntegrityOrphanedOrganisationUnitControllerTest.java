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

/**
 * @author Jason P. Pickering
 */
class DataIntegrityOrphanedOrganisationUnitControllerTest extends AbstractDataIntegrityIntegrationTest
{

    String orgunitA;

    String orgunitB;

    String orgunitC;

    String check = "orgunit_orphaned";

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

        DataIntegrityPositiveTestTemplate( "orgunits", check,
            1, 33, orgunitC, "Cupcake District", null );

    }

    @Test
    void testNotOrphanedOrganisationUnits()
    {
        orgunitA = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Fish District', 'shortName': 'Fish District', 'openingDate' : '2022-01-01'}" ) );

        orgunitB = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Pizza District', 'shortName': 'Pizza District', 'openingDate' : '2022-01-01', " +
                    "'parent': {'id' : '" + orgunitA + "'}}" ) );

        DataIntegrityNegativeTestTemplate( "orgunits", check );

    }

    @Test
    void testOrphansZeroCase()
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
        deleteMetadataObject( "organisationUnits", orgunitC );
        deleteMetadataObject( "organisationUnits", orgunitB );
        deleteMetadataObject( "organisationUnits", orgunitA );

    }
}
