/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.configuration.ConfigurationService;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsProcessor;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class DhisCorsProcessor implements CorsProcessor {
  public static final String CORS_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

  public static final String CORS_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

  public static final String CORS_MAX_AGE = "Access-Control-Max-Age";

  public static final String CORS_ALLOW_HEADERS = "Access-Control-Allow-Headers";

  public static final String CORS_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

  public static final String CORS_REQUEST_HEADERS = "Access-Control-Request-Headers";

  public static final String CORS_ALLOW_METHODS = "Access-Control-Allow-Methods";

  public static final String CORS_REQUEST_METHOD = "Access-Control-Request-Method";

  public static final String CORS_ORIGIN = "Origin";

  private static final String EXPOSED_HEADERS = "ETag, Location";

  private static final Integer MAX_AGE = 60 * 60; // 1hr max-age

  private final ConfigurationService configurationService;

  public DhisCorsProcessor(ConfigurationService configurationService) {
    this.configurationService = configurationService;
  }

  @Override
  public boolean processRequest(
      @Nullable CorsConfiguration configuration,
      HttpServletRequest request,
      HttpServletResponse response) {

    String origin = request.getHeader(CORS_ORIGIN);

    // Origin header is required for CORS requests

    if (StringUtils.isEmpty(origin)) {
      return true;
    }

    if (!isOriginWhitelisted(request, origin)) {
      log.debug("CORS request with origin " + origin + " is not whitelisted");
      return false;
    }

    response.addHeader(CORS_ALLOW_CREDENTIALS, "true");
    response.addHeader(CORS_ALLOW_ORIGIN, origin);
    response.addHeader("Vary", CORS_ORIGIN);

    if (isPreflight(request)) {
      String requestHeaders = request.getHeader(CORS_REQUEST_HEADERS);
      String requestMethod = request.getHeader(CORS_REQUEST_METHOD);

      response.addHeader(CORS_ALLOW_METHODS, requestMethod);
      response.addHeader(CORS_ALLOW_HEADERS, requestHeaders);
      response.addHeader(CORS_MAX_AGE, String.valueOf(MAX_AGE));

      response.setStatus(HttpServletResponse.SC_NO_CONTENT);

      // CORS preflight requires a 2xx status code, so short-circuit the
      // filter chain

    } else {
      response.addHeader(CORS_EXPOSE_HEADERS, EXPOSED_HEADERS);
    }
    return true;
  }

  private boolean isPreflight(HttpServletRequest request) {
    return RequestMethod.OPTIONS.toString().equals(request.getMethod())
        && !StringUtils.isEmpty(request.getHeader(CORS_ORIGIN))
        && !StringUtils.isEmpty(request.getHeader(CORS_REQUEST_METHOD));
  }

  private boolean isOriginWhitelisted(HttpServletRequest request, String origin) {
    HttpServletRequestEncodingWrapper encodingWrapper =
        new HttpServletRequestEncodingWrapper(request);
    UriComponentsBuilder uriBuilder =
        ServletUriComponentsBuilder.fromContextPath(encodingWrapper).replacePath("");

    String forwardedProto = request.getHeader("X-Forwarded-Proto");

    if (!StringUtils.isEmpty(forwardedProto)) {
      uriBuilder.scheme(forwardedProto);
    }

    String localUrl = uriBuilder.build().toString();

    return !StringUtils.isEmpty(origin)
        && (localUrl.equals(origin) || configurationService.isCorsWhitelisted(origin));
  }

  /**
   * Simple HttpServletRequestWrapper implementation that makes sure that the query string is
   * properly encoded.
   */
  static class HttpServletRequestEncodingWrapper extends HttpServletRequestWrapper {
    public HttpServletRequestEncodingWrapper(HttpServletRequest request) {
      super(request);
    }

    @Override
    public String getQueryString() {
      String queryString = super.getQueryString();

      if (!StringUtils.isEmpty(queryString)) {
        try {
          return URLEncoder.encode(queryString, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
        }
      }

      return queryString;
    }
  }
}
