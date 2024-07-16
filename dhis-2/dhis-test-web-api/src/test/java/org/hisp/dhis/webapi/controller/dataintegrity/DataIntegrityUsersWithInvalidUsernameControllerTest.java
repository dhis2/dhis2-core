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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegrityDetails;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegritySummary;
import org.junit.jupiter.api.Test;

/**
 * Integrity check to identify users who have invalid usernames.  {@see
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

    // Use the service layer, as the API will refuse to create a user with an invalid username.
    // Usernames should not start or end with _
    // Should not start with an underscore
    User userA = createUser("_bobby_tables_");
    // Non-ASCII characters are not allowed
    User userB = createUser("måns_tables");
    // At least four characters
    User userC = createUser("foo");
    // No consecutive underscores
    User userD = createUser("foo__bar");
    // Should not end with an underscore
    User userE = createUser("foo_");

    dbmsManager.clearSession();

    // Note that there are already two users which exist due to the overall test setup,
    // thus, four users in total.
    postSummary(CHECK_NAME);
    postDetails(CHECK_NAME);

    JsonDataIntegrityDetails details = getDetails(CHECK_NAME);
    JsonList<JsonDataIntegrityDetails.JsonDataIntegrityIssue> issues = details.getIssues();
    Set<String> badUsernames =
        issues.stream()
            .map(JsonDataIntegrityDetails.JsonDataIntegrityIssue::getName)
            .collect(Collectors.toUnmodifiableSet());
    Set<String> bad_usernames =
        Set.of(
            userA.getUsername(),
            userB.getUsername(),
            userC.getUsername(),
            userD.getUsername(),
            userE.getUsername());
    assertEquals(bad_usernames, badUsernames);
    assertEquals(DETAILS_ID_TYPE, details.getIssuesIdType());
    assertEquals(CHECK_NAME, details.getName());

    // There are already two existing users as part of the test setup
    JsonDataIntegritySummary summary = getSummary(CHECK_NAME);
    assertEquals(71, summary.getPercentage().intValue());
    assertEquals(bad_usernames.size(), summary.getCount());
  }

  @Test
  void testDoNotFlagUsersWithValidUserNames() {

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK_NAME, true);
  }

  private User createUser(String username) {
    User user = new User();
    user.setUid(CodeGenerator.generateUid());
    user.setFirstName("Bobby");
    user.setSurname("Tables");
    user.setUsername(username);
    user.setPassword(DEFAULT_ADMIN_PASSWORD);
    user.setLastUpdated(new Date());
    user.setCreated(new Date());
    manager.persist(user);
    return user;
  }
}
