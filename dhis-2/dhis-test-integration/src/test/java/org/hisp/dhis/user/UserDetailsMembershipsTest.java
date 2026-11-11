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
package org.hisp.dhis.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The user details of a login or a session refresh must follow the user groups and user roles as
 * stored, also when the second-level cache of this node still holds the old memberships. That is
 * the case when another node of a cluster made the change: the cache invalidation it publishes does
 * not evict every collection that changed. The tests change the rows with plain SQL, which leaves
 * this node's second-level cache as such a change does.
 *
 * <p>Not transactional: every service call commits, as on a server. {@link Lifecycle#PER_CLASS}
 * makes the framework empty the database after the class.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@TestInstance(Lifecycle.PER_CLASS)
class UserDetailsMembershipsTest extends PostgresIntegrationTestBase {

  private static final String AUTHORITY = "F_MEMBERSHIP_CHECK";

  @Autowired private UserGroupService userGroupService;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void userDetailsFollowMembershipsChangedPastTheSecondLevelCache() {
    User user = createAndAddUser("membershipuser");
    UserGroup group = createUserGroup('M', Set.of());
    userGroupService.addUserGroup(group);
    UserRole role = createUserRole('M', AUTHORITY);
    userService.addUserRole(role);
    // reads the memberships once, which puts them into the second-level cache
    UserDetails before = userService.createUserDetailsSafe(user.getUid());
    assertFalse(before.getUserGroupIds().contains(group.getUid()));
    assertFalse(before.isAuthorized(AUTHORITY));

    jdbcTemplate.update(
        "insert into usergroupmembers (usergroupid, userid) values (?, ?)",
        group.getId(),
        user.getId());
    jdbcTemplate.update(
        "insert into userrolemembers (userid, userroleid) values (?, ?)",
        user.getId(),
        role.getId());
    UserDetails added = userService.createUserDetailsSafe(user.getUid());

    assertTrue(added.getUserGroupIds().contains(group.getUid()));
    assertTrue(added.getUserRoleIds().contains(role.getUid()));
    assertTrue(added.isAuthorized(AUTHORITY));

    jdbcTemplate.update(
        "delete from usergroupmembers where usergroupid = ? and userid = ?",
        group.getId(),
        user.getId());
    jdbcTemplate.update(
        "delete from userrolemembers where userid = ? and userroleid = ?",
        user.getId(),
        role.getId());
    UserDetails removed = userService.createUserDetailsSafe(user.getUid());

    assertFalse(removed.getUserGroupIds().contains(group.getUid()));
    assertFalse(removed.getUserRoleIds().contains(role.getUid()));
    assertFalse(removed.isAuthorized(AUTHORITY));
  }

  @Test
  void userDetailsFollowAuthoritiesAndRestrictionsOfTheUserRoles() {
    UserRole role = createUserRole('N', AUTHORITY, "ALL");
    role.getRestrictions().add("R_MEMBERSHIP_CHECK");
    userService.addUserRole(role);
    User user = createAndAddUser("superuserbyrole");
    user.getUserRoles().add(role);
    userService.updateUser(user);

    UserDetails details = userService.createUserDetailsSafe(user.getUid());

    assertTrue(details.isSuper());
    assertTrue(details.isAuthorized(AUTHORITY));
    assertEquals(
        details.getAllAuthorities(),
        Set.copyOf(details.getAuthorities().stream().map(a -> a.getAuthority()).toList()));
    assertTrue(details.hasAnyRestrictions(Set.of("R_MEMBERSHIP_CHECK")));
  }
}
