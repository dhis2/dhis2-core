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
package org.hisp.dhis.jsonpatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.hibernate.Hibernate;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchException;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen
 */
@Transactional
class JsonPatchManagerTest extends PostgresIntegrationTestBase {

  private final ObjectMapper jsonMapper = JacksonObjectMapperConfig.staticJsonMapper();

  @Autowired private JsonPatchManager jsonPatchManager;

  @Autowired private IdentifiableObjectManager manager;

  @Test
  void testSimpleAddPatchNoPersist() throws Exception {
    Constant constant = createConstant('A', 1.0d);
    assertEquals("ConstantA", constant.getName());
    assertEquals(constant.getValue(), 0, 1.0d);
    JsonPatch patch =
        jsonMapper.readValue(
            "["
                + "{\"op\": \"add\", \"path\": \"/name\", \"value\": \"updated\"},"
                + "{\"op\": \"add\", \"path\": \"/value\", \"value\": 5.0}"
                + "]",
            JsonPatch.class);
    assertNotNull(patch);
    Constant patchedConstant = jsonPatchManager.apply(patch, constant);
    assertEquals("ConstantA", constant.getName());
    assertEquals(constant.getValue(), 0, 1.0d);
    assertEquals("updated", patchedConstant.getName());
    assertEquals(patchedConstant.getValue(), 0, 5.0d);
  }

  @Test
  void testSimpleAddPatch() throws Exception {
    Constant constant = createConstant('A', 1.0d);
    assertEquals("ConstantA", constant.getName());
    assertEquals(constant.getValue(), 0, 1.0d);
    JsonPatch patch =
        jsonMapper.readValue(
            "["
                + "{\"op\": \"add\", \"path\": \"/name\", \"value\": \"updated\"},"
                + "{\"op\": \"add\", \"path\": \"/value\", \"value\": 5.0}"
                + "]",
            JsonPatch.class);
    assertNotNull(patch);
    Constant patchedConstant = jsonPatchManager.apply(patch, constant);
    patchedConstant.setUid(CodeGenerator.generateUid());
    assertEquals("ConstantA", constant.getName());
    assertEquals(constant.getValue(), 0, 1.0d);
    assertEquals("updated", patchedConstant.getName());
    assertEquals(patchedConstant.getValue(), 0, 5.0d);
  }

  @Test
  void testCollectionAddPatchNoPersist() throws Exception {
    DataElementGroup dataElementGroup = createDataElementGroup('A');
    assertEquals("DataElementGroupA", dataElementGroup.getName());
    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    dataElementGroup.getMembers().add(dataElementA);
    dataElementGroup.getMembers().add(dataElementB);
    assertEquals(2, dataElementGroup.getMembers().size());
    JsonPatch patch =
        jsonMapper.readValue(
            "["
                + "{\"op\": \"add\", \"path\": \"/name\", \"value\": \"updated\"},"
                + "{\"op\": \"add\", \"path\": \"/dataElements/-\", \"value\": {\"id\": \"my-uid\"}}"
                + "]",
            JsonPatch.class);
    assertNotNull(patch);
    DataElementGroup patchedDataElementGroup = jsonPatchManager.apply(patch, dataElementGroup);
    assertEquals("DataElementGroupA", dataElementGroup.getName());
    assertEquals(2, dataElementGroup.getMembers().size());
    assertEquals("updated", patchedDataElementGroup.getName());
    assertEquals(3, patchedDataElementGroup.getMembers().size());
  }

  @Test
  void testAddAndReplaceSharingUser() throws JsonProcessingException, JsonPatchException {
    User userA = makeUser("A");
    manager.save(userA);
    DataElement dataElementA = createDataElement('A');
    manager.save(dataElementA);
    assertEquals(0, dataElementA.getSharing().getUsers().size());
    JsonPatch patch =
        jsonMapper.readValue(
            "["
                + "{\"op\": \"add\", \"path\": \"/sharing/users\", \"value\": "
                + "{"
                + "\""
                + userA.getUid()
                + "\": { \"access\":\"rw------\",\"id\": \""
                + userA.getUid()
                + "\" }"
                + "}"
                + "}"
                + "]",
            JsonPatch.class);
    assertNotNull(patch);
    DataElement patchedDE = jsonPatchManager.apply(patch, dataElementA);
    assertEquals(1, patchedDE.getSharing().getUsers().size());
    assertEquals("rw------", patchedDE.getSharing().getUsers().get(userA.getUid()).getAccess());
    JsonPatch replacedPatch =
        jsonMapper.readValue(
            "["
                + "{\"op\": \"replace\", \"path\": \"/sharing/users\", \"value\": "
                + "{"
                + "\""
                + userA.getUid()
                + "\": { \"access\":\"r-------\",\"id\": \""
                + userA.getUid()
                + "\" }"
                + "}"
                + "}"
                + "]",
            JsonPatch.class);
    DataElement replacePatchedDE = jsonPatchManager.apply(replacedPatch, patchedDE);
    assertEquals(1, replacePatchedDE.getSharing().getUsers().size());
    assertEquals(
        "r-------", replacePatchedDE.getSharing().getUsers().get(userA.getUid()).getAccess());
  }

  @Test
  void testAddAndRemoveSharingUser() throws JsonProcessingException, JsonPatchException {
    User userA = makeUser("A");
    manager.save(userA);
    DataElement dataElementA = createDataElement('A');
    manager.save(dataElementA);
    assertEquals(0, dataElementA.getSharing().getUsers().size());
    JsonPatch patch =
        jsonMapper.readValue(
            "["
                + "{\"op\": \"add\", \"path\": \"/sharing/users\", \"value\": "
                + "{"
                + "\""
                + userA.getUid()
                + "\": { \"access\":\"rw------\",\"id\": \""
                + userA.getUid()
                + "\" }"
                + "}"
                + "}"
                + "]",
            JsonPatch.class);
    assertNotNull(patch);
    DataElement patchedDE = jsonPatchManager.apply(patch, dataElementA);
    assertEquals(1, patchedDE.getSharing().getUsers().size());
    assertEquals("rw------", patchedDE.getSharing().getUsers().get(userA.getUid()).getAccess());
    JsonPatch removePatch =
        jsonMapper.readValue(
            "[" + "{\"op\": \"remove\", \"path\": \"/sharing/users/" + userA.getUid() + "\" } ]",
            JsonPatch.class);
    DataElement removedPatchedDE = jsonPatchManager.apply(removePatch, patchedDE);
    assertEquals(0, removedPatchedDE.getSharing().getUsers().size());
  }

  @Test
  void testReplaceNullProperty() throws JsonProcessingException, JsonPatchException {
    UserRole userRole = createUserRole("test", "ALL");
    userRole.setCode(null);
    manager.save(userRole);
    JsonPatch patch =
        jsonMapper.readValue(
            "[" + "{\"op\": \"replace\", \"path\": \"/code\", \"value\": \"updated\"}" + "]",
            JsonPatch.class);
    assertNotNull(patch);
    UserRole patchedUserRole = jsonPatchManager.apply(patch, userRole);
    assertEquals("updated", patchedUserRole.getCode());
  }

  @Test
  void testReplaceNotExistProperty() throws JsonProcessingException {
    UserRole userRole = createUserRole("test", "ALL");
    userRole.setCode(null);
    manager.save(userRole);
    JsonPatch patch =
        jsonMapper.readValue(
            "[" + "{\"op\": \"replace\", \"path\": \"/notexist\", \"value\": \"updated\"}" + "]",
            JsonPatch.class);
    assertNotNull(patch);
    assertThrows(JsonPatchException.class, () -> jsonPatchManager.apply(patch, userRole));
  }

  @Test
  void testAddNotExistProperty() throws JsonProcessingException {
    UserRole userRole = createUserRole("test", "ALL");
    userRole.setCode(null);
    manager.save(userRole);
    JsonPatch patch =
        jsonMapper.readValue(
            "[" + "{\"op\": \"add\", \"path\": \"/notexist\", \"value\": \"updated\"}" + "]",
            JsonPatch.class);
    assertNotNull(patch);
    assertThrows(JsonPatchException.class, () -> jsonPatchManager.apply(patch, userRole));
  }

  @Test
  void testRemoveNotExistProperty() throws JsonProcessingException {
    UserRole userRole = createUserRole("test", "ALL");
    userRole.setCode(null);
    manager.save(userRole);
    JsonPatch patch =
        jsonMapper.readValue(
            "[" + "{\"op\": \"remove\", \"path\": \"/notexist\", \"value\": \"updated\"}" + "]",
            JsonPatch.class);
    assertNotNull(patch);
    assertThrows(JsonPatchException.class, () -> jsonPatchManager.apply(patch, userRole));
  }

  @Test
  void testRemoveByIdNotExistProperty() throws JsonProcessingException {
    UserRole userRole = createUserRole("test", "ALL");
    userRole.setCode(null);
    manager.save(userRole);
    JsonPatch patch =
        jsonMapper.readValue(
            "[" + "{\"op\": \"remove\", \"path\": \"/notexist\", \"id\": \"uid\"}" + "]",
            JsonPatch.class);
    assertNotNull(patch);
    assertThrows(JsonPatchException.class, () -> jsonPatchManager.apply(patch, userRole));
  }

  @Test
  @DisplayName(
      "Scalar UserRole patch must not initialize lazy members (slow PATCH /userRoles invariant)")
  void testUserRoleScalarPatchDoesNotInitializeMembers() throws Exception {
    UserRole userRole = createUserRole("roleMembersLazy", "AUTH_A");
    manager.save(userRole);

    for (int i = 0; i < 4; i++) {
      User user = makeUser(String.valueOf((char) ('A' + i)));
      manager.save(user);
      userRole.addUser(user);
      manager.update(user);
    }
    manager.update(userRole);

    clearSession();

    UserRole reloaded = manager.get(UserRole.class, userRole.getUid());
    assertNotNull(reloaded);
    assertFalse(
        Hibernate.isInitialized(reloaded.getMembers()),
        "members should be lazy before patch apply");

    JsonPatch patch =
        jsonMapper.readValue(
            "[{\"op\": \"replace\", \"path\": \"/description\", \"value\": \"updated\"}]",
            JsonPatch.class);

    UserRole patched = jsonPatchManager.apply(patch, reloaded);

    assertEquals("updated", patched.getDescription());
    assertFalse(
        Hibernate.isInitialized(reloaded.getMembers()),
        "scalar patch must not initialize UserRole.members");
  }

  @Test
  @DisplayName("Owner collection authorities survive a name-only UserRole patch")
  void testUserRoleOwnerAuthoritiesPreservedOnNamePatch() throws Exception {
    UserRole userRole = createUserRole("roleOwnerAuths", "AUTH_A", "AUTH_B");
    manager.save(userRole);

    JsonPatch patch =
        jsonMapper.readValue(
            "[{\"op\": \"replace\", \"path\": \"/name\", \"value\": \"roleOwnerAuthsRenamed\"}]",
            JsonPatch.class);

    UserRole patched = jsonPatchManager.apply(patch, userRole);

    assertEquals("roleOwnerAuthsRenamed", patched.getName());
    assertEquals(Set.of("AUTH_A", "AUTH_B"), patched.getAuthorities());
  }

  @Test
  @DisplayName("Patch referencing non-owner /users path does not throw")
  void testUserRoleUsersPathPatchDoesNotThrow() throws Exception {
    UserRole userRole = createUserRole("roleUsersPath", "AUTH_A");
    manager.save(userRole);

    User user = makeUser("Z");
    manager.save(user);
    userRole.addUser(user);
    manager.update(user);
    manager.update(userRole);

    JsonPatch patch =
        jsonMapper.readValue(
            "[{\"op\": \"replace\", \"path\": \"/users\", \"value\": []}]", JsonPatch.class);

    UserRole patched = jsonPatchManager.apply(patch, userRole);
    assertNotNull(patched);
  }

  @Test
  @DisplayName(
      "Scalar OrganisationUnit patch must not initialize inverse children/users collections")
  void testOrganisationUnitScalarPatchDoesNotInitializeInverseCollections() throws Exception {
    OrganisationUnit parent = createOrganisationUnit('P');
    manager.save(parent);

    OrganisationUnit childA = createOrganisationUnit('A', parent);
    OrganisationUnit childB = createOrganisationUnit('B', parent);
    manager.save(childA);
    manager.save(childB);
    manager.update(parent);

    User user = makeUser("Z");
    user.addOrganisationUnit(parent);
    manager.save(user);
    manager.update(parent);

    clearSession();

    OrganisationUnit reloaded = manager.get(OrganisationUnit.class, parent.getUid());
    assertNotNull(reloaded);
    assertFalse(
        Hibernate.isInitialized(reloaded.getChildren()),
        "children should be lazy before patch apply");
    assertFalse(
        Hibernate.isInitialized(reloaded.getUsers()),
        "users should be lazy before patch apply");

    JsonPatch patch =
        jsonMapper.readValue(
            "[{\"op\": \"replace\", \"path\": \"/name\", \"value\": \"ParentRenamed\"}]",
            JsonPatch.class);

    OrganisationUnit patched = jsonPatchManager.apply(patch, reloaded);

    assertEquals("ParentRenamed", patched.getName());
    assertFalse(
        Hibernate.isInitialized(reloaded.getChildren()),
        "scalar patch must not initialize OrganisationUnit.children");
    assertFalse(
        Hibernate.isInitialized(reloaded.getUsers()),
        "scalar patch must not initialize OrganisationUnit.users");
  }

  @Test
  @DisplayName(
      "Scalar User patch must not initialize inverse userGroups; owner userRoles stay on patched object")
  void testUserScalarPatchDoesNotInitializeUserGroups() throws Exception {
    UserRole role = createUserRole("roleUserPatch", "AUTH_X");
    manager.save(role);

    User user = makeUser("G");
    user.getUserRoles().add(role);
    manager.save(user);

    UserGroup group = createUserGroup('G', Set.of(user));
    manager.save(group);
    // Keep both sides consistent for Hibernate inverse User.groups
    user.getGroups().add(group);
    manager.update(user);

    clearSession();

    User reloaded = manager.get(User.class, user.getUid());
    assertNotNull(reloaded);
    assertFalse(
        Hibernate.isInitialized(reloaded.getGroups()),
        "userGroups/groups should be lazy before patch apply");

    JsonPatch patch =
        jsonMapper.readValue(
            "[{\"op\": \"replace\", \"path\": \"/firstName\", \"value\": \"PatchedFirst\"}]",
            JsonPatch.class);

    User patched = jsonPatchManager.apply(patch, reloaded);

    assertEquals("PatchedFirst", patched.getFirstName());
    assertFalse(
        Hibernate.isInitialized(reloaded.getGroups()),
        "scalar patch must not initialize User.groups");
    assertNotNull(patched.getUserRoles());
    assertEquals(1, patched.getUserRoles().size(), "owner userRoles must survive scalar patch");
  }
}
