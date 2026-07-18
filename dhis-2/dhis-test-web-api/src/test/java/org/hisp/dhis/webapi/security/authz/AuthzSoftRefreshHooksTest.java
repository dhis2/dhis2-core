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
package org.hisp.dhis.webapi.security.authz;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.user.authz.AuthzVersionStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proves the bundle-hook and service-path bump wiring end to end through the real API against
 * Postgres and the production {@code JdbcAuthzVersionStore}: user, role and group mutations must
 * advance the affected generations and the epoch (not just return 2xx).
 *
 * @author Morten Svanæs
 */
@Transactional
class AuthzSoftRefreshHooksTest extends PostgresControllerIntegrationTestBase {

  @Autowired private AuthzVersionStore authzVersionStore;

  @Test
  void groupMembershipBump() {
    String adminUid = getAdminUid();
    long epochBefore = authzVersionStore.getEpoch();

    String groupId =
        assertStatus(
            HttpStatus.CREATED,
            POST("/userGroups", "{'name':'GroupA','users':[{'id':'" + adminUid + "'}]}"));

    assertTrue(
        GET("/userGroups/" + groupId + "?fields=users")
            .content(HttpStatus.OK)
            .toString()
            .contains(adminUid),
        "group import must persist the posted member");
    long genAfterCreate = authzVersionStore.getMaxGen(adminUid, Set.of());
    assertTrue(authzVersionStore.getEpoch() > epochBefore, "group create must advance the epoch");
    assertTrue(genAfterCreate > 0, "group create with member must bump the member's user gen");

    assertStatus(HttpStatus.OK, PUT("/userGroups/" + groupId, "{'name':'GroupA','users':[]}"));

    assertTrue(
        authzVersionStore.getMaxGen(adminUid, Set.of()) > genAfterCreate,
        "membership removal must bump the removed member's user gen");
  }

  @Test
  void roleAuthorityChangeBump() {
    String roleId =
        assertStatus(
            HttpStatus.CREATED,
            POST("/userRoles", "{'name':'RoleA','authorities':['F_DATAVALUE_ADD']}"));

    long roleGenBefore = authzVersionStore.getMaxGen("nonexistentUid", Set.of(roleId));
    long epochBefore = authzVersionStore.getEpoch();

    assertStatus(
        HttpStatus.OK,
        PUT("/userRoles/" + roleId, "{'name':'RoleA','authorities':['F_DATAVALUE_DELETE']}"));

    assertTrue(
        authzVersionStore.getMaxGen("nonexistentUid", Set.of(roleId)) > roleGenBefore,
        "authority change must bump the role gen");
    assertTrue(authzVersionStore.getEpoch() > epochBefore, "role bump must advance the epoch");
  }

  @Test
  void userRoleAssignmentBump() {
    String adminUid = getAdminUid();
    String roleId =
        assertStatus(
            HttpStatus.CREATED,
            POST("/userRoles", "{'name':'RoleC','authorities':['F_DATAVALUE_ADD']}"));

    long epochBefore = authzVersionStore.getEpoch();
    long userGenBefore = authzVersionStore.getMaxGen(adminUid, Set.of());

    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/users/" + adminUid,
            "[{'op':'add','path':'/userRoles','value':[{'id':'" + roleId + "'}]}]"));

    assertTrue(
        authzVersionStore.getMaxGen(adminUid, Set.of()) > userGenBefore,
        "role assignment must bump the user's gen");
    assertTrue(authzVersionStore.getEpoch() > epochBefore, "user bump must advance the epoch");
  }

  @Test
  void roleDeleteBump() {
    String roleId =
        assertStatus(
            HttpStatus.CREATED,
            POST("/userRoles", "{'name':'RoleB','authorities':['F_DATAVALUE_ADD']}"));

    long epochBefore = authzVersionStore.getEpoch();

    assertStatus(HttpStatus.OK, DELETE("/userRoles/" + roleId));

    assertTrue(
        authzVersionStore.getMaxGen("nonexistentUid", Set.of(roleId)) > 0,
        "role delete must bump the deleted role's gen");
    assertTrue(authzVersionStore.getEpoch() > epochBefore, "role delete must advance the epoch");
  }
}
