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
import org.junit.jupiter.api.Test;

/**
 * Checks for organisation units which are part of multiple organisation unit
 * groups within the same group set.
 * {@see dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/orgunits/orgunits_excess_group_memberships.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityOrganisationUnitExcessGroupsControllerTest extends AbstractDataIntegrityIntegrationTest
{

    private String orgunitA;

    private String orgunitB;

    private String testOrgUnitGroupA;

    private String testOrgUnitGroupB;

    private static final String check = "orgunit_group_sets_excess_groups";

    private static final String detailsIdType = "organisationUnits";

    @Test
    void testOrganisationUnitInMultipleGroupSetGroups()
    {

        orgunitA = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Fish District', 'shortName': 'Fish District', 'openingDate' : '2022-01-01'}" ) );

        orgunitB = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Pizza District', 'shortName': 'Pizza District', 'openingDate' : '2022-01-01'}" ) );

        // Create an orgunit group
        testOrgUnitGroupA = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnitGroups",
                "{'name': 'Type A', 'shortName': 'Type A', 'organisationUnits' : [{'id' : '" +
                    orgunitA + "'}, {'id' : '" + orgunitB + "'}]}" ) );

        testOrgUnitGroupB = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnitGroups",
                "{'name': 'Type B', 'shortName': 'Type B', 'organisationUnits' : [{'id' : '" + orgunitB
                    + "'}]}" ) );

        // Add it to a group set
        assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnitGroupSets",
                "{'name': 'Type', 'shortName': 'Type', 'compulsory' : 'true' , " +
                    "'organisationUnitGroups' :[{'id' : '"
                    + testOrgUnitGroupA + "'}, {'id' : '" + testOrgUnitGroupB + "'}]}" ) );

        assertHasDataIntegrityIssues( detailsIdType, check, 50, orgunitB, "Pizza District", "Type", true );

    }

    @Test
    void testOrganisationUnitNotInMultipleGroupSetGroups()
    {

        orgunitA = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Fish District', 'shortName': 'Fish District', 'openingDate' : '2022-01-01'}" ) );

        orgunitB = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Pizza District', 'shortName': 'Pizza District', 'openingDate' : '2022-01-01'}" ) );

        // Create an orgunit group
        testOrgUnitGroupA = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnitGroups",
                "{'name': 'Type A', 'shortName': 'Type A', 'organisationUnits' : [{'id' : '" +
                    orgunitA + "'}]}" ) );

        testOrgUnitGroupB = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnitGroups",
                "{'name': 'Type B', 'shortName': 'Type B', 'organisationUnits' : [{'id' : '" + orgunitB
                    + "'}]}" ) );

        // Add it to a group set
        assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnitGroupSets",
                "{'name': 'Type', 'shortName': 'Type', 'compulsory' : 'true' , " +
                    "'organisationUnitGroups' :[{'id' : '"
                    + testOrgUnitGroupA + "'}, {'id' : '" + testOrgUnitGroupB + "'}]}" ) );

        assertHasNoDataIntegrityIssues( detailsIdType, check, true );

    }

    @Test
    void testOrganisationMultipleGroupsInGroupSetDivideByZero()
    {
        assertHasNoDataIntegrityIssues( detailsIdType, check, false );
    }

}
