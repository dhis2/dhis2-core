/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static org.hisp.dhis.web.WebClient.Body;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for the Gist API that need a real database because they use DB functions that are not
 * supported by H2.
 *
 * @author Jan Bernitt
 */
class GistPostgresControllerTest extends DhisControllerIntegrationTest {
  private String orgUnitId;

  private String userGroupId;

  private User userA;

  @BeforeEach
  void setUp() {
    userA = createUserWithAuth("userA", "ALL");

    switchContextToUser(userA);

    userGroupId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/userGroups/", "{'name':'groupX', 'users':[{'id':'" + getSuperuserUid() + "'}]}"));
    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/users/{id}?importReportMode=ERRORS",
            getSuperuserUid(),
            Body("[{'op': 'add', 'path': '/birthday', 'value': '1980-12-12'}]")));
    orgUnitId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'unitA', 'shortName':'unitA', 'openingDate':'2021-01-01'}"));
    String dataSetId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'set1', 'shortName':'set1', 'organisationUnits': [{'id':'"
                    + orgUnitId
                    + "'}], 'periodType':'Daily'}"));
  }

  @Test
  void testTransform_Pluck_Multi() {
    JsonObject gist =
        GET("/users/{uid}/userGroups/gist?fields=name,users::pluck(id,surname)", getSuperuserUid())
            .content();
    JsonArray users = gist.getArray("userGroups").getObject(0).getArray("users");
    JsonObject user0 = users.getObject(0);
    assertEquals("Surnameadmin", user0.getString("surname").string());
    assertEquals(getSuperuserUid(), user0.getString("id").string());
  }

  @Test
  void testValidation_Access_CollectionOwnerSharing() {
    setUserGroupPublicAccess("--------");
    switchToNewUser("guest");
    assertEquals(
        "User not allowed to view UserGroup " + userGroupId,
        GET("/userGroups/{id}/users/gist", userGroupId).error(HttpStatus.FORBIDDEN).getMessage());
    switchToSuperuser();
    assertStatus(HttpStatus.OK, GET("/userGroups/{id}/users/gist", userGroupId));
  }

  @Test
  void testValidation_Access_CollectionOwnerReadable() {
    setUserGroupPublicAccess("r-------");
    switchToNewUser("guest");
    assertStatus(HttpStatus.OK, GET("/userGroups/{id}/users/gist", userGroupId));
  }

  @Test
  void testValidation_Access_CollectionOwnerMissingOrHidden() {
    setUserGroupPublicAccess("--------");
    switchToNewUser("guest");
    assertEquals(
        "User not allowed to view UserGroup " + userGroupId,
        GET("/userGroups/{id}/users/gist", userGroupId).error(HttpStatus.FORBIDDEN).getMessage());
    // a missing owner is refused the same way, so the response does not reveal whether it exists
    assertEquals(
        "User not allowed to view UserGroup MissingGrp1",
        GET("/userGroups/{id}/users/gist", "MissingGrp1").error(HttpStatus.FORBIDDEN).getMessage());
  }

  @Test
  void testValidation_Access_CollectionOwnerSharedWithUser() {
    String readerId = switchToNewUser("reader").getUid();
    switchToSuperuser();
    setUserGroupSharing(
        "'users':{'" + readerId + "':{'id':'" + readerId + "','access':'r-------'}}");

    switchToNewUser("other");
    assertEquals(HttpStatus.FORBIDDEN, GET("/userGroups/{id}/users/gist", userGroupId).status());
    switchToStoredUser(readerId);
    assertStatus(HttpStatus.OK, GET("/userGroups/{id}/users/gist", userGroupId));
  }

  @Test
  void testValidation_Access_CollectionOwnerSharedWithUserGroup() {
    String readerId = switchToNewUser("member").getUid();
    switchToSuperuser();
    String readersId =
        assertStatus(
            HttpStatus.CREATED,
            POST("/userGroups/", "{'name':'readers','users':[{'id':'" + readerId + "'}]}"));
    setUserGroupSharing(
        "'userGroups':{'" + readersId + "':{'id':'" + readersId + "','access':'r-------'}}");

    switchToNewUser("outsider");
    assertEquals(HttpStatus.FORBIDDEN, GET("/userGroups/{id}/users/gist", userGroupId).status());
    switchToStoredUser(readerId);
    assertStatus(HttpStatus.OK, GET("/userGroups/{id}/users/gist", userGroupId));
  }

  /** Switches to a stored user, reloaded so that its current user group memberships apply. */
  private void switchToStoredUser(String uid) {
    manager.flush();
    manager.clear();
    switchContextToUser(userService.getUser(uid));
  }

  /** Makes the user group private except for the given user or user group accesses. */
  private void setUserGroupSharing(String accesses) {
    String owner =
        GET("/userGroups/{id}", userGroupId)
            .content()
            .getObject("sharing")
            .getString("owner")
            .string();
    assertStatus(
        HttpStatus.NO_CONTENT,
        PUT(
            "/userGroups/" + userGroupId + "/sharing",
            "{'owner':'" + owner + "','public':'--------','external':false," + accesses + "}"));
  }

  private void setUserGroupPublicAccess(String publicAccess) {
    JsonObject group = GET("/userGroups/{id}", userGroupId).content();
    String sharing =
        group
            .getObject("sharing")
            .node()
            .extract()
            .member("public")
            .replaceWith("\"" + publicAccess + "\"")
            .toString();
    assertStatus(HttpStatus.NO_CONTENT, PUT("/userGroups/" + userGroupId + "/sharing", sharing));
  }

  /** Only superusers skip sharing; a user holding any other authority sees shared objects only. */
  @Test
  void testAccess_SharingAppliesToUserWithoutAllAuthority() {
    String privateSetId = createDataSet("private set", "--------");
    String publicSetId = createDataSet("public set", "r-------");

    switchToNewUser("viewer", "Test_skipSharingCheck");

    assertEquals(0, getDataSetsWithId(privateSetId).size());
    assertEquals(1, getDataSetsWithId(publicSetId).size());
  }

  private String createDataSet(String name, String publicAccess) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/dataSets/",
            "{'name':'%s','shortName':'%s','periodType':'Monthly','sharing':{'public':'%s'}}"
                .formatted(name, name, publicAccess)));
  }

  private JsonArray getDataSetsWithId(String id) {
    return GET("/dataSets/gist?fields=id&headless=true&filter=id:eq:{id}", id).content();
  }
}
