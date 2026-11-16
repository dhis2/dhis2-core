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

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * {@code /api/me} must list a user group or user role that the user can read only through a user
 * group the user just joined, also while this node still caches the old user groups of the user for
 * the sharing filter of queries. That is the case when another node of a cluster made the change:
 * only the node that made it clears that cache. The test joins the groups with plain SQL, which
 * leaves the cache of this node as such a change does, and turns the caches on, which tests
 * otherwise create with size zero. Runs on Postgres because the sharing filter uses Postgres JSON
 * functions.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ActiveProfiles("cache-test")
class MeControllerStaleGroupInfoTest extends PostgresControllerIntegrationTestBase {

  private static final String FIELDS = "/me?fields=userGroups[id],userRoles[id]";

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void meListsGroupsAndRolesReadableThroughANewGroupWhileTheGroupInfoIsCached() {
    String publicGroupId =
        assertStatus(
            HttpStatus.CREATED,
            POST("/userGroups", "{'name':'Public group','sharing':{'public':'r-------'}}"));
    // readable only by its members
    String membersGroupId =
        assertStatus(
            HttpStatus.CREATED,
            POST("/userGroups", "{'name':'Members group','sharing':{'public':'--------'}}"));
    assertStatus(
        HttpStatus.OK,
        POST(
            "/sharing?type=userGroup&id=" + membersGroupId,
            "{'object':{'publicAccess':'--------','userGroupAccesses':[{'id':'"
                + membersGroupId
                + "','access':'r-------'}]}}"));
    // readable only by the members of the members group
    String membersRoleId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/userRoles",
                "{'name':'Members role','sharing':{'public':'--------','userGroups':{'"
                    + membersGroupId
                    + "':{'id':'"
                    + membersGroupId
                    + "','access':'r-------'}}}}"));
    // readable by nobody but its owner
    String privateGroupId =
        assertStatus(
            HttpStatus.CREATED,
            POST("/userGroups", "{'name':'Private group','sharing':{'public':'--------'}}"));
    String privateRoleId =
        assertStatus(
            HttpStatus.CREATED,
            POST("/userRoles", "{'name':'Private role','sharing':{'public':'--------'}}"));

    User user = switchToNewUser("stalegroupinfouser");
    switchToAdminUser();
    assertStatus(HttpStatus.OK, POST("/userGroups/" + publicGroupId + "/users/" + user.getUid()));
    switchToNewUser(userService.getUser(user.getUid()));
    // reads the user groups of the user for the sharing filter once, which caches them
    JsonObject before = GET(FIELDS).content(HttpStatus.OK);
    assertEquals(Set.of(publicGroupId), ids(before, "userGroups"));
    Set<String> rolesBefore = ids(before, "userRoles");

    addMember("usergroupmembers", "usergroup", "usergroupid", membersGroupId, user);
    addMember("usergroupmembers", "usergroup", "usergroupid", privateGroupId, user);
    addMember("userrolemembers", "userrole", "userroleid", membersRoleId, user);
    addMember("userrolemembers", "userrole", "userroleid", privateRoleId, user);

    // the private group and role are not readable, so they are not listed
    JsonObject after = GET(FIELDS).content(HttpStatus.OK);
    assertEquals(Set.of(publicGroupId, membersGroupId), ids(after, "userGroups"));
    Set<String> expectedRoles = new HashSet<>(rolesBefore);
    expectedRoles.add(membersRoleId);
    assertEquals(expectedRoles, ids(after, "userRoles"));
  }

  private void addMember(String table, String target, String targetId, String uid, User user) {
    jdbcTemplate.update(
        "insert into %s (%s, userid) select t.%s, u.userinfoid from %s t, userinfo u"
                .formatted(table, targetId, targetId, target)
            + " where t.uid = ? and u.uid = ?",
        uid,
        user.getUid());
  }

  private static Set<String> ids(JsonObject me, String property) {
    return me.getArray(property).stream()
        .map(item -> item.as(JsonObject.class).getString("id").string())
        .collect(Collectors.toSet());
  }
}
