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
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.web.WebClient.HttpResponse;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Checks that a rejected change of the {@code userRoles} collection of a user does not leave part
 * of the change behind [DHIS2-21665]. Replacing and merging remove roles before they add roles, so
 * both are rejected as a whole before anything is removed.
 *
 * <p>The request has to run outside the test transaction (via {@link TestTransaction}) so that the
 * test sees what was actually committed. Like in production, the request runs in an open entity
 * manager (see {@code ConditionalOpenEntityManagerInViewFilter}). The database is emptied after
 * each test, so the committed data does not leak into other tests.
 */
@Transactional
class UserRoleCollectionTransactionTest extends DhisControllerConvenienceTest {

  /** An authority the user admin below does not hold. */
  private static final String UNHELD_AUTHORITY = "F_PERFORM_MAINTENANCE";

  @Autowired private TransactionTemplate transactionTemplate;

  @Autowired private EntityManagerFactory entityManagerFactory;

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
        inOpenEntityManager(
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
        inOpenEntityManager(
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

  private HttpResponse inOpenEntityManager(Supplier<HttpResponse> request) {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    TransactionSynchronizationManager.bindResource(
        entityManagerFactory, new EntityManagerHolder(entityManager));
    try {
      return request.get();
    } finally {
      TransactionSynchronizationManager.unbindResource(entityManagerFactory);
      EntityManagerFactoryUtils.closeEntityManager(entityManager);
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
