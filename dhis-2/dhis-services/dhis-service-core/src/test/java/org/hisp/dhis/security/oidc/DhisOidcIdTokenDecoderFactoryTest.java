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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class DhisOidcIdTokenDecoderFactoryTest {
  private RSAKey key;
  private HttpServer server;
  private String issuer;
  private final DhisOidcIdTokenDecoderFactory factory = new DhisOidcIdTokenDecoderFactory();

  @BeforeEach
  void setup() throws Exception {
    key = new RSAKeyGenerator(2048).keyID("id-token-key").generate();
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    byte[] jwks = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
    server.createContext(
        "/jwks",
        exchange -> {
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, jwks.length);
          try (var body = exchange.getResponseBody()) {
            body.write(jwks);
          }
        });
    server.start();
    issuer = "http://localhost:" + server.getAddress().getPort();
  }

  @AfterEach
  void cleanup() {
    server.stop(0);
  }

  @Test
  void ps256IsOptInAndDoesNotBroadenOtherRegistrations() throws Exception {
    String pssToken = token(JWSAlgorithm.PS256, claims().build());
    String rsaToken = token(JWSAlgorithm.RS256, claims().build());
    var pss = factory.createDecoder(registration("pss", "PS256"));
    var standard = factory.createDecoder(registration("standard", null));
    assertEquals("mapped-user", pss.decode(pssToken).getSubject());
    assertThrows(JwtException.class, () -> pss.decode(rsaToken));
    assertEquals("mapped-user", standard.decode(rsaToken).getSubject());
    assertThrows(JwtException.class, () -> standard.decode(pssToken));
  }

  @Test
  void configuredAlgorithmRetainsIssuerAudienceExpiryAndSignatureChecks() throws Exception {
    var decoder = factory.createDecoder(registration("pss", "PS256"));
    String wrongIssuer =
        token(JWSAlgorithm.PS256, claims().issuer("https://other.example").build());
    String wrongAudience = token(JWSAlgorithm.PS256, claims().audience("other-client").build());
    String expired =
        token(
            JWSAlgorithm.PS256,
            claims()
                .issueTime(Date.from(Instant.now().minusSeconds(600)))
                .expirationTime(Date.from(Instant.now().minusSeconds(300)))
                .build());
    assertThrows(JwtException.class, () -> decoder.decode(wrongIssuer));
    assertThrows(JwtException.class, () -> decoder.decode(wrongAudience));
    assertThrows(JwtException.class, () -> decoder.decode(expired));
    key = new RSAKeyGenerator(2048).keyID("id-token-key").generate();
    String forged = token(JWSAlgorithm.PS256, claims().build());
    assertThrows(JwtException.class, () -> decoder.decode(forged));
  }

  @ParameterizedTest
  @ValueSource(strings = {"none", "HS256", "typo"})
  void invalidIdTokenAlgorithmRejectsProviderConfiguration(String algorithm) {
    assertTrue(GenericOidcProviderConfigParser.parse(properties("invalid", algorithm)).isEmpty());
  }

  private ClientRegistration registration(String id, String algorithm) {
    return GenericOidcProviderConfigParser.parse(properties(id, algorithm))
        .get(0)
        .getClientRegistration();
  }

  private Properties properties(String id, String algorithm) {
    String prefix = "oidc.provider." + id + ".";
    Properties properties = new Properties();
    properties.setProperty(prefix + "client_id", "client");
    properties.setProperty(prefix + "client_secret", "secret");
    properties.setProperty(prefix + "issuer_uri", issuer);
    properties.setProperty(prefix + "authorization_uri", issuer + "/authorize");
    properties.setProperty(prefix + "token_uri", issuer + "/token");
    properties.setProperty(prefix + "user_info_uri", issuer + "/userinfo");
    properties.setProperty(prefix + "jwk_uri", issuer + "/jwks");
    if (algorithm != null) properties.setProperty(prefix + "id_token_jws_algorithm", algorithm);
    return properties;
  }

  private JWTClaimsSet.Builder claims() {
    return new JWTClaimsSet.Builder()
        .subject("mapped-user")
        .issuer(issuer)
        .audience("client")
        .issueTime(new Date())
        .expirationTime(Date.from(Instant.now().plusSeconds(300)));
  }

  private String token(JWSAlgorithm algorithm, JWTClaimsSet claims) throws Exception {
    SignedJWT jwt =
        new SignedJWT(new JWSHeader.Builder(algorithm).keyID(key.getKeyID()).build(), claims);
    jwt.sign(new RSASSASigner(key));
    return jwt.serialize();
  }
}
