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
package org.hisp.dhis.security.oauth2.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2Client;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;

class Dhis2OAuth2AuthorizationTest {

  @Test
  void shouldEqualAuthorizationWithSameUidCodeAndName() {
    Dhis2OAuth2Authorization authorization = authorization("AuthUid0001", "CODE", "Name");
    Dhis2OAuth2Authorization same = authorization("AuthUid0001", "CODE", "Name");
    same.setPrincipalName("other-principal");
    same.setAccessTokenValue("other-token");

    assertEquals(authorization, same);
    assertEquals(authorization.hashCode(), same.hashCode());
  }

  @Test
  void shouldNotEqualAuthorizationWithDifferentUidCodeOrName() {
    Dhis2OAuth2Authorization authorization = authorization("AuthUid0001", "CODE", "Name");

    assertNotEquals(authorization, authorization("AuthUid0002", "CODE", "Name"));
    assertNotEquals(authorization, authorization("AuthUid0001", "OTHER", "Name"));
    assertNotEquals(authorization, authorization("AuthUid0001", "CODE", "Other"));
  }

  @Test
  void shouldNotEqualOtherTypeWithSameUidCodeAndName() {
    Dhis2OAuth2Client client = new Dhis2OAuth2Client();
    client.setUid("AuthUid0001");
    client.setCode("CODE");
    client.setName("Name");

    assertNotEquals(authorization("AuthUid0001", "CODE", "Name"), client);
  }

  @Test
  void shouldKeepCreatorAndSetSharingOwnerOnSetUser() {
    User creator = new User();
    creator.setUid("CreatorUid1");
    User other = new User();
    other.setUid("OtherUid001");
    Dhis2OAuth2Authorization authorization = authorization("AuthUid0001", null, "Name");

    authorization.setUser(creator);
    authorization.setUser(other);

    assertSame(creator, authorization.getCreatedBy());
    assertEquals("OtherUid001", authorization.getSharing().getOwner());
  }

  private static Dhis2OAuth2Authorization authorization(String uid, String code, String name) {
    Dhis2OAuth2Authorization authorization = new Dhis2OAuth2Authorization();
    authorization.setUid(uid);
    authorization.setCode(code);
    authorization.setName(name);
    return authorization;
  }
}
