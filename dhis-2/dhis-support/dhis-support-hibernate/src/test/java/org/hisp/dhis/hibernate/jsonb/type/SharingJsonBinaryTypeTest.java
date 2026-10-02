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
package org.hisp.dhis.hibernate.jsonb.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Jason P. Pickering <jason@dhis2.org>
 */
class SharingJsonBinaryTypeTest {

  private SharingJsonBinaryType type;

  @BeforeEach
  void setUp() {
    type = new SharingJsonBinaryType();
    type.init(Sharing.class);
  }

  @Test
  void deepCopyIsEqual() {
    Sharing sharing = sharing();
    Object copy = type.deepCopy(sharing);

    assertNotSame(sharing, copy);
    assertTrue(type.equals(sharing, copy));
  }

  @Test
  void differentOwnerIsNotEqual() {
    Sharing other = sharing();
    other.setOwner("otherOwner1");

    assertFalse(type.equals(sharing(), other));
  }

  @Test
  void differentPublicAccessIsNotEqual() {
    Sharing other = sharing();
    other.setPublicAccess("r-------");

    assertFalse(type.equals(sharing(), other));
  }

  @Test
  void differentUserAccessIsNotEqual() {
    Sharing other = sharing();
    other.getUsers().get("userA000001").setAccess("r-------");

    assertFalse(type.equals(sharing(), other));
  }

  @Test
  void additionalUserGroupAccessIsNotEqual() {
    Sharing other = sharing();
    other.addUserGroupAccess(new UserGroupAccess("rw------", "groupB00001"));

    assertFalse(type.equals(sharing(), other));
  }

  @Test
  void differentUserGroupAccessIsNotEqual() {
    Sharing other = sharing();
    other.getUserGroups().get("groupA00001").setAccess("rw------");

    assertFalse(type.equals(sharing(), other));
  }

  @Test
  void differentAccessIdUnderSameKeyIsNotEqual() {
    Sharing other = sharing();
    other.getUsers().get("userA000001").setId("userB000001");

    assertFalse(type.equals(sharing(), other));
  }

  @Test
  void displayNameIsNotPersistedSoItsCopyIsNotEqual() {
    // displayName is written to the JSON but not read back, so the snapshot never has it
    Sharing sharing = sharing();
    sharing.getUsers().get("userA000001").setDisplayName("User A");

    assertFalse(type.equals(sharing, type.deepCopy(sharing)));
  }

  @Test
  void nullAccessIsNotEqual() {
    Sharing withNullAccess = sharing();
    withNullAccess.getUsers().put("userA000001", null);

    assertFalse(type.equals(withNullAccess, sharing()));
    assertFalse(type.equals(sharing(), withNullAccess));
  }

  @Test
  void nullKeyIsLeftToContentBasedComparison() {
    // A null key cannot be serialised, so the content based comparison treats it as not equal
    Sharing a = sharing();
    a.getUsers().put(null, new UserAccess("rw------", "userB000001"));
    Sharing b = sharing();
    b.getUsers().put(null, new UserAccess("rw------", "userB000001"));

    assertFalse(type.equals(a, b));
  }

  @Test
  void agreesWithContentBasedComparison() {
    JsonBinaryType contentBased = new JsonBinaryType();
    contentBased.init(Sharing.class);
    Sharing withoutUsers = sharing();
    withoutUsers.setUsers(null);
    Sharing withEmptyUsers = sharing();
    withEmptyUsers.setUsers(new HashMap<>());
    Sharing withoutUserGroups = sharing();
    withoutUserGroups.setUserGroups(null);
    Sharing withEmptyUserGroups = sharing();
    withEmptyUserGroups.setUserGroups(new HashMap<>());
    Sharing otherOwner = sharing();
    otherOwner.setOwner("otherOwner1");
    Sharing withDisplayName = sharing();
    withDisplayName.getUserGroups().get("groupA00001").setDisplayName("Group A");

    List<Sharing> values =
        List.of(
            sharing(),
            withoutUsers,
            withEmptyUsers,
            withoutUserGroups,
            withEmptyUserGroups,
            otherOwner,
            withDisplayName);
    for (Sharing x : values) {
      for (Sharing y : values) {
        Object copy = type.deepCopy(y);
        assertEquals(contentBased.equals(x, copy), type.equals(x, copy), x + " vs " + y);
      }
    }
  }

  private static Sharing sharing() {
    Sharing sharing = new Sharing();
    sharing.setOwner("ownerA00001");
    sharing.setPublicAccess("rw------");
    sharing.addUserAccess(new UserAccess("rw------", "userA000001"));
    sharing.addUserGroupAccess(new UserGroupAccess("r-------", "groupA00001"));
    return sharing;
  }
}
