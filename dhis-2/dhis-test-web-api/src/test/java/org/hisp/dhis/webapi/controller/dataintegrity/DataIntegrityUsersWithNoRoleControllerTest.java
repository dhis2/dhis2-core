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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.test.webapi.json.domain.JsonDataIntegrityDetails;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;

/**
 * Integrity check to identify users who have no user role associated with their user. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/users/users_with_no_user_role.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityUsersWithNoRoleControllerTest extends AbstractDataIntegrityIntegrationTest {

  private static final String CHECK_NAME = "users_with_no_user_role";

  private static final String DETAILS_ID_TYPE = "users";

  @Test
  void testCanFlagUserWithNoRoles() {

    // Use the service layer, as the API will refuse to create a user with no role.
    User user = new User();
    user.setUid(CodeGenerator.generateUid());
    user.setFirstName("Bobby");
    user.setSurname("Tables");
    user.setUsername(DEFAULT_USERNAME + "_no_role_" + CodeGenerator.generateUid());
    user.setPassword(DEFAULT_ADMIN_PASSWORD);
    user.setLastUpdated(new Date());
    user.setCreated(new Date());
    manager.persist(user);
    dbmsManager.clearSession();

    // Note that there is already one user which exists due to the overall test setup, thus, two
    // users in total.
    Integer userCount = userService.getUserCount();
    assertEquals(2, userCount);

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE,
        CHECK_NAME,
        100 / userCount,
        user.getUid(),
        user.getUsername(),
        "disabled:false",
        true);

    JsonDataIntegrityDetails details = getDetails(CHECK_NAME);
    JsonList<JsonDataIntegrityDetails.JsonDataIntegrityIssue> issues = details.getIssues();
    assertEquals(1, issues.size());
  }

  @Test
  void testDoNotFlagUsersWithRoles() {
    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK_NAME, true);
  }
}
