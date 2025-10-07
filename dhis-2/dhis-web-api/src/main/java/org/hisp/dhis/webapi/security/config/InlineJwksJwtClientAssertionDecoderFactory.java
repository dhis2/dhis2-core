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

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.interfaces.RSAPublicKey;
import java.util.function.Function;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder.PublicKeyJwtDecoderBuilder;
import org.springframework.security.oauth2.server.authorization.authentication.JwtClientAssertionDecoderFactory;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.util.StringUtils;

/**
 * JwtDecoderFactory for client assertions that uses inline JWKS (if present in ClientSettings)
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class InlineJwksJwtClientAssertionDecoderFactory
    implements JwtDecoderFactory<RegisteredClient> {

  public static final String CLIENT_INLINE_JWKS = "client.inline.jwks";
  private final JwtClientAssertionDecoderFactory delegate = new JwtClientAssertionDecoderFactory();

  private Function<RegisteredClient, OAuth2TokenValidator<Jwt>> jwtValidatorFactory =
      JwtClientAssertionDecoderFactory.DEFAULT_JWT_VALIDATOR_FACTORY;

  public void setJwtValidatorFactory(
      Function<RegisteredClient, OAuth2TokenValidator<Jwt>> factory) {
    this.jwtValidatorFactory = factory;
    // keep delegate in sync so fallback uses the same validators
    this.delegate.setJwtValidatorFactory(factory);
  }

  /**
   * Create a JwtDecoder for the given RegisteredClient. If the client has inline JWKS configured in
   * its ClientSettings, use that to create an RSA public key JwtDecoder. Otherwise, delegate to
   * default JwtClientAssertionDecoderFactory (which supports jwks_uri, client_secret, etc).
   *
   * @param client the RegisteredClient
   * @return the JwtDecoder
   * @throws IllegalStateException if the inline JWKS is invalid or does not contain an RSA key
   */
  @Override
  public JwtDecoder createDecoder(RegisteredClient client) {
    // check for inline JWKS in client settings
    Object inlineJwks = client.getClientSettings().getSetting(CLIENT_INLINE_JWKS);
    if (inlineJwks instanceof String jwksJson && StringUtils.hasText(jwksJson)) {
      try {
        JWKSet jwkSet = JWKSet.parse(jwksJson);
        RSAKey rsaKey =
            jwkSet.getKeys().stream()
                .filter(RSAKey.class::isInstance)
                .map(k -> (RSAKey) k)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No RSA key in the inline JWKS"));
        RSAPublicKey publicKey = rsaKey.toRSAPublicKey();

        PublicKeyJwtDecoderBuilder builder = NimbusJwtDecoder.withPublicKey(publicKey);
        JwsAlgorithm alg =
            client.getClientSettings().getTokenEndpointAuthenticationSigningAlgorithm();
        if (alg instanceof SignatureAlgorithm sigAlg) {
          builder.signatureAlgorithm(sigAlg);
        }

        NimbusJwtDecoder decoder = builder.build();
        // Use the same validator as the default factory
        OAuth2TokenValidator<Jwt> defaultValidator = this.jwtValidatorFactory.apply(client);
        decoder.setJwtValidator(defaultValidator);

        return decoder;

      } catch (Exception e) {
        throw new IllegalStateException(
            "Invalid inline JWKS for client " + client.getClientId(), e);
      }
    }

    // fallback to the default decoder.
    return this.delegate.createDecoder(client);
  }
}
