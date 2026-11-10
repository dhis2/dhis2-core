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

import static org.hisp.dhis.http.HttpAssertions.assertSeries;
import static org.hisp.dhis.http.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.twofa.TwoFactorType;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetailsChangeTracker;
import org.hisp.dhis.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Verifies that a user is marked as changed after commit when data held by the session principal
 * changes, whichever API or service wrote it: user roles, organisation units, the properties of the
 * user that decide access, and the deletion of the user. Changes that leave this data as it was
 * mark nobody.
 *
 * <p>The class is not {@code @Transactional}, so every request commits on its own. The marking runs
 * on a single-threaded executor after the commit; draining that executor before asserting makes the
 * assertions deterministic, also the ones that expect no change.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class UserSessionRefreshControllerTest extends H2ControllerIntegrationTestBase {

  @Autowired private UserDetailsChangeTracker userDetailsChangeTracker;

  @Autowired
  @Qualifier("userDetailsChangeTaskExecutor")
  private ThreadPoolTaskExecutor userDetailsChangeExecutor;

  @Autowired private TransactionTemplate transactionTemplate;

  private User user;

  private UserRole role;

  private OrganisationUnit orgUnit;

  @BeforeEach
  void setUp() {
    user = createUserWithAuth("member");
    role = createUserRole('R', "F_SESSION_REFRESH");
    userService.addUserRole(role);
    orgUnit = createOrganisationUnit('A');
    manager.save(orgUnit);
    awaitUserDetailsChanges();
  }

  @ParameterizedTest
  @ValueSource(strings = {"/users/{user}/userRoles/{role}", "/userRoles/{role}/users/{user}"})
  void addingUserRoleMarksUserAsChanged(String path) {
    String tokenBefore = changeToken();

    assertSeries(HttpStatus.Series.SUCCESSFUL, POST(url(path)));

    assertMarkedAsChanged(tokenBefore);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/users/{user}/userRoles/{role}", "/userRoles/{role}/users/{user}"})
  void removingUserRoleMarksUserAsChanged(String path) {
    POST(url("/users/{user}/userRoles/{role}")).content(OK);
    awaitUserDetailsChanges();
    String tokenBefore = changeToken();

    assertSeries(HttpStatus.Series.SUCCESSFUL, DELETE(url(path)));

    assertMarkedAsChanged(tokenBefore);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/users/{user}/organisationUnits/{orgUnit}",
        "/users/{user}/dataViewOrganisationUnits/{orgUnit}",
        "/users/{user}/teiSearchOrganisationUnits/{orgUnit}",
        "/organisationUnits/{orgUnit}/users/{user}"
      })
  void addingOrgUnitMarksUserAsChanged(String path) {
    String tokenBefore = changeToken();

    assertSeries(HttpStatus.Series.SUCCESSFUL, POST(url(path)));

    assertMarkedAsChanged(tokenBefore);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"organisationUnits", "dataViewOrganisationUnits", "teiSearchOrganisationUnits"})
  void removingOrgUnitMarksUserAsChanged(String property) {
    assignOrgUnit(property);
    String tokenBefore = changeToken();

    DELETE(url("/users/{user}/" + property + "/{orgUnit}")).content(OK);

    assertMarkedAsChanged(tokenBefore);
  }

  /**
   * The deletion handler reads the users of the organisation unit from its inverse collection, so
   * the deletion starts from an empty session, as its own request would. Every earlier request has
   * committed, so clearing the session loses nothing.
   */
  @ParameterizedTest
  @ValueSource(
      strings = {"organisationUnits", "dataViewOrganisationUnits", "teiSearchOrganisationUnits"})
  void deletingAssignedOrgUnitMarksUserAsChanged(String property) {
    assignOrgUnit(property);
    String tokenBefore = changeToken();

    entityManager.clear();
    DELETE(url("/organisationUnits/{orgUnit}")).content(OK);

    assertMarkedAsChanged(tokenBefore);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      quoteCharacter = '"',
      value = {
        "disabled | true",
        "accountExpiry | '2099-01-01T00:00:00.000'",
        "dataViewMaxOrganisationUnitLevel | 2",
        "externalAuth | true"
      })
  void changingAccessPropertyMarksUserAsChanged(String property, String value) {
    String tokenBefore = changeToken();

    PATCH(
            url("/users/{user}"),
            "[{'op':'replace','path':'/%s','value':%s}]".formatted(property, value))
        .content(OK);

    assertMarkedAsChanged(tokenBefore);
  }

  @Test
  void disablingUserThroughPutMarksUserAsChanged() {
    String tokenBefore = changeToken();

    PUT(url("/users/{user}"), userPayload("'disabled':true")).content(OK);

    assertMarkedAsChanged(tokenBefore);
  }

  @Test
  void changingTwoFactorTypeMarksUserAsChanged() {
    String tokenBefore = changeToken();

    transactionTemplate.executeWithoutResult(
        status -> {
          User persisted = userService.getUser(user.getUid());
          persisted.setTwoFactorType(TwoFactorType.TOTP_ENABLED);
          userService.updateUser(persisted);
        });

    assertMarkedAsChanged(tokenBefore);
  }

  @Test
  void deletingUserMarksUsernameAsChanged() {
    String tokenBefore = changeToken();

    DELETE(url("/users/{user}")).content(OK);

    assertMarkedAsChanged(tokenBefore);
  }

  /** The sessions of the user hold the old username, which the change tracker is keyed by. */
  @Test
  void changingUsernameMarksOldUsernameAsChanged() {
    String oldUsername = user.getUsername();
    String tokenBefore = userDetailsChangeTracker.getChangeToken(oldUsername);

    transactionTemplate.executeWithoutResult(
        status -> {
          User persisted = userService.getUser(user.getUid());
          persisted.setUsername("renamedmember");
          userService.updateUser(persisted);
        });
    awaitUserDetailsChanges();

    String tokenAfter = userDetailsChangeTracker.getChangeToken(oldUsername);
    assertNotNull(tokenAfter);
    assertNotEquals(tokenBefore, tokenAfter);
  }

  @Test
  void disablingInactiveUsersMarksThemAsChanged() {
    User active = createUserWithAuth("active");
    Date threeMonthsAgo = Date.from(ZonedDateTime.now().minusMonths(3).toInstant());
    transactionTemplate.executeWithoutResult(
        status -> {
          User persisted = userService.getUser(user.getUid());
          persisted.setLastLogin(threeMonthsAgo);
          userService.updateUser(persisted);
          User persistedActive = userService.getUser(active.getUid());
          persistedActive.setLastLogin(new Date());
          userService.updateUser(persistedActive);
        });
    awaitUserDetailsChanges();
    String tokenBefore = changeToken();
    String activeTokenBefore = userDetailsChangeTracker.getChangeToken(active.getUsername());

    assertEquals(
        1,
        userService.disableUsersInactiveSince(
            Date.from(ZonedDateTime.now().minusMonths(2).toInstant())));

    assertMarkedAsChanged(tokenBefore);
    assertEquals(activeTokenBefore, userDetailsChangeTracker.getChangeToken(active.getUsername()));
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "PATCH | [{'op':'replace','path':'/firstName','value':'Renamed'}]",
        "PATCH | [{'op':'replace','path':'/userRoles','value':[{'id':'{role}'}]}]",
        "PATCH | [{'op':'replace','path':'/organisationUnits','value':[{'id':'{orgUnit}'}]}]",
        "PUT | ",
        "POST_ROLE | "
      })
  void savingUnchangedPrincipalDataMarksNobody(String method, String body) {
    POST(url("/users/{user}/userRoles/{role}")).content(OK);
    POST(url("/users/{user}/organisationUnits/{orgUnit}")).content(OK);
    awaitUserDetailsChanges();
    String rolesJson = rolesJson();
    if ("PATCH".equals(method)) {
      body = body.replace("{'id':'{role}'}", rolesJson);
    }
    String tokenBefore = changeToken();

    switch (method) {
      case "PATCH" -> PATCH(url("/users/{user}"), url(body)).content(OK);
      case "PUT" ->
          PUT(
                  url("/users/{user}"),
                  userPayload("'organisationUnits':[{'id':'%s'}]".formatted(orgUnit.getUid())))
              .content(OK);
      case "POST_ROLE" -> POST(url("/users/{user}/userRoles/{role}")).content(OK);
      default -> throw new IllegalArgumentException(method);
    }
    awaitUserDetailsChanges();

    assertEquals(tokenBefore, changeToken());
  }

  @Test
  void lastLoginUpdateMarksNobody() {
    String tokenBefore = changeToken();

    userService.setLastLogin(user.getUsername());
    awaitUserDetailsChanges();

    assertEquals(tokenBefore, changeToken());
  }

  /** The same change marks the user when committed, and nobody when rolled back. */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void userRoleChangeMarksUserOnlyWhenCommitted(boolean commit) {
    String tokenBefore = changeToken();

    transactionTemplate.executeWithoutResult(
        status -> {
          User persisted = userService.getUser(user.getUid());
          persisted.getUserRoles().add(manager.get(UserRole.class, role.getUid()));
          userService.updateUser(persisted);
          entityManager.flush();
          if (!commit) {
            status.setRollbackOnly();
          }
        });
    awaitUserDetailsChanges();

    if (commit) {
      assertMarkedAsChanged(tokenBefore);
    } else {
      assertEquals(tokenBefore, changeToken());
    }
  }

  private void assignOrgUnit(String property) {
    POST(url("/users/{user}/" + property + "/{orgUnit}")).content(OK);
    awaitUserDetailsChanges();
  }

  /** A complete user for {@code PUT}, with the current roles and the given extra properties. */
  private String userPayload(String extraProperties) {
    return "{'id':'%s','username':'%s','firstName':'%s','surname':'%s','userRoles':[%s],%s}"
        .formatted(
            user.getUid(),
            user.getUsername(),
            user.getFirstName(),
            user.getSurname(),
            rolesJson(),
            extraProperties);
  }

  private String rolesJson() {
    return userService.getUser(user.getUid()).getUserRoles().stream()
        .map(r -> "{'id':'" + r.getUid() + "'}")
        .collect(Collectors.joining(","));
  }

  private String url(String template) {
    return template
        .replace("{user}", user.getUid())
        .replace("{role}", role.getUid())
        .replace("{orgUnit}", orgUnit.getUid());
  }

  private String changeToken() {
    return userDetailsChangeTracker.getChangeToken(user.getUsername());
  }

  private void assertMarkedAsChanged(String tokenBefore) {
    awaitUserDetailsChanges();
    String tokenAfter = changeToken();
    assertNotNull(tokenAfter);
    assertNotEquals(tokenBefore, tokenAfter);
  }

  /** The executor is single-threaded, so once this task ran all queued changes have run. */
  private void awaitUserDetailsChanges() {
    try {
      userDetailsChangeExecutor.submit(() -> {}).get(10, TimeUnit.SECONDS);
    } catch (Exception ex) {
      throw new IllegalStateException("User details changes were not processed", ex);
    }
  }
}
