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

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.security.oauth2.core.converter.ClaimConversionService;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.util.Assert;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public final class JoseHeader {
  private final Map<String, Object> headers;

  private JoseHeader(Map<String, Object> headers) {
    this.headers = Collections.unmodifiableMap(new HashMap<>(headers));
  }

  public JwsAlgorithm getJwsAlgorithm() {
    return getHeader(JoseHeaderNames.ALG);
  }

  public URL getJwkSetUri() {
    return getHeader(JoseHeaderNames.JKU);
  }

  public Map<String, Object> getJwk() {
    return getHeader(JoseHeaderNames.JWK);
  }

  public String getKeyId() {
    return getHeader(JoseHeaderNames.KID);
  }

  public URL getX509Uri() {
    return getHeader(JoseHeaderNames.X5U);
  }

  public List<String> getX509CertificateChain() {
    return getHeader(JoseHeaderNames.X5C);
  }

  public String getX509SHA1Thumbprint() {
    return getHeader(JoseHeaderNames.X5T);
  }

  public String getX509SHA256Thumbprint() {
    return getHeader(JoseHeaderNames.X5T_S256);
  }

  public Set<String> getCritical() {
    return getHeader(JoseHeaderNames.CRIT);
  }

  public String getType() {
    return getHeader(JoseHeaderNames.TYP);
  }

  public String getContentType() {
    return getHeader(JoseHeaderNames.CTY);
  }

  public Map<String, Object> getHeaders() {
    return this.headers;
  }

  @SuppressWarnings("unchecked")
  public <T> T getHeader(String name) {
    Assert.hasText(name, "name cannot be empty");
    return (T) getHeaders().get(name);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static Builder withAlgorithm(JwsAlgorithm jwsAlgorithm) {
    return new Builder(jwsAlgorithm);
  }

  public static Builder from(JoseHeader headers) {
    return new Builder(headers);
  }

  public static final class Builder {
    private final Map<String, Object> headers = new HashMap<>();

    private Builder() {}

    private Builder(JwsAlgorithm jwsAlgorithm) {
      Assert.notNull(jwsAlgorithm, "jwsAlgorithm cannot be null");
      header(JoseHeaderNames.ALG, jwsAlgorithm);
    }

    private Builder(JoseHeader headers) {
      Assert.notNull(headers, "headers cannot be null");
      this.headers.putAll(headers.getHeaders());
    }

    public Builder jwsAlgorithm(JwsAlgorithm jwsAlgorithm) {
      return header(JoseHeaderNames.ALG, jwsAlgorithm);
    }

    public Builder jwkSetUri(String jwkSetUri) {
      return header(JoseHeaderNames.JKU, jwkSetUri);
    }

    public Builder jwk(Map<String, Object> jwk) {
      return header(JoseHeaderNames.JWK, jwk);
    }

    public Builder keyId(String keyId) {
      return header(JoseHeaderNames.KID, keyId);
    }

    public Builder x509Uri(String x509Uri) {
      return header(JoseHeaderNames.X5U, x509Uri);
    }

    public Builder x509CertificateChain(List<String> x509CertificateChain) {
      return header(JoseHeaderNames.X5C, x509CertificateChain);
    }

    public Builder x509SHA1Thumbprint(String x509SHA1Thumbprint) {
      return header(JoseHeaderNames.X5T, x509SHA1Thumbprint);
    }

    public Builder x509SHA256Thumbprint(String x509SHA256Thumbprint) {
      return header(JoseHeaderNames.X5T_S256, x509SHA256Thumbprint);
    }

    public Builder critical(Set<String> headerNames) {
      return header(JoseHeaderNames.CRIT, headerNames);
    }

    public Builder type(String type) {
      return header(JoseHeaderNames.TYP, type);
    }

    public Builder contentType(String contentType) {
      return header(JoseHeaderNames.CTY, contentType);
    }

    public Builder header(String name, Object value) {
      Assert.hasText(name, "name cannot be empty");
      Assert.notNull(value, "value cannot be null");
      this.headers.put(name, value);
      return this;
    }

    public Builder headers(Consumer<Map<String, Object>> headersConsumer) {
      headersConsumer.accept(this.headers);
      return this;
    }

    public JoseHeader build() {
      Assert.notEmpty(this.headers, "headers cannot be empty");
      convertAsURL(JoseHeaderNames.JKU);
      convertAsURL(JoseHeaderNames.X5U);
      return new JoseHeader(this.headers);
    }

    private void convertAsURL(String header) {
      Object value = this.headers.get(header);
      if (value != null) {
        URL convertedValue = ClaimConversionService.getSharedInstance().convert(value, URL.class);
        Assert.isTrue(
            convertedValue != null,
            () ->
                "Unable to convert header '"
                    + header
                    + "' of type '"
                    + value.getClass()
                    + "' to URL.");
        this.headers.put(header, convertedValue);
      }
    }
  }
}
