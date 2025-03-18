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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.test.webapi.json.domain.JsonDataIntegrityDetails;
import org.hisp.dhis.test.webapi.json.domain.JsonDataIntegritySummary;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;

/**
 * Integrity check to identify users who have invalid usernames. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/users/users_with_invalid_usernames.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityUsersWithInvalidUsernameControllerTest
    extends AbstractDataIntegrityIntegrationTest {

  private static final String CHECK_NAME = "users_with_invalid_usernames";

  private static final String DETAILS_ID_TYPE = "users";

  @Test
  void testCanFlagUsersWithInvalidUsername() {

    Set<String> badUsers =
        Set.of(
            "_foo", // Leading underscore
            "måns", // Non-ASCII character
            "foo", // Too short
            "foo__bar", // Double underscore
            "foo_" // Trailing underscore
            );

    badUsers.forEach(this::createUser);
    dbmsManager.clearSession();

    postSummary(CHECK_NAME);
    postDetails(CHECK_NAME);

    JsonDataIntegrityDetails details = getDetails(CHECK_NAME);
    JsonList<JsonDataIntegrityDetails.JsonDataIntegrityIssue> issues = details.getIssues();
    Set<String> badUsernames =
        issues.stream()
            .map(JsonDataIntegrityDetails.JsonDataIntegrityIssue::getName)
            .collect(Collectors.toUnmodifiableSet());

    assertEquals(badUsers, badUsernames);
    assertEquals(DETAILS_ID_TYPE, details.getIssuesIdType());
    assertEquals(CHECK_NAME, details.getName());

    // Note that there is already one user which is part of the test class setup.
    JsonDataIntegritySummary summary = getSummary(CHECK_NAME);
    assertTrue(
        almostEqual(
            (double) 100 * badUsernames.size() / (userService.getUserCount()),
            summary.getPercentage().doubleValue(),
            0.1));
    assertEquals(badUsernames.size(), summary.getCount());
  }

  @Test
  void testDoNotFlagUsersWithValidUserNames() {

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK_NAME, true);
  }

  private void createUser(String username) {
    User user = new User();
    user.setUid(CodeGenerator.generateUid());
    user.setFirstName("Bobby");
    user.setSurname("Tables");
    user.setUsername(username);
    user.setPassword(DEFAULT_ADMIN_PASSWORD);
    user.setLastUpdated(new Date());
    user.setCreated(new Date());
    manager.persist(user);
  }

  private boolean almostEqual(double a, double b, double epsilon) {
    return Math.abs(a - b) < epsilon;
  }
}
