/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.common.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import lombok.experimental.Accessors;
import org.springframework.context.ApplicationContext;

/**
 * @author Morten Olav Hansen
 */
@Accessors(chain = true)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = HttpBasicAuthScheme.class, name = HttpBasicAuthScheme.HTTP_BASIC_TYPE),
  @JsonSubTypes.Type(value = ApiTokenAuthScheme.class, name = ApiTokenAuthScheme.API_TOKEN_TYPE),
  @JsonSubTypes.Type(
      value = ApiHeadersAuthScheme.class,
      name = ApiHeadersAuthScheme.API_HEADERS_TYPE),
  @JsonSubTypes.Type(
      value = ApiQueryParamsAuthScheme.class,
      name = ApiQueryParamsAuthScheme.API_QUERY_PARAMS_TYPE),
  @JsonSubTypes.Type(
      value = OAuth2ClientCredentialsAuthScheme.class,
      name = OAuth2ClientCredentialsAuthScheme.OAUTH2_CLIENT_CREDENTIALS_TYPE)
})
public interface AuthScheme extends Serializable {
  void apply(
      ApplicationContext applicationContext,
      Map<String, List<String>> headers,
      Map<String, List<String>> queryParams)
      throws Exception;

  AuthScheme encrypt(UnaryOperator<String> encryptFunc);

  AuthScheme decrypt(UnaryOperator<String> decryptFunc);

  @JsonProperty
  String getType();
}
