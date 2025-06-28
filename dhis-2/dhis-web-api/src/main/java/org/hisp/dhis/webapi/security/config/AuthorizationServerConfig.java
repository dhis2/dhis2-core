/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.security.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.oidc.KeyStoreUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.JwtClaimsSet.Builder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Slf4j
@Configuration
@Conditional(AuthorizationServerEnabledCondition.class)
public class AuthorizationServerConfig {

  @Autowired private DhisConfigurationProvider config;

  @Bean
  @Order(1)
  public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
      throws Exception {
    OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
        OAuth2AuthorizationServerConfigurer.authorizationServer();

    http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
        .with(
            authorizationServerConfigurer,
            authorizationServer ->
                authorizationServer.oidc(Customizer.withDefaults()) // Enable OpenID Connect 1.0
            )
        .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
        .exceptionHandling(
            exceptions ->
                exceptions.defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login/"),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));

    return http.build();
  }

  @Bean
  public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(UserService userService) {
    return context -> {
      Set<String> authorizedScopes = context.getAuthorizedScopes();
      if (authorizedScopes.contains("email")) {
        OAuth2TokenType tokenType = context.getTokenType();
        String username = context.getPrincipal().getName();
        User user = userService.getUserByUsername(username);
        String email = user.getEmail();
        // Use username as email if email is not set, email is mandatory, but default admin user
        // don't have email set
        if (email == null || email.isEmpty()) {
          email = user.getUsername();
        }
        if (OAuth2TokenType.ACCESS_TOKEN.equals(tokenType)
            || OidcParameterNames.ID_TOKEN.equals(tokenType.getValue())) {
          Builder claims = context.getClaims();
          claims.claim("email", email);
        }
      }
    };
  }

  @Bean
  public JWKSource<SecurityContext> jwkSource() {
    // Try to load key from keystore if configured
    String keystorePath = config.getProperty(ConfigurationKey.OAUTH2_JWT_KEYSTORE_PATH);
    boolean generateIfMissing =
        config.isEnabled(ConfigurationKey.OAUTH2_JWT_KEYSTORE_GENERATE_IF_MISSING);
    if (keystorePath != null && !keystorePath.isEmpty()) {
      log.info("Attempting to load JWK from keystore: {}", keystorePath);
      try {
        KeyStore keyStore =
            KeyStoreUtil.readKeyStore(
                keystorePath, config.getProperty(ConfigurationKey.OAUTH2_JWT_KEYSTORE_PASSWORD));

        // Use the keystore alias or fallback to the first alias in the keystore
        String alias = config.getProperty(ConfigurationKey.OAUTH2_JWT_KEYSTORE_ALIAS);
        if (alias == null || alias.isEmpty()) {
          String errorMsg =
              "Keystore alias is not configured but required: oauth2.jwt.keystore.alias";
          log.error(errorMsg);
          throw new IllegalStateException(errorMsg);
        }
        String keystoreKeyPassword =
            config.getProperty(ConfigurationKey.OAUTH2_JWT_KEYSTORE_KEY_PASSWORD);
        char[] pin = keystoreKeyPassword != null ? keystoreKeyPassword.toCharArray() : null;
        return getSecurityContextImmutableJWKSet(keyStore, alias, pin);
      } catch (KeyStoreException
          | IOException
          | NoSuchAlgorithmException
          | CertificateException e) {
        String errorMessage = "Failed to load keystore: " + e.getMessage();
        log.error(errorMessage, e);
        throw new IllegalStateException(errorMessage, e);
      }
    } else {
      if (!generateIfMissing) {
        String errorMsg =
            "Keystore configuration is missing and generation of keys is disabled. "
                + "Configure oauth2.jwt.keystore.path or enable oauth2.jwt.keystore.generate-if-missing.";
        log.error(errorMsg);
        throw new IllegalStateException(errorMsg);
      }
      log.warn("No keystore configuration provided. Generating a new ephemeral RSA key pair.");
    }
    return new ImmutableJWKSet<>(new JWKSet(generateEphemeralKey()));
  }

  private static @Nonnull ImmutableJWKSet<SecurityContext> getSecurityContextImmutableJWKSet(
      KeyStore keyStore, String alias, char[] pin) {
    RSAKey rsaKey;
    try {
      rsaKey = KeyStoreUtil.loadRSAPublicKey(keyStore, alias, pin);
      log.info("Successfully loaded JWK from keystore with key ID: {}", rsaKey.getKeyID());
      return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    } catch (KeyStoreException | JOSEException e) {
      String errorMessage = "Failed to load RSA key from keystore: " + e.getMessage();
      log.error(errorMessage, e);
      throw new IllegalStateException(errorMessage, e);
    }
  }

  private static @Nonnull RSAKey generateEphemeralKey() {
    // Generate a new key pair only if allowed or if keystore is not configured
    log.info("Generating ephemeral RSA key pair");
    KeyPair keyPair;
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      keyPair = keyPairGenerator.generateKeyPair();
    } catch (NoSuchAlgorithmException ex) {
      String errorMsg = "Failed to generate RSA key pair: " + ex.getMessage();
      log.error(errorMsg, ex);
      throw new IllegalStateException(errorMsg, ex);
    }
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
    RSAKey rsaKey =
        new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID().toString())
            .build();

    log.info("Generated ephemeral RSA key pair with key ID: {}", rsaKey.getKeyID());
    return rsaKey;
  }

  @Bean
  public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
    return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
  }

  @Bean
  public AuthorizationServerSettings authorizationServerSettings() {
    return AuthorizationServerSettings.builder()
        .issuer(config.getProperty(ConfigurationKey.SERVER_BASE_URL))
        .build();
  }
}
