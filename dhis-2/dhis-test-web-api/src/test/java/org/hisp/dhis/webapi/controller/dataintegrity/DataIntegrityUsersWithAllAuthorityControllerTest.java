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

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;

/**
 * Integrity check to identify users who have the ALL authority. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/users/users_with_all_authority.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityUsersWithAllAuthorityControllerTest
    extends AbstractDataIntegrityIntegrationTest {

  private static final String CHECK_NAME = "users_with_all_authority";

  private static final String DETAILS_ID_TYPE = "users";

  @Test
  void testCanIdentifyUsersWithAllAuthority() {

    String orgunitAUid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits",
                "{ 'name': 'Fish District', 'shortName': 'Fish District', 'openingDate' : '2022-01-01'}"));

    String userRoleUidA =
        assertStatus(
            HttpStatus.CREATED,
            POST("/userRoles", "{ 'name': 'ALL role', 'authorities': ['ALL'] }"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/users",
            "{ 'username': 'bobbytables' , 'password': 'District123+', 'firstName': 'Bobby', 'surname': 'Tables', 'organisationUnits' : [{'id' : '"
                + orgunitAUid
                + "'}], 'dataViewOrganisationUnits' : [{'id' : '"
                + orgunitAUid
                + "'}], 'userRoles' : [{'id' : '"
                + userRoleUidA
                + "'}]}"));

    // Note that there is already one user which exists due to the overall test setup, thus, two
    // users in total.
    List<User> allUsers = userService.getAllUsers();
    assertEquals(2, allUsers.size());
    Set<String> userNames = new HashSet<>();
    for (User user : allUsers) {
      userNames.add(user.getUsername());
    }
    Set<String> userUids = new HashSet<>();
    for (User user : allUsers) {
      userUids.add(user.getUid());
    }
    Set<String> userComments = new HashSet<>();
    // Each user should be active, so just create a set of "Active" for each user
    allUsers.forEach(user -> userComments.add("Active"));

    // Add a non-ALL authority user

    String userRoleUidB =
        assertStatus(
            HttpStatus.CREATED,
            POST("/userRoles", "{ 'name': 'Not all role', 'authorities': ['F_DATAVALUE_ADD'] }"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/users",
            "{ 'username': 'bobbytables2' , 'password': 'District123+', 'firstName': 'Bobby', 'surname': 'Tables', 'organisationUnits' : [{'id' : '"
                + orgunitAUid
                + "'}], 'dataViewOrganisationUnits' : [{'id' : '"
                + orgunitAUid
                + "'}], 'userRoles' : [{'id' : '"
                + userRoleUidB
                + "'}]}"));

    // Note the expected percentage is 2/3 = 66%
    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE, CHECK_NAME, 66, userUids, userNames, userComments, true);
  }
}
