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

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * HTTP-level regressions for system-wide JsonPatchManager non-owner collection skipping
 * (DHIS2-21852 / PR #24489). Complements {@link org.hisp.dhis.jsonpatch.JsonPatchManagerTest}.
 *
 * @author Morten (netroms)
 */
@Transactional
class JsonPatchSideEffectControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  @DisplayName("PATCH organisationUnit name keeps children")
  void testPatchOrganisationUnitNameKeepsChildren() {
    OrganisationUnit parent = createOrganisationUnit('P');
    manager.save(parent);
    OrganisationUnit child = createOrganisationUnit('C', parent);
    manager.save(child);
    manager.update(parent);

    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/organisationUnits/" + parent.getUid(),
            "[{'op':'replace','path':'/name','value':'ParentPatched'}]"));

    JsonObject body =
        GET("/organisationUnits/{id}?fields=name,children[id]", parent.getUid())
            .content(HttpStatus.OK);

    assertEquals("ParentPatched", body.getString("name").string());
    assertEquals(1, body.getArray("children").size());
    assertEquals(child.getUid(), body.getArray("children").getObject(0).getString("id").string());
  }

  @Test
  @DisplayName("PATCH userGroup name keeps owner users")
  void testPatchUserGroupNameKeepsUsers() {
    User user = makeUser("G");
    userService.addUser(user);

    String groupId =
        assertStatus(
            HttpStatus.CREATED,
            POST("/userGroups/", "{'name':'GroupSide','users':[{'id':'" + user.getUid() + "'}]}"));

    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/userGroups/" + groupId,
            "[{'op':'replace','path':'/name','value':'GroupSideRenamed'}]"));

    JsonObject body = GET("/userGroups/{id}?fields=name,users[id]", groupId).content(HttpStatus.OK);

    assertEquals("GroupSideRenamed", body.getString("name").string());
    assertEquals(1, body.getArray("users").size());
    assertEquals(user.getUid(), body.getArray("users").getObject(0).getString("id").string());
  }

  @Test
  @DisplayName("PATCH user firstName keeps userGroups")
  void testPatchUserFirstNameKeepsUserGroups() {
    UserRole role = createUserRole('F');
    manager.save(role);

    User user = makeUser("F");
    user.setEmail("first@example.org");
    user.getUserRoles().add(role);
    userService.addUser(user);

    String groupId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/userGroups/",
                "{'name':'UserFirstGroup','users':[{'id':'" + user.getUid() + "'}]}"));

    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/users/" + user.getUid() + "?importReportMode=ERRORS",
            "[{'op':'replace','path':'/firstName','value':'FirstPatched'}]"));

    JsonObject body =
        GET("/users/{id}?fields=firstName,userGroups[id]", user.getUid()).content(HttpStatus.OK);

    assertEquals("FirstPatched", body.getString("firstName").string());
    assertEquals(1, body.getArray("userGroups").size());
    assertEquals(groupId, body.getArray("userGroups").getObject(0).getString("id").string());
  }

  @Test
  @DisplayName("PATCH userRole replace /users does not change membership")
  void testPatchUserRoleUsersPathDoesNotChangeMembership() {
    UserRole role = createUserRole('S');
    User user = makeUser("R");
    userService.addUser(user);
    role.addUser(user);
    manager.save(role);

    // Non-owner path: may return OK with ERRORS_NOT_OWNER notes; membership must not drop.
    PATCH("/userRoles/" + role.getUid(), "[{'op':'replace','path':'/users','value':[]}]");

    JsonObject body = GET("/userRoles/{id}?fields=users[id]", role.getUid()).content(HttpStatus.OK);

    assertEquals(1, body.getArray("users").size());
    assertEquals(user.getUid(), body.getArray("users").getObject(0).getString("id").string());
  }
}
