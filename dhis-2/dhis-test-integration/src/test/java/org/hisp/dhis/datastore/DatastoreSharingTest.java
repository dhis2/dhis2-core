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
package org.hisp.dhis.datastore;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.user.CurrentUserDetails;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

/**
 * @author david mackessy
 */
class DatastoreSharingTest extends SingleSetupIntegrationTestBase {

  @Autowired private DatastoreService datastoreService;
  @Autowired private UserService _userService;
  @Autowired private UserGroupService userGroupService;
  @Autowired private ObjectMapper jsonMapper;

  private static final String NAMESPACE = "FOOTBALL";

  @Override
  protected void setUpTest() throws Exception {
    userService = _userService;
    createAndInjectAdminUser();
  }

  @BeforeEach
  final void setup() {
    clearSecurityContext();
    injectAdminUser();
  }

  @Test
  void testGetNamespaceKeys_DefaultPublicAccess()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given
    // 2 existing namespace entries with default public sharing access 'rw------'
    User basicUser = createAndAddUser(false, "basicUser", null);

    String arsenal = jsonMapper.writeValueAsString(club("arsenal"));
    String spurs = jsonMapper.writeValueAsString(club("spurs"));

    addEntry("arsenal", arsenal);
    addEntry("spurs", spurs);

    // when
    // a basic user without explicit access tries to get namespace keys
    injectSecurityContext(basicUser);
    CurrentUserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    assertFalse(currentUserDetails.isSuper());
    assertEquals("basicUser", currentUserDetails.getUsername());
    List<String> keysInNamespace = datastoreService.getKeysInNamespace(NAMESPACE, null);

    // then
    // the basic user should be able to retrieve all keys from the namespace
    assertNotNull(keysInNamespace);
    assertEquals(2, keysInNamespace.size());
    assertTrue(keysInNamespace.containsAll(List.of("arsenal", "spurs")));
  }

  @Test
  @DisplayName("basic user update with default public access")
  void updateWithDefaultPublicAccess() throws ConflictException, BadRequestException {
    // given an existing entry
    Dog entry = new Dog("1", "Zeus", "Brown");
    DatastoreEntry entry1 = addEntry(entry.getId(), entry);

    // when a basic user tries to update the entry
    User basicUser = createAndAddUser(false, "basicUser", null);
    injectSecurityContext(basicUser);
    Dog entryUpdate = new Dog("1", "Athena", "Black");
    entry1.setValue(mapValueToJson(entryUpdate));

    // then no auth error is thrown
    assertDoesNotThrow(() -> datastoreService.updateEntry(entry1));
  }

  @Test
  void testGetNamespaceKeys_NoPublicAccess_SuperUser()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given
    // 2 existing namespace entries with no sharing public access
    User superuser = createAndAddUser(true, "superUser1", null);

    String arsenal = jsonMapper.writeValueAsString(club("arsenal"));
    String spurs = jsonMapper.writeValueAsString(club("spurs"));

    DatastoreEntry entry1 = addEntry("arsenal", arsenal);
    DatastoreEntry entry2 = addEntry("spurs", spurs);

    removePublicAccess(entry1);
    removePublicAccess(entry2);

    // when
    // a superuser without explicit access tries to get namespace keys
    injectSecurityContext(superuser);
    CurrentUserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    assertTrue(currentUserDetails.isSuper());
    assertEquals("superUser1", currentUserDetails.getUsername());
    List<String> keysInNamespace = datastoreService.getKeysInNamespace(NAMESPACE, null);

    // then
    // the super user should be able to retrieve all keys from the namespace
    assertNotNull(keysInNamespace);
    assertEquals(2, keysInNamespace.size());
    assertTrue(keysInNamespace.containsAll(List.of("arsenal", "spurs")));
  }

  @Test
  void testGetNamespaceKeys_NoPublicAccess_FullUserAccess()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given
    // 2 existing namespace entries with sharing set to userWithFullAccess & no public access
    User basicUser = createAndAddUser(false, "basicUser", null);
    User userWithFullAccess = createAndAddUser(false, "userWithFullAccess", null);
    injectSecurityContext(basicUser);

    String arsenal = jsonMapper.writeValueAsString(club("arsenal"));
    String spurs = jsonMapper.writeValueAsString(club("spurs"));

    DatastoreEntry entry1 = addEntry("arsenal", arsenal);
    DatastoreEntry entry2 = addEntry("spurs", spurs);

    removePublicAccess(entry1);
    removePublicAccess(entry2);

    enableDataSharing(userWithFullAccess, entry1, "r-------");
    enableDataSharing(userWithFullAccess, entry2, "r-------");

    // when
    // a user with full access tries to get namespace keys
    injectSecurityContext(userWithFullAccess);
    CurrentUserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    assertFalse(currentUserDetails.isSuper());
    assertEquals("userWithFullAccess", currentUserDetails.getUsername());
    List<String> keysInNamespace = datastoreService.getKeysInNamespace(NAMESPACE, null);

    // then
    // the user should be able to retrieve all keys from the namespace
    assertNotNull(keysInNamespace);
    assertEquals(2, keysInNamespace.size());
    assertTrue(keysInNamespace.containsAll(List.of("arsenal", "spurs")));
  }

  @Test
  @DisplayName("user update with no default public access and user has user sharing access")
  void updateWithNoDefaultPublicAccessUserHasAccess()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given an existing entry with no public access and user has sharing access
    User userWithFullAccess = createAndAddUser(false, "userWithFullAccess", null);
    String arsenal = jsonMapper.writeValueAsString(club("arsenal"));
    DatastoreEntry entry1 = addEntry("arsenal", arsenal);
    removePublicAccess(entry1);
    enableDataSharing(userWithFullAccess, entry1, "-w------");

    // when a basic user tries to update the entry
    injectSecurityContext(userWithFullAccess);

    // then no access error is thrown
    assertDoesNotThrow(() -> datastoreService.updateEntry(entry1));
  }

  @Test
  @DisplayName("user update with no default public access and user has group sharing access")
  void updateWithNoDefaultPublicAccessUserHasGroupAccess()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given an existing entry with no public access and user has group sharing access
    User userWithUserGroupAccess = createAndAddUser(false, "userWithUserGroupAccess", null);
    UserGroup userGroup = createUserGroup('a', Set.of(userWithUserGroupAccess));
    userWithUserGroupAccess.getGroups().add(userGroup);
    injectAdminUser();
    _userService.updateUser(userWithUserGroupAccess);
    userGroupService.addUserGroup(userGroup);

    String arsenal = jsonMapper.writeValueAsString(club("arsenal"));
    DatastoreEntry entry1 = addEntry("arsenal", arsenal);
    removePublicAccess(entry1);
    enableDataSharingWithUserGroup(userGroup, entry1, "-w------");

    // when a basic user tries to update the entry
    injectSecurityContext(userWithUserGroupAccess);

    // then no access error is thrown
    assertDoesNotThrow(() -> datastoreService.updateEntry(entry1));
  }

  @Test
  @DisplayName("user update with no default public access and user has no user sharing access")
  void updateWithNoDefaultPublicAccessUserHasNoAccess()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given an existing entry with no public access and user has sharing access
    User userWithNoAccess = createAndAddUser(false, "userWithNoAccess", null);
    String arsenal = jsonMapper.writeValueAsString(club("arsenal"));
    DatastoreEntry entry1 = addEntry("arsenal", arsenal);
    removePublicAccess(entry1);

    // when a basic user tries to update the entry
    injectSecurityContext(userWithNoAccess);
    entry1.setValue(mapValueToJson(club("arsenal update 4")));

    // then an access error is thrown
    assertThrows(AccessDeniedException.class, () -> datastoreService.updateEntry(entry1));
  }

  @Test
  void testGetNamespaceKeys_NoPublicAccess_NoUserAccess()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given
    // 2 existing namespace entries with sharing set to basicUser only & no public access
    User basicUser = createAndAddUser(false, "basicUser", null);
    User userWithNoAccess = createAndAddUser(false, "userWithNoAccess", null);
    injectSecurityContext(basicUser);

    String arsenal = jsonMapper.writeValueAsString(club("arsenal"));
    String spurs = jsonMapper.writeValueAsString(club("spurs"));

    DatastoreEntry entry1 = addEntry("arsenal", arsenal);
    DatastoreEntry entry2 = addEntry("spurs", spurs);

    removePublicAccess(entry1);
    removePublicAccess(entry2);

    enableDataSharing(basicUser, entry1, "r-------");
    enableDataSharing(basicUser, entry2, "r-------");

    // when
    // a user with no explicit access tries to get namespace keys
    injectSecurityContext(userWithNoAccess);
    CurrentUserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    assertFalse(currentUserDetails.isSuper());
    assertEquals("userWithNoAccess", currentUserDetails.getUsername());
    List<String> keysInNamespace = datastoreService.getKeysInNamespace(NAMESPACE, null);

    // then
    // the user should not be able to retrieve any keys from the namespace
    assertNotNull(keysInNamespace);
    assertEquals(0, keysInNamespace.size());
  }

  @Test
  void testGetNamespaceKeys_NoPublicAccess_UserAccessOnOneEntryOnly()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given
    // 2 existing namespace entries with sharing set to nonSuperUser2 on 1 entry only
    User basicUser = createAndAddUser(false, "basicUser", null);
    User userWithSomeAccess = createAndAddUser(false, "userWithSomeAccess", null);
    injectSecurityContext(basicUser);

    String arsenal = jsonMapper.writeValueAsString(club("arsenal"));
    String spurs = jsonMapper.writeValueAsString(club("spurs"));

    DatastoreEntry entry1 = addEntry("arsenal", arsenal);
    DatastoreEntry entry2 = addEntry("spurs", spurs);

    removePublicAccess(entry1);
    removePublicAccess(entry2);

    enableDataSharing(userWithSomeAccess, entry1, "r-------");

    // when
    // a user with access to one entry tries to get namespace keys
    injectSecurityContext(userWithSomeAccess);
    CurrentUserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    assertFalse(currentUserDetails.isSuper());
    assertEquals("userWithSomeAccess", currentUserDetails.getUsername());
    List<String> keysInNamespace = datastoreService.getKeysInNamespace(NAMESPACE, null);

    // then
    // the user should be able to retrieve only one key from the namespace
    assertNotNull(keysInNamespace);
    assertEquals(1, keysInNamespace.size());
  }

  @Test
  void testGetNamespaceKeys_NoPublicAccess_FullUserGroupAccess()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given
    // 2 existing namespace entries with sharing set a specific user group only & no public access
    User basicUser = createAndAddUser(false, "basicUser", null);
    User userWithUserGroupAccess = createAndAddUser(false, "userWithUserGroupAccess", null);
    UserGroup userGroup = createUserGroup('a', Set.of(userWithUserGroupAccess));
    userWithUserGroupAccess.getGroups().add(userGroup);
    injectAdminUser();
    _userService.updateUser(userWithUserGroupAccess);
    userGroupService.addUserGroup(userGroup);
    injectSecurityContext(basicUser);

    String arsenal = jsonMapper.writeValueAsString(club("arsenal"));
    String spurs = jsonMapper.writeValueAsString(club("spurs"));

    DatastoreEntry entry1 = addEntry("arsenal", arsenal);
    DatastoreEntry entry2 = addEntry("spurs", spurs);

    removePublicAccess(entry1);
    removePublicAccess(entry2);

    enableDataSharingWithUserGroup(userGroup, entry1, "r-------");
    enableDataSharingWithUserGroup(userGroup, entry2, "r-------");

    // when
    // a user with user group access tries to get namespace keys
    injectSecurityContext(userWithUserGroupAccess);
    CurrentUserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    assertFalse(currentUserDetails.isSuper());
    assertEquals("userWithUserGroupAccess", currentUserDetails.getUsername());
    List<String> keysInNamespace = datastoreService.getKeysInNamespace(NAMESPACE, null);

    // then
    // the user should be able to retrieve all keys from the namespace
    assertNotNull(keysInNamespace);
    assertEquals(2, keysInNamespace.size());
    assertTrue(keysInNamespace.containsAll(List.of("arsenal", "spurs")));
  }

  @Test
  void testGetNamespaceKeys_NoPublicAccess_NoUserGroupAccess()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given
    // 2 existing namespace entries with sharing set to a specific user group only & no public
    // access
    User basicUser = createAndAddUser(false, "basicUser", null);
    User userWithNoAccess = createAndAddUser(false, "userWithNoAccess", null);
    UserGroup userGroup = createUserGroup('a', Set.of(basicUser));
    injectAdminUser();
    userGroupService.addUserGroup(userGroup);
    injectSecurityContext(basicUser);

    String arsenal = jsonMapper.writeValueAsString(club("arsenal"));
    String spurs = jsonMapper.writeValueAsString(club("spurs"));

    DatastoreEntry entry1 = addEntry("arsenal", arsenal);
    DatastoreEntry entry2 = addEntry("spurs", spurs);

    removePublicAccess(entry1);
    removePublicAccess(entry2);

    enableDataSharingWithUserGroup(userGroup, entry1, "r-------");
    enableDataSharingWithUserGroup(userGroup, entry2, "r-------");

    // when
    // a user with no access tries to get namespace keys
    injectSecurityContext(userWithNoAccess);
    CurrentUserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    assertFalse(currentUserDetails.isSuper());
    assertEquals("userWithNoAccess", currentUserDetails.getUsername());
    List<String> keysInNamespace = datastoreService.getKeysInNamespace(NAMESPACE, null);

    // then
    // the user should not be able to retrieve any keys from the namespace
    assertNotNull(keysInNamespace);
    assertEquals(0, keysInNamespace.size());
  }

  @Test
  void testGetNamespaceKeys_NoPublicAccess_UserGroupAccessOnOneEntryOnly()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given
    // 2 existing namespace entries with sharing set to userWithSomeAccess on 1 entry only
    User basicUser = createAndAddUser(false, "basicUser", null);
    User userWithSomeAccess = createAndAddUser(false, "userWithSomeAccess", null);
    UserGroup userGroup = createUserGroup('a', Set.of(userWithSomeAccess));
    userWithSomeAccess.getGroups().add(userGroup);
    injectAdminUser();
    _userService.updateUser(userWithSomeAccess);
    userGroupService.addUserGroup(userGroup);
    injectSecurityContext(basicUser);

    String arsenal = jsonMapper.writeValueAsString(club("arsenal"));
    String spurs = jsonMapper.writeValueAsString(club("spurs"));

    DatastoreEntry entry1 = addEntry("arsenal", arsenal);
    DatastoreEntry entry2 = addEntry("spurs", spurs);

    removePublicAccess(entry1);
    removePublicAccess(entry2);

    enableDataSharingWithUserGroup(userGroup, entry1, "r-------");

    // when
    // a user with group access for one entry tries to get namespace keys
    injectSecurityContext(userWithSomeAccess);
    CurrentUserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    assertFalse(currentUserDetails.isSuper());
    assertEquals("userWithSomeAccess", currentUserDetails.getUsername());
    List<String> keysInNamespace = datastoreService.getKeysInNamespace(NAMESPACE, null);

    // then
    // the user should be able to retrieve only one key from the namespace
    assertNotNull(keysInNamespace);
    assertEquals(1, keysInNamespace.size());
  }

  private <T> DatastoreEntry addEntry(String key, T object)
      throws ConflictException, BadRequestException {
    DatastoreEntry entry = new DatastoreEntry(NAMESPACE, key, mapValueToJson(object), false);
    datastoreService.addEntry(entry);
    return entry;
  }

  private <T> String mapValueToJson(T object) {
    try {
      return jsonMapper.writeValueAsString(object);
    } catch (JsonProcessingException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private String club(String club) {
    return String.format("{" + "\"name\": \"%s\"," + "\"league\": \"prem\"" + "}", club);
  }
}
