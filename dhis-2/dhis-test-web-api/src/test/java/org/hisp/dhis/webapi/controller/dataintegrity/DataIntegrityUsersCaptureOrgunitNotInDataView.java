/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.test.webapi.json.domain.JsonDataIntegrityDetails;
import org.junit.jupiter.api.Test;

/**
 * Integrity check to identify users who have a data view organisation unit which does not have
 * access to their data entry organisation units. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/users/users_capture_ou_not_in_data_view_ou.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityUsersCaptureOrgunitNotInDataView extends AbstractDataIntegrityIntegrationTest {

  private static final String CHECK_NAME = "users_capture_ou_not_in_data_view_ou";

  private static final String DETAILS_ID_TYPE = "users";

  private String orgunitAUid;
  private String orgunitBUid;

  private String userBUid;

  private String userRoleUid;

  @Test
  void testDataCaptureUnitInDataViewHierarchy() {

    orgunitAUid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits",
                "{ 'name': 'Fish District', 'shortName': 'Fish District', 'openingDate' : '2022-01-01'}"));

    orgunitBUid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits",
                "{ 'name': 'Pizza District', 'shortName': 'Pizza District', 'openingDate' : '2022-01-01'}"));

    userRoleUid =
        assertStatus(
            HttpStatus.CREATED,
            POST("/userRoles", "{ 'name': 'Test role', 'authorities': ['F_DATAVALUE_ADD'] }"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/users",
            "{ 'username': 'bobbytables' , 'password': 'District123+', 'firstName': 'Bobby', 'surname': 'Tables', 'organisationUnits' : [{'id' : '"
                + orgunitAUid
                + "'}], 'dataViewOrganisationUnits' : [{'id' : '"
                + orgunitAUid
                + "'}], 'userRoles' : [{'id' : '"
                + userRoleUid
                + "'}]}"));

    userBUid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/users",
                "{ 'username': 'janedoe' , 'password': 'District123+', 'firstName': 'Jane', 'surname': 'Doe', 'organisationUnits' : [{'id' : '"
                    + orgunitAUid
                    + "'}], 'dataViewOrganisationUnits' : [{'id' : '"
                    + orgunitBUid
                    + "'}], 'userRoles' : [{'id' : '"
                    + userRoleUid
                    + "'}]}"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/users",
            "{ 'username': 'tommytables' , 'password': 'District123+', 'firstName': 'Tommy', 'surname': 'Tables', 'organisationUnits' : [{'id' : '"
                + orgunitAUid
                + "'}],  'userRoles' : [{'id' : '"
                + userRoleUid
                + "'}]}"));

    JsonArray users = GET("/users").content().getArray("users");
    assertEquals(4, users.size());
    // 1 user out of 4, thus 25% of users have data integrity issues
    assertHasDataIntegrityIssues(DETAILS_ID_TYPE, CHECK_NAME, 25, userBUid, "janedoe", null, true);

    JsonDataIntegrityDetails details = getDetails(CHECK_NAME);
    JsonList<JsonDataIntegrityDetails.JsonDataIntegrityIssue> issues = details.getIssues();
    assertEquals(1, issues.size());

    Set<String> detailsRefs =
        issues.stream()
            .flatMap(issue -> issue.getRefs().stream())
            .map(JsonString::string)
            .collect(toUnmodifiableSet());

    assertTrue(detailsRefs.contains("Fish District"));
  }
}
