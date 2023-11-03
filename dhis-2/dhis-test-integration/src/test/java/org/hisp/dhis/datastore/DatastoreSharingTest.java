/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author david mackessy
 */
class DatastoreSharingTest extends SingleSetupIntegrationTestBase {

  @Autowired private DatastoreService datastoreService;
  @Autowired private UserService _userService;
  @Autowired private UserGroupService userGroupService;
  @Autowired private ObjectMapper jsonMapper;

  private static final String NAMESPACE = "FOOTBALL";

  @BeforeAll
  public void init() {
    this.userService = _userService;
  }

  @Test
  void testGetNamespaceKeys_SuperUser()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given
    // 2 existing namespace entries with sharing set to nonSuperUser only
    User basicUser = createAndAddUser(false, "basicUser", null);
    User superuser = createAndAddUser(true, "superUser1", null);

    String arsenal = jsonMapper.writeValueAsString(club("arsenal"));
    String spurs = jsonMapper.writeValueAsString(club("spurs"));

    DatastoreEntry entry1 = addEntry("arsenal", arsenal);
    DatastoreEntry entry2 = addEntry("spurs", spurs);

    enableDataSharing(basicUser, entry1, "--r-----");
    enableDataSharing(basicUser, entry2, "--r-----");

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
  void testGetNamespaceKeys_FullUserAccess()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given
    // 2 existing namespace entries with sharing set to nonSuperUser2 only
    User basicUser = createAndAddUser(false, "basicUser", null);
    User userWithFullAccess = createAndAddUser(false, "userWithFullAccess", null);
    injectSecurityContext(basicUser);

    String arsenal = jsonMapper.writeValueAsString(club("arsenal"));
    String spurs = jsonMapper.writeValueAsString(club("spurs"));

    DatastoreEntry entry1 = addEntry("arsenal", arsenal);
    DatastoreEntry entry2 = addEntry("spurs", spurs);

    enableDataSharing(userWithFullAccess, entry1, "--r-----");
    enableDataSharing(userWithFullAccess, entry2, "--r-----");

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
  void testGetNamespaceKeys_NoUserAccess()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given
    // 2 existing namespace entries with sharing set to basicUser only
    User basicUser = createAndAddUser(false, "basicUser", null);
    User userWithNoAccess = createAndAddUser(false, "userWithNoAccess", null);
    injectSecurityContext(basicUser);

    String arsenal = jsonMapper.writeValueAsString(club("arsenal"));
    String spurs = jsonMapper.writeValueAsString(club("spurs"));

    DatastoreEntry entry1 = addEntry("arsenal", arsenal);
    DatastoreEntry entry2 = addEntry("spurs", spurs);

    enableDataSharing(basicUser, entry1, "--r-----");
    enableDataSharing(basicUser, entry2, "--r-----");

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
  void testGetNamespaceKeys_UserAccessOnOneEntryOnly()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given
    // 2 existing namespace entries with sharing set to nonSuperUser2 on 1 entry only
    User basicUser = createAndAddUser(false, "basicUser", null);
    User userWithSomeAccess = createAndAddUser(false, "userWithSomeAccess", null);
    injectSecurityContext(basicUser);

    String arsenal = jsonMapper.writeValueAsString(club("arsenal"));
    String spurs = jsonMapper.writeValueAsString(club("spurs"));

    DatastoreEntry entry1 = addEntry("arsenal", arsenal);
    addEntry("spurs", spurs);
    enableDataSharing(userWithSomeAccess, entry1, "--r-----");

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
  void testGetNamespaceKeys_FullUserGroupAccess()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given
    // 2 existing namespace entries with sharing set a specific user group only
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

    enableDataSharingWithUserGroup(userGroup, entry1, "--r-----");
    enableDataSharingWithUserGroup(userGroup, entry2, "--r-----");

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
  void testGetNamespaceKeys_NoUserGroupAccess()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given
    // 2 existing namespace entries with sharing set to a specific user group only
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

    enableDataSharingWithUserGroup(userGroup, entry1, "--r-----");
    enableDataSharingWithUserGroup(userGroup, entry2, "--r-----");

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
  void testGetNamespaceKeys_UserGroupAccessOnOneEntryOnly()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given
    // 2 existing namespace entries with sharing set to nonSuperUser2 on 1 entry only
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
    addEntry("spurs", spurs);
    enableDataSharingWithUserGroup(userGroup, entry1, "--r-----");

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
    return """
      {
        "name": "%s",
        "league": "prem"
      }
    """
        .formatted(club)
        .strip();
  }
}
