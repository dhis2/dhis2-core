/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.webapi.security.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public final class TestJoseHeaders {
  private TestJoseHeaders() {}

  public static JoseHeader.Builder joseHeader(String provider) {
    return joseHeader(SignatureAlgorithm.RS256, provider);
  }

  public static JoseHeader.Builder joseHeader(
      SignatureAlgorithm signatureAlgorithm, String provider) {
    return JoseHeader.withAlgorithm(signatureAlgorithm)
        .jwkSetUri("https://" + provider + "/oauth2/jwks")
        .jwk(rsaJwk())
        .keyId("keyId")
        .x509Uri("https://" + provider + "/oauth2/x509")
        .x509CertificateChain(Arrays.asList("x509Cert1", "x509Cert2"))
        .x509SHA1Thumbprint("x509SHA1Thumbprint")
        .x509SHA256Thumbprint("x509SHA256Thumbprint")
        .type("JWT")
        .contentType("jwt-content-type")
        .header("custom-header-name", "custom-header-value");
  }

  private static Map<String, Object> rsaJwk() {
    Map<String, Object> rsaJwk = new HashMap<>();
    rsaJwk.put("kty", "RSA");
    rsaJwk.put("n", "modulus");
    rsaJwk.put("e", "exponent");
    return rsaJwk;
  }
}
