/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.security.utils;

import com.google.gson.Gson;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.produce.JWSSignerFactory;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class JwtUtils {
  private static final String ENCODING_ERROR_MESSAGE_TEMPLATE =
      "An error occurred while attempting to encode the Jwt: %s";

  private static final JWSSignerFactory JWS_SIGNER_FACTORY = new DefaultJWSSignerFactory();

  private static final Converter<JoseHeader, JWSHeader> JWS_HEADER_CONVERTER =
      new JwsHeaderConverter();

  private static final Converter<JwtClaimsSet, JWTClaimsSet> JWT_CLAIMS_SET_CONVERTER =
      new JwtClaimsSetConverter();

  private final Map<JWK, JWSSigner> jwsSigners = new ConcurrentHashMap<>();

  private final JWKSource<SecurityContext> jwkSource;

  public JWKSource<SecurityContext> jwkSource() {
    RSAKey rsaKey = Jwks.generateRsa();
    JWKSet jwkSet = new JWKSet(rsaKey);
    return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
  }

  public JwtUtils() {
    this.jwkSource = jwkSource();
  }

  public JwtUtils(JWKSource<SecurityContext> jwkSource) {
    this.jwkSource = jwkSource;
  }

  public Jwt encode(JoseHeader headers, JwtClaimsSet claims) throws JwtEncodingException {
    Assert.notNull(headers, "headers cannot be null");
    Assert.notNull(claims, "claims cannot be null");

    JWK jwk = selectJwk(headers);
    if (jwk == null) {
      throw new JwtEncodingException(
          String.format(ENCODING_ERROR_MESSAGE_TEMPLATE, "Failed to select a JWK signing key"));
    } else if (!StringUtils.hasText(jwk.getKeyID())) {
      throw new JwtEncodingException(
          String.format(
              ENCODING_ERROR_MESSAGE_TEMPLATE,
              "The \"kid\" (key ID) from the selected JWK cannot be empty"));
    }

    headers =
        JoseHeader.from(headers).type(JOSEObjectType.JWT.getType()).keyId(jwk.getKeyID()).build();
    claims = JwtClaimsSet.from(claims).id(UUID.randomUUID().toString()).build();

    JWSHeader jwsHeader = JWS_HEADER_CONVERTER.convert(headers);
    JWTClaimsSet jwtClaimsSet = JWT_CLAIMS_SET_CONVERTER.convert(claims);

    JWSSigner jwsSigner =
        this.jwsSigners.computeIfAbsent(
            jwk,
            (key) -> {
              try {
                return JWS_SIGNER_FACTORY.createJWSSigner(key);
              } catch (JOSEException ex) {
                throw new JwtEncodingException(
                    String.format(
                        ENCODING_ERROR_MESSAGE_TEMPLATE,
                        "Failed to create a JWS Signer -> " + ex.getMessage()),
                    ex);
              }
            });

    SignedJWT signedJwt = new SignedJWT(jwsHeader, jwtClaimsSet);
    try {
      signedJwt.sign(jwsSigner);
    } catch (JOSEException ex) {
      throw new JwtEncodingException(
          String.format(
              ENCODING_ERROR_MESSAGE_TEMPLATE, "Failed to sign the JWT -> " + ex.getMessage()),
          ex);
    }
    String jws = signedJwt.serialize();

    return new Jwt(
        jws, claims.getIssuedAt(), claims.getExpiresAt(), headers.getHeaders(), claims.getClaims());
  }

  private static class JwsHeaderConverter implements Converter<JoseHeader, JWSHeader> {

    @Override
    public JWSHeader convert(JoseHeader headers) {
      JWSHeader.Builder builder =
          new JWSHeader.Builder(JWSAlgorithm.parse(headers.getJwsAlgorithm().getName()));

      Set<String> critical = headers.getCritical();
      if (!CollectionUtils.isEmpty(critical)) {
        builder.criticalParams(critical);
      }

      String contentType = headers.getContentType();
      if (StringUtils.hasText(contentType)) {
        builder.contentType(contentType);
      }

      URL jwkSetUri = headers.getJwkSetUri();
      if (jwkSetUri != null) {
        try {
          builder.jwkURL(jwkSetUri.toURI());
        } catch (Exception ex) {
          throw new JwtEncodingException(
              String.format(
                  ENCODING_ERROR_MESSAGE_TEMPLATE,
                  "Failed to convert '" + JoseHeaderNames.JKU + "' JOSE header to a URI"),
              ex);
        }
      }

      Map<String, Object> jwk = headers.getJwk();
      if (!CollectionUtils.isEmpty(jwk)) {
        try {
          builder.jwk(JWK.parse(new Gson().toJson(jwk)));
        } catch (Exception ex) {
          throw new JwtEncodingException(
              String.format(
                  ENCODING_ERROR_MESSAGE_TEMPLATE,
                  "Failed to convert '" + JoseHeaderNames.JWK + "' JOSE header"),
              ex);
        }
      }

      String keyId = headers.getKeyId();
      if (StringUtils.hasText(keyId)) {
        builder.keyID(keyId);
      }

      String type = headers.getType();
      if (StringUtils.hasText(type)) {
        builder.type(new JOSEObjectType(type));
      }

      List<String> x509CertificateChain = headers.getX509CertificateChain();
      if (!CollectionUtils.isEmpty(x509CertificateChain)) {
        builder.x509CertChain(
            x509CertificateChain.stream().map(Base64::new).collect(Collectors.toList()));
      }

      String x509SHA1Thumbprint = headers.getX509SHA1Thumbprint();
      if (StringUtils.hasText(x509SHA1Thumbprint)) {
        builder.x509CertThumbprint(new Base64URL(x509SHA1Thumbprint));
      }

      String x509SHA256Thumbprint = headers.getX509SHA256Thumbprint();
      if (StringUtils.hasText(x509SHA256Thumbprint)) {
        builder.x509CertSHA256Thumbprint(new Base64URL(x509SHA256Thumbprint));
      }

      URL x509Uri = headers.getX509Uri();
      if (x509Uri != null) {
        try {
          builder.x509CertURL(x509Uri.toURI());
        } catch (Exception ex) {
          throw new JwtEncodingException(
              String.format(
                  ENCODING_ERROR_MESSAGE_TEMPLATE,
                  "Failed to convert '" + JoseHeaderNames.X5U + "' JOSE header to a URI"),
              ex);
        }
      }

      Map<String, Object> customHeaders =
          headers.getHeaders().entrySet().stream()
              .filter(
                  (header) -> !JWSHeader.getRegisteredParameterNames().contains(header.getKey()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      if (!CollectionUtils.isEmpty(customHeaders)) {
        builder.customParams(customHeaders);
      }

      return builder.build();
    }
  }

  private static class JwtClaimsSetConverter implements Converter<JwtClaimsSet, JWTClaimsSet> {

    @Override
    public JWTClaimsSet convert(JwtClaimsSet claims) {
      JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();

      URL issuer = claims.getIssuer();
      if (issuer != null) {
        builder.issuer(issuer.toExternalForm());
      }

      String subject = claims.getSubject();
      if (StringUtils.hasText(subject)) {
        builder.subject(subject);
      }

      List<String> audience = claims.getAudience();
      if (!CollectionUtils.isEmpty(audience)) {
        builder.audience(audience);
      }

      Instant issuedAt = claims.getIssuedAt();
      if (issuedAt != null) {
        builder.issueTime(Date.from(issuedAt));
      }

      Instant expiresAt = claims.getExpiresAt();
      if (expiresAt != null) {
        builder.expirationTime(Date.from(expiresAt));
      }

      Instant notBefore = claims.getNotBefore();
      if (notBefore != null) {
        builder.notBeforeTime(Date.from(notBefore));
      }

      String jwtId = claims.getId();
      if (StringUtils.hasText(jwtId)) {
        builder.jwtID(jwtId);
      }

      Map<String, Object> customClaims =
          claims.getClaims().entrySet().stream()
              .filter((claim) -> !JWTClaimsSet.getRegisteredNames().contains(claim.getKey()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      if (!CollectionUtils.isEmpty(customClaims)) {
        customClaims.forEach(builder::claim);
      }

      return builder.build();
    }
  }

  private JWK selectJwk(JoseHeader headers) {
    JWSAlgorithm jwsAlgorithm = JWSAlgorithm.parse(headers.getJwsAlgorithm().getName());
    JWSHeader jwsHeader = new JWSHeader(jwsAlgorithm);
    JWKSelector jwkSelector = new JWKSelector(JWKMatcher.forJWSHeader(jwsHeader));

    List<JWK> jwks;
    try {
      jwks = this.jwkSource.get(jwkSelector, null);
    } catch (KeySourceException ex) {
      throw new JwtEncodingException(
          String.format(
              ENCODING_ERROR_MESSAGE_TEMPLATE,
              "Failed to select a JWK signing key -> " + ex.getMessage()),
          ex);
    }

    if (jwks.size() > 1) {
      throw new JwtEncodingException(
          String.format(
              ENCODING_ERROR_MESSAGE_TEMPLATE,
              "Found multiple JWK signing keys for algorithm '" + jwsAlgorithm.getName() + "'"));
    }

    return !jwks.isEmpty() ? jwks.get(0) : null;
  }
}
