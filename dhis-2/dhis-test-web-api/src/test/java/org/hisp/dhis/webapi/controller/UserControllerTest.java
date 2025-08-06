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

import static java.util.Collections.emptySet;
import static org.hisp.dhis.external.conf.ConfigurationKey.LINKED_ACCOUNTS_ENABLED;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpClientAdapter.Accept;
import static org.hisp.dhis.http.HttpClientAdapter.Body;
import static org.hisp.dhis.http.HttpStatus.OK;
import static org.hisp.dhis.http.HttpStatus.Series.SUCCESSFUL;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.outboundmessage.OutboundMessage;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonErrorReport;
import org.hisp.dhis.test.webapi.json.domain.JsonImportSummary;
import org.hisp.dhis.test.webapi.json.domain.JsonUser;
import org.hisp.dhis.test.webapi.json.domain.JsonUserGroup;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.user.RestoreType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.jboss.aerogear.security.otp.api.Base32;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.user.UserController}.
 *
 * @author Jan Bernitt
 */
@Transactional
class UserControllerTest extends H2ControllerIntegrationTestBase {
  @Autowired private MessageSender emailMessageSender;

  @Autowired private SystemSettingsService settingsService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private SessionRegistry sessionRegistry;

  @Autowired ObjectMapper objectMapper;

  @Autowired private DhisConfigurationProvider config;

  private User peter;

  @BeforeEach
  void setUp() {
    // TODO(DHIS2-17768 platform) intentional? you are creating 2 users with username `peter` and
    // `Peter` and
    // assigning it to field peter
    // also why switch to the user and then immediately back to the admin user?
    peter = createUserWithAuth("peter");

    this.peter = switchToNewUser("Peter");
    switchToAdminUser();
    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/users/{id}?importReportMode=ERRORS",
            peter.getUid(),
            Body("[{'op': 'replace', 'path': '/email', 'value': 'peter@pan.net'}]")));

    User user = userService.getUser(peter.getUid());
    assertEquals("peter@pan.net", user.getEmail());
  }

  @AfterEach
  void afterEach() {
    emailMessageSender.clearMessages();
  }

  @Test
  void updateRolesShouldInvalidateUserSessions() {
    UserDetails sessionPrincipal = userService.createUserDetails(getAdminUser());
    sessionRegistry.registerNewSession("session1", sessionPrincipal);
    assertFalse(sessionRegistry.getAllSessions(sessionPrincipal, false).isEmpty());

    UserRole roleB = createUserRole("ROLE_B", "ALL");
    userService.addUserRole(roleB);

    String roleBID = userService.getUserRoleByName("ROLE_B").getUid();

    PATCH(
            "/users/" + getAdminUid(),
            "[{'op':'add','path':'/userRoles','value':[{'id':'" + roleBID + "'}]}]")
        .content(HttpStatus.OK);

    assertTrue(sessionRegistry.getAllSessions(sessionPrincipal, false).isEmpty());
  }

  @Test
  void updateRolesAuthoritiesShouldInvalidateUserSessions() {
    UserDetails sessionPrincipal = userService.createUserDetails(getAdminUser());

    UserRole roleB = createUserRole("ROLE_B", "ALL");
    userService.addUserRole(roleB);

    PATCH(
            "/users/" + getAdminUid(),
            "[{'op':'add','path':'/userRoles','value':[{'id':'" + roleB.getUid() + "'}]}]")
        .content(HttpStatus.OK);

    String roleBID = userService.getUserRoleByName("ROLE_B").getUid();

    sessionRegistry.registerNewSession("session1", sessionPrincipal);
    assertFalse(sessionRegistry.getAllSessions(sessionPrincipal, false).isEmpty());

    PATCH(
            "/userRoles/" + roleBID,
            "["
                + " {"
                + "   'op': 'add',"
                + "   'path': '/authorities',"
                + "   'value': ['NONE']"
                + " }"
                + "]")
        .content(HttpStatus.OK);

    assertTrue(sessionRegistry.getAllSessions(sessionPrincipal, false).isEmpty());
  }

  @Test
  void testResetToInvite() {
    assertStatus(HttpStatus.NO_CONTENT, POST("/users/" + peter.getUid() + "/reset"));
    OutboundMessage email = assertMessageSendTo("peter@pan.net");
    assertValidToken(extractTokenFromEmailText(email.getText()));
  }

  @Test
  void testResetToInvite_NoEmail() {
    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/users/{id}",
            peter.getUid() + "?importReportMode=ERRORS",
            Body("[{'op': 'replace', 'path': '/email', 'value': null}]")));
    assertEquals(
        "User account does not have a valid email address",
        POST("/users/" + peter.getUid() + "/reset").error(HttpStatus.CONFLICT).getMessage());
  }

  @Test
  @DisplayName("Check updates after setting an OpenID value works")
  void testSetOpenIdThenUpdate() {
    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/users/{id}",
            peter.getUid() + "?importReportMode=ERRORS",
            Body("[{'op': 'add', 'path': '/openId', 'value': 'mapping value'}]")));

    User user = userService.getUser(peter.getUid());
    assertEquals("mapping value", user.getOpenId());

    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/users/{id}",
            peter.getUid() + "?importReportMode=ERRORS",
            Body("[{'op': 'add', 'path': '/openId', 'value': 'mapping value'}]")));
  }

  @Test
  @DisplayName(
      "Check you can set same OpenID value on multiple accounts when linked accounts are enabled")
  void testSetOpenIdThenUpdateWithLinkedAccountsEnabled() {
    config.getProperties().put(LINKED_ACCOUNTS_ENABLED.getKey(), "on");

    User wendy = createUserWithAuth("wendy");

    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/users/{id}",
            peter.getUid() + "?importReportMode=ERRORS",
            Body("[{'op': 'add', 'path': '/openId', 'value': 'peter@mail.org'}]")));

    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/users/{id}",
            wendy.getUid() + "?importReportMode=ERRORS",
            Body("[{'op': 'add', 'path': '/openId', 'value': 'peter@mail.org'}]")));
  }

  @Test
  @DisplayName(
      "Check you can't set same OpenID value on multiple accounts when linked accounts are disabled")
  void testSetOpenIdThenUpdateWithLinkedAccountsDisabled() {
    config.getProperties().put(LINKED_ACCOUNTS_ENABLED.getKey(), "off");

    User wendy = createUserWithAuth("wendy");

    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/users/{id}",
            peter.getUid() + "?importReportMode=ERRORS",
            Body("[{'op': 'add', 'path': '/openId', 'value': 'peter@mail.org'}]")));

    JsonImportSummary response =
        PATCH(
                "/users/{id}",
                wendy.getUid() + "?importReportMode=ERRORS",
                Body("[{'op': 'add', 'path': '/openId', 'value': 'peter@mail.org'}]"))
            .content(HttpStatus.CONFLICT)
            .get("response")
            .as(JsonImportSummary.class);

    assertEquals(
        "Property `OIDC mapping value` already exists, was given `peter@mail.org`.",
        response
            .find(JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E4054)
            .getMessage());
  }

  /**
   * Test that a user admin without the ALL authority can not update a user having the ALL
   * authority.
   */
  @Test
  void testUpdateRolesWithNoAllAndCanAssignRoles() {

    settingsService.put("keyCanGrantOwnUserAuthorityGroups", true);

    JsonImportSummary response = updateRolesNonAllAdmin();

    JsonList<JsonErrorReport> errorReports =
        response.getList("errorReports", JsonErrorReport.class);

    assertEquals(1, errorReports.size());

    assertEquals(
        "User `someone` is not allowed to change a user having the ALL authority",
        response
            .find(JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E3041)
            .getMessage());
  }

  @Test
  void testUpdateRolesWithNoAllAndNoCanAssignRoles() {

    settingsService.put("keyCanGrantOwnUserAuthorityGroups", false);

    JsonImportSummary response = updateRolesNonAllAdmin();

    JsonList<JsonErrorReport> errorReports =
        response.getList("errorReports", JsonErrorReport.class);

    assertEquals(2, errorReports.size());

    assertEquals(
        "User `someone` is not allowed to change a user having the ALL authority",
        response
            .find(JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E3041)
            .getMessage());

    assertEquals(
        "User `someone` is not allowed to change a user having the ALL authority",
        response
            .find(JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E3041)
            .getMessage());
  }

  private JsonImportSummary updateRolesNonAllAdmin() {

    UserRole roleB = createUserRole("ROLE_B", "NONE");
    userService.addUserRole(roleB);

    User user = createUserWithAuth("someone", "F_USER_ADD");
    user.getUserRoles().add(roleB);
    userService.updateUser(user);

    switchContextToUser(user);

    String roleBID = userService.getUserRoleByName("ROLE_B").getUid();

    JsonImportSummary response =
        PATCH(
                "/users/" + getAdminUid(),
                "[{'op':'add','path':'/userRoles','value':[{'id':'" + roleBID + "'}]}]")
            .content(HttpStatus.CONFLICT)
            .get("response")
            .as(JsonImportSummary.class);

    return response;
  }

  @Test
  void testRemoveALLNonAllAdmin() {
    UserRole roleAll = createUserRole("ROLE_ALL", "ALL");
    userService.addUserRole(roleAll);

    User user = createUserWithAuth("someone", "F_USERROLE_PUBLIC_ADD");
    userService.updateUser(user);
    switchContextToUser(user);

    checkRoleChangFailsWhenNonALLAdmin("'ANYTHING'");
  }

  @Test
  void testAddALLNonAllAdmin() {
    UserRole roleAll = createUserRole("ROLE_ALL", "NONE");
    userService.addUserRole(roleAll);

    User user = createUserWithAuth("someone", "F_USERROLE_PUBLIC_ADD");
    userService.updateUser(user);
    switchContextToUser(user);

    checkRoleChangFailsWhenNonALLAdmin("'ALL'");
  }

  private void checkRoleChangFailsWhenNonALLAdmin(String roleName) {
    String roleAllId = userService.getUserRoleByName("ROLE_ALL").getUid();

    JsonImportSummary response =
        PATCH(
                "/userRoles/" + roleAllId,
                "["
                    + " {"
                    + "   'op': 'add',"
                    + "   'path': '/authorities',"
                    + "   'value': ["
                    + roleName
                    + "   ]"
                    + " }"
                    + "]")
            .content(HttpStatus.CONFLICT)
            .get("response")
            .as(JsonImportSummary.class);

    assertEquals(
        "User `someone` does not have access to user role",
        response
            .find(JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E3032)
            .getMessage());
  }

  @Test
  void testChangeOrgUnitLevelGivesAccessError() {
    settingsService.put("keyCanGrantOwnUserAuthorityGroups", true);

    OrganisationUnit orgA = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(orgA);
    OrganisationUnit orgB = createOrganisationUnit('B', orgA);
    organisationUnitService.addOrganisationUnit(orgB);
    OrganisationUnit orgC = createOrganisationUnit('C', orgB);
    organisationUnitService.addOrganisationUnit(orgC);

    User user = createUserWithAuth("someone", "F_USER_ADD");
    user.addOrganisationUnit(orgC);
    userService.updateUser(user);

    switchContextToUser(user);

    JsonImportSummary response =
        PATCH(
                "/users/" + user.getUid(),
                """
                [{'op':'add','path':'/organisationUnits','value':[{'id':'%s'}]},
                 {'op':'add','path':'/dataViewOrganisationUnits','value':[{'id':'%s'}]},
                 {'op':'add','path':'/teiSearchOrganisationUnits','value':[{'id':'%s'}]}]"""
                    .formatted(orgA.getUid(), orgA.getUid(), orgA.getUid()))
            .content(HttpStatus.CONFLICT)
            .get("response")
            .as(JsonImportSummary.class);

    JsonList<JsonErrorReport> errorReports =
        response.getList("errorReports", JsonErrorReport.class);

    assertEquals(3, errorReports.size());

    assertEquals(
        "Organisation unit: `ouabcdefghA` not in hierarchy of current user: `someone`",
        response
            .find(JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E7617)
            .getMessage());
  }

  @Test
  void updateUserHasAccessToUpdateGroups() {
    settingsService.put("keyCanGrantOwnUserAuthorityGroups", true);

    UserRole roleB = createUserRole("ROLE_B", "F_USER_ADD", "F_USER_GROUPS_READ_ONLY_ADD_MEMBERS");
    userService.addUserRole(roleB);

    UserGroup userGroupA = createUserGroup('A', emptySet());
    manager.save(userGroupA);

    User user = createUserWithAuth("someone", "NONE");
    user.getUserRoles().add(roleB);
    userService.updateUser(user);

    switchContextToUser(user);

    assertStatus(
        HttpStatus.OK,
        PUT(
            "/users/" + user.getUid(),
            " {"
                + "'firstName': 'test',"
                + "'surname': 'tester',"
                + "'username':'someone',"
                + "'userRoles': ["
                + "{"
                + "'id':'"
                + roleB.getUid()
                + "'"
                + "}"
                + "],"
                + "'userGroups': ["
                + "{"
                + "'id':'"
                + userGroupA.getUid()
                + "'"
                + "}]"
                + "}"));
  }

  @Test
  void testUpdateRoles() {
    UserRole userRole = createUserRole("ROLE_B", "ALL");
    userService.addUserRole(userRole);
    String newRoleUid = userService.getUserRoleByName("ROLE_B").getUid();

    User peterBefore = userService.getUser(this.peter.getUid());
    String mainRoleUid = peterBefore.getUserRoles().iterator().next().getUid();

    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/users/{id}",
            this.peter.getUid() + "?importReportMode=ERRORS",
            Body(
                "[{'op':'add','path':'/userRoles','value':[{'id':'"
                    + newRoleUid
                    + "'},{'id':'"
                    + mainRoleUid
                    + "'}]}]")));

    User peterAfter = userService.getUser(this.peter.getUid());
    Set<UserRole> userRoles = peterAfter.getUserRoles();

    assertEquals(2, userRoles.size());
  }

  @Test
  void testAddGroups() {
    UserGroup userGroupA = createUserGroup('A', emptySet());
    manager.save(userGroupA);

    UserGroup userGroupB = createUserGroup('B', emptySet());
    manager.save(userGroupB);

    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/users/{id}",
            this.peter.getUid() + "?importReportMode=ERRORS",
            Body(
                "[{'op':'add','path':'/userGroups','value':[{'id':'"
                    + userGroupA.getUid()
                    + "'},{'id':'"
                    + userGroupB.getUid()
                    + "'}]}]")));

    User peterAfter = userService.getUser(this.peter.getUid());
    Set<UserGroup> userGroups = peterAfter.getGroups();

    assertEquals(2, userGroups.size());
  }

  @Test
  void testAddThenRemoveGroups() {
    UserGroup userGroupA = createUserGroup('A', emptySet());
    manager.save(userGroupA);

    UserGroup userGroupB = createUserGroup('B', emptySet());
    manager.save(userGroupB);

    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/users/{id}",
            this.peter.getUid() + "?importReportMode=ERRORS",
            Body(
                "[{'op':'add','path':'/userGroups','value':[{'id':'"
                    + userGroupA.getUid()
                    + "'},{'id':'"
                    + userGroupB.getUid()
                    + "'}]}]")));

    User peterAfter = userService.getUser(this.peter.getUid());
    Set<UserGroup> userGroups = peterAfter.getGroups();
    assertEquals(2, userGroups.size());

    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/users/{id}",
            this.peter.getUid() + "?importReportMode=ERRORS",
            Body("[{'op':'add','path':'/userGroups','value':[]}]")));

    peterAfter = userService.getUser(this.peter.getUid());
    userGroups = peterAfter.getGroups();
    assertEquals(0, userGroups.size());
  }

  @Test
  void testResetToInvite_NoAuthority() {
    switchToNewUser("someone");
    assertEquals(
        "You don't have the proper permissions to update this user.",
        POST("/users/" + peter.getUid() + "/reset").error(HttpStatus.FORBIDDEN).getMessage());
  }

  @Test
  void testResetToInvite_NoSuchUser() {
    assertEquals(
        "User with id does-not-exist could not be found.",
        POST("/users/does-not-exist/reset").error(HttpStatus.NOT_FOUND).getMessage());
  }

  @Test
  @DisplayName(
      "Test that a user can also delete a user without UserRole write access, see: DHIS2-19693")
  void testReplicateUserNoRoleAuth() {
    settingsService.put("keyCanGrantOwnUserAuthorityGroups", true);

    UserRole replicateRole =
        createUserRole("ROLE_REPLICATE", "F_REPLICATE_USER", "F_USER_ADD", "F_USER_DELETE");
    userService.addUserRole(replicateRole);
    String roleUid = userService.getUserRoleByName("ROLE_REPLICATE").getUid();
    PATCH(
            "/users/" + peter.getUid(),
            "[{'op':'add','path':'/userRoles','value':[{'id':'" + roleUid + "'}]}]")
        .content(HttpStatus.OK);

    peter = userService.getUser(this.peter.getUid());
    assertTrue(
        peter
            .getAllAuthorities()
            .containsAll(Set.of("F_REPLICATE_USER", "F_USER_ADD", "F_USER_DELETE")));
    switchContextToUser(this.peter);

    assertWebMessage(
        "Created",
        201,
        "OK",
        "User replica created",
        POST(
                "/users/" + peter.getUid() + "/replica",
                "{'username':'peter2','password':'Saf€sEcre1'}")
            .content());

    User peter2 = userService.getUserByUsername("peter2");

    // Then
    DELETE("/users/" + peter2.getUid()).content(OK);
  }

  @Test
  void testReplicateUser() {
    assertWebMessage(
        "Created",
        201,
        "OK",
        "User replica created",
        POST(
                "/users/" + peter.getUid() + "/replica",
                "{'username':'peter2','password':'Saf€sEcre1'}")
            .content());
  }

  @Test
  void testReplicateUserCreatedByUpdated() throws JsonProcessingException {
    User newUser = createUserWithAuth("test", "ALL");

    switchToNewUser(newUser);

    String replicatedUsername = "peter2";

    assertWebMessage(
        "Created",
        201,
        "OK",
        "User replica created",
        POST(
                "/users/" + peter.getUid() + "/replica",
                "{'username':'" + replicatedUsername + "','password':'Saf€sEcre1'}")
            .content());

    User replicatedUser = userService.getUserByUsername(replicatedUsername);

    assertEquals(newUser.getUsername(), replicatedUser.getCreatedBy().getUsername());
  }

  @Test
  void testReplicateUser_UserNameAlreadyTaken() {
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Username already taken: peter",
        POST(
                "/users/" + peter.getUid() + "/replica",
                "{'username':'peter','password':'Saf€sEcre1'}")
            .content(HttpStatus.CONFLICT));
  }

  @Test
  void testReplicateUser_UserNotFound() {
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "User not found: notfoundid",
        POST("/users/notfoundid/replica", "{'username':'peter2','password':'Saf€sEcre1'}")
            .content(HttpStatus.CONFLICT));
  }

  @Test
  void testReplicateUser_UserNameNotSpecified() {
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Username must be specified",
        POST("/users/" + peter.getUid() + "/replica", "{'password':'Saf€sEcre1'}")
            .content(HttpStatus.CONFLICT));
  }

  @Test
  void testReplicateUser_PasswordNotSpecified() {
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Password must be specified",
        POST("/users/" + peter.getUid() + "/replica", "{'username':'peter2'}")
            .content(HttpStatus.CONFLICT));
  }

  @Test
  void testReplicateUser_PasswordNotValid() {
    POST("/systemSettings/maxPasswordLength", "72").content(HttpStatus.OK);
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Password must have at least 8, and at most 72 characters",
        POST("/users/" + peter.getUid() + "/replica", "{'username':'peter2','password':'lame'}")
            .content(HttpStatus.CONFLICT));
  }

  @Test
  void testPutJsonObject() {
    JsonObject user = GET("/users/{id}", peter.getUid()).content();
    assertWebMessage(
        "OK",
        200,
        "OK",
        null,
        PUT("/38/users/" + peter.getUid(), user.toString()).content(HttpStatus.OK));
  }

  @Test
  void testPutJsonObject_AddUserGroupLastUpdatedByUpdated() throws JsonProcessingException {
    // create user
    User newUser = createUserWithAuth("test", "ALL");

    // create user group as admin and give new user write permission
    UserGroup newGroup = createUserGroup('z', Set.of());
    Sharing sharing = new Sharing();
    sharing.setPublicAccess("rw------");
    sharing.setUsers(Map.of(newUser.getUid(), new UserAccess("rw------", newUser.getUid())));
    newGroup.setSharing(sharing);

    String newGroupUid =
        assertStatus(
            HttpStatus.CREATED, POST("/userGroups", objectMapper.writeValueAsString(newGroup)));

    // assert lastUpdated is by admin & no users in group
    JsonUserGroup userGroup =
        GET("/userGroups/" + newGroupUid).content(HttpStatus.OK).as(JsonUserGroup.class);

    JsonUser lastUpdatedByAdmin = userGroup.getLastUpdatedBy();
    assertTrue(userGroup.getUsers().isEmpty());
    assertEquals(ADMIN_USER_UID, lastUpdatedByAdmin.getId());
    assertEquals("admin", lastUpdatedByAdmin.getUsername());

    // switch to new user & add usergroup to new user
    String role =
        newUser.getUserRoles().stream().map(BaseIdentifiableObject::getUid).findFirst().get();
    switchToNewUser(newUser);
    PUT(
            "/users/" + newUser.getUid(),
            " {"
                + "'firstName': 'test',"
                + "'surname': 'tester',"
                + "'username':'test',"
                + "'userRoles': ["
                + "{"
                + "'id':'"
                + role
                + "'"
                + "}],"
                + "'userGroups': ["
                + "{"
                + "'id':'"
                + newGroupUid
                + "'"
                + "}]"
                + "}")
        .content(SUCCESSFUL);

    manager.flush();
    manager.clear();
    injectAdminIntoSecurityContext();

    // assert lastUpdated has been updated by new user & users not empty
    JsonUserGroup userGroupUserAdded =
        GET("/userGroups/" + newGroupUid).content(HttpStatus.OK).as(JsonUserGroup.class);
    JsonUser lastUpdatedByNewUser = userGroupUserAdded.getLastUpdatedBy();
    assertFalse(userGroupUserAdded.getUsers().isEmpty());
    assertEquals(newUser.getUid(), lastUpdatedByNewUser.getId());
    assertEquals("test", lastUpdatedByNewUser.getUsername());
  }

  @Test
  void testPutJsonObject_RemoveUserGroupLastUpdatedByUpdated() throws JsonProcessingException {
    // create user
    User newUser = createUserWithAuth("test", "ALL");

    // create user group as admin and give new user write permission
    UserGroup newGroup = createUserGroup('z', Set.of());
    Sharing sharing = new Sharing();
    sharing.setPublicAccess("rw------");
    sharing.setUsers(Map.of(newUser.getUid(), new UserAccess("rw------", newUser.getUid())));
    newGroup.setSharing(sharing);

    String newGroupUid =
        assertStatus(
            HttpStatus.CREATED, POST("/userGroups", objectMapper.writeValueAsString(newGroup)));

    // assert lastUpdated is by admin
    JsonUserGroup userGroup =
        GET("/userGroups/" + newGroupUid).content(HttpStatus.OK).as(JsonUserGroup.class);

    JsonUser lastUpdatedByAdmin = userGroup.getLastUpdatedBy();
    assertTrue(userGroup.getUsers().isEmpty());
    assertEquals(ADMIN_USER_UID, lastUpdatedByAdmin.getId());
    assertEquals("admin", lastUpdatedByAdmin.getUsername());

    manager.flush();
    manager.clear();

    // switch to new user & assign usergroup to new user
    String role =
        newUser.getUserRoles().stream().map(BaseIdentifiableObject::getUid).findFirst().get();
    switchToNewUser(newUser);
    PUT(
            "/users/" + newUser.getUid(),
            " {"
                + "'firstName': 'test',"
                + "'surname': 'tester',"
                + "'username':'test',"
                + "'userRoles': ["
                + "{"
                + "'id':'"
                + role
                + "'"
                + "}],"
                + "'userGroups': ["
                + "{"
                + "'id':'"
                + newGroupUid
                + "'"
                + "}]"
                + "}")
        .content(SUCCESSFUL)
        .as(JsonWebMessage.class);

    manager.flush();
    manager.clear();
    injectSecurityContextUser(newUser);

    // assert lastUpdated has been updated by new user
    JsonUserGroup userGroupUserAdded =
        GET("/userGroups/" + newGroupUid).content(HttpStatus.OK).as(JsonUserGroup.class);

    JsonUser lastUpdatedByNewUser = userGroupUserAdded.getLastUpdatedBy();
    assertFalse(userGroupUserAdded.getUsers().isEmpty());
    assertEquals(newUser.getUid(), lastUpdatedByNewUser.getId());
    assertEquals("test", lastUpdatedByNewUser.getUsername());

    // switch back to admin and remove group from user
    switchToAdminUser();
    PUT(
            "/users/" + newUser.getUid(),
            " {"
                + "'firstName': 'test',"
                + "'surname': 'tester',"
                + "'username':'test',"
                + "'userRoles': ["
                + "{"
                + "'id':'"
                + role
                + "'"
                + "}],"
                + "'userGroups': []"
                + "}")
        .content(SUCCESSFUL);

    // assert lastUpdated has been updated by admin
    JsonUserGroup userGroupUserRemoved =
        GET("/userGroups/" + newGroupUid).content(HttpStatus.OK).as(JsonUserGroup.class);
    JsonUser updatedByAdminAgain = userGroupUserRemoved.getLastUpdatedBy();
    assertTrue(userGroupUserRemoved.getUsers().isEmpty());
    assertEquals(getAdminUid(), updatedByAdminAgain.getId());
    assertEquals("admin", updatedByAdminAgain.getUsername());
  }

  @Test
  void testPutJsonObject_WithSettings() {
    String userId = peter.getUid();
    JsonUser user = GET("/users/{id}", userId).content().as(JsonUser.class);
    assertWebMessage(
        "OK",
        200,
        "OK",
        null,
        PUT(
                "/38/users/" + userId,
                user.node()
                    .addMembers(
                        obj -> obj.addObject("settings", s -> s.addString("keyUiLocale", "de")))
                    .toString())
            .content(HttpStatus.OK));
    assertEquals(
        "de",
        GET("/userSettings/keyUiLocale?userId=" + userId, Accept("text/plain"))
            .content("text/plain"));
  }

  @Test
  void testPutProperty_InvalidWhatsapp() {
    JsonWebMessage msg =
        assertWebMessage(
            "Conflict",
            409,
            "ERROR",
            "One or more errors occurred, please see full details in import report.",
            PATCH(
                    "/users/" + peter.getUid() + "?importReportMode=ERRORS",
                    "[{'op': 'add', 'path': '/whatsApp', 'value': 'not-a-phone-no'}]")
                .content(HttpStatus.CONFLICT));
    JsonErrorReport report =
        msg.getResponse()
            .find(JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E4027);
    assertEquals("whatsApp", report.getErrorProperty());
  }

  @Test
  void testPostJsonObject() {
    assertWebMessage(
        "Created",
        201,
        "OK",
        null,
        POST(
                "/users/",
                "{'surname':'S.','firstName':'Harry', 'username':'harrys', 'userRoles': [{'id':"
                    + " 'yrB6vc5Ip3r'}]}")
            .content(HttpStatus.CREATED));
  }

  @Test
  void testPostJsonObjectInvalidUid() {
    assertWebMessage(
        "Conflict",
        HttpStatus.CONFLICT.code(),
        "ERROR",
        "One or more errors occurred, please see full details in import report.",
        POST(
                "/users/",
                "{'id': 'yrB6vc5Ip¤¤', 'surname':'S.','firstName':'Harry', 'username':'harrys',"
                    + " 'userRoles': [{'id': 'yrB6vc5Ip3r'}]}")
            .content(HttpStatus.CONFLICT));
  }

  @Test
  void testPostJsonObjectInvalidUsername() {
    JsonWebMessage msg =
        assertWebMessage(
            "Conflict",
            409,
            "ERROR",
            "One or more errors occurred, please see full details in import report.",
            POST("/users/", "{'surname':'S.','firstName':'Harry','username':'_Harrys'}")
                .content(HttpStatus.CONFLICT));
    JsonErrorReport report =
        msg.getResponse()
            .find(JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E4049);
    assertEquals("username", report.getErrorProperty());
  }

  @Test
  void testPutNewFormat() {
    JsonObject user = GET("/users/{id}", peter.getUid()).content();

    com.google.gson.JsonObject asJsonObject =
        new Gson().fromJson(user.toString(), JsonElement.class).getAsJsonObject();
    asJsonObject.addProperty("openId", "test");

    PUT("/37/users/" + peter.getUid(), asJsonObject.toString());

    User userAfter = userService.getUser(peter.getUid());
    assertEquals("test", userAfter.getOpenId());
  }

  @Test
  void testPostJsonInvite() {
    UserRole userRole = createUserRole("inviteRole", "ALL");
    userService.addUserRole(userRole);
    UserRole inviteRole = userService.getUserRoleByName("inviteRole");
    String roleUid = inviteRole.getUid();

    assertWebMessage(
        "Created",
        201,
        "OK",
        null,
        POST(
                "/users/invite",
                "{'surname':'S.','firstName':'Harry', 'email':'test@example.com',"
                    + " 'username':'harrys', 'userRoles': [{'id': '"
                    + roleUid
                    + "'}]}")
            .content(HttpStatus.CREATED));
  }

  @Test
  void testPatchUserGroups() {
    UserGroup userGroupA = createUserGroup('A', Set.of());
    userGroupA.setUid("GZSvMCVowAx");
    manager.save(userGroupA);

    UserGroup userGroupB = createUserGroup('B', Set.of());
    userGroupB.setUid("B6JNeAQ6akX");
    manager.save(userGroupB);

    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/users/{id}",
            peter.getUid() + "?importReportMode=ERRORS",
            Body(
                "[{'op': 'add', 'path': '/userGroups', 'value': [ { 'id': 'GZSvMCVowAx' }, { 'id':"
                    + " 'B6JNeAQ6akX' } ] } ]")));

    JsonObject response =
        GET("/users/{id}?fields=userGroups", peter.getUid()).content(HttpStatus.OK);
    assertEquals(2, response.getArray("userGroups").size());

    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/users/{id}",
            peter.getUid() + "?importReportMode=ERRORS",
            Body(
                "[{'op': 'add', 'path': '/userGroups', 'value': [ { 'id': 'GZSvMCVowAx' } ] } ]")));
    response = GET("/users/{id}?fields=userGroups", peter.getUid()).content(HttpStatus.OK);
    assertEquals(1, response.getArray("userGroups").size());
  }

  private String extractTokenFromEmailText(String message) {
    int tokenPos = message.indexOf("?token=");
    return message.substring(tokenPos + 7, message.indexOf('\n', tokenPos)).trim();
  }

  /**
   * Unfortunately this is not yet a spring endpoint, so we have to do it directly instead of using
   * the REST API.
   */
  private void assertValidToken(String token) {
    String[] idAndRestoreToken = userService.decodeEncodedTokens(token);
    String idToken = idAndRestoreToken[0];
    String restoreToken = idAndRestoreToken[1];
    User user = userService.getUserByIdToken(idToken);
    assertNotNull(user);
    ErrorCode errorCode =
        userService.validateRestoreToken(user, restoreToken, RestoreType.RECOVER_PASSWORD);
    assertNull(errorCode);
  }

  private OutboundMessage assertMessageSendTo(String email) {
    List<OutboundMessage> messagesByEmail = emailMessageSender.getMessagesByEmail(email);
    assertFalse(messagesByEmail.isEmpty());
    return messagesByEmail.get(0);
  }

  @Test
  void testIllegalUpdateNoPermission() {
    switchContextToUser(this.peter);
    assertEquals(
        "You don't have the proper permissions to update this user.",
        PUT("/37/users/" + peter.getUid(), "{\"firstName\":\"nils\"}")
            .error(HttpStatus.FORBIDDEN)
            .getMessage());
  }

  @Test
  void testReset2FAPrivilegedWithNonAdminUser() {
    User newUser = makeUser("X", List.of("ALL"));
    newUser.setEmail("valid@email.com");
    String secret = Base32.random();
    newUser.setSecret(secret);
    userService.addUser(newUser);

    switchContextToUser(newUser);

    HttpResponse post = POST("/users/" + newUser.getUid() + "/twoFA/disabled");

    String message = post.error(HttpStatus.FORBIDDEN).getMessage();
    assertEquals("Not allowed to disable 2FA for current user", message);

    User userByUsername = userService.getUserByUsername(newUser.getUsername());
    assertEquals(secret, userByUsername.getSecret());
  }

  @Test
  void testReset2FAPrivilegedWithAdminUser() {
    User newUser = makeUser("X", List.of("ALL"));
    newUser.setEmail("valid@email.com");
    String secret = Base32.random();
    newUser.setSecret(secret);
    userService.addUser(newUser);

    POST("/users/" + newUser.getUid() + "/twoFA/disabled").content(HttpStatus.OK);

    User userByUsername = userService.getUserByUsername(newUser.getUsername());

    assertNull(userByUsername.getSecret());
  }
}
