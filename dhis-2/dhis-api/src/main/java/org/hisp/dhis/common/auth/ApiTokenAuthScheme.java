/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.common.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.util.StringUtils;

/**
 * Sets the Authorization header to 'ApiToken {apiToken}'. Generally to be used for dhis2 personal
 * access token, but can be used anywhere the format is accepted.
 *
 * @author Morten Olav Hansen
 */
@Getter
@Setter
@Accessors(chain = true)
public class ApiTokenAuthScheme implements AuthScheme {
  public static final String API_TOKEN_TYPE = "api-token";

  @JsonProperty(required = true, access = JsonProperty.Access.WRITE_ONLY)
  private String token;

  @Override
  public void apply(Map<String, List<String>> headers, Map<String, List<String>> queryParams) {
    if (!StringUtils.hasText(token)) {
      return;
    }

    headers.computeIfAbsent("Authorization", v -> new LinkedList<>()).add("ApiToken " + token);
  }

  @Override
  public ApiTokenAuthScheme encrypt(UnaryOperator<String> encryptFunc) {
    return copy(encryptFunc.apply(token));
  }

  @Override
  public AuthScheme decrypt(UnaryOperator<String> decryptFunc) {
    return copy(decryptFunc.apply(token));
  }

  @Override
  public String getType() {
    return API_TOKEN_TYPE;
  }

  protected ApiTokenAuthScheme copy(String token) {
    ApiTokenAuthScheme newApiTokenAuth = new ApiTokenAuthScheme();
    newApiTokenAuth.setToken(token);

    return newApiTokenAuth;
  }
}
