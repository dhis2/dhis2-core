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
import static org.hisp.dhis.http.HttpStatus.CREATED;
import static org.hisp.dhis.http.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.TimeUnit;
import org.hisp.dhis.http.HttpClientAdapter.HttpResponse;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserDetailsChangeTracker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.session.SessionRegistry;

/**
 * Verifies that users who join or leave a user group are marked as changed after the change has
 * committed, whichever API changed the membership, so that their sessions refresh their principal
 * instead of being logged out. Members whose membership did not change are left alone.
 *
 * <p>The class is not {@code @Transactional}, so every request commits on its own. The marking runs
 * on a single-threaded executor after the commit; draining that executor before asserting makes the
 * assertions deterministic, also the ones that expect no change.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class UserGroupSessionRefreshControllerTest extends H2ControllerIntegrationTestBase {

  private static final String SESSION_ID = "memberSession";

  @Autowired private SessionRegistry sessionRegistry;

  @Autowired private UserDetailsChangeTracker userDetailsChangeTracker;

  @Autowired
  @Qualifier("userDetailsChangeTaskExecutor")
  private ThreadPoolTaskExecutor userDetailsChangeExecutor;

  private User member;

  private UserDetails memberPrincipal;

  private String groupId;

  @BeforeEach
  void setUp() {
    member = createUserWithAuth("member");
    memberPrincipal = userService.createUserDetails(member);
    groupId = assertStatus(CREATED, POST("/userGroups", "{'name':'Group'}"));
    sessionRegistry.registerNewSession(SESSION_ID, memberPrincipal);
  }

  @AfterEach
  void tearDown() {
    sessionRegistry.removeSessionInformation(SESSION_ID);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "PATCH | /users/{user} | [{'op':'add','path':'/userGroups','value':[{'id':'{group}'}]}]",
        "POST | /userGroups/{group}/users/{user} | ",
        "POST | /users/{user}/userGroups/{group} | ",
        "PUT | /userGroups/{group} | {'name':'Group','users':[{'id':'{user}'}]}"
      })
  void joiningUserGroupMarksMemberAsChanged(String method, String path, String body) {
    String tokenBefore = changeToken();

    request(method, path, body).content(OK);
    awaitUserDetailsChanges();

    assertMarkedAsChangedWithSessionKept(tokenBefore);
  }

  @Test
  void creatingUserGroupWithMemberMarksMemberAsChanged() {
    String tokenBefore = changeToken();

    assertStatus(
        CREATED,
        POST("/userGroups", "{'name':'Other','users':[{'id':'" + member.getUid() + "'}]}"));
    awaitUserDetailsChanges();

    assertMarkedAsChangedWithSessionKept(tokenBefore);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "PATCH | /users/{user} | [{'op':'add','path':'/userGroups','value':[]}]",
        "DELETE | /userGroups/{group}/users/{user} | ",
        "PUT | /userGroups/{group} | {'name':'Group','users':[]}"
      })
  void leavingUserGroupMarksMemberAsChanged(String method, String path, String body) {
    joinGroup();
    String tokenBefore = changeToken();

    request(method, path, body).content(OK);
    awaitUserDetailsChanges();

    assertMarkedAsChangedWithSessionKept(tokenBefore);
  }

  @Test
  void updatingUserGroupWithUnchangedMembersDoesNotMarkMember() {
    joinGroup();
    String tokenBefore = changeToken();

    PUT("/userGroups/" + groupId, "{'name':'Renamed','users':[{'id':'" + member.getUid() + "'}]}")
        .content(OK);
    awaitUserDetailsChanges();

    assertEquals(tokenBefore, changeToken());
  }

  private void joinGroup() {
    POST("/userGroups/" + groupId + "/users/" + member.getUid()).content(OK);
    awaitUserDetailsChanges();
  }

  private String changeToken() {
    return userDetailsChangeTracker.getChangeToken(member.getUsername());
  }

  private void assertMarkedAsChangedWithSessionKept(String tokenBefore) {
    String tokenAfter = changeToken();
    assertNotNull(tokenAfter);
    assertNotEquals(tokenBefore, tokenAfter);
    assertFalse(sessionRegistry.getAllSessions(memberPrincipal, false).isEmpty());
  }

  /** The executor is single-threaded, so once this task ran all queued changes have run. */
  private void awaitUserDetailsChanges() {
    try {
      userDetailsChangeExecutor.submit(() -> {}).get(10, TimeUnit.SECONDS);
    } catch (Exception ex) {
      throw new IllegalStateException("User details changes were not processed", ex);
    }
  }

  private HttpResponse request(String method, String path, String body) {
    String url = path.replace("{user}", member.getUid()).replace("{group}", groupId);
    String json =
        body == null ? null : body.replace("{user}", member.getUid()).replace("{group}", groupId);
    return switch (method) {
      case "POST" -> json == null ? POST(url) : POST(url, json);
      case "PUT" -> PUT(url, json);
      case "PATCH" -> PATCH(url, json);
      case "DELETE" -> DELETE(url);
      default -> throw new IllegalArgumentException(method);
    };
  }
}
