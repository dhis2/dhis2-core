/*
 * Copyright (c) 2004-2022, University of Oslo
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonError;
import org.hisp.dhis.test.webapi.json.domain.JsonErrorReport;
import org.hisp.dhis.test.webapi.json.domain.JsonImportSummary;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests that the generic collection endpoints of a user, and user replication, apply the rules of a
 * user update [DHIS2-21665]:
 *
 * <ul>
 *   <li>changing a collection of a user requires rights over that user,
 *   <li>organisation units given to a user must be within the current user's hierarchy,
 *   <li>a replica may only get what the current user could give a new user.
 * </ul>
 *
 * @author Morten Svanaes
 */
@Transactional
class UserCollectionAuthorizationControllerTest extends H2ControllerIntegrationTestBase {

  /** An authority no test actor below holds, and which is not needed to reach any endpoint. */
  private static final String UNHELD_AUTHORITY = "F_PERFORM_MAINTENANCE";

  private static final String REPLICA = "{'username':'clone','password':'Vh7!qzLm3xTe'}";

  /** Needed, besides write sharing, to update a user group. */
  private static final String GROUP_AUTHORITY = "F_USERGROUP_PUBLIC_ADD";

  /** The organisation unit of the user admin. */
  private OrganisationUnit ownUnit;

  /** A child of {@link #ownUnit}, so within the hierarchy of the user admin. */
  private OrganisationUnit childUnit;

  /** A second root, outside the hierarchy of the user admin. */
  private OrganisationUnit otherUnit;

  @BeforeEach
  void setUp() {
    ownUnit = createOrganisationUnit('A');
    manager.save(ownUnit);
    childUnit = createOrganisationUnit('B', ownUnit);
    manager.save(childUnit);
    otherUnit = createOrganisationUnit('C');
    manager.save(otherUnit);
  }

  // -------------------------------------------------------------------------
  // Generic collection endpoints: /api/users/{uid}/{property}
  // -------------------------------------------------------------------------

  @ParameterizedTest
  @ValueSource(
      strings = {"organisationUnits", "dataViewOrganisationUnits", "teiSearchOrganisationUnits"})
  void addOrgUnitOutsideHierarchyViaCollectionEndpointIsRejected(String property) {
    User target = createUserWithAuth("target");
    switchContextToUser(userAdmin());

    JsonError error =
        POST("/users/" + target.getUid() + "/" + property + "/" + otherUnit.getUid())
            .error(HttpStatus.FORBIDDEN);
    assertEquals(ErrorCode.E7617, error.getErrorCode());
    assertEquals(
        "Organisation unit: `"
            + otherUnit.getUid()
            + "` not in hierarchy of current user: `useradmin`",
        error.getMessage());

    User persisted = userService.getUser(target.getUid());
    assertFalse(persisted.getOrganisationUnits().contains(otherUnit));
    assertFalse(persisted.getDataViewOrganisationUnits().contains(otherUnit));
    assertFalse(persisted.getTeiSearchOrganisationUnits().contains(otherUnit));
  }

  @Test
  void replaceOrgUnitsWithUnitOutsideHierarchyViaCollectionEndpointIsRejected() {
    User target = createUserWithAuth("target");
    switchContextToUser(userAdmin());

    assertStatus(
        HttpStatus.FORBIDDEN,
        PUT(
            "/users/" + target.getUid() + "/organisationUnits",
            "{'identifiableObjects':[{'id':'" + otherUnit.getUid() + "'}]}"));

    assertFalse(userService.getUser(target.getUid()).getOrganisationUnits().contains(otherUnit));
  }

  @Test
  void addOrgUnitWithinHierarchyViaCollectionEndpointIsAllowed() {
    User target = createUserWithAuth("target");
    switchContextToUser(userAdmin());

    assertStatus(
        HttpStatus.OK,
        POST("/users/" + target.getUid() + "/organisationUnits/" + childUnit.getUid()));

    assertTrue(userService.getUser(target.getUid()).getOrganisationUnits().contains(childUnit));
  }

  /** A user admin may not change a user having authorities they do not hold. */
  @Test
  void changeOrgUnitsOfMorePowerfulUserViaCollectionEndpointIsRejected() {
    User target = createUserWithAuth("target", UNHELD_AUTHORITY);
    switchContextToUser(userAdmin());

    assertEquals(
        ErrorCode.E3020,
        POST("/users/" + target.getUid() + "/organisationUnits/" + childUnit.getUid())
            .error(HttpStatus.FORBIDDEN)
            .getErrorCode());

    assertFalse(userService.getUser(target.getUid()).getOrganisationUnits().contains(childUnit));
  }

  /**
   * Adding a group through the {@code userGroups} collection of a user currently fails for every
   * caller, a superuser included (E1108). The user checks run before that, so the route stays
   * closed should that ever change.
   */
  @Test
  void addUserGroupToMorePowerfulUserViaCollectionEndpointIsRejected() {
    UserGroup group = writableGroup('G');
    User target = createUserWithAuth("target", UNHELD_AUTHORITY);
    switchContextToUser(userAdmin(GROUP_AUTHORITY));

    assertStatus(
        HttpStatus.FORBIDDEN, POST("/users/" + target.getUid() + "/userGroups/" + group.getUid()));

    assertFalse(userService.getUser(target.getUid()).getGroups().contains(group));
  }

  /** The {@code users} of an organisation unit are the other side of the same membership. */
  @Test
  void addUserToOrgUnitOutsideHierarchyViaOrgUnitUsersEndpointIsRejected() {
    User target = createUserWithAuth("target");
    switchContextToUser(userAdmin("F_ORGANISATIONUNIT_ADD"));

    assertStatus(
        HttpStatus.FORBIDDEN,
        POST(
            "/organisationUnits/" + otherUnit.getUid() + "/users",
            "{'additions':[{'id':'" + target.getUid() + "'}]}"));

    assertFalse(userService.getUser(target.getUid()).getOrganisationUnits().contains(otherUnit));
  }

  /**
   * Replacing the users of an organisation unit only checks the users added or removed: members who
   * stay, here a superuser, are not changed.
   */
  @Test
  void replaceOrgUnitUsersKeepingSuperuserIsAllowed() {
    addSuperuserToChildUnit();
    User target = createUserWithAuth("target");
    switchContextToUser(userAdmin("F_ORGANISATIONUNIT_ADD"));

    assertStatus(
        HttpStatus.OK,
        PUT(
            "/organisationUnits/" + childUnit.getUid() + "/users",
            "{'identifiableObjects':[{'id':'"
                + getAdminUid()
                + "'},{'id':'"
                + target.getUid()
                + "'}]}"));

    assertTrue(userService.getUser(target.getUid()).getOrganisationUnits().contains(childUnit));
    assertTrue(userService.getUser(getAdminUid()).getOrganisationUnits().contains(childUnit));
  }

  /** Removing a superuser from an organisation unit by replacing its users stays rejected. */
  @Test
  void replaceOrgUnitUsersRemovingSuperuserIsRejected() {
    addSuperuserToChildUnit();
    User target = createUserWithAuth("target");
    switchContextToUser(userAdmin("F_ORGANISATIONUNIT_ADD"));

    assertEquals(
        ErrorCode.E3041,
        PUT(
                "/organisationUnits/" + childUnit.getUid() + "/users",
                "{'identifiableObjects':[{'id':'" + target.getUid() + "'}]}")
            .error(HttpStatus.FORBIDDEN)
            .getErrorCode());

    assertTrue(userService.getUser(getAdminUid()).getOrganisationUnits().contains(childUnit));
  }

  /** Sets both sides of the membership, as the organisation unit is loaded in this session. */
  private void addSuperuserToChildUnit() {
    User superuser = userService.getUser(getAdminUid());
    childUnit.addUser(superuser);
    userService.updateUser(superuser);
  }

  // -------------------------------------------------------------------------
  // User import: PUT /api/users/{uid}
  // -------------------------------------------------------------------------

  /** An unknown organisation unit gives an import error, not a server error. */
  @Test
  void userUpdateWithUnknownOrgUnitIsRejected() {
    User target = createUserWithAuth("target");
    switchContextToUser(userAdmin());
    String unknown = CodeGenerator.generateUid();

    JsonImportSummary response =
        PUT(
                "/users/" + target.getUid(),
                "{'firstName':'Tess','surname':'Target','username':'target',"
                    + "'organisationUnits':[{'id':'"
                    + unknown
                    + "'}]}")
            .content(HttpStatus.CONFLICT)
            .get("response")
            .as(JsonImportSummary.class);

    assertEquals(
        "Organisation unit: `" + unknown + "` not in hierarchy of current user: `useradmin`",
        response
            .find(JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E7617)
            .getMessage());
  }

  // -------------------------------------------------------------------------
  // User replication: POST /api/users/{uid}/replica
  // -------------------------------------------------------------------------

  @Test
  void replicatingUserWithOrgUnitOutsideHierarchyIsRejected() {
    User source = createUserWithAuth("source");
    source.getDataViewOrganisationUnits().add(otherUnit);
    userService.updateUser(source);
    switchContextToUser(userAdmin("F_REPLICATE_USER"));

    JsonError error =
        POST("/users/" + source.getUid() + "/replica", REPLICA).error(HttpStatus.FORBIDDEN);
    assertEquals(ErrorCode.E7617, error.getErrorCode());
    assertEquals(
        "Organisation unit: `"
            + otherUnit.getUid()
            + "` not in hierarchy of current user: `useradmin`",
        error.getMessage());

    assertNull(userService.getUserByUsername("clone"), "no replica must be created");
  }

  @Test
  void replicatingUserWithOrgUnitsWithinHierarchyIsAllowed() {
    User source = createUserWithAuth("source");
    source.getOrganisationUnits().add(childUnit);
    userService.updateUser(source);
    switchContextToUser(userAdmin("F_REPLICATE_USER"));

    assertStatus(HttpStatus.CREATED, POST("/users/" + source.getUid() + "/replica", REPLICA));

    assertTrue(userService.getUserByUsername("clone").getOrganisationUnits().contains(childUnit));
  }

  @Test
  void replicatingSuperuserIsRejected() {
    switchContextToUser(userAdmin("F_REPLICATE_USER"));

    assertEquals(
        "User `useradmin` is not allowed to change a user having the ALL authority",
        POST("/users/" + getAdminUid() + "/replica", REPLICA)
            .error(HttpStatus.FORBIDDEN)
            .getMessage());
  }

  /** A new user gets the dimension constraints of its creator, so a replica may not drop them. */
  @Test
  void replicatingUserLackingDimensionConstraintsOfCurrentUserIsRejected() {
    CategoryOption option = createCategoryOption('O');
    manager.save(option);
    Category category = createCategory('K', option);
    manager.save(category);
    User source = createUserWithAuth("source");
    User replicator = userAdmin("F_REPLICATE_USER");
    replicator.getCatDimensionConstraints().add(category);
    userService.updateUser(replicator);
    switchContextToUser(replicator);

    JsonError error =
        POST("/users/" + source.getUid() + "/replica", REPLICA).error(HttpStatus.FORBIDDEN);
    assertEquals(ErrorCode.E3053, error.getErrorCode());
    assertEquals(
        "User `useradmin` is not allowed to replicate a user lacking their dimension constraints",
        error.getMessage());
  }

  /**
   * A user admin, without the ALL authority, whose hierarchy is {@link #ownUnit}. The extra
   * authorities are added to the admin's role.
   */
  private User userAdmin(String... extraAuthorities) {
    String[] authorities = new String[extraAuthorities.length + 1];
    authorities[0] = "F_USER_ADD";
    System.arraycopy(extraAuthorities, 0, authorities, 1, extraAuthorities.length);
    User admin = createUserWithAuth("useradmin", authorities);
    admin.getOrganisationUnits().add(ownUnit);
    userService.updateUser(admin);
    return admin;
  }

  /**
   * A group whose members any holder of {@link #GROUP_AUTHORITY} may change, so only the rules
   * under test decide.
   */
  private UserGroup writableGroup(char id) {
    UserGroup group = createUserGroup(id, Set.of());
    group.setPublicAccess("rw------");
    manager.save(group);
    return group;
  }
}
