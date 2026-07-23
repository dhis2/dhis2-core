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
 * DHIS2-specific {@link JwtDecoderFactory} used by Spring Authorization Server to verify {@code
 * private_key_jwt} client assertions (RFC 7523) at the {@code /oauth2/token} endpoint.
 *
 * <p>In the {@code private_key_jwt} authentication method, an OAuth2 client authenticates to the
 * token endpoint by signing a short-lived JWT (a "client assertion") with its private key instead
 * of sending a shared {@code client_secret}. The Authorization Server verifies the assertion's
 * signature against the client's public JWKS. Spring Authorization Server supports two JWKS sources
 * for this:
 *
 * <ul>
 *   <li>A public {@code jwks_uri} that the Authorization Server fetches over HTTP, handled by the
 *       default {@link JwtClientAssertionDecoderFactory}.
 *   <li>An inline JWKS stored directly in the client's {@link
 *       org.springframework.security.oauth2.server.authorization.settings.ClientSettings} under the
 *       {@link #CLIENT_INLINE_JWKS} key, which is the extension this factory adds.
 * </ul>
 *
 * <p>Inline JWKS support exists for clients that cannot host a public JWKS URL. The primary use
 * case is the DHIS2 Android Capture app: each device generates its keypair inside the Android
 * Keystore, registers with DHIS2 via Dynamic Client Registration (DCR) sending its public JWK
 * inline in the registration payload, and from then on authenticates to the token endpoint with
 * {@code private_key_jwt} signed by the Keystore-held private key.
 *
 * <p>For a given {@link RegisteredClient} this factory returns a {@link JwtDecoder} that:
 *
 * <ol>
 *   <li>If an inline JWKS string is present in {@code ClientSettings} under {@link
 *       #CLIENT_INLINE_JWKS}, parses it, extracts the first RSA key, and builds a {@link
 *       NimbusJwtDecoder} from that public key, honouring the client's configured token endpoint
 *       signing algorithm when present.
 *   <li>Otherwise delegates to the default {@link JwtClientAssertionDecoderFactory}, which handles
 *       {@code jwks_uri}, {@code client_secret_jwt}, and other standard cases.
 * </ol>
 *
 * <p>The same {@link OAuth2TokenValidator} is applied to both paths so inline-JWKS clients get the
 * same assertion validation rules (audience, issuer, expiry, and so on) as {@code jwks_uri}
 * clients.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class InlineJwksJwtClientAssertionDecoderFactory
    implements JwtDecoderFactory<RegisteredClient> {

  /**
   * {@link org.springframework.security.oauth2.server.authorization.settings.ClientSettings} key
   * under which the inline JWKS JSON is stored for a {@link RegisteredClient}.
   */
  public static final String CLIENT_INLINE_JWKS = "client.inline.jwks";

  private final JwtClientAssertionDecoderFactory delegate = new JwtClientAssertionDecoderFactory();

  private Function<RegisteredClient, OAuth2TokenValidator<Jwt>> jwtValidatorFactory =
      JwtClientAssertionDecoderFactory.DEFAULT_JWT_VALIDATOR_FACTORY;

  /**
   * Set the factory used to build the {@link OAuth2TokenValidator} applied to each decoded client
   * assertion. Both the inline-JWKS decoder and the delegate {@link
   * JwtClientAssertionDecoderFactory} are kept in sync so validation is identical regardless of
   * which decoder path a client uses.
   *
   * @param factory function producing a validator for a given {@link RegisteredClient}
   */
  public void setJwtValidatorFactory(
      Function<RegisteredClient, OAuth2TokenValidator<Jwt>> factory) {
    this.jwtValidatorFactory = factory;
    // keep delegate in sync so fallback uses the same validators
    this.delegate.setJwtValidatorFactory(factory);
  }

  /**
   * Create a {@link JwtDecoder} for verifying {@code private_key_jwt} client assertions from the
   * given {@link RegisteredClient}. If the client has an inline JWKS configured in its {@code
   * ClientSettings} under {@link #CLIENT_INLINE_JWKS}, a {@link NimbusJwtDecoder} is built from the
   * first RSA key in that set. Otherwise the call is delegated to {@link
   * JwtClientAssertionDecoderFactory}, which supports {@code jwks_uri}, {@code client_secret_jwt},
   * and other standard sources.
   *
   * @param client the client whose assertion is being verified
   * @return a decoder configured for this client's JWKS source
   * @throws IllegalStateException if the inline JWKS is present but cannot be parsed, or does not
   *     contain an RSA key
   */
  @Override
  public JwtDecoder createDecoder(RegisteredClient client) {
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
