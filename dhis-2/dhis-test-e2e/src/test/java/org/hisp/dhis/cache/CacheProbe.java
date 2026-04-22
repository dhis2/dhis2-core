/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.cache;

import io.restassured.http.Header;
import io.restassured.http.Headers;
import org.apache.http.HttpHeaders;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;

final class CacheProbe {
  CacheResponse get(String path) {
    RequestTarget target = RequestTarget.from(path);
    return new CacheResponse(new RestApiActions(target.resourcePath()).get(target.queryParams()));
  }

  CacheResponse getIfNoneMatch(String path, String etag) {
    RequestTarget target = RequestTarget.from(path);
    Headers headers = new Headers(new Header(HttpHeaders.IF_NONE_MATCH, etag));

    return new CacheResponse(
        new RestApiActions(target.resourcePath())
            .getWithHeaders("", target.queryParams(), headers));
  }

  CacheResponse head(String path) {
    RequestTarget target = RequestTarget.from(path);
    return new CacheResponse(new RestApiActions(target.resourcePath()).head(target.queryParams()));
  }

  record CacheResponse(ApiResponse response) {
    int statusCode() {
      return response.statusCode();
    }

    String etag() {
      return response.getHeader(HttpHeaders.ETAG);
    }

    String vary() {
      return response.getHeader(HttpHeaders.VARY);
    }

    String cacheControl() {
      return response.getHeader(HttpHeaders.CACHE_CONTROL);
    }

    boolean hasHeader(String name) {
      return response.hasHeader(name);
    }

    String body() {
      return response.getAsString();
    }

    ApiResponse raw() {
      return response;
    }
  }

  private record RequestTarget(String resourcePath, QueryParamsBuilder queryParams) {
    private static RequestTarget from(String path) {
      String normalized = path.startsWith("/") ? path : "/" + path;
      String[] split = normalized.split("\\?", 2);
      QueryParamsBuilder queryParams = null;

      if (split.length == 2 && !split[1].isBlank()) {
        queryParams = new QueryParamsBuilder();
        for (String queryParam : split[1].split("&")) {
          if (!queryParam.isBlank()) {
            queryParams.add(queryParam);
          }
        }
      }

      return new RequestTarget(split[0], queryParams);
    }
  }
}
