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
  void testGetNamespaceKeysAsSuperUser()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given
    // 2 existing namespace entries with sharing set to nonSuperUser only
    User nonSuperUser = createAndAddUser(false, "nonSuperUser1", null);
    User superuser = createAndAddUser(true, "superUser1", null);

    String arsenal = jsonMapper.writeValueAsString(club("arsenal"));
    String spurs = jsonMapper.writeValueAsString(club("spurs"));

    DatastoreEntry entry1 = addEntry("arsenal", arsenal);
    DatastoreEntry entry2 = addEntry("spurs", spurs);

    enableDataSharing(nonSuperUser, entry1, "--r-----");
    enableDataSharing(nonSuperUser, entry2, "--r-----");

    // when
    // a superuser without explicit access tries to get namespace keys
    injectSecurityContext(superuser);
    CurrentUserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    assertTrue(currentUserDetails.isSuper());
    List<String> keysInNamespace = datastoreService.getKeysInNamespace(NAMESPACE, null);

    // then
    // the super user should be able to retrieve all keys from the namespace
    assertNotNull(keysInNamespace);
    assertEquals(2, keysInNamespace.size());
    assertTrue(keysInNamespace.containsAll(List.of("arsenal", "spurs")));
  }

  @Test
  void testGetNamespaceKeysWithFullUserAccess()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given
    // 2 existing namespace entries with sharing set to nonSuperUser2 only
    User nonSuperUser1 = createAndAddUser(false, "nonSuperUser1", null);
    User nonSuperUser2 = createAndAddUser(false, "nonSuperUser2", null);
    injectSecurityContext(nonSuperUser1);

    String arsenal = jsonMapper.writeValueAsString(club("arsenal"));
    String spurs = jsonMapper.writeValueAsString(club("spurs"));

    DatastoreEntry entry1 = addEntry("arsenal", arsenal);
    DatastoreEntry entry2 = addEntry("spurs", spurs);

    enableDataSharing(nonSuperUser2, entry1, "--r-----");
    enableDataSharing(nonSuperUser2, entry2, "--r-----");

    // when
    // a non-superuser with explicit user access tries to get namespace keys
    injectSecurityContext(nonSuperUser2);
    CurrentUserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    assertFalse(currentUserDetails.isSuper());
    List<String> keysInNamespace = datastoreService.getKeysInNamespace(NAMESPACE, null);

    // then
    // the user should be able to retrieve all keys from the namespace
    assertNotNull(keysInNamespace);
    assertEquals(2, keysInNamespace.size());
    assertTrue(keysInNamespace.containsAll(List.of("arsenal", "spurs")));
  }

  @Test
  void testGetNamespaceKeysWithNoUserAccess()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given
    // 2 existing namespace entries with sharing set to nonSuperUser1 only
    User nonSuperUser1 = createAndAddUser(false, "nonSuperUser1", null);
    User nonSuperUser2 = createAndAddUser(false, "nonSuperUser2", null);
    injectSecurityContext(nonSuperUser1);

    String arsenal = jsonMapper.writeValueAsString(club("arsenal"));
    String spurs = jsonMapper.writeValueAsString(club("spurs"));

    DatastoreEntry entry1 = addEntry("arsenal", arsenal);
    DatastoreEntry entry2 = addEntry("spurs", spurs);

    enableDataSharing(nonSuperUser1, entry1, "--r-----");
    enableDataSharing(nonSuperUser1, entry2, "--r-----");

    // when
    // a non-superuser with no explicit user access tries to get namespace keys
    injectSecurityContext(nonSuperUser2);
    CurrentUserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    assertFalse(currentUserDetails.isSuper());
    List<String> keysInNamespace = datastoreService.getKeysInNamespace(NAMESPACE, null);

    // then
    // the user should not be able to retrieve all keys from the namespace
    assertNotNull(keysInNamespace);
    assertEquals(0, keysInNamespace.size());
  }

  @Test
  void testGetNamespaceKeysWithUserAccessOnOneEntryOnly()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given
    // 2 existing namespace entries with sharing set to nonSuperUser2 on 1 entry only
    User nonSuperUser1 = createAndAddUser(false, "nonSuperUser1", null);
    User nonSuperUser2 = createAndAddUser(false, "nonSuperUser2", null);
    injectSecurityContext(nonSuperUser1);

    String arsenal = jsonMapper.writeValueAsString(club("arsenal"));
    String spurs = jsonMapper.writeValueAsString(club("spurs"));

    DatastoreEntry entry1 = addEntry("arsenal", arsenal);
    addEntry("spurs", spurs);
    enableDataSharing(nonSuperUser2, entry1, "--r-----");

    // when
    // a non-superuser with explicit user access for one entry tries to get namespace keys
    injectSecurityContext(nonSuperUser2);
    CurrentUserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    assertFalse(currentUserDetails.isSuper());
    List<String> keysInNamespace = datastoreService.getKeysInNamespace(NAMESPACE, null);

    // then
    // the user should be able to retrieve only one key from the namespace
    assertNotNull(keysInNamespace);
    assertEquals(1, keysInNamespace.size());
  }

  @Test
  void testGetNamespaceKeysWithFullUserGroupAccess()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given
    // 2 existing namespace entries with sharing set a specific user group only
    User nonSuperUser1 = createAndAddUser(false, "nonSuperUser1", null);
    User nonSuperUser2 = createAndAddUser(false, "nonSuperUser2", null);
    UserGroup userGroup = createUserGroup('a', Set.of(nonSuperUser2));
    injectAdminUser();
    userGroupService.addUserGroup(userGroup);
    injectSecurityContext(nonSuperUser1);

    String arsenal = jsonMapper.writeValueAsString(club("arsenal"));
    String spurs = jsonMapper.writeValueAsString(club("spurs"));

    DatastoreEntry entry1 = addEntry("arsenal", arsenal);
    DatastoreEntry entry2 = addEntry("spurs", spurs);

    enableDataSharingWithUserGroup(userGroup, entry1, "--r-----");
    enableDataSharingWithUserGroup(userGroup, entry2, "--r-----");

    // when
    // a non-superuser with explicit user group access tries to get namespace keys
    injectSecurityContext(nonSuperUser2);
    CurrentUserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    assertFalse(currentUserDetails.isSuper());
    List<String> keysInNamespace = datastoreService.getKeysInNamespace(NAMESPACE, null);

    // then
    // the user should be able to retrieve all keys from the namespace
    assertNotNull(keysInNamespace);
    assertEquals(2, keysInNamespace.size());
    assertTrue(keysInNamespace.containsAll(List.of("arsenal", "spurs")));
  }

  @Test
  void testGetNamespaceKeysWithNoUserGroupAccess()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given
    // 2 existing namespace entries with sharing set to a specific user group only
    User nonSuperUser1 = createAndAddUser(false, "nonSuperUser1", null);
    User nonSuperUser2 = createAndAddUser(false, "nonSuperUser2", null);
    UserGroup userGroup = createUserGroup('a', Set.of(nonSuperUser1));
    injectAdminUser();
    userGroupService.addUserGroup(userGroup);
    injectSecurityContext(nonSuperUser1);

    String arsenal = jsonMapper.writeValueAsString(club("arsenal"));
    String spurs = jsonMapper.writeValueAsString(club("spurs"));

    DatastoreEntry entry1 = addEntry("arsenal", arsenal);
    DatastoreEntry entry2 = addEntry("spurs", spurs);

    enableDataSharingWithUserGroup(userGroup, entry1, "--r-----");
    enableDataSharingWithUserGroup(userGroup, entry2, "--r-----");

    // when
    // a non-superuser with no explicit user access tries to get namespace keys
    injectSecurityContext(nonSuperUser2);
    CurrentUserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    assertFalse(currentUserDetails.isSuper());
    List<String> keysInNamespace = datastoreService.getKeysInNamespace(NAMESPACE, null);

    // then
    // the user should not be able to retrieve all keys from the namespace
    assertNotNull(keysInNamespace);
    assertEquals(0, keysInNamespace.size());
  }

  @Test
  void testGetNamespaceKeysWithUserGroupAccessOnOneEntryOnly()
      throws ConflictException, BadRequestException, JsonProcessingException {
    // given
    // 2 existing namespace entries with sharing set to nonSuperUser2 on 1 entry only
    User nonSuperUser1 = createAndAddUser(false, "nonSuperUser1", null);
    User nonSuperUser2 = createAndAddUser(false, "nonSuperUser2", null);
    UserGroup userGroup = createUserGroup('a', Set.of(nonSuperUser2));
    injectAdminUser();
    userGroupService.addUserGroup(userGroup);
    injectSecurityContext(nonSuperUser1);

    String arsenal = jsonMapper.writeValueAsString(club("arsenal"));
    String spurs = jsonMapper.writeValueAsString(club("spurs"));

    DatastoreEntry entry1 = addEntry("arsenal", arsenal);
    addEntry("spurs", spurs);
    enableDataSharingWithUserGroup(userGroup, entry1, "--r-----");

    // when
    // a non-superuser with explicit user group access for one entry tries to get namespace keys
    injectSecurityContext(nonSuperUser2);
    CurrentUserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    assertFalse(currentUserDetails.isSuper());
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
        "league: "prem"
      }
    """
        .formatted(club)
        .strip();
  }
}
