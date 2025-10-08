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
package org.hisp.dhis.security.oauth2.dcr;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.condition.AuthorizationServerEnabledCondition;
import org.hisp.dhis.security.oauth2.authorization.Dhis2OAuth2AuthorizationService;
import org.hisp.dhis.security.oauth2.authorization.Dhis2OAuth2AuthorizationServiceImpl;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2ClientService;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing OAuth2 Dynamic Client Registration (DCR) in DHIS2, including creation of
 * initial access tokens (IAT) for enrolling new device clients.
 *
 * <p>See RFC 7591: OAuth 2.0 Dynamic Client Registration Protocol, section 2.1.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Service
@Conditional(AuthorizationServerEnabledCondition.class)
public class OAuth2DcrService {

  public static final String SYSTEM_REGISTRAR_DEVICE_CLIENTS_CLIENTID =
      "system-dcr-registrar-client";

  @Autowired private Dhis2OAuth2ClientService oAuth2ClientService;
  @Autowired private Dhis2OAuth2AuthorizationService dhis2OAuth2AuthorizationService;
  @Autowired private AuthorizationServerSettings authorizationServerSettings;
  @Autowired private JWKSource<SecurityContext> jwkSource;
  @Autowired private JwtDecoder jwtDecoder;
  @Autowired private SystemSettingsService systemSettingsService;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private RegisteredClient registeredClient;
  private JwtEncoder jwtEncoder;

  @PostConstruct
  void init() {
    this.jwtEncoder = new NimbusJwtEncoder(jwkSource);

    // Configure Jackson mapper with required modules
    ClassLoader classLoader = Dhis2OAuth2AuthorizationServiceImpl.class.getClassLoader();
    List<Module> securityModules = SecurityJackson2Modules.getModules(classLoader);
    this.objectMapper.registerModules(securityModules);
    this.objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
    this.objectMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    // Ensure a system client exists for registering device clients
    RegisteredClient systemRegistrationClient =
        oAuth2ClientService.findByClientId(SYSTEM_REGISTRAR_DEVICE_CLIENTS_CLIENTID);
    if (systemRegistrationClient == null) {

      String uriAllowList =
          systemSettingsService.getCurrentSettings().getDeviceEnrollmentRedirectAllowlist();

      // Create a client with "client.create" scope to be able to register new clients
      this.registeredClient =
          RegisteredClient.withId(CodeGenerator.generateUid())
              .clientId(SYSTEM_REGISTRAR_DEVICE_CLIENTS_CLIENTID)
              .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
              .redirectUris(
                  l -> {
                    if (uriAllowList != null && !uriAllowList.isBlank()) {
                      for (String entry : uriAllowList.split(",")) {
                        String trimmed = entry.trim();
                        if (!trimmed.isEmpty()) {
                          l.add(trimmed);
                        }
                      }
                    }
                  })
              .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
              .scope("client.create")
              .build();
      oAuth2ClientService.save(registeredClient, new SystemUser());
    } else {
      this.registeredClient = systemRegistrationClient;
    }
  }

  @Nonnull
  @Transactional
  public IatPair createIat(@Nonnull String redirectUri) {
    String issuer = authorizationServerSettings.getIssuer();
    int ttlSeconds = systemSettingsService.getCurrentSettings().getDeviceEnrollmentIATTtlSeconds();
    IatPair iaToken =
        createIaToken(registeredClient, redirectUri, issuer, ttlSeconds, objectMapper, jwtEncoder);

    dhis2OAuth2AuthorizationService.save(iaToken.authorization());
    return iaToken;
  }

  /**
   * Create an initial access token (IAT) as a JWT with the "client.create" scope, valid for the
   * specified number of seconds.
   *
   * <p>See RFC 7591: OAuth 2.0 Dynamic Client Registration Protocol, section 2.1.
   *
   * @param registeredClient the client that is registering new clients
   * @param redirectUri the redirect URI to include in the token claims
   * @param issuer the issuer to include in the token claims
   * @param ttlSeconds time-to-live in seconds for the token
   * @param objectMapper the Jackson object mapper to use for claim serialization
   * @param jwtEncoder the JWT encoder to use for signing the token
   * @return the created OAuth2Authorization persisted object and the serialized JWT string
   */
  @Nonnull
  public static IatPair createIaToken(
      @Nonnull RegisteredClient registeredClient,
      @Nonnull String redirectUri,
      @Nonnull String issuer,
      int ttlSeconds,
      @Nonnull ObjectMapper objectMapper,
      @Nonnull JwtEncoder jwtEncoder) {

    Instant now = Instant.now();
    Instant ttlInSeconds = now.plus(ttlSeconds, ChronoUnit.SECONDS);

    // The subject/owner is the current user
    String username = CurrentUserUtil.getCurrentUsername();

    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .audience(List.of(issuer))
            .issuedAt(now)
            .expiresAt(ttlInSeconds)
            .subject(username)
            .id(UUID.randomUUID().toString()) // (jti) Unique token ID
            .claim("scope", "client.create")
            .claim("redirect_url", redirectUri)
            .build();

    JwsHeader jwsHeader = JwsHeader.with(SignatureAlgorithm.RS256).build();

    // Encodes and signs the JWT
    String jwtEncodedToken =
        jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();

    OAuth2AccessToken accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            jwtEncodedToken,
            now,
            ttlInSeconds,
            Set.of("client.create"));

    // Persist the authorization so that it can be used for authentication/authorization
    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName(username)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .token(
                accessToken,
                metadata -> covertMetadata(claims, metadata, accessToken, objectMapper))
            .build();

    return new IatPair(authorization, jwtEncodedToken);
  }

  public record IatPair(OAuth2Authorization authorization, String iatJwt) {}

  /**
   * Validate and convert the JWT claims and access token scopes to a JSON-safe map and store it in
   * the metadata under the {@link OAuth2Authorization.Token#CLAIMS_METADATA_NAME} key.
   */
  private static void covertMetadata(
      @Nonnull JwtClaimsSet claims,
      @Nonnull Map<String, Object> metadata,
      @Nonnull OAuth2AccessToken accessToken,
      @Nonnull ObjectMapper objectMapper) {
    @SuppressWarnings("unchecked")
    Map<String, Object> safe = (Map<String, Object>) jsonSafe(claims.getClaims(), objectMapper);

    // Ensure 'scope' is present as Collection<String> in the claims map.
    // Use a mutable, Jackson-friendly type to avoid the deserialization problems.
    Collection<String> tokenScopes = accessToken.getScopes();
    if (tokenScopes != null && !tokenScopes.isEmpty()) {
      safe.put(OAuth2ParameterNames.SCOPE, new ArrayList<>(tokenScopes));
    }

    Object scopeClaim = safe.get(OAuth2ParameterNames.SCOPE);
    if (scopeClaim instanceof String s) {
      safe.put(
          OAuth2ParameterNames.SCOPE,
          Arrays.stream(s.split("[\\s,]+"))
              .filter(v -> !v.isBlank())
              .collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
    } else if (scopeClaim instanceof Collection<?> c) {
      safe.put(
          OAuth2ParameterNames.SCOPE,
          c.stream()
              .map(String::valueOf)
              .collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
    }

    Object redirectUrl = safe.get("redirect_url");
    if (redirectUrl instanceof String s && !s.isBlank()) {
      safe.put("redirect_url", s);
    }

    metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, safe);
  }

  /**
   * Recursively convert a value to a JSON-safe representation, suitable for inclusion in the
   * OAuth2Authorization metadata.
   *
   * <p>This ensures that arbitrary POJOs are converted to Map/List/primitive structures that can be
   * serialized by Jackson without additional type information. This is needed since the metadata
   * map is serialized without type information.
   *
   * @param value the value to convert
   * @param mapper the object mapper to use for conversion of arbitrary POJOs
   * @return a JSON-safe representation of the value
   */
  @CheckForNull
  private static Object jsonSafe(@CheckForNull Object value, @Nonnull ObjectMapper mapper) {
    if (value == null) return null;
    if (value instanceof Map<?, ?> m) {
      Map<String, Object> copy = new LinkedHashMap<>();
      for (Map.Entry<?, ?> e : m.entrySet()) {
        copy.put(String.valueOf(e.getKey()), jsonSafe(e.getValue(), mapper));
      }
      return copy;
    }
    if (value instanceof Iterable<?> it) {
      List<Object> copy = new ArrayList<>();
      for (Object o : it) copy.add(jsonSafe(o, mapper));
      return copy;
    }
    // Numbers, booleans, strings, dates, etc.
    if (value.getClass().getPackageName().startsWith("java.")) {
      return value;
    }
    // Fallback: convert arbitrary POJOs to Map/List primitives
    return mapper.convertValue(value, Map.class);
  }
}
