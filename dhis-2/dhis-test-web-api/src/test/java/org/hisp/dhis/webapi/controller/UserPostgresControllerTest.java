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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonUser;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class UserPostgresControllerTest extends PostgresControllerIntegrationTestBase {

  @Autowired private OrganisationUnitService organisationUnitService;

  @Test
  @DisplayName("GET /users?ou={uid}&includeChildren=true returns users in org unit subtree")
  void testGetUsersFilterByOrgUnitWithChildren() {
    OrganisationUnit orgA = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(orgA);
    OrganisationUnit orgB = createOrganisationUnit('B', orgA);
    organisationUnitService.addOrganisationUnit(orgB);
    OrganisationUnit orgC = createOrganisationUnit('C', orgB);
    organisationUnitService.addOrganisationUnit(orgC);

    User alice = createUserWithAuth("alice");
    alice.addOrganisationUnit(orgA);
    userService.updateUser(alice);

    User bob = createUserWithAuth("bob");
    bob.addOrganisationUnit(orgB);
    userService.updateUser(bob);

    User charlie = createUserWithAuth("charlie");
    charlie.addOrganisationUnit(orgC);
    userService.updateUser(charlie);

    // Switch to a user whose org units are orgA (fresh security context)
    User viewer = createUserWithAuth("viewer", "ALL");
    viewer.addOrganisationUnit(orgA);
    userService.updateUser(viewer);
    switchToNewUser(viewer);

    JsonList<JsonUser> users =
        GET("/users?ou=" + orgA.getUid() + "&includeChildren=true")
            .content(HttpStatus.OK)
            .getList("users", JsonUser.class);

    List<String> uids = users.stream().map(JsonUser::getId).toList();
    assertTrue(uids.contains(alice.getUid()));
    assertTrue(uids.contains(bob.getUid()));
    assertTrue(uids.contains(charlie.getUid()));
    assertTrue(uids.contains(viewer.getUid()));
  }

  @Test
  @DisplayName(
      "GET /users?filter=organisationUnits.id:in:[uid]&userOrgUnits=true&includeChildren=true"
          + " returns intersection of filter and subtree")
  void testGetUsersFilterByOrgUnitMembershipWithChildren() {
    OrganisationUnit orgA = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(orgA);
    OrganisationUnit orgB = createOrganisationUnit('B', orgA);
    organisationUnitService.addOrganisationUnit(orgB);

    User alice = createUserWithAuth("alice");
    alice.addOrganisationUnit(orgA);
    userService.updateUser(alice);

    User bob = createUserWithAuth("bob");
    bob.addOrganisationUnit(orgB);
    userService.updateUser(bob);

    // viewer's org units = orgA → userOrgUnits=true&includeChildren=true covers orgA subtree
    User viewer = createUserWithAuth("viewer", "ALL");
    viewer.addOrganisationUnit(orgA);
    userService.updateUser(viewer);
    switchToNewUser(viewer);

    // filter=organisationUnits.id:in:[orgB] AND userOrgUnits subtree (orgA+children)
    // → only bob satisfies both (directly in orgB, which is under orgA)
    JsonList<JsonUser> users =
        GET("/users?filter=organisationUnits.id:in:["
                + orgB.getUid()
                + "]&userOrgUnits=true&includeChildren=true")
            .content(HttpStatus.OK)
            .getList("users", JsonUser.class);

    List<String> uids = users.stream().map(JsonUser::getId).toList();
    assertTrue(uids.contains(bob.getUid()), "bob (in orgB, subtree of orgA) should be returned");
    assertFalse(uids.contains(alice.getUid()), "alice (in orgA, not orgB) should not be returned");
    assertFalse(
        uids.contains(viewer.getUid()), "viewer (in orgA, not orgB) should not be returned");
  }
}
