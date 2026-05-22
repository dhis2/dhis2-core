/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpStatus.FORBIDDEN;
import static org.hisp.dhis.http.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.security.twofa.TwoFactorType;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class UserTwoFactorAuditControllerTest extends H2ControllerIntegrationTestBase {

  private User totpUser;
  private User emailUser;
  private User plainUser;

  @BeforeEach
  void setUpUsers() {
    totpUser = createUserWithTwoFactorType("totp", TwoFactorType.TOTP_ENABLED);
    emailUser = createUserWithTwoFactorType("email", TwoFactorType.EMAIL_ENABLED);
    plainUser = createUserWithTwoFactorType("plain", TwoFactorType.NOT_ENABLED);
  }

  @Test
  @DisplayName("GET /users/twoFactor/summary returns counts grouped by 2FA type")
  void testSummary_returnsCountsByType() {
    JsonObject summary = GET("/users/twoFactor/summary").content(OK);

    assertTrue(
        summary.getNumber("totalUsers").integer() >= 3,
        "summary must count at least the three seeded users");
    assertTrue(
        summary.getNumber("enabled").integer() >= 2,
        "totp + email users must be counted as enabled");
    assertTrue(
        summary.getNumber("disabled").integer() >= 1, "plain user must be counted as disabled");

    JsonObject byType = summary.getObject("byType");
    assertTrue(byType.getNumber("TOTP_ENABLED").integer() >= 1);
    assertTrue(byType.getNumber("EMAIL_ENABLED").integer() >= 1);
    assertTrue(byType.getNumber("NOT_ENABLED").integer() >= 1);

    JsonObject privileged = summary.getObject("privileged");
    assertTrue(privileged.getNumber("withAllAuthority").integer() >= 1, "admin counts as super");
  }

  @Test
  @DisplayName("GET /users/twoFactor/summary is forbidden for non-superusers")
  void testSummary_forbiddenForNonSuperuser() {
    switchToNewUser("regular");
    assertEquals(FORBIDDEN, GET("/users/twoFactor/summary").status());
  }

  @Test
  @DisplayName("GET /users/twoFactor lists all users with their 2FA type by default")
  void testList_returnsAllByDefault() {
    JsonObject body = GET("/users/twoFactor").content(OK);
    JsonList<JsonObject> users = body.getList("users", JsonObject.class);

    assertContainsUid(users, totpUser.getUid(), "TOTP_ENABLED");
    assertContainsUid(users, emailUser.getUid(), "EMAIL_ENABLED");
    assertContainsUid(users, plainUser.getUid(), "NOT_ENABLED");
  }

  @Test
  @DisplayName("GET /users/twoFactor?status=ENABLED hides users without 2FA")
  void testList_filterByStatusEnabled() {
    JsonList<JsonObject> users =
        GET("/users/twoFactor?status=ENABLED").content(OK).getList("users", JsonObject.class);

    List<String> uids = users.stream().map(u -> u.getString("id").string()).toList();
    assertTrue(uids.contains(totpUser.getUid()));
    assertTrue(uids.contains(emailUser.getUid()));
    assertTrue(uids.stream().noneMatch(plainUser.getUid()::equals));
  }

  @Test
  @DisplayName("GET /users/twoFactor?status=DISABLED returns only users without 2FA")
  void testList_filterByStatusDisabled() {
    JsonList<JsonObject> users =
        GET("/users/twoFactor?status=DISABLED").content(OK).getList("users", JsonObject.class);

    List<String> uids = users.stream().map(u -> u.getString("id").string()).toList();
    assertTrue(uids.contains(plainUser.getUid()));
    assertTrue(uids.stream().noneMatch(totpUser.getUid()::equals));
    assertTrue(uids.stream().noneMatch(emailUser.getUid()::equals));
  }

  @Test
  @DisplayName("GET /users/twoFactor?type=TOTP_ENABLED filters by 2FA type")
  void testList_filterByType() {
    JsonList<JsonObject> users =
        GET("/users/twoFactor?type=TOTP_ENABLED").content(OK).getList("users", JsonObject.class);

    List<String> uids = users.stream().map(u -> u.getString("id").string()).toList();
    assertTrue(uids.contains(totpUser.getUid()));
    assertTrue(uids.stream().noneMatch(emailUser.getUid()::equals));
    assertTrue(uids.stream().noneMatch(plainUser.getUid()::equals));
  }

  @Test
  @DisplayName("GET /users/twoFactor is forbidden for non-superusers")
  void testList_forbiddenForNonSuperuser() {
    switchToNewUser("regular");
    assertEquals(FORBIDDEN, GET("/users/twoFactor").status());
  }

  private User createUserWithTwoFactorType(String username, TwoFactorType type) {
    User user = createUserWithAuth(username);
    user.setTwoFactorType(type);
    userService.updateUser(user);
    return user;
  }

  private static void assertContainsUid(JsonList<JsonObject> users, String uid, String expectType) {
    JsonObject match =
        users.stream()
            .filter(u -> uid.equals(u.getString("id").string()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("user " + uid + " not in list"));
    assertEquals(expectType, match.getString("twoFactorType").string());
  }
}
