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
package org.hisp.dhis.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.hisp.dhis.common.IdentifiableObjectSnapshot;
import org.junit.jupiter.api.Test;

class UserInfoSnapshotTest {
  @Test
  void shouldBeEqualIfAllFieldsAreEqual() {
    UserInfoSnapshot a = UserInfoSnapshot.of(1, "alice", "aliceUid001", "alice", "Alice", "Aydin");
    UserInfoSnapshot b = UserInfoSnapshot.of(1, "alice", "aliceUid001", "alice", "Alice", "Aydin");

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void shouldNotBeEqualIfNamesDiffer() {
    UserInfoSnapshot before =
        UserInfoSnapshot.of(1, "alice", "aliceUid001", "alice", "Alice", "Aydin");
    UserInfoSnapshot renamed =
        UserInfoSnapshot.of(1, "alice", "aliceUid001", "alice", "Alice", "Okafor");

    assertNotEquals(before, renamed);
  }

  @Test
  void shouldNotBeEqualToIdentifiableObjectSnapshot() {
    UserInfoSnapshot user = UserInfoSnapshot.of(1, "alice", "aliceUid001", null, null, null);
    IdentifiableObjectSnapshot object = new IdentifiableObjectSnapshot();
    object.setId(1);
    object.setCode("alice");
    object.setUid("aliceUid001");

    assertNotEquals(object, user);
    assertNotEquals(user, object);
  }
}
