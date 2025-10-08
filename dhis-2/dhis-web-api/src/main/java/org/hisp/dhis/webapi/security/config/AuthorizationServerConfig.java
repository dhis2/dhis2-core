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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.condition.AuthorizationServerEnabledCondition;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2Client;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2ClientService;
import org.hisp.dhis.security.oidc.KeyStoreUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.security.config.InlineJwksClientMetadataConfig.ClientRegistrationConverter;
import org.hisp.dhis.webapi.security.config.InlineJwksClientMetadataConfig.RegisteredClientConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet.Builder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.JwtClientAssertionAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.JwtClientAssertionDecoderFactory;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcClientConfigurationAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcClientRegistrationAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

/**
 * Configuration class for the OAuth2 Authorization Server.
 *
 * <p>This class sets up the necessary beans and configurations to enable OAuth2 authorization
 * server capabilities, including JWT token handling, client registration (DCR), and security
 * filters.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Configuration
@Conditional(AuthorizationServerEnabledCondition.class)
public class AuthorizationServerConfig {

  private static final String USERNAME_CLAIM = "username";
  private static final String EMAIL_CLAIM = "email";

  /**
   * Configures the JWT decoder for the authorization server using the provided JWK source.
   *
   * @param jwkSource the source of JSON Web Keys (JWKs) used for decoding JWT tokens
   * @return a {@link JwtDecoder} instance configured with the JWK source
   */
  @Bean
  public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
    return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
  }

  /**
   * Configures the authorization server settings, and sets the issuer URL from the the dhis.conf
   * SERVER_BASE_URL (server.base.url) property.
   *
   * @param config the system configuration
   * @return an {@link AuthorizationServerSettings} instance with the configured issuer URL
   */
  @Bean
  public AuthorizationServerSettings authorizationServerSettings(DhisConfigurationProvider config) {
    return AuthorizationServerSettings.builder()
        .issuer(config.getProperty(ConfigurationKey.SERVER_BASE_URL))
        .build();
  }

  /**
   * Configures the security filter chain for the authorization server endpoints.
   *
   * <p>This configuration includes client authentication using JWT client assertions with inline
   * JWKS, and customizes the client registration endpoint to use specific converters for reading
   * and writing client details.
   *
   * <p>All requests to the authorization server endpoints require authentication, and HTML requests
   * are redirected to a login page.
   *
   * @param http the {@link HttpSecurity} to configure
   * @param customClaimValidator the custom DHIS2 claim validator to include in JWT validation
   * @return a {@link SecurityFilterChain} configured for the authorization server
   * @throws Exception if an error occurs while configuring the security filter chain
   */
  @Bean
  @Order(1)
  public SecurityFilterChain authorizationServerSecurityFilterChain(
      HttpSecurity http, CustomClaimValidator<Jwt> customClaimValidator) throws Exception {
    OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
        OAuth2AuthorizationServerConfigurer.authorizationServer();

    http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
        .with(
            authorizationServerConfigurer,
            authorizationServer ->
                authorizationServer
                    .clientAuthentication(
                        clientAuthentication ->
                            clientAuthentication.authenticationProviders(
                                configureJwtClientAssertionWithInlineJwks(customClaimValidator)))
                    .oidc(
                        oidc ->
                            oidc.clientRegistrationEndpoint(
                                cr -> cr.authenticationProviders(customizeDcrProviders()))))
        .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
        .exceptionHandling(
            exceptions ->
                exceptions.defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login/"),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));

    return http.build();
  }

  /**
   * Configure JWT client assertion to use our InlineJwksJwtClientAssertionDecoderFactory that
   * prefers inline JWKS from client metadata over the default remote JWKS URI, also adds our custom
   * claim validator on top of the default SAS validators.
   *
   * @param customClaimValidator the custom claim validator to include in the JWT validation
   * @return a consumer that configures the authentication providers
   */
  private Consumer<List<AuthenticationProvider>> configureJwtClientAssertionWithInlineJwks(
      CustomClaimValidator<Jwt> customClaimValidator) {
    return providers ->
        providers.forEach(
            ap -> {
              if (ap instanceof JwtClientAssertionAuthenticationProvider provider) {
                InlineJwksJwtClientAssertionDecoderFactory decoderFactory =
                    new InlineJwksJwtClientAssertionDecoderFactory();

                Function<RegisteredClient, OAuth2TokenValidator<Jwt>> jwtTokenValidator =
                    registeredClient ->
                        new DelegatingOAuth2TokenValidator<>(
                            // default SAS validators (iss/sub/aud/exp/nbf)
                            JwtClientAssertionDecoderFactory.DEFAULT_JWT_VALIDATOR_FACTORY.apply(
                                registeredClient),
                            // DHIS2 custom claim validator
                            customClaimValidator);

                decoderFactory.setJwtValidatorFactory(jwtTokenValidator);
                provider.setJwtDecoderFactory(decoderFactory);
              }
            });
  }

  /**
   * Customizes the Dynamic Client Registration (DCR) authentication providers to use our custom
   * client converters.
   *
   * @return a consumer that customizes the DCR authentication providers.
   */
  private Consumer<List<AuthenticationProvider>> customizeDcrProviders() {
    RegisteredClientConverter regConverter = new RegisteredClientConverter();
    ClientRegistrationConverter readConverter = new ClientRegistrationConverter();
    return providers ->
        providers.forEach(
            p -> {
              if (p instanceof OidcClientRegistrationAuthenticationProvider cr) {
                cr.setRegisteredClientConverter(regConverter);
                cr.setClientRegistrationConverter(readConverter);
              }
              if (p instanceof OidcClientConfigurationAuthenticationProvider cp) {
                cp.setClientRegistrationConverter(readConverter);
              }
            });
  }

  /**
   * Customizes the JWT token by adding additional claims based on the authorized scopes.
   *
   * <p>If the "email" scope is authorized, the user's email is added as a claim. If the "username"
   * scope is authorized, the username is added as a claim.
   *
   * <p>For client credentials grant type, the username of the user who created the client is added.
   *
   * @param userService the service to retrieve user information
   * @return an {@link OAuth2TokenCustomizer} that adds custom claims to the JWT token
   */
  @Bean
  public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(
      UserService userService, Dhis2OAuth2ClientService oAuth2ClientService) {
    return context -> {
      Builder claims = context.getClaims();
      OAuth2TokenType tokenType = context.getTokenType();
      Set<String> authorizedScopes = context.getAuthorizedScopes();
      AuthorizationGrantType authorizationGrantType = context.getAuthorizationGrantType();

      if (OAuth2TokenType.ACCESS_TOKEN.equals(tokenType)
          || OidcParameterNames.ID_TOKEN.equals(tokenType.getValue())) {

        if (authorizedScopes.contains(EMAIL_CLAIM)) {
          String username = context.getPrincipal().getName();
          User user = userService.getUserByUsername(username);
          String email = user.getEmail();
          if (email != null && !email.isEmpty()) {
            claims.claim(EMAIL_CLAIM, email);
          } else {
            log.error("User '{}' has no email address, cannot include 'email' claim", username);
            throw new IllegalStateException(
                "User has no email address, cannot include 'email' claim");
          }
        }

        if (authorizedScopes.contains(USERNAME_CLAIM)) {
          // CLIENT_CREDENTIALS is used in DCR with JWT client assertions.
          // In this flow there is no end-user, so we include the username of the user
          // who created the client.
          if (authorizationGrantType.equals(AuthorizationGrantType.CLIENT_CREDENTIALS)) {
            String clientId = context.getPrincipal().getName();
            Dhis2OAuth2Client registeredClient =
                oAuth2ClientService.getAsDhis2OAuth2ClientByClientId(clientId);
            User createdBy = registeredClient.getCreatedBy();
            claims.claim(USERNAME_CLAIM, createdBy.getUsername());
          } else {
            String username = context.getPrincipal().getName();
            if (!username.isEmpty()) {
              claims.claim(USERNAME_CLAIM, username);
            } else {
              log.error("Principal has no name, cannot include 'username' claim");
              throw new IllegalStateException(
                  "Principal has no username, cannot include 'username' claim");
            }
          }
        }
      }
    };
  }

  /**
   * Configures the JWK source for the authorization server. It attempts to load a RSA key from a
   * keystore if configured. If the keystore is not configured and key generation is allowed, it
   * generates an ephemeral RSA key pair.
   *
   * @return a {@link JWKSource} containing the RSA key for signing the servers's JWT tokens
   * @throws IllegalStateException if the keystore cannot be loaded or if key generation is not
   *     allowed when the keystore is missing
   */
  @Bean
  public JWKSource<SecurityContext> jwkSource(DhisConfigurationProvider config) {
    String keystorePath = config.getProperty(ConfigurationKey.OAUTH2_JWT_KEYSTORE_PATH);
    boolean generateIfMissing =
        config.isEnabled(ConfigurationKey.OAUTH2_JWT_KEYSTORE_GENERATE_IF_MISSING);
    if (keystorePath != null && !keystorePath.isEmpty()) {
      log.info("Attempting to load JWK from keystore: {}", keystorePath);
      try {
        // Try to load key from keystore if configured
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

        return loadJWKSetFromKeystore(keyStore, alias, pin);

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
      return new ImmutableJWKSet<>(new JWKSet(generateEphemeralKey()));
    }
  }

  // Load RSA key from keystore and return as ImmutableJWKSet
  private static @Nonnull ImmutableJWKSet<SecurityContext> loadJWKSetFromKeystore(
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

  // Generate a new ephemeral RSA key pair
  private static @Nonnull RSAKey generateEphemeralKey() {
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
}
