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

import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonIdentifiableObject;
import org.hisp.dhis.webapi.json.domain.JsonUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.user.UserLookupController} API.
 *
 * @author Jan Bernitt
 */
class UserLookupControllerTest extends DhisControllerConvenienceTest {

  private String roleId;

  private User john;

  private User paul;

  private User george;

  private User ringo;

  @BeforeEach
  void setUp() {
    john = switchToNewUser("John");
    paul = switchToNewUser("Paul");
    george = switchToNewUser("George");
    ringo = switchToNewUser("Ringo");
    switchToSuperuser();
    roleId = assertStatus(HttpStatus.CREATED, POST("/userRoles", "{'name':'common'}"));
    assertStatus(HttpStatus.NO_CONTENT, POST("/userRoles/" + roleId + "/users/" + john.getUid()));
    assertStatus(HttpStatus.NO_CONTENT, POST("/userRoles/" + roleId + "/users/" + paul.getUid()));
    assertStatus(HttpStatus.NO_CONTENT, POST("/userRoles/" + roleId + "/users/" + george.getUid()));
    assertStatus(HttpStatus.NO_CONTENT, POST("/userRoles/" + roleId + "/users/" + ringo.getUid()));
  }

  /**
   * This test makes sure a user having the same role as users in the system can see those users.
   */
  @Test
  void testLookUpUsers() {
    User tester = switchToNewUser("tester");
    switchToSuperuser();
    assertStatus(HttpStatus.NO_CONTENT, POST("/userRoles/" + roleId + "/users/" + tester.getUid()));
    switchContextToUser(tester);
    JsonArray matches = GET("/userLookup?query=John").content().getArray("users");
    assertEquals(1, matches.size());
    JsonUser user = matches.get(0, JsonUser.class);
    assertEquals("FirstNameJohn", user.getFirstName());
  }

  @Test
  void testLookUpUsers_captureUnitsOnly() {
    // setup
    String ouA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'testA', 'shortName':'TA', 'openingDate':'2021-01-01'}"));
    String ouB =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'testB', 'shortName':'TB', 'openingDate':'2021-01-01'}"));

    BiConsumer<String, String> addUserToOrgUnit =
        (user, orgUnit) ->
            assertEquals(
                HttpStatus.OK,
                PATCH(
                        "/users/" + user + "?importReportMode=ERRORS",
                        "["
                            + "{'op': 'add', 'path': '/organisationUnits', 'value': [{'id':'"
                            + orgUnit
                            + "'}]}"
                            + "]")
                    .status());

    addUserToOrgUnit.accept(john.getUid(), ouA);
    addUserToOrgUnit.accept(paul.getUid(), ouA);
    addUserToOrgUnit.accept(george.getUid(), ouB);
    addUserToOrgUnit.accept(ringo.getUid(), ouB);

    // verify setup
    assertEquals(
        List.of(ouA),
        GET("/users/" + paul.getUid())
            .content()
            .as(JsonUser.class)
            .getOrganisationUnits()
            .toList(JsonIdentifiableObject::getId));

    // test
    switchContextToUser(john);
    JsonList<JsonUser> matches =
        GET("/userLookup?query=FirstName&orgUnitBoundary=DATA_CAPTURE")
            .content()
            .getArray("users")
            .asList(JsonUser.class);
    // all 4 users have "FirstName" in their first name but john can see
    // paul and himself
    assertEquals(2, matches.size());
    assertEquals(
        Set.of("John", "Paul"), matches.stream().map(JsonUser::getUsername).collect(toSet()));

    // similar
    switchContextToUser(george);
    matches =
        GET("/userLookup?query=FirstName&orgUnitBoundary=DATA_CAPTURE")
            .content()
            .getArray("users")
            .asList(JsonUser.class);
    // all 4 users have "FirstName" in their first name but george can see
    // ringo and himself
    assertEquals(2, matches.size());
    assertEquals(
        Set.of("George", "Ringo"), matches.stream().map(JsonUser::getUsername).collect(toSet()));
  }
}
