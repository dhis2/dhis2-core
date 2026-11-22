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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonError;
import org.hisp.dhis.webapi.json.domain.JsonErrorReport;
import org.hisp.dhis.webapi.json.domain.JsonImportSummary;
import org.hisp.dhis.webapi.json.domain.JsonTypeReport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the rules for changing user roles and role memberships [DHIS2-21665]:
 *
 * <ul>
 *   <li>a role may only carry authorities the current user holds,
 *   <li>a role may only be given to a user by someone allowed to issue it,
 *   <li>changing the roles of a user requires rights over that user.
 * </ul>
 *
 * @author Morten Svanaes
 */
@Transactional
class UserRoleControllerTest extends DhisControllerConvenienceTest {

  /** An authority no test actor below holds, and which is not needed to reach any endpoint. */
  private static final String UNHELD_AUTHORITY = "F_PERFORM_MAINTENANCE";

  private static final String CANNOT_CHANGE_SUPERUSER =
      "User `useradmin` is not allowed to change a user having the ALL authority";

  @Autowired private SystemSettingManager systemSettingManager;

  @AfterEach
  void resetSettings() {
    systemSettingManager.saveSystemSetting(SettingKey.CAN_GRANT_OWN_USER_ROLES, false);
  }

  // -------------------------------------------------------------------------
  // Creating and updating a role (metadata / ObjectBundle path)
  // -------------------------------------------------------------------------

  @Test
  void createUserRoleWithUnheldAuthorityIsRejected() {
    switchContextToUser(createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD"));

    JsonImportSummary response =
        POST("/userRoles", "{'name':'unheld','authorities':['" + UNHELD_AUTHORITY + "']}")
            .content(HttpStatus.CONFLICT)
            .get("response")
            .as(JsonImportSummary.class);

    assertEquals(
        "User `roleadmin` is not allowed to grant users access to user role `unheld`",
        response
            .find(JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E3003)
            .getMessage());
  }

  @Test
  void createUserRoleWithUnheldAuthorityViaMetadataImportIsRejected() {
    switchContextToUser(
        createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD", "F_METADATA_IMPORT"));

    JsonImportSummary response =
        POST(
                "/metadata",
                "{'userRoles':[{'name':'unheld','authorities':['" + UNHELD_AUTHORITY + "']}]}")
            .content(HttpStatus.CONFLICT)
            .as(JsonImportSummary.class);

    assertEquals(
        "User `roleadmin` is not allowed to grant users access to user role `unheld`",
        response
            .find(JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E3003)
            .getMessage());
    assertNull(userService.getUserRoleByName("unheld"));
  }

  @Test
  void updateUserRoleWithUnheldAuthorityIsRejected() {
    switchContextToUser(createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD"));

    String roleId = assertStatus(HttpStatus.CREATED, POST("/userRoles", "{'name':'own'}"));

    JsonImportSummary response =
        PATCH(
                "/userRoles/" + roleId,
                "[{'op':'add','path':'/authorities','value':['" + UNHELD_AUTHORITY + "']}]")
            .content(HttpStatus.CONFLICT)
            .get("response")
            .as(JsonImportSummary.class);

    assertEquals(
        "User `roleadmin` is not allowed to grant users access to user role `own`",
        response
            .find(JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E3003)
            .getMessage());
  }

  @Test
  void createUserRoleWithHeldAuthoritiesIsAllowed() {
    switchContextToUser(
        createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD", "F_METADATA_IMPORT"));

    assertStatus(
        HttpStatus.CREATED,
        POST("/userRoles", "{'name':'delegated','authorities':['F_METADATA_IMPORT']}"));
  }

  @Test
  void createUserRoleWithoutAuthoritiesIsAllowed() {
    switchContextToUser(createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD"));

    assertStatus(HttpStatus.CREATED, POST("/userRoles", "{'name':'plain','authorities':[]}"));
  }

  /** {@code null} authorities are treated like no authorities. */
  @Test
  void createUserRoleWithNullAuthoritiesIsAllowed() {
    switchContextToUser(createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD"));

    assertStatus(HttpStatus.CREATED, POST("/userRoles", "{'name':'plain','authorities':null}"));
  }

  @Test
  void createUserRoleWithNullAuthorityEntryIsRejected() {
    switchContextToUser(createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD"));
    JsonImportSummary response =
        POST("/userRoles", "{'name':'nullentry','authorities':[null]}")
            .content(HttpStatus.CONFLICT)
            .get("response")
            .as(JsonImportSummary.class);
    assertEquals(
        ErrorCode.E3003,
        response
            .find(JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E3003)
            .getErrorCode());
    assertNull(userService.getUserRoleByName("nullentry"));
  }

  /** Renaming a role you already hold must stay possible; only the authority set is restricted. */
  @Test
  void renamingAnOwnedRoleIsStillAllowed() {
    User roleAdmin = createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD", "F_METADATA_IMPORT");
    UserRole own = writableRole("OWN_ROLE", "F_METADATA_IMPORT");
    roleAdmin.getUserRoles().add(own);
    userService.updateUser(roleAdmin);
    switchContextToUser(roleAdmin);

    assertStatus(
        HttpStatus.OK,
        PATCH("/userRoles/" + own.getUid(), "[{'op':'replace','path':'/name','value':'renamed'}]"));
  }

  /**
   * Any update of a role, not just one that adds authorities, requires the current user to hold all
   * authorities of the resulting role.
   */
  @Test
  void renamingRoleCarryingUnheldAuthorityIsRejected() {
    UserRole other = writableRole("OTHER", UNHELD_AUTHORITY);
    switchContextToUser(createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD"));

    JsonImportSummary response =
        PATCH("/userRoles/" + other.getUid(), "[{'op':'replace','path':'/name','value':'renamed'}]")
            .content(HttpStatus.CONFLICT)
            .as(JsonImportSummary.class);

    assertEquals(
        "User `roleadmin` is not allowed to grant users access to user role `renamed`",
        response
            .find(JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E3003)
            .getMessage());
  }

  /** An update without authorities keeps the stored ones, so those are checked. */
  @Test
  void renamingRoleCarryingUnheldAuthorityWithoutAuthoritiesIsRejected() {
    UserRole other = writableRole("OTHER", UNHELD_AUTHORITY);
    switchContextToUser(createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD"));

    JsonImportSummary response =
        PUT("/userRoles/" + other.getUid(), "{'name':'renamed','authorities':null}")
            .content(HttpStatus.CONFLICT)
            .get("response")
            .as(JsonImportSummary.class);

    assertEquals(
        "User `roleadmin` is not allowed to grant users access to user role `renamed`",
        response
            .find(JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E3003)
            .getMessage());
    assertEquals("OTHER", userService.getUserRole(other.getUid()).getName());
  }

  // -------------------------------------------------------------------------
  // Changing the sharing of a role
  // -------------------------------------------------------------------------

  @Test
  void changingSharingOfRoleCarryingUnheldAuthorityIsRejected() {
    UserRole other = writableRole("OTHER", UNHELD_AUTHORITY);
    switchContextToUser(createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD"));

    JsonTypeReport report =
        PUT(
                "/userRoles/" + other.getUid() + "/sharing",
                "{'owner':'" + getAdminUid() + "','public':'r-------'}")
            .content(HttpStatus.CONFLICT)
            .get("response")
            .as(JsonTypeReport.class);

    assertEquals(
        ErrorCode.E3003, report.getObjectReports().get(0).getErrorReports().get(0).getErrorCode());
    assertEquals(
        "rw------", userService.getUserRole(other.getUid()).getSharing().getPublicAccess());
  }

  @Test
  void changingSharingOfRoleCarryingUnheldAuthorityViaSharingEndpointIsRejected() {
    UserRole other = writableRole("OTHER", UNHELD_AUTHORITY);
    switchContextToUser(createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD"));

    assertEquals(
        ErrorCode.E3003,
        POST(
                "/sharing?type=userRole&id=" + other.getUid(),
                "{'object':{'publicAccess':'r-------'}}")
            .error(HttpStatus.CONFLICT)
            .getErrorCode());
    assertEquals(
        "rw------", userService.getUserRole(other.getUid()).getSharing().getPublicAccess());
  }

  @Test
  void changingSharingOfRoleWithHeldAuthoritiesIsAllowed() {
    UserRole held = writableRole("HELD", "F_METADATA_IMPORT");
    switchContextToUser(
        createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD", "F_METADATA_IMPORT"));

    assertStatus(
        HttpStatus.NO_CONTENT,
        PUT(
            "/userRoles/" + held.getUid() + "/sharing",
            "{'owner':'" + getAdminUid() + "','public':'rw------'}"));
    assertStatus(
        HttpStatus.OK,
        POST(
            "/sharing?type=userRole&id=" + held.getUid(),
            "{'object':{'publicAccess':'r-------'}}"));
    assertEquals("r-------", userService.getUserRole(held.getUid()).getSharing().getPublicAccess());
  }

  @Test
  void createUserRoleWithAllAuthorityIsStillRejected() {
    switchContextToUser(createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD"));

    JsonImportSummary response =
        POST("/userRoles", "{'name':'superrole','authorities':['ALL']}")
            .content(HttpStatus.CONFLICT)
            .get("response")
            .as(JsonImportSummary.class);

    assertEquals(
        "User `roleadmin` does not have access to user role",
        response
            .find(JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E3032)
            .getMessage());
  }

  @Test
  void superuserCanCreateUserRoleWithAnyAuthority() {
    switchToSuperuser();

    assertStatus(
        HttpStatus.CREATED,
        POST("/userRoles", "{'name':'anything','authorities':['" + UNHELD_AUTHORITY + "']}"));
  }

  // -------------------------------------------------------------------------
  // Deleting a role
  // -------------------------------------------------------------------------

  /** Deleting a role does not grant anything, so the authority superset rule does not apply. */
  @Test
  void deleteUserRoleCarryingUnheldAuthorityIsAllowed() {
    UserRole other = writableRole("OTHER", UNHELD_AUTHORITY);
    switchContextToUser(
        createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD", "F_USERROLE_DELETE"));

    assertStatus(HttpStatus.OK, DELETE("/userRoles/" + other.getUid()));

    assertNull(userService.getUserRole(other.getUid()));
  }

  @Test
  void deleteUserRoleCarryingUnheldAuthorityViaMetadataImportIsAllowed() {
    UserRole other = writableRole("OTHER", UNHELD_AUTHORITY);
    switchContextToUser(
        createUserWithAuth(
            "roleadmin", "F_USERROLE_PUBLIC_ADD", "F_USERROLE_DELETE", "F_METADATA_IMPORT"));

    assertStatus(
        HttpStatus.OK,
        POST("/metadata?importStrategy=DELETE", "{'userRoles':[{'id':'" + other.getUid() + "'}]}"));

    assertNull(userService.getUserRole(other.getUid()));
  }

  @Test
  void deleteUserRoleCarryingAllAuthorityIsRejected() {
    UserRole superRole = writableRole("SUPER", "ALL");
    switchContextToUser(
        createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD", "F_USERROLE_DELETE"));

    JsonImportSummary response =
        DELETE("/userRoles/" + superRole.getUid())
            .content(HttpStatus.CONFLICT)
            .as(JsonImportSummary.class);

    assertEquals(
        "User `roleadmin` does not have access to user role",
        response
            .find(JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E3032)
            .getMessage());
    assertNotNull(userService.getUserRole(superRole.getUid()));
  }

  // -------------------------------------------------------------------------
  // Membership endpoints: /api/userRoles/{id}/users/{userId}
  // -------------------------------------------------------------------------

  @Test
  void addSelfToRoleWithUnheldAuthorityIsRejected() {
    UserRole unissuable = writableRole("UNISSUABLE", UNHELD_AUTHORITY);
    User roleAdmin = createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD", "F_USER_ADD");
    switchContextToUser(roleAdmin);

    assertStatus(
        HttpStatus.FORBIDDEN,
        POST("/userRoles/" + unissuable.getUid() + "/users/" + roleAdmin.getUid()));

    assertFalse(hasRole(roleAdmin, unissuable), "the role must not have been granted");
  }

  @Test
  void addSelfToRoleWithUnheldAuthorityUsingPutIsRejected() {
    UserRole unissuable = writableRole("UNISSUABLE", UNHELD_AUTHORITY);
    User roleAdmin = createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD", "F_USER_ADD");
    switchContextToUser(roleAdmin);

    assertStatus(
        HttpStatus.FORBIDDEN,
        PUT("/userRoles/" + unissuable.getUid() + "/users/" + roleAdmin.getUid()));

    assertFalse(hasRole(roleAdmin, unissuable), "the role must not have been granted");
  }

  @Test
  void addUserToRoleWithHeldAuthoritiesIsAllowed() {
    UserRole grantable = writableRole("GRANTABLE", "F_METADATA_IMPORT");
    User target = createUserWithAuth("target");
    switchContextToUser(userAdmin());

    assertStatus(
        HttpStatus.NO_CONTENT,
        POST("/userRoles/" + grantable.getUid() + "/users/" + target.getUid()));

    assertTrue(hasRole(target, grantable));
  }

  /** Role write access alone must not be enough to edit another user's role set. */
  @Test
  void addUserToRoleWithoutRightsOverTargetUserIsRejected() {
    UserRole grantable = writableRole("GRANTABLE", "F_METADATA_IMPORT");
    User target = createUserWithAuth("target");
    switchContextToUser(
        createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD", "F_METADATA_IMPORT"));

    assertStatus(
        HttpStatus.FORBIDDEN,
        POST("/userRoles/" + grantable.getUid() + "/users/" + target.getUid()));
  }

  @Test
  void removeUserFromRoleWithoutRightsOverTargetUserIsRejected() {
    UserRole grantable = writableRole("GRANTABLE", "F_METADATA_IMPORT");
    User target = createUserWithAuth("target");
    target.getUserRoles().add(grantable);
    userService.updateUser(target);
    switchContextToUser(
        createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD", "F_METADATA_IMPORT"));

    assertStatus(
        HttpStatus.FORBIDDEN,
        DELETE("/userRoles/" + grantable.getUid() + "/users/" + target.getUid()));
  }

  @Test
  void removeSuperuserFromRoleIsRejectedForUserAdmin() {
    UserRole grantable = writableRole("GRANTABLE", "F_METADATA_IMPORT");
    User superuser = createUserWithAuth("superuser", "ALL");
    superuser.getUserRoles().add(grantable);
    userService.updateUser(superuser);
    switchContextToUser(userAdmin());

    assertEquals(
        CANNOT_CHANGE_SUPERUSER,
        DELETE("/userRoles/" + grantable.getUid() + "/users/" + superuser.getUid())
            .error(HttpStatus.FORBIDDEN)
            .getMessage());

    assertTrue(hasRole(superuser, grantable));
  }

  /**
   * Revoking is not restricted to the roles the current user could grant. With "can grant own user
   * roles" disabled a user admin may not hand out the role they hold themselves, but they must
   * still be able to take it away from someone.
   */
  @Test
  void removeUnissuableRoleFromUserIsAllowedForUserAdmin() {
    systemSettingManager.saveSystemSetting(SettingKey.CAN_GRANT_OWN_USER_ROLES, false);
    UserRole shared = writableRole("SHARED", "F_METADATA_IMPORT");
    User userAdmin = userAdmin();
    userAdmin.getUserRoles().add(shared);
    userService.updateUser(userAdmin);
    User target = createUserWithAuth("target");
    target.getUserRoles().add(shared);
    userService.updateUser(target);
    User other = createUserWithAuth("other");
    switchContextToUser(userAdmin);

    // granting the role the current user holds themselves is not allowed by default ...
    assertStatus(
        HttpStatus.FORBIDDEN, POST("/userRoles/" + shared.getUid() + "/users/" + other.getUid()));

    // ... but revoking it is
    assertStatus(
        HttpStatus.NO_CONTENT,
        DELETE("/userRoles/" + shared.getUid() + "/users/" + target.getUid()));

    assertFalse(hasRole(other, shared));
    assertFalse(hasRole(target, shared));
  }

  @Test
  void addUserToOwnRoleIsAllowedWhenGrantingOwnRolesIsEnabled() {
    systemSettingManager.saveSystemSetting(SettingKey.CAN_GRANT_OWN_USER_ROLES, true);
    UserRole shared = writableRole("SHARED", "F_METADATA_IMPORT");
    User userAdmin = userAdmin();
    userAdmin.getUserRoles().add(shared);
    userService.updateUser(userAdmin);
    User other = createUserWithAuth("other");
    switchContextToUser(userAdmin);

    assertStatus(
        HttpStatus.NO_CONTENT, POST("/userRoles/" + shared.getUid() + "/users/" + other.getUid()));

    assertTrue(hasRole(other, shared));
  }

  // -------------------------------------------------------------------------
  // Generic collection endpoints: /api/users/{uid}/userRoles
  // -------------------------------------------------------------------------

  @Test
  void grantRoleViaUserRolesCollectionEndpointIsRejected() {
    UserRole unissuable = writableRole("UNISSUABLE", UNHELD_AUTHORITY);
    User roleAdmin = createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD", "F_USER_ADD");
    switchContextToUser(roleAdmin);

    assertStatus(
        HttpStatus.FORBIDDEN,
        POST("/users/" + roleAdmin.getUid() + "/userRoles/" + unissuable.getUid()));

    assertFalse(hasRole(roleAdmin, unissuable), "the role must not have been granted");
  }

  @Test
  void grantHeldRoleViaCollectionEndpointIsAllowed() {
    UserRole grantable = writableRole("GRANTABLE", "F_METADATA_IMPORT");
    User target = createUserWithAuth("target");
    switchContextToUser(userAdmin());

    assertStatus(
        HttpStatus.OK, POST("/users/" + target.getUid() + "/userRoles/" + grantable.getUid()));

    assertTrue(hasRole(target, grantable));
  }

  @Test
  void removeRoleViaCollectionEndpointIsAllowed() {
    UserRole grantable = writableRole("GRANTABLE", "F_METADATA_IMPORT");
    User target = createUserWithAuth("target");
    target.getUserRoles().add(grantable);
    userService.updateUser(target);
    switchContextToUser(userAdmin());

    assertStatus(
        HttpStatus.OK, DELETE("/users/" + target.getUid() + "/userRoles/" + grantable.getUid()));

    assertFalse(hasRole(target, grantable));
  }

  @Test
  void grantRoleToSuperuserViaCollectionEndpointIsRejected() {
    UserRole grantable = writableRole("GRANTABLE", "F_METADATA_IMPORT");
    switchContextToUser(userAdmin());

    assertEquals(
        CANNOT_CHANGE_SUPERUSER,
        POST("/users/" + getAdminUid() + "/userRoles/" + grantable.getUid())
            .error(HttpStatus.FORBIDDEN)
            .getMessage());

    assertFalse(hasRole(getSuperUser(), grantable));
  }

  @Test
  void removeRoleFromSuperuserViaCollectionEndpointIsRejected() {
    UserRole superRole = getSuperUser().getUserRoles().iterator().next();
    switchContextToUser(userAdmin());

    assertEquals(
        CANNOT_CHANGE_SUPERUSER,
        DELETE("/users/" + getAdminUid() + "/userRoles/" + superRole.getUid())
            .error(HttpStatus.FORBIDDEN)
            .getMessage());
    assertEquals(
        CANNOT_CHANGE_SUPERUSER,
        POST(
                "/users/" + getAdminUid() + "/userRoles",
                "{'deletions':[{'id':'" + superRole.getUid() + "'}]}")
            .error(HttpStatus.FORBIDDEN)
            .getMessage());

    assertTrue(hasRole(getSuperUser(), superRole));
  }

  /** A user admin may not change the roles of a user having authorities they do not hold. */
  @Test
  void removeRoleFromUserWithUnheldAuthorityViaCollectionEndpointIsRejected() {
    UserRole grantable = writableRole("GRANTABLE", "F_METADATA_IMPORT");
    User target = createUserWithAuth("target", UNHELD_AUTHORITY);
    target.getUserRoles().add(grantable);
    userService.updateUser(target);
    switchContextToUser(userAdmin());

    assertEquals(
        "You must have permissions to create user, or ability to manage at least one user group for the user",
        DELETE("/users/" + target.getUid() + "/userRoles/" + grantable.getUid())
            .error(HttpStatus.FORBIDDEN)
            .getMessage());

    assertTrue(hasRole(target, grantable));
  }

  /**
   * The {@code users} collection of a role cannot be changed through the generic collection
   * endpoint (it is not the owning side, and the update fails for every caller). The role checks
   * still run before that, so the route stays closed should that ever change.
   */
  @Test
  void grantRoleViaRoleUsersCollectionEndpointIsRejected() {
    UserRole unissuable = writableRole("UNISSUABLE", UNHELD_AUTHORITY);
    User roleAdmin = createUserWithAuth("roleadmin", "F_USERROLE_PUBLIC_ADD", "F_USER_ADD");
    switchContextToUser(roleAdmin);

    assertStatus(
        HttpStatus.FORBIDDEN,
        POST(
            "/userRoles/" + unissuable.getUid() + "/users",
            "{'additions':[{'id':'" + roleAdmin.getUid() + "'}]}"));

    assertFalse(hasRole(roleAdmin, unissuable), "the role must not have been granted");
  }

  // -------------------------------------------------------------------------
  // User replication copies role memberships
  // -------------------------------------------------------------------------

  @Test
  void replicatingUserWithUnissuableRoleIsRejected() {
    UserRole unissuable = writableRole("UNISSUABLE", UNHELD_AUTHORITY);
    User source = createUserWithAuth("source");
    source.getUserRoles().add(unissuable);
    userService.updateUser(source);
    switchContextToUser(
        createUserWithAuth(
            "replicator", "F_REPLICATE_USER", "F_USER_ADD", "F_USERROLE_PUBLIC_ADD"));

    assertStatus(
        HttpStatus.FORBIDDEN,
        POST(
            "/users/" + source.getUid() + "/replica",
            "{'username':'clone','password':'Vh7!qzLm3xTe'}"));
  }

  @Test
  void replicatingUserWithIssuableRolesIsAllowed() {
    UserRole grantable = writableRole("GRANTABLE", "F_METADATA_IMPORT");
    User source = createUserWithAuth("source");
    source.getUserRoles().add(grantable);
    userService.updateUser(source);
    switchContextToUser(
        createUserWithAuth(
            "replicator",
            "F_REPLICATE_USER",
            "F_USER_ADD",
            "F_USERROLE_PUBLIC_ADD",
            "F_METADATA_IMPORT"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/users/" + source.getUid() + "/replica",
            "{'username':'clone','password':'Vh7!qzLm3xTe'}"));
  }

  /**
   * Every role membership of the source is copied, including roles the replicator cannot read, so
   * those are checked as well. Here the replicator holds the role, and granting own roles is off.
   */
  @Test
  void replicatingUserWithUnreadableRoleIsRejected() {
    UserRole hidden = createUserRole("HIDDEN", "F_METADATA_IMPORT");
    userService.addUserRole(hidden);
    // Saving gives a new role the default public access, so make it private afterwards.
    hidden.getSharing().setPublicAccess("--------");
    manager.updateNoAcl(hidden);
    User source = createUserWithAuth("source");
    source.getUserRoles().add(hidden);
    userService.updateUser(source);
    User replicator =
        createUserWithAuth(
            "replicator",
            "F_REPLICATE_USER",
            "F_USER_ADD",
            "F_USERROLE_PUBLIC_ADD",
            "F_METADATA_IMPORT");
    replicator.getUserRoles().add(hidden);
    userService.updateUser(replicator);
    switchContextToUser(replicator);

    JsonError error =
        POST(
                "/users/" + source.getUid() + "/replica",
                "{'username':'clone','password':'Vh7!qzLm3xTe'}")
            .error(HttpStatus.FORBIDDEN);
    assertEquals(ErrorCode.E3003, error.getErrorCode());
    // The error names an unreadable role by its UID only.
    assertEquals(
        "User `replicator` is not allowed to grant users access to user role `"
            + hidden.getUid()
            + "`",
        error.getMessage());
    assertNull(userService.getUserByUsername("clone"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "?skipSharing=true"})
  void grantingUnreadableRoleByUserUpdateIsRejected(String query) {
    UserRole hidden = createUserRole("HIDDEN", UNHELD_AUTHORITY);
    userService.addUserRole(hidden);
    hidden.getSharing().setPublicAccess("--------");
    manager.updateNoAcl(hidden);
    User target = createUserWithAuth("target");
    switchContextToUser(userAdmin());

    JsonImportSummary response =
        PUT(
                "/users/" + target.getUid() + query,
                "{'firstName':'Tess','surname':'Target','username':'target','userRoles':[{'id':'"
                    + hidden.getUid()
                    + "'}]}")
            .content(HttpStatus.CONFLICT)
            .get("response")
            .as(JsonImportSummary.class);

    // The error names an unreadable role by its UID only.
    assertEquals(
        "User `useradmin` is not allowed to grant users access to user role `"
            + hidden.getUid()
            + "`",
        response
            .find(JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E3003)
            .getMessage());
    assertFalse(hasRole(target, hidden), "the role must not have been granted");
    assertEquals(
        ErrorCode.E3003,
        response
            .find(JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E3003)
            .getErrorCode(),
        response.toString());
  }

  /**
   * A user admin who may write roles and users and holds {@code F_METADATA_IMPORT}, but not the ALL
   * authority.
   */
  private User userAdmin() {
    return createUserWithAuth(
        "useradmin", "F_USERROLE_PUBLIC_ADD", "F_USER_ADD", "F_METADATA_IMPORT");
  }

  /**
   * A role any holder of {@code F_USERROLE_PUBLIC_ADD} may update, so only the rules under test
   * decide whether it may be granted.
   */
  private UserRole writableRole(String name, String... authorities) {
    UserRole role = createUserRole(name, authorities);
    role.setPublicAccess("rw------");
    userService.addUserRole(role);
    return role;
  }

  private boolean hasRole(User user, UserRole role) {
    return userService.getUser(user.getUid()).getUserRoles().contains(role);
  }
}
