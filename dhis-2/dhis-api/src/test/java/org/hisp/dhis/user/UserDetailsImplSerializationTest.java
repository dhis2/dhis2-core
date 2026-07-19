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
package org.hisp.dhis.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.io.ObjectStreamClass;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Guards the session-compatibility contract of {@link UserDetailsImpl}: the serialVersionUID is
 * pinned to the pre-authz-stamp shape of the class, so HTTP sessions serialized by earlier releases
 * (Redis / persisted container sessions) deserialize cleanly with stamps defaulting to 0 — one soft
 * refresh on the next request instead of a forced re-login. If this test fails you changed the
 * serialized shape; do NOT re-pin without deciding the upgrade story.
 *
 * @author Morten Svanæs
 */
class UserDetailsImplSerializationTest {

  @Test
  void serialVersionUidIsPinnedToPreStampShape() {
    assertEquals(
        -6804263748578733471L,
        ObjectStreamClass.lookup(UserDetailsImpl.class).getSerialVersionUID());
  }

  @Test
  void withAuthzStampCopiesDataAndReplacesStamps() {
    UserDetails original =
        UserDetails.empty()
            .username("alice")
            .uid("u1")
            .userRoleIds(Set.of("r1"))
            .authzCheckedEpoch(3L)
            .authzGen(2L)
            .build();

    UserDetailsImpl stamped = ((UserDetailsImpl) original).withAuthzStamp(7L, 5L);

    assertNotSame(original, stamped);
    assertEquals("alice", stamped.getUsername());
    assertEquals("u1", stamped.getUid());
    assertEquals(Set.of("r1"), stamped.getUserRoleIds());
    assertEquals(7L, stamped.getAuthzCheckedEpoch());
    assertEquals(5L, stamped.getAuthzGen());
  }

  @Test
  void defaultStampsAreZeroMeaningUnknown() {
    UserDetails details = UserDetails.empty().username("bob").uid("u2").build();

    assertEquals(0L, details.getAuthzCheckedEpoch());
    assertEquals(0L, details.getAuthzGen());
  }
}
