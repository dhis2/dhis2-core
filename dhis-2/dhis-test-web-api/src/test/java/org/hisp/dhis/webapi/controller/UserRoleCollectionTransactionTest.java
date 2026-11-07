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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks that a rejected change of the {@code userRoles} collection of a user does not leave part
 * of the change behind [DHIS2-21665]. Replacing and merging remove roles before they add roles, so
 * both are rejected as a whole before anything is removed.
 *
 * <p>The request has to run outside the test transaction (via {@link TestTransaction}) so that the
 * test sees what was actually committed. {@link TestInstance.Lifecycle#PER_CLASS} makes the
 * framework empty the database after the class, so the committed data does not leak into other
 * tests.
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRoleCollectionTransactionTest extends H2ControllerIntegrationTestBase {

  /** An authority the user admin below does not hold. */
  private static final String UNHELD_AUTHORITY = "F_PERFORM_MAINTENANCE";

  @Test
  void rejectedReplaceOfUserRolesKeepsExistingRoles() {
    User target = setUpTarget("replace");

    assertStatus(
        HttpStatus.FORBIDDEN,
        PUT(
            "/users/" + target.getUid() + "/userRoles",
            "{'identifiableObjects':[{'id':'" + role("replace_UNISSUABLE").getUid() + "'}]}"));

    assertCommittedRoles(target, "replace_GRANTABLE", "target_replace");
  }

  @Test
  void rejectedMergeOfUserRolesKeepsExistingRoles() {
    User target = setUpTarget("merge");

    assertStatus(
        HttpStatus.FORBIDDEN,
        POST(
            "/users/" + target.getUid() + "/userRoles",
            "{'additions':[{'id':'"
                + role("merge_UNISSUABLE").getUid()
                + "'}],'deletions':[{'id':'"
                + role("merge_GRANTABLE").getUid()
                + "'}]}"));

    assertCommittedRoles(target, "merge_GRANTABLE", "target_merge");
  }

  /**
   * Creates a target user holding a role the user admin may grant, and a role the user admin may
   * not grant, commits them and switches to the user admin.
   */
  private User setUpTarget(String prefix) {
    UserRole grantable = createUserRole(prefix + "_GRANTABLE", "F_METADATA_IMPORT");
    userService.addUserRole(grantable);
    UserRole unissuable = createUserRole(prefix + "_UNISSUABLE", UNHELD_AUTHORITY);
    unissuable.setPublicAccess("rw------");
    userService.addUserRole(unissuable);
    User target = createUserWithAuth("target_" + prefix);
    target.getUserRoles().add(grantable);
    userService.updateUser(target);
    User userAdmin =
        createUserWithAuth(
            "useradmin_" + prefix, "F_USERROLE_PUBLIC_ADD", "F_USER_ADD", "F_METADATA_IMPORT");
    switchContextToUser(userAdmin);

    TestTransaction.flagForCommit();
    TestTransaction.end();
    return target;
  }

  private UserRole role(String name) {
    return userService.getUserRoleByName(name);
  }

  private void assertCommittedRoles(User user, String... expected) {
    TestTransaction.start();
    dbmsManager.clearSession();
    Set<String> roles =
        userService.getUser(user.getUid()).getUserRoles().stream()
            .map(UserRole::getName)
            .collect(Collectors.toSet());
    assertEquals(Set.of(expected), roles);
  }
}
