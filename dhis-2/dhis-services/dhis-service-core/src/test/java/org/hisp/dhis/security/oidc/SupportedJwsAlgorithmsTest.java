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
package org.hisp.dhis.security.oidc;

import static org.junit.jupiter.api.Assertions.*;

import com.nimbusds.jose.JWSAlgorithm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link SupportedJwsAlgorithms}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class SupportedJwsAlgorithmsTest {

  @Test
  void parsesNullAsRs256Default() {
    assertEquals(JWSAlgorithm.RS256, SupportedJwsAlgorithms.parseOrDefault(null));
  }

  @Test
  void parsesBlankAsRs256Default() {
    assertEquals(JWSAlgorithm.RS256, SupportedJwsAlgorithms.parseOrDefault("  "));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"RS256", "RS384", "RS512", "PS256", "PS384", "PS512", "ES256", "ES384", "ES512"})
  void parsesAllSupportedAlgorithms(String name) {
    assertEquals(name, SupportedJwsAlgorithms.parseOrDefault(name).getName());
  }

  @Test
  void rejectsUnsupportedAlgorithm() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> SupportedJwsAlgorithms.parseOrDefault("HS256"));
    assertTrue(ex.getMessage().contains("HS256"));
  }

  @Test
  void rejectsNonsense() {
    assertThrows(
        IllegalArgumentException.class, () -> SupportedJwsAlgorithms.parseOrDefault("nope"));
  }
}
