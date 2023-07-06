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

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.security.oauth2.jwt.JwtClaimAccessor;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.util.Assert;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public final class JwtClaimsSet implements JwtClaimAccessor {
  private final Map<String, Object> claims;

  private JwtClaimsSet(Map<String, Object> claims) {
    this.claims = Collections.unmodifiableMap(new HashMap<>(claims));
  }

  @Override
  public Map<String, Object> getClaims() {
    return this.claims;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static Builder from(JwtClaimsSet claims) {
    return new Builder(claims);
  }

  public static final class Builder {
    private final Map<String, Object> claims = new HashMap<>();

    private Builder() {}

    private Builder(JwtClaimsSet claims) {
      Assert.notNull(claims, "claims cannot be null");
      this.claims.putAll(claims.getClaims());
    }

    public Builder issuer(String issuer) {
      return claim(JwtClaimNames.ISS, issuer);
    }

    public Builder subject(String subject) {
      return claim(JwtClaimNames.SUB, subject);
    }

    public Builder audience(List<String> audience) {
      return claim(JwtClaimNames.AUD, audience);
    }

    public Builder expiresAt(Instant expiresAt) {
      return claim(JwtClaimNames.EXP, expiresAt);
    }

    public Builder notBefore(Instant notBefore) {
      return claim(JwtClaimNames.NBF, notBefore);
    }

    public Builder issuedAt(Instant issuedAt) {
      return claim(JwtClaimNames.IAT, issuedAt);
    }

    public Builder id(String jti) {
      return claim(JwtClaimNames.JTI, jti);
    }

    public Builder claim(String name, Object value) {
      Assert.hasText(name, "name cannot be empty");
      Assert.notNull(value, "value cannot be null");
      this.claims.put(name, value);
      return this;
    }

    public Builder claims(Consumer<Map<String, Object>> claimsConsumer) {
      claimsConsumer.accept(this.claims);
      return this;
    }

    public JwtClaimsSet build() {
      Assert.notEmpty(this.claims, "claims cannot be empty");
      return new JwtClaimsSet(this.claims);
    }
  }
}
