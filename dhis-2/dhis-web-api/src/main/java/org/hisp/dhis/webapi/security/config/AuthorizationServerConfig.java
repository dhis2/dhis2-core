/*
 * Copyright (c) 2004-2025, University of Oslo
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
import java.util.UUID;
import org.hisp.dhis.security.oidc.KeyStoreUtil;
import org.hisp.dhis.security.spring2fa.TwoFactorWebAuthenticationDetailsSource;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
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

@Configuration
public class AuthorizationServerConfig {

  private static final Logger log = LoggerFactory.getLogger(AuthorizationServerConfig.class);

  @Autowired
  private TwoFactorWebAuthenticationDetailsSource twoFactorWebAuthenticationDetailsSource;

  @Value("${oauth2.jwt.keystore.path:}")
  private String keystorePath;

  @Value("${oauth2.jwt.keystore.password:}")
  private String keystorePassword;

  @Value("${oauth2.jwt.keystore.alias:}")
  private String keystoreAlias;

  @Value("${oauth2.jwt.keystore.key-password:}")
  private String keyPassword;

  @Value("${oauth2.jwt.keystore.generate-if-missing:false}")
  private boolean generateIfMissing;

  @Bean
  @Order(1)
  public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
      throws Exception {
    OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
        OAuth2AuthorizationServerConfigurer.authorizationServer();

    http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
        .with(
            authorizationServerConfigurer,
            (authorizationServer) ->
                authorizationServer.oidc(Customizer.withDefaults()) // Enable OpenID Connect 1.0
            )
        .authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated())
        .exceptionHandling(
            (exceptions) ->
                exceptions.defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/dhis-web-login/"),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));

    return http.build();
  }

  @Bean
  public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(UserService userService) {
    return (context) -> {
      OAuth2TokenType tokenType = context.getTokenType();
      if (OAuth2TokenType.ACCESS_TOKEN.equals(tokenType)) {
        String username = context.getPrincipal().getName();
        User user = userService.getUserByUsername(username);
        String email = user.getEmail();
        Builder claims = context.getClaims();
        claims.claim("email", email);
      }

      if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
        String username = context.getPrincipal().getName();
        User user = userService.getUserByUsername(username);
        context.getClaims().claim("email", user.getEmail());
      }
    };
  }

  @Bean
  public JWKSource<SecurityContext> jwkSource() {
    RSAKey rsaKey;

    // Try to load key from keystore if configured
    if (keystorePath != null && !keystorePath.isEmpty()) {
      log.info("Attempting to load JWK from keystore: {}", keystorePath);
      try {
        KeyStore keyStore = KeyStoreUtil.readKeyStore(keystorePath, keystorePassword);
        log.debug("Keystore loaded successfully");
        
        // Use the keystore alias or fallback to the first alias in the keystore
        String alias = keystoreAlias;
        if (alias == null || alias.isEmpty()) {
          String errorMsg = "Keystore alias is not configured but required: oauth2.jwt.keystore.alias";
          log.error(errorMsg);
          throw new IllegalStateException(errorMsg);
        }
        
        char[] pin = keyPassword != null ? keyPassword.toCharArray() : null;
        
        try {
          log.debug("Loading RSA key with alias: {}", alias);
          rsaKey = KeyStoreUtil.loadRSAPublicKey(keyStore, alias, pin);
          log.info("Successfully loaded JWK from keystore with key ID: {}", rsaKey.getKeyID());
          return new ImmutableJWKSet<>(new JWKSet(rsaKey));
        } catch (KeyStoreException | JOSEException e) {
          String errorMessage = "Failed to load RSA key from keystore: " + e.getMessage();
          if (generateIfMissing) {
            log.warn("{} Generating a new key pair.", errorMessage);
          } else {
            log.error(errorMessage, e);
            throw new IllegalStateException(errorMessage, e);
          }
        }
      } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
        String errorMessage = "Failed to load keystore: " + e.getMessage();
        if (generateIfMissing) {
          log.warn("{} Generating a new key pair.", errorMessage);
        } else {
          log.error(errorMessage, e);
          throw new IllegalStateException(errorMessage, e);
        }
      }
    } else {
      if (!generateIfMissing) {
        String errorMsg = "Keystore configuration is missing and generation of keys is disabled. "
            + "Configure oauth2.jwt.keystore.path or enable oauth2.jwt.keystore.generate-if-missing.";
        log.error(errorMsg);
        throw new IllegalStateException(errorMsg);
      }
      log.warn("No keystore configuration provided. Generating a new ephemeral RSA key pair.");
    }

    // Generate a new key pair only if allowed or if keystore is not configured
    log.info("Generating ephemeral RSA key pair");
    KeyPair keyPair = generateRsaKey();
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
    rsaKey = new RSAKey.Builder(publicKey)
        .privateKey(privateKey)
        .keyID(UUID.randomUUID().toString())
        .build();
    
    log.info("Generated ephemeral RSA key pair with key ID: {}", rsaKey.getKeyID());
    return new ImmutableJWKSet<>(new JWKSet(rsaKey));
  }

  private static KeyPair generateRsaKey() {
    KeyPair keyPair;
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      keyPair = keyPairGenerator.generateKeyPair();
    } catch (Exception ex) {
      String errorMsg = "Failed to generate RSA key pair: " + ex.getMessage();
      LoggerFactory.getLogger(AuthorizationServerConfig.class).error(errorMsg, ex);
      throw new IllegalStateException(errorMsg, ex);
    }
    return keyPair;
  }

  @Bean
  public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
    return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
  }

  @Bean
  public AuthorizationServerSettings authorizationServerSettings() {
    return AuthorizationServerSettings.builder().build();
  }
}
