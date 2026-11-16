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
package org.hisp.dhis.security.oauth2.consent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Objects;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class Dhis2OAuth2AuthorizationConsentTest {

  @Test
  void testEqualConsentsHaveEqualHashCodes() {
    Dhis2OAuth2AuthorizationConsent a = consent(c -> {});
    Dhis2OAuth2AuthorizationConsent b = consent(c -> {});
    b.setId(42);
    b.setAuthorities("SCOPE_other");

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void testConsentsDifferingInIdentityOrKeyAreNotEqual() {
    Dhis2OAuth2AuthorizationConsent base = consent(c -> {});

    assertNotEquals(base, consent(c -> c.setUid("consentUid2")));
    assertNotEquals(base, consent(c -> c.setCode("CODE_2")));
    assertNotEquals(base, consent(c -> c.setName("other-name")));
    assertNotEquals(base, consent(c -> c.setRegisteredClientId("other-client")));
    assertNotEquals(base, consent(c -> c.setPrincipalName("other-user")));
  }

  /**
   * Pins the pre-migration formula: {@code BaseIdentifiableObject}'s uid/code/name hash combined
   * with the consent key through {@link Objects#hash}.
   */
  @Test
  void testHashCodeKeepsLegacyFormula() {
    Dhis2OAuth2AuthorizationConsent consent = consent(c -> {});
    int identity = "consentUid1".hashCode();
    identity = 31 * identity + "CODE_1".hashCode();
    identity = 31 * identity + "name".hashCode();

    assertEquals(Objects.hash(identity, "client", "user"), consent.hashCode());
  }

  private static Dhis2OAuth2AuthorizationConsent consent(
      Consumer<Dhis2OAuth2AuthorizationConsent> change) {
    Dhis2OAuth2AuthorizationConsent consent = new Dhis2OAuth2AuthorizationConsent();
    consent.setUid("consentUid1");
    consent.setCode("CODE_1");
    consent.setName("name");
    consent.setRegisteredClientId("client");
    consent.setPrincipalName("user");
    consent.setAuthorities("SCOPE_openid");
    change.accept(consent);
    return consent;
  }
}
