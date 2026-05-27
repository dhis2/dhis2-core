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

import com.nimbusds.jose.JWSAlgorithm;
import java.util.Set;
import javax.annotation.CheckForNull;

/**
 * Allow-list of JWS algorithms accepted for OIDC userinfo JWT verification. Failing closed at parse
 * time prevents accidental acceptance of unexpected signature algorithms (e.g. HMAC) configured in
 * {@code dhis.conf}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public final class SupportedJwsAlgorithms {

  public static final JWSAlgorithm DEFAULT = JWSAlgorithm.RS256;

  private static final Set<JWSAlgorithm> ALLOWED =
      Set.of(
          JWSAlgorithm.RS256,
          JWSAlgorithm.RS384,
          JWSAlgorithm.RS512,
          JWSAlgorithm.PS256,
          JWSAlgorithm.PS384,
          JWSAlgorithm.PS512,
          JWSAlgorithm.ES256,
          JWSAlgorithm.ES384,
          JWSAlgorithm.ES512);

  private SupportedJwsAlgorithms() {}

  /**
   * Parses a configured JWS algorithm name against the allow-list.
   *
   * @param value config string from {@code dhis.conf}; case-sensitive (Nimbus algorithm names)
   * @return the matching {@link JWSAlgorithm}, or {@link #DEFAULT} when {@code value} is null/blank
   * @throws IllegalArgumentException when the algorithm is not in the allow-list
   */
  public static JWSAlgorithm parseOrDefault(@CheckForNull String value) {
    if (value == null || value.isBlank()) {
      return DEFAULT;
    }
    JWSAlgorithm parsed = JWSAlgorithm.parse(value.trim());
    if (!ALLOWED.contains(parsed)) {
      throw new IllegalArgumentException(
          "Unsupported user_info_jws_algorithm: '" + value + "'. Allowed: " + ALLOWED);
    }
    return parsed;
  }
}
