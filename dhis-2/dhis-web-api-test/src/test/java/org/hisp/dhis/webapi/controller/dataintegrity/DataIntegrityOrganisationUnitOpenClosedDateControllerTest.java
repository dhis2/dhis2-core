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

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.web.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests for orgunits whose closed dates are after their opening dates
 * {@see dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/orgunits/orgunit_open_date_gt_closed_date.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityOrganisationUnitOpenClosedDateControllerTest extends AbstractDataIntegrityIntegrationTest
{
    @Autowired
    private OrganisationUnitService orgUnitService;

    private static final String check = "orgunits_openingdate_gt_closeddate";

    private static final String detailsIdType = "organisationUnits";

    @Test
    void testOrgUnitOpeningDateAfterClosedDate()
    {

        OrganisationUnit unitA = createOrganisationUnit( 'A' );
        unitA.setOpeningDate( getDate( "2022-01-01" ) );
        unitA.setClosedDate( getDate( "2020-01-01" ) );
        orgUnitService.addOrganisationUnit( unitA );

        OrganisationUnit unitB = createOrganisationUnit( 'B' );
        unitB.setOpeningDate( getDate( "2022-01-01" ) );
        unitB.setClosedDate( getDate( "2023-01-01" ) );
        orgUnitService.addOrganisationUnit( unitB );

        OrganisationUnit unitC = createOrganisationUnit( 'C' );
        unitC.setOpeningDate( getDate( "2022-01-01" ) );
        unitC.setClosedDate( null );
        orgUnitService.addOrganisationUnit( unitC );

        dbmsManager.clearSession();

        assertHasDataIntegrityIssues( detailsIdType, check, 33, unitA.getUid(), unitA.getName(), null, true );
    }

    @Test
    void testOrgunitsWithOpenClosedDates()
    {

        assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Null Island', 'shortName': 'Null Island', " +
                    "'openingDate' : '2022-01-01', 'closedDate' : '2023-02-22', 'geometry' : {'type' : 'Point', 'coordinates' : [5,6]} }" ) );

        assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{ 'name': 'Not Null Island', 'shortName': 'Null Island', " +
                    "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Point', 'coordinates' : [ 10.2, 13.2]} }" ) );

        assertHasNoDataIntegrityIssues( detailsIdType, check, true );

    }

    @Test
    void testOrgunitsOpenClosedDateRuns()
    {
        assertHasNoDataIntegrityIssues( detailsIdType, check, false );

    }

}
