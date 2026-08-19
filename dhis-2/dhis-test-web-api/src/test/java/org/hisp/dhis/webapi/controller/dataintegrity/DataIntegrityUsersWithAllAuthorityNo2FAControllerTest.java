/*
 * Copyright (c) 2004-2025, University of Oslo
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

import java.util.Set;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;

/**
 * Integrity check to identify users who have the ALL authority but do not have two-factor
 * authentication enabled. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/users/users_with_all_authority_no_2fa.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityUsersWithAllAuthorityNo2FAControllerTest
    extends AbstractDataIntegrityIntegrationTest {

  private static final String CHECK_NAME = "users_with_all_authority_no_2fa";

  private static final String DETAILS_ID_TYPE = "users";

  @Test
  void testUsersWithAllAuthorityAndNo2FAAreFlagged() {

    String orgUnitUid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits",
                "{ 'name': 'Fish District', 'shortName': 'Fish District', 'openingDate' : '2022-01-01'}"));

    String userRoleUid =
        assertStatus(
            HttpStatus.CREATED,
            POST("/userRoles", "{ 'name': 'ALL role', 'authorities': ['ALL'] }"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/users",
            "{ 'username': 'bobbytables', 'password': 'District123+', 'firstName': 'Bobby',"
                + " 'surname': 'Tables', 'organisationUnits': [{'id': '"
                + orgUnitUid
                + "'}], 'dataViewOrganisationUnits': [{'id': '"
                + orgUnitUid
                + "'}], 'userRoles': [{'id': '"
                + userRoleUid
                + "'}]}"));

    // Both the pre-existing admin user and bobbytables have ALL authority with no 2FA.
    User admin = userService.getUserByUsername("admin");
    User bobbytables = userService.getUserByUsername("bobbytables");

    // 2 out of 2 total users = 100%
    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE,
        CHECK_NAME,
        100,
        Set.of(admin.getUid(), bobbytables.getUid()),
        Set.of(admin.getUsername(), bobbytables.getUsername()),
        Set.of("Active"),
        true);
  }

  @Test
  void testUsersWithAllAuthorityAnd2FAEnabledAreNotFlagged() {

    // Give the admin user (who has ALL authority) a non-null secret to simulate 2FA being enabled.
    User admin = userService.getUserByUsername("admin");
    admin.setSecret("JBSWY3DPEHPK3PXP");
    userService.updateUser(admin);
    manager.flush();
    manager.clear();

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK_NAME, true);
  }
}
