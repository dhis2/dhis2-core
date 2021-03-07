/*
 * Copyright 2020-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hisp.dhis.webapi.security;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.webapi.utils.JoseHeader;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

/**
 * @author Joe Grandja
 */
public final class TestJoseHeaders {

	private TestJoseHeaders() {
	}

	public static JoseHeader.Builder joseHeader( String provider ) {
		return joseHeader(SignatureAlgorithm.RS256, provider );
	}

	public static JoseHeader.Builder joseHeader( SignatureAlgorithm signatureAlgorithm, String provider ) {
		// @formatter:off

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
		// @formatter:on
	}

	private static Map<String, Object> rsaJwk() {
		Map<String, Object> rsaJwk = new HashMap<>();
		rsaJwk.put("kty", "RSA");
		rsaJwk.put("n", "modulus");
		rsaJwk.put("e", "exponent");
		return rsaJwk;
	}
}
