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
package org.hisp.dhis.webapi.controller.security;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nimbusds.jose.util.Base64URL;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.security.oidc.DhisOidcProviderRepository;
import org.hisp.dhis.security.oidc.GenericOidcProviderConfigParser;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the public {@code /api/publicKeys/{clientId}/jwks.json} endpoint that publishes the public
 * half of a {@code private_key_jwt} provider's signing key.
 *
 * <p>The keystore is created with the JDK's {@code keytool} so the key carries a real self-signed
 * X.509 certificate, and the provider is registered through {@link
 * GenericOidcProviderConfigParser}, the same path {@code dhis.conf} providers take.
 *
 * @author Morten (Morty) <netroms@gmail.com>
 */
class PublicKeysControllerTest extends H2ControllerIntegrationTestBase {

  private static final String KEY_PROVIDER = "jwks-test-esignet";
  private static final String NO_KEY_PROVIDER = "jwks-test-no-key";
  private static final String KEY_ALIAS = "dhis2-oidc-client";
  private static final String KEYSTORE_PASSWORD = "keystore-password";

  @TempDir static Path tempDir;

  private static Path keystorePath;

  @Autowired private DhisOidcProviderRepository providerRepository;

  @BeforeAll
  static void createKeystore() throws Exception {
    keystorePath = tempDir.resolve("dhis2-oidc-client.p12");
    Path keytool = Path.of(System.getProperty("java.home"), "bin", "keytool");
    Process process =
        new ProcessBuilder(
                keytool.toString(),
                "-genkeypair",
                "-alias",
                KEY_ALIAS,
                "-keyalg",
                "RSA",
                "-keysize",
                "2048",
                "-sigalg",
                "SHA256withRSA",
                "-dname",
                "CN=dhis2-oidc-client",
                "-validity",
                "1",
                "-storetype",
                "PKCS12",
                "-keystore",
                keystorePath.toString(),
                "-storepass",
                KEYSTORE_PASSWORD,
                "-keypass",
                KEYSTORE_PASSWORD)
            .redirectErrorStream(true)
            .start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    if (!process.waitFor(60, TimeUnit.SECONDS) || process.exitValue() != 0) {
      throw new IllegalStateException("keytool failed: " + output);
    }
  }

  @AfterEach
  void removeProviders() {
    providerRepository.removeRegistration(KEY_PROVIDER);
    providerRepository.removeRegistration(NO_KEY_PROVIDER);
  }

  @Test
  void getKeysPublishesThePublicSigningKeyWithItsCertificateThumbprint() throws Exception {
    registerProvider(KEY_PROVIDER, privateKeyJwtProvider(KEY_PROVIDER));
    X509Certificate certificate = loadCertificate();
    RSAPublicKey publicKey = (RSAPublicKey) certificate.getPublicKey();

    JsonArray keys =
        GET("/publicKeys/{id}/jwks.json", KEY_PROVIDER).content(HttpStatus.OK).getArray("keys");

    assertEquals(1, keys.size());
    JsonObject key = keys.getObject(0);
    assertEquals("RSA", key.getString("kty").string());
    assertEquals(KEY_ALIAS, key.getString("kid").string());
    assertEquals("sig", key.getString("use").string());
    assertEquals("RS256", key.getString("alg").string());
    assertEquals(
        Base64URL.encode(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()))
            .toString(),
        key.getString("x5t#S256").string());
    assertEquals(Base64URL.encode(publicKey.getModulus()).toString(), key.getString("n").string());
    assertEquals(
        Base64URL.encode(publicKey.getPublicExponent()).toString(), key.getString("e").string());
    for (String privateParam : List.of("d", "p", "q", "dp", "dq", "qi")) {
      assertFalse(key.has(privateParam), "private key parameter published: " + privateParam);
    }
  }

  @Test
  void getKeysReturnsNotFoundForAnUnknownProvider() {
    assertStatus(HttpStatus.NOT_FOUND, GET("/publicKeys/{id}/jwks.json", "no-such-provider"));
  }

  @Test
  void getKeysReturnsConflictForAProviderWithoutASigningKey() {
    registerProvider(NO_KEY_PROVIDER, baseProvider(NO_KEY_PROVIDER));
    assertNull(providerRepository.getDhisOidcClientRegistration(NO_KEY_PROVIDER).getJwk());

    assertStatus(HttpStatus.CONFLICT, GET("/publicKeys/{id}/jwks.json", NO_KEY_PROVIDER));
  }

  private void registerProvider(String id, Properties config) {
    GenericOidcProviderConfigParser.parse(config).forEach(providerRepository::addRegistration);
    assertNotNull(providerRepository.getDhisOidcClientRegistration(id));
  }

  private static X509Certificate loadCertificate() throws Exception {
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    try (InputStream in = new FileInputStream(keystorePath.toFile())) {
      keyStore.load(in, KEYSTORE_PASSWORD.toCharArray());
    }
    return (X509Certificate) keyStore.getCertificate(KEY_ALIAS);
  }

  private static Properties baseProvider(String id) {
    Properties p = new Properties();
    String pre = "oidc.provider." + id + ".";
    p.setProperty(pre + "client_id", "dhis2-client");
    p.setProperty(pre + "client_secret", "dhis2-client-secret");
    p.setProperty(pre + "authorization_uri", "https://idp.example.org/authorize");
    p.setProperty(pre + "token_uri", "https://idp.example.org/token");
    p.setProperty(pre + "user_info_uri", "https://idp.example.org/userinfo");
    p.setProperty(pre + "jwk_uri", "https://idp.example.org/jwks.json");
    return p;
  }

  private static Properties privateKeyJwtProvider(String id) {
    Properties p = baseProvider(id);
    String pre = "oidc.provider." + id + ".";
    p.setProperty(pre + "client_authentication_method", "private_key_jwt");
    p.setProperty(pre + "keystore_path", keystorePath.toString());
    p.setProperty(pre + "keystore_password", KEYSTORE_PASSWORD);
    p.setProperty(pre + "key_alias", KEY_ALIAS);
    p.setProperty(pre + "key_password", KEYSTORE_PASSWORD);
    p.setProperty(
        pre + "jwk_set_url", "https://dhis2.example.org/api/publicKeys/" + id + "/jwks.json");
    return p;
  }
}
