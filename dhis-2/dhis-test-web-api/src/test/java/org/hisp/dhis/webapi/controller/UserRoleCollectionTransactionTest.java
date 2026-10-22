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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.web.WebClient.HttpResponse;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.SessionFactoryUtils;
import org.springframework.orm.hibernate5.SessionHolder;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Checks that a rejected change of the {@code userRoles} collection of a user does not leave part
 * of the change behind [DHIS2-21665]. Replacing and merging remove roles before they add roles, so
 * both are rejected as a whole before anything is removed. The legacy PATCH requests change the
 * persistent object before the checks run, so they are not accepted for user roles, nor for the
 * other collections of a user, at all.
 *
 * <p>The request has to run outside the test transaction (via {@link TestTransaction}) so that the
 * test sees what was actually committed. Like in production, the request runs in an open session
 * (see {@code ConditionalOpenSessionInViewFilter}). The database is emptied after each test, so the
 * committed data does not leak into other tests.
 */
@Transactional
class UserRoleCollectionTransactionTest extends DhisControllerConvenienceTest {

  /** An authority the user admin below does not hold. */
  private static final String UNHELD_AUTHORITY = "F_PERFORM_MAINTENANCE";

  @Autowired private TransactionTemplate transactionTemplate;

  @Autowired private SessionFactory sessionFactory;

  @AfterEach
  void emptyDatabase() {
    if (TestTransaction.isActive()) {
      TestTransaction.end();
    }
    transactionTemplate.execute(
        status -> {
          dbmsManager.emptyDatabase();
          return null;
        });
  }

  @Test
  void rejectedReplaceOfUserRolesKeepsExistingRoles() {
    User target = setUpTarget("replace");

    assertStatus(
        HttpStatus.FORBIDDEN,
        inOpenSession(
            () ->
                PUT(
                    "/users/" + target.getUid() + "/userRoles",
                    "{'identifiableObjects':[{'id':'"
                        + role("replace_UNISSUABLE").getUid()
                        + "'}]}")));

    assertCommittedRoles(target, "replace_GRANTABLE", "target_replace");
  }

  @Test
  void rejectedMergeOfUserRolesKeepsExistingRoles() {
    User target = setUpTarget("merge");

    assertStatus(
        HttpStatus.FORBIDDEN,
        inOpenSession(
            () ->
                POST(
                    "/users/" + target.getUid() + "/userRoles",
                    "{'additions':[{'id':'"
                        + role("merge_UNISSUABLE").getUid()
                        + "'}],'deletions':[{'id':'"
                        + role("merge_GRANTABLE").getUid()
                        + "'}]}")));

    assertCommittedRoles(target, "merge_GRANTABLE", "target_merge");
  }

  @Test
  void legacyPatchOfUserRoleIsRejected() {
    UserRole own = createUserRole("legacy_OWN");
    own.setPublicAccess("rw------");
    userService.addUserRole(own);
    switchContextToUser(createUserWithAuth("roleadmin_legacy", "F_USERROLE_PUBLIC_ADD"));
    TestTransaction.flagForCommit();
    TestTransaction.end();

    assertStatus(
        HttpStatus.CONFLICT,
        inOpenSession(
            () ->
                PATCH_OLD(
                    "/userRoles/" + own.getUid() + "/authorities",
                    "{'authorities':['" + UNHELD_AUTHORITY + "']}")));
    assertStatus(
        HttpStatus.CONFLICT,
        inOpenSession(() -> PATCH_OLD("/userRoles/" + own.getUid(), "{'name':'renamed'}")));

    TestTransaction.start();
    dbmsManager.clearSession();
    UserRole committed = userService.getUserRole(own.getUid());
    assertEquals(Set.of(), committed.getAuthorities());
    assertEquals("legacy_OWN", committed.getName());
  }

  @Test
  void legacyPatchOfUserRolesOfUserIsRejected() {
    UserRole grantable = createUserRole("legacy_GRANTABLE", "F_METADATA_IMPORT");
    userService.addUserRole(grantable);
    User target = createUserWithAuth("target_legacy", UNHELD_AUTHORITY);
    target.getUserRoles().add(grantable);
    userService.updateUser(target);
    switchContextToUser(
        createUserWithAuth(
            "useradmin_legacy", "F_USERROLE_PUBLIC_ADD", "F_USER_ADD", "F_METADATA_IMPORT"));
    TestTransaction.flagForCommit();
    TestTransaction.end();

    assertStatus(
        HttpStatus.FORBIDDEN,
        inOpenSession(
            () -> PATCH_OLD("/users/" + target.getUid() + "/userRoles", "{'userRoles':[]}")));

    assertCommittedRoles(target, "legacy_GRANTABLE", "target_legacy");
  }

  /** A user admin may not change a user having authorities they do not hold. */
  @Test
  void legacyPatchOfOrgUnitsOfMorePowerfulUserIsRejected() {
    OrganisationUnit unit = createOrganisationUnit('A');
    manager.save(unit);
    User target = createUserWithAuth("target_orgunits", UNHELD_AUTHORITY);
    target.getOrganisationUnits().add(unit);
    userService.updateUser(target);
    User userAdmin = createUserWithAuth("useradmin_orgunits", "F_USER_ADD");
    userAdmin.getOrganisationUnits().add(unit);
    userService.updateUser(userAdmin);
    switchContextToUser(userAdmin);
    TestTransaction.flagForCommit();
    TestTransaction.end();

    assertStatus(
        HttpStatus.FORBIDDEN,
        inOpenSession(
            () ->
                PATCH_OLD(
                    "/users/" + target.getUid() + "/organisationUnits",
                    "{'organisationUnits':[]}")));

    TestTransaction.start();
    dbmsManager.clearSession();
    Set<String> units =
        userService.getUser(target.getUid()).getOrganisationUnits().stream()
            .map(OrganisationUnit::getUid)
            .collect(Collectors.toSet());
    assertEquals(Set.of(unit.getUid()), units);
  }

  @Test
  void legacyWholePatchOfSuperuserIsRejected() {
    assertLegacyScalarPatch("ALL", "", "openId", HttpStatus.FORBIDDEN);
  }

  @Test
  void legacyPropertyPatchOfSuperuserIsRejected() {
    assertLegacyScalarPatch("ALL", "/openId", "openId", HttpStatus.FORBIDDEN);
  }

  @Test
  void legacyPropertyPatchOfMorePowerfulUserIsRejected() {
    assertLegacyScalarPatch(UNHELD_AUTHORITY, "/email", "email", HttpStatus.FORBIDDEN);
  }

  @Test
  void legacyWholePatchOfLessPowerfulUserIsAllowed() {
    assertLegacyScalarPatch(null, "", "email", HttpStatus.NO_CONTENT);
  }

  /**
   * The legacy whole object PATCH refuses arrays, but loads and adds a single UID given for a
   * collection, without the checks of the user import, so it may not change the roles of a user,
   * even of a user the current user may change.
   */
  @Test
  void legacyWholePatchOfUserRolesIsRejected() {
    User target = setUpTarget("whole");

    assertStatus(
        HttpStatus.CONFLICT,
        inOpenSession(
            () ->
                PATCH_OLD(
                    "/users/" + target.getUid(),
                    "{'userRoles':'" + role("whole_UNISSUABLE").getUid() + "'}")));

    assertCommittedRoles(target, "whole_GRANTABLE", "target_whole");
  }

  private void assertLegacyScalarPatch(
      String authority, String suffix, String property, HttpStatus expected) {
    User target =
        authority == null
            ? createUserWithAuth("scalar_target")
            : createUserWithAuth("scalar_target", authority);
    target.setEmail("before@example.org");
    target.setOpenId("before");
    userService.updateUser(target);
    switchContextToUser(createUserWithAuth("scalar_admin", "F_USER_ADD"));
    TestTransaction.flagForCommit();
    TestTransaction.end();
    HttpResponse response =
        inOpenSession(
            () ->
                PATCH_OLD(
                    "/users/" + target.getUid() + suffix,
                    "{'" + property + "':'after@example.org'}"));
    TestTransaction.start();
    dbmsManager.clearSession();
    User committed = userService.getUser(target.getUid());
    org.junit.jupiter.api.Assertions.assertAll(
        () -> assertStatus(expected, response),
        () -> {
          if ("ALL".equals(authority)) {
            // 2.39: the 403 carries the message of E3041, no error code
            assertEquals(
                "User `scalar_admin` is not allowed to change a user having the ALL authority",
                response.error(HttpStatus.FORBIDDEN).getMessage());
          }
        },
        () ->
            assertEquals(
                expected == HttpStatus.NO_CONTENT ? "after@example.org" : "before@example.org",
                committed.getEmail()),
        () -> assertEquals("before", committed.getOpenId()));
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

  private HttpResponse inOpenSession(Supplier<HttpResponse> request) {
    Session session = sessionFactory.openSession();
    session.setHibernateFlushMode(FlushMode.MANUAL);
    TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
    try {
      return request.get();
    } finally {
      TransactionSynchronizationManager.unbindResource(sessionFactory);
      SessionFactoryUtils.closeSession(session);
    }
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
