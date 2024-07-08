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

import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegrityDetails;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integrity check to identify users who have a data view organisation unit, but who cannot access data which they have possibly entered
 * at a higher level of the hierarchy. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/users/users_capture_ou_not_in_data_view_ou.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityUsersCaptureOrgunitNotInDataView extends AbstractDataIntegrityIntegrationTest {


    private static final String check = "users_capture_ou_not_in_data_view_ou";

    private static final String detailsIdType = "users";

    private String orgunitA_uid;
    private String orgunitB_uid;

    private String userA_uid;
    private String userB_uid;

    private String userRole_uid;

    @Test
    void testDataCatpureInDataViewHierarchy() {

        orgunitA_uid =
            assertStatus(
                HttpStatus.CREATED,
                POST(
                    "/organisationUnits",
                    "{ 'name': 'Fish District', 'shortName': 'Fish District', 'openingDate' : '2022-01-01'}"));

        orgunitB_uid =
            assertStatus(
                HttpStatus.CREATED,
                POST(
                    "/organisationUnits",
                    "{ 'name': 'Pizza District', 'shortName': 'Pizza District', 'openingDate' : '2022-01-01'}"));

        userRole_uid = assertStatus(HttpStatus.CREATED,
            POST("/userRoles", "{ 'name': 'Test role', 'authorities': ['F_DATAVALUE_ADD'] }") );

       assertStatus(
            HttpStatus.CREATED,
            POST(
                "/users",
                "{ 'username': 'bobbytables' , 'password': 'District123+', 'firstName': 'Bobby', 'surname': 'Tables', 'organisationUnits' : [{'id' : '"
                    + orgunitA_uid
                    + "'}], 'dataViewOrganisationUnits' : [{'id' : '"
                    + orgunitA_uid
                    + "'}], 'userRoles' : [{'id' : '" + userRole_uid + "'}]}") );

        userB_uid = assertStatus( HttpStatus.CREATED,
            POST(
                "/users",
                "{ 'username': 'janedoe' , 'password': 'District123+', 'firstName': 'Jane', 'surname': 'Doe', 'organisationUnits' : [{'id' : '"
                    + orgunitA_uid
                    + "'}], 'dataViewOrganisationUnits' : [{'id' : '"
                    + orgunitB_uid
                    + "'}], 'userRoles' : [{'id' : '" + userRole_uid + "'}]}") );

        assertStatus( HttpStatus.CREATED,
            POST(
                "/users",
                "{ 'username': 'tommytables' , 'password': 'District123+', 'firstName': 'Tommy', 'surname': 'Tables', 'organisationUnits' : [{'id' : '"
                    + orgunitA_uid
                    + "'}],  'userRoles' : [{'id' : '" + userRole_uid + "'}]}") );


        //Note that there are already two users which exist due to the overall test setup, thus, five users in total. Only userB should be flagged.
        assertHasDataIntegrityIssues(
            detailsIdType, check, 20, userB_uid, "janedoe", null, true);

        JsonDataIntegrityDetails details = getDetails(check);
        JsonList<JsonDataIntegrityDetails.JsonDataIntegrityIssue> issues = details.getIssues();
        assertEquals(1, issues.size());

        Set<String> detailsRefs =
            issues.stream().flatMap(issue -> issue.getRefs().stream())
                .map(JsonString::string)
                .collect(toUnmodifiableSet());

        assertTrue( detailsRefs.contains( "Fish District" ) );


    }

}
