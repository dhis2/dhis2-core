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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the ACL filtering functionality in {@link
 * org.hisp.dhis.webapi.controller.user.MeController}. These tests require PostgreSQL because they
 * use PostgreSQL-specific JSON functions for ACL queries.
 *
 * <p>See DHIS2-20458: /api/me allows to retrieve userGroup/userRole data even when user has no read
 * access to it
 *
 * @author Morten Svanæs
 */
@Transactional
class MeControllerAclTest extends DhisControllerIntegrationTest {

  @Test
  @DisplayName("UserGroups without read access should be excluded from /api/me response")
  void testGetCurrentUser_UserGroupsWithoutReadAccessAreExcluded() {
    // Create a user group with private sharing (no public access)
    String privateGroupId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/userGroups",
                "{'name':'PrivateGroup','sharing':{'public':'--------','external':false}}"));

    // Create a user group with public read access
    String publicGroupId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/userGroups",
                "{'name':'PublicGroup','sharing':{'public':'r-------','external':false}}"));

    // Create a test user with no special authorities
    User testUser = switchToNewUser("testUserGroups");
    String testUserId = testUser.getUid();

    // As admin, add the test user to both groups
    switchToSuperuser();
    assertStatus(HttpStatus.OK, POST("/userGroups/" + privateGroupId + "/users/" + testUserId));
    assertStatus(HttpStatus.OK, POST("/userGroups/" + publicGroupId + "/users/" + testUserId));

    // Clear Hibernate session to ensure fresh data is loaded
    manager.flush();
    manager.clear();

    // Fetch fresh user from DB (to get updated group memberships) and switch to them
    User freshUser = userService.getUser(testUserId);
    switchToNewUser(freshUser);

    // Query /api/me with userGroups fields
    JsonObject response = GET("/me?fields=userGroups[id,name]").content();
    JsonArray userGroups = response.getArray("userGroups");

    // The test user should only see the public group, not the private group
    Set<String> visibleGroupIds =
        userGroups.asList(JsonObject.class).stream()
            .map(g -> g.as(JsonObject.class).getString("id").string())
            .collect(Collectors.toSet());

    assertTrue(
        visibleGroupIds.contains(publicGroupId), "User should see the public group they belong to");
    assertFalse(
        visibleGroupIds.contains(privateGroupId),
        "User should NOT see the private group without read access");
  }

  @Test
  @DisplayName("UserRoles without read access should be excluded from /api/me response")
  void testGetCurrentUser_UserRolesWithoutReadAccessAreExcluded() {
    // Create a user role with private sharing (no public access)
    String privateRoleId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/userRoles",
                "{'name':'PrivateRole','sharing':{'public':'--------','external':false}}"));

    // Create a user role with public read access
    String publicRoleId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/userRoles",
                "{'name':'PublicRole','sharing':{'public':'r-------','external':false}}"));

    // Create a test user and assign both roles
    String testUserJson =
        String.format(
            "{"
                + "'username': 'testUserRoles',"
                + "'firstName': 'Test',"
                + "'surname': 'User',"
                + "'password': 'Test1234!',"
                + "'userRoles': [{'id': '%s'}, {'id': '%s'}]"
                + "}",
            privateRoleId, publicRoleId);

    String testUserId = assertStatus(HttpStatus.CREATED, POST("/users", testUserJson));

    // Clear Hibernate session to ensure fresh data is loaded
    manager.flush();
    manager.clear();

    // Fetch fresh user from DB and switch to them
    User testUser = userService.getUser(testUserId);
    switchToNewUser(testUser);

    // Query /api/me with userRoles fields
    JsonObject response = GET("/me?fields=userRoles[id,name]").content();
    JsonArray userRoles = response.getArray("userRoles");

    // The test user should only see the public role, not the private role
    Set<String> visibleRoleIds =
        userRoles.asList(JsonObject.class).stream()
            .map(r -> r.as(JsonObject.class).getString("id").string())
            .collect(Collectors.toSet());

    assertTrue(visibleRoleIds.contains(publicRoleId), "User should see the public role they have");
    assertFalse(
        visibleRoleIds.contains(privateRoleId),
        "User should NOT see the private role without read access");
  }

  @Test
  @DisplayName("UserGroups with read access should be returned with expanded fields")
  void testGetCurrentUser_UserGroupsWithReadAccessExpandedFields() {
    // Create a user group with public read access
    String publicGroupId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/userGroups",
                "{'name':'PublicGroupExpanded','sharing':{'public':'r-------','external':false}}"));

    // Create a test user
    User testUser = switchToNewUser("testUserGroupsExpanded");
    String testUserId = testUser.getUid();

    // As admin, add the test user to the group
    switchToSuperuser();
    assertStatus(HttpStatus.OK, POST("/userGroups/" + publicGroupId + "/users/" + testUserId));

    // Clear Hibernate session to ensure fresh data is loaded
    manager.flush();
    manager.clear();

    // Fetch fresh user from DB (to get updated group memberships) and switch to them
    User freshUser = userService.getUser(testUserId);
    switchToNewUser(freshUser);

    // Query /api/me with expanded userGroups fields
    JsonObject response = GET("/me?fields=userGroups[id,name,displayName]").content();
    JsonArray userGroups = response.getArray("userGroups");

    assertEquals(1, userGroups.size(), "User should see exactly one group");
    JsonObject group = userGroups.getObject(0);
    assertEquals(publicGroupId, group.getString("id").string());
    assertEquals("PublicGroupExpanded", group.getString("name").string());
  }

  @Test
  @DisplayName("Admin user should see all userGroups regardless of sharing settings")
  void testGetCurrentUser_AdminSeesAllUserGroups() {
    // Create a user group with private sharing
    String privateGroupId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/userGroups",
                "{'name':'AdminPrivateGroup','sharing':{'public':'--------','external':false}}"));

    // Add admin to the group
    assertStatus(
        HttpStatus.OK, POST("/userGroups/" + privateGroupId + "/users/" + getSuperuserUid()));

    // Clear Hibernate session to ensure fresh data is loaded
    manager.flush();
    manager.clear();

    // Query /api/me as admin with userGroups fields
    JsonObject response = GET("/me?fields=userGroups[id,name]").content();
    JsonArray userGroups = response.getArray("userGroups");

    // Admin should see the private group because they have ALL authority
    Set<String> visibleGroupIds =
        userGroups.asList(JsonObject.class).stream()
            .map(g -> g.as(JsonObject.class).getString("id").string())
            .collect(Collectors.toSet());

    assertTrue(
        visibleGroupIds.contains(privateGroupId),
        "Admin should see all groups including private ones");
  }
}
