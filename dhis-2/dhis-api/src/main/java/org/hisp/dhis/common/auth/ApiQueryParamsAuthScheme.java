/*
 * Copyright (c) 2004-2025, University of Oslo
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
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.util.MultiValueMap;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ApiQueryParamsAuthScheme extends AuthScheme {
  public static final String API_QUERY_PARAMS_TYPE = "api-query-params";

  @JsonProperty(required = true, access = JsonProperty.Access.WRITE_ONLY)
  private Map<String, String> queryParams = new HashMap<>();

  public ApiQueryParamsAuthScheme() {
    super(API_QUERY_PARAMS_TYPE);
  }

  @Override
  public void apply(
      MultiValueMap<String, String> headers, MultiValueMap<String, String> queryParams) {
    for (Map.Entry<String, String> queryParam : this.queryParams.entrySet()) {
      queryParams.set(queryParam.getKey(), queryParam.getValue());
    }
  }

  @Override
  public ApiQueryParamsAuthScheme encrypt(UnaryOperator<String> encryptFunc) {
    Map<String, String> encryptedQueryParams =
        queryParams.entrySet().stream()
            .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), encryptFunc.apply(e.getValue())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return copy(encryptedQueryParams);
  }

  @Override
  public ApiQueryParamsAuthScheme decrypt(UnaryOperator<String> decryptFunc) {
    Map<String, String> encryptedQueryParams =
        queryParams.entrySet().stream()
            .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), decryptFunc.apply(e.getValue())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return copy(encryptedQueryParams);
  }

  protected ApiQueryParamsAuthScheme copy(Map<String, String> queryParams) {
    ApiQueryParamsAuthScheme apiQueryParamsAuth = new ApiQueryParamsAuthScheme();
    apiQueryParamsAuth.setQueryParams(queryParams);

    return apiQueryParamsAuth;
  }
}
