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
import java.util.Base64;
import java.util.function.UnaryOperator;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * @author Morten Olav Hansen
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class HttpBasicAuthScheme extends AuthScheme {
  public static final String HTTP_BASIC_TYPE = "http-basic";

  @JsonProperty(required = true)
  private String username;

  @JsonProperty(required = true, access = JsonProperty.Access.WRITE_ONLY)
  private String password;

  public HttpBasicAuthScheme() {
    super(HTTP_BASIC_TYPE);
  }

  @Override
  public void apply(
      MultiValueMap<String, String> headers, MultiValueMap<String, String> queryParams) {
    if (!(StringUtils.hasText(username) && StringUtils.hasText(password))) {
      return;
    }

    headers.add("Authorization", getBasicAuth(username, password));
  }

  @Override
  public HttpBasicAuthScheme encrypt(UnaryOperator<String> encryptFunc) {
    return copy(encryptFunc.apply(password));
  }

  @Override
  public HttpBasicAuthScheme decrypt(UnaryOperator<String> decryptFunc) {
    return copy(decryptFunc.apply(password));
  }

  protected HttpBasicAuthScheme copy(String password) {
    HttpBasicAuthScheme newHttpBasicAuth = new HttpBasicAuthScheme();
    newHttpBasicAuth.setUsername(username);
    newHttpBasicAuth.setPassword(password);

    return newHttpBasicAuth;
  }

  private String getBasicAuth(String username, String password) {
    String string = String.format("%s:%s", username, password);
    return "Basic " + Base64.getEncoder().encodeToString(string.getBytes());
  }
}
