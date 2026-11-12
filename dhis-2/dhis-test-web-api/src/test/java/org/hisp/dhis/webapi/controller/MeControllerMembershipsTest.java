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

import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * {@code /api/me} must list the user groups, user roles and authorities of the user as stored, also
 * when the second-level cache of this node still holds the old memberships. That is the case when
 * another node of a cluster made the change: the cache invalidation it publishes does not evict
 * every collection that changed. The test changes the rows with plain SQL, which leaves this node's
 * second-level cache as such a change does. Runs on Postgres because the H2 test setup disables the
 * second-level cache.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class MeControllerMembershipsTest extends PostgresControllerIntegrationTestBase {

  private static final String AUTHORITY = "F_ME_MEMBERSHIP_CHECK";

  private static final String FIELDS = "/me?fields=userGroups[id],userRoles[id],authorities";

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void meFollowsMembershipsChangedPastTheSecondLevelCache() {
    String groupId =
        assertStatus(
            HttpStatus.CREATED,
            POST("/userGroups", "{'name':'Me group','sharing':{'public':'r-------'}}"));
    String roleId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/userRoles",
                "{'name':'Me role','authorities':['"
                    + AUTHORITY
                    + "'],'sharing':{'public':'r-------'}}"));
    User user = switchToNewUser("memembershipuser");
    // reads the memberships once, which puts them into the second-level cache
    assertMe(false, groupId, roleId);

    jdbcTemplate.update(
        "insert into usergroupmembers (usergroupid, userid)"
            + " select g.usergroupid, u.userinfoid from usergroup g, userinfo u"
            + " where g.uid = ? and u.uid = ?",
        groupId,
        user.getUid());
    jdbcTemplate.update(
        "insert into userrolemembers (userroleid, userid)"
            + " select r.userroleid, u.userinfoid from userrole r, userinfo u"
            + " where r.uid = ? and u.uid = ?",
        roleId,
        user.getUid());
    assertMe(true, groupId, roleId);

    jdbcTemplate.update(
        "delete from usergroupmembers where userid = (select userinfoid from userinfo where uid = ?)"
            + " and usergroupid = (select usergroupid from usergroup where uid = ?)",
        user.getUid(),
        groupId);
    jdbcTemplate.update(
        "delete from userrolemembers where userid = (select userinfoid from userinfo where uid = ?)"
            + " and userroleid = (select userroleid from userrole where uid = ?)",
        user.getUid(),
        roleId);
    assertMe(false, groupId, roleId);
  }

  private void assertMe(boolean member, String groupId, String roleId) {
    JsonObject me = GET(FIELDS).content(HttpStatus.OK);
    assertEquals(member, ids(me, "userGroups").contains(groupId), "user group listed");
    assertEquals(member, ids(me, "userRoles").contains(roleId), "user role listed");
    assertEquals(
        member,
        me.getArray("authorities").stream()
            .map(authority -> authority.as(JsonString.class).string())
            .anyMatch(AUTHORITY::equals),
        "authority listed");
  }

  private static Set<String> ids(JsonObject me, String property) {
    return me.getArray(property).stream()
        .map(item -> item.as(JsonObject.class).getString("id").string())
        .collect(Collectors.toSet());
  }
}
