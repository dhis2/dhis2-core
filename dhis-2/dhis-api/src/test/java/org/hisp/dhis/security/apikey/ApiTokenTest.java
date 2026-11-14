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
package org.hisp.dhis.security.apikey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;

class ApiTokenTest {

  @Test
  void testTokensWithSameUidAndCodeAreEqual() {
    ApiToken a = token("apiTokenAAA", "codeA", "hash-1");
    ApiToken b = token("apiTokenAAA", "codeA", "hash-2");

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void testTokensWithDifferentUidOrCodeAreNotEqual() {
    ApiToken a = token("apiTokenAAA", "codeA", "hash-1");

    assertNotEquals(a, token("apiTokenBBB", "codeA", "hash-1"));
    assertNotEquals(a, token("apiTokenAAA", "codeB", "hash-1"));
  }

  @Test
  void testToStringDoesNotContainKey() {
    assertFalse(token("apiTokenAAA", "codeA", "secret-hash").toString().contains("secret-hash"));
  }

  @Test
  void testSetUserKeepsExistingCreatorAndMakesUserTheOwner() {
    User creator = new User();
    creator.setUid("creatorUid1");
    User other = new User();
    other.setUid("otherUid111");
    ApiToken token = token("apiTokenAAA", null, "hash");

    token.setUser(creator);
    token.setUser(other);

    assertEquals(creator, token.getCreatedBy());
    assertEquals("otherUid111", token.getSharing().getOwner());
  }

  private static ApiToken token(String uid, String code, String key) {
    ApiToken token = ApiToken.builder().key(key).code(code).build();
    token.setUid(uid);
    return token;
  }
}
