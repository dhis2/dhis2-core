/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller.dataintegrity;

import java.util.Set;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Tests the check listing user roles that carry authorities their creator does not hold, or the
 * Test_skipSharingCheck authority. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/users/user_roles_with_authorities_not_held_by_creator.yaml}
 *
 * @author Morten Svanaes
 */
class DataIntegrityUserRolesWithAuthoritiesNotHeldByCreatorTest
    extends AbstractDataIntegrityIntegrationTest {

  private static final String CHECK_NAME = "user_roles_with_authorities_not_held_by_creator";

  private static final String DETAILS_ID_TYPE = "userRoles";

  @Autowired private JdbcTemplate jdbcTemplate;

  /**
   * The test setup makes the superuser role as the superuser, who then holds ALL only through a
   * role they created, which this check lists. A new installation records no creator for that role.
   */
  @BeforeEach
  void clearCreatorOfSetupSuperuserRoles() {
    jdbcTemplate.update(
        "UPDATE userrole SET userid = NULL WHERE userroleid IN"
            + " (SELECT userroleid FROM userroleauthorities WHERE authority = 'ALL')");
  }

  @Test
  void testRolesCarryingAuthoritiesNotHeldByCreatorAreListed() {
    // Roles are saved directly, as a database upgraded from a version without the role checks may
    // hold them; the API rejects them now.
    User roleAdmin = createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD");
    UserRole unheld = role("Unheld", roleAdmin, "F_USERROLE_PUBLIC_ADD", "F_PERFORM_MAINTENANCE");
    role("Held", roleAdmin, "F_USERROLE_PUBLIC_ADD");
    UserRole skipSharing = role("Skip sharing", getCurrentUser(), "Test_skipSharingCheck");
    // The creator holds the ALL authority, so any authority is held.
    role("By superuser", getCurrentUser(), "F_PERFORM_MAINTENANCE");

    // 7 roles: two setup roles, the role of roleadmin, and the 4 above; 2 are listed.
    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE,
        CHECK_NAME,
        28,
        Set.of(unheld.getUid(), skipSharing.getUid()),
        Set.of("Unheld", "Skip sharing"),
        Set.of("Created by roleadmin", "Created by " + getCurrentUser().getUsername()),
        true);
  }

  @Test
  void testRolesWithinTheirCreatorsAuthoritiesAreNotListed() {
    User roleAdmin = createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD", "F_METADATA_IMPORT");
    role("Held", roleAdmin, "F_METADATA_IMPORT");

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK_NAME, true);
  }

  /**
   * A creator who assigned their roles to themselves holds the authorities of those roles only
   * through them, which must not count as holding them.
   */
  @Test
  void testRolesHeldByCreatorOnlyThroughRolesTheyCreatedAreListed() {
    User roleAdmin = createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD");
    UserRole selfAssigned = role("Self assigned", roleAdmin, "F_PERFORM_MAINTENANCE");
    UserRole sibling = role("Sibling", roleAdmin, "F_PERFORM_MAINTENANCE");
    roleAdmin.getUserRoles().add(selfAssigned);
    roleAdmin.getUserRoles().add(sibling);
    userService.updateUser(roleAdmin);

    // 5 roles: two setup roles, the role of roleadmin, and the 2 above; 2 are listed.
    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE,
        CHECK_NAME,
        40,
        Set.of(selfAssigned.getUid(), sibling.getUid()),
        Set.of("Self assigned", "Sibling"),
        Set.of("Created by roleadmin"),
        true);
  }

  /**
   * A creator who gave themselves ALL through a role they created does not hold ALL for this check,
   * so that role is listed. Their other roles are not, as ALL covers their authorities.
   */
  @Test
  void testRoleCarryingAllHeldByCreatorOnlyThroughRolesTheyCreatedIsListed() {
    User roleAdmin = createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD");
    UserRole selfMadeAll = role("Self made all", roleAdmin, "ALL");
    role("Other", roleAdmin, "F_PERFORM_MAINTENANCE");
    roleAdmin.getUserRoles().add(selfMadeAll);
    userService.updateUser(roleAdmin);

    // 5 roles: two setup roles, the role of roleadmin, and the 2 above; 1 is listed.
    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE,
        CHECK_NAME,
        20,
        Set.of(selfMadeAll.getUid()),
        Set.of("Self made all"),
        Set.of("Created by roleadmin"),
        true);
  }

  private UserRole role(String name, User createdBy, String... authorities) {
    UserRole role = createUserRole(name, authorities);
    role.setCreatedBy(createdBy);
    userService.addUserRole(role);
    return role;
  }
}
