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
package org.hisp.dhis.security.oauth2.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hisp.dhis.common.IdScheme;
import org.junit.jupiter.api.Test;

class Dhis2OAuth2ClientTest {

  @Test
  void shouldEqualClientWithSameUidCodeAndName() {
    Dhis2OAuth2Client client = client("ClientUid01", "CODE", "Name", "client-a");
    Dhis2OAuth2Client same = client("ClientUid01", "CODE", "Name", "client-b");

    assertEquals(client, same);
    assertEquals(client.hashCode(), same.hashCode());
  }

  @Test
  void shouldNotEqualClientWithDifferentUidCodeOrName() {
    Dhis2OAuth2Client client = client("ClientUid01", "CODE", "Name", "client-a");

    assertNotEquals(client, client("ClientUid02", "CODE", "Name", "client-a"));
    assertNotEquals(client, client("ClientUid01", "OTHER", "Name", "client-a"));
    assertNotEquals(client, client("ClientUid01", "CODE", "Other", "client-a"));
  }

  @Test
  void shouldCompareNameFallbackToClientIdInEquality() {
    Dhis2OAuth2Client withoutName = client("ClientUid01", "CODE", null, "client-a");
    Dhis2OAuth2Client namedLikeClientId = client("ClientUid01", "CODE", "client-a", "client-b");

    assertEquals(withoutName, namedLikeClientId);
    assertEquals(withoutName.hashCode(), namedLikeClientId.hashCode());
  }

  @Test
  void shouldFallBackToClientIdWhenNameIsMissing() {
    Dhis2OAuth2Client client = client("ClientUid01", null, "", "client-a");

    assertEquals("client-a", client.getName());
    assertEquals("client-a", client.getDisplayName());
    assertEquals("", client.getRawName());
  }

  @Test
  void shouldTruncateClientIdFallbackToNameColumnLength() {
    Dhis2OAuth2Client client = client("ClientUid01", null, null, "c".repeat(255));

    assertEquals("c".repeat(230), client.getName());
    assertNull(client.getRawName());
  }

  @Test
  void shouldReturnNullNameWithoutNameAndClientId() {
    assertNull(client("ClientUid01", null, null, null).getName());
  }

  @Test
  void shouldReturnStoredNameWithoutFallbackForNameIdScheme() {
    assertNull(client("ClientUid01", null, null, "client-a").getPropertyValue(IdScheme.NAME));
    assertEquals(
        "Name", client("ClientUid01", null, "Name", "client-a").getPropertyValue(IdScheme.NAME));
  }

  private static Dhis2OAuth2Client client(String uid, String code, String name, String clientId) {
    Dhis2OAuth2Client client = new Dhis2OAuth2Client();
    client.setUid(uid);
    client.setCode(code);
    client.setName(name);
    client.setClientId(clientId);
    return client;
  }
}
