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
package org.hisp.dhis.common.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.context.ApplicationContext;

@Getter
@Setter
@Accessors(chain = true)
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiQueryParamsAuthScheme implements AuthScheme {
  public static final String API_QUERY_PARAMS_TYPE = "api-query-params";

  @JsonProperty(required = true, access = JsonProperty.Access.WRITE_ONLY)
  private Map<String, String> queryParams = new HashMap<>();

  @Override
  public void apply(
      ApplicationContext applicationContext,
      Map<String, List<String>> headers,
      Map<String, List<String>> queryParams) {
    for (Map.Entry<String, String> queryParam : this.queryParams.entrySet()) {
      queryParams
          .computeIfAbsent(queryParam.getKey(), v -> new LinkedList<>())
          .add(queryParam.getValue());
    }
  }

  @Override
  public ApiQueryParamsAuthScheme encrypt(UnaryOperator<String> encryptFunc) {
    Map<String, String> encryptedQueryParams =
        queryParams.entrySet().stream()
            .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), encryptFunc.apply(e.getValue())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return this.toBuilder().queryParams(encryptedQueryParams).build();
  }

  @Override
  public ApiQueryParamsAuthScheme decrypt(UnaryOperator<String> decryptFunc) {
    Map<String, String> decryptedQueryParams =
        queryParams.entrySet().stream()
            .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), decryptFunc.apply(e.getValue())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return this.toBuilder().queryParams(decryptedQueryParams).build();
  }

  @Override
  public String getType() {
    return API_QUERY_PARAMS_TYPE;
  }
}
