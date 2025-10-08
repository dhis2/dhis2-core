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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.oidc.OidcClientRegistration;
import org.springframework.security.oauth2.server.authorization.oidc.converter.OidcClientRegistrationRegisteredClientConverter;
import org.springframework.security.oauth2.server.authorization.oidc.converter.RegisteredClientOidcClientRegistrationConverter;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

/**
 * Support for inline JWKS in OIDC Dynamic Client Registration (DCR).
 *
 * <p>This allows clients to register their public keys directly in the registration request,
 * instead of hosting them at an URL. The keys are stored in the database as a JSON string in the
 * client settings JSONB column, and exposed back as an Java object in the client info response.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
final class InlineJwksClientMetadataConfig {

  public static final String CLIENT_INLINE_JWKS = "client.inline.jwks";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /** Converter used when registering a new client '/connect/register' (DCR POST). */
  public static class RegisteredClientConverter
      implements Converter<OidcClientRegistration, RegisteredClient> {

    private final OidcClientRegistrationRegisteredClientConverter delegate =
        new OidcClientRegistrationRegisteredClientConverter();

    @Override
    public RegisteredClient convert(OidcClientRegistration reg) {
      // Let SAS map standard fields first
      RegisteredClient rc = delegate.convert(reg);

      // Copy current client settings so we can add our own
      ClientSettings.Builder cs = ClientSettings.withSettings(rc.getClientSettings().getSettings());
      Map<String, Object> claims = reg.getClaims();

      // Accept optional "token_endpoint_auth_signing_alg": "RS256"
      Object alg = claims.get("token_endpoint_auth_signing_alg");
      if (alg instanceof String s && SignatureAlgorithm.from(s) != null) {
        cs.tokenEndpointAuthenticationSigningAlgorithm(SignatureAlgorithm.from(s));
      }

      Object jwks = claims.get("jwks");
      if (jwks != null) {
        try {
          String jwksJson = OBJECT_MAPPER.writeValueAsString(jwks);
          // Validation
          JWKSet.parse(jwksJson);
          cs.setting(CLIENT_INLINE_JWKS, jwksJson);
        } catch (Exception e) {
          throw new IllegalArgumentException("Invalid inline 'jwks' in registration", e);
        }
      } else {
        throw new IllegalArgumentException("'jwks' must be provided in registration");
      }

      return RegisteredClient.from(rc).clientSettings(cs.build()).build();
    }
  }

  /** Converter used when reading a client. Puts custom settings back into claims. */
  public static class ClientRegistrationConverter
      implements Converter<RegisteredClient, OidcClientRegistration> {

    private final RegisteredClientOidcClientRegistrationConverter delegate =
        new RegisteredClientOidcClientRegistrationConverter();

    @Override
    public OidcClientRegistration convert(RegisteredClient rc) {
      OidcClientRegistration out = delegate.convert(rc);

      Map<String, Object> claims = new HashMap<>(out.getClaims());
      var settings = rc.getClientSettings();

      var alg = settings.getTokenEndpointAuthenticationSigningAlgorithm();
      if (alg != null) {
        claims.put("token_endpoint_auth_signing_alg", alg.getName());
      }

      Object inline = settings.getSetting(CLIENT_INLINE_JWKS);
      if (inline instanceof String s && !s.isBlank()) {
        try {
          claims.put("jwks", OBJECT_MAPPER.readValue(s, Map.class));
        } catch (Exception ignore) {
          log.error("Failed to parse stored inline JWKS for client {}", rc.getClientId());
          // ignore, should not happen since we validate on input
        }
      }

      return OidcClientRegistration.withClaims(claims).build();
    }
  }
}
