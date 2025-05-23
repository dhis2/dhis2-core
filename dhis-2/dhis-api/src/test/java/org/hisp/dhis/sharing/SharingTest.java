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
package org.hisp.dhis.sharing;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.function.UnaryOperator;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link Sharing} class, in particular the {@link Sharing#withAccess(UnaryOperator)}
 * operation.
 *
 * @author Jan Bernitt
 */
class SharingTest {

  @Test
  void withAccessAppliesToPublic() {
    Sharing original = new Sharing();
    original.setPublicAccess("abcd1234");
    assertEquals("abab1234", original.withAccess(Sharing::copyMetadataToData).getPublicAccess());
  }

  @Test
  void withAccessAppliesToUsers() {
    Sharing original = new Sharing();
    original.setUsers(singletonMap("key", new UserAccess("abcd1234", "uid")));
    Sharing actual = original.withAccess(Sharing::copyMetadataToData);
    Map<String, UserAccess> users = actual.getUsers();
    assertEquals(1, users.size());
    assertEquals("key", users.keySet().iterator().next());
    assertEquals("abab1234", users.values().iterator().next().getAccess());
    assertEquals("uid", users.values().iterator().next().getId());
  }

  @Test
  void withAccessAppliesToUserGroups() {
    Sharing original = new Sharing();
    original.setUserGroups(singletonMap("key", new UserGroupAccess("abcd1234", "uid")));
    Sharing actual = original.withAccess(Sharing::copyMetadataToData);
    Map<String, UserGroupAccess> groups = actual.getUserGroups();
    assertEquals(1, groups.size());
    assertEquals("key", groups.keySet().iterator().next());
    assertEquals("abab1234", groups.values().iterator().next().getAccess());
    assertEquals("uid", groups.values().iterator().next().getId());
  }

  @Test
  void withAccessKeepsOwner() {
    Sharing original = new Sharing();
    original.setOwner("userid");
    assertEquals("userid", original.withAccess(Sharing::copyMetadataToData).getOwner());
  }

  @Test
  void withAccessKeepsExternal() {
    Sharing original = new Sharing();
    original.setExternal(true);
    assertTrue(original.withAccess(Sharing::copyMetadataToData).isExternal());
  }

  @Test
  void setUserAccessesClearsExisting() {
    Sharing actual = new Sharing();
    actual.setUserAccesses(singleton(new UserAccess("rw------", "id")));
    actual.setUserAccesses(singleton(new UserAccess("r-------", "uid")));
    assertEquals(1, actual.getUsers().size());
    UserAccess userAccess = actual.getUsers().values().iterator().next();
    assertEquals("r-------", userAccess.getAccess());
    assertEquals("uid", userAccess.getId());
  }

  @Test
  void setUserGroupAccessClearsExisting() {
    Sharing actual = new Sharing();
    actual.setUserGroupAccess(singleton(new UserGroupAccess("rw------", "id")));
    actual.setUserGroupAccess(singleton(new UserGroupAccess("r-------", "uid")));
    assertEquals(1, actual.getUserGroups().size());
    UserGroupAccess userAccess = actual.getUserGroups().values().iterator().next();
    assertEquals("r-------", userAccess.getAccess());
    assertEquals("uid", userAccess.getId());
  }

  @Test
  void addUserAccessIgnoresNull() {
    Sharing actual = new Sharing();
    actual.setUserAccesses(singleton(new UserAccess("rw------", "uid")));
    actual.addUserAccess(null);
    assertEquals(1, actual.getUsers().size());
  }

  @Test
  void addUserGroupAccessCreatesMapWhenNeeded() {
    Sharing actual = new Sharing();
    actual.addUserGroupAccess(new UserGroupAccess("rw------", "uid"));
    assertEquals(1, actual.getUserGroups().size());
  }

  @Test
  void testCopySharingRead() {
    String source = "r-------";
    String result = Sharing.copyDataWrite(source);
    assertEquals("r-------", result);

    source = "rw------";
    result = Sharing.copyDataWrite(source);
    assertEquals("rw-w----", result);

    source = "rw-w----";
    result = Sharing.copyDataWrite(source);
    assertEquals("rw-w----", result);

    source = "rwr-----";
    result = Sharing.copyDataWrite(source);
    assertEquals("rwrw----", result);
  }

  @Test
  @DisplayName(
      "Users and UserGroups access string should be transformed using Sharing.withUserAndUserGroupAccess, public access must not be changed.")
  void testCopyWithAccessForSharingRead() {
    Sharing original = new Sharing();
    original.setPublicAccess("rw------");
    original.setUserAccesses(singleton(new UserAccess("rw------", "id")));
    original.setUserGroupAccess(singleton(new UserGroupAccess("r-------", "uid")));
    Sharing actual = original.withUserAndUserGroupAccess(Sharing::copyDataWrite);
    UserAccess userAccess = actual.getUsers().values().iterator().next();
    assertEquals("rw-w----", userAccess.getAccess());
    UserGroupAccess userGroupAccess = actual.getUserGroups().values().iterator().next();
    assertEquals("r-------", userGroupAccess.getAccess());
    assertEquals("rw------", actual.getPublicAccess());
  }
}
