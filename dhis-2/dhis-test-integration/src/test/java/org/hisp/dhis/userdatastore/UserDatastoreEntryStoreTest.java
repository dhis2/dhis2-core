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
package org.hisp.dhis.userdatastore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Stian Sandvold.
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class UserDatastoreEntryStoreTest extends PostgresIntegrationTestBase {

  @Autowired private UserDatastoreStore userDatastoreStore;

  @Autowired private UserService injectUserService;

  private User user;

  @BeforeAll
  void setUp() {
    this.userService = injectUserService;
    user = createUserAndInjectSecurityContext(true);
  }

  @Test
  void testAddUserKeyJsonValue() {
    UserDatastoreEntry userEntry = new UserDatastoreEntry();
    userEntry.setValue("{}");
    userEntry.setKey("test");
    userEntry.setNamespace("a");
    userEntry.setCreatedBy(user);
    userDatastoreStore.save(userEntry);
    long id = userEntry.getId();
    assertNotNull(userEntry);
    assertEquals(userEntry, userDatastoreStore.get(id));
  }

  @Test
  void testAddUserKeyJsonValuesAndGetNamespacesByUser() {
    UserDatastoreEntry userEntryA = new UserDatastoreEntry();
    userEntryA.setValue("{}");
    userEntryA.setNamespace("test_a");
    userEntryA.setKey("a");
    userEntryA.setCreatedBy(user);
    userDatastoreStore.save(userEntryA);
    UserDatastoreEntry userEntryB = new UserDatastoreEntry();
    userEntryB.setValue("{}");
    userEntryB.setNamespace("test_b");
    userEntryB.setKey("b");
    userEntryB.setCreatedBy(user);
    userDatastoreStore.save(userEntryB);
    List<String> list = userDatastoreStore.getNamespaces(user);
    assertTrue(list.contains("test_a"));
    assertTrue(list.contains("test_b"));
  }

  @Test
  void testAddUserKeyJsonValuesAndGetUserKeyJsonValuesByUser() {
    UserDatastoreEntry userEntryA = new UserDatastoreEntry();
    userEntryA.setValue("{}");
    userEntryA.setNamespace("a");
    userEntryA.setKey("test_a");
    userEntryA.setCreatedBy(user);
    userDatastoreStore.save(userEntryA);
    UserDatastoreEntry userEntryB = new UserDatastoreEntry();
    userEntryB.setValue("{}");
    userEntryB.setNamespace("a");
    userEntryB.setKey("test_b");
    userEntryB.setCreatedBy(user);
    userDatastoreStore.save(userEntryB);
    List<UserDatastoreEntry> list = userDatastoreStore.getEntriesInNamespace(user, "a");
    assertTrue(list.contains(userEntryA));
    assertTrue(list.contains(userEntryB));
  }

  /**
   * Reproduces DHIS2-21740: a user datastore entry must only be updated for its owning user. Two
   * users can each own an entry under the same namespace + key. Updating one user's value must not
   * change the other user's value.
   */
  @Test
  void testUpdateEntryOnlyAffectsOwningUser() {
    User userA = user;
    User userB = createUserWithAuth("otheruser");

    UserDatastoreEntry entryA = new UserDatastoreEntry();
    entryA.setNamespace("test");
    entryA.setKey("foo");
    entryA.setValue("{\"a\": \"999999\"}");
    entryA.setCreatedBy(userA);
    userDatastoreStore.save(entryA);

    UserDatastoreEntry entryB = new UserDatastoreEntry();
    entryB.setNamespace("test");
    entryB.setKey("foo");
    entryB.setValue("{\"a\": \"111111\"}");
    entryB.setCreatedBy(userB);
    userDatastoreStore.save(entryB);

    // user B updates the root value of their own "test"/"foo" entry
    userDatastoreStore.updateEntry(userB, "test", "foo", "{\"a\": \"0\"}", null, null);

    // the update runs as native SQL; clear the session so the entries are re-read from the DB
    entityManager.flush();
    entityManager.clear();

    UserDatastoreEntry updatedB = userDatastoreStore.getEntry(userB, "test", "foo");
    UserDatastoreEntry updatedA = userDatastoreStore.getEntry(userA, "test", "foo");
    assertNotNull(updatedA);
    assertNotNull(updatedB);

    // user B's own value was updated as requested
    assertTrue(
        updatedB.getValue().contains("0"),
        "user B's value should have been updated but was: " + updatedB.getValue());
    // user A's value must be untouched by user B's update
    assertTrue(
        updatedA.getValue().contains("999999"),
        "user A's value was overwritten by user B's update: " + updatedA.getValue());
  }
}
